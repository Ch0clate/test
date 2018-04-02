package com.coxandkings.travel.bookingengine.orchestrator.bus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;

import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;

import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;


import redis.clients.jedis.Jedis;

public class BusBookProcessor implements BusConstants{

	private static final Logger logger = LogManager.getLogger(BusBookProcessor.class);
	private static String mRcvsPriceCompQualifier = JSON_PROP_RECEIVABLES.concat(SIGMA).concat(".").concat(JSON_PROP_RECEIVABLE).concat(".");
	public static String process(JSONObject reqJson) {
		
		try
		{

		  
		  
		KafkaBookProducer bookProducer = new KafkaBookProducer();
		OperationConfig opConfig = BusConfig.getOperationConfig("CreateBooking");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./busi:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./bus:OTA_BusBookTicketRQWrapper");
		reqBodyElem.removeChild(wrapperElem);
		
		
           
			TrackingContext.setTrackingContext(reqJson);
            JSONObject kafkaMsgJson = new JSONObject(reqJson.toString());
            String bookId = kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).getString("bookId");
            JSONArray paymentInfoArr = kafkaMsgJson.getJSONObject("requestBody").getJSONArray("paymentInfo");
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
//			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);
			
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_TRANSPORT,PROD_CATEG_SUBTYPE_BUS);

			
			BusSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
			
			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			String redisKey = sessionID.concat("|").concat(PRODUCT_BUS).concat("|").concat("blockId");
			Map<String, String> detailsMap = redisConn.hgetAll(redisKey); // map gives passanger information and blockid 
			if (detailsMap == null) {
				throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
			}
			
			
			JSONObject reqBodyJson = new JSONObject();
			
			JSONArray kafkaServiceArr = new JSONArray();
			
			JSONArray service = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_SERVICE);
			for(int z=0;z<service.length();z++)
			{
				JSONObject serviceJSon =service.getJSONObject(z);
				if((detailsMap.get(BusSeatBlockingProcessor.getMapKey(serviceJSon)))!=null)
				{
					reqBodyJson = new JSONObject(detailsMap.get(BusSeatBlockingProcessor.getMapKey(serviceJSon)));
	//				kafkaMsgJson.put("requestBody",reqBodyJson);
					
	//			JSONObject reqBodyJson = reqJsonblock.getJSONObject(JSON_PROP_REQBODY);
				
	//			JSONArray busserviceJSONArr = reqBodyJson.getJSONArray(JSON_PROP_SERVICE);
				
	//			for (int y=0; y < busserviceJSONArr.length(); y++) {
					
					JSONObject busServiceJson = reqBodyJson;
					
//					String suppID = busServiceJson.getString(JSON_PROP_SUPPREF);
					
					Element suppWrapperElem = null;
					suppWrapperElem = (Element) wrapperElem.cloneNode(true);
					reqBodyElem.appendChild(suppWrapperElem);
	
					
					
					Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
							"./bus:RequestHeader/com:SupplierCredentialsList");
					for (ProductSupplier prodSupplier : prodSuppliers) {
						suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
					}
			        
			        XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:SupplierID", busServiceJson.getString(JSON_PROP_SUPPREF));
			        XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:Sequence", String.valueOf(z));
			        
			        Element otaBookTkt = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_BusBookTicketRQ");
			        Element newElem;
			        
//			        if(busServiceJson.get("HoldKey").toString().isEmpty()==false)
//					  {
						  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "HoldKey");
	//					  newElem.setTextContent(busServiceJson.get("HoldKey").toString());
	//					  newElem.setTextContent(redisConn.get(redisKey).toString());
						  
						  
						  //uncomment
						  JSONObject detail = new JSONObject(detailsMap.get(BusSeatBlockingProcessor.getMapKey(busServiceJson)));
						  newElem.setTextContent(detail.get("holdKey").toString());
//						  newElem.setTextContent("O_22293819");
						  otaBookTkt.appendChild(newElem);
