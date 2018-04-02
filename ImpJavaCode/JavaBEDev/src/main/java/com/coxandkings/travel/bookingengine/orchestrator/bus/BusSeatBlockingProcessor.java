package com.coxandkings.travel.bookingengine.orchestrator.bus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.bus.BusConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;

import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class BusSeatBlockingProcessor implements BusConstants{

	private static final Logger logger = LogManager.getLogger(BusSeatBlockingProcessor.class);
	public static String process(JSONObject reqJson) {
		
		OperationConfig opConfig = BusConfig.getOperationConfig("SeatBlocking");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./busi:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./bus:OTA_BusBlockTicketRQWrapper");
		reqBodyElem.removeChild(wrapperElem);
		
		try
		{
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
			
			//------------ redis--------------
			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			String redisKey = sessionID.concat("|").concat(PRODUCT_BUS).concat("|").concat("seatmap");
			Map<String, String> reprcSuppFaresMap = redisConn.hgetAll(redisKey);
			if (reprcSuppFaresMap == null) {
				logger.error("seatmap not found in redis");
				throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
			}
		
			
			JSONArray busserviceJSONArr = reqBodyJson.getJSONArray(JSON_PROP_SERVICE);
			for (int y=0; y < busserviceJSONArr.length(); y++) {
				
				JSONObject busServiceJson = busserviceJSONArr.getJSONObject(y);
				String suppID = busServiceJson.getString(JSON_PROP_SUPPREF);
				
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				reqBodyElem.appendChild(suppWrapperElem);
				

				
				
		        
		  XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:SupplierID", busServiceJson.getString("supplierRef"));
		  XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:Sequence", String.valueOf(y));
		  Element otaBlockTkt = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_BusBlockTicketRQ");
		  Element tktinfo = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "TicketInfo");
		  otaBlockTkt.appendChild(tktinfo);
		  
		 
		  
//		  if(busServiceJson.get("RouteScheduleId").toString().isEmpty()==false)
//		  {
//			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "RouteScheduleId");
//			  newElem.setTextContent(busServiceJson.get("RouteScheduleId").toString());
//			  tktinfo.appendChild(newElem);
//		  }
		  if(busServiceJson.get(JSON_PROP_JOURNEYDATE).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "JourneyDate");
			  newElem.setTextContent(busServiceJson.get(JSON_PROP_JOURNEYDATE).toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_SERVICEID).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "serviceId");
			  newElem.setTextContent(busServiceJson.get("serviceId").toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_LAYOUTID).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "layoutId");
			  newElem.setTextContent(busServiceJson.get(JSON_PROP_LAYOUTID).toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_SOURCE).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "sourceStationId");
			  newElem.setTextContent(busServiceJson.get("sourceStationId").toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_DESTINATION).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "destinationStationId");
			  newElem.setTextContent(busServiceJson.get("destinationStationId").toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get("boardingPointID").toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "boardingPointID");
			  newElem.setTextContent(busServiceJson.get("boardingPointID").toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get("droppingPointID").toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "droppingPointID");
			  newElem.setTextContent(busServiceJson.get("droppingPointID").toString());
			  tktinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_OPERATORID).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "OperatorID");
			  newElem.setTextContent(busServiceJson.get(JSON_PROP_OPERATORID).toString());
			  tktinfo.appendChild(newElem);
		  }
		  
//		  if(busServiceJson.opt("PickUpID").toString().isEmpty()==false)
//		  {
//			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "PickUpID");
//			  newElem.setTextContent(busServiceJson.get("PickUpID").toString());
//			  tktinfo.appendChild(newElem);
//		  }
//		  
//
//		  if(busServiceJson.get("address").toString().isEmpty()==false)
//		  {
//			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "address");
//			  newElem.setTextContent(busServiceJson.get("address").toString());
//			  tktinfo.appendChild(newElem);
//		  }
//		  if(busServiceJson.get("ladiesSeat").toString().isEmpty()==false)
//		  {
//			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "ladiesSeat");
//			  newElem.setTextContent(busServiceJson.get("ladiesSeat").toString());
//			  tktinfo.appendChild(newElem);
//		  }	  
//		  
		  Element contactinfo = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "ContactInformation");
		  otaBlockTkt.appendChild(contactinfo);
		  
		  if(busServiceJson.get(JSON_PROP_FIRSTNAME).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "CustomerName");
			  newElem.setTextContent(busServiceJson.get(JSON_PROP_FIRSTNAME).toString());
			  contactinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_EMAIL).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Email");
			  newElem.setTextContent(busServiceJson.get(JSON_PROP_EMAIL).toString());
			  contactinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get("phone").toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Phone");
			  newElem.setTextContent(busServiceJson.get("phone").toString());
			  contactinfo.appendChild(newElem);
		  }
		  if(busServiceJson.get(JSON_PROP_MOBILENBR).toString().isEmpty()==false)
		  {
			  Element newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Mobile");
			  newElem.setTextContent(busServiceJson.get(JSON_PROP_MOBILENBR).toString());
			  contactinfo.appendChild(newElem);
		  }
		  
		  getPassangers(ownerDoc, busServiceJson, otaBlockTkt,sessionID);
		  
		
		}
