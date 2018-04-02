package com.coxandkings.travel.bookingengine.orchestrator.air;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.enums.TripIndicator;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.OperationsShellConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

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

public class AirCancelProcessor implements AirConstants  {

	private static final Logger logger = LogManager.getLogger(AirCancelProcessor.class);
	//public static final String OPERATION = "LowFarePrice";
	//public static final String HTTP_AUTH_BASIC_PREFIX = "Basic ";

	public static Element getAirTravelerAvailElement(Document ownerDoc, JSONObject traveller) {
		Element travellerElem = ownerDoc.createElementNS(NS_OTA, "ota:AirTravelerAvail");
		Element psgrElem = ownerDoc.createElementNS(NS_OTA, "ota:PassengerTypeQuantity");
		
		psgrElem.setAttribute("Code", traveller.getString(JSON_PROP_PAXTYPE));
		psgrElem.setAttribute("Quantity", String.valueOf(traveller.getInt(JSON_PROP_QTY)));
		travellerElem.appendChild(psgrElem);
		return travellerElem;
	}

	public static String process(JSONObject reqJson) {
		try {
			OperationConfig opConfig = AirConfig.getOperationConfig("cancel");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			
			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_CancelRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			
			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
            TripIndicator tripInd = AirSearchProcessor.deduceTripIndicator(reqHdrJson, reqBodyJson);
            reqBodyJson.put(JSON_PROP_TRIPIND, tripInd.toString());

			Element retrieveResElem=getRetrieveResponse(reqJson);
			logger.trace("Retrieve Res XML",XMLTransformer.toString(retrieveResElem));
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			
			AirSearchProcessor.createHeader(reqHdrJson, reqElem);
			
		
		
			Element[] retreieveResWrapperElems=sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(retrieveResElem, "./airi:ResponseBody/air:OTA_AirBookRSWrapper"));
			
			
			Map<String,Integer> supplierSequenceMap=new HashMap<String,Integer>();

			int seqNo = 1;
			JSONArray cancelReqsJSONArr = reqBodyJson.getJSONArray(JSON_PROP_CANCELREQS);
			for (int y=0; y < cancelReqsJSONArr.length(); y++) {
				JSONObject cancelReqJson = cancelReqsJSONArr.getJSONObject(y);
				
				JSONArray suppBookRefsJsonArr = cancelReqJson.getJSONArray(JSON_PROP_SUPPBOOKREFS);
				for (int z=0; z < suppBookRefsJsonArr.length(); z++) {
				
					
					
					
					JSONObject suppBookRefJson = suppBookRefsJsonArr.getJSONObject(z);
				
					
					
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
					
				
					
					Element cancelRQElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_CancelRQ");
					Element uniqueIdElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_CancelRQ/ota:UniqueID");
					//TODO:Get Type from bookResponse;Determine where to get
					uniqueIdElem.setAttribute("Type", "14");
					uniqueIdElem.setAttribute("ID", suppBookRefJson.getString(JSON_PROP_BOOKREFID));
					uniqueIdElem.setAttribute("ID_Context", cancelReqJson.getString(JSON_PROP_CANCELTYPE));
					
				
					if(cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_JOU) || cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_SSR) || cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_PAX)) {
						
						Element retrieveResWrapperElem=getRetrieveWrapperByBookRefId(suppBookRefJson.getString(JSON_PROP_BOOKREFID),retreieveResWrapperElems);
						Map<String,Element> flightSegElemMap=getFlightSegMap(retrieveResWrapperElem);
						Map<String,Integer> travellRefMap=getTravellRefMap(retrieveResWrapperElem);
					
						//TODO:Determine if and where to get this elem attrs from
						Element companyNameElem=ownerDoc.createElementNS(NS_OTA, "ota:CompanyName");
						
					
						companyNameElem.setAttribute("CompanyShortName", "");
						companyNameElem.setAttribute("CodeContext", "");
						uniqueIdElem.appendChild(companyNameElem);
						//odi part for pax cancel
						
						JSONArray paxDetailsJsonArr=suppBookRefJson.getJSONArray(JSON_PROP_PAXDETAILS);
						if(cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_PAX))
						{
						Element originDestSegElem=ownerDoc.createElementNS(NS_OTA, "ota:OriginAndDestinationSegment");
						
						Element originLocationElem=ownerDoc.createElementNS(NS_OTA, "ota:OriginLocation");
						
						originLocationElem.setAttribute("LocationCode", "");
						originDestSegElem.appendChild(originLocationElem);
						
						Element destLocationElem=ownerDoc.createElementNS(NS_OTA, "ota:DestinationLocation");
					
						destLocationElem.setAttribute("LocationCode", "");
						originDestSegElem.appendChild(destLocationElem);
						
						
						for(int n=0;n<paxDetailsJsonArr.length();n++)
						{
						JSONObject paxDetJson=	paxDetailsJsonArr.getJSONObject(n);
						
						Element travellerElem=ownerDoc.createElementNS(NS_OTA, "ota:Traveler");
						
						Element givenNameElem=ownerDoc.createElementNS(NS_OTA, "ota:GivenName");						
						givenNameElem.setTextContent(paxDetJson.getString(JSON_PROP_FIRSTNAME));
						travellerElem.appendChild(givenNameElem);
						
						Element surNameElem=ownerDoc.createElementNS(NS_OTA, "ota:Surname");
						surNameElem.setTextContent(paxDetJson.getString(JSON_PROP_SURNAME));
						travellerElem.appendChild(surNameElem);
						
						Element tpaExtentionsElem=ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
						
						Element travellerRefNoElem=ownerDoc.createElementNS(NS_AIR, "air:TravelerRefNumber");
						
						travellerRefNoElem.setAttribute("RPH", travellRefMap.get(paxDetJson.getString(JSON_PROP_FIRSTNAME).concat(paxDetJson.getString(JSON_PROP_SURNAME))).toString());
						tpaExtentionsElem.appendChild(travellerRefNoElem);
						travellerElem.appendChild(tpaExtentionsElem);
						
						originDestSegElem.appendChild(travellerElem);
						}
						cancelRQElem.appendChild(originDestSegElem);
					}
						
