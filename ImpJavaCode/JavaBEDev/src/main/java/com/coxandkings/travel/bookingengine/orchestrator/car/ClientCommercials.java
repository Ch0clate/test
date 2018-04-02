package com.coxandkings.travel.bookingengine.orchestrator.car;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.car.CarConfig;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientCommercials {

    private static Logger logger = LogManager.getLogger(ClientCommercials.class);

    public static JSONObject getClientCommercials(JSONObject suppCommRes) {
        CommercialsConfig commConfig = CarConfig.getCommercialsConfig();
        CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
        JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
        JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.carrentals_commercialscalculationengine.clienttransactionalrules.Root"); 
        
        JSONObject suppCommResRoot = suppCommRes.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.carrentals_commercialscalculationengine.suppliertransactionalrules.Root");
        JSONObject suppCommResHdr = suppCommResRoot.getJSONObject("header");
        breClientReqRoot.put("header", suppCommResHdr);
        
        JSONArray briJsonArr = new JSONArray();
        JSONArray suppCommResBRIArr = suppCommResRoot.getJSONArray("businessRuleIntake");
        for (int i=0; i < suppCommResBRIArr.length(); i++) {
        	JSONObject suppCommResBRI = suppCommResBRIArr.getJSONObject(i);
        	JSONObject advDefnJson = suppCommResBRI.getJSONObject("advancedDefinition");
        	advDefnJson.put("ancillaryName", "AN1");
        	advDefnJson.put("ancillaryType", "AT1");
        	advDefnJson.put("applicableOn", "AO1");
        	
        	JSONObject commElemJson = suppCommResBRI.getJSONObject("commonElements");
        	
        	commElemJson.put("rateCode", "RT1");
        	commElemJson.put("rateType", "RC1");
        	commElemJson.put("segment", "Active");
	        // TODO: Following entity details have been hard-coded. These should be retrieved from client context and then added here.
	        JSONArray entityDtlsJsonArr = new JSONArray();
	        JSONObject entityDtlsJson = new JSONObject();
	        entityDtlsJson.put("entityType", "ClientType");
	        entityDtlsJson.put("entityName", "SitaramTravels");
	        entityDtlsJson.put("entityMarket", "India");
	        entityDtlsJsonArr.put(entityDtlsJson);
	        suppCommResBRI.put("entityDetails", entityDtlsJsonArr);

	        suppCommResBRI.remove("ruleFlowName");
	        briJsonArr.put(suppCommResBRI);
        }
        breClientReqRoot.put("businessRuleIntake", briJsonArr);
		JSONObject breClientResJson = null;
		try {
			breClientResJson = HTTPServiceConsumer.consumeJSONService("BRMS/ClientComm", commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breClientReqJson);			
		}
		catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
        return breClientResJson;
    }
    
}
