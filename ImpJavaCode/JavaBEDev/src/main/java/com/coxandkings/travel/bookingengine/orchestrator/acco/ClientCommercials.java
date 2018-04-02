package com.coxandkings.travel.bookingengine.orchestrator.acco;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;

public class ClientCommercials implements AccoConstants {
	private static final Logger logger = LogManager.getLogger(AccoSearchProcessor.class);

	public static JSONObject getClientCommercials(JSONObject suppCommRes) {

		CommercialsConfig commConfig = AccoConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
		JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
		JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object")
				.getJSONObject("cnk.acco_commercialscalculationengine.clienttransactionalrules.Root");
		JSONObject suppCommResRoot = suppCommRes.getJSONObject("result").getJSONObject("execution-results")
				.getJSONArray("results").getJSONObject(0).getJSONObject("value")
				.getJSONObject("cnk.acco_commercialscalculationengine.suppliertransactionalrules.Root");
		JSONObject suppCommResHdr = suppCommResRoot.getJSONObject("header");
		suppCommResHdr.remove("status");
        
		breClientReqRoot.put("header", suppCommResHdr);
		JSONArray briJsonArr = new JSONArray();
		JSONArray suppCommResBRIArr = suppCommResRoot.getJSONArray(JSON_PROP_BUSSRULEINTAKE);
		int suppCommResBRILength = suppCommResBRIArr.length();
                JSONArray entityDtlsJsonArr = new JSONArray();
	
		for (int i = 0; i < suppCommResBRILength; i++) {
			// TODO: Following entity details have been hard-coded. These should be
			// retrieved from client context and then added here.
			JSONObject currSuppCommResBRI = suppCommResBRIArr.getJSONObject(i);
			JSONObject entityDtlsJson = new JSONObject();
			entityDtlsJson.put("entityType", "ClientSpecific");
			entityDtlsJson.put("entityName", "CnKB2B");
			entityDtlsJson.put("entityMarket", "India");
			entityDtlsJsonArr.put(entityDtlsJson);
			currSuppCommResBRI.put("entityDetails", entityDtlsJsonArr);
			briJsonArr.put(currSuppCommResBRI);

		}

		
		breClientReqRoot.put("businessRuleIntake", briJsonArr);
		JSONObject breClientResJson = null;
		try {
			//long start = System.currentTimeMillis();
			breClientResJson = HTTPServiceConsumer.consumeJSONService("BRMS-CLIENTCOMMERCIALS", commTypeConfig.getServiceURL(),
					commConfig.getHttpHeaders(), breClientReqJson);
			//logger.info(String.format("Time taken to get client commercials response : %s ms", System.currentTimeMillis()-start));
		} catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
		return breClientResJson;
	}

}