//					  }
			        
			        
					
					if(busServiceJson.get(JSON_PROP_JOURNEYDATE).toString().isEmpty()==false)
					  {
						   newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "JourneyDate");
						  newElem.setTextContent(busServiceJson.get(JSON_PROP_JOURNEYDATE).toString());
						  otaBookTkt.appendChild(newElem);
					  }
				    if(busServiceJson.get(JSON_PROP_OPERATORID).toString().isEmpty()==false)
					  {
						  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "operatorId");
						  newElem.setTextContent(busServiceJson.get(JSON_PROP_OPERATORID).toString());
						  otaBookTkt.appendChild(newElem);
					  }
				    //additinal
	//			    if(busServiceJson.get("RouteScheduleId").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "RouteScheduleId");
	//					  newElem.setTextContent(busServiceJson.get("RouteScheduleId").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("sourceStationId").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "sourceStationId");
	//					  newElem.setTextContent(busServiceJson.get("sourceStationId").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("destinationStationId").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "destinationStationId");
	//					  newElem.setTextContent(busServiceJson.get("destinationStationId").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("serviceId").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "serviceId");
	//					  newElem.setTextContent(busServiceJson.get("serviceId").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("layoutId").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "layoutId");
	//					  newElem.setTextContent(busServiceJson.get("layoutId").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("address").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "address");
	//					  newElem.setTextContent(busServiceJson.get("address").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("ladiesSeat").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "ladiesSeat");
	//					  newElem.setTextContent(busServiceJson.get("ladiesSeat").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("boardingPointID").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "boardingPointID");
	//					  newElem.setTextContent(busServiceJson.get("boardingPointID").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("droppingPointID").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "droppingPointID");
	//					  newElem.setTextContent(busServiceJson.get("droppingPointID").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("PickUpID").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "PickUpID");
	//					  newElem.setTextContent(busServiceJson.get("PickUpID").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("CustomerName").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "CustomerName");
	//					  newElem.setTextContent(busServiceJson.get("CustomerName").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("Email").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Email");
	//					  newElem.setTextContent(busServiceJson.get("Email").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("Phone").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Phone");
	//					  newElem.setTextContent(busServiceJson.get("Phone").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
	//			    if(busServiceJson.get("Mobile").toString().isEmpty()==false)
	//				  {
	//					  newElem = ownerDoc.createElementNS("http://www.opentravel.org/OTA/2003/05", "Mobile");
	//					  newElem.setTextContent(busServiceJson.get("Mobile").toString());
	//					  otaBookTkt.appendChild(newElem);
	//				  }
				  
				   JSONArray passArr = busServiceJson.getJSONArray("passangers");
				   JSONArray paxDetailsArr = new JSONArray();
				   
				   getPassengerDetails(busServiceJson, passArr, paxDetailsArr);
				   
				   
				    String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
			    	String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
				   
				    String redisKeySuppComm = reqHdrJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS).concat("|").concat("suppComm");
			        Map<String, String> suppCommMap = redisConn.hgetAll(redisKeySuppComm); //map gives supplier commercials per seat
					if (detailsMap == null) {
						logger.error("SuppComm not found in redis | busbookProcessor");
						throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
					}
					
					String redisKeyClienComm = reqHdrJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS).concat("|").concat("clientComm");	
	
					Map<String, String> clientCommMap = redisConn.hgetAll(redisKeyClienComm); //map gives client commercials per seat
						if (clientCommMap == null) {
							logger.error("clientComm not found in redis | busbookProcessor");
							throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
						}
						JSONArray orderlevelCommArr = new JSONArray();
//						JSONArray orderlevelclientCommArr =  new JSONArray();
//						BigDecimal commercialAmount = new BigDecimal(0);
//						String commercialName;
//				    	String commercialType;
//				    	String commercialCurrency;
				    	
				    	BigDecimal clientcommercialAmount = new BigDecimal(0);
//						String clientcommercialName;
//				    	String clientcommercialType;
//				    	String clientcommercialCurrency;
				    	BigDecimal totalFare = new BigDecimal(0);
//				    	BigDecimal supplierTotalfare = new BigDecimal(0);
				    	BigDecimal calcSuppTotalFare = new BigDecimal(0);
				    	String SupplierTotalFareCurrency = null;
//				    	String totalFareCurrency = null;
				    JSONObject orderLevelJson = new JSONObject();
				    JSONObject orderLevelcommercialJson = new JSONObject();
				    JSONArray clientCommTotalArr = new JSONArray();
//				    JSONArray supplierCommTotalArr = new JSONArray();
				    JSONObject busTotalPriceInfoJson = new JSONObject();
				    JSONObject suppInfoJson = new JSONObject();
				    JSONObject supplierPricingInfoJson = new JSONObject();
				    JSONObject totalFareJson = new JSONObject();
				    JSONObject supplierFareJson = new JSONObject();
				    JSONObject supplierTotalFareJson = new JSONObject();
//				    JSONArray  receivablesArr = new JSONArray();
				    JSONArray paxClientFaresArr = new JSONArray();
				    JSONArray paxSupplierFaresArr = new JSONArray();
