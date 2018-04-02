package com.coxandkings.travel.bookingengine.config;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.coxandkings.travel.bookingengine.utils.Constants;


public class CommercialTypeConfig {
	
	private static final Logger logger = LogManager.getLogger(CommercialTypeConfig.class);
	
	private String mTypeName;
	private URL mServiceURL;
	private String mReqJSONShell;
	
	public CommercialTypeConfig(Document commTypeConfig) {
		mTypeName = commTypeConfig.getString(Constants.CONFIG_PROP_TYPE);
		mReqJSONShell = commTypeConfig.getString(Constants.CONFIG_PROP_REQ_JSON_SHELL);
		try {
			mServiceURL = new URL(commTypeConfig.getString(Constants.CONFIG_PROP_SERVICE_URL));
		}
		catch (MalformedURLException mux) {
			logger.warn(String.format("Error occurred while initializing service URL for %s", commTypeConfig.getString(Constants.CONFIG_PROP_SERVICE_URL)), mux);
		}
	}
	
	public String getTypeName() {
		return mTypeName;
	}
	
	public URL getServiceURL() {
		return mServiceURL;
	}
	
	public String getRequestJSONShell() {
		return mReqJSONShell;
	}

}
