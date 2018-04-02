package com.coxandkings.travel.bookingengine.db.kafka;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.db.mongo.MongoProductConfig;



public class KafkaConfig {
	
	
	 private static String OPS_PRODUCER_TOPIC ;
	 private static String OPS_PRODUCER_BOOTSTRAP_SERVERS;
	 private static String BE_CONSUMER_BOOTSTRAP_SERVERS;
	 private static String BE_CONSUMER_GROUP;
	 private static String BE_CONSUMER_TOPIC;
	 


	    public static void loadConfig() {
	        Document configDoc = MongoProductConfig.getConfig("KAFKA");
	        
	        Document opsDoc = (Document) configDoc.get("opsKafka");
	        OPS_PRODUCER_TOPIC=opsDoc.getString("topic");
	        OPS_PRODUCER_BOOTSTRAP_SERVERS=opsDoc.getString("kafkaURL");
	        
	        Document beDoc = (Document) configDoc.get("BEKafka");
	        
	        BE_CONSUMER_BOOTSTRAP_SERVERS = beDoc.getString("kafkaURL");
	        BE_CONSUMER_GROUP = beDoc.getString("consumerGroup");
	        BE_CONSUMER_TOPIC = beDoc.getString("topic");
	    }

		public static String getOPS_PRODUCER_TOPIC() {
			return OPS_PRODUCER_TOPIC;
		}

		public static String getOPS_PRODUCER_BOOTSTRAP_SERVERS() {
			return OPS_PRODUCER_BOOTSTRAP_SERVERS;
		}

		public static String getBE_CONSUMER_BOOTSTRAP_SERVERS() {
			return BE_CONSUMER_BOOTSTRAP_SERVERS;
		}

		public static String getBE_CONSUMER_GROUP() {
			return BE_CONSUMER_GROUP;
		}

		public static String getBE_CONSUMER_TOPIC() {
			return BE_CONSUMER_TOPIC;
		}

}
