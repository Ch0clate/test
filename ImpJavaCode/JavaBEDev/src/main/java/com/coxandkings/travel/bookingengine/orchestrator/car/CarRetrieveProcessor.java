package com.coxandkings.travel.bookingengine.orchestrator.car;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.car.CarConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class CarRetrieveProcessor implements CarConstants{
	
	@Autowired
	private static final Logger logger = LogManager.getLogger(CarModifyProcessor.class);
	
	public static String process(JSONObject reqJson) {

		try {
			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			
			return process(reqJson, usrCtx);
			
		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}
	
	public static String process(JSONObject reqJson, UserContext usrCtx) {

		try {
			OperationConfig opConfig = CarConfig.getOperationConfig("retrieve");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cari:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./car:OTA_VehRetResRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);
			
			CarSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
			JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_CARRENTALARR);
			
			for (int y = 0; y < multiReqArr.length(); y++) {
				
				JSONObject carRentalReq = multiReqArr.getJSONObject(y);
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				XMLUtils.setValueAtXPath(suppWrapperElem, "./car:Sequence", String.valueOf(y));
				reqBodyElem.appendChild(suppWrapperElem);
				
				populateWrapperElement(reqElem, ownerDoc, suppWrapperElem, usrCtx, carRentalReq);
			}
			
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(),
					CarConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			JSONObject resBodyJson = new JSONObject();
			JSONObject vehicleAvailJson = null;
			Element[] resWrapperElems = XMLUtils.getElementsAtXPath(resElem, "./cari:ResponseBody/car:OTA_VehRetResRSWrapper");
			
			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			JSONArray carRentalArr = new JSONArray();
			for (Element resWrapperElem : resWrapperElems) {
				
				int sequence = Utils.convertToInt(XMLUtils.getValueAtXPath(resWrapperElem, "./car:Sequence"), 0);
				JSONObject retrieveReq = reqBodyJson.getJSONArray(JSON_PROP_CARRENTALARR).getJSONObject(sequence);
				vehicleAvailJson = new JSONObject();
				vehicleAvailJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(resWrapperElem, "./car:SupplierID"));
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_VehRetResRS/ota:VehRetResRSCore");
				getSupplierBookResponseJSON(retrieveReq, resBodyElem, vehicleAvailJson);
				carRentalArr.put(sequence, vehicleAvailJson);
			}	
			
			resBodyJson.put(JSON_PROP_CARRENTALARR, carRentalArr);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));
			
			return resJson.toString();
		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	private static void getSupplierBookResponseJSON(JSONObject modifyReq, Element resBodyElem,
			JSONObject vehicleAvailJson) throws Exception{
		
		Element vehSegCoreElem = XMLUtils.getFirstElementAtXPath(resBodyElem, "./ota:VehReservation/ota:VehSegmentCore");
		Element vehSegInfoElem = XMLUtils.getFirstElementAtXPath(resBodyElem, "./ota:VehReservation/ota:VehSegmentInfo");
		
		Element vehRentalElem = XMLUtils.getFirstElementAtXPath(vehSegCoreElem, "./ota:VehRentalCore");
		
		vehicleAvailJson.put(JSON_PROP_PICKUPDATE, XMLUtils.getValueAtXPath(vehRentalElem, "./@PickUpDateTime"));
		vehicleAvailJson.put(JSON_PROP_RETURNDATE, XMLUtils.getValueAtXPath(vehRentalElem, "./@ReturnDateTime"));
		// TODO: To Convert OneWayIndicator to Boolean
		String oneWayIndc = XMLUtils.getValueAtXPath(vehRentalElem, "./@OneWayIndicator");

		vehicleAvailJson.put(JSON_PROP_TRIPTYPE,
				!oneWayIndc.isEmpty() ? (oneWayIndc.equals("true") ? "OneWay" : "Return") : "");
		vehicleAvailJson.put(JSON_PROP_PICKUPLOCCODE,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:PickUpLocation/@LocationCode"));
		vehicleAvailJson.put(JSON_PROP_RETURNLOCCODE,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:ReturnLocation/@LocationCode"));
		vehicleAvailJson.put(JSON_PROP_PICKUPLOCEXTCODE,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:PickUpLocation/@ExtendedLocationCode"));
		vehicleAvailJson.put(JSON_PROP_RETURNLOCEXTCODE,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:ReturnLocation/@ExtendedLocationCode"));
		vehicleAvailJson.put(JSON_PROP_PICKUPLOCCODECONTXT,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:PickUpLocation/@CodeContext"));
		vehicleAvailJson.put(JSON_PROP_RETURNLOCCODECONTXT,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:ReturnLocation/@CodeContext"));
		vehicleAvailJson.put(JSON_PROP_PICKUPLOCNAME,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:PickUpLocation/@Name"));
		vehicleAvailJson.put(JSON_PROP_RETURNLOCNAME,
				XMLUtils.getValueAtXPath(vehRentalElem, "./ota:ReturnLocation/@Name"));
		
		Element vehicleElem = XMLUtils.getFirstElementAtXPath(vehSegCoreElem, "./ota:Vehicle");
		vehicleAvailJson.put(JSON_PROP_VEHICLEINFO, CarSearchProcessor.getVehicleInfo(vehicleElem));
		
		JSONObject pricingInfo = CarSearchProcessor.getPricingInfoJSON(vehSegCoreElem, vehSegInfoElem);
		vehicleAvailJson.put(JSON_PROP_TOTALPRICEINFO, pricingInfo);
	}

	private static void populateWrapperElement(Element reqElem, Document ownerDoc, Element suppWrapperElem,
			UserContext usrCtx, JSONObject retrieveReq) throws Exception{
		
		//TODO : Need To Change later
		Element sourceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehRetResRQ/ota:POS/ota:Source");
		sourceElem.setAttribute("ISOCurrency", "EUR");
		sourceElem.setAttribute("ISOCountry", "IE");
		
		String suppID = retrieveReq.getString(JSON_PROP_SUPPREF);
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
		
		 Element vehRetCoreElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehRetResRQ/ota:VehRetResRQCore");
		 Element vehRetInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehRetResRQ/ota:VehRetResRQInfo");
		 
		 String bookRefid="";
		 for(Object suppBookRef : retrieveReq.getJSONArray(JSON_PROP_SUPPBOOKREF)){
			 if(((JSONObject) suppBookRef).getString("type").equals("14")) {
				 bookRefid = ((JSONObject) suppBookRef).getString("id");
			 }
		 }
		 
		 Element uniqIdElem = ownerDoc.createElementNS(NS_OTA, "ota:UniqueID");
		 uniqIdElem.setAttribute("ID", bookRefid);
		 uniqIdElem.setAttribute("Type", "14");
		 vehRetCoreElem.appendChild(uniqIdElem);
		 
		 //Finding Primary Customer
		 JSONArray paxDetails = retrieveReq.optJSONArray(JSON_PROP_PAXDETAILS);
		 JSONObject paxDetail = null;
		 for(int i=0;i<paxDetails.length();i++) {
			 paxDetail = paxDetails.getJSONObject(i);
			 if(paxDetail.optBoolean(JSON_PROP_ISLEAD) == true){
				 break;
			 }
		 }
		 Element personName = ownerDoc.createElementNS(Constants.NS_OTA, "ota:PersonName");
		 Element givenName = ownerDoc.createElementNS(Constants.NS_OTA, "ota:GivenName");
		 givenName.setTextContent(paxDetail.getString(JSON_PROP_FIRSTNAME));
		 Element surname = ownerDoc.createElementNS(Constants.NS_OTA, "ota:Surname");
		 surname.setTextContent(paxDetail.getString(JSON_PROP_SURNAME));
		 personName.appendChild(givenName);
		 personName.appendChild(surname);
		 vehRetCoreElem.appendChild(personName);
		 
		 Element vendorElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:Vendor");
		 vendorElem.setAttribute("Code", retrieveReq.getString("vendorPrefCode"));
		 vehRetInfoElem.appendChild(vendorElem);
		 
	}
}
