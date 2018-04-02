package com.coxandkings.travel.bookingengine.utils;

//import java.util.ArrayList;
//import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCursor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class RedisHotelData {

	private static final Logger logger = LogManager.getLogger(RedisCityData.class);
	public static final String COLL_HOTEL_DATA = "productAccomodations";
	
	public static void load() {
		insertHotelInfo();
	}
	 
	 
	public static FindIterable<Document> getConfig() {
		MongoCollection<Document> mHotelData = MDMConfig.getCollection(COLL_HOTEL_DATA);
		FindIterable<Document> hotelDataDocs = mHotelData.find().batchSize(50000);
		return hotelDataDocs;
	}	 
	 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertHotelInfo() {
		long startTime = System.currentTimeMillis();
		//Jedis jedis = null;
		//Pipeline pipeline = null;
		
		// TODO: Change the try block to try-with-resources
		try ( Jedis jedis = RedisConfig.getRedisConnectionFromPool();
			  Pipeline pipeline = jedis.pipelined(); ) {
			FindIterable<Document> hotelDataDocs = getConfig();
			Document doc = null;
			Map<String,String> data = new HashMap<String,String>();
			for (Document hotelDataDoc : hotelDataDocs) {
				doc = (Document) hotelDataDoc.get("accomodationInfo");
				if (doc != null && doc.containsKey("commonProductId")) {
					data.put("name",  doc.containsKey("name")?doc.getString("name"):"");
					data.put("brand", doc.containsKey("brand")?doc.getString("brand"):"");
					data.put("chain",  doc.containsKey("chain")?doc.getString("chain"):"");
					if(doc.containsKey("address")) {
						Document address = (Document) doc.get("address");
						data.put("city", address.containsKey("city")?address.getString("city"):"");
					}
					else {
						data.put("city", "");
					}
					pipeline.hmset(doc.getString("commonProductId"),data);
				}
			}
	
			pipeline.sync();
		}
		catch (Exception x) {
			logger.warn("An exception occurred when loading hotel data", x);
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
		logger.info("Redis Hotel Data push time: " + (System.currentTimeMillis() - startTime));
	}
	
	public static Map<String, String> getHotelInfo(String hotelCode) {
		Map<String, String> hotelAttrs = new HashMap<String, String>();
		if(hotelCode==null)
			return hotelAttrs;
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool(); ) {
			hotelAttrs = redisConn.hgetAll(hotelCode);
			//RedisConfig.releaseRedisConnectionToPool(redisConn);
			return hotelAttrs;
		}
	}
	 
	public static String getHotelInfo(String hotelCode, String key) {
		if(hotelCode==null || key==null)
			return "";
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			String hotelAttr = redisConn.hget(hotelCode, key);
			//RedisConfig.releaseRedisConnectionToPool(redisConn);
			return hotelAttr;
		}
	}
}
