package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.enums.AccoSubType;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class AccoModifyProcessor implements AccoConstants {

	private static final Logger logger = LogManager.getLogger(AccoModifyProcessor.class);
	static final String OPERATION_NAME = "modify";

	//***********************************SI JSON TO XML FOR REQUEST BODY STARTS HERE**************************************//
	public static void setSupplierRequestElem(JSONObject reqJson,Element reqElem) throws Exception {

		Document ownerDoc = reqElem.getOwnerDocument();
		Element blankWrapperElem = XMLUtils.getFirstElementAtXPath(reqElem, "./accoi:RequestBody/acco:OTA_HotelResModifyRQWrapper");
		XMLUtils.removeNode(blankWrapperElem);
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
		JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();

		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);

		AccoSearchProcessor.createSuppReqHdrElem(XMLUtils.getFirstElementAtXPath(reqElem, "./acco:RequestHeader"),sessionID,transactionID,userID);
		UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
		String suppID;
		ProductSupplier prodSupplier;
		JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		JSONObject roomObjectJson;
		Element wrapperElement,suppCredsListElem= XMLUtils.getFirstElementAtXPath(reqElem, "./acco:RequestHeader/com:SupplierCredentialsList");
		AccoSubType prodCategSubtype;
		for (int j=0; j < multiReqArr.length(); j++) {

			roomObjectJson =   multiReqArr.getJSONObject(j);
			suppID = roomObjectJson.getString(JSON_PROP_SUPPREF);
			prodCategSubtype = AccoSubType.forString(roomObjectJson.optString(JSON_PROP_ACCOSUBTYPEARR));
			prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_ACCO,prodCategSubtype!=null?prodCategSubtype.toString():DEF_PRODSUBTYPE, suppID);

			if (prodSupplier == null) {
				throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
			}

			suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc,j));
			wrapperElement= (Element) blankWrapperElem.cloneNode(true);

			XMLUtils.setValueAtXPath(wrapperElement, "./acco:SupplierID",suppID);
			XMLUtils.setValueAtXPath(wrapperElement, "./acco:Sequence", String.valueOf(j));
			setSuppReqOTAElem(ownerDoc, XMLUtils.getFirstElementAtXPath(wrapperElement,"./ota:OTA_HotelResModifyRQ"), roomObjectJson);

			XMLUtils.insertChildNode(reqElem, "./accoi:RequestBody", wrapperElement, false);
		}
	}

	public static void setSuppReqOTAElem(Document ownerDoc,Element reqOTAElem,JSONObject roomObjectJson) {

		Element hotelResModifyElem = XMLUtils.getFirstElementAtXPath(reqOTAElem, "./ota:HotelResModifies/ota:HotelResModify");
		Element roomStaysElem = (Element) XMLUtils.getFirstElementAtXPath(hotelResModifyElem, "./ota:RoomStays");
		Element resGuestsElem =  XMLUtils.getFirstElementAtXPath(hotelResModifyElem, "./ota:ResGuests");
		Element roomStayElem = ownerDoc.createElementNS(NS_OTA, "ota:RoomStay");
		Element guestCountsElem =  ownerDoc.createElementNS(NS_OTA, "ota:GuestCounts");
		JSONObject roomInfoJson = roomObjectJson.getJSONObject(JSON_PROP_ROOMINFO);
		String cityCode = roomObjectJson.getString(JSON_PROP_CITYCODE);
		String countryCode = roomObjectJson.getString(JSON_PROP_COUNTRYCODE);
		String chkIn = roomObjectJson.getString(JSON_PROP_CHKIN);
		String chkOut = roomObjectJson.getString(JSON_PROP_CHKOUT);
		JSONArray paxArr = roomObjectJson.getJSONArray(JSON_PROP_PAXINFOARR);

		roomStayElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:RoomTypes")).appendChild(AccoBookProcessor.getRoomTypeElem(ownerDoc, roomInfoJson));
		roomStayElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:RatePlans")).appendChild(AccoBookProcessor.getRateElem(ownerDoc, roomInfoJson));
		for(int k=0;k<paxArr.length();k++) {
			AccoBookProcessor.addGuestDetails(ownerDoc, (JSONObject) paxArr.get(k), resGuestsElem, guestCountsElem, k);
		}
		roomStayElem.appendChild(guestCountsElem);
		roomStayElem.appendChild(AccoBookProcessor.getTimeSpanElem(ownerDoc, chkIn, chkOut));
		//roomStayElem.appendChild(totalElem);
		roomStayElem.appendChild(AccoBookProcessor.getHotelElem(ownerDoc, roomInfoJson, cityCode, countryCode));
		for(Object reference:roomInfoJson.getJSONArray(JSON_PROP_REFERENCESARR)) {
			roomStayElem.appendChild(AccoBookProcessor.getReferenceElem(ownerDoc, (JSONObject) reference));
		}
		roomStayElem.setAttribute("RPH", String.valueOf(roomInfoJson.getInt(JSON_PROP_ROOMINDEX)));
		roomStayElem.setAttribute("RoomStayStatus", (roomInfoJson.getString(JSON_PROP_AVAILSTATUS)));
		roomStayElem.setAttribute("IndexNumber", (roomObjectJson.getString("supplierRoomIndex")));

		roomStaysElem.appendChild(roomStayElem);
		hotelResModifyElem.appendChild(roomStaysElem);
		hotelResModifyElem.appendChild(resGuestsElem);
		
		for(Object bookRefJson:roomObjectJson.getJSONArray("supplierBookReferences")) {
			XMLUtils.insertChildNode(hotelResModifyElem, ".", getUniqueIdElem(ownerDoc, (JSONObject) bookRefJson), true);
		}
		//TODO:set some enum
		reqOTAElem.setAttribute("Target", roomObjectJson.getString("modificationType"));
	}

	public static Element getUniqueIdElem(Document ownerDoc, JSONObject bookRefJson) {

		Element uniqueIdElem = ownerDoc.createElementNS(NS_OTA, "ota:UniqueID");

		uniqueIdElem.setAttribute("ID",bookRefJson.getString(JSON_PROP_REFVALUE));
		uniqueIdElem.setAttribute("Type",bookRefJson.getString(JSON_PROP_REFCODE));

		return uniqueIdElem;
	}
	//***********************************SI JSON TO XML FOR REQUEST BODY ENDS HERE**************************************//
	
	public static String process(JSONObject reqJson) {
		try{
			OperationConfig opConfig = AccoConfig.getOperationConfig(OPERATION_NAME);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			TrackingContext.setTrackingContext(reqJson);

			setSupplierRequestElem(reqJson, reqElem);

			//getKakfaAmendRequest(reqJson);
			
			Element resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), AccoConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			//JSONObject resJson = getSupplierResponseJSON(reqJson, resElem);

			return XMLTransformer.toString(resElem);
		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	private static void getKakfaAmendRequest(JSONObject reqJson) {
		JSONObject reqBody = reqJson.getJSONObject(JSON_PROP_REQBODY);
		JSONObject reqHdr = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONArray accoInfoArr = reqBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		
		for(int i=0;i<accoInfoArr.length();i++) {
			JSONObject accoInfoObj=accoInfoArr.getJSONObject(i);
			JSONObject kafkaRqJson=new JSONObject();
			kafkaRqJson.put(JSON_PROP_REQHEADER, reqHdr);
			JSONObject reqBdy=new JSONObject();
			String modType = accoInfoObj.getString("modificationType");
			if(modType!=null) {
			reqBdy.put(JSON_PROP_PROD, "ACCO");
			reqBdy.put(JSON_PROP_TYPE, modType);
			Object entityName = "FULLCANCELLATION".equals(modType)?reqBdy.put("entityName", "order"):reqBdy.put("entityName", "room");
			reqBdy.put("orderId", accoInfoObj.getString("orderId"));
			if(modType.endsWith("PASSENGER")) 
				setPaxKafka(reqBdy,modType,accoInfoObj);
			else if(modType.endsWith("ROOM") || modType=="FULLCANCELLATION") 
				setRoomKafka(reqBdy,modType,accoInfoObj);
			kafkaRqJson.put(JSON_PROP_REQBODY, reqBdy);
		    System.out.println("AmendRQ"+kafkaRqJson);
		}
		}
		
	}


	private static void setRoomKafka(JSONObject newReqBody,String modType,JSONObject accoInfoObj) {
		if("UPDATEROOM".equals(modType)){
			newReqBody.put("requestType", "amend");
			JSONObject roomInfoObj = accoInfoObj.getJSONObject("roomInfo");
			newReqBody.put(JSON_PROP_ROOMTYPECODE, roomInfoObj.getJSONObject(JSON_PROP_ROOMTYPEINFO).getString(JSON_PROP_ROOMTYPECODE));
		    newReqBody.put(JSON_PROP_RATEPLANCODE, roomInfoObj.getJSONObject(JSON_PROP_RATEPLANINFO).getString(JSON_PROP_RATEPLANCODE));
		 }
		else {
		newReqBody.put("requestType", "cancel");
		Object entityId = "FULLCANCELLATION".equals(modType)?newReqBody.put("entityId", accoInfoObj.getString("orderId")):newReqBody.put("entityId", accoInfoObj.getString("roomId"));
	     }
	}

	private static void setPaxKafka(JSONObject newReqBody,String modType, JSONObject accoInfoObj) {
		Object reqType = "CANCELPASSENGER".equals(modType)? newReqBody.put("requestType", "cancel"):newReqBody.put("requestType", "amend") ;
		newReqBody.put("entityId", accoInfoObj.getString("roomId"));
		JSONArray paxArr = accoInfoObj.getJSONArray(JSON_PROP_PAXINFOARR);
		JSONArray paxInfoArr=new JSONArray();
		for(int i=0;i<paxArr.length();i++) {
			if(paxArr.getJSONObject(i).optInt("flag") == 1 ) {
				paxInfoArr.put(paxArr.getJSONObject(i));
			}
			
		}
		newReqBody.put("paxInfo", paxInfoArr);
	}
}
