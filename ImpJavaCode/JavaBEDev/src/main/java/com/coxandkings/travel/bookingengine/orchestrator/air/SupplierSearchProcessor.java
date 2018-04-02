package com.coxandkings.travel.bookingengine.orchestrator.air;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
//import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class SupplierSearchProcessor implements AirConstants, Runnable {

	private static final Logger logger = LogManager.getLogger(SupplierSearchProcessor.class);
	private AirSearchListenerResponder mSearchListener;
	private ProductSupplier mProdSupplier;
	private JSONObject mReqJson;
	private Element mReqElem;
	private long mParentThreadId;
	
	SupplierSearchProcessor(AirSearchListenerResponder searchListener, ProductSupplier prodSupplier, JSONObject reqJson, Element reqElem) {
		mSearchListener = searchListener;
		mProdSupplier = prodSupplier;
		mReqJson = reqJson;
		mReqElem = (Element) reqElem.cloneNode(true);
		mParentThreadId = Thread.currentThread().getId();
	}
	
	public void process(ProductSupplier prodSupplier, JSONObject reqJson, Element reqElem) {
		OperationConfig opConfig = AirConfig.getOperationConfig("search");
        JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		
		Document ownerDoc = reqElem.getOwnerDocument();
        Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
        suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc, 1));
        
        Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:TransactionID");
        transactionElem.setTextContent(transactionElem.getTextContent().concat(String.format("-%s", prodSupplier.getSupplierID())));

        Element resElem = null;
        try {
    		TrackingContext.setTrackingContext(reqJson);
	        resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
	        // TODO: Revisit this. Should the code really throw an exception? Or just log the problem and send an empty response back? 
	        if (resElem == null) {
	        	throw new Exception("Null response received from SI");
	        }
	        
            JSONObject resBodyJson = new JSONObject();
            JSONArray pricedItinsJsonArr = new JSONArray();
            Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirLowFareSearchRSWrapper");
            for (Element wrapperElem : wrapperElems) {
            	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_AirLowFareSearchRS");
            	AirSearchProcessor.getSupplierResponsePricedItinerariesJSON(resBodyElem, pricedItinsJsonArr);
            }
            resBodyJson.put(JSON_PROP_PRICEDITIN, pricedItinsJsonArr);

            JSONObject resJson = new JSONObject();
            resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
            resJson.put(JSON_PROP_RESBODY, resBodyJson);

            // Call BRMS Supplier Commercials
           JSONObject resSupplierJson =  SupplierCommercials.getSupplierCommercialsV2(reqJson, resJson);
			if (BRMS_STATUS_TYPE_FAILURE.equals(resSupplierJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Supplier Commercials calculation engine: %s", resSupplierJson.toString()));
				//AirSearchProcessor.getEmptyResponse(reqHdrJson).toString();
			}
           
            //**********************************************************************
            // There are no supplier offers for Air. As communicated by Offers team.
			
           JSONObject resClientJson = ClientCommercials.getClientCommercialsV2(reqJson, resJson, resSupplierJson);
			if (BRMS_STATUS_TYPE_FAILURE.equals(resClientJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Client Commercials calculation engine: %s", resClientJson.toString()));
				//return AirSearchProcessor.getEmptyResponse(reqHdrJson).toString();
			}
           
           //AirSearchProcessor.calculatePricesV3(reqJson, resJson, resSupplierJson, resClientJson, false, UserContext.getUserContextForSession(reqHdrJson));
			AirSearchProcessor.calculatePricesV4(reqJson, resJson, resSupplierJson, resClientJson, false, UserContext.getUserContextForSession(reqHdrJson));
           
           // Apply company offers
           CompanyOffers.getCompanyOffers(reqJson, resJson, OffersConfig.Type.COMPANY_SEARCH_TIME);
            
           mSearchListener.receiveSupplierResponse(resJson);
        }
        catch (Exception x) {
        	x.printStackTrace();
        }
	}

	@Override
	public void run() {
		//TrackingContext.setTrackingContext(Thread.currentThread().getId(), TrackingContext.getTrackingContext(mParentThreadId));
		process(mProdSupplier, mReqJson, mReqElem);
	}
	
}
