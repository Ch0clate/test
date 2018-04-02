package com.coxandkings.travel.bookingengine.orchestrator.air;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientCommercials implements AirConstants {

    private static Logger logger = LogManager.getLogger(ClientCommercials.class);

    public static JSONObject getClientCommercialsV2(JSONObject reqJson, JSONObject resJson, JSONObject suppCommRes) {
        CommercialsConfig commConfig = AirConfig.getCommercialsConfig();
        CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
        JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
        JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.air_commercialscalculationengine.clienttransactionalrules.Root"); 
        
        JSONObject suppCommResRoot = suppCommRes.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.air_commercialscalculationengine.suppliertransactionalrules.Root");
        JSONObject suppCommResHdr = suppCommResRoot.getJSONObject(JSON_PROP_HEADER);
        breClientReqRoot.put(JSON_PROP_HEADER, suppCommResHdr);
        
        JSONObject reqHeaderJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
        UserContext usrCtx = UserContext.getUserContextForSession(reqHeaderJson);
        
        JSONArray briJsonArr = new JSONArray();
        JSONArray suppCommResBRIArr = suppCommResRoot.getJSONArray(JSON_PROP_BUSSRULEINTAKE);
        for (int i=0; i < suppCommResBRIArr.length(); i++) {
        	JSONObject suppCommResBRI = suppCommResBRIArr.getJSONObject(i);
	        suppCommResBRI.put(JSON_PROP_ENTITYDETAILS, usrCtx.getClientCommercialsHierarchy());
	        briJsonArr.put(suppCommResBRI);
        }
        breClientReqRoot.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);
        
		JSONObject breClientResJson = null;
		try {
			breClientResJson = HTTPServiceConsumer.consumeJSONService(TARGET_SYSTEM_CLIENTCOMM, commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breClientReqJson);			
		}
		catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
		
        return breClientResJson;
    }
    
}