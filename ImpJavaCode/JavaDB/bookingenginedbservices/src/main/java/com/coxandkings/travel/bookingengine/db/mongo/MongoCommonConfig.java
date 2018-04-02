package com.coxandkings.travel.bookingengine.db.mongo;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

@SuppressWarnings("unchecked")
public class MongoCommonConfig {

	private static final Logger logger = LogManager.getLogger(MongoCommonConfig.class);
	private static Document mConfigDoc = null;
//	private static Map<String, ProductCacheHandler> nsHandler = new HashMap<String, ProductCacheHandler>();
//	private static Map<String, Class<CacheEntry>> nsEntry = new HashMap<String, Class<CacheEntry>>();
	private static List<Document> nsContext = new ArrayList<Document>();
	private static List<SimpleEntry<String,String>> mTrackElems = new ArrayList<SimpleEntry<String,String>>();
//	private static Map<String, ProductCacheHandler> mHandlerClasses = new HashMap<String, ProductCacheHandler>();  
	
	static {
		loadCommonConfig();
//		loadHandlersConfig();
		
		nsContext = (ArrayList<Document>) mConfigDoc.get("NamespacesContext");
		if (nsContext == null) {
			logger.warn("Common configuration does not contain <NamespacesContext> definition");
		}
		
		// Tracking elements configuration retrieval
		ArrayList<Document> trackingElems = (ArrayList<Document>) mConfigDoc.get("TrackingElements");
		if (trackingElems == null) {
			logger.warn("Common configuration for does not contain <TrackingElements> definition");
		}
		else {
			for (Document trackingElem : trackingElems) {
				mTrackElems.add(new SimpleEntry<String,String>(trackingElem.getString("ElementName"), trackingElem.getString("ElementXPath")));
			}
		}
	}

//	public static ProductCacheHandler getProductCacheHandler(String prodNSURI, String suppID) {
//		ProductCacheHandler prodCacheHandler = getProductCacheHandlerInternal(prodNSURI, suppID);
//		if (prodCacheHandler == null) {
//			loadCommonConfig();
//			loadHandlersConfig();
//		}
//		return getProductCacheHandlerInternal(prodNSURI, suppID);
//	}
	
//	private static ProductCacheHandler getProductCacheHandlerInternal(String prodNSURI, String suppID) {
//		ProductCacheHandler prodCacheHandler = null;
//		if (suppID != null) {
//			prodCacheHandler = nsHandler.get(prodNSURI.concat("|").concat(suppID));
//		}
//		
//		return (prodCacheHandler != null) ? prodCacheHandler : nsHandler.get(prodNSURI);
//	}

	
//	public static Class<CacheEntry> getProductCacheEntryClassName(String prodNSURI) {
//		return (prodNSURI != null) ? nsEntry.get(prodNSURI) : null;
//	}
	
	public static List<Document> getNamespacesContextConfig() {
		return nsContext;
	}
	
	public static List<SimpleEntry<String,String>> getTrackingElements() {
		return mTrackElems;
	}
	
//	private static String getHandlerKey(Document handlerConfig) {
//		String nsURI = handlerConfig.getString("NamespaceURI");
//		String suppID = handlerConfig.getString("SupplierID");
//		return (suppID != null) ? nsURI.concat("|").concat(suppID) : nsURI;
//	}

	private static void loadCommonConfig() {
		mConfigDoc = MongoProductConfig.getConfig("COMMON");
		logger.info(String.format("MongoCommonConfig: configDoc=<%s>", (mConfigDoc != null) ? mConfigDoc : "null"));
	}
	
//	private static void loadHandlersConfig() {
//		if (mConfigDoc == null) {
//			LoggerHolder.logWarning("Product configuration document for product <COMMON> not found!");
//			return;
//		}
//		
//		ArrayList<Document> handlersConfigs = (ArrayList<Document>) mConfigDoc.get("Handlers");
//		if (handlersConfigs == null) {
//			LoggerHolder.logWarning("Common configuration does not contain <Handlers> definition");
//			return;
//		}
//
//		ProductCacheHandler prodCacheHdlr = null;
//		for (Document handlerConfig : handlersConfigs) {
//			String hdlrClassName = handlerConfig.getString("HandlerClass");
//			String hdlrKey = getHandlerKey(handlerConfig);
//			
//			if (nsHandler.containsKey(hdlrKey) && hdlrClassName.equals(nsHandler.get(hdlrKey).getClass().getName())) {
//				continue;
//			}
//
//			try {
//				prodCacheHdlr = (mHandlerClasses.containsKey(hdlrClassName)) 
//									? prodCacheHdlr = mHandlerClasses.get(hdlrClassName) 
//									: ((ProductCacheHandler) Class.forName(hdlrClassName).newInstance());
//				mHandlerClasses.put(hdlrClassName, prodCacheHdlr);
//				nsHandler.put(hdlrKey, prodCacheHdlr);
//
//				Class<CacheEntry> entryClass = (Class<CacheEntry>) Class.forName(handlerConfig.getString("CacheEntryClass"));
//				nsEntry.put(handlerConfig.getString("NamespaceURI"), entryClass);
//			}
//			catch (Exception x) {
//				LoggerHolder.logWarning("An exception occurred when loading handler classes", x);
//			}
//		}
//	}
	
}
