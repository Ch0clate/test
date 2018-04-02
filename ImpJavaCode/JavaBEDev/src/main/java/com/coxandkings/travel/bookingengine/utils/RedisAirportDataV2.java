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

public class RedisAirportDataV2 implements Constants{

	private static final Logger logger = LogManager.getLogger(RedisAirportDataV2.class);
	private static final String REDIS_KEY_AIRPORTDATA = "be:air:airport";
	public static final String AIRPORT_CITY = "city";
	public static final String AIRPORT_COUNTRY = "country";
	public static final String AIRPORT_CONTINENT = "continent";
	public static final String AIRPORT_STATE = "state";

	public static void loadConfig() {
		insertAirportInfo();
	}
	 
	public static ArrayList<Document> getConfig() {
		MongoCollection<Document> mAncillaryData = MDMConfig.getCollection(MDM_COLL_ANCILLARYDATA);
		long startTime = System.currentTimeMillis();

		ArrayList<Document> result = new ArrayList<Document>();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(MDM_PROP_DELETED, false);
		props.put(MDM_PROP_ANCILLARYTYPE, MDM_VAL_AIRPORT);
		Document findDoc = new Document(props);
		FindIterable<Document> res = mAncillaryData.find(findDoc);
		MongoCursor<Document> airportDataDocs = res.iterator();
		while (airportDataDocs.hasNext()) {
			result.add(airportDataDocs.next());
		}
		airportDataDocs.close();
		logger.debug(String.format("Mongo Airport Data fetch time = %dms", (System.currentTimeMillis() - startTime)));
		return result;
	}	 
	 
	public static void insertAirportInfo() {
		long startTime = System.currentTimeMillis();
		
		try ( Jedis jedis = RedisConfig.getRedisConnectionFromPool(); ) {
			ArrayList<Document> airportDataDocs = getConfig();
			
			Map<String, String> airportMap = new HashMap<String, String>();
			Document data = null;
			for (Document airportDataDoc : airportDataDocs) {
				data = (Document) airportDataDoc.get(MDM_PROP_DATA);
				if (data != null && data.containsKey(MDM_PROP_IATACODE)) {
					airportMap.put(data.getString(MDM_PROP_IATACODE), data.toJson());
				}
			}
			
			jedis.hmset(REDIS_KEY_AIRPORTDATA, airportMap);
		}
		catch (Exception x) {
			logger.warn("An exception occurred when loading airport data", x);
		}
		
		logger.info("Redis Airport Data push time: " + (System.currentTimeMillis() - startTime));
	}
	
	public static Map<String, Object> getAirportInfo(String iataCode) {
		if (iataCode == null || iataCode.isEmpty()) {
			return null;
		}
		
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool(); ) {
			String airportAttrs = redisConn.hget(REDIS_KEY_AIRPORTDATA, iataCode);
			return Document.parse(airportAttrs);
		}
	}
	 
	public static String getAirportInfo(String iataCode, String key) {
		Map<String, Object> airportInfo = getAirportInfo(iataCode);
		return (airportInfo != null) ? airportInfo.getOrDefault(key, "").toString() : "";
	}

}

