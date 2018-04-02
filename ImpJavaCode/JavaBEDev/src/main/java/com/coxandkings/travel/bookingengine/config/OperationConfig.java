package com.coxandkings.travel.bookingengine.config;

import java.net.MalformedURLException;
import java.net.URL;


import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;


public class OperationConfig {

	private String mName;
	private URL mServiceURL;
	private Element mReqXMLShell;
	private static final Logger logger = LogManager.getLogger(OperationConfig.class);
	
	public OperationConfig(org.bson.Document opConfig) {
		mName = opConfig.getString("name");
		mReqXMLShell = XMLTransformer.fromEscapedString(opConfig.getString("suppIntegReqXMLShell"));
		try { 
			mServiceURL = new URL(opConfig.getString("suppIntegServiceURL"));
		}
		catch (MalformedURLException mux) {
			logger.warn(String.format("Error occurred while initializing service URL for operation %s", mName), mux);
		}
	}
	
	public String getOperationName() {
		return mName;
	}
	
	public Element getRequestXMLShell() {
		return mReqXMLShell;
	}
	
	public URL getSIServiceURL() {
		return mServiceURL;
	}
}
