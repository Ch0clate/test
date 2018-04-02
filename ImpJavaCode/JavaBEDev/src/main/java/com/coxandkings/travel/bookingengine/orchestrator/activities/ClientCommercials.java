package com.coxandkings.travel.bookingengine.orchestrator.activities;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.activities.ActivitiesConfig;
import com.coxandkings.travel.bookingengine.utils.LoggerUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.apache.logging.log4j.Logger;

public class ClientCommercials implements ActivityService{

private static Logger logger = LoggerUtil.getLoggerInstance(ClientCommercials.class);

    public static JSONObject getClientCommercials(JSONObject suppCommRes) {
        CommercialsConfig commConfig = ActivitiesConfig.getCommercialsConfig();
        CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
        JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
        JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.activities_commercialscalculationengine.clienttransactionalrules.Root"); 
        
        JSONObject suppCommResRoot = suppCommRes.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.activities_commercialscalculationengine.suppliertransactionalrules.Root");
        JSONObject suppCommResHdr = suppCommResRoot.getJSONObject("header");
        JSONArray suppCommResBRIArr = suppCommResRoot.getJSONArray(JSON_PROP_BUSINESS_RULE_INTAKE);
        breClientReqRoot.put("header", suppCommResHdr);
        JSONArray briJsonArr = new JSONArray();

        // TODO: Following entity details have been hard-coded. These should be retrieved from client context and then added here.
        JSONArray entityDtlsJsonArr = new JSONArray();
        JSONObject entityDtlsJson1 = new JSONObject();
        entityDtlsJson1.put("entityType", "ClientType");
        entityDtlsJson1.put("entityName", "CnKB2BIndEng");
        entityDtlsJson1.put("entityMarket", "India");
        
        JSONObject entityDtlsJson2 = new JSONObject();
        entityDtlsJson2.put("entityType", "ClientSpecific");
        entityDtlsJson2.put("entityName", "AkbarTravels");
        entityDtlsJson2.put("entityMarket", "India");
        entityDtlsJson2.put("parentEntityName", "CnKB2BIndEng");
       
        entityDtlsJsonArr.put(entityDtlsJson1);
        entityDtlsJsonArr.put(entityDtlsJson2);
        
        for(int i=0;i<suppCommResBRIArr.length();i++) {
        JSONObject suppCommResBRI=suppCommResBRIArr.getJSONObject(i);
        suppCommResBRI.put("entityDetails", entityDtlsJsonArr);
        briJsonArr.put(suppCommResBRI);
        } 
        breClientReqRoot.put(JSON_PROP_BUSINESS_RULE_INTAKE, briJsonArr);
		
        JSONObject breClientResJson = null;
		try {
			//logger.trace(String.format("BRMS Client Commercials Request = %s", breClientReqJson.toString()));
			breClientResJson = HTTPServiceConsumer.consumeJSONService("BRMS Client Commercials", commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breClientReqJson);			
	        //logger.trace(String.format("BRMS Client Commercials Response = %s", breClientResJson.toString()));
		}
		catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
		
        return breClientResJson;
    }
}