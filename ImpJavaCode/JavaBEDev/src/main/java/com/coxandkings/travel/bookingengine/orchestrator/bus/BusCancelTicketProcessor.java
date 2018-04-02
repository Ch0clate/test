package com.coxandkings.travel.bookingengine.orchestrator.bus;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.OperationsShellConfig;
import com.coxandkings.travel.bookingengine.config.bus.BusConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class BusCancelTicketProcessor implements BusConstants{

	private static final Logger logger = LogManager.getLogger(BusCancelTicketProcessor.class);
	public static String process(JSONObject reqJson) 
	{
		try
		{
			KafkaBookProducer cancelProducer = new KafkaBookProducer();
			
			OperationConfig opConfig = BusConfig.getOperationConfig("CancelBooking");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true); // xmshell
			Document ownerDoc = reqElem.getOwnerDocument();
			
			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./busi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./bus:OTA_BusCancelTicketRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			
            TrackingContext.setTrackingContext(reqJson);
			
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct("Transportation","Bus");

			
			BusSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
			

			
			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./bus:RequestHeader/com:SupplierCredentialsList");
			for (ProductSupplier prodSupplier : prodSuppliers) {
				suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
			}
			
			JSONArray busserviceJSONArr = reqBodyJson.getJSONArray(JSON_PROP_SERVICE);
			
			for(int i=0;i<busserviceJSONArr.length();i++)
			{
				JSONObject busServiceJson = busserviceJSONArr.getJSONObject(i);
				String suppID = busServiceJson.getString(JSON_PROP_SUPPREF);
				
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				reqBodyElem.appendChild(suppWrapperElem);
				
				
		        
		        XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:SupplierID", busServiceJson.getString("supplierRef"));
		        XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:Sequence", String.valueOf(i));
		        
		        Element otacancelTkt = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_BusCancelTicketRQ");
		        Element newElem;
		        
		        if(busServiceJson.get("operatorId").toString().isEmpty()==false)
				  {
					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "operatorId");
					  newElem.setTextContent(busServiceJson.get("operatorId").toString());
					  otacancelTkt.appendChild(newElem);
				  }
		        
		        if(busServiceJson.get("phone").toString().isEmpty()==false)
				  {
					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "phoneNum");
				
					  newElem.setTextContent(busServiceJson.get("phone").toString());
					  otacancelTkt.appendChild(newElem);
				  }
		        if(busServiceJson.get("ticketNo").toString().isEmpty()==false)
				  {
					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "ticketNo");
					  newElem.setTextContent(busServiceJson.get("ticketNo").toString());
					  otacancelTkt.appendChild(newElem);
				  }
		        if(busServiceJson.get("partialCancellation").toString().isEmpty()==false)
				  {
					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "partialCancellation");
					  newElem.setTextContent(busServiceJson.get("partialCancellation").toString());
					  otacancelTkt.appendChild(newElem);
				  }
//		        if(busServiceJson.get("cancelSeats").toString().isEmpty()==false)
//				  {
//					   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "cancelSeats");
//					  newElem.setTextContent(busServiceJson.get("cancelSeats").toString());
//					  otacancelTkt.appendChild(newElem);
//				  }
		        
		        JSONArray seatToCancelArr = busServiceJson.getJSONArray("seatsToCancel");
		        StringBuilder entityIds = new StringBuilder();
		        
		        for(int k=0;k<seatToCancelArr.length();k++)
		        {
		        	JSONObject seatCancelJson = seatToCancelArr.getJSONObject(k);
		        	if(seatCancelJson.get("seatNo").toString().isEmpty()==false)
					  {
						  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "seatToCancel");
						  newElem.setTextContent(seatCancelJson.get("seatNo").toString());
						  otacancelTkt.appendChild(newElem);
					  }
		        	entityIds.append(seatCancelJson.getString("paxId"));
		        	entityIds.append("|");
		        }
		        String temp = entityIds.toString();
//		        reqBodyJson.put("entityId", StringUtils.collectionToDelimitedString(entityIdList, "|"));
		        reqBodyJson.put("entityId", temp.substring(0, temp.length()-1));
//		        if(busServiceJson.get("PNRNo").toString().isEmpty()==false)
//				  {
//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "PNRNo");
//					  newElem.setTextContent(busServiceJson.get("PNRNo").toString());
//					  otacancelTkt.appendChild(newElem);
//				  }
			}
			
			System.out.println(XMLTransformer.toString(reqElem));
			reqJson.put(JSON_PROP_REQBODY, reqBodyJson);
			JSONObject kafkaMsgJson = new JSONObject(reqJson.toString());
			System.out.println("KAfkaRQMsg->"+kafkaMsgJson);
			cancelProducer.runProducer(1, kafkaMsgJson);
			
			
			  Element resElem = null;
	          resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), BusConfig.getHttpHeaders(), reqElem);
	          if (resElem == null) {
	          	throw new Exception("Null response received from SI");
	          }
			System.out.println(XMLTransformer.toString(resElem));
	          
	        JSONObject resBodyJson = new JSONObject();
		    JSONObject getCancelTktJson = new JSONObject();
		    
