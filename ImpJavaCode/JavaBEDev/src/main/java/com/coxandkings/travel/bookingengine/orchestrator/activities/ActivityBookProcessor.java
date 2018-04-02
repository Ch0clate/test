package com.coxandkings.travel.bookingengine.orchestrator.activities;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.activities.ActivitiesConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

@Service
public class ActivityBookProcessor implements ActivityService  {
	
	private static final Logger logger = LogManager.getLogger(ActivityBookProcessor.class);

	public String process(JSONObject reqJson) {
		try {
			KafkaBookProducer bookProducer = new KafkaBookProducer();
			
			OperationConfig opConfig = ActivitiesConfig.getOperationConfig("book");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			Element blankWrapperElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./sig:RequestBody/sig1:OTA_TourActivityBookRQWrapper");
			XMLUtils.removeNode(blankWrapperElem);

			TrackingContext.setTrackingContext(reqJson);

			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
			
			//Kafka JSON Creation
			JSONObject kafkaMsgJson = reqJson;
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_TYPE, "request");
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_PRODUCT, PRODUCT_CATEGORY);
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put("bookID", reqBodyJson.get("bookID"));

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			
			// Adding clientIATANumber to kafka reqjson
			kafkaMsgJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT)
					.put("clientIATANumber", usrCtx.getClientIATANUmber());


			XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_SESSIONID, sessionID);
			XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_TRANSACTIONID, transactionID);
			XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_USERID, userID);

			JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_RESERVATIONS);

			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			String redisKey = sessionID.concat("|").concat(PRODUCT);
			Map<String, String> reprcSuppFaresMap  = redisConn.hgetAll(redisKey);
			
			
			for (int j = 0; j < multiReqArr.length(); j++) {
				String redisPriceInfoKey = ActivityRepriceProcessor.getRedisKeyForTourActivityInfo(multiReqArr.getJSONObject(j).getJSONObject(JSON_PROP_BASICINFO),multiReqArr.getJSONObject(j).getString(SUPPLIER_ID));
				String priceInfoInStr = reprcSuppFaresMap.get(redisPriceInfoKey);
				if(null == priceInfoInStr) {
					continue;
				}
				JSONObject priceInfoJSON = new JSONObject(priceInfoInStr);
				
				// TODO : Pending 
				// TODO : Basic Info JSON needs to send unique key in request Json and this needs to be 
				// TODO : Mapped with BasicInfo request JSon and the similar needs to be put for Kafka 
				createBookRequest(reqElem, ownerDoc, blankWrapperElem, usrCtx, multiReqArr, j,priceInfoJSON);
				JSONObject kafkaReservationJson = kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_RESERVATIONS).getJSONObject(j);
				
				JSONArray suppPriceInfo = priceInfoJSON.getJSONObject(JSON_PROP_SUPPPRICEINFO).getJSONArray(JSON_PROP_PRICING);
				kafkaReservationJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceInfo);
				
				kafkaReservationJson.put(JSON_PROP_TOTAL_PRICE_INFO, priceInfoJSON.getJSONArray(JSON_PROP_TOTAL_PRICE_INFO));
				
				kafkaReservationJson.remove(JSON_PROP_PRICING);
			}

			System.out.println(XMLTransformer.toString(reqElem));
		
			System.out.println("kafka msg JSon for Request :  " + kafkaMsgJson);
			
			
          // TODO Kafka msg before SI request. 
            bookProducer.runProducer(1, kafkaMsgJson);
			
			Element resElem = null;
			logger.trace(String.format("SI XML Request = %s", XMLTransformer.toString(reqElem)));
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					ActivitiesConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception(ERROR_MSG_NULL_RESPONSE_SI);
			}
			logger.trace(String.format("SI XML Response = %s", XMLTransformer.toString(resElem)));

		System.out.println("Respnse JSON from Suppliers : " + XMLTransformer.toString(resElem) );
				JSONObject resJson = getBookResponseJSON(reqHdrJson, reqBodyJson, resElem);
				System.out.println("Response JSON : "+ resJson);
				
				kafkaMsgJson = resJson;
				kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_PRODUCT, PRODUCT_CATEGORY);
				// Adding clientIATANumber to kafka resjson
				kafkaMsgJson.getJSONObject(JSON_PROP_RESHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).put("clientIATANumber", usrCtx.getClientIATANUmber());
				System.out.println(kafkaMsgJson);
				
