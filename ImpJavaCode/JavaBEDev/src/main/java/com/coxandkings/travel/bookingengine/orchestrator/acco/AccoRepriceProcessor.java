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
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class AccoRepriceProcessor implements AccoConstants {

	private static final Logger logger = LogManager.getLogger(AccoSearchProcessor.class);
	static final String OPERATION_NAME = "reprice";

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
	
	public static void setSuppReqOTAElem(Document ownerDoc,Element reqOTAElem,JSONObject reqParamJson,JSONObject reqHdrJson) {

		Element baseElem = XMLUtils.getFirstElementAtXPath(reqOTAElem, "./ota:AvailRequestSegments/ota:AvailRequestSegment/ota:HotelSearchCriteria/ota:Criterion");
		JSONArray roomConfigArr = reqParamJson.getJSONArray(JSON_PROP_REQUESTEDROOMARR);
		JSONObject roomJson;

		XMLUtils.setValueAtXPath(baseElem,"./ota:RefPoint/@CountryCode",reqParamJson.getString(JSON_PROP_COUNTRYCODE));
		XMLUtils.setValueAtXPath(baseElem,"./ota:HotelRef/@HotelCityCode",reqParamJson.getString(JSON_PROP_CITYCODE));
		XMLUtils.setValueAtXPath(baseElem,"./ota:HotelRef/@HotelCode",((JSONObject)roomConfigArr.get(0)).getJSONObject(JSON_PROP_ROOMINFO).getJSONObject(JSON_PROP_HOTELINFO).getString(JSON_PROP_HOTELCODE));
		XMLUtils.setValueAtXPath(baseElem,"./ota:StayDateRange/@Start",reqParamJson.getString(JSON_PROP_CHKIN));
		XMLUtils.setValueAtXPath(baseElem,"./ota:StayDateRange/@End",reqParamJson.getString(JSON_PROP_CHKOUT));
		XMLUtils.setValueAtXPath(baseElem,
				"./ota:Profiles/ota:ProfileInfo/ota:Profile/ota:Customer/ota:CitizenCountryName/@Code",
				reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTNATIONALITY));

		for(int i=0;i<roomConfigArr.length();i++){
			roomJson = (JSONObject) roomConfigArr.get(i);
			XMLUtils.insertChildNode(baseElem,"./ota:RatePlanCandidates",getRatePlanCandidateElem(ownerDoc,roomJson,i+1),false);
			XMLUtils.insertChildNode(baseElem,"./ota:RoomStayCandidates",getRoomStayCandidateElem(ownerDoc,roomJson,i+1),false);
		}

	}


	public static Element getRoomStayCandidateElem(Document ownerDoc, JSONObject roomJson, int rph) {

		Element roomElem = AccoSearchProcessor.getRoomStayCandidateElem(ownerDoc, roomJson, rph);
		// extra params to be set in reprice
		JSONObject roomTypeJson = roomJson.getJSONObject(JSON_PROP_ROOMINFO).getJSONObject(JSON_PROP_ROOMTYPEINFO);
		Element tpaElem = ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");

		tpaElem.appendChild(getMealInfo(ownerDoc, roomJson));
		roomElem.setAttribute("BookingCode", roomJson.getJSONObject(JSON_PROP_ROOMINFO)
				.getJSONObject(JSON_PROP_RATEPLANINFO).getString(JSON_PROP_RATEBOOKINGREF));
		roomElem.setAttribute("RoomTypeCode", roomTypeJson.getString(JSON_PROP_ROOMTYPECODE));
		roomElem.setAttribute("RoomType", roomTypeJson.getString(JSON_PROP_ROOMTYPENAME));
		roomElem.setAttribute("RoomID", roomTypeJson.getString(JSON_PROP_ROOMREF));
		roomElem.appendChild(tpaElem);
		
		return roomElem;
	}

	private static Element getMealInfo(Document ownerDoc, JSONObject roomJson) {
		
		Element mealsElem =  ownerDoc.createElementNS(NS_ACCO, "acco:Meals");
		Element mealElem = ownerDoc.createElementNS(NS_ACCO, "acco:Meal");
		JSONObject mealInfoJson = roomJson.getJSONObject(JSON_PROP_ROOMINFO).getJSONObject(JSON_PROP_MEALINFO);
		
		mealElem.setAttribute("MealId", mealInfoJson.getString(JSON_PROP_MEALCODE));
		mealElem.setAttribute("Name", mealInfoJson.getString(JSON_PROP_MEALNAME));
		
		mealsElem.appendChild(mealElem);
		
		return mealsElem;
		
	}

	private static Element getRatePlanCandidateElem(Document ownerDoc, JSONObject roomJson, int rph) {
		
		Element ratePlanElem = ownerDoc.createElementNS(NS_OTA, "ota:RatePlanCandidate");
		JSONObject ratePlanJson = roomJson.getJSONObject(JSON_PROP_ROOMINFO).getJSONObject(JSON_PROP_RATEPLANINFO);

		ratePlanElem.setAttribute("RatePlanID",ratePlanJson.getString(JSON_PROP_RATEPLANREF));
		ratePlanElem.setAttribute("RatePlanCode",ratePlanJson.getString(JSON_PROP_RATEPLANCODE));
		ratePlanElem.setAttribute("RPH",Integer.toString(rph));

		return ratePlanElem;
	}

	//***********************************SI JSON TO XML FOR REQUEST BODY ENDS HERE**************************************//

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
			
			JSONObject resJson = AccoPriceProcessor.getSupplierResponseJSON(reqJson, resElem);
			
			Map<Integer,String> SI2BRMSRoomMap= new HashMap<Integer,String>();
			JSONObject resSupplierComJson =  SupplierCommercials.getSupplierCommercials(reqJson, resJson, SI2BRMSRoomMap);
			JSONObject resClientComJson = ClientCommercials.getClientCommercials(resSupplierComJson);
			
			AccoSearchProcessor.calculatePrices(reqJson,resJson,resClientComJson,resSupplierComJson,SI2BRMSRoomMap,true);
			pushSuppFaresToRedisAndRemove(reqJson,resJson);

			return resJson.toString();

		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}

	}
	private static void pushSuppFaresToRedisAndRemove(JSONObject reqJson,JSONObject resJson) throws Exception {

		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		JSONArray multiResJsonArr = resBodyJson.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		JSONArray multiReqJsonArr = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ACCOMODATIONARR);
		Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();
		String redisReqKey="",redisRoomKey="";
		for(int j=0; j < multiResJsonArr.length(); j++) {

			JSONArray roomStayJsonArr = ((JSONObject) multiResJsonArr.get(j)).optJSONArray(JSON_PROP_ROOMSTAYARR);
			if (roomStayJsonArr == null) {
				// TODO: This should never happen. Log a warning message here.
				return;
			}
			redisReqKey =getRedisKeyForReq((JSONObject) multiReqJsonArr.get(j));
			for (int i=0; i < roomStayJsonArr.length(); i++) {
				JSONObject roomStayJson = roomStayJsonArr.getJSONObject(i);
				Object suppTotalPriceInfoJson = roomStayJson.remove(JSON_PROP_SUPPROOMPRICE);
				Object suppNghtlyPriceInfoJson = roomStayJson.remove(JSON_PROP_SUPPNIGHTLYPRICEARR);
				Object TotalPriceInfoJson = roomStayJson.getJSONObject(JSON_PROP_ROOMPRICE);//ADDED FOR TOTAL PRICE
				Object clientCommercialsInfoJson = roomStayJson.remove(JSON_PROP_CLIENTENTITYCOMMS);
				Object supplierCommercialsInfoJson = roomStayJson.remove(JSON_PROP_SUPPCOMM);


				if ( suppTotalPriceInfoJson == null || suppNghtlyPriceInfoJson == null) {
					// TODO: This should never happen. Log a warning message here.
					//continue;
					suppTotalPriceInfoJson = roomStayJson.getJSONObject(JSON_PROP_ROOMPRICE);
					suppNghtlyPriceInfoJson = roomStayJson.getJSONArray(JSON_PROP_NIGHTLYPRICEARR);
				}

				JSONObject suppPriceBookInfoJson = new JSONObject();
				suppPriceBookInfoJson.put(JSON_PROP_SUPPROOMPRICE, suppTotalPriceInfoJson);
				suppPriceBookInfoJson.put(JSON_PROP_SUPPNIGHTLYPRICEARR, suppNghtlyPriceInfoJson);
				suppPriceBookInfoJson.put(JSON_PROP_CLIENTCOMMENTITYDTLS, clientCommercialsInfoJson);
				suppPriceBookInfoJson.put(JSON_PROP_SUPPCOMM, supplierCommercialsInfoJson);
				suppPriceBookInfoJson.put(JSON_PROP_ROOMPRICE, TotalPriceInfoJson);
				
				redisRoomKey = redisReqKey.concat(getRedisKeyForRoomStay(roomStayJson.getJSONObject(JSON_PROP_ROOMINFO)));
				if(reprcSuppFaresMap.containsKey(redisRoomKey)) {
					//If this happens ask SI to make response unique on this key
					logger.error(String.format("[Overriding Key:%s,SubResponseIndex:%d,RoomIndex:%d]Prices cannot be cached in Redis as keys formed are not unique",redisRoomKey,j,i));
					//TODO:add a return instead.This is done for testing
					throw new Exception(String.format("[Overriding Key:%s,SubResponseIndex:%d,RoomIndex:%d]Prices cannot be cached in Redis as keys formed are not unique",redisRoomKey,j,i));
				}
				reprcSuppFaresMap.put(redisRoomKey, suppPriceBookInfoJson.toString());

			}
		}

		String redisKey = String.format("%s%c%s",resHeaderJson.optString(JSON_PROP_SESSIONID),KEYSEPARATOR,PRODUCT_ACCO);
		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		redisConn.hmset(redisKey, reprcSuppFaresMap);
		redisConn.pexpire(redisKey, (long) (AccoConfig.getRedisTTLMins() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);

	}

    static String getRedisKeyForReq( JSONObject subReqJson) {

		return String.format("%s%c%s%c%s%c%s%c%s",subReqJson.getString(JSON_PROP_SUPPREF),KEYSEPARATOR,
				subReqJson.getString(JSON_PROP_COUNTRYCODE),KEYSEPARATOR,subReqJson.getString(JSON_PROP_CITYCODE),KEYSEPARATOR,
				subReqJson.getString(JSON_PROP_CHKIN),KEYSEPARATOR,subReqJson.getString(JSON_PROP_CHKOUT));
	}

	static String getRedisKeyForRoomStay(JSONObject roomInfoJson) {
		//TODO:should supplier ref be present or indexes/uuid should be used?
		//TODO:add req params here
		return String.format("%c%s%c%s%c%s%c%s%c%d%c%s",KEYSEPARATOR,roomInfoJson.getJSONObject(JSON_PROP_HOTELINFO).getString(JSON_PROP_HOTELCODE),KEYSEPARATOR,
				roomInfoJson.getJSONObject(JSON_PROP_ROOMTYPEINFO).getString(JSON_PROP_ROOMTYPECODE),KEYSEPARATOR,roomInfoJson.getJSONObject(JSON_PROP_ROOMTYPEINFO).getString(JSON_PROP_ROOMCATEGCODE),
				KEYSEPARATOR,roomInfoJson.getJSONObject(JSON_PROP_RATEPLANINFO).getString(JSON_PROP_RATEPLANCODE),KEYSEPARATOR,roomInfoJson.getInt(JSON_PROP_ROOMINDEX),KEYSEPARATOR,roomInfoJson.getJSONObject(JSON_PROP_ROOMTYPEINFO).getString(JSON_PROP_ROOMREF));
	}

	static String getRedisKeyForRoomPrice(JSONObject subReqJson,JSONObject roomInfoJson) {
		return String.format("%s%s", getRedisKeyForReq(subReqJson),getRedisKeyForRoomStay(roomInfoJson));
	}


}
