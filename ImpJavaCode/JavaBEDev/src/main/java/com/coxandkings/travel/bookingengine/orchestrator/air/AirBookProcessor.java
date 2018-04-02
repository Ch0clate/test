package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.ServletContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import redis.clients.jedis.Jedis;

public class AirBookProcessor implements AirConstants {

	private static final Logger logger = LogManager.getLogger(AirBookProcessor.class);

	public static String process(JSONObject reqJson) {
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
		  JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
		try {
			TrackingContext.setTrackingContext(reqJson);
			KafkaBookProducer bookProducer = new KafkaBookProducer();
			OperationConfig opConfig = AirConfig.getOperationConfig(TrackingContext.getTrackingContext().getTrackingParameter(ServletContext.RESTPROP_OPERATION));
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_AirBookRQWrapper");
			reqBodyElem.removeChild(wrapperElem);

			JSONObject kafkaMsgJson = reqJson;
			/*JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);*/

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			AirSearchProcessor.createHeader(reqHdrJson, reqElem);

			Map<String, String> reprcSuppFaresMap = null;
			Map<String,String> ptcBrkDwnMap=null;
			try ( Jedis redisConn = RedisConfig.getRedisConnectionFromPool(); ) {
				String redisKey = sessionID.concat("|").concat(PRODUCT_AIR);
				String ptcRedisKey="ptcXml|".concat(sessionID.concat("|").concat(PRODUCT_AIR));
				reprcSuppFaresMap = redisConn.hgetAll(redisKey);
				ptcBrkDwnMap= redisConn.hgetAll(ptcRedisKey);
				if (reprcSuppFaresMap == null || reprcSuppFaresMap.isEmpty()) {
					throw new Exception(String.format("Reprice context not found for %s", redisKey));
				}
				if (ptcBrkDwnMap == null || ptcBrkDwnMap.isEmpty()) {
					throw new Exception(String.format("Ptc Break down not found for %s", ptcRedisKey));
				}
				
			}
			
			String prevSuppID = "";
			JSONArray pricedItinsJSONArr = reqBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
			for (int y = 0; y < pricedItinsJSONArr.length(); y++) {
				JSONObject pricedItinJson = pricedItinsJSONArr.getJSONObject(y);
				Map<String,String> flightMap=new HashMap<String,String>();

				String suppID = pricedItinJson.getString(JSON_PROP_SUPPREF);
				Element suppWrapperElem = null;
				Element otaReqElem = null;
				Element travelerInfoElem = null;
				Element odosElem = null;
				Element specialRqDetElem=null;
				if (suppID.equals(prevSuppID)) {
					suppWrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem,String.format("./air:OTA_AirBookRQWrapper[air:SupplierID = '%s']", suppID));
					travelerInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirBookRQ/ota:TravelerInfo");
					otaReqElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirBookRQ");
					
					odosElem = XMLUtils.getFirstElementAtXPath(otaReqElem, "./ota:AirItinerary/ota:OriginDestinationOptions");
					if (odosElem == null) {
						logger.warn(String.format("XML element for ota:OriginDestinationOptions not found for supplier %s", suppID));
					}
				} 
				else {
					suppWrapperElem = (Element) wrapperElem.cloneNode(true);
					reqBodyElem.appendChild(suppWrapperElem);

					ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT, suppID);
					if (prodSupplier == null) {
						throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
					}

					Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
					Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem, String.format("./com:SupplierCredentials[com:SupplierID = '%s']", suppID));
					if (suppCredsElem == null) {
						suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
					}

					Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./air:SupplierID");
					suppIDElem.setTextContent(suppID);

					travelerInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem,	"./ota:OTA_AirBookRQ/ota:TravelerInfo");
					otaReqElem = (Element) travelerInfoElem.getParentNode();
					
