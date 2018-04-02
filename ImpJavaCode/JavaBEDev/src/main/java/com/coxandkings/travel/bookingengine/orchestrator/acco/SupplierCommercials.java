package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoSearchProcessor;
import com.coxandkings.travel.bookingengine.utils.RedisCityData;
import com.coxandkings.travel.bookingengine.utils.RedisHotelData;

public class SupplierCommercials implements AccoConstants{

	private static final Logger logger = LogManager.getLogger(AccoSearchProcessor.class);

	public static JSONObject getSupplierCommercials(JSONObject req, JSONObject res, Map<Integer,String> SI2BRMSRoomMap) {
		
		CommercialsConfig commConfig = AccoConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_SUPPLIER_TRANSACTIONAL);
		JSONObject breSuppReqJson = new JSONObject(commTypeConfig.getRequestJSONShell());

		JSONObject reqHeader = req.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBody = req.getJSONObject(JSON_PROP_REQBODY);

		JSONObject resHeader = res.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBody = res.getJSONObject(JSON_PROP_RESBODY);

		JSONObject breHdrJson = new JSONObject();
		breHdrJson.put(JSON_PROP_SESSIONID, resHeader.getString(JSON_PROP_SESSIONID));
		breHdrJson.put(JSON_PROP_TRANSACTID, resHeader.getString(JSON_PROP_TRANSACTID));
		breHdrJson.put(JSON_PROP_USERID, resHeader.getString(JSON_PROP_USERID));
		breHdrJson.put("operationName", "Search");

		JSONObject rootJson = breSuppReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object")
				.getJSONObject("cnk.acco_commercialscalculationengine.suppliertransactionalrules.Root");
		