//			TODO KafkaMsg after SI request
			bookProducer.runProducer(1, kafkaMsgJson);
				
//			TODO send Book response json. Uncomment below line
			return resJson.toString();
//			return XMLTransformer.toString(resElem);
		} catch (Exception e) {
			e.printStackTrace();
			return STATUS_ERROR;
		}

	}

	/**
	 * @param reqHdrJson
	 * @param resElem
	 * @return
	 */
	private JSONObject getBookResponseJSON(JSONObject reqHdrJson,JSONObject reqBodyJson, Element resElem) {
		JSONObject resJson = new JSONObject();
		JSONObject resBodyJson = new JSONObject();
		
		resBodyJson.put("product", "Activities");
		JSONArray supplierBookRefrencesArr = new JSONArray();

		Element[] wrappers = XMLUtils.getElementsAtXPath(resElem,
				"./sig:ResponseBody/sig1:OTA_TourActivityBookRSWrapper");

		for (int wrapperCount = 0; wrapperCount < wrappers.length; wrapperCount++) {

			JSONObject supplierBookRefrence = new JSONObject();

			supplierBookRefrence.put(SUPPLIER_ID, XMLUtils.getValueAtXPath(wrappers[wrapperCount], "./sig1:SupplierID"));

			// For now it is done as Single Concatenated String from multiple confirmation
			// tags
			StringBuilder confirmationID = new StringBuilder();
			Element[] confirmations = XMLUtils.getElementsAtXPath(wrappers[wrapperCount],
					"./ota:OTA_TourActivityBookRS/ota:ReservationDetails/ota:Confirmation");
			for (int confirmationCount = 0; confirmationCount < confirmations.length; confirmationCount++) {
				confirmationID.append(XMLUtils.getValueAtXPath(confirmations[confirmationCount], "./@ID")).append(",");
				confirmationID.append(XMLUtils.getValueAtXPath(confirmations[confirmationCount], "./@Type"))
						.append(",");
				confirmationID.append(XMLUtils.getValueAtXPath(confirmations[confirmationCount], "./@Instance"));
				if (confirmationCount != (confirmations.length - 1)) {
					confirmationID.append("|");
				}
			}

			supplierBookRefrence.put(JSON_PROP_BOOKREFID, confirmationID);
			supplierBookRefrencesArr.put(supplierBookRefrence);
			supplierBookRefrence=null;
		}
		resBodyJson.put("supplierBookReferences", supplierBookRefrencesArr);
		resBodyJson.put("bookID", reqBodyJson.get("bookID")); 

		resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
		resJson.put(JSON_PROP_RESBODY, resBodyJson);
		return resJson;}

	/**
	 * @param reqElem
	 * @param ownerDoc
	 * @param blankWrapperElem
	 * @param usrCtx
	 * @param multiReqArr
	 * @param j
	 * @throws Exception
	 */
	private void createBookRequest(Element reqElem, Document ownerDoc, Element blankWrapperElem, UserContext usrCtx,
			JSONArray multiReqArr, int j,JSONObject priceInfoJSON) throws Exception {
		String suppID;
		ProductSupplier prodSupplier;
		JSONObject reservationDetail = multiReqArr.getJSONObject(j);
		suppID = reservationDetail.getString(SUPPLIER_ID);
		prodSupplier = usrCtx.getSupplierForProduct(PRODUCT_CATEGORY,PRODUCT_SUBCATEGORY, suppID);

		if (prodSupplier == null) {
			throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
		}

		XMLUtils.insertChildNode(reqElem, XPATH_REQUESTHEADER_SUPPLIERCREDENTIALSLIST,
				prodSupplier.toElement(ownerDoc, j), false);
		Element wrapperElement = (Element) blankWrapperElem.cloneNode(true);
		Document wrapperOwner = wrapperElement.getOwnerDocument();

		XMLUtils.setValueAtXPath(wrapperElement, "./sig1:SupplierID", suppID);
		XMLUtils.setValueAtXPath(wrapperElement, "./sig1:Sequence", String.valueOf(j));

		Element source = XMLUtils.getFirstElementAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:POS/ota:Source");

		source.setAttribute(JSON_PROP_ISO_CURRENCY, reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONObject(JSON_PROP_POS).getString(JSON_PROP_ISO_CURRENCY));

		Element contactDetail = XMLUtils.getFirstElementAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:ContactDetail");

		createContactDetails(reservationDetail, contactDetail);

		createBasicInfo(reservationDetail, wrapperElement, wrapperOwner);

		Element schedule = XMLUtils.getFirstElementAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:Schedule");

		createParticipantInfo(reservationDetail, wrapperElement, wrapperOwner, schedule);

		createSchedule(reservationDetail, schedule);

		createLocation(reservationDetail, wrapperElement);

		createPickupDropOff(reservationDetail, wrapperElement);

		createPricing(reservationDetail, wrapperElement,priceInfoJSON);

		XMLUtils.insertChildNode(reqElem, "./sig:RequestBody", wrapperElement, false);

	}

	/**
	 * @param reservationDetail
	 * @param wrapperElement
	 * @param wrapperOwner
	 */
	private void createParticipantInfo(JSONObject reservationDetail, Element wrapperElement, Document wrapperOwner,
			Element schedule) {
		JSONArray particiapntInfo = reservationDetail.getJSONArray(JSON_PROP_PARTICIPANT_INFO);

		Element bookingInfo = XMLUtils.getFirstElementAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo");

		for (int participantCount = 0; participantCount < particiapntInfo.length(); participantCount++) {
			Element participantInfo = wrapperOwner.createElementNS(NS_OTA, "n3:ParticipantInfo");

			Element category = wrapperOwner.createElementNS(NS_OTA, "n3:Category");

			String Age = calculateAge(particiapntInfo, participantCount);

			category.setAttribute("Age", Age);
			category.setAttribute("Quantity", "1");
			category.setAttribute("ParticipantCategoryID", "1");

			Element qualifierInfo = createQualifierInfo(wrapperOwner, particiapntInfo, participantCount);

			Element contact = wrapperOwner.createElementNS(NS_OTA, "n3:Contact");

			Element personName = createPersonName(wrapperOwner, particiapntInfo, participantCount);

			Element telephone = createParticiapntInfoTelephone(wrapperOwner, particiapntInfo, participantCount);

			contact.appendChild(personName);
			contact.appendChild(telephone);

			category.appendChild(qualifierInfo);
			category.appendChild(contact);

			participantInfo.appendChild(category);

			bookingInfo.insertBefore(participantInfo, schedule);

		}
	}

	/**
	 * @param particiapntInfo
	 * @param participantCount
	 * @return
	 */
	private String calculateAge(JSONArray particiapntInfo, int participantCount) {
		String DOB = particiapntInfo.getJSONObject(participantCount).getString(JSON_PROP_DOB);
		String[] dateOFBirthArray = DOB.split("-");
		int date = Integer.parseInt(dateOFBirthArray[2]);
		int month = Integer.parseInt(dateOFBirthArray[1]);
		int year = Integer.parseInt(dateOFBirthArray[0]);
		LocalDate birthDate = LocalDate.of(year, month, date);
		LocalDate currentDate = LocalDate.now();
		String Age = Integer.toString(Period.between(birthDate, currentDate).getYears());
		return Age;
	}

	/**
	 * @param wrapperOwner
	 * @param particiapntInfo
	 * @param participantCount
	 * @return
	 */
	private Element createQualifierInfo(Document wrapperOwner, JSONArray particiapntInfo, int participantCount) {
		Element qualifierInfo = wrapperOwner.createElementNS(NS_OTA, "n3:QualifierInfo");
		qualifierInfo.setAttribute("Extension", "1");
		qualifierInfo.setTextContent(particiapntInfo.getJSONObject(participantCount).getString(JSON_PROP_QUALIFIERINFO));
		return qualifierInfo;
	}

	/**
	 * @param wrapperOwner
	 * @param particiapntInfo
	 * @param participantCount
	 * @return
	 */
	private Element createParticiapntInfoTelephone(Document wrapperOwner, JSONArray particiapntInfo,
			int participantCount) {
		Element telephone = wrapperOwner.createElementNS(NS_OTA, "n3:Telephone");
		telephone.setAttribute("CountryAccessCode", particiapntInfo.getJSONObject(participantCount)
				.getJSONObject(JSON_PROP_CONTACTDETAILS).getString(JSON_PROP_CTRYACESCODE));
		telephone.setAttribute("PhoneNumber", particiapntInfo.getJSONObject(participantCount)
				.getJSONObject(JSON_PROP_CONTACTDETAILS).getString(JSON_PROP_PHONE_NUMBER));
		return telephone;
	}

	/**
	 * @param wrapperOwner
	 * @param particiapntInfo
	 * @param participantCount
	 * @return
	 */
	private Element createPersonName(Document wrapperOwner, JSONArray particiapntInfo, int participantCount) {
		Element personName = wrapperOwner.createElementNS(NS_OTA, "n3:PersonName");

		Element namePrefix = wrapperOwner.createElementNS(NS_OTA, "n3:NamePrefix");
		namePrefix.setTextContent(
				particiapntInfo.getJSONObject(participantCount).getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_NAME_PREFIX));

		Element givenName = wrapperOwner.createElementNS(NS_OTA, "n3:GivenName");
		givenName.setTextContent(
				particiapntInfo.getJSONObject(participantCount).getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_GIVEN_NAME));

		Element middleName = wrapperOwner.createElementNS(NS_OTA, "n3:MiddleName");
		middleName.setTextContent(
				particiapntInfo.getJSONObject(participantCount).getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_MIDDLE_NAME));

		Element surname = wrapperOwner.createElementNS(NS_OTA, "n3:Surname");
		surname.setTextContent(
				particiapntInfo.getJSONObject(participantCount).getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_SURNAME));

		Element nameTitle = wrapperOwner.createElementNS(NS_OTA, "n3:NameTitle");
		nameTitle.setTextContent(
				particiapntInfo.getJSONObject(participantCount).getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_NAME_TITLE));

		personName.appendChild(namePrefix);
		personName.appendChild(givenName);
		personName.appendChild(middleName);
		personName.appendChild(surname);
		personName.appendChild(nameTitle);
		return personName;
	}

	/**
	 * @param reservationDetail
	 * @param wrapperElement
	 * @param wrapperOwner
	 */
	private void createPricing(JSONObject reservationDetail, Element wrapperElement, JSONObject priceInfoJSON) {
		Element pricingElem = XMLUtils.getFirstElementAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:Pricing");
		
        Document priceOwner = pricingElem.getOwnerDocument(); 
		
		JSONArray pricingJSONElem = priceInfoJSON.getJSONObject(JSON_PROP_SUPPPRICEINFO).getJSONArray(JSON_PROP_PRICING);
		
			for (int pricingCount = 0; pricingCount < pricingJSONElem.length(); pricingCount++) {
				if(JSON_PROP_SUMMARY.equals(pricingJSONElem.getJSONObject(pricingCount).getString(JSON_PROP_PARTICIPANTCATEGORY))) {
					XMLUtils.setValueAtXPath(wrapperElement,
							"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:Pricing/ota:Summary/@CurrencyCode",
							pricingJSONElem.getJSONObject(pricingCount).getString(JSON_PROP_CCYCODE));

					XMLUtils.setValueAtXPath(wrapperElement,
							"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:Pricing/ota:Summary/@Amount",
							pricingJSONElem.getJSONObject(pricingCount).getBigDecimal(JSON_PROP_TOTALPRICE).toString());
	
				}else {
				Element particiapntCategoryPricing = priceOwner.createElementNS(NS_OTA, "ota:ParticipantCategory");

				Element qualifierInfo = priceOwner.createElementNS(NS_OTA, "ota:QualifierInfo");
			
				qualifierInfo
						.setTextContent(pricingJSONElem.getJSONObject(pricingCount).getString(JSON_PROP_PARTICIPANTCATEGORY));

				Element price = priceOwner.createElementNS(NS_OTA, "ota:Price");
				price.setAttribute("Amount",pricingJSONElem.getJSONObject(pricingCount).getBigDecimal(JSON_PROP_TOTALPRICE).toString());

				particiapntCategoryPricing.appendChild(qualifierInfo);
				particiapntCategoryPricing.appendChild(price);

				pricingElem.appendChild(particiapntCategoryPricing);
				}
			}
	}

	/**
	 * @param reservationDetail
	 * @param wrapperElement
	 */
	private void createPickupDropOff(JSONObject reservationDetail, Element wrapperElement) {
		XMLUtils.setValueAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:PickupDropoff/@DateTime",
				reservationDetail.getJSONObject("pickupDropoff").getString("dateTime"));
		
		XMLUtils.setValueAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:PickupDropoff/@LocationName",
				reservationDetail.getJSONObject("pickupDropoff").getString("locationName"));
		
	}

	/**
	 * @param reservationDetail
	 * @param wrapperElement
	 */
	private void createLocation(JSONObject reservationDetail, Element wrapperElement) {
		XMLUtils.setValueAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:Location/ota:Address/ota:CountryName/@Code",
				reservationDetail.getString(JSON_PROP_COUNTRYCODE));

		XMLUtils.setValueAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:Location/ota:Region/@RegionCode",
				reservationDetail.getString(JSON_PROP_CITYCODE));
	}

	/**
	 * @param reservationDetail
	 * @param schedule
	 */
	private void createSchedule(JSONObject reservationDetail, Element schedule) {
		JSONObject scheduleJSONElem = reservationDetail.getJSONObject(JSON_PROP_SCHEDULE);

		XMLUtils.setValueAtXPath(schedule, "./@Start", scheduleJSONElem.getString(JSON_PROP_START));

		XMLUtils.setValueAtXPath(schedule, "./@End", scheduleJSONElem.getString(JSON_PROP_END));
	}

	/**
	 * @param reservationDetail
	 * @param wrapperElement
	 * @param wrapperOwner
	 */
	private void createBasicInfo(JSONObject reservationDetail, Element wrapperElement, Document wrapperOwner) {
		Element basicInfo = XMLUtils.getFirstElementAtXPath(wrapperElement,
				"./ota:OTA_TourActivityBookRQ/ota:BookingInfo/ota:BasicInfo");

		createBasicInfoAttr(reservationDetail, basicInfo);

		Element activityTPA = XMLUtils.getFirstElementAtXPath(basicInfo, "./ota:TPA_Extensions/sig1:Activity_TPA");

		createCancellationPolicy(reservationDetail, wrapperOwner, activityTPA);
		
		createTourLanguages(reservationDetail, wrapperOwner, activityTPA);

		createTimeSlots(reservationDetail, wrapperOwner, activityTPA);

		createSupplierDetails(reservationDetail, activityTPA);

		createAnswers(reservationDetail, wrapperOwner, activityTPA);

		createShippingDetails(reservationDetail, activityTPA);

		createPOS(reservationDetail, activityTPA);
	}

	/**
	 * @param reservationDetail
	 * @param wrapperOwner
	 * @param activityTPA
	 */
	private void createTourLanguages(JSONObject reservationDetail, Document wrapperOwner, Element activityTPA) {
		JSONArray tourLanguages = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONArray(JSON_PROP_TOURLANGUAGE);
		
		Element tourLanguageElement = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:TourLanguage");
		tourLanguageElement.setAttribute("Code", tourLanguages.getJSONObject(0).getString(JSON_PROP_CODE));
		tourLanguageElement.setAttribute("LanguageListCode", tourLanguages.getJSONObject(0).getString(JSON_PROP_LANGUAGELISTCODE));
		tourLanguageElement.setTextContent(tourLanguages.getJSONObject(0).getString(JSON_PROP_VALUE));
		activityTPA.appendChild(tourLanguageElement);
	}

	/**
	 * @param reservationDetail
	 * @param basicInfo
	 */
	private void createBasicInfoAttr(JSONObject reservationDetail, Element basicInfo) {
		XMLUtils.setValueAtXPath(basicInfo, "./@SupplierProductCode",
				reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getString(JSON_PROP_SUPPLIERPRODUCTCODE));

		XMLUtils.setValueAtXPath(basicInfo, "./@SupplierBrandCode", reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getString("supplierBrandCode"));

		XMLUtils.setValueAtXPath(basicInfo, "./@Name", reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getString(JSON_PROP_NAME));
	}

	/**
	 * @param reservationDetail
	 * @param wrapperOwner
	 * @param activityTPA
	 */
	private void createCancellationPolicy(JSONObject reservationDetail, Document wrapperOwner, Element activityTPA) {
		JSONArray cancellationPolicyJson = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONArray("cancellationPolicy");

		for (int cancellationCount = 0; cancellationCount < cancellationPolicyJson.length(); cancellationCount++) {

			Element cancellationPolicyUnit = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:Unit");
			cancellationPolicyUnit
					.setTextContent(cancellationPolicyJson.getJSONObject(cancellationCount).getString("unit"));

			Element cancellationPolicyFromValue = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:FromValue");
			cancellationPolicyFromValue
					.setTextContent(cancellationPolicyJson.getJSONObject(cancellationCount).getString("fromValue"));

			Element cancellationPolicyChargeType = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:ChargeType");
			cancellationPolicyChargeType
					.setTextContent(cancellationPolicyJson.getJSONObject(cancellationCount).getString("chargeType"));

			Element cancellationPolicyRate = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:Rate");
			cancellationPolicyRate
					.setTextContent(cancellationPolicyJson.getJSONObject(cancellationCount).getString("rate"));

			Element cancellationPolicy = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:CancellationPolicy");

			cancellationPolicy.appendChild(cancellationPolicyUnit);
			cancellationPolicy.appendChild(cancellationPolicyFromValue);
			cancellationPolicy.appendChild(cancellationPolicyChargeType);
			cancellationPolicy.appendChild(cancellationPolicyRate);

			activityTPA.appendChild(cancellationPolicy);

		}
	}

	/**
	 * @param reservationDetail
	 * @param wrapperOwner
	 * @param activityTPA
	 */
	private void createTimeSlots(JSONObject reservationDetail, Document wrapperOwner, Element activityTPA) {
		JSONArray timeslots = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONArray(JSON_PROP_TIME_SLOT_DETAILS);
		Element timeSlotElem = XMLUtils.getFirstElementAtXPath(activityTPA, "./sig1:TimeSlots");

		for (int timeSlotCount = 0; timeSlotCount < timeslots.length(); timeSlotCount++) {
			Element timeSlot = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:TimeSlot");

			timeSlot.setAttribute(JSON_PROP_CODE, timeslots.getJSONObject(timeSlotCount).getString(JSON_PROP_CODE));
			timeSlot.setAttribute(JSON_PROP_STARTTIME, timeslots.getJSONObject(timeSlotCount).getString(JSON_PROP_STARTTIME));
			timeSlot.setAttribute(JSON_PROP_ENDTIME, timeslots.getJSONObject(timeSlotCount).getString(JSON_PROP_ENDTIME));

			timeSlotElem.appendChild(timeSlot);
		}
	}

	/**
	 * @param reservationDetail
	 * @param activityTPA
	 */
	private void createSupplierDetails(JSONObject reservationDetail, Element activityTPA) {
		JSONObject supplier_Details = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONObject(JSON_PROP_SUPPLIER_DETAILS);

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Supplier_Details/sig1:name", supplier_Details.getString(JSON_PROP_NAME));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Supplier_Details/sig1:ID", supplier_Details.getString(JSON_PROP_ID));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Supplier_Details/sig1:Reference",
				supplier_Details.getString(JSON_PROP_REFERENCE));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Supplier_Details/sig1:RateKey",
				supplier_Details.getString(JSON_PROP_RATEKEY));
	}

	/**
	 * @param reservationDetail
	 * @param wrapperOwner
	 * @param activityTPA
	 */
	private void createAnswers(JSONObject reservationDetail, Document wrapperOwner, Element activityTPA) {
		JSONArray answersJsonArr = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONArray("answers");

		Element answers = XMLUtils.getFirstElementAtXPath(activityTPA, "./sig1:Answers");

		for (int answersCount = 0; answersCount < answersJsonArr.length(); answersCount++) {

			Element questionID = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:QuestionID");
			questionID.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("questionID"));

			Element questionText = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:QuestionText");
			questionText.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("questionText"));

			Element answerType = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:AnswerType");
			answerType.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("answerType"));

			Element answerExample = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:answerExample");
			answerExample.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("answerExample"));

			Element requiredFlag = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:RequiredFlag");
			requiredFlag.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("requiredFlag"));

			Element extraInfo = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:ExtraInfo");
			extraInfo.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("extraInfo"));

			Element question = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:Question");
			question.appendChild(questionID);
			question.appendChild(questionText);
			question.appendChild(answerType);
			question.appendChild(answerExample);

			Element answer = null;
			if (!answersJsonArr.getJSONObject(answersCount).getString("answer").isEmpty()) {
				answer = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:Answer");
				answer.setTextContent(answersJsonArr.getJSONObject(answersCount).getString("answer"));
			}

			Element answerSet = wrapperOwner.createElementNS(NS_SIGHTSEEING, "n2:Answer");
			answerSet.appendChild(question);
			if (answer != null)
				answerSet.appendChild(answer);

			answers.appendChild(answerSet);
		}
	}

	/**
	 * @param reservationDetail
	 * @param activityTPA
	 */
	private void createPOS(JSONObject reservationDetail, Element activityTPA) {
		JSONObject POSJSONElem = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONObject(JSON_PROP_POS);

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:POS/sig1:Source", POSJSONElem.getString("source"));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:POS/sig1:Agent_Name", POSJSONElem.getString("agent_Name"));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:POS/sig1:Email", POSJSONElem.getString(JSON_PROP_EMAIL));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:POS/sig1:Phone", POSJSONElem.getString("phone"));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:POS/sig1:Country", POSJSONElem.getString("country"));
	}

	/**
	 * @param reservationDetail
	 * @param activityTPA
	 */
	private void createShippingDetails(JSONObject reservationDetail, Element activityTPA) {
		JSONObject shipping_Details = reservationDetail.getJSONObject(JSON_PROP_BASICINFO).getJSONObject(JSON_PROP_SHIPPINGDETAILS);

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:ID", shipping_Details.getString(JSON_PROP_ID));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:OptionName",
				shipping_Details.getString("optionName"));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:Details",
				shipping_Details.getString(JSON_PROP_DETAILS));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:ShippingAreas/sig1:RateList/sig1:AreaID",
				shipping_Details.getJSONObject(JSON_PROP_SHIPPINGAREAS).getString(JSON_PROP_AREAID));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:ShippingAreas/sig1:RateList/sig1:Name",
				shipping_Details.getJSONObject(JSON_PROP_SHIPPINGAREAS).getString(JSON_PROP_NAME));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:ShippingAreas/sig1:RateList/sig1:Cost",
				shipping_Details.getJSONObject(JSON_PROP_SHIPPINGAREAS).getString("cost"));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:TotalCost",
				shipping_Details.getString(JSON_PROP_TOTALCOST));

		XMLUtils.setValueAtXPath(activityTPA, "./sig1:Shipping_Details/sig1:Currency",
				shipping_Details.getString("currency"));
	}

	/**
	 * @param reservationDetail
	 * @param contactDetail
	 */
	private void createContactDetails(JSONObject reservationDetail, Element contactDetail) {
		JSONObject contactDetailJson = reservationDetail.getJSONObject("contactDetail");

		XMLUtils.setValueAtXPath(contactDetail, "./ota:PersonName/ota:NamePrefix",
				contactDetailJson.getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_NAME_PREFIX));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:PersonName/ota:GivenName",
				contactDetailJson.getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_GIVEN_NAME));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:PersonName/ota:MiddleName",
				contactDetailJson.getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_MIDDLE_NAME));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:PersonName/ota:Surname",
				contactDetailJson.getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_SURNAME));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:PersonName/ota:NameTitle",
				contactDetailJson.getJSONObject(JSON_PROP_PERSONNAME).getString(JSON_PROP_NAME_TITLE));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:Telephone/@CountryAccessCode",
				contactDetailJson.getJSONObject(JSON_PROP_TELEPHONE).getString(JSON_PROP_CTRYACESCODE));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:Telephone/@PhoneNumber",
				contactDetailJson.getJSONObject(JSON_PROP_TELEPHONE).getString(JSON_PROP_PHONE_NUMBER));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:Address/ota:BldgRoom",
				contactDetailJson.getJSONObject(JSON_PROP_ADDRESS).getString(JSON_PROP_BLDG_ROOM));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:Address/ota:AddressLine",
				contactDetailJson.getJSONObject(JSON_PROP_ADDRESS).getString(JSON_PROP_ADDRESSLINE));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:Address/ota:CountryName",
				contactDetailJson.getJSONObject(JSON_PROP_ADDRESS).getString(JSON_PROP_COUNTRYCODE));

		XMLUtils.setValueAtXPath(contactDetail, "./@BirthDate", contactDetailJson.getString(JSON_PROP_DOB));

		XMLUtils.setValueAtXPath(contactDetail, "./ota:Email", contactDetailJson.getString(JSON_PROP_EMAIL));
	}

}
