package com.coxandkings.travel.bookingengine.config.cruise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.controller.cruise.CruiseController;
import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruiseConstants;
import com.coxandkings.travel.bookingengine.utils.Constants;
import org.bson.Document;

public class CruiseConfig {
		
		private static final int THREAD_POOL_CORE_SIZE = 10;
		private static final int THREAD_POOL_MAX_SIZE = 20;
		private static final int THREAD_POOL_KEEP_ALIVE_SECONDS = 60;
		private static final int THREAD_POOL_QUEUE_SIZE = 10;
		
		private static final int DEFAULT_REDIS_TTL_MINS = 15;
		
		private static Map<String, OperationConfig> mOpConfig = new HashMap<String, OperationConfig>();
		private static CommercialsConfig mCommConfig;
		private static Map<String, String> mHttpHeaders = new HashMap<String, String>();
		
		private static ThreadPoolExecutor mThreadPool;
		private static int mRedisTTLMins;
		
		@SuppressWarnings("unchecked")
		public static void loadConfig() {
			mHttpHeaders.put("Content-Type", "application/xml");
			
			Document configDoc = MongoProductConfig.getConfig(CruiseConstants.PRODUCT_CRUISE);
			mRedisTTLMins = configDoc.getInteger("redisTTLMinutes", DEFAULT_REDIS_TTL_MINS);
			List<Document> opConfigDocs = (List<Document>) configDoc.get("operations");
			if (opConfigDocs != null) {
				for (Document opConfigDoc : opConfigDocs) {
					OperationConfig opCfg = new OperationConfig(opConfigDoc);
					mOpConfig.put(opCfg.getOperationName(), opCfg);
				}
			}
			
			mCommConfig = new CommercialsConfig((Document) configDoc.get("commercials"));

			//------------------------------------------------------------
			// Create thread pool to process air search requests

			int coreSize = THREAD_POOL_CORE_SIZE;
			int maxSize = THREAD_POOL_MAX_SIZE;
			long keepAliveSeconds =  THREAD_POOL_KEEP_ALIVE_SECONDS;
			int queueSize = THREAD_POOL_QUEUE_SIZE;
			
			Document threadPoolDoc = (Document) configDoc.get("threadPool");
			if (threadPoolDoc != null) {
				coreSize = threadPoolDoc.getInteger("coreThreads", THREAD_POOL_CORE_SIZE);
				maxSize = threadPoolDoc.getInteger("maxThreads", THREAD_POOL_MAX_SIZE);
				keepAliveSeconds = threadPoolDoc.getInteger("keepAliveSeconds", THREAD_POOL_KEEP_ALIVE_SECONDS);
				queueSize = threadPoolDoc.getInteger("queueSize", THREAD_POOL_QUEUE_SIZE);
			}
			mThreadPool = new ThreadPoolExecutor(coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize));
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
		
		public static Map<String, String> getHttpHeaders() {
			return mHttpHeaders;
		}
		
		public static ThreadPoolExecutor getThreadPool() {
			return mThreadPool;
		}
		
		public static int getRedisTTLMinutes() {
			return mRedisTTLMins;
		}
		
}
