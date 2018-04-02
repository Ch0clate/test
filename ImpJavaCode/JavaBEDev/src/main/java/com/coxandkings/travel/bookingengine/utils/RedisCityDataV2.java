package com.coxandkings.travel.bookingengine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;


import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import redis.clients.jedis.Jedis;

public class RedisCityDataV2 implements Constants {

	private static final Logger logger = LogManager.getLogger(RedisCityDataV2.class);
	private static final String REDIS_KEY_CITYCODEDATA = "be:common:citycode";
	private static final String REDIS_KEY_CITYNAMEDATA = "be:common:cityname";
	public static final String COLL_ANCILLARY_DATA = "ancillaryData";
	
	public static void loadConfig() {
		Document data = null;
		long startTime = System.currentTimeMillis();
		try ( Jedis jedis = RedisConfig.getRedisConnectionFromPool(); ) {
			
			List<Document> cityDataDocs = getConfig();
			
			Map<String, String> cityCodeMap = new HashMap<String, String>();
			Map<String, String> cityNameMap = new HashMap<String, String>();
			for (Document cityDataDoc : cityDataDocs) {
				data = (Document) cityDataDoc.get(MDM_PROP_DATA);
				if (data == null) {
					continue;
				}
				
				String dataJSONStr = data.toJson();
				if (data.containsKey(MDM_PROP_CODE)) {
					cityCodeMap.put(data.getString(MDM_PROP_CODE), dataJSONStr);
				}

				if (data.containsKey(MDM_PROP_VALUE)) {
					cityNameMap.put(data.getString(MDM_PROP_VALUE), dataJSONStr);
				}
			}
			
			if (cityCodeMap.size() > 0) {
				jedis.hmset(REDIS_KEY_CITYCODEDATA, cityCodeMap);
			}
			if (cityNameMap.size() > 0) {
				jedis.hmset(REDIS_KEY_CITYNAMEDATA, cityNameMap);
			}
		}
		catch (Exception x) {
			logger.warn("An exception occurred when loading city data", x);
		}
		
		logger.info("Redis City Data push time: " + (System.currentTimeMillis() - startTime));
	}
	 
	 
	public static List<Document> getConfig() {
		MongoCollection<Document> mAncillaryData = MDMConfig.getCollection(COLL_ANCILLARY_DATA);
		long startTime = System.currentTimeMillis();

		ArrayList<Document> result = new ArrayList<Document>();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(MDM_PROP_ANCILLARYTYPE, MDM_VAL_CITY);
		Document findDoc = new Document(props);
		FindIterable<Document> res = mAncillaryData.find(findDoc);
		MongoCursor<Document> cityDataDocs = res.iterator();
		while (cityDataDocs.hasNext()) {
			result.add(cityDataDocs.next());
		}
		cityDataDocs.close();
		logger.info(String.format("Mongo City Data fetch time = %dms", (System.currentTimeMillis() - startTime)));
		return result;
	}	 
	 
	public static Map<String, Object> getCityInfo(String cityName) {
		Map<String,Object> cityAttrs = new HashMap<String,Object>();
		if (cityName == null) {
			return cityAttrs;
		}
		
		try ( Jedis redisConn = RedisConfig.getRedisConnectionFromPool(); ) {
			String cityNameData = redisConn.hget(REDIS_KEY_CITYNAMEDATA, cityName);
			return Document.parse((cityNameData != null) ? cityNameData : JSON_OBJECT_EMPTY);
		}
	}
	 
	public static String getCityInfo(String cityName, String key) {
		return (cityName == null || key == null) ? "" : getCityInfo(cityName).getOrDefault(key, "").toString();
	}
	
	public static Map<String, Object> getCityCodeInfo(String cityCode) {
		Map<String, Object> cityAttrs = new HashMap<String, Object>();
		if (cityCode == null) {
			return cityAttrs;
		}
		
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			String cityCodeData = redisConn.hget(REDIS_KEY_CITYCODEDATA, cityCode);
			return Document.parse((cityCodeData != null) ? cityCodeData : JSON_OBJECT_EMPTY);
		}
	}
	 
	public static String getCityCodeInfo(String cityCode, String key) {
		return (cityCode == null || key == null) ? "" : getCityCodeInfo(cityCode).getOrDefault(key, "").toString();
	}
}
