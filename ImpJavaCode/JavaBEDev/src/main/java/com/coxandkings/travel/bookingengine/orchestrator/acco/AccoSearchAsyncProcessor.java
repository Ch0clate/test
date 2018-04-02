package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.enums.AccoSubType;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class AccoSearchAsyncProcessor implements AccoConstants, Runnable{

	
	private static final Logger logger = LogManager.getLogger(AccoSearchAsyncProcessor.class);
	static final String OPERATION_NAME = "search";
	private JSONObject mReqJson;
	
	public AccoSearchAsyncProcessor(JSONObject reqJson) {
		mReqJson = reqJson;
	}
	
	public static String process(JSONObject reqJson) {
		try {
		OperationConfig opConfig = AccoConfig.getOperationConfig(OPERATION_NAME);
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		TrackingContext.setTrackingContext(reqJson);
 
		setSupplierRequestElem(reqJson, reqElem);
		
		System.out.println(XMLTransformer.toString(reqElem));
	
	
		}
		catch (Exception x) {
			x.printStackTrace();
			//return "{\"status\": \"ERROR\"}";
		}
		logger.debug("AccoSearchAsyncProcessor completing and exiting now...");
        return null;
	}
	
	public static void setSupplierRequestElem(JSONObject reqJson, Element reqElem) throws Exception {

		Document ownerDoc = reqElem.getOwnerDocument();
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null)
				? reqJson.optJSONObject(JSON_PROP_REQHEADER)
				: new JSONObject();
		JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null)
				? reqJson.optJSONObject(JSON_PROP_REQBODY)
				: new JSONObject();
     
		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);
		
		AccoSearchProcessor.createSuppReqHdrElem(XMLUtils.getFirstElementAtXPath(reqElem, "./acco:RequestHeader"), sessionID, transactionID, userID);
		
		AccoSearchProcessor.setSuppReqOTAElem(ownerDoc,
				XMLUtils.getFirstElementAtXPath(reqElem, "./accoi:RequestBody/ota:OTA_HotelAvailRQ"), reqBodyJson,
				reqHdrJson);
		//System.out.println(XMLTransformer.toString(reqElem));
		AccoSubType prodCategSubtype = AccoSubType.forString(reqBodyJson.optString(JSON_PROP_ACCOSUBTYPEARR));
		UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
		List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_ACCO,
				prodCategSubtype != null ? prodCategSubtype.toString() : DEF_PRODSUBTYPE);
		 AccoSearchListenerResponder searchListener = new AccoSearchListenerResponder(prodSuppliers, reqJson);
         
		 for (ProductSupplier prodSupplier : prodSuppliers) {
             SupplierSearchProcessor suppSrchPrc = new SupplierSearchProcessor(searchListener, prodSupplier, reqJson, reqElem);
         	//AirConfig.getThreadPool().execute(suppSrchPrc);
             AirConfig.execute(suppSrchPrc);
         }
         
       // TODO: The wait time for AirSearchListenerResponder wait is hard-coded. Need to get this from AirConfig or from input request.
       synchronized(searchListener) {
         	searchListener.wait(AirConfig.getAsyncSearchWaitMillis());
         }
         
 	/*	JSONObject clientCtxJson = reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT);
 		String callbackAddr = clientCtxJson.optString(JSON_PROP_CLIENTCALLBACK);
 		if (callbackAddr != null) {
 			// This is a synchronous search request
 			//return searchListener.getSearchResponse().toString();
 		}*/
	}
	
	@Override
	public void run() {
		process(mReqJson);
		
	}
}