//				    JSONObject suppinfoTotalFareJson = new JSONObject();
				    
		    		PriceComponentsGroup totalFareCompsGroup = new PriceComponentsGroup(JSON_PROP_BUSSERVICETOTALFARE, clientCcyCode, new BigDecimal(0), true);
		    		
				    Map<String,BigDecimal> suppliercommTypeAmountMap = new HashMap<String,BigDecimal>();
			    	Map<String,BigDecimal> clientcommTypeAmountMap = new HashMap<String,BigDecimal>();
			    	
			    	redisKey = sessionID.concat("|").concat(PRODUCT_BUS).concat("|").concat("seatmap");
					Map<String, String> reprcSuppFaresMap = redisConn.hgetAll(redisKey);
					if (reprcSuppFaresMap == null) {
						logger.error("seatmap not found in redis | busbookProcessor ");
						throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
					}
					
					JSONObject clienInfo = usrCtx.toJSON();
				    JSONArray clientEntityDetailsArr = clienInfo.getJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS);
				    JSONObject clientEntityDetailsJson=new JSONObject();
				    for(int x = 0;x<clientEntityDetailsArr.length();x++)
				    {
				    	clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(x);
				    }
					
				    for(int i=0;i<passArr.length();i++)
				    {
				    	JSONObject passJson = passArr.getJSONObject(i);

				    	JSONObject suppCommJson = new JSONObject(suppCommMap.get(SeatMapProcessor.getRedisKeyForSeatFare(busServiceJson,passJson)));
				    	JSONObject clientCommJson = new JSONObject(clientCommMap.get(SeatMapProcessor.getRedisKeyForSeatFare(busServiceJson,passJson)));
				    	
				    	//TODO: in supplierComm json , passdetails array only one fare should be present because one fare per seat. 
				    	
				    	JSONArray commdetailsArr = suppCommJson.getJSONArray(JSON_PROP_PASSANGERDETAILS).getJSONObject(0).getJSONArray(JSON_PROP_COMMDETAILS);
				    	JSONArray entityCommArr  = clientCommJson.getJSONArray(JSON_PROP_PASSANGERDETAILS).getJSONObject(0).getJSONArray(JSON_PROP_ENTITYCOMMS);
				    	
				    	JSONObject paxClientFareJson =new JSONObject();
				    	JSONObject paxSupplierFareJson =new JSONObject();
				    	JSONArray paxLevelSupplierCommArr = new JSONArray();
				    	
				    	Map<String, String> commToTypeMap = new HashMap<String, String>();
				    	
				    	for(int m=0;m<entityCommArr.length();m++)
				    	{
				    		JSONArray additionArr = entityCommArr.getJSONObject(m).getJSONArray("additionalCommercialDetails");
				    		for(int n=0;n<additionArr.length();n++)
				    		{
				    			JSONObject addJson = additionArr.getJSONObject(n);
				    			commToTypeMap.put(addJson.getString("commercialName"), addJson.getString("commercialType"));
				    		}
				    		JSONArray retensionArr = entityCommArr.getJSONObject(m).getJSONArray("retentionCommercialDetails");
				    		for(int n=0;n<retensionArr.length();n++)
				    		{
				    			JSONObject retenionJson = additionArr.getJSONObject(n);
				    			commToTypeMap.put(retenionJson.getString("commercialName"), retenionJson.getString("commercialType"));
				    		}
				    		JSONArray fixedArr = entityCommArr.getJSONObject(m).getJSONArray("fixedCommercialDetails");
				    		for(int n=0;n<fixedArr.length();n++)
				    		{
				    			JSONObject fixedJson = additionArr.getJSONObject(n);
				    			commToTypeMap.put(fixedJson.getString("commercialName"), fixedJson.getString("commercialType"));
				    		}
				    		JSONObject markUpCommercialDetails = entityCommArr.getJSONObject(m).getJSONObject("markUpCommercialDetails");
				    			commToTypeMap.put(markUpCommercialDetails.getString("commercialName"), markUpCommercialDetails.getString("commercialType"));
				    		
				    	}
				    	
				    		calculateSuppCommercials(orderlevelCommArr, clientcommercialAmount, orderLevelJson,
									suppliercommTypeAmountMap, commdetailsArr, paxLevelSupplierCommArr);
	
				    		BigDecimal paxCount  = new BigDecimal(passArr.length());
				    		JSONArray paxLevelClientCommArr = new JSONArray();
				    		
				    		calculateClientCommercials(clientMarket, clientCcyCode, totalFare, orderLevelcommercialJson,
									clientCommTotalArr, totalFareJson, paxClientFaresArr, totalFareCompsGroup,
									clientcommTypeAmountMap, clientEntityDetailsJson, passJson, entityCommArr,
									paxClientFareJson, paxCount, paxLevelClientCommArr,commToTypeMap);
				    		
	
				    		
				    		
				    		String redisKeyForFare = SeatMapProcessor.getRedisKeyForSeatFare(busServiceJson,passJson);
				    		supplierFareJson = new JSONObject(reprcSuppFaresMap.get(redisKeyForFare));
					    	
					    	calcSuppTotalFare = 	calcSuppTotalFare.add(supplierFareJson.getBigDecimal("fare"));
					    	SupplierTotalFareCurrency = supplierFareJson.getString(JSON_PROP_CURRENCY);
					    	
					    	//suppinfo
					    	
//					    	JSONObject paxSupplierLevelJson = new JSONObject();
//					    	supplierTotalFareJson.put("amount", calcSuppTotalFare);
//					    	supplierTotalFareJson.put("currency", SupplierTotalFareCurrency);
//					    	supplierPricingInfoJson.put("itinTotalFare", supplierTotalFareJson);//supplier itintotal
					    	
					    	JSONObject paxLevelFareJson = new JSONObject();
					    	JSONObject paxLevelTotalFareJson = new JSONObject();
					    	paxLevelFareJson.put(JSON_PROP_AMOUNT, supplierFareJson.getBigDecimal("fare"));
					    	paxLevelFareJson.put(JSON_PROP_CURRENCY, supplierFareJson.getString(JSON_PROP_CURRENCY));
					    	paxLevelTotalFareJson.put(JSON_PROP_AMOUNT, paxLevelFareJson.getBigDecimal(JSON_PROP_AMOUNT));
					    	paxLevelTotalFareJson.put(JSON_PROP_CURRENCY, paxLevelFareJson.getString(JSON_PROP_CURRENCY));
					    	paxLevelTotalFareJson.put(JSON_PROP_BASEFARE, paxLevelFareJson);
					    	
					    	
//					    	paxLevelFareJson.put(JSON_PROP_AMOUNT, supplierFareJson.getBigDecimal("fare"));
//					    	paxLevelFareJson.put(JSON_PROP_CURRENCY, supplierFareJson.getBigDecimal(JSON_PROP_CURRENCY));
//					    	supplierTotalFareJson.put(JSON_PROP_CCYCODE, supplierFareJson.getString(JSON_PROP_CURRENCY));
					    	paxSupplierFareJson.put(JSON_PROP_TOTALFARE, paxLevelTotalFareJson);//total added at pax level
					    	paxSupplierFareJson.put(JSON_PROP_SEATNO, passJson.getString(JSON_PROP_SEATNO));// calculation as per seat
//					    	paxSupplierFareJson.put(JSON_PROP_BASEFARE, paxLevelFareJson);
					    	paxSupplierFareJson.put(JSON_PROP_SUPPCOMM, paxLevelSupplierCommArr);
					    	paxSupplierFaresArr.put(paxSupplierFareJson);
					    	
					    	
					    	
					    	
				    	
				    	
				    }
	//			    orderlevelCommArr.put(orderLevelJson);
				    
				  //suppinfo
				    
			    	supplierTotalFareJson.put(JSON_PROP_AMOUNT, calcSuppTotalFare);
			    	supplierTotalFareJson.put(JSON_PROP_CURRENCY, SupplierTotalFareCurrency);
			    	supplierPricingInfoJson.put(JSON_PROP_BUSSERVICETOTALFARE, supplierTotalFareJson);//supplier itintotal
			    	supplierPricingInfoJson.put(JSON_PROP_SUPPCOMMTOTALS, orderlevelCommArr);
			    	supplierPricingInfoJson.put(JSON_PROP_PAXSEATFARES, paxSupplierFaresArr);
			    	suppInfoJson.put(JSON_PROP_SUPPPRICEINFO, supplierPricingInfoJson);
			    	busServiceJson.put("suppInfo", suppInfoJson);
			    	
				   //bustotalpriceinfo
			    	JSONArray clientEntityTotalCommArr = new JSONArray();
			    	clientEntityDetailsJson.put("clientCommercialsTotal", clientCommTotalArr);
			    	clientEntityTotalCommArr.put(clientEntityDetailsJson);
				    busTotalPriceInfoJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientEntityTotalCommArr);
				    busTotalPriceInfoJson.put(JSON_PROP_BUSSERVICETOTALFARE, totalFareCompsGroup.toJSON());
				    busTotalPriceInfoJson.put(JSON_PROP_PAXSEATFARES, paxClientFaresArr);
				    busServiceJson.put(JSON_PROP_BUSTOTALPRICEINFO, busTotalPriceInfoJson);
				     
				    kafkaServiceArr.put(busServiceJson);
				     
