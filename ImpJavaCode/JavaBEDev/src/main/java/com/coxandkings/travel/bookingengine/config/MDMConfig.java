package com.coxandkings.travel.bookingengine.config;

import com.coxandkings.travel.bookingengine.config.mongo.MongoConnect;
import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.utils.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

public class MDMConfig implements Constants {

    private static MongoConnect mMongoConn;
    private static URL mloginServiceURL;
    private static Map<String, String> mHttpHeaders = new HashMap<String, String>();
    private static int mRedisTTLMins;
    
    //TODO: change it based on the MDM config
    private static final int DEFAULT_REDIS_TTL_MINS = 60;

    public static void loadConfig() throws MalformedURLException {
        Document configDoc = MongoProductConfig.getConfig("MDM");
        Document connDoc = (Document) configDoc.get("connection");
        mMongoConn = MongoConnect.newInstance(connDoc);
        
        //added for login through MDM
        mloginServiceURL = new URL(configDoc.getString("loginURL"));
        mHttpHeaders.put(HTTP_HEADER_CONTENT_TYPE,  HTTP_CONTENT_TYPE_APP_JSON);
        mRedisTTLMins = configDoc.getInteger("redisTTLMinutes", DEFAULT_REDIS_TTL_MINS);
    }

    public static MongoCollection<Document> getCollection(String collName) {
        return mMongoConn.getCollection(collName);
    }

    public static void unloadConfig() {
        mMongoConn.close();
    }

	public static URL getLoginServiceURL() {
		return mloginServiceURL;
	}

	public static Map<String, String> getmHttpHeaders() {
		return mHttpHeaders;
	}

	public static int getmRedisTTLMins() {
		return mRedisTTLMins;
	}

	

}
