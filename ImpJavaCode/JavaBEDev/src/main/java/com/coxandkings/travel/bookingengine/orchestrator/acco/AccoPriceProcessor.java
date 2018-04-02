package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.enums.AccoSubType;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class AccoPriceProcessor implements AccoConstants {

	private static final Logger logger = LogManager.getLogger(AccoPriceProcessor.class);
	static final String OPERATION_NAME = "price";

	//***********************************SI JSON TO XML FOR REQUEST BODY STARTS HERE**************************************//

	public static void setSupplierRequestElem(JSONObject reqJson,Element reqElem) throws Exception {

		Document ownerDoc = reqElem.getOwnerDocument();
		Element blankWrapperElem = XMLUtils.getFirstElementAtXPath(reqElem, "./accoi:RequestBody/acco:OTA_HotelAvailRQWrapper");
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
			setSuppReqOTAElem(ownerDoc, XMLUtils.getFirstElementAtXPath(wrapperElement,"./ota:OTA_HotelAvailRQ"), roomObjectJson,reqHdrJson);

			XMLUtils.insertChildNode(reqElem, "./accoi:RequestBody", wrapperElement, false);
		}
	}
	
	public static void setSuppReqOTAElem(Document ownerDoc,Element reqOTAElem,JSONObject reqParamJson,JSONObject reqHdrJSON) {

		Element baseElem = XMLUtils.getFirstElementAtXPath(reqOTAElem, "./ota:AvailRequestSegments/ota:AvailRequestSegment/ota:HotelSearchCriteria/ota:Criterion");
		JSONArray roomConfigArr = reqParamJson.getJSONArray(JSON_PROP_REQUESTEDROOMARR);
		JSONObject roomJson;
		
		XMLUtils.setValueAtXPath(baseElem,"./ota:RefPoint/@CountryCode",reqParamJson.getString(JSON_PROP_COUNTRYCODE));
		XMLUtils.setValueAtXPath(baseElem,"./ota:HotelRef/@HotelCityCode",reqParamJson.getString(JSON_PROP_CITYCODE));
		XMLUtils.setValueAtXPath(baseElem,"./ota:HotelRef/@HotelCode",((JSONObject)roomConfigArr.get(0)).getJSONObject(JSON_PROP_ROOMINFO).getJSONObject(JSON_PROP_HOTELINFO).getString(JSON_PROP_HOTELCODE));
		XMLUtils.setValueAtXPath(baseElem,"./ota:StayDateRange/@Start",reqParamJson.getString(JSON_PROP_CHKIN));
		XMLUtils.setValueAtXPath(baseElem,"./ota:StayDateRange/@End",reqParamJson.getString(JSON_PROP_CHKOUT));
		//TODO:country code should be added
		XMLUtils.setValueAtXPath(baseElem,"./ota:Profiles/ota:ProfileInfo/ota:Profile/ota:Customer/ota:CitizenCountryName/@Code",
					reqHdrJSON.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTNATIONALITY));

		for(int i=0;i<roomConfigArr.length();i++){
			roomJson = (JSONObject) roomConfigArr.get(i);
			XMLUtils.insertChildNode(baseElem,"./ota:RoomStayCandidates",AccoSearchProcessor.getRoomStayCandidateElem(ownerDoc,(JSONObject) roomJson, i+1),false);
		}
		
	}

	//***********************************SI JSON TO XML FOR REQUEST BODY ENDS HERE**************************************//
	
	//***********************************SI XML TO JSON FOR RESPONSE STARTS HERE**************************************//

	public static JSONObject getSupplierResponseJSON(JSONObject reqJson,Element resElem){
		
		JSONObject resBodyJson = new JSONObject();
		JSONArray multiResArr = new JSONArray();
		resBodyJson.put(JSON_PROP_ACCOMODATIONARR, multiResArr);
		int sequence = 0;String sequence_str="";
		JSONArray roomStayJsonArr;
		JSONObject roomStayJson;

		for(Element wrapperElem:XMLUtils.getElementsAtXPath(resElem, "./accoi:ResponseBody/acco:OTA_HotelAvailRSWrapper")) {
			roomStayJsonArr = new JSONArray();
			for (Element roomStayElem : XMLUtils.getElementsAtXPath(wrapperElem, "./ota:OTA_HotelAvailRS/ota:RoomStays/ota:RoomStay")) {
				roomStayJson = AccoSearchProcessor.getRoomStayJSON(roomStayElem);
				roomStayJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(wrapperElem, "./acco:SupplierID"));
				roomStayJsonArr.put(roomStayJson);
			}
			sequence_str = XMLUtils.getValueAtXPath(wrapperElem, "./acco:Sequence");
			sequence = sequence_str.isEmpty()?sequence:Integer.parseInt(sequence_str);
			multiResArr.put(sequence, (new JSONObject()).put(JSON_PROP_ROOMSTAYARR, roomStayJsonArr));
			sequence++;
		}
		
		JSONObject resJson = new JSONObject();
		resJson.put(JSON_PROP_RESHEADER, reqJson.getJSONObject(JSON_PROP_REQHEADER));
		resJson.put(JSON_PROP_RESBODY, resBodyJson);

		return resJson;

	}
	
	//***********************************SI XML TO JSON FOR RESPONSE ENDS HERE**************************************//

	public static String process(JSONObject reqJson) {
		try{
			OperationConfig opConfig = AccoConfig.getOperationConfig(OPERATION_NAME);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			TrackingContext.setTrackingContext(reqJson);
			
			setSupplierRequestElem(reqJson, reqElem);
			
			Element resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), AccoConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			
			JSONObject resJson = getSupplierResponseJSON(reqJson, resElem);
			
			Map<Integer,String> SI2BRMSRoomMap= new HashMap<Integer,String>();
			JSONObject resSupplierComJson =  SupplierCommercials.getSupplierCommercials(reqJson, resJson, SI2BRMSRoomMap);
			JSONObject resClientComJson = ClientCommercials.getClientCommercials(resSupplierComJson);
			
			AccoSearchProcessor.calculatePrices(reqJson,resJson,resClientComJson,resSupplierComJson,SI2BRMSRoomMap,false);
			
			return resJson.toString();

		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}

	}


}
