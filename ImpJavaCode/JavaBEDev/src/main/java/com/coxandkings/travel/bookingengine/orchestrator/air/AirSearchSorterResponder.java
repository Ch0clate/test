package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;

public class AirSearchSorterResponder implements AirConstants {
	
	private static final DecimalFormat decFmt = new DecimalFormat("000000000.000000"); 
	private static final Logger logger = LogManager.getLogger(AirSearchSorterResponder.class);
	
	private static JSONArray sortItinsByPrice(List<JSONArray> itinsList) {
		Map<String, JSONObject> pricedItinsSorterMap = new TreeMap<String, JSONObject>();
		StringBuilder strBldr = new StringBuilder();
		for (JSONArray itins : itinsList) {
			for (int i=0; i < itins.length(); i++) {
				JSONObject pricedItin = itins.getJSONObject(i);
				JSONObject pricingInfoJson = pricedItin.getJSONObject(JSON_PROP_AIRPRICEINFO);
				JSONObject itinTotalFareJson = pricingInfoJson.getJSONObject(JSON_PROP_ITINTOTALFARE);
				
				strBldr.setLength(0);
				strBldr.append(decFmt.format(itinTotalFareJson.getDouble(JSON_PROP_AMOUNT)));
				strBldr.append(String.format("%-15s", pricedItin.getString(JSON_PROP_SUPPREF)));
				strBldr.append(AirRepriceProcessor.getRedisKeyForPricedItinerary(pricedItin));
				pricedItinsSorterMap.put(strBldr.toString(), pricedItin);
			}
		}
		
		JSONArray pricedItinsJsonArr = new JSONArray();
		Iterator<Entry<String, JSONObject>> pricedItinsIter = pricedItinsSorterMap.entrySet().iterator();
		while (pricedItinsIter.hasNext()) {
			Entry<String, JSONObject> pricedItinEntry = pricedItinsIter.next();
			pricedItinsJsonArr.put(pricedItinEntry.getValue());
		}
		
		return pricedItinsJsonArr;
	}
	
	public static void respondToCallback(URL callbackURL, JSONObject reqHdrJson, List<JSONArray> itinsList) {
		JSONObject resJson = getSortedResponse(reqHdrJson, itinsList);
		Map<String, String> httpHdrs = new HashMap<String, String>();
		httpHdrs.put(HTTP_HEADER_CONTENT_TYPE, HTTP_CONTENT_TYPE_APP_JSON);
		try {
			HTTPServiceConsumer.consumeJSONService("CallBack", callbackURL, httpHdrs, resJson);
		}
		catch (Exception x) {
			logger.warn(String.format("An exception occurred while responding to callback address %s", callbackURL.toString()), x);
		}
	}
	
	static JSONObject getSortedResponse(JSONObject reqHdrJson, List<JSONArray> itinsList) {
		JSONObject resBodyJson = new JSONObject();
		JSONArray itinsJsonArr = sortItinsByPrice(itinsList);
		resBodyJson.put(JSON_PROP_PRICEDITIN, itinsJsonArr);
		
		JSONObject resJson = new JSONObject();
		resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
		resJson.put(JSON_PROP_RESBODY, resBodyJson);

		return resJson;
	}

}
