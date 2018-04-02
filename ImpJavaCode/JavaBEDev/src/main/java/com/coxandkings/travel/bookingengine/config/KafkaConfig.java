package com.coxandkings.travel.bookingengine.config;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;

public class KafkaConfig {
	
	// private static MongoConnect mMongoConn;
	 private static String TOPIC ;
	 private static String BOOTSTRAP_SERVERS;
	 
	 /*private final static String TOPIC;
	 private final static String BOOTSTRAP_SERVERS;*/

	    public static void loadConfig() {
	        Document configDoc = MongoProductConfig.getConfig("KAFKA");
	        Document connDoc = (Document) configDoc.get("BEKafka");
	        TOPIC=connDoc.getString("topic");
	        BOOTSTRAP_SERVERS=connDoc.getString("kafkaURL");
	        //mMongoConn = MongoConnect.newInstance(connDoc);
	    }

		public static String getTOPIC() {
			return TOPIC;
		}

		public static String getBOOTSTRAP_SERVERS() {
			return BOOTSTRAP_SERVERS;
		}
	   

}
