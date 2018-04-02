package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.OperationsShellConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class AirAmendProcessor implements AirConstants {
	private static final Logger logger = LogManager.getLogger(AirCancelProcessor.class);
	
	public static String process(JSONObject reqJson) {
		try {
		OperationConfig opConfig = AirConfig.getOperationConfig("amend");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_AirBookModifyRQWrapper");
		reqBodyElem.removeChild(wrapperElem);
		
		TrackingContext.setTrackingContext(reqJson);
		JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
		Element retrieveResElem=getRetrieveResponse(reqJson);
		logger.trace("Retrieve Res XML",XMLTransformer.toString(retrieveResElem));
		
		UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
		
		AirSearchProcessor.createHeader(reqHdrJson, reqElem);
		
	
	
		Element[] retreieveResWrapperElems=sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(retrieveResElem, "./airi:ResponseBody/air:OTA_AirBookRSWrapper"));
		int seqNo = 1;
		Map<String,Integer> supplierSequenceMap=new HashMap<String,Integer>();
		JSONArray amendReqsJsonArr = reqBodyJson.getJSONArray(JSON_PROP_AMENDRQS);
		for(int i=0;i<amendReqsJsonArr.length();i++)
		{
			JSONObject amendReqJson= amendReqsJsonArr.getJSONObject(i);
			JSONArray suppBookRefJsonArr=amendReqJson.getJSONArray(JSON_PROP_SUPPBOOKREFS);
			
			for(int j=0;j<suppBookRefJsonArr.length();j++)
			{
				JSONObject suppBookRefJson = suppBookRefJsonArr.getJSONObject(j);
				String suppID = suppBookRefJson.getString(JSON_PROP_SUPPREF);
				
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				reqBodyElem.appendChild(suppWrapperElem);
				
				ProductSupplier prodSupplier=usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT,suppID);
				if (prodSupplier == null) {
					throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
				}
				
				Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
				Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem, String.format("./com:SupplierCredentials[com:SupplierID = '%s']", suppID));
				if (suppCredsElem == null) {
					suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
				}
				
				if(supplierSequenceMap.containsKey(suppID)) {
					int mapSeqVal=supplierSequenceMap.get(suppID)+1;
					supplierSequenceMap.replace(suppID, mapSeqVal);
				}
				else {
					supplierSequenceMap.put(suppID, seqNo);
				}
				
				Element supplierIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./air:SupplierID");
				supplierIDElem.setTextContent(suppID);
				
				Element sequenceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./air:Sequence");
				sequenceElem.setTextContent(String.valueOf(supplierSequenceMap.get(suppID)));
				
				Element amendRQElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirBookModifyRQ/ota:AirBookModifyRQ");
				
				
				Element retrieveResWrapperElem=getRetrieveWrapperByBookRefId(suppBookRefJson.getString(JSON_PROP_BOOKREFID),retreieveResWrapperElems);
				Map<String,Integer> travellRefMap=getTravellRefMap(retrieveResWrapperElem);
				Map<String,Element> flightSegElemMap=getFlightSegMap(retrieveResWrapperElem);
				
				
				//AirItenerary
				if(amendReqJson.getString(JSON_PROP_AMENDTYPE).equals(CANCEL_TYPE_SSR))
				{
				Element airItinElem=ownerDoc.createElementNS(NS_OTA, "ota:AirItinerary");
				
				Element originDestinationOptionsElem=ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOptions");
				JSONArray odoJsonArr=suppBookRefJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
				for(int k=0;k<odoJsonArr.length();k++)
				{
					
				JSONObject odoJson=	odoJsonArr.getJSONObject(k);
				JSONArray flightSegJsonArr=odoJson.getJSONArray(JSON_PROP_FLIGHTSEG);
				Element originDestinationOptionElem=ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOption");
				
			
				 for(int l=0;l<flightSegJsonArr.length();l++)
				 {
				JSONObject flightSegJson=flightSegJsonArr.getJSONObject(l);
				Element flightSegElem=ownerDoc.createElementNS(NS_OTA, "ota:FlightSegment");
				flightSegElem.setAttribute("DepartureDateTime", flightSegJson.getString(JSON_PROP_DEPARTDATE));
				flightSegElem.setAttribute("ArrivalDateTime",  flightSegJson.getString(JSON_PROP_ARRIVEDATE));
				//TODO:Determine value
				flightSegElem.setAttribute("ConnectionType",  "");
				flightSegElem.setAttribute("FlightNumber",  flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).getString(JSON_PROP_FLIGHTNBR));
				flightSegElem.setAttribute("RPH", Integer.toString(l));
				
				Element depAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:DepartureAirport");
				depAirportElem.setAttribute("LocationCode", flightSegJson.getString(JSON_PROP_ORIGLOC));
				flightSegElem.appendChild(depAirportElem);
				
				Element arrAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:ArrivalAirport");
				arrAirportElem.setAttribute("LocationCode", flightSegJson.getString(JSON_PROP_DESTLOC));
				flightSegElem.appendChild(arrAirportElem);
				
				Element operatingAirlineElem=ownerDoc.createElementNS(NS_OTA, "ota:OperatingAirline");
				operatingAirlineElem.setAttribute("FlightNumber", flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).getString(JSON_PROP_FLIGHTNBR));
				operatingAirlineElem.setAttribute("Code", flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).getString(JSON_PROP_AIRLINECODE));
				flightSegElem.appendChild(operatingAirlineElem);
				
				Element tpaEtxnElem=ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
				
				Element quoteElem=ownerDoc.createElementNS(NS_OTA, "air:QuoteID");
				quoteElem.setTextContent(flightSegJson.getString(JSON_PROP_QUOTEID).toString());
				tpaEtxnElem.appendChild(quoteElem);
				
				Element exntendedRphElem=ownerDoc.createElementNS(NS_OTA, "air:ExtendedRPH");
				exntendedRphElem.setTextContent(flightSegJson.getString(JSON_PROP_EXTENDEDRPH));
				tpaEtxnElem.appendChild(exntendedRphElem);
				flightSegElem.appendChild(tpaEtxnElem);
				
				Element marketingAirlineElem=ownerDoc.createElementNS(NS_OTA, "ota:MarketingAirline");
				marketingAirlineElem.setAttribute("Code",flightSegJson.getJSONObject(JSON_PROP_MARKAIRLINE).getString(JSON_PROP_AIRLINECODE));
				flightSegElem.appendChild(marketingAirlineElem);
				
				Element bookingClassAvailsElem=ownerDoc.createElementNS(NS_OTA, "ota:BookingClassAvails");
				bookingClassAvailsElem.setAttribute("CabinType", "");
				
				Element bookingClassAvailElem=ownerDoc.createElementNS(NS_OTA, "ota:BookingClassAvail");
				bookingClassAvailElem.setAttribute("ResBookDesigCode", "");
				
				bookingClassAvailsElem.appendChild(bookingClassAvailElem);
				flightSegElem.appendChild(bookingClassAvailsElem);
				
				originDestinationOptionElem.appendChild(flightSegElem);
				 }
				originDestinationOptionsElem.appendChild(originDestinationOptionElem);
				}
				airItinElem.appendChild(originDestinationOptionsElem);
				amendRQElem.appendChild(airItinElem);
			}
				
				
				
				/*//priceInfo starts
				Element priceInfoElem=ownerDoc.createElementNS(NS_OTA, "ota:PriceInfo");
				
				Element ptcBreakDownsElem=ownerDoc.createElementNS(NS_OTA, "ota:PTC_FareBreakdowns");
				
				Element ptcBreakDownElem=ownerDoc.createElementNS(NS_OTA, "ota:PTC_FareBreakdown");
				//TODO:Get value from retrieve
				ptcBreakDownElem.setAttribute("FlightRefNumberRPHList", "");
				
				Element passengerTypeQuantity=ownerDoc.createElementNS(NS_OTA, "ota:PassengerTypeQuantity");
				//TODO:Get value from retrieve
				passengerTypeQuantity.setAttribute("Age", "");
				passengerTypeQuantity.setAttribute("Code", "");

				ptcBreakDownElem.appendChild(passengerTypeQuantity);
				
				Element fareInfoElem=ownerDoc.createElementNS(NS_OTA, "ota:FareInfo");
				
				Element deptAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:DepartureAirport");
				//TODO:Get value from retrieve
				deptAirportElem.setAttribute("LocationCode", "");
				fareInfoElem.appendChild(deptAirportElem);
				
				Element arrivalAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:ArrivalAirport");
				//TODO:Get value from retrieve
				arrivalAirportElem.setAttribute("LocationCode", "");
				fareInfoElem.appendChild(arrivalAirportElem);
				
				Element dateElem=ownerDoc.createElementNS(NS_OTA, "ota:Date");
				//TODO:Get value from retrieve
				dateElem.setAttribute("Date", "");
				fareInfoElem.appendChild(dateElem);
				
				Element innerFareInfoElem=ownerDoc.createElementNS(NS_OTA, "ota:FareInfo");
				//TODO:Get value from retrieve
				innerFareInfoElem.setAttribute("FareBasisCode", "");
				
				Element tpEtxInfoElem=ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
					
				Element quoteIdElem=ownerDoc.createElementNS(NS_AIR, "ota:QuoteID");
				quoteIdElem.setTextContent("");
				
				tpEtxInfoElem.appendChild(quoteIdElem);
				
				innerFareInfoElem.appendChild(tpEtxInfoElem);
				
				fareInfoElem.appendChild(innerFareInfoElem);
				ptcBreakDownElem.appendChild(fareInfoElem);
				
				ptcBreakDownsElem.appendChild(ptcBreakDownElem);
				priceInfoElem.appendChild(ptcBreakDownsElem);
				amendRQElem.appendChild(priceInfoElem);
				*/
				//travelerInfo starts
				Element travelerInfoElem=ownerDoc.createElementNS(NS_OTA, "ota:TravelerInfo");
				JSONArray paxDetailsJsonArr=suppBookRefJson.getJSONArray(JSON_PROP_PAXDETAILS);
				
				
				for(int z=0;z<paxDetailsJsonArr.length();z++)
				{
				JSONObject paxDetailJson=paxDetailsJsonArr.getJSONObject(z);
				JSONObject paxDataJson=paxDetailJson.getJSONObject(JSON_PROP_PAXDATA_AMEND);
				
				
				
				Element airTravelerElem=ownerDoc.createElementNS(NS_OTA, "ota:AirTraveler");
				
				
			
					
				
				Element personNameElem=ownerDoc.createElementNS(NS_OTA, "ota:PersonName");
				
				Element namePrefixElem=ownerDoc.createElementNS(NS_OTA, "ota:NamePrefix");
				
				namePrefixElem.setTextContent(paxDetailJson.getString(JSON_PROP_NAMEPREFIX));
				personNameElem.appendChild(namePrefixElem);
				
				Element givenNameElem=ownerDoc.createElementNS(NS_OTA, "ota:GivenName");
				
				givenNameElem.setTextContent(paxDetailJson.getString(JSON_PROP_FIRSTNAME));
				personNameElem.appendChild(givenNameElem);
				
				Element surNameElem=ownerDoc.createElementNS(NS_OTA, "ota:Surname");
				
				surNameElem.setTextContent(paxDetailJson.getString(JSON_PROP_SURNAME));
				personNameElem.appendChild(surNameElem);
				
				Element tpaEtxElem=ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
				
				
				
				/*Element quoteIdElem=ownerDoc.createElementNS(NS_AIR, "air:QuoteID");
				
				
				quoteIdElem.setTextContent(((XMLUtils.getFirstElementAtXPath(travelerRetElem, "./ota:TPA_Extensions/air:QuoteID")).getTextContent()).toString());
				
				tpaEtxElem.appendChild(quoteIdElem);*/
				
				personNameElem.appendChild(tpaEtxElem);
				
				
				//personNameElem.appendChild(travelerRefElem);
				
				airTravelerElem.appendChild(personNameElem);
				
				if(amendReqJson.getString(JSON_PROP_AMENDTYPE).equals(JSON_PROP_AMENDTYPE_PIS))
				{	
					JSONObject telephoneInfoJson=paxDataJson.optJSONObject(JSON_PROP_TELEPHONEINFO);
					if(telephoneInfoJson!=null)
					{
					Element telephoneElem=ownerDoc.createElementNS(NS_OTA, "ota:Telephone");
					telephoneElem.setAttribute("PhoneNumber", telephoneInfoJson.getString(JSON_PROP_PHNO));
					telephoneElem.setAttribute("PhoneUseType", telephoneInfoJson.getString(JSON_PROP_PHUSETYPE));
					
					telephoneElem.setAttribute("RPH", (travellRefMap.get(paxDetailJson.getString(JSON_PROP_FIRSTNAME).concat(paxDetailJson.getString(JSON_PROP_SURNAME)))).toString());
					
					airTravelerElem.appendChild(telephoneElem);
					}
					JSONObject emailInfoJson=paxDataJson.optJSONObject(JSON_PROP_EMAILINFO);
					if(emailInfoJson!=null)
					{
					Element emailElem=ownerDoc.createElementNS(NS_OTA, "ota:Email");
					emailElem.setAttribute("EmailType", emailInfoJson.getString(JSON_PROP_EMAILTYPE));
					
					emailElem.setAttribute("RPH", (travellRefMap.get(paxDetailJson.getString(JSON_PROP_FIRSTNAME).concat(paxDetailJson.getString(JSON_PROP_SURNAME)))).toString());
					//TODO:Determine value
					emailElem.setAttribute("Remark", "Test");
					emailElem.setTextContent(emailInfoJson.getString(JSON_PROP_EMAILID));
					airTravelerElem.appendChild(emailElem);
					}
				}
				
				
				Element paxTypeQuantity=ownerDoc.createElementNS(NS_OTA, "ota:PassengerTypeQuantity");
				paxTypeQuantity.setAttribute("Age", paxDetailJson.getString(JSON_PROP_AGE));
				paxTypeQuantity.setAttribute("Code", paxDetailJson.getString(JSON_PROP_PAXTYPE));
				
				airTravelerElem.appendChild(paxTypeQuantity);
				
				Element travelerRefElem=ownerDoc.createElementNS(NS_OTA, "ota:TravelerRefNumber");
				travelerRefElem.setAttribute("RPH", (travellRefMap.get(paxDetailJson.getString(JSON_PROP_FIRSTNAME).concat(paxDetailJson.getString(JSON_PROP_SURNAME)))).toString());
				
				airTravelerElem.appendChild(travelerRefElem);
				travelerInfoElem.appendChild(airTravelerElem);	
				
				
				if(amendReqJson.getString(JSON_PROP_AMENDTYPE).equals(JSON_PROP_AMENDTYPE_REM) || (amendReqJson.getString(JSON_PROP_AMENDTYPE)).equals(CANCEL_TYPE_SSR))
				{	
					
					
					
					//JSONObject paxDetailJson=paxDetailJsonArr.getJSONObject(q);
					Element specialReqDetailsElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialReqDetails");
					
					if((amendReqJson.getString(JSON_PROP_AMENDTYPE)).equals(JSON_PROP_AMENDTYPE_REM)) {
						
					
					Element specialRemarksElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialRemarks");
					JSONArray paxRemarkArrJson=suppBookRefJson.getJSONArray("paxRemarkInfo");
					for(int r=0;r<paxRemarkArrJson.length();r++)
					{
						JSONObject paxRemarkJson=paxRemarkArrJson.getJSONObject(r);
					Element specialRemarkElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialRemark");
					specialRemarkElem.setAttribute("RemarkType", paxRemarkJson.getString(JSON_PROP_PAXREMARKTYPE));
					
					/*Element flightRefNumberElem=ownerDoc.createElementNS(NS_OTA, "ota:FlightRefNumber");
					specialRemarkElem.setAttribute("RPH", (travellRefMap.get(paxDetailJson.getString(JSON_PROP_FIRSTNAME).concat(paxDetailJson.getString(JSON_PROP_SURNAME)))).toString());*/
					
					//specialRemarkElem.appendChild(flightRefNumberElem);
					
					Element textElem=ownerDoc.createElementNS(NS_OTA, "ota:Text");
					textElem.setTextContent(paxRemarkJson.getString(JSON_PROP_PAXREMARKTEXT));
					
					specialRemarkElem.appendChild(textElem);
					specialRemarksElem.appendChild(specialRemarkElem);
					}
					
					
					
					specialReqDetailsElem.appendChild(specialRemarksElem);
					}
				
					
					if((amendReqJson.getString(JSON_PROP_AMENDTYPE)).equals(CANCEL_TYPE_SSR)) {
						JSONArray ssrJsonArr=paxDetailJson.getJSONArray(JSON_PROP_SPECIALREQUESTINFO);
						Element specialServiceRequestsElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequests");
						for(int p=0;p<ssrJsonArr.length();p++)
						{
							JSONObject ssrJson=ssrJsonArr.getJSONObject(p);
							
							
						Element flightSegElem=flightSegElemMap.get(ssrJson.get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
					
						
						Element specialServiceRequestElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequest");
						specialServiceRequestElem.setAttribute("SSRCode", ssrJson.get(JSON_PROP_SSRCODE).toString());
						specialServiceRequestElem.setAttribute("TravelerRefNumberRPHList", travellRefMap.get(paxDetailJson.getString(JSON_PROP_FIRSTNAME).concat(paxDetailJson.getString(JSON_PROP_SURNAME))).toString());
						specialServiceRequestElem.setAttribute("FlightRefNumberRPHList", ssrJson.get(JSON_PROP_FLIGHTNBR).toString());
						specialServiceRequestElem.setAttribute("Number", ssrJson.opt(JSON_PROP_NUMBER).toString());
						specialServiceRequestElem.setAttribute("Status",  ssrJson.optString(JSON_PROP_STATUS,"NN"));
						specialServiceRequestElem.setAttribute("ServiceQuantity", ssrJson.opt(JSON_PROP_SVCQTY).toString());
						
						
						if(ssrJson.has(JSON_PROP_SVCPRC))
						{
						Element servicePriceElem=ownerDoc.createElementNS(NS_OTA, "ota:ServicePrice");
						servicePriceElem.setAttribute("CurrencyCode", ssrJson.getString(JSON_PROP_CCYCODE));
						servicePriceElem.setAttribute("Total",  ssrJson.getString(JSON_PROP_AMOUNT));
						
						Element basePriceElem=ownerDoc.createElementNS(NS_OTA, "ota:BasePrice");
						basePriceElem.setAttribute("Amount",ssrJson.getJSONObject(JSON_PROP_SVCPRC).getString(JSON_PROP_BASEPRICE));
						
						Element taxesElem=ownerDoc.createElementNS(NS_OTA, "ota:Taxes");
						taxesElem.setAttribute("Amount", ssrJson.getJSONObject(JSON_PROP_SVCPRC).optString((JSON_PROP_TAXES),""));
						
						}
						
						Element airLineElem=ownerDoc.createElementNS(NS_OTA, "ota:Airline");
						airLineElem.setAttribute("Code", XMLUtils.getValueAtXPath(flightSegElem, "./ota:OperatingAirline/@Code"));
						specialServiceRequestElem.appendChild(airLineElem);
						
						Element flightLegElem=ownerDoc.createElementNS(NS_OTA, "ota:FlightLeg");
						flightLegElem.setAttribute("FlightNumber",  ssrJson.get(JSON_PROP_FLIGHTNBR).toString());
						Element flightRetSegElem=flightSegElemMap.get(ssrJson.get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
						flightLegElem.setAttribute("Date", flightRetSegElem.getAttribute("DepartureDateTime"));
						
						Element departureAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:DepartureAirport");
						departureAirportElem.setAttribute("LocationCode", XMLUtils.getValueAtXPath(flightSegElem, "./ota:DepartureAirport/@LocationCode"));
						
						flightLegElem.appendChild(departureAirportElem);
						
						Element arrivalAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:ArrivalAirport");
						arrivalAirportElem.setAttribute("LocationCode",  XMLUtils.getValueAtXPath(flightSegElem, "./ota:ArrivalAirport/@LocationCode"));
						
						flightLegElem.appendChild(arrivalAirportElem);
						specialServiceRequestElem.appendChild(flightLegElem);
						
						
						specialServiceRequestsElem.appendChild(specialServiceRequestElem);
					}
						specialReqDetailsElem.appendChild(specialServiceRequestsElem);
				}
					travelerInfoElem.appendChild(specialReqDetailsElem);
				}
				
				}
				
				
				amendRQElem.appendChild(travelerInfoElem);
				
				 
				Element[] bookRefElemArr=XMLUtils.getElementsAtXPath(retrieveResWrapperElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID");
				  for (Element bookRefElem : bookRefElemArr)
				  {
				
				Element bookReferenceId=ownerDoc.createElementNS(NS_OTA, "ota:BookingReferenceID");
				bookReferenceId.setAttribute("Type", bookRefElem.getAttribute("Type"));
				bookReferenceId.setAttribute("ID", bookRefElem.getAttribute("ID"));
				bookReferenceId.setAttribute("ID_Context", amendReqJson.getString(JSON_PROP_AMENDTYPE));
				amendRQElem.appendChild(bookReferenceId);
				  }
				  
				
				
				
			}
			
			
		}
		
		//System.out.println("AmendRqElem->"+XMLTransformer.toString(reqElem));
		 Element resElem = null;
         resElem = HTTPServiceConsumer.consumeXMLService("SI/Cancel", opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
         if (resElem == null) {
         	throw new Exception("Null response received from SI");
         }
         
         JSONObject resJson = new JSONObject();
         resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
         
         JSONObject resBodyJson = new JSONObject();
        // JSONArray amendReqJsonArr = new JSONArray();
         JSONObject amendReqJson= new JSONObject();
         JSONArray supplierBookReferencesReqJsonArr = new JSONArray();
         
         Element[] resWrapperElems = sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirBookRSWrapper"));
         //int index=0;
         for (Element resWrapperElem : resWrapperElems) {
        	 
        	 
        	 if(XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:ErrorList")!=null)
         	{	
         		//JSONObject amendReq=reqBodyJson.getJSONArray("amendRequests").getJSONObject(index);
         		
         		Element errorMessage=XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:ErrorList/com:Error/com:ErrorCode");
         		String errMsgStr=errorMessage.getTextContent().toString();
         		if(CANCEL_SI_ERR.equalsIgnoreCase(errMsgStr))
         		{	
         			logger.error("This service is not supported. Kindly contact our operations team for support.");
         			//callOperationTodo(resJson,amendReq);
         			resBodyJson.put("ErrorMessage", "This service is not supported. Kindly contact our operations team for support.");
         			resJson.put(JSON_PROP_RESBODY, resBodyJson);
         			return resJson.toString();
         			  
         			//return getSIErrorResponse(resJson).toString();
         		
         			
         		}
         		
         	}
        	 else {
        		 JSONObject supplierBookReferencesReqJson= new JSONObject();
        		 supplierBookReferencesReqJson.put(JSON_PROP_SUPPREF, XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:SupplierID").getTextContent());
        		 supplierBookReferencesReqJson.put(JSON_PROP_BOOKREFIDX, XMLUtils.getValueAtXPath(resWrapperElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID/@ID"));
        		 supplierBookReferencesReqJson.put("comment", XMLUtils.getValueAtXPath(resWrapperElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:Comment/@TextFormat"));
        		
        		 Element commentElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_AirBookRS/ota:AirReservation/ota:Comment");
            	 commentElem.getAttribute("TextFormat");
            	 supplierBookReferencesReqJsonArr.put(supplierBookReferencesReqJson);
            	 
        	 }
        	 
        	
         	    	
         	
         	
        	 
         }
         //TODO:Determine arr req or not
         amendReqJson.put(JSON_PROP_SUPPBOOKREFS, supplierBookReferencesReqJsonArr);
         resBodyJson.put(JSON_PROP_AMENDRQ, amendReqJson);
         resJson.put(JSON_PROP_RESBODY, resBodyJson);
         
			return resJson.toString();
		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}
	
	
private static JSONObject getSIErrorResponse(JSONObject resJson) {
		
		JSONObject errorMessage=new JSONObject();
		
		errorMessage.put("errorMessage", "This service is not supported. Kindly contact our operations team for support.");
		 
		resJson.put(JSON_PROP_RESBODY, errorMessage);
        
		return resJson;
		
	}
	
private static String getSubTypeIdType(String cancelType) {
	
		
		if(cancelType.equals(CANCEL_TYPE_PAX))
		{
			return "PASSENGER";
		}
		else if(cancelType.equals(CANCEL_TYPE_JOU))
		{
			return "JOURNEY";
		}
		else if(cancelType.equals(CANCEL_TYPE_SSR))
		{
			return CANCEL_TYPE_SSR;
		}
		else if(cancelType.equals(CANCEL_TYPE_PRE))
		{
			return "PRE";
		}
		else {
			return "";
		}
		
	}
	
	private static JSONObject callOperationTodo(JSONObject resJson, JSONObject cancelRq) {
		// TODO:Create Request for operation list
		JSONObject operationMessageJson= new JSONObject(new JSONTokener(OperationsShellConfig.getOperationsTodoErrorShell()));
		String operationsUrl=OperationsShellConfig.getOperationsUrl();
		/*Mandatory fields to filled are createdByUserId,productId,referenceId,taskFunctionalAreaId,
		taskNameId,taskPriorityId,taskSubTypeId,taskTypeId*/
		//Add main field dueOn to JSON
		operationMessageJson.put("createdByUserId", "bookingEngine");
		operationMessageJson.put("taskFunctionalAreaId", "OPERATIONS");
		operationMessageJson.put("taskNameId", "CANCEL");
		//TODO:have to decide on the value
		operationMessageJson.put("taskPriorityId", "HIGH");
		//TODO:Determine exact values to be passed for taskSubType
		operationMessageJson.put("taskSubTypeId", getSubTypeIdType(cancelRq.getString(JSON_PROP_CANCELTYPE)));
		//TODO:Determine Value
		operationMessageJson.put("taskTypeId", "MAIN");
		//TODO:Determing value
		operationMessageJson.put("dueOn", "2");
		
		operationMessageJson.put("productId", cancelRq.get("bookID").toString());
		//TODO:Get from db
		operationMessageJson.put("referenceId", cancelRq.get("referenceID").toString());
	
		InputStream httpResStream=HTTPServiceConsumer.consumeService(operationMessageJson, operationsUrl);
		JSONObject opResJson = new JSONObject(new JSONTokener(httpResStream));
		if (logger.isInfoEnabled()) {
			logger.info(String.format("%s JSON Response = %s", opResJson.toString()));
		}
		return opResJson;
	
		
	}

	
private static Map<String, Integer> getTravellRefMap(Element retrieveResElem) {
		
		Map<String,Integer> traverllerRefMap=new HashMap<String,Integer>();
		Element[] travellerInfoElems = XMLUtils.getElementsAtXPath(retrieveResElem,"./ota:OTA_AirBookRS/ota:AirReservation/ota:TravelerInfo/ota:AirTraveler");
		 int travellerIndex=0;
		for (Element travellerInfoElem : travellerInfoElems) {
			 
			 traverllerRefMap.put((XMLUtils.getFirstElementAtXPath(travellerInfoElem, "./ota:PersonName/ota:GivenName").getTextContent().toString().toUpperCase()).concat(XMLUtils.getFirstElementAtXPath(travellerInfoElem, "./ota:PersonName/ota:Surname").getTextContent().toString().toUpperCase()), travellerIndex++);
		}
		
		
		return traverllerRefMap;
	}


private static Element getRetrieveWrapperByBookRefId(String bookRefId, Element[] retreieveResWrapperElems) {
		
		Element reqWrapperElem=null;
	    for (Element wrapperElem : retreieveResWrapperElems) {
	    	//TODO:Does this require more bookRefChecks?
	    	Element reqBookRsElem=XMLUtils.getFirstElementAtXPath(wrapperElem, String.format("./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID[@ID='%s']",bookRefId));
	    	if(reqBookRsElem!=null)
	    	{
	    	
	    	reqWrapperElem=(Element) reqBookRsElem.getParentNode().getParentNode().getParentNode();
	    	break;
	    	}
	    	
	    }
		return reqWrapperElem;
	}
private static Map<String, Element> getFlightSegMap(Element retrieveResElem) {
	
	Map<String,Element> flightSegMap=new HashMap<String,Element>();
	//System.out.println("RetrieveResElem=>"+XMLTransformer.toString(retrieveResElem));
	Element[] odosElems = XMLUtils.getElementsAtXPath(retrieveResElem,"./ota:OTA_AirBookRS/ota:AirReservation/ota:AirItinerary/ota:OriginDestinationOptions/ota:OriginDestinationOption");
	for (Element odoElem : odosElems) {
		Element[] flightSegElems=XMLUtils.getElementsAtXPath(odoElem,"./ota:FlightSegment");
		for (Element flightSegElem : flightSegElems) {
			flightSegMap.put(XMLUtils.getFirstElementAtXPath(flightSegElem, "./ota:OperatingAirline").getAttribute("FlightNumber").toString().replaceAll("\\s+",""), flightSegElem);
			
			
	}
	}
	return flightSegMap;
}
	

public static Element getRetrieveResponse(JSONObject reqJson) {
	  JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
      JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
      
      try {
      	OperationConfig opConfig = AirConfig.getOperationConfig("retrievepnr");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			
			TrackingContext.setTrackingContext(reqJson);
			
			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_ReadRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			
			AirSearchProcessor.createHeader(reqHdrJson, reqElem);
			
			int seqNo = 1;
			Map<String,Integer> supplierSequenceMap=new HashMap<String,Integer>();
			JSONArray retrieveReqsJSONArr = reqBodyJson.getJSONArray("amendRequests");
			for(int i=0;i<retrieveReqsJSONArr.length();i++) {
				JSONObject retrieveJson=retrieveReqsJSONArr.getJSONObject(i);
				JSONArray supplierBookRefArr=retrieveJson.getJSONArray("supplierBookReferences");
				for(int j=0;j<supplierBookRefArr.length();j++)
				{
					JSONObject suppBookRefJson = supplierBookRefArr.getJSONObject(j);
					
					String suppID = suppBookRefJson.getString(JSON_PROP_SUPPREF);
					Element suppWrapperElem = null;

					suppWrapperElem = (Element) wrapperElem.cloneNode(true);
					reqBodyElem.appendChild(suppWrapperElem);
					
					ProductSupplier prodSupplier=usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT,suppID);
					if (prodSupplier == null) {
						throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
					}
					
					Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
					Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem, String.format("./com:SupplierCredentials[com:SupplierID = '%s']", suppID));
					if (suppCredsElem == null) {
						suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
					}
					Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./air:SupplierID");
					suppIDElem.setTextContent(suppID);
					
					if(supplierSequenceMap.containsKey(suppID)) {
						int mapSeqVal=supplierSequenceMap.get(suppID)+1;
						supplierSequenceMap.replace(suppID, mapSeqVal);
					}
					else {
						supplierSequenceMap.put(suppID, seqNo);
					}
					
					Element sequenceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./air:Sequence");
					sequenceElem.setTextContent(String.valueOf(supplierSequenceMap.get(suppID)));
					
					
					
					Element uniqueIdElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_ReadRQ/ota:UniqueID");
					
					uniqueIdElem.setAttribute("Type","14");
					uniqueIdElem.setAttribute("ID", suppBookRefJson.getString(JSON_PROP_BOOKREFID));
					
					
				}
			}
			
			//System.out.println("reqElem->"+XMLTransformer.toString(reqElem));
			
			Element resElem = null;
          resElem = HTTPServiceConsumer.consumeXMLService("SI/Cancel", opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
          if (resElem == null) {
          	throw new Exception("Null response received from SI");
          }
          
         // System.out.println("resElem->"+XMLTransformer.toString(resElem));
          return resElem;
        
      	
      }
      catch(Exception E) {
      	return null;
      }
      
      
      
		
}
public static Element[] sortWrapperElementsBySequence(Element[] wrapperElems) {
	Map<Integer, Element> wrapperElemsMap = new TreeMap<Integer, Element>();
	for (Element wrapperElem : wrapperElems) {
		wrapperElemsMap.put(Utils.convertToInt(XMLUtils.getValueAtXPath(wrapperElem, "./air:Sequence"), 0), wrapperElem);
	}
	
	int  idx = 0;
	Element[] seqSortedWrapperElems = new Element[wrapperElems.length];
	Iterator<Map.Entry<Integer, Element>> wrapperElemsIter = wrapperElemsMap.entrySet().iterator();
	while (wrapperElemsIter.hasNext()) {
		seqSortedWrapperElems[idx++] = wrapperElemsIter.next().getValue();
	}
	
	return seqSortedWrapperElems;
}
}
