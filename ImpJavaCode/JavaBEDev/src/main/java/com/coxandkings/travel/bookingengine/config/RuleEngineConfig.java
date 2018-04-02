package com.coxandkings.travel.bookingengine.config;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.utils.Constants;

abstract class RuleEngineConfig implements Constants {

	private String mUserID;
	private transient String mPassword;
	private transient String mHttpBasicAuth;
	private Map<String, String> mHttpHeaders = new HashMap<String, String>();
	private Map<String,CommercialTypeConfig> mCommTypeConfig = new HashMap<String,CommercialTypeConfig>();

	RuleEngineConfig(Document rulesEngConfigDoc) {
		mUserID = rulesEngConfigDoc.getString(PROP_USER_ID);
		// TODO: Add code for decrypting password
		mPassword = rulesEngConfigDoc.getString(PROP_PASSWORD);
		mHttpBasicAuth = HTTP_AUTH_BASIC_PREFIX.concat(Base64.getEncoder().encodeToString(mUserID.concat(":").concat(mPassword).getBytes()));
		mHttpHeaders.put(HTTP_HEADER_CONTENT_TYPE, HTTP_CONTENT_TYPE_APP_JSON);
		mHttpHeaders.put(HTTP_HEADER_AUTHORIZATION, mHttpBasicAuth);
		
		@SuppressWarnings("unchecked")
		List<Document> commTypeConfigDocs = (List<Document>) rulesEngConfigDoc.get(getInvocationTypeConfigName());
		for (Document commTypeConfigDoc : commTypeConfigDocs) {
			CommercialTypeConfig commTypeConfig = new CommercialTypeConfig(commTypeConfigDoc);
			mCommTypeConfig.put(commTypeConfig.getTypeName(), commTypeConfig);
		}
	}
	
	abstract String getInvocationTypeConfigName();
	
	public CommercialTypeConfig getInvocationTypeConfig(String invcType) {
		return mCommTypeConfig.get(invcType);
	}
	
	public String getCredentialsAsBasicAuth() {
		return mHttpBasicAuth;
	}

	public Map<String, String> getHttpHeaders() {
		return mHttpHeaders;
	}
	
}
