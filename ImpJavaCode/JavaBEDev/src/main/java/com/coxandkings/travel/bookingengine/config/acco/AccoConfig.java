package com.coxandkings.travel.bookingengine.config.acco;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoConstants;


public class AccoConfig {

	private static Map<String, OperationConfig> mOpConfig = new HashMap<String, OperationConfig>();
	private static CommercialsConfig mCommConfig;
	private static OffersConfig mOffConfig;
	private static Map<String, String> mHttpHeaders = new HashMap<String, String>();
	
        private static final int DEFAULT_REDIS_TTL_MINS = 15;
	private static int mRedisTTLMins;
	
	@SuppressWarnings("unchecked")
	public static void loadConfig() {
		
		mHttpHeaders.put("Content-Type", "application/xml");
		org.bson.Document configDoc = MongoProductConfig.getConfig(AccoConstants.PRODUCT_ACCO);
		List<Document> opConfigDocs = (List<Document>) configDoc.get("operations");
		if (opConfigDocs != null) {
			for (Document opConfigDoc : opConfigDocs) {
				OperationConfig opCfg = new OperationConfig(opConfigDoc);
				mOpConfig.put(opCfg.getOperationName(), opCfg);
			}
		}
		mCommConfig = new CommercialsConfig((Document) configDoc.get("commercials"));
		mOffConfig = new OffersConfig((Document) configDoc.get("offers"));
		mRedisTTLMins = configDoc.getInteger("redisTTLMinutes", DEFAULT_REDIS_TTL_MINS);
	}
	
	public static OperationConfig getOperationConfig(String opName) {
		return mOpConfig.get(opName);
	}
	
	public static CommercialsConfig getCommercialsConfig() {
		return mCommConfig;
	}
	
	public static CommercialTypeConfig getCommercialTypeConfig(String commType) {
		return mCommConfig.getCommercialTypeConfig(commType);
	}
	public static OffersConfig getOffersConfig() {
		return mOffConfig;
	}
	
	public static CommercialTypeConfig getOffersTypeConfig(OffersConfig.Type offType) {
		return mOffConfig.getOfferTypeConfig(offType);
	}
	
	public static Map<String, String> getHttpHeaders() {
		return mHttpHeaders;
	}
	
        public static int getRedisTTLMins() {
		return mRedisTTLMins;
	}

}
