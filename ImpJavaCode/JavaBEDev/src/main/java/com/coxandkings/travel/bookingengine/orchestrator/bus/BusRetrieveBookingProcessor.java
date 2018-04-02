package com.coxandkings.travel.bookingengine.orchestrator.bus;


import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.bus.BusConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class BusRetrieveBookingProcessor implements BusConstants{

	public static String process(JSONObject reqJson) {
		
		try {
			OperationConfig opConfig = BusConfig.getOperationConfig("RetrieveBooking");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true); // xmshell
			Document ownerDoc = reqElem.getOwnerDocument();
			
			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./busi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./bus:OTA_BusGetTicketDetailsRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			
            TrackingContext.setTrackingContext(reqJson);
			
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_TRANSPORT,PROD_CATEG_SUBTYPE_BUS);

			
			BusSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
			
			JSONArray busserviceJSONArr = reqBodyJson.getJSONArray(JSON_PROP_SERVICE);
			
			for(int i=0;i<busserviceJSONArr.length();i++)
			{
				JSONObject busServiceJson = busserviceJSONArr.getJSONObject(i);
				
				
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				reqBodyElem.appendChild(suppWrapperElem);
				
				Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
						"./bus:RequestHeader/com:SupplierCredentialsList");
				for (ProductSupplier prodSupplier : prodSuppliers) {
					suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
				}
		        
		        XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:SupplierID", busServiceJson.getString(JSON_PROP_SUPPREF));
		        
		        Element otaGetTkt = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_BusGetTicketDetailsRQ");
		        Element newElem;
		        
		        if(busServiceJson.get("TicketNo").toString().isEmpty()==false)
				  {
					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "TicketNo");
					  newElem.setTextContent(busServiceJson.get("TicketNo").toString());
					  otaGetTkt.appendChild(newElem);
				  }
		        
		        if(busServiceJson.get("strPNRNo").toString().isEmpty()==false)
				  {
					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "strPNRNo");
					  newElem.setTextContent(busServiceJson.get("strPNRNo").toString());
					  otaGetTkt.appendChild(newElem);
				  }
		        
			}
			
			System.out.println(XMLTransformer.toString(reqElem));
			  Element resElem = null;
	          resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), BusConfig.getHttpHeaders(), reqElem);
	          if (resElem == null) {
	          	throw new Exception("Null response received from SI");
	          }
			System.out.println(XMLTransformer.toString(resElem));
	          
	          JSONObject resBodyJson = new JSONObject();
		      JSONObject getTktJson = new JSONObject();
		      
		      
		      Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./busi:ResponseBody/bus:OTA_BusGetTicketDetailsRSWrapper");
		        for (Element wrapperElement : wrapperElems) {
		        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ota:OTA_BusGetTicketDetailsRS/ota:GetTicketDetailsRS");
		        	getTktJSON(resBodyElem, getTktJson);
		        }
		        resBodyJson.put("bookTicket", getTktJson);
		        
		        JSONObject resJson = new JSONObject();
		        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
		        resJson.put(JSON_PROP_RESBODY, resBodyJson);
		        System.out.println(resJson.toString());
		return resJson.toString();
	}
	catch(Exception e )
		{
		e.printStackTrace();
		return null;
		}
		
	

}

	private static void getTktJSON(Element resBodyElem, JSONObject getTktJson) {
		
		getTktJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./bus:SupplierID"));
		getTktJson.put("BusTypeName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:BusTypeName"));
		getTktJson.put("servicename", XMLUtils.getValueAtXPath(resBodyElem, "./ota:servicename"));
		getTktJson.put("Board_Halt_Time", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Board_Halt_Time"));
		getTktJson.put("Name", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Name"));
		getTktJson.put("Gender", XMLUtils.getValueAtXPath(resBodyElem, "./ota:serviceno"));
		getTktJson.put("DropOff", XMLUtils.getValueAtXPath(resBodyElem, "./ota:DropOff"));
		getTktJson.put("Discount", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Discount"));
		getTktJson.put("DiscountPer", XMLUtils.getValueAtXPath(resBodyElem, "./ota:DiscountPer"));
		getTktJson.put("DiscountReason", XMLUtils.getValueAtXPath(resBodyElem, "./ota:DiscountReason"));
		getTktJson.put("OrderID", XMLUtils.getValueAtXPath(resBodyElem, "./ota:OrderID"));
		getTktJson.put("TransactionId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:TransactionId"));
		getTktJson.put("TicketNo", XMLUtils.getValueAtXPath(resBodyElem, "./ota:TicketNo"));
        getTktJson.put("PNRNo", XMLUtils.getValueAtXPath(resBodyElem, "./ota:PNRNo"));
        getTktJson.put("Status", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Status"));
        getTktJson.put("OperatorName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:OperatorName"));
        getTktJson.put("RouteScheduleId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:RouteScheduleId"));
        getTktJson.put("CompanyId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:CompanyId"));
        getTktJson.put("FromCityName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:FromCityName"));
        getTktJson.put("ToCityName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ToCityNamee"));
        getTktJson.put("JourneyDate", XMLUtils.getValueAtXPath(resBodyElem, "./ota:JourneyDate"));
        getTktJson.put("BookingDate", XMLUtils.getValueAtXPath(resBodyElem, "./ota:BookingDate"));
        getTktJson.put("Landmark", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Landmark"));
        getTktJson.put("Start_Time", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Start_Time"));
        getTktJson.put("Arr_Time", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Arr_Time"));
        getTktJson.put("Reporting_Time", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Reporting_Time"));
        getTktJson.put("Boarding_Place_Name", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Boarding_Place_Name"));
        getTktJson.put("Pickup", getpickupDetails(resBodyElem));
        JSONArray passArr = new JSONArray();
		Element[] passangerElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:Passengers/ota:Passenger");
		for(Element passElem : passangerElems)
		{
			BusBookProcessor.getPassangers(passElem,passArr);
		}

		getTktJson.put("Passengers", passArr);
        getTktJson.put("ContactInfo", getContactInfo(resBodyElem));
        getTktJson.put("TotalSeats", XMLUtils.getValueAtXPath(resBodyElem, "./ota:TotalSeats"));
        getTktJson.put("TotalFare", XMLUtils.getValueAtXPath(resBodyElem, "./ota:TotalFaree"));
        getTktJson.put("BookingTotalFare", XMLUtils.getValueAtXPath(resBodyElem, "./ota:BookingTotalFare"));
        getTktJson.put("IsCOD", XMLUtils.getValueAtXPath(resBodyElem, "./ota:IsCOD"));
        getTktJson.put("CODCharges", XMLUtils.getValueAtXPath(resBodyElem, "./ota:CODCharges"));
        getTktJson.put("IsDelivered", XMLUtils.getValueAtXPath(resBodyElem, "./ota:IsDelivered"));
        getTktJson.put("CancelDate", XMLUtils.getValueAtXPath(resBodyElem, "./ota:CancelDate"));
        getTktJson.put("CurrencyCode", XMLUtils.getValueAtXPath(resBodyElem, "./ota:CurrencyCode"));
        getTktJson.put("CancellationPolicyDescription", XMLUtils.getValueAtXPath(resBodyElem, "./ota:CancellationPolicyDescription"));
        getTktJson.put("ResponseMessage", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ResponseMessage"));
		
	}

	private static Object getContactInfo(Element resBodyElem) {
		
		JSONObject contactJson = new JSONObject();
		contactJson.put("CustomerName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ContactInfo/ota:CustomerName"));
		contactJson.put("Email", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ContactInfo/ota:Email"));
		contactJson.put("Phone", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ContactInfo/ota:Phone"));
		contactJson.put("Mobile", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ContactInfo/ota:Mobile"));
		contactJson.put("ContactInfo", XMLUtils.getValueAtXPath(resBodyElem, "./ota:ContactInfo/ota:ContactInfo"));

		return contactJson;
	}

	private static JSONObject getpickupDetails(Element resBodyElem) {
		
		JSONObject pickupJson = new JSONObject();
		pickupJson.put("ProviderId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:ProviderId"));
		pickupJson.put("PickupId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:PickupId"));
		pickupJson.put("PickupName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:PickupName"));
		pickupJson.put("PickupTime", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:PickupTimed"));
		pickupJson.put("PkpTime", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:PkpTime"));
		pickupJson.put("Address", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:Address"));
		pickupJson.put("Landmark", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:Landmark"));
		pickupJson.put("ProviderPickupId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:ProviderPickupId"));
		pickupJson.put("Phone", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Pickup/ota:Phone"));
		return pickupJson;
	}
}