//				    orderlevelclientCommArr.put(orderLevelcommercialJson);
//				    busServiceJson.put("SupplierCommercials", orderlevelCommArr);
//				    busServiceJson.put("ClientCommercials", orderlevelclientCommArr);
//				    busServiceJson.put("SupplierTotalFare", calcSuppTotalFare);
//				    busServiceJson.put("SupplierTotalFareCurrency", SupplierTotalFareCurrency);
//				    busServiceJson.put("TotalFare", totalFare);
//				    busServiceJson.put("TotalFareCurrency", totalFareCurrency);
	
//				    BusSeatBlockingProcessor.getPassangers(ownerDoc, busServiceJson, otaBookTkt,sessionID);
	
			}
		}
			kafkaMsgJson.put(JSON_PROP_REQBODY,new JSONObject());
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_SERVICE, kafkaServiceArr);
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_PAYINFO, paymentInfoArr);
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put("product", "Bus");
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put(JSON_PROP_BOOKID, bookId);//"CRT123AR43"
			System.out.println("KAfkaRQMsg->"+kafkaMsgJson);
			bookProducer.runProducer(1, kafkaMsgJson);
			
//			System.out.println(XMLTransformer.toString(reqElem));
			  Element resElem = null;
	          resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), BusConfig.getHttpHeaders(), reqElem);
	          if (resElem == null) {
	        	  logger.error("Null response received from SI");
	          	throw new Exception("Null response received from SI");
	          }
//			System.out.println(XMLTransformer.toString(resElem));
			
			JSONObject resBodyJson = new JSONObject();
	        JSONArray bookTktJsonArr = new JSONArray();
	        
