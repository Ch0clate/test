package com.coxandkings.travel.bookingengine.config;

import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.utils.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

public class TaxEngineConfig implements Constants {

	private static final Logger logger = LogManager.getLogger(TaxEngineConfig.class);
	
	private static String mUserID;
	private static transient String mPassword;
	private static transient String mHttpBasicAuth;
	private static Map<String, String> mHttpHeaders = new HashMap<String, String>();
	private static URL mServiceURL;
	private static String mReqJSONShell;    
    
    public static void loadConfig() throws MalformedURLException {
        Document configDoc = MongoProductConfig.getConfig("TAXENGINE");
		mUserID = configDoc.getString(PROP_USER_ID);
		// TODO: Add code for decrypting password
		mPassword = configDoc.getString(PROP_PASSWORD);
		mHttpBasicAuth = HTTP_AUTH_BASIC_PREFIX.concat(Base64.getEncoder().encodeToString(mUserID.concat(":").concat(mPassword).getBytes()));
		mHttpHeaders.put(HTTP_HEADER_CONTENT_TYPE, HTTP_CONTENT_TYPE_APP_JSON);
		mHttpHeaders.put(HTTP_HEADER_AUTHORIZATION, mHttpBasicAuth);
		
		mReqJSONShell = configDoc.getString(CONFIG_PROP_REQ_JSON_SHELL);
		try {
			mServiceURL = new URL(configDoc.getString(CONFIG_PROP_SERVICE_URL));
		}
		catch (MalformedURLException mux) {
			logger.warn(String.format("Error occurred while initializing service URL for %s", configDoc.getString(CONFIG_PROP_SERVICE_URL)), mux);
		}
    }

	public static Map<String, String> getHttpHeaders() {
		return mHttpHeaders;
	}

	public static URL getServiceURL() {
		return mServiceURL;
	}
	
	public static String getRequestJSONShell() {
		return mReqJSONShell;
	}

}
