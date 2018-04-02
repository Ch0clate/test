package com.coxandkings.travel.bookingengine.orchestrator.car;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.car.CarConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.orchestrator.car.CarBookProcessor;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class CarBookProcessor implements CarConstants{
	
	@Autowired
	private static final Logger logger = LogManager.getLogger(CarBookProcessor.class);
	private static final DateFormat mDtFmt = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	public static String process(JSONObject reqJson) {
		try {
			KafkaBookProducer bookProducer = new KafkaBookProducer();
			OperationConfig opConfig = CarConfig.getOperationConfig("book");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cari:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./car:OTA_VehResRQWrapper");
			reqBodyElem.removeChild(wrapperElem);

			TrackingContext.setTrackingContext(reqJson);
			JSONObject kafkaMsgJson = reqJson;
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);

			CarSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
			
			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			String redisKey = String.format("%s%c%s", reqHdrJson.optString(JSON_PROP_SESSIONID), KEYSEPARATOR, PRODUCT_CAR);
			Map<String, String> reprcSuppFaresMap = redisConn.hgetAll(redisKey);
			if (reprcSuppFaresMap == null) {
				throw new Exception(String.format("Reprice context not found,for %s", redisKey));
			}
			
			JSONArray carRentalInfoArr = reqBodyJson.getJSONArray(JSON_PROP_CARRENTALARR);
			for (int y = 0; y < carRentalInfoArr.length(); y++) {
				JSONObject bookReq = carRentalInfoArr.getJSONObject(y);
				Element suppWrapperElem = null;
				JSONObject totalPricingInfo = bookReq.getJSONObject(JSON_PROP_TOTALPRICEINFO);
				String vehicleKey = CarPriceProcessor.getRedisKeyForVehicleAvail(bookReq);
				//Appending Commercials and Fares in KafkaBookReq For Database Population
				JSONObject suppPriceBookInfoJson = new JSONObject(reprcSuppFaresMap.get(vehicleKey));
				JSONObject suppPricingInfo = suppPriceBookInfoJson.getJSONObject(JSON_PROP_SUPPPRICEINFO);
				bookReq.put(JSON_PROP_SUPPPRICEINFO, suppPricingInfo);
				JSONArray clientCommercialItinTotalInfoArr = suppPriceBookInfoJson.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMS);
				totalPricingInfo.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientCommercialItinTotalInfoArr);
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				XMLUtils.setValueAtXPath(suppWrapperElem, "./car:Sequence", String.valueOf(y));
				reqBodyElem.appendChild(suppWrapperElem);
				populateWrapperElement(reqElem, ownerDoc, suppWrapperElem, usrCtx, bookReq, suppPricingInfo.getJSONObject(JSON_PROP_TOTALFARE));
			}
	
	        
			 kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_PROD, PROD_CATEG_SUBTYPE_CAR);
			bookProducer.runProducer(1, kafkaMsgJson);
			
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(), CarConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			
	        
			// Element resBodyElem = XMLUtils.getFirstElementAtXPath(resElem,
			// "./airi:ResponseBody/air:OTA_AirPriceRSWrapper/ota:OTA_AirPriceRS");
			// JSONObject resBodyJson =
			// AirSearchProcessor.getSupplierResponseJSON(resBodyElem);
	        int sequence = 0;
			String sequence_str;
            JSONArray reservationJsonArr = new JSONArray();
            Element[] resWrapperElems = XMLUtils.getElementsAtXPath(resElem,"./cari:ResponseBody/car:OTA_VehResRSWrapper");
			for (Element resWrapperElem : resWrapperElems) {
				
				JSONObject reservationJson = new JSONObject();
				reservationJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(resWrapperElem, "./car:SupplierID"));
				Element vehResCoreElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_VehResRS/ota:VehResRSCore");
				getSupplierBookResponseJSON(vehResCoreElem, reservationJson);
				sequence = (sequence_str = XMLUtils.getValueAtXPath(resWrapperElem, "./car:Sequence")).isEmpty()? sequence: Integer.parseInt(sequence_str);
				reservationJsonArr.put(sequence, reservationJson);
			
			}
			JSONObject resBodyJson = new JSONObject();
			resBodyJson.put("reservation", reservationJsonArr);

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			
	        kafkaMsgJson = new JSONObject(resJson.toString());
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_PROD, reqBodyJson.getString(JSON_PROP_PROD));
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_BOOKID, reqBodyJson.getString(JSON_PROP_BOOKID));
			
			kafkaMsgJson.getJSONObject(JSON_PROP_RESHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).put("clientIATANumber", usrCtx.getClientIATANUmber());
			bookProducer.runProducer(1, kafkaMsgJson);
			return resJson.toString();
			
		}catch(Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	private static void getSupplierBookResponseJSON(Element vehResCoreElem, JSONObject reservationJson) {
		
		Element reservationElem = XMLUtils.getFirstElementAtXPath(vehResCoreElem, "ota:VehReservation");
		
		reservationJson.put("creatorID", XMLUtils.getValueAtXPath(reservationElem, "./@CreatorID"));
		reservationJson.put("lastModifyDateTime", XMLUtils.getValueAtXPath(reservationElem, "./@LastModifyDateTime"));
		reservationJson.put("createDateTime", XMLUtils.getValueAtXPath(reservationElem, "./@CreateDateTime"));
		reservationJson.put("status", XMLUtils.getValueAtXPath(reservationElem, "./@ReservationStatus"));
		
		/*reservationJson.put(JSON_PROP_CUSTOMER, getCustomerJSONArray(XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:Customer")));
		Element rateDistElems[] = XMLUtils.getElementsAtXPath(reservationElem, "./ota:VehSegmentCore/ota:RentalRate/ota:RateDistance");
		reservationJson.put(JSON_PROP_RATEDISTANCE, CarSearchProcessor.getRateDistanceJSON(rateDistElems));
		Element rateQualifierElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentCore/ota:RentalRate/ota:RateQualifier");
		reservationJson.put(JSON_PROP_RATEQUALIFIER, CarSearchProcessor.getRateQualifierJSON(rateQualifierElem));
		Element referenceElem = XMLUtils.getFirstElementAtXPath(reservationElem, "/ota:VehSegmentCore/ota:Reference");
		reservationJson.put(JSON_PROP_REFERENCE, CarSearchProcessor.getReferenceJSON(referenceElem));
		Element locationDetailsElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentInfo/ota:LocationDetails");
		reservationJson.put(JSON_PROP_LOCATIONDETAIL, CarSearchProcessor.getLocationDetailsJSON(locationDetailsElem));
		Element rentalPaymentAmtElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentInfo/ota:RentalPaymentAmount");
		reservationJson.put("RentalPaymentPref", getRentalPaymentJSON(rentalPaymentAmtElem));
		Element totalChrgElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentInfo/ota:TotalCharge");
		JSONObject totalChrgJson = new JSONObject();
		totalChrgJson.put(JSON_PROP_ESTIMATEDTOTALAMOUNT, Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(totalChrgElem, "./@EstimatedTotalAmount"),0));
		totalChrgJson.put(JSON_PROP_RATETOTALAMOUNT, Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(totalChrgElem, "./@RateTotalAmount"),0));
		totalChrgJson.put(JSON_PROP_CURRENCYCODE, XMLUtils.getValueAtXPath(totalChrgElem, "./@CurrencyCode"));
		reservationJson.put(JSON_PROP_TOTALCHARGE, totalChrgJson);
		*/
//		reservationJson.put("VehSegment", getVehSegmentJSON(XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentCore")));
		
		Element vehSegCoreElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentCore");
		Element vehSegInfoElem =  XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentInfo");
		reservationJson.put(JSON_PROP_SUPPBOOKREF, getSuppBookReferences(vehSegCoreElem));
		reservationJson.put(JSON_PROP_SUPPPRICEINFO, CarSearchProcessor.getPricingInfoJSON(vehSegCoreElem, vehSegInfoElem));
	}
	
	private static JSONArray getSuppBookReferences(Element vehSegmentElem) {
		JSONArray confIdJsonArr = new JSONArray();
		Element confIdsElem[] = XMLUtils.getElementsAtXPath(vehSegmentElem, "./ota:ConfID");
		for(Element confIdElem:confIdsElem) {
			JSONObject confIdJson = new JSONObject();
			confIdJson.put("id", XMLUtils.getValueAtXPath(confIdElem, "./@ID"));
			confIdJson.put("type", XMLUtils.getValueAtXPath(confIdElem, "./@Type"));
			confIdJsonArr.put(confIdJson);
		}
		return confIdJsonArr;
	}
	
	private static JSONObject getVehSegmentJSON(Element vehSegmentElem) {
		
		JSONObject vehSegmentJson= new JSONObject();
		JSONArray confIdJsonArr = new JSONArray();
		Element confIdsElem[] = XMLUtils.getElementsAtXPath(vehSegmentElem, "./ota:ConfID");
		for(Element confIdElem:confIdsElem) {
			JSONObject confIdJson = new JSONObject();
			confIdJson.put("Id", XMLUtils.getValueAtXPath(confIdElem, "./@ID"));
			confIdJson.put("Type", XMLUtils.getValueAtXPath(confIdElem, "./@Type"));
			confIdJson.put("Status", XMLUtils.getValueAtXPath(confIdElem, "./@Status"));
			confIdJson.put("Url", XMLUtils.getValueAtXPath(confIdElem, "./@URL"));
			confIdJson.put("ID_Context", XMLUtils.getValueAtXPath(confIdElem, "./@ID_Context"));
			confIdJsonArr.put(confIdJson);
		}
		
		vehSegmentJson.put("ConfID", confIdJsonArr);
		vehSegmentJson.put("VendorCode", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@Code"));
		vehSegmentJson.put("VendorCompanyShortName", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@CompanyShortName"));
		vehSegmentJson.put("VendorTravelSector", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@TravelSector"));
		vehSegmentJson.put("VendorCodeContext", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@CodeContext"));
		
		Element vehRentalCoreElem = XMLUtils.getFirstElementAtXPath(vehSegmentElem, "./ota:VehRentalCore");
		vehSegmentJson.put(JSON_PROP_PICKUPDATE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./@PickUpDateTime"));
		vehSegmentJson.put(JSON_PROP_RETURNDATE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./@ReturnDateTime"));
		//TODO : Put OneWayIndicator as a Boolean Value
		String oneWayIndc = XMLUtils.getValueAtXPath(vehRentalCoreElem, "./@OneWayIndicator");
		
		vehSegmentJson.put(JSON_PROP_TRIPTYPE, !oneWayIndc.isEmpty() ? (oneWayIndc.equals("true") ? "OneWay" : "Return") : "");
		vehSegmentJson.put(JSON_PROP_PICKUPLOCCODE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:PickUpLocation/@LocationCode"));
		vehSegmentJson.put(JSON_PROP_RETURNLOCCODE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:ReturnLocation/@LocationCode"));
		vehSegmentJson.put(JSON_PROP_PICKUPLOCCODECONTXT, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:PickUpLocation/@CodeContext"));
		vehSegmentJson.put(JSON_PROP_RETURNLOCCODECONTXT, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:ReturnLocation/@CodeContext"));
		vehSegmentJson.put(JSON_PROP_PICKUPLOCNAME, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:PickUpLocation/@Name"));
		vehSegmentJson.put(JSON_PROP_RETURNLOCNAME, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:ReturnLocation/@Name"));
		vehSegmentJson.put(JSON_PROP_PICKUPLOCEXTCODE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:PickUpLocation/@ExtendedLocationCode"));
		vehSegmentJson.put(JSON_PROP_RETURNLOCEXTCODE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./ota:ReturnLocation/@ExtendedLocationCode"));
		
		return vehSegmentJson;
	}
	
	private static JSONObject getRentalPaymentJSON(Element rentalPaymentAmtElem) {
		
		JSONObject rentalPaymentJson = new JSONObject();
		rentalPaymentJson.put("paymentType", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./@PaymentType"));
		rentalPaymentJson.put("type", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./@Type"));
		rentalPaymentJson.put("paymentCardCode", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentCard/@Code"));
		rentalPaymentJson.put("paymentCardExpiryDate", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentCard/@ExpireDate"));
		rentalPaymentJson.put("cardType", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentCard/ota:CardType"));
		rentalPaymentJson.put("cardHolderName", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentCard/ota:CardHolderName"));
		rentalPaymentJson.put("cardNumber", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentCard/ota:CardNumber"));
		rentalPaymentJson.put("amount", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentAmount/@Amount"));
		rentalPaymentJson.put("currencyCode", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentAmount/@CurrencyCode"));
		rentalPaymentJson.put("seriesCode", XMLUtils.getValueAtXPath(rentalPaymentAmtElem, "./ota:PaymentCard/ota:SeriesCode/ota:PlainText"));
		
		return rentalPaymentJson;
	}
	
	private static JSONArray getCustomerJSONArray(Element customerElem) {
		
		JSONArray customerJsonArr = new JSONArray();
		
		Element primaryElem = XMLUtils.getFirstElementAtXPath(customerElem, "./ota:Primary");	
		customerJsonArr.put(getCustomerJSON(primaryElem, true));
		Element additionalElems[] = XMLUtils.getElementsAtXPath(customerElem, "./ota:Additional");
		for(Element additionalElem : additionalElems) {
			customerJsonArr.put(getCustomerJSON(additionalElem, false));
		}
		return customerJsonArr;
	}
	
	private static JSONObject getCustomerJSON(Element elem, Boolean isLead) {
		
		JSONObject customerJson = new JSONObject();
		
		customerJson.put("customerId", XMLUtils.getValueAtXPath(elem, "./ota:CustomerID/@ID"));
		customerJson.put("custLoyaltyMembershipID", XMLUtils.getValueAtXPath(elem, "./ota:CustLoyalty/@MembershipID"));
		customerJson.put(JSON_PROP_TITLE, XMLUtils.getValueAtXPath(elem, "./ota:NamePrefix"));
		customerJson.put(JSON_PROP_GENDER, XMLUtils.getValueAtXPath(elem, "./@Gender"));
		customerJson.put(JSON_PROP_ISLEAD, isLead);
		customerJson.put(JSON_PROP_FIRSTNAME, XMLUtils.getValueAtXPath(elem, "./ota:GivenName"));
		customerJson.put(JSON_PROP_SURNAME, XMLUtils.getValueAtXPath(elem, "./ota:Surname"));
		customerJson.put(JSON_PROP_AREACITYCODE, XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:StateProv/@StateCode"));
		customerJson.put(JSON_PROP_MOBILENBR, XMLUtils.getValueAtXPath(elem, "./ota:Telephone/@PhoneNumber"));
		customerJson.put(JSON_PROP_EMAIL, XMLUtils.getValueAtXPath(elem, "./ota:Email"));
		String addressLine = XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:AddressLine[1]");
		customerJson.put(JSON_PROP_ADDRLINE1, addressLine);
		addressLine = XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:AddressLine[2]");
		customerJson.put(JSON_PROP_ADDRLINE2, addressLine);
		customerJson.put(JSON_PROP_CITY, XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:CityName"));
		customerJson.put(JSON_PROP_ZIP, XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:PostalCode"));
		customerJson.put(JSON_PROP_COUNTRY, XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:CountryName"));
		customerJson.put(JSON_PROP_COUNTRYCODE, XMLUtils.getValueAtXPath(elem, "./ota:Address/ota:CountryName/@Code"));
		
		return customerJson;
		
	}
	
	private static void populateWrapperElement(Element reqElem, Document ownerDoc, Element suppWrapperElem, UserContext usrCtx, 
			JSONObject vehicleAvailJson, JSONObject suppTotalFare) throws Exception {
		
		String suppID = vehicleAvailJson.getString(JSON_PROP_SUPPREF);
		
		ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_CAR, suppID);
		if (prodSupplier == null) {
			throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
		}

		Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cari:RequestHeader/com:SupplierCredentialsList");
		Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem, String.format("./com:SupplierCredentials[com:SupplierID = '%s']", suppID));
		if (suppCredsElem == null) {
			suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
		}

		Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./car:SupplierID");
		suppIDElem.setTextContent(suppID);
		
		 Element vehResCoreElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehResRQ/ota:VehResRQCore");
		 Element vehResInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehResRQ/ota:VehResRQInfo");
		 
		 Element vehRentalElem =  CarSearchProcessor.getVehRentalCoreElement(ownerDoc,vehicleAvailJson);
		 vehResCoreElem.appendChild(vehRentalElem);
		 
		 JSONArray customerJsonArr = vehicleAvailJson.optJSONArray(JSON_PROP_PAXDETAILS);
		 if(customerJsonArr!=null && customerJsonArr.length()!=0) {
			 
			 Element customerElem =  CarSearchProcessor.populateCustomerElement(ownerDoc, customerJsonArr);
			 vehResCoreElem.appendChild(customerElem);
		 }
		 
		 if(vehicleAvailJson.optString("vendorPrefCode")!=null && !vehicleAvailJson.optString("vendorPrefCode").equals("")) {
			 Element vendorPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VendorPref");
			 vendorPrefElem.setAttribute("Code", vehicleAvailJson.getString("vendorPrefCode"));
			 vehResCoreElem.appendChild(vendorPrefElem);
		 }
		 
		 JSONObject vehPrefJson = vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO);
		 
		 Element VehPrefElem = XMLUtils.getFirstElementAtXPath(vehResCoreElem, "./ota:VehPref");
		 String temp;
		 if(!(temp = vehPrefJson.optString(JSON_PROP_CODECONTEXT)).isEmpty())
			 VehPrefElem.setAttribute("CodeContext", temp);
		 if(!(temp = vehPrefJson.optString("airConditionInd")).isEmpty())
			 VehPrefElem.setAttribute("AirConditionInd", temp);
		 if(!(temp = vehPrefJson.optString("transmissionType")).isEmpty())
			 VehPrefElem.setAttribute("TransmissionType", temp);
		 if(!(temp = vehPrefJson.optString("driveType")).isEmpty())
		 	VehPrefElem.setAttribute("DriveType", temp);
		 if(!(temp = vehPrefJson.optString("fuelType")).isEmpty())
			 VehPrefElem.setAttribute("FuelType", temp);
		 if(!(temp = vehPrefJson.optString("vehicleQty")).isEmpty())
			 VehPrefElem.setAttribute("VehicleQty", temp);
		 if(!(temp = vehPrefJson.optString("code")).isEmpty())
			 VehPrefElem.setAttribute("Code", temp);
		 
		 Element vehMakeModelElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehMakeModel");
		 if(!vehPrefJson.optString("vehMakeModelName").isEmpty())
		 vehMakeModelElem.setAttribute("Name", vehPrefJson.optString("vehMakeModelName"));
		 vehMakeModelElem.setAttribute("Code", vehPrefJson.optString("vehMakeModelCode"));
		 VehPrefElem.appendChild(vehMakeModelElem);
		 
		 Element vehClassElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehClass");
		 vehClassElem.setAttribute("Size", vehPrefJson.optString("vehicleClassSize"));
		 VehPrefElem.insertBefore(vehClassElem, vehMakeModelElem);
		 
         Element vehTypeElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehType");
         vehTypeElem.setAttribute("VehicleCategory", vehPrefJson.optString(JSON_PROP_VEHICLECATEGORY));
         VehPrefElem.insertBefore(vehTypeElem, vehClassElem);

		 vehResCoreElem.appendChild(VehPrefElem);
		 
		 Element driverAge = ownerDoc.createElementNS(Constants.NS_OTA, "ota:DriverType");
		 driverAge.setAttribute("Age", CARRENTAL_DRIVER_AGE);
		 vehResCoreElem.appendChild(driverAge);
				
		/* JSONArray feesJsonArr = suppTotalFare.getJSONObject(JSON_PROP_FEES).getJSONArray(JSON_PROP_FEE);
		 Element feeElem = null;
		 Element feesElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:Fees");	
		 for(int i=0;i<feesJsonArr.length();i++) {
			 
			 JSONObject feesJson = feesJsonArr.getJSONObject(i);
			 feeElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:Fee");
			 feeElem.setAttribute("Amount", BigDecimaltoString(feesJson, JSON_PROP_AMOUNT));
			 feeElem.setAttribute("CurrencyCode", feesJson.optString(JSON_PROP_CCYCODE));
			 if(!(temp = feesJson.optString("purpose")).isEmpty())
				 feeElem.setAttribute("Purpose", temp);
			 if(!(temp = feesJson.optString(JSON_PROP_ISINCLDINBASE)).isEmpty())
				 feeElem.setAttribute("IncludedInRate",temp);
			 if(!(temp = feesJson.optString(JSON_PROP_DESCRIPTION)).isEmpty())
				 feeElem.setAttribute("Description", temp);
			 
			 feesElem.appendChild(feeElem);
		 }
		 vehResCoreElem.appendChild(feesElem);*/
		
		 JSONArray rateDistJsonArr = vehicleAvailJson.optJSONArray(JSON_PROP_RATEDISTANCE);
		 if(rateDistJsonArr!=null) {
			 Element rateDistElem = null;
			 for(int i=0;i<rateDistJsonArr.length();i++) {
				 JSONObject rateDistJson = rateDistJsonArr.getJSONObject(i);
				 rateDistElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:RateDistance");
				 rateDistElem.setAttribute("Quantity", rateDistJson.optString(JSON_PROP_QTY));
				 rateDistElem.setAttribute("DistUnitName", rateDistJson.optString("distUnitName"));
				 rateDistElem.setAttribute("VehiclePeriodUnitName", rateDistJson.optString("vehiclePeriodUnitName"));
				 
				 vehResCoreElem.appendChild(rateDistElem);
			 }
		 }
		 
		 JSONObject rateQualifier = vehicleAvailJson.optJSONObject(JSON_PROP_RATEQUALIFIER);
		 if(rateQualifier!=null) {
			 Element rateQualifierElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:RateQualifier");
			 rateQualifierElem.setAttribute("RateQualifier", rateQualifier.getString(JSON_PROP_RATEQUALIFIER));
			 vehResCoreElem.appendChild(rateQualifierElem);
		 }
		 
		 JSONArray splEquipsArr = suppTotalFare.getJSONObject(JSON_PROP_SPLEQUIPS).optJSONArray(JSON_PROP_SPLEQUIP);
		 if(splEquipsArr!=null) {
			 
			 Element specialEquipPrefs = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPrefs");
			 Element specicalEquipPref = null;
			 for(int i=0;i<splEquipsArr.length();i++) {
				 JSONObject splEquips = splEquipsArr.getJSONObject(i);
				 specicalEquipPref = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPref");
				 specicalEquipPref.setAttribute("EquipType", splEquips.optString(JSON_PROP_EQUIPTYPE));
				 specicalEquipPref.setAttribute("Quantity", splEquips.optString(JSON_PROP_QTY));
				 specialEquipPrefs.appendChild(specicalEquipPref);
			 }
			 vehResCoreElem.appendChild(specialEquipPrefs);
		 }
		 
		 //TotalCharge Element not required for foreign Suppliers.
		 Element totalChargElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:TotalCharge");
		 totalChargElem.setAttribute("RateTotalAmount", BigDecimaltoString(suppTotalFare.getJSONObject(JSON_PROP_BASEFARE), JSON_PROP_AMOUNT));
	 	 totalChargElem.setAttribute("EstimatedTotalAmount", BigDecimaltoString(suppTotalFare, JSON_PROP_AMOUNT));
		 totalChargElem.setAttribute("CurrencyCode", suppTotalFare.optString(JSON_PROP_CCYCODE));
		 
		 //TODO : TotalCharge for Supplier taken from Redis
		/* totalChargElem.setAttribute("RateTotalAmount", suppPriceInfo.getBigDecimal(JSON_PROP_RATETOTALAMOUNT).toString());
		 totalChargElem.setAttribute("EstimatedTotalAmount", suppPriceInfo.getBigDecimal(JSON_PROP_ESTIMATEDTOTALAMOUNT).toString());*/
		 
		 vehResCoreElem.appendChild(totalChargElem);
		 
		 //TODO : Required only for Indian Suppliers 
		 if((vehicleAvailJson.optString(JSON_PROP_BOOKINGDURATION) !=null && !vehicleAvailJson.optString(JSON_PROP_BOOKINGDURATION).equals(""))
	       	   && (vehicleAvailJson.optString(JSON_PROP_BOOKINGUNIT) !=null && !vehicleAvailJson.optString(JSON_PROP_BOOKINGUNIT).equals(""))) {
	        Element Elem = CarSearchProcessor.getBookingDetailsElement(ownerDoc,vehicleAvailJson);
	        vehResCoreElem.appendChild(Elem);
	     }		 
		 
		 
         JSONObject refJson = vehicleAvailJson.getJSONObject(JSON_PROP_REFERENCE);
         Element refElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:Reference");
         if(!(temp = refJson.optString("type")).isEmpty())
        	 refElem.setAttribute("Type", temp);
         if(!(temp = refJson.optString("id")).isEmpty())
        	 refElem.setAttribute("ID", temp);
         if(!(temp = refJson.optString("url")).isEmpty())
        	 refElem.setAttribute("URL", temp);
         if(!(temp = refJson.optString("id_Context")).isEmpty())
        	 refElem.setAttribute("ID_Context", temp);
         
         vehResInfoElem.appendChild(refElem);
         
         /*Element rentalPaymentPrefElem = createRentalPaymentPrefElement(ownerDoc, vehicleAvailJson, suppTotalFare);
         vehResInfoElem.insertBefore(rentalPaymentPrefElem, refElem);*/
	}

	private static Element createRentalPaymentPrefElement(Document ownerDoc, JSONObject vehicleAvailJson, JSONObject suppTotalFare) {
		 
//		 String temp;
		 JSONObject rentalPaymentPrefJson = vehicleAvailJson.getJSONObject("rentalPaymentPref");
         Element rentalPaymentPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:RentalPaymentPref");
         rentalPaymentPrefElem.setAttribute("PaymentType", rentalPaymentPrefJson.optString("paymentType"));
         rentalPaymentPrefElem.setAttribute("Type", rentalPaymentPrefJson.optString("type"));
         
        /* Element paymentCardElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:PaymentCard"); 
         if(!(temp = rentalPaymentPrefJson.optString("PaymentCardCode")).isEmpty())
        	 paymentCardElem.setAttribute("CardCode", temp);
         if(!(temp = rentalPaymentPrefJson.optString("PaymentCardExpiryDate")).isEmpty())
        	 paymentCardElem.setAttribute("ExpireDate", temp);
         
         Element cardType = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CardType");
         cardType.setTextContent(rentalPaymentPrefJson.optString("CardType"));
         paymentCardElem.appendChild(cardType);
         Element cardHolderName = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CardHolderName");
         cardHolderName.setTextContent(rentalPaymentPrefJson.optString("CardHolderName"));
         paymentCardElem.appendChild(cardHolderName);
         Element cardNumber = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CardNumber");
         cardNumber.setTextContent(rentalPaymentPrefJson.optString("CardNumber"));
         paymentCardElem.appendChild(cardNumber);*/
         
         // Payment Amount taken from Redis which was saved in price operation 
         Element paymentAmountElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:PaymentAmount");
		 paymentAmountElem.setAttribute("Amount",  BigDecimaltoString(rentalPaymentPrefJson,JSON_PROP_AMOUNT));
         paymentAmountElem.setAttribute("CurrencyCode", rentalPaymentPrefJson.getString(JSON_PROP_CCYCODE));
         
        /* Element paymentAmountElem =ownerDoc.createElementNS(Constants.NS_OTA, "ota:PaymentAmount");
         if(!(temp = rentalPaymentPrefJson.optString("Amount")).isEmpty())
         paymentAmountElem.setAttribute("Amount", temp);
         if(!(temp = rentalPaymentPrefJson.optString("CurrencyCode")).isEmpty())
         paymentAmountElem.setAttribute("CurrencyCode", temp);*/
         
//       rentalPaymentPrefElem.appendChild(paymentCardElem);
         rentalPaymentPrefElem.appendChild(paymentAmountElem);
		return rentalPaymentPrefElem;
	}
	
	public static String BigDecimaltoString(JSONObject json, String prop) {
		
		if(json==null)
			return "";
		try {
			if(json.getBigDecimal(prop).compareTo(new BigDecimal(0)) == 0)
				return "";
			else
				return json.getBigDecimal(prop).toString();
		}
		catch(JSONException e) {
			return "";
		}
	}
	
	public static String NumbertoString(JSONObject json, String prop) {
		
		if(json==null)
			return "";
		try {
			if(json.getNumber(prop).equals(0))
				return "";
			else
				return json.getNumber(prop).toString();
		}
		catch(JSONException e) {
			return "";
		}
	}
	
}
