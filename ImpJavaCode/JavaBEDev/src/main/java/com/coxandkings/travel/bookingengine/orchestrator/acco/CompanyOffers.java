package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.utils.RedisCityData;
import com.coxandkings.travel.bookingengine.utils.RedisHotelData;

public class CompanyOffers implements AccoConstants {

	private static final Logger logger = LogManager.getLogger(CompanyOffers.class);

	public static JSONObject getCompanyOffers(JSONObject req, JSONObject res, JSONObject clientCommResJson,
			Map<String, JSONObject> BRMS2SIRoomMap) {
		JSONObject companyDtlsJson, clientDtlsjson, commonElemJson, productDtlObj, ccommHtlDtlsJson, subReqBody,
				resRoomStayJson, comOfferReqJson, briJsonObj, briJson,roomDtlsjsonObj,RoomStayJson,passDtlObj;
		JSONArray ccommHtlDtlsJsonArr, ccommRoomDtlsJsonArr, productDtlsArr, roomDtlArr;
		OffersConfig offConfig = AccoConfig.getOffersConfig();
		CommercialTypeConfig commTypeConfig = offConfig.getOfferTypeConfig(OffersConfig.Type.COMPANY_SEARCH_TIME);
		comOfferReqJson = new JSONObject(commTypeConfig.getRequestJSONShell());

		JSONObject reqHeader = req.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBody = req.getJSONObject(JSON_PROP_REQBODY);

		JSONObject rootJson = comOfferReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object").getJSONObject("cnk.accomodation_companyoffers.withoutredemption.Root");

		JSONArray briArr = clientCommResJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES)
				.getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject(JSON_PROP_VALUE)
				.getJSONObject(JSON_PROP_CLIENTTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);

