package com.coxandkings.travel.bookingengine.config;

import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import org.bson.Document;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisConfig {

	public static final int DFT_REDIS_PORT = 6379;
	private static String mRedisHost;
	private static int mRedisPort;
	private static JedisPool mRedisConnPool;
	
	public static void loadConfig() {
		Document redisConfig = MongoProductConfig.getConfig("REDIS");
		Document connConfig = (Document) redisConfig.get("connection");
		mRedisHost = connConfig.getString("host");
		mRedisPort = connConfig.getInteger("port", DFT_REDIS_PORT);
		mRedisConnPool = new JedisPool(mRedisHost, mRedisPort);
	}
	
	public static Jedis getRedisConnectionFromPool() {
		return mRedisConnPool.getResource();
	}
	
	public static void releaseRedisConnectionToPool(Jedis conn) {
		conn.close();
	}
}
