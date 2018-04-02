package com.coxandkings.travel.bookingengine.utils;

import java.util.ArrayList;
import java.util.HashMap;
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
import redis.clients.jedis.Pipeline;

@Deprecated
public class RedisCityData {

	private static final Logger logger = LogManager.getLogger(RedisCityData.class);
	public static final String COLL_ANCILLARY_DATA = "ancillaryData";
	
	public static void load() {
		insertCityInfo();
	}
	 
	 
	public static ArrayList<Document> getConfig() {
		MongoCollection<Document> mAncillaryData = MDMConfig.getCollection(COLL_ANCILLARY_DATA);
		long startTime = System.currentTimeMillis();

		ArrayList<Document> result = new ArrayList<Document>();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("ancillaryType", "city");
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
	 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertCityInfo() {
		long startTime = System.currentTimeMillis();
		//Jedis jedis = null;
		//Pipeline pipeline = null;
		
		// TODO: Change the try block to try-with-resources
		try ( Jedis jedis = RedisConfig.getRedisConnectionFromPool();
				Pipeline pipeline = jedis.pipelined(); ) {
			
			ArrayList<Document> cityDataDocs = getConfig();
			
			
			Document data = null;
			for (Document cityDataDoc : cityDataDocs) {
				data = (Document) cityDataDoc.get("data");
				if (data != null && data.containsKey("value")) {
					pipeline.hmset(data.getString("value"), (Map) data);
				}
				if(data != null && data.containsKey("code")) {
					pipeline.hmset(data.getString("code"), (Map) data);
				}
			}
	
			pipeline.sync();
		}
		catch (Exception x) {
			logger.warn("An exception occurred when loading city data", x);
		}
//		finally {
//			if (pipeline != null) {
//				try { 
//					pipeline.close(); 
//				}
//				catch (Exception x) { 
//					// TODO: Check if it is safe to eat this exception 
//				}
//			}
//			if (jedis != null) {
//				RedisConfig.releaseRedisConnectionToPool(jedis);
//			}
//		}
		
		logger.info("Redis City Data push time: " + (System.currentTimeMillis() - startTime));
	}
	
	public static Map<String, String> getCityInfo(String cityName) {
		Map<String, String> cityAttrs = new HashMap<String, String>();
		if(cityName==null)
			return cityAttrs;
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			cityAttrs = redisConn.hgetAll(cityName);
			//RedisConfig.releaseRedisConnectionToPool(redisConn);
			return cityAttrs;
		}
	}
	 
	public static String getCityInfo(String cityName, String key) {
		if(cityName==null || key==null)
			return "";
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			String cityAttr = redisConn.hget(cityName, key);
			//RedisConfig.releaseRedisConnectionToPool(redisConn);
			return cityAttr;
		}
	}
	
	public static Map<String, String> getCityCodeInfo(String cityCode) {
		Map<String, String> cityAttrs = new HashMap<String, String>();
		if(cityCode==null)
			return cityAttrs;
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			cityAttrs = redisConn.hgetAll(cityCode);
			//RedisConfig.releaseRedisConnectionToPool(redisConn);
			return cityAttrs;
		}
	}
	 
	public static String getCityCodeInfo(String cityCode, String key) {
		if(cityCode==null || key==null)
			return "";
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			String cityAttr = redisConn.hget(cityCode, key);
			//RedisConfig.releaseRedisConnectionToPool(redisConn);
			return cityAttr;
		}
	}
}