		JSONArray briJsonArr = new JSONArray();
		rootJson.put("header", breHdrJson);
		rootJson.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);
		
		int briIndex=0,hotelIndex = 0;
		String prevSuppId,suppId,cityName,hotelCode;
		JSONObject briJson = null,commonElemsJson,advDefnJson,hotelDetailsJson,roomDetailsJson,subReqBody,accoInfoJson;
		Map<String, String> cityAttrs,hotelAttrs;
		Map<String, JSONObject> hotelMap = null;
		Map<String, Integer> hotelIndexMap = null;
		JSONArray multiResArr = resBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		int roomIdx=0;
		
		for(int i=0;i<multiResArr.length();i++) {
			
			accoInfoJson = (JSONObject) multiResArr.get(i);
			subReqBody = reqBody.has(JSON_PROP_ACCOMODATIONARR)?(JSONObject) reqBody.getJSONArray(JSON_PROP_ACCOMODATIONARR).get(i):reqBody;
			//briIndex = briJsonArr.length();
			prevSuppId = DEF_SUPPID;
			
			for(Object roomStayJson: accoInfoJson.getJSONArray(JSON_PROP_ROOMSTAYARR)) {
				
				suppId = ((JSONObject)roomStayJson).getString(JSON_PROP_SUPPREF);
				hotelCode = ((JSONObject) ((JSONObject) ((JSONObject)roomStayJson).get(JSON_PROP_ROOMINFO)).get(JSON_PROP_HOTELINFO)).getString(JSON_PROP_HOTELCODE);
				
				if(!prevSuppId.equals(suppId)) {
					
					prevSuppId = suppId;
					hotelMap = new HashMap<String,JSONObject>();
					hotelIndexMap = new HashMap<String,Integer>();
					hotelIndex = 0;
					briJson = new JSONObject();
					
					commonElemsJson = new JSONObject();
					commonElemsJson.put("supplier", suppId);
	                // TODO: Supplier market is hard-coded below. Where will this come from?
					commonElemsJson.put("supplierMarket", "India");
					commonElemsJson.put("contractValidity", mDateFormat.format(new Date()));
					//TODO:get from enum
					commonElemsJson.put("productCategorySubType", PRODUCT_NAME_BRMS);
					// TODO: Hard-coded value. This should be set from client context.Check if
					// required in acco or no
					// commonElemsJson.put("clientType", "B2C");
					// TODO: Properties for clientGroup, clientName, iatanumber are not yet set. Are
					// these required for B2C? What will be BRMS behavior if these properties are  not sent.
					
					advDefnJson = new JSONObject();
					advDefnJson.put("travelCheckInDate", subReqBody.getString(JSON_PROP_CHKIN).concat("T00:00:00"));
					advDefnJson.put("travelCheckOutDate", subReqBody.getString(JSON_PROP_CHKOUT).concat("T00:00:00"));
					// TODO: Significance of this data in acco.Is it same as the check in date or
					// date of search
					advDefnJson.put("salesDate", mDateFormat.format(new Date()));
					// TODO: is this the default value for all.Which other values are supported and
					// where will it come from
					advDefnJson.put("bookingType", "Online");
					// TODO: Is this the same as client type.If yes, set from client context
					advDefnJson.put("credentialsName", reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientType"));
					// TODO: Is this expected from req? This data is also needed by some supplier?
					// Will it be provided by wem
					advDefnJson.put("nationality", "Indian");
					// TODO: city country continent and state mapping should be there iin adv def

					//TODO:is empty default requied?data should be present in redis
					cityName = RedisHotelData.getHotelInfo(hotelCode, "city");
					cityAttrs = RedisCityData.getCityInfo(cityName);
					advDefnJson.put("continent", cityAttrs.getOrDefault("continent",""));
					advDefnJson.put("country", cityAttrs.getOrDefault("country",""));
					advDefnJson.put("city", cityName);
					advDefnJson.put("state", cityAttrs.getOrDefault("state",""));

					briJson.put("commonElements", commonElemsJson);
					briJson.put("advancedDefinition", advDefnJson);
					
					briJsonArr.put(briIndex++,briJson);
				}
				
				hotelDetailsJson = hotelMap.get(hotelCode);
				if (hotelDetailsJson == null) {
					
					hotelDetailsJson = new JSONObject();
					
					hotelAttrs = RedisHotelData.getHotelInfo(hotelCode);
					hotelDetailsJson.put("productName", hotelAttrs.getOrDefault("name", ""));
					hotelDetailsJson.put("productBrand", hotelAttrs.getOrDefault("brand", ""));
					hotelDetailsJson.put("productChain", hotelAttrs.getOrDefault("chain", ""));
					
	                briJson.append("hotelDetails", hotelDetailsJson);
					hotelMap.put(hotelCode, hotelDetailsJson);
					hotelIndexMap.put(hotelCode, hotelIndex++);
				}
				
				roomDetailsJson = getBRMSRoomDetailsJSON((JSONObject) roomStayJson);
				hotelDetailsJson.append("roomDetails", roomDetailsJson);
				//this is done so that while calculating prices from client response finding a particular room becomes efficient
				SI2BRMSRoomMap.put(roomIdx++,String.format("%s%c%s%c%s",briIndex-1,KEYSEPARATOR,hotelIndexMap.get(hotelCode),KEYSEPARATOR,hotelDetailsJson.getJSONArray("roomDetails").length()-1));
			}
		}

		
		JSONObject breSuppResJson = null;
		try {
			//long start = System.currentTimeMillis();
			breSuppResJson = HTTPServiceConsumer.consumeJSONService("BRMS-SUPPLIERCOMMERCIALS", commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(),breSuppReqJson);
			//logger.info(String.format("Time taken to get supplier commercials response : %s ms", System.currentTimeMillis()-start));
		} catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
		
		return breSuppResJson;
	}

	public static JSONObject getBRMSRoomDetailsJSON(JSONObject roomStayJson) {
		
		JSONObject roomDetailsJson = new JSONObject();
		JSONObject roomInfoJson = roomStayJson.getJSONObject(JSON_PROP_ROOMINFO);
		
		JSONObject roomTypeJson = roomInfoJson.getJSONObject(JSON_PROP_ROOMTYPEINFO);
		//TODO:should codes come here
		roomDetailsJson.put("roomType",roomTypeJson.get(JSON_PROP_ROOMTYPENAME));
		//TODO:this data is taken from si as it is not found in mat's system.Ask mdm if room name has single mapping to room category
		//If yes take from mdm
		roomDetailsJson.put("roomCategory",roomTypeJson.get(JSON_PROP_ROOMCATEGNAME));
		
		JSONObject ratePlanJson = roomInfoJson.getJSONObject(JSON_PROP_RATEPLANINFO);
		//TODO:Will SI lookup this?If not,where this data will come from
		roomDetailsJson.put("rateType", ratePlanJson.get(JSON_PROP_RATEPLANNAME));
		roomDetailsJson.put("rateCode", ratePlanJson.get(JSON_PROP_RATEPLANCODE));
		//TODO:This hack is rght?
		//roomDetailsJson.put("bookingEngineKey", roomIndex);
		
		// TODO:Yet to add passenger type.If passenger type is taken from req, how to handle multiroom
		// case? ask SI to map in response

		JSONObject totalPriceJson = (JSONObject) roomStayJson.get(JSON_PROP_ROOMPRICE);
		BigDecimal totalPrice = totalPriceJson.getBigDecimal(JSON_PROP_AMOUNT);
		roomDetailsJson.put("totalFare", String.valueOf(totalPrice));

		JSONObject taxesJson = totalPriceJson.optJSONObject(JSON_PROP_TOTALTAX);
		//farebreakup will not be a part of req if null
		if(taxesJson!=null && taxesJson.has(JSON_PROP_AMOUNT)) {
			JSONObject fareBreakupJson = new JSONObject();
			fareBreakupJson.put("baseFare", totalPrice.subtract(taxesJson.getBigDecimal(JSON_PROP_AMOUNT)));
			JSONArray taxArr = taxesJson.optJSONArray(JSON_PROP_TAXBRKPARR);
			if(taxArr!=null) {
				JSONArray taxDetailsArr = new JSONArray();
				JSONObject taxDetailJson;
				for(Object tax:taxArr) {
					taxDetailJson = new JSONObject();
					//TODO:Will SI standardized these codes
					taxDetailJson.put("taxName", ((JSONObject) tax).getString(JSON_PROP_TAXCODE));
					taxDetailJson.put("taxValue", ((JSONObject) tax).getBigDecimal(JSON_PROP_AMOUNT));
					taxDetailsArr.put(taxDetailJson);
				}
				fareBreakupJson.put("taxDetails",taxDetailsArr);
			}
			roomDetailsJson.put("fareBreakUp", fareBreakupJson);
		}

		return roomDetailsJson;

	}
	
	//TODO:can be splitted in two methods and keep indexes as key
	@Deprecated
	public static String getBRMS2SIMapperKey(JSONObject BRMSHotelDetailsJson,JSONObject BRMSRoomDetailsJSON, String suppId) {
		
		return String.format("%s|%s|%s|%s|%s|%s|%s", suppId,BRMSHotelDetailsJson.getString("productName"),
				BRMSHotelDetailsJson.getString("productBrand"),BRMSHotelDetailsJson.getString("productChain"),
				BRMSRoomDetailsJSON.getString("roomType"),BRMSRoomDetailsJSON.getString("roomCategory"),
				BRMSRoomDetailsJSON.getString("rateType"));
	}

	//********************OLD STUFF*************************//
	/*public static JSONObject getSupplierCommercialsCommonJson(JSONObject req, JSONObject res) {
		CommercialsConfig commConfig = AccoConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig
				.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_SUPPLIER_TRANSACTIONAL);
		JSONObject breSuppReqJson = new JSONObject(commTypeConfig.getRequestJSONShell());
		JSONObject resHeader = res.getJSONObject("responseHeader");
		
		JSONObject breHdrJson = new JSONObject();
		breHdrJson.put("sessionID", resHeader.getString("sessionID"));
		breHdrJson.put("transactionID", resHeader.getString("transactionID"));
		breHdrJson.put("userID", resHeader.getString("userID"));
		breHdrJson.put("operationName", "Search");

		JSONObject rootJson = breSuppReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object")
				.getJSONObject("cnk.acco_commercialscalculationengine.suppliertransactionalrules.Root");
		rootJson.put("header", breHdrJson);
		rootJson.put("businessRuleIntake", new JSONArray());
		return breSuppReqJson;
	}*/

	
	/*String suppID,cityName,hotelCode,compositeKey;
	Map<String, JSONObject> Supplier2HotelMap = new HashMap<String, JSONObject>();
	Map<String, JSONObject> Hotel2RoomMap = new HashMap<String, JSONObject>();
	Map<String, String> cityAttrs,hotelAttrs; 
	//TODO:temporary hack of indexing.Can be reverted to map.Also int should be changed to string?
	int roomindex=-1;
	
	for(Object roomStayJson: resBody.getJSONArray("roomStay")) {
		roomindex++;
		suppID = ((JSONObject)roomStayJson).getString("supplierRef");
		hotelCode = ((JSONObject) ((JSONObject) ((JSONObject)roomStayJson).get("roomInfo")).get("hotelInfo")).getString("hotelCode");
		compositeKey = String.format("%s|%s", suppID,hotelCode);
		
		briJson = Supplier2HotelMap.get(suppID);
		if(briJson==null) {
			briJson = new JSONObject();
			commonElemsJson = new JSONObject();
			commonElemsJson.put("supplier", suppID);
            // TODO: Supplier market is hard-coded below. Where will this come from?
			commonElemsJson.put("supplierMarket", "India");
			commonElemsJson.put("contractValidity", mDateFormat.format(new Date()));
			commonElemsJson.put("productCategorySubType", "Hotels");
			// TODO: Hard-coded value. This should be set from client context.Check if
			// required in acco or no
			// commonElemsJson.put("clientType", "B2C");
			// TODO: Properties for clientGroup, clientName, iatanumber are not yet set. Are
			// these required for B2C? What will be BRMS behavior if these properties are  not sent.
			
			advDefnJson = new JSONObject();
			advDefnJson.put("travelCheckInDate", reqBody.getString("checkIn").concat("T00:00:00"));
			advDefnJson.put("travelCheckOutDate", reqBody.getString("checkOut").concat("T00:00:00"));
			// TODO: Significance of this data in acco.Is it same as the check in date or
			// date of search
			advDefnJson.put("salesDate", mDateFormat.format(new Date()));
			// TODO: is this the default value for all.Which other values are supported and
			// where will it come from
			advDefnJson.put("bookingType", "Online");
			// TODO: Is this the same as client type.If yes, set from client context
			advDefnJson.put("credentialsName", reqHeader.getJSONObject("clientContext").getString("clientType"));
			// TODO: Is this expected from req? This data is also needed by some supplier?
			// Will it be provided by wem
			advDefnJson.put("nationality", "Indian");
			// TODO: city country continent and state mapping should be there iin adv def

			//TODO:is empty default requied?data should be present in redis
			cityName = RedisHotelData.getHotelInfo(hotelCode, "city");
			cityAttrs = RedisCityData.getCityInfo(cityName);
			advDefnJson.put("continent", cityAttrs.getOrDefault("continent",""));
			advDefnJson.put("country", cityAttrs.getOrDefault("country",""));
			advDefnJson.put("city", cityName);
			advDefnJson.put("state", cityAttrs.getOrDefault("state",""));

			briJson.put("commonElements", commonElemsJson);
			briJson.put("advancedDefinition", advDefnJson);
			rootJson.append("businessRuleIntake", briJson);
			Supplier2HotelMap.put(suppID, briJson);
		}
		
		hotelDetailsJson = Hotel2RoomMap.get(compositeKey);
		if (hotelDetailsJson == null) {
			hotelDetailsJson = new JSONObject();
			hotelAttrs = RedisHotelData.getHotelInfo(hotelCode);
			hotelDetailsJson.put("productName", hotelAttrs.getOrDefault("name", ""));
			hotelDetailsJson.put("productBrand", hotelAttrs.getOrDefault("brand", ""));
			hotelDetailsJson.put("productChain", hotelAttrs.getOrDefault("chain", ""));
            briJson.append("hotelDetails", hotelDetailsJson);
			Hotel2RoomMap.put(compositeKey, hotelDetailsJson);
		}
		roomDetailsJson = getBRMSRoomDetailsJSON((JSONObject) roomStayJson, roomindex); 
		hotelDetailsJson.append("roomDetails", roomDetailsJson);
		
		//this is done so that while calculating prices from client response finding a particular room becomes efficient
		//BRMS2SIRoomMap.put(count, (JSONObject) roomStayJson);
	}*/
}