//		    Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./busi:ResponseBody/bus:OTA_BusCancelTicketRSWrapper");
	        Element[] resWrapperElems=SeatMapProcessor.sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem, "./busi:ResponseBody/bus:OTA_BusCancelTicketRSWrapper"));

		    for (Element wrapperElement : resWrapperElems) {
	        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ota:OTA_BusCancelTicketRS");
	        	getCancelTktJson(resBodyElem, getCancelTktJson);
	        }
	        resBodyJson.put("service", getCancelTktJson);
	        JSONObject resJson = new JSONObject();
	        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
	        resJson.put(JSON_PROP_RESBODY, resBodyJson);
	        System.out.println(resJson.toString());
	        
	        kafkaMsgJson = new JSONObject(resJson.toString());
	    
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("product", reqBodyJson.get("product"));
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("type", reqBodyJson.get("type"));
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("requestType", reqBodyJson.get("requestType"));
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("cancelType", reqBodyJson.get("cancelType"));
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("entityName", reqBodyJson.get("entityName"));
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("entityId", reqBodyJson.get("entityId"));
	        System.out.println("KAfkaRQMsg->"+kafkaMsgJson);
			cancelProducer.runProducer(1, kafkaMsgJson);
	        
	        int index=0;
            for (Element resWrapperElem : resWrapperElems) {
            
            	if(XMLUtils.getFirstElementAtXPath(resWrapperElem, "./bus:ErrorList")!=null)
            	{
            		JSONObject cancelReq=reqBodyJson.getJSONArray(JSON_PROP_SERVICE).getJSONObject(index);
            		Element errorMessage=XMLUtils.getFirstElementAtXPath(resWrapperElem, "./bus:Errors/com:Error");
            		
            		String errMsgStr=XMLUtils.getValueAtXPath(errorMessage, "./@ShortText");
            		if(CANCEL_SI_ERR.equalsIgnoreCase(errMsgStr))
            		{	
            			logger.error("This service is not supported. Kindly contact our operations team for support.");
            			callOperationTodo(resJson,cancelReq);
            			return getSIErrorResponse(resJson).toString();
            		
            			
            		}
            	}
            }
	        
			return resJson.toString();

		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		
	}

	private static JSONObject callOperationTodo(JSONObject resJson, JSONObject cancelReq) {
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
				operationMessageJson.put("taskSubTypeId", getSubTypeIdType(cancelReq.getString(JSON_PROP_CANCELTYPE)));
				//TODO:Determine Value
				operationMessageJson.put("taskTypeId", "MAIN");
				//TODO:Determing value
				operationMessageJson.put("dueOn", "2");
				
				operationMessageJson.put("productId", cancelReq.get("bookID").toString());
				//TODO:Get from db
				operationMessageJson.put("referenceId", cancelReq.get("referenceID").toString());
			
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
		else
		return "";
	}

	private static Object getSIErrorResponse(JSONObject resJson) {
		
		JSONObject errorMessage=new JSONObject();
		
		errorMessage.put("errorMessage", "This service is not supported. Kindly contact our operations team for support.");
		 
		resJson.put(JSON_PROP_RESBODY, errorMessage);
        
		return resJson;
	}

	private static void getCancelTktJson(Element resBodyElem, JSONObject getCancelTktJson) {

          getCancelTktJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./bus:SupplierID"));
          getCancelTktJson.put(JSON_PROP_STATUS, XMLUtils.getValueAtXPath(resBodyElem, "./ota:Status"));
          getCancelTktJson.put("message", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Message"));
          getCancelTktJson.put(JSON_PROP_TOTALFARE, Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Status"), 0));
          getCancelTktJson.put("refundAmount", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(resBodyElem, "./ota:RefundAmount"), 0));
          getCancelTktJson.put("chargePct", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(resBodyElem, "./ota:ChargePct"), 0));
          getCancelTktJson.put("cancellationCharge", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(resBodyElem, "./ota:cancellationCharge"), 0));
          getCancelTktJson.put(JSON_PROP_CURRENCY, XMLUtils.getValueAtXPath(resBodyElem, "./ota:Currency"));

	}
}
