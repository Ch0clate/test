package com.coxandkings.travel.bookingengine.orchestrator.car;

/*import java.io.File;
import java.io.PrintWriter;*/
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.OperationsShellConfig;
import com.coxandkings.travel.bookingengine.config.car.CarConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class CarModifyProcessor implements CarConstants{

	@Autowired
	private static final Logger logger = LogManager.getLogger(CarModifyProcessor.class);

	public static String process(JSONObject reqJson) {
		
		try {
			KafkaBookProducer bookProducer = new KafkaBookProducer();
			OperationConfig opConfig = CarConfig.getOperationConfig("modify");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cari:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./car:OTA_VehModifyRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			
			JSONObject kafkaMsgJson = new JSONObject(reqJson.toString());
			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			
			CarSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
			JSONObject retrieveReq = new JSONObject(reqJson.toString());
			JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_CARRENTALARR);
			
			JSONObject retrieveRes = new JSONObject(CarRetrieveProcessor.process(retrieveReq, usrCtx));
			JSONArray retrieveResArr = retrieveRes.getJSONObject(JSON_PROP_RESBODY).optJSONArray(JSON_PROP_CARRENTALARR);
			
			for (int y = 0; y < multiReqArr.length(); y++) {
				
				JSONObject carRentalReq = multiReqArr.getJSONObject(y);
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				XMLUtils.setValueAtXPath(suppWrapperElem, "./car:Sequence", String.valueOf(y));
				reqBodyElem.appendChild(suppWrapperElem);
				
				populateModifyWrapperElement(reqElem, ownerDoc, suppWrapperElem, usrCtx, carRentalReq, retrieveResArr.getJSONObject(y));
			}
			
			/*PrintWriter pw = new PrintWriter(new File("D:\\BookingEngine\\Modify\\SIreqInterfaceXML.xml"));
	        pw.write(XMLTransformer.toString(reqElem));
	        pw.close();*/
	        
	        kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_PROD, PROD_CATEG_SUBTYPE_CAR);
			bookProducer.runProducer(1, kafkaMsgJson);
			
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(),
					CarConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			
			/*pw = new PrintWriter(new File("D:\\BookingEngine\\Modify\\SIresInterfaceXML.xml"));
	        pw.write(XMLTransformer.toString(resElem));
	        pw.close();*/

			JSONObject resBodyJson = new JSONObject();
			JSONObject vehicleAvailJson = null;
			Element[] resWrapperElems = XMLUtils.getElementsAtXPath(resElem,
					"./cari:ResponseBody/car:OTA_VehModifyRSWrapper");
			
			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			JSONArray carRentalArr = new JSONArray();
			for (Element resWrapperElem : resWrapperElems) {
				
				int sequence = Utils.convertToInt(XMLUtils.getValueAtXPath(resWrapperElem, "./car:Sequence"), 0);
				JSONObject modifyReq = reqBodyJson.getJSONArray(JSON_PROP_CARRENTALARR).getJSONObject(sequence);
				if(XMLUtils.getFirstElementAtXPath(resWrapperElem, "./car:ErrorList")!=null){	
					
            		Element errorMessage = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./car:ErrorList/com:Error/com:ErrorCode");
            		String errMsgStr = errorMessage.getTextContent().toString();
            		if(CANCEL_SI_ERR.equalsIgnoreCase(errMsgStr)){	
            			
            			logger.error("This service is not supported. Kindly contact our operations team for support.");
            			callOperationTodo(reqBodyJson, resJson, modifyReq);
            			return getSIErrorResponse(resJson).toString();
            		}
            	}
				vehicleAvailJson = new JSONObject();
				vehicleAvailJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(resWrapperElem, "./car:SupplierID"));
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_VehModifyRS/ota:VehModifyRSCore");
				getSupplierBookResponseJSON(modifyReq, resBodyElem, vehicleAvailJson);
				carRentalArr.put(sequence, vehicleAvailJson);
			}	
			
			resBodyJson.put(JSON_PROP_CARRENTALARR, carRentalArr);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));
			/*pw = new PrintWriter(new File("D:\\BookingEngine\\Modify\\SIModifyResJSON.json"));
	        pw.write(resJson.toString(3));
	        pw.close();*/
	        
	        kafkaMsgJson = new JSONObject(resJson.toString());
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_PROD, reqBodyJson.getString(JSON_PROP_PROD));
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_BOOKID, reqBodyJson.getString(JSON_PROP_BOOKID));
			
			kafkaMsgJson.getJSONObject(JSON_PROP_RESHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).put("clientIATANumber", usrCtx.getClientIATANUmber());
			bookProducer.runProducer(1, kafkaMsgJson);
			
			return resJson.toString();
		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}
	
	private static void getSupplierBookResponseJSON(JSONObject modifyReq,Element vehResCoreElem, JSONObject reservationJson) {
		
		Element reservationElem = XMLUtils.getFirstElementAtXPath(vehResCoreElem, "ota:VehReservation");
		
		Element vehSegCoreElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentCore");
		Element vehSegInfoElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentInfo");
		
		reservationJson.put("requestType", modifyReq.getString("requestType"));
		reservationJson.put("type", modifyReq.getString("type"));
		reservationJson.put("entityId", modifyReq.getString("entityId"));
		reservationJson.put("entityName", modifyReq.getString("entityName").isEmpty() ? "order" : modifyReq.getString("entityName"));
		reservationJson.put(JSON_PROP_TOTALPRICEINFO, CarSearchProcessor.getPricingInfoJSON(vehSegCoreElem, vehSegInfoElem));
//		Element vehSegmentElem = XMLUtils.getFirstElementAtXPath(reservationElem, "./ota:VehSegmentCore");
//		reservationJson.put(JSON_PROP_SUPPPRICEINFO, CarSearchProcessor.getPricingInfoJSON(vehSegmentElem));
		
	}
	
	private static JSONObject getVehSegmentJSON(Element vehSegmentElem) {
		
		JSONObject vehSegmentJson = new JSONObject();
		JSONArray confIdJsonArr = new JSONArray();
		Element confIdsElem[] = XMLUtils.getElementsAtXPath(vehSegmentElem, "./ota:ConfID");
		for(Element confIdElem:confIdsElem) {
			JSONObject confIdJson = new JSONObject();
			confIdJson.put("id", XMLUtils.getValueAtXPath(confIdElem, "./@ID"));
			confIdJson.put("type", XMLUtils.getValueAtXPath(confIdElem, "./@Type"));
			confIdJson.put("status", XMLUtils.getValueAtXPath(confIdElem, "./@Status"));
			confIdJson.put("url", XMLUtils.getValueAtXPath(confIdElem, "./@URL"));
			confIdJson.put("id_Context", XMLUtils.getValueAtXPath(confIdElem, "./@ID_Context"));
			confIdJsonArr.put(confIdJson);
		}
		
		vehSegmentJson.put("confID", confIdJsonArr);
		vehSegmentJson.put("vendorCode", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@Code"));
		vehSegmentJson.put("vendorCompanyShortName", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@CompanyShortName"));
		vehSegmentJson.put("vendorTravelSector", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@TravelSector"));
		vehSegmentJson.put("vendorCodeContext", XMLUtils.getValueAtXPath(vehSegmentElem, "./ota:Vendor/@CodeContext"));
		
		Element vehRentalCoreElem = XMLUtils.getFirstElementAtXPath(vehSegmentElem, "./ota:VehRentalCore");
		vehSegmentJson.put(JSON_PROP_PICKUPDATE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./@PickUpDateTime"));
		vehSegmentJson.put(JSON_PROP_RETURNDATE, XMLUtils.getValueAtXPath(vehRentalCoreElem, "./@ReturnDateTime"));
		
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
		for(Element additionalElem:additionalElems) {
			customerJsonArr.put(getCustomerJSON(additionalElem, false));
		}
		return customerJsonArr;
	}
	
	private static JSONObject getCustomerJSON(Element elem, Boolean isLead) {
		
		JSONObject customerJson = new JSONObject();
		
		customerJson.put("customerId", XMLUtils.getValueAtXPath(elem, "./ota:CustomerID/@ID"));
		customerJson.put("custLoyaltyMembershipID", XMLUtils.getValueAtXPath(elem, "./ota:CustLoyalty/@MembershipID"));
		customerJson.put("namePrefix", XMLUtils.getValueAtXPath(elem, "./ota:NamePrefix"));
		customerJson.put(JSON_PROP_TITLE, XMLUtils.getValueAtXPath(elem, "./ota:NameTitle"));
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
		 paymentAmountElem.setAttribute("Amount",  CarBookProcessor.BigDecimaltoString(rentalPaymentPrefJson,JSON_PROP_AMOUNT));
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

	
	private static void populateModifyWrapperElement(Element reqElem, Document ownerDoc, Element suppWrapperElem,
			UserContext usrCtx, JSONObject modifyReq, JSONObject retrieveRes) throws Exception {
		
		String amendType =  modifyReq.optString(JSON_PROP_TYPE);
		
		//TODO : Need To Change later
		Element sourceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehModifyRQ/ota:POS/ota:Source");
		sourceElem.setAttribute("ISOCurrency", "EUR");
		sourceElem.setAttribute("ISOCountry", "IE");
		
		String suppID = modifyReq.getString(JSON_PROP_SUPPREF);
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
		
		 Element vehModifyCoreElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehModifyRQ/ota:VehModifyRQCore");
		 Element vehModifyInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehModifyRQ/ota:VehModifyRQInfo");
		 
		 vehModifyCoreElem.setAttribute("Status", "Available");
		 vehModifyCoreElem.setAttribute("ModifyType", modifyReq.getString("modifyType"));
		 
		 String bookRefid="";
		 for(Object suppBookRef : modifyReq.getJSONArray(JSON_PROP_SUPPBOOKREF)){
			 if(((JSONObject) suppBookRef).getString("type").equals("14")) {
				 bookRefid = ((JSONObject) suppBookRef).getString("id");
			 }
		 }
		 
		 Element uniqIdElem = ownerDoc.createElementNS(NS_OTA, "ota:UniqueID");
		 uniqIdElem.setAttribute("ID", bookRefid);
		 uniqIdElem.setAttribute("Type", "14");
		 vehModifyCoreElem.appendChild(uniqIdElem);
		 
		 Element vehRentalElem =  CarSearchProcessor.getVehRentalCoreElement(ownerDoc, modifyReq);
		 vehModifyCoreElem.appendChild(vehRentalElem);
		 
		 JSONArray customerJsonArr = modifyReq.optJSONArray(JSON_PROP_PAXDETAILS);
		 if(customerJsonArr!=null && customerJsonArr.length()!=0) {
			 
			 Element customerElem = CarSearchProcessor.populateCustomerElement(ownerDoc, customerJsonArr);
			 vehModifyCoreElem.appendChild(customerElem);
		 }
		 
		 Element vendorPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VendorPref");
		 vendorPrefElem.setAttribute("Code", modifyReq.getString("vendorPrefCode"));
		 vehModifyCoreElem.appendChild(vendorPrefElem);
		 
		 
		 String temp;
		 if(amendType.equals(JSON_PROP_UPGRADECAR)) {
			 JSONObject vehPrefJson = modifyReq.getJSONObject(JSON_PROP_VEHICLEINFO);
			 
			 Element VehPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehPref");
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
			 
			 Element vehTypeElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehType");
			 vehTypeElem.setAttribute("VehicleCategory", vehPrefJson.optString(JSON_PROP_VEHICLECATEGORY));
			 VehPrefElem.appendChild(vehTypeElem);
			 
			 Element vehClassElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehClass");
			 vehClassElem.setAttribute("Size", vehPrefJson.optString("vehicleClassSize"));
			 VehPrefElem.appendChild(vehClassElem);
			 
			 Element vehMakeModelElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehMakeModel");
			 if(!vehPrefJson.optString(JSON_PROP_VEHMAKEMODELNAME).isEmpty())
				 vehMakeModelElem.setAttribute("Name", vehPrefJson.optString(JSON_PROP_VEHMAKEMODELNAME));
			 vehMakeModelElem.setAttribute("Code", vehPrefJson.optString(JSON_PROP_VEHMAKEMODELCODE));
			 VehPrefElem.appendChild(vehMakeModelElem);
	
	         vehModifyCoreElem.appendChild(VehPrefElem);
		}
		
		Element driverAge = ownerDoc.createElementNS(Constants.NS_OTA, "ota:DriverType");
		driverAge.setAttribute("Age", CARRENTAL_DRIVER_AGE);
		vehModifyCoreElem.appendChild(driverAge);
		
		 JSONArray rateDistJsonArr = modifyReq.optJSONArray(JSON_PROP_RATEDISTANCE);
		 if(rateDistJsonArr!=null) {
			 Element rateDistElem = null;
			 for(int i=0;i<rateDistJsonArr.length();i++) {
				 JSONObject rateDistJson = rateDistJsonArr.getJSONObject(i);
				 rateDistElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:RateDistance");
				 rateDistElem.setAttribute("Quantity", rateDistJson.optString(JSON_PROP_QTY));
				 rateDistElem.setAttribute("DistUnitName", rateDistJson.optString("distUnitName"));
				 rateDistElem.setAttribute("VehiclePeriodUnitName", rateDistJson.optString("vehiclePeriodUnitName"));
				 
				 vehModifyCoreElem.appendChild(rateDistElem);
			 }
		 }
		 
		 JSONObject rateQualifier = modifyReq.optJSONObject(JSON_PROP_RATEQUALIFIER);
		 if(rateQualifier!=null) {
			 Element rateQualifierElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:RateQualifier");
			 rateQualifierElem.setAttribute("RateQualifier", rateQualifier.getString(JSON_PROP_RATEQUALIFIER));
			 vehModifyCoreElem.appendChild(rateQualifierElem);
		 }
		 
		 //Ancillary includes Equipments,Additional Drivers and Coverages.
		 if(amendType.equals(JSON_PROP_ADDANCILLARY)) {
			 //Additinal Driver to be sent in <SpecialEquipPref> tag
			 JSONArray splEquipsArr = modifyReq.optJSONArray(JSON_PROP_SPLEQUIPS);
			 if(splEquipsArr!=null && splEquipsArr.length()!=0) {
				 
				 Element specialEquipPrefs = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPrefs");
				 Element specialEquipPref = null;
				 for(int i=0;i<splEquipsArr.length();i++) {
					 JSONObject splEquips = splEquipsArr.getJSONObject(i);
					 specialEquipPref = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPref");
					 specialEquipPref.setAttribute("EquipType", splEquips.optString(JSON_PROP_EQUIPTYPE));
					 specialEquipPref.setAttribute("Quantity", String.valueOf(splEquips.optInt(JSON_PROP_QTY, 1)));
					 specialEquipPref.setAttribute("Action", "Add");
					 specialEquipPrefs.appendChild(specialEquipPref);
				 }
				 vehModifyCoreElem.appendChild(specialEquipPrefs);
			 }
		 
			 //TODO : When adding Coverages Element, SI Not giving response. Check with SI.
	        /* JSONArray pricedCovrgsArr = modifyReq.optJSONArray(JSON_PROP_PRICEDCOVRGS);
	         if(pricedCovrgsArr!=null && pricedCovrgsArr.length()!=0) {
	        	 Element pricedCovrgsElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CoveragePrefs");
	        	 for(int i=0;i<pricedCovrgsArr.length();i++) {
					 JSONObject pricedCovrgJson = pricedCovrgsArr.optJSONObject(i);
					 Element pricedCovrgElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CoveragePref");
					 //TODO: Better Way to handle this
					 pricedCovrgElem.setAttribute("CoverageType", pricedCovrgJson.getString(JSON_PROP_COVERAGETYPE));
					 pricedCovrgsElem.appendChild(pricedCovrgElem);
				 }
	        	 vehModifyInfoElem.appendChild(pricedCovrgsElem);
	         }*/
		 }
		 
		 if(amendType.equals(JSON_PROP_REMOVEANCILLARY)) {
			 
			 JSONArray retsplEquipArr = retrieveRes.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_SPLEQUIPS).optJSONArray(JSON_PROP_SPLEQUIP);
			 JSONArray splEquipsArr = modifyReq.optJSONArray(JSON_PROP_SPLEQUIPS);
			 if(splEquipsArr!=null && splEquipsArr.length()!=0) {
				 
				 Element specialEquipPrefs = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPrefs");
				 Element specialEquipPref = null;
				 for(int i=0;i<splEquipsArr.length();i++) {
					 JSONObject splEquips = splEquipsArr.getJSONObject(i);
					 specialEquipPref = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPref");
					 specialEquipPref.setAttribute("EquipType", splEquips.optString(JSON_PROP_EQUIPTYPE));
					 Boolean isPresent = false;
					 String isRequired = "";
					 Integer quantity = splEquips.optInt(JSON_PROP_QTY);
					 //if quantity not provided by WEM ,Cancel the equipment.
					 if(quantity==0) {
						 specialEquipPref.setAttribute("Action", "Delete");
					 }
					 for(int j=0;j<retsplEquipArr.length();j++) {
						 JSONObject retsplEquip = retsplEquipArr.getJSONObject(j);
						 String equipType = retsplEquip.getString(JSON_PROP_EQUIPTYPE);
						 if(equipType.equals(splEquips.optString(JSON_PROP_EQUIPTYPE))) {
							 isPresent = true;
							 isRequired = retsplEquip.optString(JSON_PROP_ISREQUIRED);
							 if("true".equals(isRequired)) {
								 logger.info(String.format("Cannot remove EquipmentType %s as Required is true", equipType));
								 break;
							 }
							 quantity = retsplEquip.getInt(JSON_PROP_QTY) - quantity;
						 }
					 }
					 if(isPresent==false || "true".equals(isRequired)) 
						 continue;
					 if(quantity<=0) {
						 specialEquipPref.setAttribute("Action", "Delete");
					 }
					 else {
						 specialEquipPref.setAttribute("Quantity", quantity.toString());
					 }
					 specialEquipPrefs.appendChild(specialEquipPref);
				 }
				 if(XMLUtils.getElementsAtXPath(specialEquipPrefs, "./ota:SpecialEquipPref").length!=0)
					 vehModifyCoreElem.appendChild(specialEquipPrefs);
			 }
		 
			 //TODO : When adding Coverages Element, SI Not giving response.
	        /* JSONArray pricedCovrgsArr = modifyReq.optJSONArray(JSON_PROP_PRICEDCOVRGS);
	         if(pricedCovrgsArr!=null && pricedCovrgsArr.length()!=0) {
	        	 Element pricedCovrgsElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CoveragePrefs");
	        	 for(int i=0;i<pricedCovrgsArr.length();i++) {
					 JSONObject pricedCovrgJson = pricedCovrgsArr.optJSONObject(i);
					 Element pricedCovrgElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CoveragePref");
					 //TODO: Better Way to handle this
					 pricedCovrgElem.setAttribute("CoverageType", pricedCovrgJson.getString(JSON_PROP_COVERAGETYPE));
					 pricedCovrgsElem.appendChild(pricedCovrgElem);
				 }
	        	 vehModifyInfoElem.appendChild(pricedCovrgsElem);
	         }*/
		}
		 
        JSONObject refJson = modifyReq.optJSONObject(JSON_PROP_REFERENCE);
        if(refJson!=null && refJson.length()!=0) {
	        Element refElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:Reference");
	        if(!(temp = refJson.optString("type")).isEmpty())
	       	 refElem.setAttribute("Type", temp);
	        if(!(temp = refJson.optString("id")).isEmpty())
	       	 refElem.setAttribute("ID", temp);
	        if(!(temp = refJson.optString("url")).isEmpty())
	       	 refElem.setAttribute("URL", temp);
	        if(!(temp = refJson.optString("id_Context")).isEmpty())
	       	 refElem.setAttribute("ID_Context", temp);
	        
	        vehModifyInfoElem.appendChild(refElem);
        }
	} 	  
	
	private static JSONObject callOperationTodo(JSONObject reqBodyJson,JSONObject resJson, JSONObject modifyReq) {
		// TODO:Create Request for operation list
		JSONObject operationMessageJson = new JSONObject(new JSONTokener(OperationsShellConfig.getOperationsTodoErrorShell()));
		String operationsUrl = OperationsShellConfig.getOperationsUrl();
		/*Mandatory fields to filled are createdByUserId,productId,referenceId,taskFunctionalAreaId,
		taskNameId,taskPriorityId,taskSubTypeId,taskTypeId*/
		//Add main field dueOn to JSON
		operationMessageJson.put("createdByUserId", "bookingEngine");
		operationMessageJson.put("taskFunctionalAreaId", "OPERATIONS");
		operationMessageJson.put("taskNameId", modifyReq.getString("requestType"));
		//TODO:have to decide on the value
		operationMessageJson.put("taskPriorityId", "HIGH");
		//TODO:Determine exact values to be passed for taskSubType
		operationMessageJson.put("taskSubTypeId", modifyReq.getString(JSON_PROP_TYPE));
		//TODO:Determine Value
		operationMessageJson.put("taskTypeId", "MAIN");
		//TODO:Determing value
		operationMessageJson.put("dueOn", "2");
		
		operationMessageJson.put("productId", reqBodyJson.getString("bookID"));
		//TODO:Get from db
		operationMessageJson.put("referenceId", modifyReq.getString("referenceID"));
	
		InputStream httpResStream = HTTPServiceConsumer.consumeService(operationMessageJson, operationsUrl);
		JSONObject opResJson = new JSONObject(new JSONTokener(httpResStream));
		if (logger.isInfoEnabled()) {
			logger.info(String.format("%s JSON Response = %s", opResJson.toString()));
		}
		return opResJson;
	}
	
	private static JSONObject getSIErrorResponse(JSONObject resJson) {
		
		JSONObject errorMessage = new JSONObject();
		errorMessage.put("errorMessage", "This service is not supported. Kindly contact our operations team for support.");
		 
		resJson.put(JSON_PROP_RESBODY, errorMessage);
        
		return resJson;
		
	}

}
