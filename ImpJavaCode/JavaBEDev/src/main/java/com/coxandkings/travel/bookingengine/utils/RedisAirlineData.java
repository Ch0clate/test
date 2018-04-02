package com.coxandkings.travel.bookingengine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import redis.clients.jedis.Jedis;

public class RedisAirlineData implements Constants{

	private static final Logger logger = LogManager.getLogger(RedisAirlineData.class);
	private static final String REDIS_KEY_AIRLINEDATA = "be:air:airline";
	public static final String AIRLINE_CODE = "code";
	public static final String AIRLINE_NAME = "name";
	public static final String AIRLINE_TYPE = "value";

	public static void loadConfig() {
		long startTime = System.currentTimeMillis();
		
		try ( Jedis jedis = RedisConfig.getRedisConnectionFromPool(); ) {
			ArrayList<Document> airlineDtlsDocs = getConfig();
			
			Map<String, String> airlineMap = new HashMap<String, String>();
			Document data = null;
			for (Document airportDataDoc : airlineDtlsDocs) {
				data = (Document) airportDataDoc.get(MDM_PROP_DATA);
				if (data != null && data.containsKey(MDM_PROP_CODE)) {
					airlineMap.put(data.getString(MDM_PROP_CODE), data.toJson());
				}
			}
			
			jedis.hmset(REDIS_KEY_AIRLINEDATA, airlineMap);
		}
		catch (Exception x) {
			logger.warn("An exception occurred when loading airline details", x);
		}
		
		logger.info("Redis Airline Details push time: " + (System.currentTimeMillis() - startTime));
	}
	 
	public static ArrayList<Document> getConfig() {
		MongoCollection<Document> mAncillaryData = MDMConfig.getCollection(MDM_COLL_ANCILLARYDATA);
		long startTime = System.currentTimeMillis();

		ArrayList<Document> result = new ArrayList<Document>();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(MDM_PROP_DELETED, false);
		props.put(MDM_PROP_ANCILLARYTYPE, MDM_VAL_AIRLINEDETAILS);
		Document findDoc = new Document(props);
		FindIterable<Document> res = mAncillaryData.find(findDoc);
		MongoCursor<Document> airlineDtlsDocs = res.iterator();
		while (airlineDtlsDocs.hasNext()) {
			result.add(airlineDtlsDocs.next());
		}
		airlineDtlsDocs.close();
		logger.debug(String.format("Mongo Airline Details fetch time = %dms", (System.currentTimeMillis() - startTime)));
		return result;
	}	 
	 
	public static Map<String, Object> getAirlineDetails(String airlineCode) {
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool(); ) {
			String airlineAttrs = redisConn.hget(REDIS_KEY_AIRLINEDATA, airlineCode);
			return Document.parse((airlineAttrs != null) ? airlineAttrs : JSON_OBJECT_EMPTY);
		}
	}
	 
	public static String getAirlineDetails(String airlineCode, String key) {
		return getAirlineDetails(airlineCode).getOrDefault(key, "").toString();
	}

}

