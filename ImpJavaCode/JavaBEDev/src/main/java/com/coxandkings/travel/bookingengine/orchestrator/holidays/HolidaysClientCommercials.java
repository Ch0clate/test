package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;

public class HolidaysClientCommercials {
	private static final Logger logger = LogManager.getLogger(HolidaysClientCommercials.class);
	public static JSONObject getClientCommercials(JSONObject supplierCommRes) {
		
		JSONObject clientReqShell = null ;

			CommercialsConfig commConfig = HolidaysConfig.getCommercialsConfig();
			CommercialTypeConfig commTypeConfig = commConfig
					.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);

			 clientReqShell = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
			JSONObject clientReqRoot = clientReqShell.getJSONArray("commands").getJSONObject(0)
					.getJSONObject("insert").getJSONObject("object")
					.getJSONObject("cnk.holidays_commercialscalculationengine.clienttransactionalrules.Root");

			JSONObject supplierCommercialResponseRoot = supplierCommRes.getJSONObject("result").getJSONObject("execution-results")
					.getJSONArray("results").getJSONObject(0).getJSONObject("value")
					.getJSONObject("cnk.holidays_commercialscalculationengine.suppliertransactionalrules.Root");

			JSONObject supplierCommercialResponseHeader = supplierCommercialResponseRoot.getJSONObject("header");
			
			//--- Started
			 JSONArray briJsonArr = new JSONArray();
			JSONArray suppCommResBRIArr = supplierCommercialResponseRoot.getJSONArray("businessRuleIntake");
			for (int i=0; i < suppCommResBRIArr.length(); i++) {
	        	JSONObject supplierCommercialResponseBRI = suppCommResBRIArr.getJSONObject(i);
	        	
	        	//supplierCommercialResponseBRI.remove("commercialHead");

				JSONArray entityDtlsJsonArr = new JSONArray();
				JSONObject entityDtlsJson = new JSONObject();
				//TODO: hardcoded entity details, fetched from where??
				entityDtlsJson.put("entityType", "ClientType");
				entityDtlsJson.put("entityName", "B2B");
				entityDtlsJson.put("entityMarket", "India");
				entityDtlsJsonArr.put(entityDtlsJson);
				supplierCommercialResponseBRI.put("entityDetails", entityDtlsJsonArr);

				JSONObject advancedDefinition = supplierCommercialResponseBRI.getJSONObject("advancedDefinition");
				//supplierCommercialResponseBRI.remove("advancedDefinition");
				//TODO: hardcoded advanced definition, fetched from where??
				advancedDefinition.put("ancillaryName", "AN1");
				advancedDefinition.put("ancillaryType", "AT1");
				advancedDefinition.put("applicableOn", "AO");
				supplierCommercialResponseBRI.put("advancedDefinition", advancedDefinition);

				briJsonArr.put(supplierCommercialResponseBRI);
				
				
	        	
			}
			clientReqRoot.put("businessRuleIntake", briJsonArr);
			clientReqRoot.put("header", supplierCommercialResponseHeader);
        	
			
			//--ended
			
			
			logger.info(
					String.format("Client commercial request: %s", clientReqShell));
			System.out.println("Client commercial request: " +clientReqShell);
			
			JSONObject breClientResJson = null;
			try {
				logger.trace(String.format("BRMS Holidays Client Commercials Request = %s", clientReqShell.toString()));
				
				breClientResJson = HTTPServiceConsumer.consumeJSONService("BRMS", commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), clientReqShell);			
		        logger.trace(String.format("BRMS Holidays Client Commercials Response = %s", breClientResJson.toString()));
		        
			}
			catch (Exception x) {
				logger.warn("An exception occurred when calling client commercials", x);
			}
			
	        return breClientResJson;
		
	

	}

}
