package com.coxandkings.travel.bookingengine.orchestrator.bus;

import java.io.File;
import java.io.FileNotFoundException;
//import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.bus.BusConfig;

public class ClientCommercials {

	public static JSONObject getClientCommercials(JSONObject resSupplierJson) {
		
		CommercialsConfig commConfig = BusConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
		JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
		JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.bus_commercialscalculationengine.clienttransactionalrules.Root");
		
		JSONObject suppCommResRoot = resSupplierJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.bus_commercialscalculationengine.suppliertransactionalrules.Root");
        JSONObject suppCommResHdr = suppCommResRoot.getJSONObject("header");
        breClientReqRoot.put("header", suppCommResHdr);
        
        JSONArray briJsonArr = new JSONArray();
        JSONArray suppCommResBRIArr = suppCommResRoot.getJSONArray("businessRuleIntake");
        
        for (int i=0; i < suppCommResBRIArr.length(); i++) {
        	JSONObject suppCommResBRI = suppCommResBRIArr.getJSONObject(i);

	        // TODO: Following entity details have been hard-coded. These should be retrieved from client context and then added here.
	        JSONArray entityDtlsJsonArr = new JSONArray();
	        JSONObject entityDtlsJson = new JSONObject();
	        entityDtlsJson.put("entityType", "ClientType");
	        entityDtlsJson.put("entityName", "CnKB2BIndEng");
	        entityDtlsJson.put("entityMarket", "India");
	        entityDtlsJsonArr.put(entityDtlsJson);
	        suppCommResBRI.put("entityDetails", entityDtlsJsonArr);
	        suppCommResBRI.remove("commercialHead");
	        suppCommResBRI.remove("ruleFlowName");
	        suppCommResBRI.remove("selectedRow");
	        suppCommResBRI.getJSONObject("commonElements").put("rateCode", "RT1");
	        suppCommResBRI.getJSONObject("commonElements").put("rateType", "RC1");
	        briJsonArr.put(suppCommResBRI);
        }
        breClientReqRoot.put("businessRuleIntake", briJsonArr);
       
//        PrintWriter pw2;
//		try {
//			pw2 = new PrintWriter(new File("D:\\BE\\temp\\clientcommreqgenJson.txt"));
//			pw2.write(breClientReqJson.toString());
//			pw2.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        JSONObject breClientResJson = null;
		try {
			breClientResJson = HTTPServiceConsumer.consumeJSONService("BRMS/ClientComm", commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breClientReqJson);			
		}
		catch (Exception x) {
			//logger.warn("An exception occurred when calling supplier commercials", x);
			x.printStackTrace();
		}
		
        return breClientResJson;

	}

}
