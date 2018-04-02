package com.coxandkings.travel.bookingengine.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.utils.Constants;

public class CommercialCacheConfig implements Constants{

	
	private static Map<String, URL> mCCConfig = new HashMap<String, URL>();
	private static Map<String, String> mHttpHeaders = new HashMap<String, String>();

	@SuppressWarnings("unchecked")
	public static void loadConfig() {
		mHttpHeaders.put(HTTP_HEADER_CONTENT_TYPE, HTTP_CONTENT_TYPE_APP_JSON);
		org.bson.Document configDoc = MongoProductConfig.getConfig("CommercialCache");
		List<Document> opConfigDocs = (List<Document>) configDoc.get("operations");
		if (opConfigDocs != null) {
			for (Document opConfigDoc : opConfigDocs) {
				String opName = opConfigDoc.getString("name");
				try {
					URL serviceURL = new URL(opConfigDoc.getString("serviceURL"));
					mCCConfig.put(opName, serviceURL);

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}

	public static URL getServiceURL(String operationName) {
		return mCCConfig.get(operationName);
	}

	public static Map<String, String> getmHttpHeaders() {
		return mHttpHeaders;
	}
	

	
}