//		  System.out.println(XMLTransformer.toString(reqElem));
		  Element resElem = null;
          resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), BusConfig.getHttpHeaders(), reqElem);
          if (resElem == null) {
          	throw new Exception("Null response received from SI");
          }
//			System.out.println(XMLTransformer.toString(resElem));
			
			JSONObject resBodyJson = new JSONObject();
	        JSONObject blockTktJson = new JSONObject();
			
	        
//			Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./busi:ResponseBody/bus:OTA_BusBlockTicketRSWrapper");
			
	        Element[] wrapperElems=SeatMapProcessor.sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem,
					"./busi:ResponseBody/bus:OTA_BusBlockTicketRSWrapper"));
	        for (Element wrapperElement : wrapperElems) {
	        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ota:OTA_BusBlockTicketRS");
	        	getBlockJSON(resBodyElem, blockTktJson);
	        }
	        resBodyJson.put("blockTicket", blockTktJson);
	        
	        JSONObject resJson = new JSONObject();
	        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
	        resJson.put(JSON_PROP_RESBODY, resBodyJson);
//	        System.out.println(resJson.toString());
	        
	        pushBlockIdToRedis(reqJson,resJson,sessionID);
	        return resJson.toString();
		}
		catch(Exception e)
		{
			logger.error("exception in BusSeatBlocking|Process method");
			e.printStackTrace();
			return null;
		}
		
	}

	public static void getPassangers(Document ownerDoc, JSONObject busServiceJson, Element otaBlockTkt,String sessionID) {
		Element passangers = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Passengers");
		  otaBlockTkt.appendChild(passangers);
		  
		  JSONArray passArr = new JSONArray();
		  passArr = busServiceJson.getJSONArray("passangers");
		  Element newElem= null;
		  for(int k=0; k<passArr.length(); k++)
		  {
			  JSONObject passJson = passArr.getJSONObject(k);
			  
			  Element passElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Passenger");
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Name");
			  newElem.setTextContent(passJson.opt(JSON_PROP_FIRSTNAME).toString());
			  passElem.appendChild(newElem);
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Title");
			  newElem.setTextContent(passJson.opt(JSON_PROP_TITLE).toString());
			  passElem.appendChild(newElem);
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Age");
			  newElem.setTextContent(passJson.opt("age").toString());
			  passElem.appendChild(newElem);
			  
			  
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Gender");
			  newElem.setTextContent(passJson.opt("gender").toString());
			  passElem.appendChild(newElem);
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "SeatNo");
			  newElem.setTextContent(passJson.get(JSON_PROP_SEATNO).toString());
			  passElem.appendChild(newElem);
			  
//			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "ota:Fare");
//			  newElem.setTextContent("800");
//			  passElem.appendChild(newElem);
			  
			  try
			  {
			  Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
				String redisKey = sessionID.concat("|").concat(PRODUCT_BUS).concat("|").concat("seatmap");
				Map<String, String> reprcSuppFaresMap = redisConn.hgetAll(redisKey);
				if (reprcSuppFaresMap == null) {
					throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
				}
				String mapKey = SeatMapProcessor.getRedisKeyForSeatFare(busServiceJson,passJson);
				JSONObject mapJson = new JSONObject(reprcSuppFaresMap.get(mapKey));
				 newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "ota:Fare");
				  newElem.setTextContent(mapJson.getBigDecimal("fare").toString());
				  passElem.appendChild(newElem);
				
			  }
			  catch(Exception e)
			  {
				  e.printStackTrace();
			  }
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "seatTypesList");
			  newElem.setTextContent(passJson.get("seatTypesList").toString());
			  passElem.appendChild(newElem);
			  
			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "seatTypeIds");
			  newElem.setTextContent(passJson.get("seatTypeIds").toString());
			  passElem.appendChild(newElem);
			  