//	        Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./busi:ResponseBody/bus:OTA_BusBookTicketRSWrapper");
			
	        Element[] wrapperElems=SeatMapProcessor.sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem, "./busi:ResponseBody/bus:OTA_BusBookTicketRSWrapper"));

	        for (Element wrapperElement : wrapperElems) {
	        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ota:OTA_BusBookTicketRS/ota:BookSeatRS");
	        	getBookJSON(resBodyElem, bookTktJsonArr);
	        }
	        resBodyJson.put("bookTicket", bookTktJsonArr);
	        resBodyJson.put("product", "Bus");
	        resBodyJson.put(JSON_PROP_BOOKID, bookId);//"CRT123AR43"
	        JSONObject resJson = new JSONObject();
	        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
	        resJson.put(JSON_PROP_RESBODY, resBodyJson);
	        kafkaMsgJson = new JSONObject(resJson.toString());
	        System.out.println("KAfkaRQMsg->"+kafkaMsgJson);
			bookProducer.runProducer(1, kafkaMsgJson);
			
	        return resJson.toString();
		}
		catch(Exception e)
		{
			logger.error("exception in bookProcessor|process");
			e.printStackTrace();
			return null;
		}
		
		
	}



	private static void getPassengerDetails(JSONObject busServiceJson, JSONArray passArr, JSONArray paxDetailsArr) {
		for(int i=0;i<passArr.length();i++)
		   {
			   JSONObject paxJson = new JSONObject();
			   JSONObject passJson = passArr.getJSONObject(i);
			   JSONObject documentDetails = new JSONObject();
			   JSONArray documentInfoArr = new JSONArray();
			   JSONArray contactDetailsArr = new JSONArray();
			   
			   JSONObject docInfoJson = new JSONObject();
			   docInfoJson.put("IdNumber", passJson.get("IdNumber"));
			   docInfoJson.put("IdType", passJson.get("IdType"));
			   documentInfoArr.put(docInfoJson);
			   documentDetails.put("documentInfo", documentInfoArr);
			   paxJson.put("documentDetails", documentDetails);
			   
			   paxJson.put(JSON_PROP_FIRSTNAME, passJson.get(JSON_PROP_FIRSTNAME));
			   paxJson.put(JSON_PROP_MIDDLENAME, passJson.opt(JSON_PROP_MIDDLENAME));
			   paxJson.put(JSON_PROP_SURNAME, passJson.opt(JSON_PROP_SURNAME));
			   paxJson.put(JSON_PROP_SEATNO, passJson.get(JSON_PROP_SEATNO));
			   paxJson.put(JSON_PROP_TITLE, passJson.get(JSON_PROP_TITLE));
			   paxJson.put(JSON_PROP_GENDER, passJson.get(JSON_PROP_GENDER));
			   paxJson.put(JSON_PROP_DATEOFBIRTH, passJson.get(JSON_PROP_DATEOFBIRTH));
			   paxJson.put("seatTypesList", passJson.get("seatTypesList"));
			   paxJson.put("seatTypeIds", passJson.get("seatTypeIds"));
			   
			   
			   	JSONObject contactJson = new JSONObject();
			   	JSONObject contactInfoJson = new JSONObject();
			   	contactInfoJson.put(JSON_PROP_EMAIL, passJson.get(JSON_PROP_EMAIL));
			   	contactInfoJson.put(JSON_PROP_MOBILENBR, passJson.get(JSON_PROP_MOBILENBR));
			   	contactInfoJson.put("phone", passJson.get("phone"));
			   	contactInfoJson.put(JSON_PROP_FIRSTNAME, passJson.get(JSON_PROP_FIRSTNAME));
			   	contactJson.put("contactInfo", contactInfoJson);
			   	contactDetailsArr.put(contactJson);
			   	paxJson.put("contactDetails", contactDetailsArr);
			   	
			   	paxDetailsArr.put(paxJson);
		   }
		   busServiceJson.remove("passangers");
		   busServiceJson.put("paxDetails", paxDetailsArr);
	}



	private static void calculateClientCommercials(String clientMarket, String clientCcyCode, BigDecimal totalFare,
			JSONObject orderLevelcommercialJson, JSONArray clientCommTotalArr, JSONObject totalFareJson,
			JSONArray paxClientFaresArr, PriceComponentsGroup totalFareCompsGroup,
			Map<String, BigDecimal> clientcommTypeAmountMap, JSONObject clientEntityDetailsJson, JSONObject passJson,
			JSONArray entityCommArr, JSONObject paxClientFareJson, BigDecimal paxCount,
			JSONArray paxLevelClientCommArr,Map<String, String> commToTypeMap) {
		
		String commercialName;
		String commercialType;
		String commercialCurrency;
		BigDecimal clientcommercialAmount;
		String totalFareCurrency;
		
		
        
        
//		 Map<String, String> clientCommToTypeMap = getClientCommercialsAndTheirType(resClientJson);
		for(int k=0;k<entityCommArr.length();k++)
		{
			
//			    			JSONObject orderLevelJson = new JSONObject();
			JSONArray additinaldetailsArr = entityCommArr.getJSONObject(k).getJSONArray(JSON_PROP_ADDITIONCOMMDETAILS);
			JSONArray tempadditionJsonArr = new JSONArray();
			for(int p=0;p<additinaldetailsArr.length();p++)
			{
				JSONObject additionJson= additinaldetailsArr.getJSONObject(p);
				BigDecimal calculatedAmt = new BigDecimal(0);
				
				JSONObject paxTempJson = new JSONObject();
//				    				paxTempJson.put(JSON_PROP_COMMNAME, additionJson.getString(JSON_PROP_COMMNAME));
//				    				paxTempJson.put(JSON_PROP_COMMAMOUNT, additionJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
//				    				paxTempJson.put("commercialCurrency", additionJson.getString("commercialCurrency"));
//				    				paxTempJson.put(JSON_PROP_COMMTYPE, additionJson.getString(JSON_PROP_COMMTYPE));
				
//			    				JSONArray tempJsonArr = new JSONArray();
				if(clientcommTypeAmountMap.containsKey(additionJson.getString(JSON_PROP_COMMNAME)))
				{
					clientcommercialAmount = additionJson.getBigDecimal(JSON_PROP_COMMAMOUNT);
					calculatedAmt = clientcommercialAmount.add(clientcommTypeAmountMap.get(additionJson.getString(JSON_PROP_COMMNAME)));
					commercialCurrency = additionJson.getString(JSON_PROP_COMMCURRENCY);
			    	commercialName = additionJson.getString(JSON_PROP_COMMNAME);
		    		commercialType = additionJson.getString(JSON_PROP_COMMTYPE);
		    		JSONObject tempJson = new JSONObject();
		    		tempJson.put(JSON_PROP_COMMAMOUNT, calculatedAmt);
		    		tempJson.put(JSON_PROP_COMMCURRENCY, commercialCurrency);
		    		tempJson.put(JSON_PROP_COMMNAME, commercialName);
		    		tempJson.put(JSON_PROP_COMMTYPE, commercialType);
//							    		tempadditionJsonArr.put(tempJson);
		    		clientCommTotalArr.put(tempJson);
		    		paxTempJson = new JSONObject(tempJson.toString());
		    		paxTempJson.put(JSON_PROP_COMMAMOUNT, clientcommercialAmount);
		    		paxLevelClientCommArr.put(paxTempJson);
		    		clientcommTypeAmountMap.put(additionJson.getString(JSON_PROP_COMMNAME), clientcommercialAmount);
				}
				else
				{
					clientcommTypeAmountMap.put(additionJson.getString(JSON_PROP_COMMNAME), additionJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					JSONObject tempJson = new JSONObject();
					tempJson.put(JSON_PROP_COMMNAME, additionJson.getString(JSON_PROP_COMMNAME));
					tempJson.put(JSON_PROP_COMMAMOUNT, additionJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					tempJson.put(JSON_PROP_COMMCURRENCY, additionJson.getString(JSON_PROP_COMMCURRENCY));
					tempJson.put(JSON_PROP_COMMTYPE, additionJson.getString(JSON_PROP_COMMTYPE));
//				    					tempadditionJsonArr.put(tempJson);
					clientCommTotalArr.put(tempJson);
					paxLevelClientCommArr.put(tempJson);
				}
				orderLevelcommercialJson.put(JSON_PROP_ADDITIONCOMMDETAILS, tempadditionJsonArr);
				
				String additionalCommName = additionJson.optString(JSON_PROP_COMMNAME);
				if (COMM_TYPE_RECEIVABLE.equals(commToTypeMap.get(additionalCommName))) {
					String additionalCommCcy = additionJson.getString(JSON_PROP_COMMCCY);
					BigDecimal additionalCommAmt = additionJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
					calculatedAmt = calculatedAmt.add(additionalCommAmt);
					totalFareCompsGroup.add(mRcvsPriceCompQualifier.concat(additionalCommName).concat("@").concat(JSON_PROP_CODE), clientCcyCode, additionalCommAmt.multiply(paxCount));
				}
				
				
			}
			
			JSONArray retensionCommDetailsArr = entityCommArr.getJSONObject(k).getJSONArray(JSON_PROP_RETENSIONCOMMDETAILS);
			JSONArray tempRetensionJsonArr = new JSONArray();
			for(int p=0;p<retensionCommDetailsArr.length();p++)
			{
				JSONObject retensionJson= retensionCommDetailsArr.getJSONObject(p);
				JSONObject paxTempJson = new JSONObject();
//			    				JSONArray tempJsonArr = new JSONArray();
				BigDecimal calculatedAmt = new BigDecimal(0);
				if(clientcommTypeAmountMap.containsKey(retensionJson.getString(JSON_PROP_COMMNAME)))
				{
					clientcommercialAmount = retensionJson.getBigDecimal(JSON_PROP_COMMAMOUNT);
					 calculatedAmt = clientcommercialAmount.add((BigDecimal)clientcommTypeAmountMap.get(retensionJson.getString(JSON_PROP_COMMNAME)));
					commercialCurrency = retensionJson.getString(JSON_PROP_COMMCURRENCY);
			    	commercialName = retensionJson.getString(JSON_PROP_COMMNAME);
		    		commercialType = retensionJson.getString(JSON_PROP_COMMTYPE);
		    		JSONObject tempJson = new JSONObject();
		    		tempJson.put(JSON_PROP_COMMAMOUNT, calculatedAmt);
		    		tempJson.put(JSON_PROP_COMMCURRENCY, commercialCurrency);
		    		tempJson.put(JSON_PROP_COMMNAME, commercialName);
		    		tempJson.put(JSON_PROP_COMMTYPE, commercialType);
//							    		tempRetensionJsonArr.put(tempJson);
		    		clientCommTotalArr.put(tempJson);
		    		paxTempJson = new JSONObject(tempJson.toString());
		    		paxTempJson.put(JSON_PROP_COMMAMOUNT, clientcommercialAmount);
		    		paxLevelClientCommArr.put(paxTempJson);
					clientcommTypeAmountMap.put(retensionJson.getString(JSON_PROP_COMMNAME), clientcommercialAmount);

				}
				else
				{
					clientcommTypeAmountMap.put(retensionJson.getString(JSON_PROP_COMMNAME), retensionJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					JSONObject tempJson = new JSONObject();
					tempJson.put(JSON_PROP_COMMNAME, retensionJson.getString(JSON_PROP_COMMNAME));
					tempJson.put(JSON_PROP_COMMAMOUNT, retensionJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					tempJson.put(JSON_PROP_COMMCURRENCY, retensionJson.getString(JSON_PROP_COMMCURRENCY));
					tempJson.put(JSON_PROP_COMMTYPE, retensionJson.getString(JSON_PROP_COMMTYPE));
//				    					tempRetensionJsonArr.put(tempJson);
					clientCommTotalArr.put(tempJson);
					paxLevelClientCommArr.put(tempJson);
				}
				orderLevelcommercialJson.put(JSON_PROP_RETENSIONCOMMDETAILS, tempRetensionJsonArr);
			}
			JSONArray fixedCommDetailsArr = entityCommArr.getJSONObject(k).getJSONArray(JSON_PROP_FIXEDCOMMDETAILS);
			JSONArray tempFixedJsonArr = new JSONArray();
			for(int p=0;p<fixedCommDetailsArr.length();p++)
			{
				JSONObject fixedJson= fixedCommDetailsArr.getJSONObject(p);
				JSONObject paxTempJson = new JSONObject();
//			    				JSONArray tempJsonArr = new JSONArray();
				BigDecimal calculatedAmt = new BigDecimal(0);
				if(clientcommTypeAmountMap.containsKey(fixedJson.getString(JSON_PROP_COMMNAME)))
				{
					clientcommercialAmount = fixedJson.getBigDecimal(JSON_PROP_COMMAMOUNT);
					calculatedAmt = clientcommercialAmount.add(clientcommTypeAmountMap.get(fixedJson.getString(JSON_PROP_COMMNAME)));
					commercialCurrency = fixedJson.getString(JSON_PROP_COMMCURRENCY);
			    	commercialName = fixedJson.getString(JSON_PROP_COMMNAME);
		    		commercialType = fixedJson.getString(JSON_PROP_COMMTYPE);
		    		JSONObject tempJson = new JSONObject();
		    		tempJson.put(JSON_PROP_COMMAMOUNT, calculatedAmt);
		    		tempJson.put(JSON_PROP_COMMCURRENCY, commercialCurrency);
		    		tempJson.put(JSON_PROP_COMMNAME, commercialName);
		    		tempJson.put(JSON_PROP_COMMTYPE, commercialType);
//							    		tempFixedJsonArr.put(tempJson);
		    		clientCommTotalArr.put(tempJson);
		    		paxTempJson = new JSONObject(tempJson.toString());
		    		paxTempJson.put(JSON_PROP_COMMAMOUNT, clientcommercialAmount);
		    		paxLevelClientCommArr.put(paxTempJson);
					clientcommTypeAmountMap.put(fixedJson.getString(JSON_PROP_COMMNAME), clientcommercialAmount);

				}
				else
				{
					clientcommTypeAmountMap.put(fixedJson.getString(JSON_PROP_COMMNAME), fixedJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					JSONObject tempJson = new JSONObject();
					tempJson.put(JSON_PROP_COMMNAME, fixedJson.getString(JSON_PROP_COMMNAME));
					tempJson.put(JSON_PROP_COMMAMOUNT, fixedJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					tempJson.put(JSON_PROP_COMMCURRENCY, fixedJson.getString(JSON_PROP_COMMCURRENCY));
					tempJson.put(JSON_PROP_COMMTYPE, fixedJson.getString(JSON_PROP_COMMTYPE));
//				    					tempFixedJsonArr.put(tempJson);
					clientCommTotalArr.put(tempJson);
					paxLevelClientCommArr.put(tempJson);
				}
				orderLevelcommercialJson.put(JSON_PROP_FIXEDCOMMDETAILS, tempFixedJsonArr);
			}
			JSONObject markupJson = entityCommArr.getJSONObject(k).getJSONObject(JSON_PROP_MARKUPCOMDTLS);
			
			
			totalFare = totalFare.add(markupJson.getBigDecimal(JSON_PROP_TOTALFARE));
			totalFareCurrency = markupJson.getString(JSON_PROP_COMMCURRENCY);
			
			
			JSONObject paxTempJson = new JSONObject();
			
			JSONObject tempJson = new JSONObject();
			BigDecimal calculatedAmt = new BigDecimal(0);
			if(clientcommTypeAmountMap.containsKey(markupJson.getString(JSON_PROP_COMMNAME)))
			{
				clientcommercialAmount = markupJson.getBigDecimal(JSON_PROP_COMMAMOUNT);
				calculatedAmt =  clientcommercialAmount.add(clientcommTypeAmountMap.get(markupJson.getString(JSON_PROP_COMMNAME)));
				commercialCurrency = markupJson.getString(JSON_PROP_COMMCURRENCY);
		    	commercialName = markupJson.getString(JSON_PROP_COMMNAME);
				commercialType = markupJson.getString(JSON_PROP_COMMTYPE);
				tempJson.put(JSON_PROP_COMMAMOUNT, calculatedAmt);
				tempJson.put(JSON_PROP_COMMCURRENCY, commercialCurrency);
				tempJson.put(JSON_PROP_COMMNAME, commercialName);
				tempJson.put(JSON_PROP_COMMTYPE, commercialType);
//					    		tempJsonArr.put(tempJson);
				paxTempJson = new JSONObject(tempJson.toString());
				paxTempJson.put(JSON_PROP_COMMAMOUNT, clientcommercialAmount);
				paxLevelClientCommArr.put(paxTempJson);
				clientcommTypeAmountMap.put(markupJson.getString(JSON_PROP_COMMNAME), clientcommercialAmount);

			}
			else
			{
				clientcommTypeAmountMap.put(markupJson.getString(JSON_PROP_COMMNAME), markupJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
				
				tempJson.put(JSON_PROP_COMMNAME, markupJson.getString(JSON_PROP_COMMNAME));
				tempJson.put(JSON_PROP_COMMAMOUNT, markupJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
				tempJson.put(JSON_PROP_COMMCURRENCY, markupJson.getString(JSON_PROP_COMMCURRENCY));
				tempJson.put(JSON_PROP_COMMTYPE, markupJson.getString(JSON_PROP_COMMTYPE));
				paxLevelClientCommArr.put(tempJson);
//		    					tempJsonArr.put(tempJson);
			}
//				    			orderLevelcommercialJson.put("markUpCommercialDetails",tempJson);
			clientCommTotalArr.put(tempJson);
			
//			    			orderlevelclientCommArr.put(orderLevelcommercialJson);
			
			totalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, calculatedAmt.multiply(paxCount));
			
			totalFareJson.put(JSON_PROP_AMOUNT, totalFare);
			totalFareJson.put(JSON_PROP_CCYCODE, totalFareCurrency);
			paxClientFareJson.put(JSON_PROP_TOTALFARE, totalFareJson);//total added at pax level
			paxClientFareJson.put(JSON_PROP_SEATNO, passJson.getString(JSON_PROP_SEATNO));// calculation as per seat
			
			// clientcomm at pax level clientEntityDetailsJson
			JSONArray clientEntityCommArr = new JSONArray();
			JSONObject paxcliententitydtlsJson = new JSONObject(clientEntityDetailsJson.toString());
			paxcliententitydtlsJson.put(JSON_PROP_CLIENTCOMM, paxLevelClientCommArr);
			clientEntityCommArr.put(paxcliententitydtlsJson);
			paxClientFareJson.put(JSON_PROP_CLIENTENTITYCOMMS, clientEntityCommArr);
			paxClientFareJson.put(JSON_PROP_BASEFARE, totalFareJson);
			paxClientFaresArr.put(paxClientFareJson);
			
		}
	}



	private static void calculateSuppCommercials(JSONArray orderlevelCommArr, BigDecimal clientcommercialAmount,
			JSONObject orderLevelJson, Map<String, BigDecimal> suppliercommTypeAmountMap, JSONArray commdetailsArr,
			JSONArray paxLevelSupplierCommArr) {
		BigDecimal commercialAmount;
		for(int j=0;j<commdetailsArr.length();j++)
		{
			JSONObject paxTempJson = new JSONObject();
			JSONObject commJson = commdetailsArr.getJSONObject(j);
			BigDecimal calculatedAmt = new BigDecimal(0);
			if(suppliercommTypeAmountMap.containsKey(commJson.getString(JSON_PROP_COMMNAME)))
			{
				commercialAmount = commJson.getBigDecimal(JSON_PROP_COMMAMOUNT);
				calculatedAmt = commercialAmount.add(suppliercommTypeAmountMap.get(commJson.getString(JSON_PROP_COMMNAME)));
//				for(int m=0;m<orderlevelCommArr.length();m++)
//				{
//					if(orderlevelCommArr.getJSONObject(m).getString(JSON_PROP_COMMNAME).equals(commJson.getString(JSON_PROP_COMMNAME)))
//					{
//						orderLevelJson = orderlevelCommArr.getJSONObject(m);
//						break;
//					}
//				}
				orderLevelJson.put(JSON_PROP_COMMAMOUNT, calculatedAmt);
				orderLevelJson.put(JSON_PROP_COMMNAME, commJson.getString(JSON_PROP_COMMNAME));
				orderLevelJson.put("commercialCurrency", commJson.getString(JSON_PROP_COMMCURRENCY));
				orderLevelJson.put(JSON_PROP_COMMTYPE, commJson.getString(JSON_PROP_COMMTYPE));
				orderlevelCommArr.put(orderLevelJson);
				paxTempJson = new JSONObject(orderLevelJson.toString());
				paxTempJson.put(JSON_PROP_COMMAMOUNT, clientcommercialAmount);
				paxLevelSupplierCommArr.put(paxTempJson);
				suppliercommTypeAmountMap.put(commJson.getString(JSON_PROP_COMMNAME), calculatedAmt);
				
			}
			else
			{
				suppliercommTypeAmountMap.put(commJson.getString(JSON_PROP_COMMNAME), commJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
				 orderLevelJson = new JSONObject();
				orderLevelJson.put(JSON_PROP_COMMAMOUNT, commJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
				orderLevelJson.put(JSON_PROP_COMMNAME, commJson.getString(JSON_PROP_COMMNAME));
				orderLevelJson.put(JSON_PROP_COMMCURRENCY, commJson.getString(JSON_PROP_COMMCURRENCY));
				orderLevelJson.put(JSON_PROP_COMMTYPE, commJson.getString(JSON_PROP_COMMTYPE));
				orderlevelCommArr.put(orderLevelJson);
				paxLevelSupplierCommArr.put(orderLevelJson);

			}
			

		}
		
		
	}

	

	private static void getBookJSON(Element resBodyElem, JSONArray bookTktJsonArr) {
		
		JSONObject bookTktJson = new JSONObject();
		bookTktJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./bus:SupplierID"));
		bookTktJson.put(JSON_PROP_STATUS, XMLUtils.getValueAtXPath(resBodyElem, "./ota:Response/ota:IsSuccess"));
		bookTktJson.put("message", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Response/ota:Message"));
		bookTktJson.put(JSON_PROP_PNRNO, XMLUtils.getValueAtXPath(resBodyElem, "./ota:PNRNo"));
		bookTktJson.put(JSON_PROP_TICKETNO, XMLUtils.getValueAtXPath(resBodyElem, "./ota:TicketNo"));
		bookTktJson.put("transactionId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:TransactionId"));
		bookTktJson.put(JSON_PROP_JOURNEYDATE, XMLUtils.getValueAtXPath(resBodyElem, "./ota:DateOfJourney"));
		bookTktJson.put(JSON_PROP_TOTALFARE, XMLUtils.getValueAtXPath(resBodyElem, "./ota:TotalFare"));
		bookTktJson.put(JSON_PROP_CURRENCY, XMLUtils.getValueAtXPath(resBodyElem, "./ota:Currency"));
		JSONArray passArr = new JSONArray();
		Element[] passangerElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:Passengers/ota:Passenger");
		for(Element passElem : passangerElems)
		{
			getPassangers(passElem,passArr);
		}

		bookTktJson.put("passengers", passArr);
		bookTktJsonArr.put(bookTktJson);
	}

	public static void getPassangers(Element passElem, JSONArray passArr) {
		
		JSONObject passJson = new JSONObject();
		passJson.put(JSON_PROP_FIRSTNAME, XMLUtils.getValueAtXPath(passElem, "./ota:Name"));
		passJson.put("age", Utils.convertToInt(XMLUtils.getValueAtXPath(passElem, "./ota:Age").trim(), 0));
		passJson.put("gender", XMLUtils.getValueAtXPath(passElem, "./ota:Gender"));
		passJson.put(JSON_PROP_SEATNO, XMLUtils.getValueAtXPath(passElem, "./ota:SeatNo"));
		passJson.put("fare", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(passElem, "./ota:Fare").trim(), 0));
		passJson.put(JSON_PROP_CURRENCY, XMLUtils.getValueAtXPath(passElem, "./ota:Currency"));
		passJson.put("seatType", XMLUtils.getValueAtXPath(passElem, "./ota:SeatType"));
		passJson.put("isAcSeat", Boolean.valueOf(XMLUtils.getValueAtXPath(passElem, "./ota:IsAcSeat")));
		passArr.put(passJson);
		
		
	}
	

}
