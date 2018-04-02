package com.coxandkings.travel.bookingengine.orchestrator.cruise;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.cruise.CruiseConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class CruiseCabinAvailProcessor implements CruiseConstants {
	
	public static String process(JSONObject reqJson) throws Exception {
		
		OperationConfig opConfig = CruiseConfig.getOperationConfig("cabinavail");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./cru1:OTA_CruiseCabinAvailRQWrapper");
		reqBodyElem.removeChild(wrapperElem);

		TrackingContext.setTrackingContext(reqJson);
		JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);
		
		UserContext userctx = UserContext.getUserContextForSession(reqHdrJson);
		
		CruiseSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
		
		Element suppWrapperElem = null;
		int seqItr =0;
		
		JSONArray categoryOptionsReqArr = reqBodyJson.getJSONArray("cruiseOptions");
		
		for(int i=0;i<categoryOptionsReqArr.length();i++)
		{
			JSONObject cabinOptionsJson = categoryOptionsReqArr.getJSONObject(i);
			
			String suppID = cabinOptionsJson.getString("supplierRef");
			
			suppWrapperElem = (Element) wrapperElem.cloneNode(true);
			reqBodyElem.appendChild(suppWrapperElem);
			
			ProductSupplier prodSupplier = userctx.getSupplierForProduct(PROD_CATEG_TRANSPORT, "Cruise", suppID);
			if (prodSupplier == null) {
				throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
			}
			seqItr++;
			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru1:RequestHeader/com:SupplierCredentialsList");
//--------------------------------------------------------------Adding sequence and suppliercredentialslist------------------------------------------------------
			Element supplierCredentials = prodSupplier.toElement(ownerDoc);
			
			Element sequence = ownerDoc.createElementNS(Constants.NS_COM, "Sequence");
			sequence.setTextContent(String.valueOf(seqItr));
			
			Element credentialsElem = XMLUtils.getFirstElementAtXPath(supplierCredentials, "./com:Credentials");
			supplierCredentials.insertBefore(sequence, credentialsElem);
			
			suppCredsListElem.appendChild(supplierCredentials);
			
//-------------------------------------------------Response Body--------------------------------------------------------------------------------
			
			Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./cru1:SupplierID");
			suppIDElem.setTextContent(suppID);
			
			Element sequenceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./cru1:Sequence");
			sequenceElem.setTextContent(String.valueOf(seqItr));
			
			Element otaCabinAvail = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_CruiseCabinAvailRQ");
			
			CruisePriceProcessor.createPOS(ownerDoc, otaCabinAvail);
			
			JSONArray guestsReqJsonArr = cabinOptionsJson.getJSONArray("Guests");
			CruisePriceProcessor.createGuestsAndGuestCounts(ownerDoc,guestsReqJsonArr,otaCabinAvail);
			
			Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(otaCabinAvail, "./ota:SailingInfo/ota:SelectedSailing");
			selectedSailingElem.setAttribute("VoyageID",cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getString("voyageId"));
			selectedSailingElem.setAttribute("Start",cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getString("start"));
			
			Element cruiseLineElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:CruiseLine");
			cruiseLineElem.setAttribute("ShipCode", cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getJSONObject("cruiseLine").getString("shipCode"));
			cruiseLineElem.setAttribute("VendorCodeContext", cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getJSONObject("cruiseLine").getString("vendorCodeCotext"));
			
			Element selectedCategoryElem = XMLUtils.getFirstElementAtXPath(otaCabinAvail, "./ota:SailingInfo/ota:SelectedCategory");
			selectedCategoryElem.setAttribute("PricedCategoryCode", cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getJSONObject("selectedCategory").getString("PricedCategoryCode"));
			
			Element selectedCabinElem = XMLUtils.getFirstElementAtXPath(selectedCategoryElem, "./ota:SelectedCabin");
			selectedCabinElem.setAttribute("CabinCategoryStatusCode", cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getJSONObject("selectedCategory").getJSONObject("SelectedCabin").getString("CabinCategoryStatusCode"));
			selectedCabinElem.setAttribute("MaxOccupancy", cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getJSONObject("selectedCategory").getJSONObject("SelectedCabin").getString("MaxOccupancy"));
			
			Element ItineraryIDElem = XMLUtils.getFirstElementAtXPath(selectedCategoryElem, "./ota:TPA_Extensions/cru1:ItineraryID");
			ItineraryIDElem.setTextContent(cabinOptionsJson.getJSONObject("SailingInfo").getJSONObject("selectedSailing").getJSONObject("selectedCategory").getJSONObject("SelectedCabin").getJSONObject("TPA_Extensions").getString("ItineraryID"));;
			
			Element selectedFareElem = XMLUtils.getFirstElementAtXPath(otaCabinAvail, "./ota:SelectedFare");
			selectedFareElem.setAttribute("FareCode", cabinOptionsJson.getJSONObject("SelectedFare").getString("FareCode"));
			
			Element sailingElem = XMLUtils.getFirstElementAtXPath(otaCabinAvail, "./ota:TPA_Extensions/cru1:Cruise/cru1:SailingDates/cru1:Sailing");
			sailingElem.setAttribute("SailingID", cabinOptionsJson.getJSONObject("TPA_Extensions").getJSONObject("Cruise").getJSONObject("SailingDates").getJSONObject("Sailing").getString("SailingID"));
			
		}
		System.out.println(XMLTransformer.toString(reqElem));
		
		Element resElem = null;
        resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), CruiseConfig.getHttpHeaders(), reqElem);
        if (resElem == null) {
        	throw new Exception("Null response received from SI");
        }
		
        System.out.println(XMLTransformer.toString(resElem));
        
        JSONObject resBodyJson = new JSONObject();
        JSONArray cabinOptionJsonArr = new JSONArray();
        System.out.println(XMLTransformer.toString(resElem));
        Element[] RswrapperElems = XMLUtils.getElementsAtXPath(resElem, "./cru:ResponseBody/cru1:OTA_CruiseCabinAvailRSWrapper");
        
        for(Element RswrapperElem : RswrapperElems)
        {
//        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_CruiseSailAvailRS");
        	getCabinOptionsJSON(RswrapperElem,cabinOptionJsonArr);
        }
        
        resBodyJson.put("cabinOptions", cabinOptionJsonArr);
        
        System.out.println(resBodyJson.toString());
        
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, resBodyJson);
        
        System.out.println(resJson.toString());
		
		return resJson.toString();
	}
	
	public static void getCabinOptionsJSON(Element wrapperElem, JSONArray cabinOptionJsonArr) throws Exception {
		 
//		 getSupplierResponseSailingOptionsJSONV2(wrapperElem, sailingOptionJsonArr,false);
		getCabinOptionsJSON(wrapperElem, cabinOptionJsonArr,false);
   }

	public static void getCabinOptionsJSON(Element resBodyElem, JSONArray cabinOptionJsonArr,Boolean value) throws Exception {
    	
		 Element[] cabinOptionsElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:OTA_CruiseCabinAvailRS/ota:CabinOptions/ota:CabinOption");
		 for(Element cabinOptionElem : cabinOptionsElems)
		 {
			 JSONObject cabinOptionJson = new JSONObject();
			 
			 cabinOptionJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./cru1:SupplierID"));
			 cabinOptionJson.put("CabinNumber", XMLUtils.getValueAtXPath(cabinOptionElem, "./@CabinNumber"));
			 cabinOptionJson.put("DeckName", XMLUtils.getValueAtXPath(cabinOptionElem, "./@DeckName"));
			 cabinOptionJson.put("DeckNumber", XMLUtils.getValueAtXPath(cabinOptionElem, "./@DeckNumber"));
			 cabinOptionJson.put("Status", XMLUtils.getValueAtXPath(cabinOptionElem, "./@Status"));
			 
			 cabinOptionJson.put("Remark", XMLUtils.getValueAtXPath(cabinOptionElem, "./ota:Remark"));
			 cabinOptionJson.put("IsGuaranteed", XMLUtils.getValueAtXPath(cabinOptionElem, "./ota:TPA_Extensions/cru1:IsGuaranteed"));
			 
			 cabinOptionJsonArr.put(cabinOptionJson);
		 }
   }
	
	 private static JSONObject getRemarkJSON(Element sailingOptionElem)
	 {
		 
		 return null;
	 }
	
}