//			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "IsPrimary");
//			  newElem.setTextContent(passJson.opt("IsPrimary").toString());
//			  passElem.appendChild(newElem);
//			  
//			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "IdNumber");
//			  newElem.setTextContent(passJson.opt("IdNumber").toString());
//			  passElem.appendChild(newElem);
//			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "IdType");
//			  newElem.setTextContent(passJson.opt("IdType").toString());
//			  passElem.appendChild(newElem);
			 
			  
			  
			  


			 
				
			  
			  //temporarily fare is hardcoded
			 
			  
//			  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "isAcSeat");
//			  newElem.setTextContent(passJson.get("isAcSeat").toString());
//			  passElem.appendChild(newElem);
//			  
			  passangers.appendChild(passElem);
			  
			  
		  }
	}

	private static void pushBlockIdToRedis(JSONObject reqJson,JSONObject resJson,String sessionId) {
		
		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		
		JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
		

		
		
		try
		{
			
			

			
//			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
//			String redisKey = sessionId.concat("|").concat(PRODUCT_BUS).concat("|").concat("blockId");
			
			Map<String,String> detailsMap= new  HashMap<String, String>();
//			Map<String,String> suppCommMap= new  HashMap<String, String>();
//			Map<String,String> clientCommMap= new  HashMap<String, String>();

		    JSONArray serviceArr = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_SERVICE);
		    for(int i=0;i<serviceArr.length();i++)
		    {
		    	
		    	 JSONObject infoJson = new JSONObject();
		    	 JSONObject serviceJson = serviceArr.getJSONObject(i);
		    	 if((resJson.getJSONObject("responseBody").getJSONObject("blockTicket").optString("blockingId"))!=null)
		    	 {
		    		 serviceJson.put("holdKey", resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("blockTicket").optString("blockingId"));
		    	 }
		    	 else
		    	 {
		    		 serviceJson.put("holdKey","");
		    	 }
		    	 String mapKey = getMapKey(serviceJson);
		    	 detailsMap.put(mapKey, serviceJson.toString());
		    	 
	    	 
		    }
//			redisConn.set(redisKey, resBodyJson.getJSONObject("blockTicket").getString("BlockingId"));
		    Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		    String redisKey = sessionId.concat("|").concat(PRODUCT_BUS).concat("|").concat("blockId");
		    
			redisConn.hmset(redisKey, detailsMap);
			redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
			
//			 redisConn = RedisConfig.getRedisConnectionFromPool();
			 redisKey = sessionId.concat("|").concat(PRODUCT_BUS).concat("|").concat("suppComm");
			 Map<String, String> suppCommMap = redisConn.hgetAll(redisKey);
				if (suppCommMap == null) {
					logger.error("suppComm not found in redis");
					throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
				}
				redisConn.hmset(redisKey, suppCommMap);
				redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
				
			redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS).concat("|").concat("clientComm");	
			Map<String, String> clientCommMap = redisConn.hgetAll(redisKey);
			if (clientCommMap == null) {
				logger.error("clientComm not found in redis");
				throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
			}
			redisConn.hmset(redisKey, clientCommMap);
			redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
			

			 
			
		}
		catch(Exception e)
		{
			logger.error("exception in BusSeatBlocking|pushToRedis method ");;
			e.printStackTrace();
		}
	}

	public static String getMapKey(JSONObject serviceJson) {

        StringBuilder mapKey = new StringBuilder();
        mapKey.append(serviceJson.get(JSON_PROP_SUPPREF));
   	    mapKey.append("|");
//   	    mapKey.append(serviceJson.opt("RouteScheduleId"));
//   	    mapKey.append("|");
   	    mapKey.append(serviceJson.get(JSON_PROP_JOURNEYDATE));
		return mapKey.toString();
	}

	private static void getBlockJSON(Element resBodyElem, JSONObject blockTktJson) {
		
		blockTktJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./bus:SupplierID"));
		blockTktJson.put("status", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Status"));
		blockTktJson.put("blockingId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:BlockingId"));
	}

}
