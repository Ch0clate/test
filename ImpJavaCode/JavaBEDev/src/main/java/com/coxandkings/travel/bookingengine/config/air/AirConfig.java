package com.coxandkings.travel.bookingengine.config.air;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirConstants;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;

import org.bson.Document;


public class AirConfig implements Constants {
	
	private static final int THREAD_POOL_CORE_SIZE = 10;
	private static final int THREAD_POOL_MAX_SIZE = 20;
	private static final int THREAD_POOL_KEEP_ALIVE_SECONDS = 60;
	private static final int THREAD_POOL_QUEUE_SIZE = 10;
	
	private static final int DEFAULT_REDIS_TTL_MINS = 15;
	
	private static final int DEFAULT_ASYNC_SEARCH_WAIT_SECS = 60;
	
	private static Map<String, OperationConfig> mOpConfig = new HashMap<String, OperationConfig>();
	private static CommercialsConfig mCommConfig;
	private static OffersConfig mOffConfig;
	private static Map<String, String> mHttpHeaders = new HashMap<String, String>();
	
	private static ThreadPoolExecutor mThreadPool;
	private static int mRedisTTLMins;
	private static int mAsyncSearchWaitSecs;
	
	@SuppressWarnings("unchecked")
	public static void loadConfig() {
		mHttpHeaders.put(HTTP_HEADER_CONTENT_TYPE, HTTP_CONTENT_TYPE_APP_XML);
		
		Document configDoc = MongoProductConfig.getConfig(AirConstants.PRODUCT_AIR);
		mRedisTTLMins = configDoc.getInteger("redisTTLMinutes", DEFAULT_REDIS_TTL_MINS);
		mAsyncSearchWaitSecs = configDoc.getInteger("asyncSearchWaitSeconds", DEFAULT_ASYNC_SEARCH_WAIT_SECS);
		List<Document> opConfigDocs = (List<Document>) configDoc.get("operations");
		if (opConfigDocs != null) {
			for (Document opConfigDoc : opConfigDocs) {
				OperationConfig opCfg = new OperationConfig(opConfigDoc);
				mOpConfig.put(opCfg.getOperationName(), opCfg);
			}
		}
		
		mCommConfig = new CommercialsConfig((Document) configDoc.get("commercials"));
		mOffConfig = new OffersConfig((Document) configDoc.get("offers"));

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

	public static OffersConfig getOffersConfig() {
		return mOffConfig;
	}
	
	public static CommercialTypeConfig getOffersTypeConfig(OffersConfig.Type offType) {
		return mOffConfig.getOfferTypeConfig(offType);
	}
	
	public static Map<String, String> getHttpHeaders() {
		return mHttpHeaders;
	}
	
	public static ThreadPoolExecutor getThreadPool() {
		return mThreadPool;
	}
	
	public static void execute(Runnable runabbleClazz) {
		Thread newThrd = mThreadPool.getThreadFactory().newThread(runabbleClazz);
		TrackingContext.setTrackingContext(newThrd.getId(), TrackingContext.getTrackingContext());
		//newThrd.run();
		newThrd.start();
	}
	
	public static int getRedisTTLMinutes() {
		return mRedisTTLMins;
	}
	
	public static int getAsyncSearchWaitSeconds() {
		return mAsyncSearchWaitSecs;
	}
	
	public static int getAsyncSearchWaitMillis() {
		return (mAsyncSearchWaitSecs * 1000);
	}

}
