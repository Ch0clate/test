package com.coxandkings.travel.bookingengine.config;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;

public class OperationsShellConfig {
	public static final String PROP_MONGO_DOC = "OPERATIONS";
	public static final String PROP_REQ_JSON_SHELL = "todoJsonShell";
	public static final String PROP_REQ_JSON_URL = "operationsUrl";
	private static String errorJsonString;
	private static String operationsUrl;
	public static void loadConfig() {
		
		Document configDoc = MongoProductConfig.getConfig(PROP_MONGO_DOC);
		Document todoTaskListInfo = (Document) configDoc.get("todoTaskListInfo");
		 errorJsonString=todoTaskListInfo.getString(PROP_REQ_JSON_SHELL);
		 operationsUrl=todoTaskListInfo.getString(PROP_REQ_JSON_URL);
		
	}
	 public static String getOperationsTodoErrorShell() {
		
		 return errorJsonString;
	 }
	 
	 public static String getOperationsUrl() {
		 
		 return operationsUrl;
		 
	 }
	

}
