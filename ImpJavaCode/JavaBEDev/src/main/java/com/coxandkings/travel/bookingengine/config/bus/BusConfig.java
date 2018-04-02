package com.coxandkings.travel.bookingengine.config.bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.orchestrator.bus.BusConstants;

public class BusConfig {
	
private static final int DEFAULT_REDIS_TTL_MINS = 15;
private static int mRedisTTLMins;
	
	private static Map<String, OperationConfig> mOpConfig = new HashMap<String, OperationConfig>();
	private static CommercialsConfig mCommConfig;
	private static Map<String, String> mHttpHeaders = new HashMap<String, String>();

	@SuppressWarnings("unchecked")
	public static void loadConfig()
	{
        mHttpHeaders.put("Content-Type", "application/xml");
		
		Document configDoc = MongoProductConfig.getConfig(BusConstants.PRODUCT_BUS);
		mRedisTTLMins = configDoc.getInteger("redisTTLMinutes", DEFAULT_REDIS_TTL_MINS);
		List<Document> opConfigDocs = (List<Document>) configDoc.get("operations");
		if (opConfigDocs != null) {
			for (Document opConfigDoc : opConfigDocs) {
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
	
	public static int getRedisTTLMinutes() {
		return mRedisTTLMins;
	}
}