					Element airItinElem = ownerDoc.createElementNS(NS_OTA, "ota:AirItinerary");
					airItinElem.setAttribute("DirectionInd", reqBodyJson.getString(JSON_PROP_TRIPTYPE));
					odosElem = ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOptions");
					airItinElem.appendChild(odosElem);
					otaReqElem.insertBefore(airItinElem, travelerInfoElem);
				}

				JSONObject airItinJson = pricedItinJson.getJSONObject(JSON_PROP_AIRITINERARY);
				JSONArray odoJsonArr = airItinJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
				for (int i = 0; i < odoJsonArr.length(); i++) {
					JSONObject odoJson = odoJsonArr.getJSONObject(i);
					Element odoElem = ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOption");
					JSONArray flSegJsonArr = odoJson.getJSONArray(JSON_PROP_FLIGHTSEG);
					for (int j = 0; j < flSegJsonArr.length(); j++) {
						JSONObject flSegJson = flSegJsonArr.getJSONObject(j);
						Element flSegElem = ownerDoc.createElementNS(NS_OTA, "ota:FlightSegment");

						Element depAirportElem = ownerDoc.createElementNS(NS_OTA, "ota:DepartureAirport");
						depAirportElem.setAttribute("LocationCode", flSegJson.getString(JSON_PROP_ORIGLOC));
						flSegElem.appendChild(depAirportElem);

						Element arrAirportElem = ownerDoc.createElementNS(NS_OTA, "ota:ArrivalAirport");
						arrAirportElem.setAttribute("LocationCode", flSegJson.getString(JSON_PROP_DESTLOC));
						flSegElem.appendChild(arrAirportElem);

						Element opAirlineElem = ownerDoc.createElementNS(NS_OTA, "ota:OperatingAirline");
						JSONObject opAirlineJson = flSegJson.getJSONObject(JSON_PROP_OPERAIRLINE);
						flSegElem.setAttribute("FlightNumber", opAirlineJson.getString(JSON_PROP_FLIGHTNBR));
						flightMap.put((opAirlineJson.getString(JSON_PROP_FLIGHTNBR).replaceAll("\\s","")).toString(), flSegJson.getString(JSON_PROP_RESBOOKDESIG));
						flSegElem.setAttribute("DepartureDateTime", flSegJson.getString(JSON_PROP_DEPARTDATE));
						flSegElem.setAttribute("ArrivalDateTime", flSegJson.getString(JSON_PROP_ARRIVEDATE));
						String connType = flSegJson.optString(JSON_PROP_CONNTYPE);
						if (connType != null) {
							flSegElem.setAttribute("ConnectionType", connType);
						}
						opAirlineElem.setAttribute("Code", opAirlineJson.getString(JSON_PROP_AIRLINECODE));
						opAirlineElem.setAttribute("FlightNumber", opAirlineJson.getString(JSON_PROP_FLIGHTNBR));
						String companyShortName = opAirlineJson.optString(JSON_PROP_COMPANYSHORTNAME, "");
						if (companyShortName.isEmpty() == false) {
							opAirlineElem.setAttribute("CompanyShortName", companyShortName);
						}
						flSegElem.appendChild(opAirlineElem);

						Element tpaExtsElem = ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
						Element extRPHElem = ownerDoc.createElementNS(NS_AIR, "air:ExtendedRPH");
						extRPHElem.setTextContent(flSegJson.getString(JSON_PROP_EXTENDEDRPH));
						tpaExtsElem.appendChild(extRPHElem);

						Element quoteElem = ownerDoc.createElementNS(NS_AIR, "air:QuoteID");
						quoteElem.setTextContent(flSegJson.getString(JSON_PROP_QUOTEID));
						tpaExtsElem.appendChild(quoteElem);

						flSegElem.appendChild(tpaExtsElem);

						Element mkAirlineElem = ownerDoc.createElementNS(NS_OTA, "ota:MarketingAirline");
						JSONObject mkAirlineJson = flSegJson.getJSONObject(JSON_PROP_MARKAIRLINE);
						mkAirlineElem.setAttribute("Code", mkAirlineJson.getString(JSON_PROP_AIRLINECODE));
						
						/*String mkAirlineFlNbr = mkAirlineJson.optString(JSON_PROP_FLIGHTNBR, "");
						if (mkAirlineFlNbr.isEmpty() == false) {
							mkAirlineElem.setAttribute("FlightNumber", mkAirlineFlNbr);
						}*/
						String mkAirlineShortName = mkAirlineJson.optString(JSON_PROP_COMPANYSHORTNAME, "");
						if (mkAirlineShortName.isEmpty() == false) {
							mkAirlineElem.setAttribute("CompanyShortName", mkAirlineShortName);
						}
						flSegElem.appendChild(mkAirlineElem);

						Element bookClsAvailsElem = ownerDoc.createElementNS(NS_OTA, "BookingClassAvails");
						bookClsAvailsElem.setAttribute("CabinType", flSegJson.getString(JSON_PROP_CABINTYPE));
						Element bookClsAvailElem = ownerDoc.createElementNS(NS_OTA, "ota:BookingClassAvail");
						bookClsAvailElem.setAttribute("ResBookDesigCode", flSegJson.getString(JSON_PROP_RESBOOKDESIG));
						bookClsAvailElem.setAttribute("RPH", flSegJson.getString(JSON_PROP_RPH));
						bookClsAvailsElem.appendChild(bookClsAvailElem);
						flSegElem.appendChild(bookClsAvailsElem);

						odoElem.appendChild(flSegElem);
					}

					odosElem.appendChild(odoElem);
				}
				String pricedItinRedisKey = AirRepriceProcessor.getRedisKeyForPricedItinerary(pricedItinJson);
				String ptcBrkDwnKey="ptcXml|".concat(pricedItinRedisKey);
				String ptcBrkDwnsXmlStr=ptcBrkDwnMap.get(ptcBrkDwnKey);
				
				
				Element ptcBrkDownsElem=(Element) ownerDoc.importNode(XMLTransformer.toXMLElement(ptcBrkDwnsXmlStr),true);
				
				
				//Element ptcBrkDownsElem=XMLTransformer.toXMLElement(ptcBrkDwnsXmlStr);
				
				Element priceInfoElem=ownerDoc.createElementNS(NS_OTA, "ota:PriceInfo");
				
				
				
				priceInfoElem.appendChild(ptcBrkDownsElem);
				
				otaReqElem.insertBefore(priceInfoElem, travelerInfoElem);
			
				JSONObject suppPriceBookInfoJson = new JSONObject(reprcSuppFaresMap.get(pricedItinRedisKey));
				JSONArray clientCommercialItinInfoArr=suppPriceBookInfoJson.getJSONArray(JSON_PROP_CLIENTCOMMITININFO);
				suppPriceBookInfoJson.remove(JSON_PROP_CLIENTCOMMITININFO);
				JSONArray clientCommercialItinTotalInfoArr=suppPriceBookInfoJson.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMS);
				suppPriceBookInfoJson.remove(JSON_PROP_CLIENTENTITYTOTALCOMMS);
				
				JSONObject airPriceItinInfo=pricedItinJson.getJSONObject(JSON_PROP_AIRPRICEINFO);
				airPriceItinInfo.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientCommercialItinTotalInfoArr);
				JSONArray paxTypeFaresJsonArr=airPriceItinInfo.getJSONArray(JSON_PROP_PAXTYPEFARES);
				
				
				Map<String,JSONArray> clientCommPaxMap=new HashMap<String,JSONArray>();
				for(int t=0; t < clientCommercialItinInfoArr.length(); t++) {
					JSONObject clientCommercialItin=new JSONObject();
					clientCommercialItin=clientCommercialItinInfoArr.getJSONObject(t);
					clientCommPaxMap.put(clientCommercialItin.get(JSON_PROP_PAXTYPE).toString(),clientCommercialItin.getJSONArray(JSON_PROP_CLIENTENTITYCOMMS));
				}
				
				for(int z=0;z<paxTypeFaresJsonArr.length();z++) {
					JSONObject paxTypeFareJson=paxTypeFaresJsonArr.getJSONObject(z);
					clientCommPaxMap.get(paxTypeFareJson.get(JSON_PROP_PAXTYPE).toString());
					paxTypeFareJson.put(JSON_PROP_CLIENTENTITYCOMMS, clientCommPaxMap.get(paxTypeFareJson.get(JSON_PROP_PAXTYPE).toString()));
				}
				
				
				if (suppID.equals(prevSuppID) == false) {
					JSONArray paxDetailsJsonArr = reqBodyJson.getJSONArray(JSON_PROP_PAXDETAILS);
					for (int i = 0; i < paxDetailsJsonArr.length(); i++) {
						JSONObject traveller = (JSONObject) paxDetailsJsonArr.get(i);
						createTraveler(ownerDoc, traveller, travelerInfoElem, i);
					}

					// int bookRefIdx = pricedItinJson.getInt("bookRefIdx");
					Element tPA_Extensions = XMLUtils.getFirstElementAtXPath(otaReqElem, "./ota:TPA_Extensions");
					//TODO:Create paymentDetails as array
				
					JSONObject paymentInfoJson = reqBodyJson.getJSONArray(JSON_PROP_PAYINFO).getJSONObject(0);
					JSONObject bookRefJson = suppPriceBookInfoJson.getJSONObject(JSON_PROP_BOOKREFS);
					ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT, suppID);
					createFulfillment(ownerDoc, otaReqElem, tPA_Extensions, paymentInfoJson, bookRefJson, prodSupplier);

					createTPA_Extensions(ownerDoc, tPA_Extensions, reqBodyJson);
				}
				pricedItinJson.put("suppInfo", suppPriceBookInfoJson);
				//TODO:complete SSR OP once standardization comes.
				//If SSR are not to be passed comment from here:
				JSONArray paxDetailsJsArr = reqBodyJson.getJSONArray("paxDetails");
				
				if(XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:SpecialReqDetails")==null) {
					 specialRqDetElem=ownerDoc.createElementNS(NS_OTA,"ota:SpecialReqDetails");
					travelerInfoElem.appendChild(specialRqDetElem);
				}
				else {
					 specialRqDetElem=XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:SpecialReqDetails");
				}
				
				
				
				//specialRqDetElem=XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:SpecialReqDetails");
				if(prevSuppID.equalsIgnoreCase(suppID)) {
					specialRqDetElem=XMLUtils.getFirstElementAtXPath(travelerInfoElem,
							"./ota:SpecialReqDetails");
				}
				else {
					specialRqDetElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialReqDetails");
					travelerInfoElem.appendChild(specialRqDetElem);
					specialRqDetElem=XMLUtils.getFirstElementAtXPath(travelerInfoElem,
							"./ota:SpecialReqDetails");
				}
				for (int i=0; i < paxDetailsJsArr.length(); i++) {
					JSONObject traveller = (JSONObject) paxDetailsJsArr.get(i);
					
					//getSSR for passengers
					if(!traveller.isNull("specialRequests"))
					{
						
						
				createSSRElem(ownerDoc, specialRqDetElem, i, traveller,flightMap);
					}
				}
				//to here
				prevSuppID = suppID;
			}

			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put("product", PRODUCT_AIR);
			// TODO: Remove hard-coded bookID
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_BOOKID, reqBodyJson.get(JSON_PROP_BOOKID).toString());
			bookProducer.runProducer(1, kafkaMsgJson);
			logger.trace(String.format("Air Book Request Kafka Message: %s", kafkaMsgJson.toString()));
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			
			JSONObject resBodyJson = new JSONObject();
			/*String bookingId = reqHdrJson.getString(JSON_PROP_SESSIONID).concat("-").concat(mDtFmt.format(Calendar.getInstance().getTime()));*/
			//resBodyJson.put(JSON_PROP_BOOKID, reqBodyJson.get(JSON_PROP_BOOKID).toString());
			JSONArray suppBooksJsonArr = new JSONArray();
			Element[] wrapprElems = XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirBookRSWrapper");
			for (Element wrapprElem : wrapprElems) {
				JSONObject suppBookJson = new JSONObject();
				suppBookJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(wrapprElem, "./air:SupplierID"));
				suppBookJson.put(JSON_PROP_BOOKREFID, XMLUtils.getValueAtXPath(wrapprElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID[@Type='14']/@ID"));
				suppBookJson.put(JSON_PROP_AIRLINEPNR, XMLUtils.getValueAtXPath(wrapprElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID[@Type='14']/@ID"));
				suppBookJson.put(JSON_PROP_GDSPNR, XMLUtils.getValueAtXPath(wrapprElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID[@Type='16']/@ID"));
				suppBookJson.put(JSON_PROP_TICKETPNR, XMLUtils.getValueAtXPath(wrapprElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID[@Type='30']/@ID"));
				suppBooksJsonArr.put(suppBookJson);
			}
			resBodyJson.put(JSON_PROP_SUPPBOOKREFS, suppBooksJsonArr);

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			kafkaMsgJson = new JSONObject();
			kafkaMsgJson=resJson;
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("product", PRODUCT_AIR);
			
			//kafkaMsgJson.put("messageType", "CONFIRM");
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_BOOKID, reqBodyJson.get(JSON_PROP_BOOKID).toString());
			bookProducer.runProducer(1, kafkaMsgJson);
			return resJson.toString();
		} catch (Exception x) {
			logger.error("Exception received while processing", x);
            return AirSearchProcessor.getEmptyResponse(reqHdrJson).toString();
		}
	}

	public static void createTPA_Extensions(Document ownerDoc, Element tPA_Extensions, JSONObject reqBodyJson) {
		Element tripType = XMLUtils.getFirstElementAtXPath(tPA_Extensions, "./air:TripType");
		tripType.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPTYPE));

		Element customerInfo = ownerDoc.createElementNS(NS_AIR, "air:CustomerInfo");
		tPA_Extensions.appendChild(customerInfo);

		/**
		 * TODO: Need for loop to fetch contact details for customer info
		 */
		JSONObject paxInfo = reqBodyJson.getJSONArray(JSON_PROP_PAXDETAILS).getJSONObject(0);

		customerInfo.setAttribute("BirthDate", paxInfo.getString(JSON_PROP_DATEOFBIRTH));

		/**
		 * TODO : Need to find path for PassengerTypeCode. Currently literal is used.
		 */
		//customerInfo.setAttribute("PassengerTypeCode", "ADT");
		customerInfo.setAttribute("PassengerTypeCode", paxInfo.getString(JSON_PROP_PAXTYPE));

		Element personName = ownerDoc.createElementNS(NS_OTA, "ota:PersonName");
		customerInfo.appendChild(personName);

		Element namePrefix = ownerDoc.createElementNS(NS_OTA, "ota:NamePrefix");
		namePrefix.setTextContent(paxInfo.getString(JSON_PROP_TITLE));
		personName.appendChild(namePrefix);

		Element givenName = ownerDoc.createElementNS(NS_OTA, "ota:GivenName");
		givenName.setTextContent(paxInfo.getString(JSON_PROP_FIRSTNAME));
		personName.appendChild(givenName);

		Element surname = ownerDoc.createElementNS(NS_OTA, "ota:Surname");
		surname.setTextContent(paxInfo.getString(JSON_PROP_SURNAME));
		personName.appendChild(surname);

		Element telephone = ownerDoc.createElementNS(NS_OTA, "ota:Telephone");
		customerInfo.appendChild(telephone);

		/**
		 * TODO: Need for loop to fetch contact details for customer info
		 */
		JSONObject contactInfo = paxInfo.getJSONArray(JSON_PROP_CONTACTDTLS).getJSONObject(0).getJSONObject(JSON_PROP_CONTACTINFO);
		telephone.setAttribute("CountryAccessCode", contactInfo.getString(JSON_PROP_COUNTRYCODE));
		telephone.setAttribute("PhoneNumber", contactInfo.getString(JSON_PROP_MOBILENBR));

		Element email = ownerDoc.createElementNS(NS_OTA, "ota:Email");
		email.setTextContent(contactInfo.getString(JSON_PROP_EMAIL));
		customerInfo.appendChild(email);

		Element address = ownerDoc.createElementNS(NS_OTA, "ota:Address");
		customerInfo.appendChild(address);

		JSONObject addressDetails = paxInfo.getJSONObject(JSON_PROP_ADDRDTLS);

		Element addressLine = ownerDoc.createElementNS(NS_OTA, "ota:AddressLine");
		addressLine.setTextContent(addressDetails.getString(JSON_PROP_ADDRLINE1) + " " + addressDetails.getString(JSON_PROP_ADDRLINE2));
		address.appendChild(addressLine);

		Element cityName = ownerDoc.createElementNS(NS_OTA, "ota:CityName");
		cityName.setTextContent(addressDetails.getString(JSON_PROP_CITY));
		address.appendChild(cityName);

		Element postalCode = ownerDoc.createElementNS(NS_OTA, "ota:PostalCode");
		postalCode.setTextContent(addressDetails.getString(JSON_PROP_ZIP));
		address.appendChild(postalCode);

		Element stateProv = ownerDoc.createElementNS(NS_OTA, "ota:StateProv");
		stateProv.setAttribute("StateCode", addressDetails.getString(JSON_PROP_STATE));
		address.appendChild(stateProv);

		Element countryName = ownerDoc.createElementNS(NS_OTA, "ota:CountryName");
		countryName.setAttribute("Code", addressDetails.getString(JSON_PROP_COUNTRY));
		address.appendChild(countryName);

		Element document = ownerDoc.createElementNS(NS_OTA, "ota:Document");
		customerInfo.appendChild(document);

		JSONArray documentInfo = paxInfo.getJSONObject(JSON_PROP_DOCDTLS).getJSONArray(JSON_PROP_DOCINFO);

		document.setAttribute("DocIssueAuthority", documentInfo.getJSONObject(0).getString(JSON_PROP_ISSUEAUTH));
		document.setAttribute("DocIssueLocation", documentInfo.getJSONObject(0).getString(JSON_PROP_ISSUELOC));
		document.setAttribute("DocID", documentInfo.getJSONObject(0).getString(JSON_PROP_DOCNBR));
		document.setAttribute("DocType", documentInfo.getJSONObject(0).getString(JSON_PROP_DOCTYPE));
		document.setAttribute("DocHolderNationality", documentInfo.getJSONObject(0).getString(JSON_PROP_NATIONALITY));
		document.setAttribute("Gender", paxInfo.getString(JSON_PROP_GENDER));
		document.setAttribute("BirthDate", paxInfo.getString(JSON_PROP_DATEOFBIRTH));
		document.setAttribute("EffectiveDate", documentInfo.getJSONObject(0).getString(JSON_PROP_EFFDATE));
		document.setAttribute("ExpireDate", documentInfo.getJSONObject(0).getString(JSON_PROP_EXPDATE));

		Element docHolderName = ownerDoc.createElementNS(NS_OTA, "ota:DocHolderName");
		docHolderName.setTextContent(String.format("%s %s", paxInfo.getString(JSON_PROP_FIRSTNAME), paxInfo.getString(JSON_PROP_SURNAME)));
		document.appendChild(docHolderName);

	}

	public static void createFulfillment(Document ownerDoc, Element oTA_AirBookRQ, Element tPA_Extensions, JSONObject paymentInfoNode, JSONObject bookRefJson, ProductSupplier prodSupplier) {
		Element fulfillment = ownerDoc.createElementNS(NS_OTA, "ota:Fulfillment");
		oTA_AirBookRQ.appendChild(fulfillment);

		oTA_AirBookRQ.insertBefore(fulfillment, tPA_Extensions);

		Element paymentDetails = ownerDoc.createElementNS(NS_OTA, "ota:PaymentDetails");
		fulfillment.appendChild(paymentDetails);

		Element paymentDetail = prodSupplier.getPaymentDetailsElement(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT, ownerDoc);
		paymentDetails.appendChild(paymentDetail);
		
		if(prodSupplier.getSupplierID().equals("GALILEO")) {
			createGalileoPaymentDetail(ownerDoc, paymentDetail, paymentInfoNode, bookRefJson);
		}
		else {
			createPaymentDetail(ownerDoc, paymentDetail, paymentInfoNode, bookRefJson);
		}
		
	
	}

	private static void createGalileoPaymentDetail(Document ownerDoc, Element paymentDetail, JSONObject paymentInfoNode,
			JSONObject bookRefJson) {
		// TODO:Remove this method later; only done for testing purposes
		paymentDetail.setAttribute("PaymentType", "5");
		paymentDetail.setAttribute("RPH", "1");
		
		Element paymentCard = ownerDoc.createElementNS(NS_OTA, "ota:PaymentCard");
		paymentCard.setAttribute("RPH", "1");
		paymentCard.setAttribute("ExpireDate", "0119");
		
		Element cardTypeElem = ownerDoc.createElementNS(NS_OTA, "ota:CardType");
		cardTypeElem.setTextContent("VISA");
		paymentCard.appendChild(cardTypeElem);
		
		Element cardHolderName=ownerDoc.createElementNS(NS_OTA, "ota:CardHolderName");
		cardHolderName.setTextContent("JOHN SMITH");
		paymentCard.appendChild(cardHolderName);
		
		Element cardNumber=ownerDoc.createElementNS(NS_OTA, "ota:CardNumber");
		cardNumber.setAttribute("EncryptionKey", "595");
		cardNumber.setAttribute("Token", "4895390000000013");
		paymentCard.appendChild(cardNumber);
		
		paymentDetail.appendChild(paymentCard);
		
		Element paymentAmount = ownerDoc.createElementNS(NS_OTA, "ota:PaymentAmount");
		paymentDetail.appendChild(paymentAmount);

		JSONObject suppBookFareJson = bookRefJson.getJSONObject(JSON_PROP_SUPPBOOKFARE);
		paymentAmount.setAttribute(XML_ATTR_CURRENCYCODE, suppBookFareJson.getString(JSON_PROP_CCYCODE));
		paymentAmount.setAttribute(XML_ATTR_AMOUNT, suppBookFareJson.getBigDecimal(JSON_PROP_AMOUNT).toString());
		
	}

	private static void createPaymentDetail(Document ownerDoc, Element paymentDetail, JSONObject paymentInfoNode, JSONObject bookRefJson) {
		Element paymentAmount = ownerDoc.createElementNS(NS_OTA, "ota:PaymentAmount");
		paymentDetail.appendChild(paymentAmount);

		JSONObject suppBookFareJson = bookRefJson.getJSONObject(JSON_PROP_SUPPBOOKFARE);
		paymentAmount.setAttribute(XML_ATTR_CURRENCYCODE, suppBookFareJson.getString(JSON_PROP_CCYCODE));
		paymentAmount.setAttribute(XML_ATTR_AMOUNT, suppBookFareJson.getBigDecimal(JSON_PROP_AMOUNT).toString());
	}

	private static void createTraveler(Document ownerDoc, JSONObject paxInfo, Element travelerInfo, int i) {

		Element airTraveler = ownerDoc.createElementNS(NS_OTA, "ota:AirTraveler");
		travelerInfo.appendChild(airTraveler);

		String dataStr;

		dataStr = paxInfo.get(JSON_PROP_GENDER).toString();
		dataStr = dataStr.replace("\"", "");
		airTraveler.setAttribute("Gender", dataStr);

		String dob = paxInfo.get(JSON_PROP_DATEOFBIRTH).toString();
		if (dob != null && dob.toString().trim().length() > 0) {
			dataStr = dob.toString();
			dataStr = dataStr.replace("\"", "");
			airTraveler.setAttribute("BirthDate", dataStr);
		}


		dataStr = paxInfo.get(JSON_PROP_PAXTYPE).toString();
		dataStr = dataStr.replace("\"", "");
		airTraveler.setAttribute("PassengerTypeCode", dataStr);


		Element PersonName = ownerDoc.createElementNS(NS_OTA, "ota:PersonName");
		airTraveler.appendChild(PersonName);

		Element NamePrefix = ownerDoc.createElementNS(NS_OTA, "ota:NamePrefix");
		PersonName.appendChild(NamePrefix);

		NamePrefix.setTextContent(paxInfo.get(JSON_PROP_TITLE).toString().replace("\"", ""));

		Element GivenName = ownerDoc.createElementNS(NS_OTA, "ota:GivenName");
		PersonName.appendChild(GivenName);
		GivenName.setTextContent(paxInfo.getString(JSON_PROP_FIRSTNAME).replace("\"", ""));

		Element Surname = ownerDoc.createElementNS(NS_OTA, "ota:Surname");
		PersonName.appendChild(Surname);
		Surname.setTextContent(paxInfo.getString(JSON_PROP_SURNAME).replace("\"", ""));


		int contactLength = paxInfo.getJSONArray(JSON_PROP_CONTACTDTLS).length();
		for (int l = 0; l < contactLength; l++) {
			JSONObject contactInfo = paxInfo.getJSONArray(JSON_PROP_CONTACTDTLS).getJSONObject(l).getJSONObject(JSON_PROP_CONTACTINFO);
			createTelephoneEmailDetails(ownerDoc, airTraveler, contactInfo);
		}

		// Address

		Element Address = ownerDoc.createElementNS(NS_OTA, "ota:Address");
		airTraveler.appendChild(Address);

		createAddress(ownerDoc, airTraveler, Address, paxInfo);

		// For minors documentDetails information might not be available
		JSONObject documentDetails = paxInfo.optJSONObject(JSON_PROP_DOCDTLS);
		if (documentDetails != null) {
			JSONArray documentInfoJsonArr = documentDetails.getJSONArray(JSON_PROP_DOCINFO);
			for (int l = 0; l < documentInfoJsonArr.length(); l++) {
				JSONObject documentInfoJson = documentInfoJsonArr.getJSONObject(l);
				if (documentInfoJson != null && documentInfoJson.toString().trim().length() > 0)
					createDocuments(ownerDoc, airTraveler, documentInfoJson, paxInfo);
			}
		}

		Element travelerRefNumber = ownerDoc.createElementNS(NS_OTA, "ota:TravelerRefNumber");
		airTraveler.appendChild(travelerRefNumber);

		travelerRefNumber.setAttribute("RPH", String.valueOf(i));
	}

	private static void createDocuments(Document ownerDoc, Element airTraveler, JSONObject documentInfo,
			JSONObject paxInfo) {

		Element document = ownerDoc.createElementNS(NS_OTA, "ota:Document");
		airTraveler.appendChild(document);

		createDocumentAttr(ownerDoc, document, documentInfo, "DocIssueAuthority", JSON_PROP_ISSUEAUTH);
		createDocumentAttr(ownerDoc, document, documentInfo, "DocIssueLocation", JSON_PROP_ISSUELOC);
		createDocumentAttr(ownerDoc, document, documentInfo, "DocHolderNationality", JSON_PROP_NATIONALITY);
		createDocumentAttr(ownerDoc, document, paxInfo, "Gender", JSON_PROP_GENDER);
		createDocumentAttr(ownerDoc, document, paxInfo, "BirthDate", JSON_PROP_DATEOFBIRTH);
		createDocumentAttr(ownerDoc, document, documentInfo, "EffectiveDate", JSON_PROP_EFFDATE);
		createDocumentAttr(ownerDoc, document, documentInfo, "ExpireDate", JSON_PROP_EXPDATE);
		createDocumentAttr(ownerDoc, document, documentInfo, "DocID", JSON_PROP_DOCNBR);
		createDocumentAttr(ownerDoc, document, documentInfo, "DocType", JSON_PROP_DOCTYPE);

		Element docHolderName = ownerDoc.createElementNS(NS_OTA, "ota:DocHolderName");
		document.appendChild(docHolderName);
		if (paxInfo.get(JSON_PROP_FIRSTNAME) != null && paxInfo.get(JSON_PROP_FIRSTNAME).toString().trim().length() > 0) {
			if (paxInfo.get(JSON_PROP_SURNAME) != null && paxInfo.get(JSON_PROP_SURNAME).toString().trim().length() > 0) {
				String firstName = paxInfo.get(JSON_PROP_FIRSTNAME).toString();
				String surname = paxInfo.get(JSON_PROP_SURNAME).toString();
				String name = firstName.toString() + " " + surname.toString();
				docHolderName.setTextContent(name);
			}
		}

	}

	private static void createDocumentAttr(Document ownerDoc, Element document, JSONObject documentInfo, String attrStr, String attrValue) {
		String dataStr = documentInfo.get(attrValue).toString();
		if (dataStr != null && dataStr.trim().isEmpty() == false) {
			document.setAttribute(attrStr, dataStr);
		}
	}

	private static void createAddress(Document ownerDoc, Element airTraveler, Element address, JSONObject paxInfo) {

		Element StreetNmbr = ownerDoc.createElementNS(NS_OTA, "ota:StreetNmbr");
		address.appendChild(StreetNmbr);

		JSONObject addressDetails = paxInfo.getJSONObject(JSON_PROP_ADDRDTLS);
		if (addressDetails != null) {
			String addrLine2 = addressDetails.get(JSON_PROP_ADDRLINE2).toString();
			if (addrLine2 != null && addrLine2.toString().trim().length() > 0) {
				StreetNmbr.setTextContent(addrLine2.toString().replace("\"", ""));
			}

			Element BldgRoom = ownerDoc.createElementNS(NS_OTA, "ota:BldgRoom");
			address.appendChild(BldgRoom);

			String addrLine1 = addressDetails.get(JSON_PROP_ADDRLINE1).toString();
			if (addrLine1 != null && addrLine1.toString().trim().length() > 0) {
				BldgRoom.setTextContent(addrLine1.toString().replace("\"", ""));
			}

			BldgRoom.setAttribute("BldgNameIndicator", "false");

			Element AddressLine = ownerDoc.createElementNS(NS_OTA, "ota:AddressLine");
			address.appendChild(AddressLine);

			String addrLine = addressDetails.get(JSON_PROP_ADDRLINE2).toString();
			if (addrLine != null && addrLine.toString().trim().length() > 0) {
				AddressLine.setTextContent(addrLine.toString().replace("\"", ""));
			}

			Element CityName = ownerDoc.createElementNS(NS_OTA, "ota:CityName");
			address.appendChild(CityName);

			String city = addressDetails.get(JSON_PROP_CITY).toString();
			if (city != null && city.toString().trim().length() > 0) {
				CityName.setTextContent(city.toString().replace("\"", ""));
			}

			Element PostalCode = ownerDoc.createElementNS(NS_OTA, "ota:PostalCode");
			address.appendChild(PostalCode);

			String zip = addressDetails.get(JSON_PROP_ZIP).toString();
			if (zip != null && zip.toString().trim().length() > 0) {
				PostalCode.setTextContent(zip.toString().replace("\"", ""));
			}

			Element County = ownerDoc.createElementNS(NS_OTA, "ota:County");
			address.appendChild(County);
			String country = addressDetails.get(JSON_PROP_COUNTRY).toString();
			if (country != null && country.toString().trim().length() > 0) {
				County.setTextContent(country.toString().replace("\"", ""));
			}

			Element StateProv = ownerDoc.createElementNS(NS_OTA, "ota:StateProv");
			address.appendChild(StateProv);
			String state = addressDetails.get(JSON_PROP_STATE).toString();
			if (state != null && state.toString().trim().length() > 0) {
				StateProv.setTextContent(state.toString().replace("\"", ""));
			}

			Element CountryName = ownerDoc.createElementNS(NS_OTA, "ota:CountryName");
			address.appendChild(CountryName);
			CountryName.setAttribute("Code", addressDetails.getString(JSON_PROP_COUNTRY));
		}

	}

private	static void createSSRElem(Document ownerDoc, Element specReqDetails, int travellerRPH, JSONObject traveller, Map<String, String> flightMap) {
		//Element ssrElem=ownerDoc.createElementNS(NS_AIR, "air:SSR");

Element specialServiceRequestsElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequests");


JSONArray specialServiceRequests=traveller.getJSONObject("specialRequests").getJSONArray("specialRequestInfo");



for(int s=0;s<specialServiceRequests.length();s++)
{


JSONObject specialServiceRequestJson=specialServiceRequests.getJSONObject(s);

	Element specialServiceRequestElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequest");
	
specialServiceRequestElem.setAttribute("TravelerRefNumberRPHList", Integer.toString(travellerRPH));

specialServiceRequestElem.setAttribute("FlightRefNumberRPHList", specialServiceRequestJson.optString(JSON_PROP_FLIGHREFNBR,""));

specialServiceRequestElem.setAttribute("SSRCode", specialServiceRequestJson.optString(JSON_PROP_SSRCODE, ""));

specialServiceRequestElem.setAttribute("ServiceQuantity", specialServiceRequestJson.optString(JSON_PROP_SVCQTY,""));

specialServiceRequestElem.setAttribute("Type", specialServiceRequestJson.optString(JSON_PROP_TYPE,""));

specialServiceRequestElem.setAttribute("Status", specialServiceRequestJson.optString(JSON_PROP_STATUS,""));


if(specialServiceRequestJson.has(JSON_PROP_NUMBER) && !specialServiceRequestJson.get(JSON_PROP_NUMBER).equals("")){
	//TODO:Confirm the default value for Number or remove this element if SI removes it.
	specialServiceRequestElem.setAttribute("Number", specialServiceRequestJson.optString(JSON_PROP_NUMBER,"0"));
}
//TODO: Add/Remove/Alter below Elems and more once standardization is confirmed.
Element airlineElem=ownerDoc.createElementNS(NS_OTA, "ota:Airline");

airlineElem.setAttribute("CompanyShortName", specialServiceRequestJson.optString((JSON_PROP_COMPANYSHORTNAME),""));

airlineElem.setAttribute("Code", specialServiceRequestJson.optString((JSON_PROP_AIRLINECODE),""));

specialServiceRequestElem.appendChild(airlineElem);


Element textElem=ownerDoc.createElementNS(NS_OTA, "ota:Text");

textElem.setTextContent(specialServiceRequestJson.optString((JSON_PROP_DESC),""));

specialServiceRequestElem.appendChild(textElem);


Element flightLegElem=ownerDoc.createElementNS(NS_OTA, "ota:FlightLeg");

flightLegElem.setAttribute("FlightNumber", specialServiceRequestJson.getString((JSON_PROP_FLIGHTNBR)));

flightLegElem.setAttribute("ResBookDesigCode",flightMap.get((specialServiceRequestJson.getString((JSON_PROP_FLIGHTNBR))).replaceAll("\\s","")).toString());

specialServiceRequestElem.appendChild(flightLegElem);

Element categoryCodeElem=ownerDoc.createElementNS(NS_OTA, "ota:CategoryCode");

categoryCodeElem.setTextContent(specialServiceRequestJson.optString((JSON_PROP_CATCODE),""));

specialServiceRequestElem.appendChild(categoryCodeElem);

Element travelerRefElem=ownerDoc.createElementNS(NS_OTA,"ota:TravelerRef");

travelerRefElem.setTextContent(traveller.getString(JSON_PROP_PAXTYPE));

specialServiceRequestElem.appendChild(travelerRefElem);


if(specialServiceRequestJson.has(JSON_PROP_AMOUNT))
{
Element servicePriceElem=ownerDoc.createElementNS(NS_OTA,"ota:ServicePrice");


servicePriceElem.setAttribute("Total", specialServiceRequestJson.getString(JSON_PROP_AMOUNT));
servicePriceElem.setAttribute("CurrencyCode", specialServiceRequestJson.getString(JSON_PROP_CCYCODE));

if(specialServiceRequestJson.has(JSON_PROP_SVCPRC))
{
	Element basePriceElem=ownerDoc.createElementNS(NS_OTA,"ota:BasePrice");

	basePriceElem.setAttribute("Amount",specialServiceRequestJson.getJSONObject(JSON_PROP_SVCPRC).getString(JSON_PROP_BASEPRICE));

	servicePriceElem.appendChild(basePriceElem);
}

if(specialServiceRequestJson.has(JSON_PROP_TAXES))
{
	Element taxesElem=ownerDoc.createElementNS(NS_OTA,"ota:Taxes");

	taxesElem.setAttribute("Amount",specialServiceRequestJson.getJSONObject(JSON_PROP_TAXES).getString(JSON_PROP_AMOUNT));

	servicePriceElem.appendChild(taxesElem);
}



specialServiceRequestElem.appendChild(servicePriceElem);
}

specialServiceRequestsElem.appendChild(specialServiceRequestElem);



specReqDetails.appendChild(specialServiceRequestsElem);


}

	}


	private static void createTelephoneEmailDetails(Document ownerDoc, Element airTraveler, JSONObject contactInfo) {

		Element Telephone = ownerDoc.createElementNS(NS_OTA, "ota:Telephone");
		airTraveler.appendChild(Telephone);

		String dataStr;

		String countryCode = contactInfo.get(JSON_PROP_COUNTRYCODE).toString();
		if (countryCode != null && countryCode.toString().trim().length() > 0) {
			dataStr = countryCode.toString();
			dataStr = dataStr.replace("\"", "");
			Telephone.setAttribute("CountryAccessCode", dataStr);
		}

		String mobileNo = contactInfo.get(JSON_PROP_MOBILENBR).toString();
		if (countryCode != null && countryCode.toString().trim().length() > 0) {
			dataStr = mobileNo.toString();
			dataStr = dataStr.replace("\"", "");
			Telephone.setAttribute("PhoneNumber", dataStr);
		}

		Element Email = ownerDoc.createElementNS(NS_OTA, "ota:Email");
		airTraveler.appendChild(Email);
		String email = contactInfo.get(JSON_PROP_EMAIL).toString();
		if (email != null && email.toString().trim().length() > 0)
			Email.setTextContent(email.toString().replace("\"", ""));

	}

}
