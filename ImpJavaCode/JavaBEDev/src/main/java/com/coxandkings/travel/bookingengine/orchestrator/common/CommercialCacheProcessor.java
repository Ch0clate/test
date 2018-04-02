package com.coxandkings.travel.bookingengine.orchestrator.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialCacheConfig;
import com.coxandkings.travel.bookingengine.utils.Constants;

public class CommercialCacheProcessor implements Constants {
	
	private static final Logger logger = LogManager.getLogger(CommercialCacheProcessor.class);
	
	public static String getFromCache(String pProductType, String pClientEntity,JSONArray suppCredsJsonArr, JSONObject pRequest) {
		
		JSONObject commCacheReq = new JSONObject();
        commCacheReq.put("pProductType", pProductType);
        
        
        commCacheReq.put("pClientEntity", pClientEntity);
        commCacheReq.put("suppCredsJsonArr", suppCredsJsonArr);
        commCacheReq.put("pRequest", pRequest);
        JSONObject commCacheRes = new JSONObject() ;
        try {
        	 commCacheRes = HTTPServiceConsumer.consumeJSONService(TARGET_SYSTEM_COMMCACHE, CommercialCacheConfig.getServiceURL("get"), CommercialCacheConfig.getmHttpHeaders(), commCacheReq);
        	  if(commCacheRes!=null && commCacheRes.getJSONObject("responseBody").has("errorMessage")) {
              return commCacheRes.toString();
              
        	}
              
        }
        catch (Exception x) {
            logger.warn("An exception occurred when calling commercial Cache", x);
           
        }
        return "error";
 
	}
	
	public static String putInCache(String pProductType, String pClientEntity,JSONObject pResponse, JSONObject pRequest) {
		
		JSONObject commCacheReq = new JSONObject();
        commCacheReq.put("pProductType", pProductType);
        
        //TODO: currently hardcoding. Discuss it with Sir and find out where to get it from 
        commCacheReq.put("pClientEntity", pClientEntity);
        commCacheReq.put("pResponse", pResponse);
        commCacheReq.put("pRequest", pRequest);
        JSONObject commCacheRes = new JSONObject() ;
        try {
        	 commCacheRes = HTTPServiceConsumer.consumeJSONService(TARGET_SYSTEM_COMMCACHE, CommercialCacheConfig.getServiceURL("put"), CommercialCacheConfig.getmHttpHeaders(), commCacheReq);
        	  if(commCacheRes!=null) {
              return commCacheRes.toString();
              
        	}
              
        }
        catch (Exception x) {
            logger.warn("An exception occurred when calling commercial Cache", x);
           
        }
        return "error";
 
	}
	
	
	public static String updateInCache(String pProductType, String pClientEntity,JSONObject pResponse, JSONObject pRequest) {
		
		JSONObject commCacheReq = new JSONObject();
        commCacheReq.put("pProductType", pProductType);
        
        //TODO: currently hardcoding. Discuss it with Sir and find out where to get it from 
        commCacheReq.put("pClientEntity", pClientEntity);
        commCacheReq.put("pResponse", pResponse);
        commCacheReq.put("pRequest", pRequest);
        JSONObject commCacheRes = new JSONObject() ;
        try {
        	 commCacheRes = HTTPServiceConsumer.consumeJSONService(TARGET_SYSTEM_COMMCACHE, CommercialCacheConfig.getServiceURL("put"), CommercialCacheConfig.getmHttpHeaders(), commCacheReq);
        	  if(commCacheRes!=null) {
              return commCacheRes.toString();
              
        	}
              
        }
        catch (Exception x) {
            logger.warn("An exception occurred when calling commercial Cache", x);
           
        }
        return "error";
 
	}
	

}
