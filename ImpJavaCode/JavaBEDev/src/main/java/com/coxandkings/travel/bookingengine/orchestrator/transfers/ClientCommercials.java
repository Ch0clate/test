package com.coxandkings.travel.bookingengine.orchestrator.transfers;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.transfers.TransfersConfig;
import com.coxandkings.travel.bookingengine.orchestrator.transfers.ClientCommercials;
import com.coxandkings.travel.bookingengine.utils.LoggerUtil;
import com.mongodb.util.JSON;

public class ClientCommercials {

	  private static Logger logger = LoggerUtil.getLoggerInstance(ClientCommercials.class);

	public static JSONObject getClientCommercialsV2(JSONObject suppCommRes) {
	
		  CommercialsConfig commConfig = TransfersConfig.getCommercialsConfig();
	        CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
	        JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
	        JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.transfers_commercialscalculationengine.clienttransactionalrules.Root"); 
	        
	        JSONObject suppCommResRoot = suppCommRes.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.transfers_commercialscalculationengine.suppliertransactionalrules.Root");
	        JSONObject suppCommResHdr = suppCommResRoot.getJSONObject("header");
	        breClientReqRoot.put("header", suppCommResHdr);
	        
	        JSONArray briJsonArr = new JSONArray();
	        JSONArray suppCommResBRIArr = suppCommResRoot.getJSONArray("businessRuleIntake");
	        for (int i=0; i < suppCommResBRIArr.length(); i++) {
	        	JSONObject suppCommResBRI = suppCommResBRIArr.getJSONObject(i);
	        	
	              JSONObject commElemJson = suppCommResBRI.getJSONObject("commonElements");
	              commElemJson.put("rateCode", "RC1");
	              commElemJson.put("rateType", "RT1");
	              
	           /*suppCommResBRI.getJSONObject("transfersDetails");*/
	            JSONArray transferArr = suppCommResBRI.getJSONArray("transfersDetails");
	            for(int j = 0 ;j < transferArr.length();j++) {
	            	JSONObject transfersJson = transferArr.getJSONObject(j);
	            	transfersJson.put("transfersNumber", "302");
	            }

		        // TODO: Following entity details have been hard-coded. These should be retrieved from client context and then added here.
		        JSONArray entityDtlsJsonArr = new JSONArray();
		        JSONObject entityDtlsJson = new JSONObject();
		        entityDtlsJson.put("entityType", "ClientType");
		        entityDtlsJson.put("entityName", "CnKB2BIndEng");
		        entityDtlsJson.put("entityMarket", "India");
		        entityDtlsJsonArr.put(entityDtlsJson);
		        suppCommResBRI.put("entityDetails", entityDtlsJsonArr);
		        
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
		
	