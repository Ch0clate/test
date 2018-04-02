package com.coxandkings.travel.bookingengine.config.rail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.orchestrator.rail.RailConstants;

public class RailConfig {
	private static Map<String, OperationConfig> mOpConfig = new HashMap<String, OperationConfig>();
	private static CommercialsConfig mCommConfig;
	private static Map<String, String> mHttpHeaders = new HashMap<String, String>();
	
	@SuppressWarnings("unchecked")
	public static void loadConfig() {
		
		org.bson.Document configDoc = MongoProductConfig.getConfig(RailConstants.PRODUCT_RAIL);
		mHttpHeaders.put("Content-Type", "application/xml");
		List<Document> operationConfigDocs = (List<Document>) configDoc.get("operations");
		if (operationConfigDocs != null) {
			for (Document opConfigDoc : operationConfigDocs) {
				OperationConfig opCfg = new OperationConfig(opConfigDoc);
				mOpConfig.put(opCfg.getOperationName(), opCfg);
			}
		}
		mCommConfig = new CommercialsConfig((Document) configDoc.get("commercials"));
	}
	
	public static OperationConfig getOperationConfig(String opName) {
		return mOpConfig.get(opName);
	}
	
	public static Map<String, String> getHttpHeaders() {
		return mHttpHeaders;
	}
	
	public static CommercialsConfig getCommercialsConfig() {
		return mCommConfig;
	}
	
	public static CommercialTypeConfig getCommercialTypeConfig(String commType) {
		return mCommConfig.getCommercialTypeConfig(commType);
	}

}
