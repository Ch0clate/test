package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class SupplierSearchProcessor implements AccoConstants, Runnable {

	private static final Logger logger = LogManager.getLogger(SupplierSearchProcessor.class);
	static final String OPERATION_NAME = "search";
	private AccoSearchListenerResponder mSearchListener;
	private ProductSupplier mProdSupplier;
	private JSONObject mReqJson;
	private Element mReqElem;
	private long mParentThreadId;
	
	SupplierSearchProcessor(AccoSearchListenerResponder searchListener, ProductSupplier prodSupplier, JSONObject reqJson, Element reqElem) {
		mSearchListener = searchListener;
		mProdSupplier = prodSupplier;
		mReqJson = reqJson;
		mReqElem = (Element) reqElem.cloneNode(true);
		mParentThreadId = Thread.currentThread().getId();
	}
	
	public void process(ProductSupplier prodSupplier, JSONObject reqJson, Element reqElem) {
		OperationConfig opConfig = AccoConfig.getOperationConfig(OPERATION_NAME);
		
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,"./acco:RequestHeader/com:SupplierCredentialsList");
		suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc, 1));
        
		Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./acco:RequestHeader/com:TransactionID");
        transactionElem.setTextContent(transactionElem.getTextContent().concat(String.format("-%s", prodSupplier.getSupplierID())));

        Element resElem = null;
        try {
        	TrackingContext.setTrackingContext(reqJson);
        	 resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					AccoConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
           
			JSONObject resJson = AccoSearchProcessor.getSupplierResponseJSON(reqJson, resElem);
			System.out.println("Si json res"+resJson);
			Map<Integer,String> SI2BRMSRoomMap= new HashMap<Integer,String>();
			JSONObject resSupplierComJson = SupplierCommercials.getSupplierCommercials(reqJson, resJson,
					SI2BRMSRoomMap);
			JSONObject resClientComJson = ClientCommercials.getClientCommercials(resSupplierComJson);
			AccoSearchProcessor.calculatePrices(reqJson, resJson, resClientComJson, resSupplierComJson, SI2BRMSRoomMap, false);
		    System.out.println("INside SupplierSearchProcessor wELCOME"+resJson);
			mSearchListener.receiveSupplierResponse(resJson);
        }
        catch (Exception x) {
			// TODO: handle exception
        	x.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//TrackingContext.setTrackingContext(Thread.currentThread().getId(), TrackingContext.getTrackingContext(mParentThreadId));
				process(mProdSupplier, mReqJson, mReqElem);
	}

}