		JSONArray briJsonArr = new JSONArray();
		//HashMap<String, List<JSONObject>> suppHotelResMap = new HashMap<String, List<JSONObject>>();
		//JSONArray multiResArr = resBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		//for (int i = 0; i < multiResArr.length(); i++) {
			 for (int i = 0; i < briArr.length(); i++) {
			briJson = (JSONObject) briArr.get(i);
			ccommHtlDtlsJsonArr = briJson.getJSONArray(JSON_PROP_HOTELDETAILS);

			subReqBody = reqBody.has(JSON_PROP_ACCOMODATIONARR)
					? (JSONObject) reqBody.getJSONArray(JSON_PROP_ACCOMODATIONARR).get(i)
					: reqBody;
			briJsonObj = new JSONObject();

			companyDtlsJson = new JSONObject();
			companyDtlsJson.put("sbu", "abc");
			companyDtlsJson.put("bu", "Marketing");
			companyDtlsJson.put("division", "E");
			companyDtlsJson.put("salesOfficeLocation", "Pune");
			companyDtlsJson.put("salesOffice", "Akbar Travels");
			companyDtlsJson.put("companyMarket",
					reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientMarket"));

			clientDtlsjson = new JSONObject();
			clientDtlsjson.put("clientCategory", "Travel Agent");
			clientDtlsjson.put("clientSubCategory", "Franchisee");
			clientDtlsjson.put("clientName", "Akbar Travels");
			clientDtlsjson.put("clientGroup", "B2B Agents");
			clientDtlsjson.put("clientType", "B2B");
			clientDtlsjson.put("nationality", "India");
			clientDtlsjson.put("pointOfSale", "kolkata");

			commonElemJson = new JSONObject();
			commonElemJson.put("bookingDate", mDateFormat.format(new Date()));
			commonElemJson.put("checkInDate", subReqBody.getString(JSON_PROP_CHKIN).concat("T00:00:00"));
			commonElemJson.put("checkOutDate", subReqBody.getString(JSON_PROP_CHKOUT).concat("T00:00:00"));
			commonElemJson.put("firstBookingOnSite", true);

			/*JSONObject accoInfoJson = (JSONObject) multiResArr.get(i);
			JSONArray roomStayArr = accoInfoJson.getJSONArray("roomStay");
			for (int j = 0; j < roomStayArr.length(); j++) {
				JSONObject roomStayjson = roomStayArr.getJSONObject(j);
				String hotelCode = roomStayjson.getJSONObject("roomInfo").getJSONObject("hotelInfo")
						.getString("hotelCode"); // System.out.println("hotelcode "+hotelCode);
				if (!suppHotelResMap.containsKey(hotelCode))
					suppHotelResMap.put(hotelCode, new ArrayList<JSONObject>());
				suppHotelResMap.get(hotelCode).add(roomStayjson);
			}

			roomDtlArr = new JSONArray();
			productDtlsArr = new JSONArray();

			String cityName;
			Map<String, String> cityAttrs, hotelAttrs;
			for (Entry<String, List<JSONObject>> entry : suppHotelResMap.entrySet()) {
				hotelAttrs = RedisHotelData.getHotelInfo(entry.getKey());
				cityName = RedisHotelData.getHotelInfo(entry.getKey(), "city");
				cityAttrs = RedisCityData.getCityInfo(cityName);
				productDtlObj = new JSONObject();
				productDtlObj.put("productCategory", "Accomodation");
				productDtlObj.put("productCategorySubType", PRODUCT_NAME_BRMS);
				productDtlObj.put("brand", hotelAttrs.getOrDefault("brand", ""));
				productDtlObj.put("chain", hotelAttrs.getOrDefault("chain", ""));
				productDtlObj.put("city", cityName);
				productDtlObj.put("country", cityAttrs.getOrDefault("country", ""));
				productDtlObj.put("productName", hotelAttrs.getOrDefault("name", ""));
				productDtlObj.put("noOfNights", 2);

				roomDtlsArr = new JSONArray();
				roomDtlsjsonObj=new JSONObject();
				for (JSONObject valueList : entry.getValue()) {
					roomDtlsjsonObj.put("roomCategory", valueList.getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomCategoryName"));
					roomDtlsjsonObj.put("roomType", valueList.getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomTypeName"));
					JSONArray nightlyprcArr=new JSONArray();
					JSONArray fareDtlsArr=new JSONArray();
					for(Object nightprc:valueList.getJSONArray("nightlyPriceInfo")) {
						JSONObject nightlyObj=new JSONObject();
						nightlyObj.put("effectiveDate", ((JSONObject) nightprc).getString("effectiveDate").concat("T00:00:00"));
						nightlyObj.put("fareDetails", fareDtlsArr);
						nightlyprcArr.put(nightlyObj);
					}
					roomDtlsjsonObj.put("nightDetails", nightlyprcArr);
					roomDtlsArr.put(roomDtlsjsonObj);
				}
				productDtlObj.put("roomDetails", roomDtlsArr);
				productDtlsArr.put(productDtlObj);
				System.out.println("arr" + productDtlsArr);
			}*/
			  String cityName;
			  Map<String, String> cityAttrs, hotelAttrs;
			  productDtlsArr = new JSONArray();
			  for (int j = 0;j < ccommHtlDtlsJsonArr.length(); j++) {
			  roomDtlArr=new JSONArray();
			  RoomStayJson = BRMS2SIRoomMap.get(String.format("%s%c%s%c%s", i,KEYSEPARATOR,j,KEYSEPARATOR,0));//k=0 because we take the first room of the hotel to fetch the hotelCode
			  ccommHtlDtlsJson = ccommHtlDtlsJsonArr.getJSONObject(j);
			  ccommRoomDtlsJsonArr =ccommHtlDtlsJson.getJSONArray(JSON_PROP_ROOMDETAILS);
			  String hotelCode=RoomStayJson.getJSONObject(JSON_PROP_ROOMINFO).getJSONObject(JSON_PROP_HOTELINFO).getString(JSON_PROP_HOTELCODE);
			  hotelAttrs = RedisHotelData.getHotelInfo(hotelCode);
			  cityName = RedisHotelData.getHotelInfo(hotelCode, "city");
			  cityAttrs = RedisCityData.getCityInfo(cityName);
			  productDtlObj = new JSONObject();
			  productDtlObj.put("productCategory", "Accomodation");
			  productDtlObj.put("productCategorySubType", PRODUCT_NAME_BRMS);
			  productDtlObj.put("brand", hotelAttrs.getOrDefault("brand", ""));
			  productDtlObj.put("chain", hotelAttrs.getOrDefault("chain", ""));
			  productDtlObj.put("city", cityName);
			  productDtlObj.put("country", cityAttrs.getOrDefault("country", ""));
			  productDtlObj.put("productName", hotelAttrs.getOrDefault("name", ""));
			  productDtlObj.put("noOfNights", "");
			  
			  for(int k=0;k<ccommRoomDtlsJsonArr.length();k++)
			  { 
			roomDtlsjsonObj=new JSONObject();
			  resRoomStayJson =BRMS2SIRoomMap.get(String.format("%s%c%s%c%s", i,KEYSEPARATOR,j,KEYSEPARATOR,k));//Sada wala room
			  roomDtlsjsonObj.put("roomCategory", resRoomStayJson.getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomCategoryName"));
				roomDtlsjsonObj.put("roomType", resRoomStayJson.getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomTypeName"));
				JSONArray nightlyprcArr=new JSONArray();
				JSONArray fareDtlsArr=new JSONArray();
				for(Object nightprc:resRoomStayJson.getJSONArray("nightlyPriceInfo")) {
					JSONObject nightlyObj=new JSONObject();
					nightlyObj.put("effectiveDate", ((JSONObject) nightprc).getString("effectiveDate").concat("T00:00:00"));
					nightlyObj.put("fareDetails", fareDtlsArr);
					nightlyprcArr.put(nightlyObj);
				}
				roomDtlsjsonObj.put("nightDetails", nightlyprcArr);
				JSONArray passDtlsArr=new JSONArray();
				int roomNo=resRoomStayJson.getJSONObject("roomInfo").getInt("requestedRoomIndex");
				JSONObject room = (JSONObject) reqBody.getJSONArray("roomConfig").get(roomNo-1);
				for(int h=0;h<room.getInt("adultCount");h++) {
					passDtlObj=new JSONObject();
					passDtlObj.put("passengerType", "ADT");	
					passDtlsArr.put(passDtlObj);
				}
				JSONArray roomChld = room.getJSONArray("childAges");
				for(int t=0;t<roomChld.length();t++) {
					passDtlObj=new JSONObject();
					passDtlObj.put("passengerType", "CHD");
					passDtlObj.put("age", roomChld.get(t));
					passDtlsArr.put(passDtlObj);
				}
				roomDtlsjsonObj.put("passengerDetails", passDtlsArr);
				roomDtlArr.put(roomDtlsjsonObj); 
			  }
			  productDtlObj.put("roomDetails",roomDtlArr);
			  productDtlsArr.put(productDtlObj);
			  }
			briJsonObj.put("companyDetails", companyDtlsJson);
			briJsonObj.put("clientDetails", clientDtlsjson);
			briJsonObj.put("commonElements", commonElemJson);
			briJsonObj.put("productDetails", productDtlsArr);
			
			briJsonArr.put(briJsonObj);

		}
		
		rootJson.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);
		System.out.println("companyOffers request" + comOfferReqJson);
		JSONObject comOfferResJson = null;
		try {
			logger.info("Before opening HttpURLConnection to BRMS for Supplier Commercials");
			comOfferResJson = HTTPServiceConsumer.consumeJSONService("BRMS", commTypeConfig.getServiceURL(),
					offConfig.getHttpHeaders(), comOfferReqJson);
			logger.info("HttpURLConnection to BRMS closed");
		} catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
        System.out.println("companyOffers response"+comOfferResJson);
		
		 return null;
	}

}