						if(cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_JOU) || cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_SSR) )
						{
						Element tpaEtxElem=ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
						
						//Airitenary for tpaExtension
				
						Element airIternaryElem=ownerDoc.createElementNS(NS_AIR, "air:AirItinerary");
						
						Element odosElem=ownerDoc.createElementNS(NS_AIR, "air:OriginDestinationOptions");
						
						JSONArray odosJsonArr=suppBookRefJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
						
						for(int j=0;j<odosJsonArr.length();j++)
						{
						Element odoElem=ownerDoc.createElementNS(NS_AIR, "air:OriginDestinationOption");
						JSONObject odoJson=odosJsonArr.getJSONObject(j);
						JSONArray flightSegJsonArr=odoJson.getJSONArray(JSON_PROP_FLIGHTSEG);
						for(int k=0;k<flightSegJsonArr.length();k++)
						{
							JSONObject flightSegJson=flightSegJsonArr.getJSONObject(k);
							Element retrieveFlightSeg=flightSegElemMap.get(flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
							
						Element flightSegElem=ownerDoc.createElementNS(NS_AIR, "air:FlightSegment");
						flightSegElem.setAttribute("RPH",Integer.toString(j));
					
					
						if(retrieveFlightSeg.getAttribute("ConnectionType").toString().isEmpty())
						{
							flightSegElem.setAttribute("ConnectionType","");
						}
						else {
							flightSegElem.setAttribute("ConnectionType",retrieveFlightSeg.getAttribute("ConnectionType").toString());
						}
						
						flightSegElem.setAttribute("FlightNumber", flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
						flightSegElem.setAttribute("DepartureDateTime",  flightSegJson.get(JSON_PROP_DEPARTDATE).toString().replaceAll("\\s+",""));
						flightSegElem.setAttribute("ArrivalDateTime", flightSegJson.get(JSON_PROP_ARRIVEDATE).toString().replaceAll("\\s+",""));
						
						flightSegElem.setAttribute("Status", retrieveFlightSeg.getAttribute("Status"));
						
						Element departureAirportElem=ownerDoc.createElementNS(NS_AIR, "air:DepartureAirport");
						departureAirportElem.setAttribute("LocationCode", flightSegJson.get(JSON_PROP_ORIGLOC).toString().replaceAll("\\s+",""));
						departureAirportElem.setAttribute("Terminal", flightSegJson.optString(((JSON_PROP_DEPARTTERMINAL).toString().replaceAll("\\s+","")),""));
						flightSegElem.appendChild(departureAirportElem);
						
						Element arrivalAirportElem=ownerDoc.createElementNS(NS_AIR, "air:ArrivalAirport");
						arrivalAirportElem.setAttribute("LocationCode", flightSegJson.get(JSON_PROP_DESTLOC).toString().replaceAll("\\s+",""));
						arrivalAirportElem.setAttribute("Terminal",  flightSegJson.optString(((JSON_PROP_ARRIVETERMINAL).toString().replaceAll("\\s+","")),""));
						flightSegElem.appendChild(arrivalAirportElem);
						
						
						Element operatingAirlineElem=ownerDoc.createElementNS(NS_AIR, "air:OperatingAirline");
						operatingAirlineElem.setAttribute("Code", flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).get(JSON_PROP_AIRLINECODE).toString().replaceAll("\\s+",""));
						operatingAirlineElem.setAttribute("FlightNumber", flightSegJson.getJSONObject(JSON_PROP_OPERAIRLINE).get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
						flightSegElem.appendChild(operatingAirlineElem);
						
						Element tpaExtnElem=ownerDoc.createElementNS(NS_AIR, "air:TPA_Extensions");
						
						Element extendedRphElem=ownerDoc.createElementNS(NS_AIR, "air:ExtendedRPH");
						extendedRphElem.setTextContent(flightSegJson.get(JSON_PROP_EXTENDEDRPH).toString());
						tpaExtnElem.appendChild(extendedRphElem);
						
						flightSegElem.appendChild(tpaExtnElem);
						
						odoElem.appendChild(flightSegElem);
						}
						
						odosElem.appendChild(odoElem);
						}
						
						airIternaryElem.appendChild(odosElem);
						
						tpaEtxElem.appendChild(airIternaryElem);
						if(cancelReqJson.get(JSON_PROP_CANCELTYPE).equals(CANCEL_TYPE_SSR)) 
								{
						Element ssrElem=ownerDoc.createElementNS(NS_AIR, "air:SSR");
					
						Element specSSRElems=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequests");
						
						
						
						
						Element specialServiceRequestsElem=XMLUtils.getFirstElementAtXPath(retrieveResElem, "./airi:ResponseBody/air:OTA_AirBookRSWrapper/ota:OTA_AirBookRS/ota:AirReservation/ota:TravelerInfo/ota:SpecialReqDetails/ota:SpecialServiceRequests");
						for(int k=0;k<paxDetailsJsonArr.length();k++)
						{
							JSONObject paxDetailJson=paxDetailsJsonArr.getJSONObject(k);
						int travellerRPHList=travellRefMap.get(paxDetailJson.get(JSON_PROP_FIRSTNAME).toString().concat(paxDetailJson.get(JSON_PROP_SURNAME).toString()));
						
						JSONArray ssrJsonArr=paxDetailJson.getJSONArray(JSON_PROP_SPECIALREQUESTINFO);
						
						
						JSONObject ssrJson=ssrJsonArr.getJSONObject(0);
						Element[] retrieveSsrElems=XMLUtils.getElementsAtXPath(specialServiceRequestsElem,String.format("./ota:SpecialServiceRequest[@TravelerRefNumberRPHList='%s']",Integer.toString(travellerRPHList)));
						
					
						for(int l=0;l<ssrJsonArr.length();l++)
						{
							
						Element retrieveSsrElem=getSsrElemForTraveller(retrieveSsrElems, ssrJson.getString(JSON_PROP_SSRCODE));
						Element flightSegElem=flightSegElemMap.get(ssrJson.get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
						
						Element specSSRElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequest");
						specSSRElem.setAttribute("FlightRefNumberRPHList", ssrJson.get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
						specSSRElem.setAttribute("TravelerRefNumberRPHList", Integer.toString(travellerRPHList));
						specSSRElem.setAttribute("SSRCode", ssrJson.get(JSON_PROP_SSRCODE).toString());
						specSSRElem.setAttribute("Number",retrieveSsrElem.getAttribute("Number").toString() );
						specSSRElem.setAttribute("ServiceQuantity", retrieveSsrElem.getAttribute("ServiceQuantity").toString());
						specSSRElem.setAttribute("Status", retrieveSsrElem.getAttribute("Status").toString());
						
						Element airLineElem=ownerDoc.createElementNS(NS_OTA, "ota:Airline");
						airLineElem.setAttribute("Code", XMLUtils.getValueAtXPath(flightSegElem, "./ota:OperatingAirline/@Code"));
						specSSRElem.appendChild(airLineElem);
						
						Element flightLegElem=ownerDoc.createElementNS(NS_OTA, "ota:FlightLeg");
						flightLegElem.setAttribute("FlightNumber",  ssrJson.get(JSON_PROP_FLIGHTNBR).toString().replaceAll("\\s+",""));
						flightLegElem.setAttribute("Date", "");
						
						Element departureAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:DepartureAirport");
						departureAirportElem.setAttribute("LocationCode", XMLUtils.getValueAtXPath(flightSegElem, "./ota:DepartureAirport/@LocationCode"));
						
						flightLegElem.appendChild(departureAirportElem);
						
						Element arrivalAirportElem=ownerDoc.createElementNS(NS_OTA, "ota:ArrivalAirport");
						arrivalAirportElem.setAttribute("LocationCode",  XMLUtils.getValueAtXPath(flightSegElem, "./ota:ArrivalAirport/@LocationCode"));
						
						flightLegElem.appendChild(arrivalAirportElem);
						
						
						specSSRElem.appendChild(flightLegElem);
						
						specSSRElems.appendChild(specSSRElem);
						
						}
						
						
						
						
						}
						ssrElem.appendChild(specSSRElems);
						
						tpaEtxElem.appendChild(ssrElem);
						}
						cancelRQElem.appendChild(tpaEtxElem);
						}
						
						
						
						
						
						
					}
				}
			}
			
			// logger.trace("ReqCancelElem->",XMLTransformer.toString(reqElem));
            Element resElem = null;
            resElem = HTTPServiceConsumer.consumeXMLService("SI/Cancel", opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
            if (resElem == null) {
            	throw new Exception("Null response received from SI");
            }
          
           // logger.trace("ResCancelElem->",XMLTransformer.toString(resElem));
            JSONObject resJson = new JSONObject();
            resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
           //System.out.println("ResCancelElem->"+XMLTransformer.toString(resElem));
            JSONObject resBodyJson = new JSONObject();
            JSONArray cancelReqJsonArr = new JSONArray();
            JSONObject cancelReqJson= new JSONObject();
            JSONArray supplierBookReferencesReqJsonArr = new JSONArray();
            Element[] resWrapperElems = sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_CancelRSWrapper"));
           int index=0;
            for (Element resWrapperElem : resWrapperElems) {
            	
            	if(XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:ErrorList")!=null)
            	{	
            		JSONObject cancelReq=reqBodyJson.getJSONArray(JSON_PROP_CANCELREQS).getJSONObject(index);
            		
            		Element errorMessage=XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:ErrorList/com:Error/com:ErrorCode");
            		String errMsgStr=errorMessage.getTextContent().toString();
            		if(CANCEL_SI_ERR.equalsIgnoreCase(errMsgStr))
            		{	
            			logger.error("This service is not supported. Kindly contact our operations team for support.");
            			callOperationTodo(resJson,cancelReq);
            			return getSIErrorResponse(resJson).toString();
            		
            			
            		}
            		
            	}
            	Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_CancelRS");
            	
            	
            	JSONObject supplierBookReferencesReqJson= new JSONObject();
            	
            	
            	supplierBookReferencesReqJson.put(JSON_PROP_SUPPREF, XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:SupplierID").getTextContent());
            	supplierBookReferencesReqJson.put(JSON_PROP_BOOKREFID, XMLUtils.getFirstElementAtXPath(resBodyElem, "./ota:UniqueID").getAttribute("ID"));
            	
            
            	Element[] cancelRulesElemArr=XMLUtils.getElementsAtXPath(resBodyElem, "./ota:CancelInfoRS/ota:CancelRules/ota:CancelRule");
            	JSONArray cancelRulesJsonArr=new JSONArray();
            	for(Element cancelRuleElem :cancelRulesElemArr) {
            		JSONObject cancelRuleJson=new JSONObject();
            		cancelRuleJson.put(cancelRuleElem.getAttribute("Type"), cancelRuleElem.getAttribute("Amount"));
            		cancelRulesJsonArr.put(cancelRuleJson);
            	}
            	supplierBookReferencesReqJson.put(JSON_PROP_CANCELRULES, cancelRulesJsonArr);
            	
            	
            	
            	
            	supplierBookReferencesReqJsonArr.put(supplierBookReferencesReqJson); 
            	index++;
            	
            }
            
            //Currently the complete use of cancelReq array is not determined
            cancelReqJson.put(JSON_PROP_SUPPBOOKREFS, supplierBookReferencesReqJsonArr);
        	cancelReqJsonArr.put(cancelReqJson);
        	
            resBodyJson.put(JSON_PROP_CANCELREQUESTS, cancelReqJsonArr);
           
            resJson.put(JSON_PROP_RESBODY, resBodyJson);
            
			return resJson.toString();
		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	

	private static Element getRetrieveWrapperByBookRefId(String bookRefId, Element[] retreieveResWrapperElems) {
		
		Element reqWrapperElem=null;
	    for (Element wrapperElem : retreieveResWrapperElems) {
	    	Element reqBookRsElem=XMLUtils.getFirstElementAtXPath(wrapperElem, String.format("./ota:OTA_AirBookRS/ota:AirReservation/ota:BookingReferenceID[@ID='%s']",bookRefId));
	    	if(reqBookRsElem!=null)
	    	{
	    	reqWrapperElem=(Element) reqBookRsElem.getParentNode().getParentNode().getParentNode();
	    	break;
	    	}
	    	
	    }
		return reqWrapperElem;
	}


	
	private static Element getSsrElemForTraveller(Element[] retrieveSsrElems, String ssrCode) {
		Element ssrElem=null;
		
		for (Element retrieveSsrElem : retrieveSsrElems)
		{	
			
			if(retrieveSsrElem.getAttribute("SSRCode").equals(ssrCode))
			{
		 ssrElem=retrieveSsrElem;
		 break;
			}
			
		}
		
		return ssrElem;
	}

	private static Map<String, Element> getFlightSegMap(Element retrieveResElem) {
		
		Map<String,Element> flightSegMap=new HashMap<String,Element>();
		Element[] odosElems = XMLUtils.getElementsAtXPath(retrieveResElem,"./ota:OTA_AirBookRS/ota:AirReservation/ota:AirItinerary/ota:OriginDestinationOptions/ota:OriginDestinationOption");
		for (Element odoElem : odosElems) {
			Element[] flightSegElems=XMLUtils.getElementsAtXPath(odoElem,"./ota:FlightSegment");
			for (Element flightSegElem : flightSegElems) {
				flightSegMap.put(XMLUtils.getFirstElementAtXPath(flightSegElem, "./ota:OperatingAirline").getAttribute("FlightNumber").toString().replaceAll("\\s+",""), flightSegElem);
				
				
		}
		}
		return flightSegMap;
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
			return "SSR";
		}
		else if(cancelType.equals(CANCEL_TYPE_PRE))
		{
			return "PRE";
		}
		else {
			return "";
		}
		
	}

	private static JSONObject getSIErrorResponse(JSONObject resJson) {
		
		JSONObject errorMessage=new JSONObject();
		
		errorMessage.put("errorMessage", "This service is not supported. Kindly contact our operations team for support.");
		 
		resJson.put(JSON_PROP_RESBODY, errorMessage);
        
		return resJson;
		
	}

	protected static void createTPA_Extensions(JSONObject reqBodyJson, Element travelerInfoElem) {
		Element tpaExtsElem = XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:TPA_Extensions");
		Element tripTypeElem = XMLUtils.getFirstElementAtXPath(tpaExtsElem, "./air:TripType");
		tripTypeElem.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPTYPE));
		Element tripIndElem = XMLUtils.getFirstElementAtXPath(tpaExtsElem, "./air:TripIndicator");
		tripIndElem.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPIND));
	}

	protected static void createOriginDestinationOptions(Document ownerDoc, JSONObject pricedItinJson,
			Element odosElem) {
		JSONObject airItinJson = pricedItinJson.getJSONObject(JSON_PROP_AIRITINERARY);
		JSONArray odoJsonArr = airItinJson.getJSONArray("originDestinationOptions");
		for (int i=0; i < odoJsonArr.length(); i++) {
			JSONObject odoJson = odoJsonArr.getJSONObject(i);
			Element odoElem = ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOption");
			JSONArray flSegJsonArr = odoJson.getJSONArray(JSON_PROP_FLIGHTSEG);
			for (int j=0; j < flSegJsonArr.length(); j++) {
				JSONObject flSegJson = flSegJsonArr.getJSONObject(j);
				Element flSegElem = ownerDoc.createElementNS(NS_OTA, "ota:FlightSegment");

				Element depAirportElem = ownerDoc.createElementNS(NS_OTA, "ota:DepartureAirport");
				depAirportElem.setAttribute("LocationCode", flSegJson.getString("originLocation"));
				flSegElem.appendChild(depAirportElem);

				Element arrAirportElem = ownerDoc.createElementNS(NS_OTA, "ota:ArrivalAirport");
				arrAirportElem.setAttribute("LocationCode", flSegJson.getString("destinationLocation"));
				flSegElem.appendChild(arrAirportElem);

				Element opAirlineElem = ownerDoc.createElementNS(NS_OTA,"ota:OperatingAirline");
				JSONObject opAirlineJson = flSegJson.getJSONObject("operatingAirline");
				flSegElem.setAttribute("FlightNumber", opAirlineJson.getString(JSON_PROP_FLIGHTNBR));
				flSegElem.setAttribute("DepartureDateTime", flSegJson.getString("departureDate"));
				flSegElem.setAttribute("ArrivalDateTime", flSegJson.getString("arrivalDate"));
				opAirlineElem.setAttribute("Code", opAirlineJson.getString("airlineCode"));
				opAirlineElem.setAttribute("FlightNumber", opAirlineJson.getString(JSON_PROP_FLIGHTNBR));
				String companyShortName = opAirlineJson.optString("companyShortName", "");
				if (companyShortName.isEmpty() == false) {
					opAirlineElem.setAttribute("CompanyShortName", companyShortName);
				}
				flSegElem.appendChild(opAirlineElem);

				Element tpaExtsElem = ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
				Element extRPHElem = ownerDoc.createElementNS(NS_AIR, "air:ExtendedRPH");
				extRPHElem.setTextContent(flSegJson.getString("extendedRPH"));
				tpaExtsElem.appendChild(extRPHElem);

				Element quoteElem = ownerDoc.createElementNS(NS_AIR, "air:QuoteID");
				quoteElem.setTextContent(flSegJson.getString("quoteID"));
				tpaExtsElem.appendChild(quoteElem);

				flSegElem.appendChild(tpaExtsElem);

				Element mkAirlineElem = ownerDoc.createElementNS(NS_OTA, "ota:MarketingAirline");
				JSONObject mkAirlineJson = flSegJson.getJSONObject("marketingAirline");
				mkAirlineElem.setAttribute("Code", mkAirlineJson.getString("airlineCode"));
				String mkAirlineFlNbr = mkAirlineJson.optString(JSON_PROP_FLIGHTNBR, "");
				if (mkAirlineFlNbr.isEmpty() == false) {
					mkAirlineElem.setAttribute("FlightNumber", mkAirlineFlNbr);
				}
				String mkAirlineShortName = mkAirlineJson.optString("companyShortName", "");
				if (mkAirlineShortName.isEmpty() == false) {
					mkAirlineElem.setAttribute("CompanyShortName", mkAirlineShortName);
				}
				flSegElem.appendChild(mkAirlineElem);

				Element bookClsAvailsElem = ownerDoc.createElementNS(NS_OTA, "BookingClassAvails");
				bookClsAvailsElem.setAttribute("CabinType", flSegJson.getString("CabinType"));
				Element bookClsAvailElem = ownerDoc.createElementNS(NS_OTA, "ota:BookingClassAvail");
				bookClsAvailElem.setAttribute("ResBookDesigCode", flSegJson.getString("ResBookDesigCode"));
				bookClsAvailElem.setAttribute("RPH", flSegJson.getString("RPH"));
				bookClsAvailsElem.appendChild(bookClsAvailElem);
				flSegElem.appendChild(bookClsAvailsElem);

				odoElem.appendChild(flSegElem);
			}

			odosElem.appendChild(odoElem);
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
				JSONArray retrieveReqsJSONArr = reqBodyJson.getJSONArray(JSON_PROP_CANCELREQUESTS);
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
				
				System.out.println("reqElem->"+XMLTransformer.toString(reqElem));
				
				Element resElem = null;
	            resElem = HTTPServiceConsumer.consumeXMLService("SI/Cancel", opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
	            if (resElem == null) {
	            	throw new Exception("Null response received from SI");
	            }
	            
	            System.out.println("resElem->"+XMLTransformer.toString(resElem));
	            return resElem;
	          
	        	
	        }
	        catch(Exception E) {
	        	return null;
	        }
	        
	        
	        
			
	}
	
	
	
}
