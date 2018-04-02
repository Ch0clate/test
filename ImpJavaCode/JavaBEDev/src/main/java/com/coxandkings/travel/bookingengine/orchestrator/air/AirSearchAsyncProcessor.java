package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.enums.TripIndicator;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class AirSearchAsyncProcessor implements AirConstants, Runnable {

	private static final Logger logger = LogManager.getLogger(AirSearchAsyncProcessor.class);
	private JSONObject mReqJson;
	
	public AirSearchAsyncProcessor(JSONObject reqJson) {
		mReqJson = reqJson;
	}
	
	public static String process(JSONObject reqJson) {
        try {
            OperationConfig opConfig = AirConfig.getOperationConfig("search");
            Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
            Document ownerDoc = reqElem.getOwnerDocument();

            TrackingContext.setTrackingContext(reqJson);
            JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
            JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
            TripIndicator tripInd = AirSearchProcessor.deduceTripIndicator(reqHdrJson, reqBodyJson);
            reqBodyJson.put(JSON_PROP_TRIPIND, tripInd.toString());
            

            AirSearchProcessor.createHeader(reqHdrJson, reqElem);

            Element travelPrefsElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody/ota:OTA_AirLowFareSearchRQ/ota:TravelPreferences");
            Element otaReqElem = (Element) travelPrefsElem.getParentNode();
            JSONArray origDestArr = reqBodyJson.getJSONArray(JSON_PROP_ORIGDESTINFO);
            for (int i=0; i < origDestArr.length(); i++) {
                JSONObject origDest = (JSONObject) origDestArr.get(i);
                Element origDestElem = AirSearchProcessor.getOriginDestinationElement(ownerDoc, origDest);
                otaReqElem.insertBefore(origDestElem, travelPrefsElem);
            }

            Element cabinPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CabinPref");
            cabinPrefElem.setAttribute("Cabin", reqBodyJson.getString(JSON_PROP_CABINTYPE));
            travelPrefsElem.appendChild(cabinPrefElem);

            Element priceInfoElem = XMLUtils.getFirstElementAtXPath(otaReqElem, "./ota:TravelerInfoSummary/ota:PriceRequestInformation");
            Element travelerInfoElem = (Element) priceInfoElem.getParentNode();
            JSONArray travellerArr = reqBodyJson.getJSONArray(JSON_PROP_PAXINFO);
            for (int i=0; i < travellerArr.length(); i++) {
                JSONObject traveller = (JSONObject) travellerArr.get(i);
                Element travellerElem = AirSearchProcessor.getAirTravelerAvailElement(ownerDoc, traveller);
                travelerInfoElem.insertBefore(travellerElem, priceInfoElem);
            }

            Element nbyDepsElem = XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:TPA_Extensions/air:NearbyDepartures");
            Element tpaExtnsElem = (Element) nbyDepsElem.getParentNode();

            Element tripTypeElem = ownerDoc.createElementNS(Constants.NS_AIR, "air:TripType");
            tripTypeElem.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPTYPE));
            tpaExtnsElem.insertBefore(tripTypeElem, nbyDepsElem);

            Element tripIndElem = ownerDoc.createElementNS(Constants.NS_AIR, "air:TripIndicator");
            tripIndElem.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPIND));
            tpaExtnsElem.insertBefore(tripIndElem, nbyDepsElem);

            //UserContext usrCtx = UserContext.getUserContextForSession(sessionID);
            UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
            //List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(AirConstants.PRODUCT_AIR);
            List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT);
            
            AirSearchListenerResponder searchListener = new AirSearchListenerResponder(prodSuppliers, reqJson);
            for (ProductSupplier prodSupplier : prodSuppliers) {
                SupplierSearchProcessor suppSrchPrc = new SupplierSearchProcessor(searchListener, prodSupplier, reqJson, reqElem);
            	//AirConfig.getThreadPool().execute(suppSrchPrc);
                AirConfig.execute(suppSrchPrc);
            }
            
            // TODO: The wait time for AirSearchListenerResponder wait is hard-coded. Need to get this from AirConfig or from input request.
            synchronized(searchListener) {
            	searchListener.wait(AirConfig.getAsyncSearchWaitMillis());
            }
            
    		JSONObject clientCtxJson = reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT);
    		String callbackAddr = clientCtxJson.optString(JSON_PROP_CLIENTCALLBACK);
    		if (callbackAddr != null) {
    			// This is a synchronous search request
    			return searchListener.getSearchResponse().toString();
    		}

        }
        catch (Exception x) {
            x.printStackTrace();
            //return "{\"status\": \"ERROR\"}";
        }
        
        logger.debug("AirSearchAsyncProcessor completing and exiting now...");
        return null;
	}

	@Override
	public void run() {
		process(mReqJson);
		
	}
}
