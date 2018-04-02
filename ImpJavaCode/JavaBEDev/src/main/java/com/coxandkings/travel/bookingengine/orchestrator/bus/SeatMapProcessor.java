package com.coxandkings.travel.bookingengine.orchestrator.bus;

//import java.io.File;
//import java.io.PrintWriter;
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

import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;

import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class SeatMapProcessor implements BusConstants {

	private static final Logger logger = LogManager.getLogger(SeatMapProcessor.class);
	
	public static String process(JSONObject reqJson) {

		OperationConfig opConfig = BusConfig.getOperationConfig("seatmap");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();

		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./busi:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./bus:OTA_BusGetLayoutRQWrapper");
		reqBodyElem.removeChild(wrapperElem);

		try {
			TrackingContext.setTrackingContext(reqJson);

			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct("Transportation", "Bus");
			BusSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);

			String prevSuppID = "";
			JSONArray busserviceJSONArr = reqBodyJson.getJSONArray(JSON_PROP_SERVICE);

			for (int y = 0; y < busserviceJSONArr.length(); y++) {
				JSONObject busServiceJson = busserviceJSONArr.getJSONObject(y);
				String suppID = busServiceJson.getString(JSON_PROP_SUPPREF);

				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				reqBodyElem.appendChild(suppWrapperElem);
				// Element otaLayout = XMLUtils.getFirstElementAtXPath(suppWrapperElem,
				// "/busi:BusInterfaceRQ/busi:RequestBody/bus:OTA_BusGetLayoutRQWrapper/ota:OTA_BusGetLayoutRQ");
				
				

				Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
						"./bus:RequestHeader/com:SupplierCredentialsList");
				Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem,
						String.format("./com:SupplierCredentials[com:SupplierID = '%s']", suppID));
				for (ProductSupplier prodSupplier : prodSuppliers) {
					suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
				}

				Element otaLayout = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ");

				XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:SupplierID", busServiceJson.getString("supplierRef"));
				XMLUtils.setValueAtXPath(suppWrapperElem, "./bus:Sequence", String.valueOf(y));
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:sourceStationId",
						busServiceJson.get(JSON_PROP_SOURCE).toString());
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:destinationStationId",
						busServiceJson.get(JSON_PROP_DESTINATION).toString());
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:JourneyDate",
						busServiceJson.getString(JSON_PROP_JOURNEYDATE));
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:operatorId",
						busServiceJson.get(JSON_PROP_OPERATORID).toString());
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:serviceId",
						busServiceJson.get(JSON_PROP_SERVICEID).toString());
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:layoutId",
						busServiceJson.get(JSON_PROP_LAYOUTID).toString());
				XMLUtils.setValueAtXPath(suppWrapperElem, "./ota:OTA_BusGetLayoutRQ/ota:seatFare",
						busServiceJson.get("seatFare").toString());
				

			}
			// System.out.println(XMLTransformer.toString(reqElem));
			// PrintWriter pw = new PrintWriter(new
			// File("D:\\BE\\temp\\seatmap\\siRQ_xml.txt"));
			// pw.write(XMLTransformer.toString(reqElem));
			// pw.close();
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					BusConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
//			 System.out.println(XMLTransformer.toString(resElem));

			JSONObject resBodyJson = new JSONObject();
			// JSONArray seatDetailsArr = new JSONArray();
			JSONArray availabilityJsonArr = new JSONArray();
			JSONObject layoutDetails = new JSONObject();
			int idx = 0;
//			Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem,
//					"./busi:ResponseBody/bus:OTA_BusGetLayoutRSWrapper");
			
			Element[] wrapperElems=sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem,
					"./busi:ResponseBody/bus:OTA_BusGetLayoutRSWrapper"));

			for (Element wrapperElement : wrapperElems) {
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ota:OTA_BusGetLayoutRS");
				getSupplierResponseAvailableTripsJSON(resBodyElem, availabilityJsonArr, true, idx++);
			}

			resBodyJson.put(JSON_PROP_AVAILABILITY, availabilityJsonArr);
			// resBodyJson.put("travelInfo", getTravelInfo(resBodyElem));
//			 PrintWriter pw1 = new PrintWriter(new
//			 File("D:\\BE\\temp\\seatmap\\si_json.json"));
//			 pw1.write(resBodyJson.toString());
//			 pw1.close();

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			// System.out.println(resBodyJson.toString());
			Map<String,Integer> BRMS2SIBusMap = new HashMap<String,Integer>();
			JSONObject resSupplierJson = SupplierCommercials.getSupplierCommercials(reqJson, resJson,BRMS2SIBusMap);
//			PrintWriter pw2 = new PrintWriter(new File("D:\\BE\\temp\\seatmap\\suppJson.json"));
//			pw2.write(resSupplierJson.toString());
//			pw2.close();

			JSONObject radisSupplierJson = new JSONObject(resSupplierJson.toString());
			JSONObject resClientJson = ClientCommercials.getClientCommercials(resSupplierJson);
//			PrintWriter pw3 = new PrintWriter(new File("D:\\BE\\temp\\seatmap\\clientJson.txt"));
//			pw3.write(resClientJson.toString());
//			pw3.close();

			BusSearchProcessor.calculatePrices(reqJson, resJson, resClientJson, BRMS2SIBusMap,true);
			
//			calculatePrices(reqJson, resJson, radisSupplierJson,resClientJson, BRMS2SIBusMap,true);
			// PrintWriter pw4 = new PrintWriter(new
			// File("D:\\BE\\temp\\seatmap\\final_json.json"));
			// pw4.write(resBodyJson.toString());
			// pw4.close();
			
			pushSuppFaresToRedisAndRemove(resJson, radisSupplierJson, resClientJson);
			
			return resJson.toString();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public static Element[] sortWrapperElementsBySequence(Element[] wrapperElems) {
		Map<Integer, Element> wrapperElemsMap = new TreeMap<Integer, Element>();
		for (Element wrapperElem : wrapperElems) {
			wrapperElemsMap.put(Utils.convertToInt(XMLUtils.getValueAtXPath(wrapperElem, "./bus:Sequence"), 0), wrapperElem);
		}
		
		int  idx = 0;
		Element[] seqSortedWrapperElems = new Element[wrapperElems.length];
		Iterator<Map.Entry<Integer, Element>> wrapperElemsIter = wrapperElemsMap.entrySet().iterator();
		while (wrapperElemsIter.hasNext()) {
			seqSortedWrapperElems[idx++] = wrapperElemsIter.next().getValue();
		}
		
		return seqSortedWrapperElems;
	}

	private static void calculatePrices(JSONObject reqJson, JSONObject resJson, JSONObject radisSupplierJson,JSONObject resClientJson,
			Map<String, Integer> bRMS2SIBusMap, boolean b) {
		
		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		try
		{
			JSONArray availJsonArr = resBodyJson.optJSONArray(JSON_PROP_AVAILABILITY);
			if (availJsonArr == null) {
				return;
			}
			Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();
			
			 Map<String, String> suppCommToTypeMap = getSupplierCommercialsAndTheirType(radisSupplierJson);
			 
			 Map<String, String> clientCommToTypeMap = getClientCommercialsAndTheirType(resClientJson);
			 
			 String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
			 String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
			
			 
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private static Map<String, String> getClientCommercialsAndTheirType(JSONObject resClientJson) {

		JSONArray commHeadJsonArr = null;
        JSONObject commHeadJson = null;
        JSONArray entityDtlsJsonArr = null;
        Map<String, String> commToTypeMap = new HashMap<String, String>();
        JSONArray ccommBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(resClientJson);
        for (int i=0; i < ccommBRIJsonArr.length(); i++) {
        	if ((entityDtlsJsonArr = ccommBRIJsonArr.getJSONObject(i).optJSONArray(JSON_PROP_ENTITYDETAILS)) == null) {
        		continue;
        	}
        	for (int j=0; j < entityDtlsJsonArr.length(); j++) {
            	if ((commHeadJsonArr = entityDtlsJsonArr.getJSONObject(j).optJSONArray(JSON_PROP_COMMHEAD)) == null) {
            		logger.warn("No commercial heads found in client commercials");
            		continue;
            	}
            	
            	for (int k=0; k < commHeadJsonArr.length(); k++) {
            		commHeadJson = commHeadJsonArr.getJSONObject(k);
            		commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME), commHeadJson.getString(JSON_PROP_COMMTYPE));
            	}
        	}
        }
        
        return commToTypeMap;

		
	}

	private static JSONArray getClientCommercialsBusinessRuleIntakeJSONArray(JSONObject resClientJson) {
    	return resClientJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject("value").getJSONObject(JSON_PROP_CLIENTTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);

	}

	private static Map<String, String> getSupplierCommercialsAndTheirType(JSONObject radisSupplierJson) {
		
		JSONArray commHeadJsonArr = null;
        JSONObject commHeadJson = null;
        Map<String, String> commToTypeMap = new HashMap<String, String>();
        JSONArray scommBRIJsonArr = getSupplierCommercialsBusinessRuleIntakeJSONArray(radisSupplierJson);
        for (int i=0; i < scommBRIJsonArr.length(); i++) {
        	if ((commHeadJsonArr = scommBRIJsonArr.getJSONObject(i).optJSONArray(JSON_PROP_COMMHEAD)) == null) {
        		logger.warn("No commercial heads found in supplier commercials");
        		continue;
        	}
        	
        	for (int j=0; j < commHeadJsonArr.length(); j++) {
        		commHeadJson = commHeadJsonArr.getJSONObject(j);
        		commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME), commHeadJson.getString(JSON_PROP_COMMTYPE));
        	}
        }
        
        return commToTypeMap;
	}

	private static JSONArray getSupplierCommercialsBusinessRuleIntakeJSONArray(JSONObject radisSupplierJson) {

    	return radisSupplierJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject("value").getJSONObject(JSON_PROP_SUPPLIERTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);

	}

	private static void pushSuppFaresToRedisAndRemove(JSONObject resJson, JSONObject resSupplierJson,
			JSONObject resClientJson) {

		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		try {
			JSONArray availJsonArr = resBodyJson.optJSONArray(JSON_PROP_AVAILABILITY);

			if (availJsonArr == null) {
				return;
			}

			Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();

			Map<String, String> suppCommMap = new HashMap<String, String>();

			Map<String, String> clientCommMap = new HashMap<String, String>();

			for (int j = 0; j < availJsonArr.length(); j++) {
				JSONArray serviceJsonArr = availJsonArr.getJSONObject(j).getJSONArray(JSON_PROP_SERVICE);

				for (int i = 0; i < serviceJsonArr.length(); i++) {

					JSONObject serviceJson = serviceJsonArr.getJSONObject(i);

					JSONArray suppFAresArr = new JSONArray();
				
					suppFAresArr = serviceJsonArr.getJSONObject(i).getJSONArray(JSON_PROP_SUPPLIERFARES);
					for (int l = 0; l < suppFAresArr.length(); l++) {
						JSONObject suppFareJson = suppFAresArr.getJSONObject(l);
						JSONObject mapJson = new JSONObject();
						mapJson.put("fare", suppFareJson.getBigDecimal("fare"));
						mapJson.put(JSON_PROP_CURRENCY, suppFareJson.getString(JSON_PROP_CURRENCY));
						reprcSuppFaresMap.put(getRedisKeyForSeatFare(serviceJson, suppFareJson),
								mapJson.toString());
					}
					serviceJsonArr.getJSONObject(i).remove(JSON_PROP_SUPPLIERFARES);

					String redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS)
							.concat("|").concat("seatmap");
					Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
					redisConn.hmset(redisKey, reprcSuppFaresMap);
					redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
					RedisConfig.releaseRedisConnectionToPool(redisConn);
					
					JSONArray faresArr = serviceJsonArr.getJSONObject(i).getJSONArray(JSON_PROP_FARESARRAY);

					for (int k = 0; k < faresArr.length(); k++) {

						
						
						redisConn = RedisConfig.getRedisConnectionFromPool();
						 redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS)
								.concat("|").concat("seatmap");
						  reprcSuppFaresMap = redisConn.hgetAll(redisKey);
							if (reprcSuppFaresMap == null) {
								throw new Exception(String.format("GetLayout context not found,for %s", redisKey));
							}
						 
						
						JSONArray ccommSuppBRIJsonArr = resSupplierJson.getJSONObject("result")
								.getJSONObject("execution-results").getJSONArray("results").getJSONObject(0)
								.getJSONObject("value")
								.getJSONObject("cnk.bus_commercialscalculationengine.suppliertransactionalrules.Root")
								.getJSONArray("businessRuleIntake");

						for (int m = 0; m < ccommSuppBRIJsonArr.length(); m++) {
							JSONObject briJson = ccommSuppBRIJsonArr.getJSONObject(m);
							
							Map<String, String> commercialHeadMap = new HashMap<String, String>();
							JSONArray commHeadArr = briJson.getJSONArray(JSON_PROP_COMMHEAD);
							
							getSupplierCommercialHeadMap(commercialHeadMap, commHeadArr);
							
							JSONArray busServicedtlsArr = briJson.getJSONArray(JSON_PROP_BUSSERVICEDETAILS);
							for (int n = 0; n < busServicedtlsArr.length(); n++) {
								JSONObject busDetails = busServicedtlsArr.getJSONObject(n);
								JSONArray commdetailsArr = busDetails.getJSONArray(JSON_PROP_PASSANGERDETAILS).getJSONObject(0)
										.getJSONArray("commercialDetails");
								JSONObject fareJson = faresArr.optJSONObject(n);
								for (int p = 0; p < commdetailsArr.length(); p++) {
									JSONObject commJson = commdetailsArr.getJSONObject(p);
//									commJson.put("commercialType",
//											commHeadArr.getJSONObject(p).getString("commercialType"));
									commJson.put(JSON_PROP_COMMTYPE, commercialHeadMap.get(commJson.getString(JSON_PROP_COMMNAME)));
									
									if((reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson))!=null)) {
										
										JSONObject mapJson = new JSONObject(reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)));
										if(commJson.optString("commercialCurrency").equals(""))
										{
										commJson.put("commercialCurrency", mapJson.getString(JSON_PROP_CURRENCY));
										}
									}
								}
								
								suppCommMap.put(getRedisKeyForSeatFare(serviceJson, fareJson), busDetails.toString());

							}
							redisKey = resHeaderJson.getString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS)
									.concat("|").concat("suppComm");
							
							redisConn.hmset(redisKey, suppCommMap);
							redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
						}
					
		// ---------------------------------------------------------------------------------------------------------------
						JSONArray ccommClientBRIJsonArr = resClientJson.getJSONObject("result")
								.getJSONObject("execution-results").getJSONArray("results").getJSONObject(0)
								.getJSONObject("value")
								.getJSONObject("cnk.bus_commercialscalculationengine.clienttransactionalrules.Root")
								.getJSONArray("businessRuleIntake");

						for (int n = 0; n < ccommClientBRIJsonArr.length(); n++) {
							JSONObject briJson = ccommClientBRIJsonArr.getJSONObject(n);
							
							Map<String, String> commercialHeadMap = new HashMap<String, String>();
							Map<String,String> entityToCommHeadMap = new HashMap<String,String>();
							
							JSONArray entityArr = briJson.getJSONArray("entityDetails");
							
							
							for (int m = 0; m < entityArr.length(); m++) {
								JSONArray commHeadArr = entityArr.getJSONObject(m).getJSONArray(JSON_PROP_COMMHEAD);

								
			                     Map<String, String> commToTypeMap = new HashMap<String, String>();
			                     JSONObject cCommEntityJson = entityArr.getJSONObject(m);
			                     
			                     JSONObject commHeadJson = new JSONObject();
			                     
			                     for (int s = 0; s < commHeadArr.length(); s++) {
			                           commHeadJson = commHeadArr.getJSONObject(s);
			                           commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME),
			                                         commHeadJson.getString(JSON_PROP_COMMTYPE));
			                     }
			                     entityToCommHeadMap.put(cCommEntityJson.optString(JSON_PROP_ENTITYNAME), new JSONObject(commToTypeMap).toString());
			                     
			                     Map<String, String> clientcommercialHeadMap = new HashMap<String, String>();
			                     getClientCommercialHeadMap(clientcommercialHeadMap,entityToCommHeadMap,cCommEntityJson);
			                     
								JSONArray busServicedtlsArr = briJson.getJSONArray(JSON_PROP_BUSSERVICEDETAILS);
								for (int q = 0; q < busServicedtlsArr.length(); q++) {
									JSONArray entityCommArr = busServicedtlsArr.getJSONObject(q)
											.getJSONArray(JSON_PROP_PASSANGERDETAILS).getJSONObject(0)
											.getJSONArray(JSON_PROP_ENTITYCOMMS);
									JSONObject fareJson = faresArr.optJSONObject(q);
									JSONArray additionalCommArr = entityCommArr.getJSONObject(m)
											.getJSONArray(JSON_PROP_ADDITIONCOMMDETAILS);
									for (int p = 0; p < additionalCommArr.length(); p++) {
										String commType = clientcommercialHeadMap
												.get(additionalCommArr.getJSONObject(p).getString(JSON_PROP_COMMNAME));
										additionalCommArr.getJSONObject(p).put(JSON_PROP_COMMTYPE, commType);
										
										
										if((reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)))!=null)
										{
											
												JSONObject mapJson = new JSONObject(reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)));
												if(additionalCommArr.getJSONObject(p).optString(JSON_PROP_COMMCURRENCY).equals(""))
												{
													additionalCommArr.getJSONObject(p).put(JSON_PROP_COMMCURRENCY, mapJson.getString(JSON_PROP_CURRENCY));
												}
											
										}
										
									}
									
									JSONArray fixedCommArr = entityCommArr.getJSONObject(m)
											.getJSONArray(JSON_PROP_FIXEDCOMMDETAILS);
									for (int p = 0; p < fixedCommArr.length(); p++) {
										String commType = clientcommercialHeadMap
												.get(fixedCommArr.getJSONObject(p).getString(JSON_PROP_COMMNAME));
										fixedCommArr.getJSONObject(p).put(JSON_PROP_COMMTYPE, commType);
										
										if((reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)))!=null)
										{
											JSONObject mapJson = new JSONObject(reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)));
											if(fixedCommArr.getJSONObject(p).optString(JSON_PROP_COMMCURRENCY).equals(""))
											{
												fixedCommArr.getJSONObject(p).put(JSON_PROP_COMMCURRENCY, mapJson.getString(JSON_PROP_CURRENCY));
											}
										}
										
									}

									JSONArray retensionCommArr = entityCommArr.getJSONObject(m)
											.getJSONArray(JSON_PROP_RETENSIONCOMMDETAILS);
									for (int p = 0; p < retensionCommArr.length(); p++) {
										String commType = clientcommercialHeadMap
												.get(retensionCommArr.getJSONObject(p).getString(JSON_PROP_COMMNAME));
										retensionCommArr.getJSONObject(p).put(JSON_PROP_COMMTYPE, commType);
										if((reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)))!=null)
										{
											JSONObject mapJson = new JSONObject(reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)));
											if(retensionCommArr.getJSONObject(p).optString(JSON_PROP_COMMCURRENCY).equals(""))
											{
												retensionCommArr.getJSONObject(p).put(JSON_PROP_COMMCURRENCY, mapJson.getString(JSON_PROP_CURRENCY));
											}
										}
									}

									JSONObject maekupCommJson = entityCommArr.getJSONObject(m)
											.getJSONObject(JSON_PROP_MARKUPCOMDTLS);
									String commType = clientcommercialHeadMap.get(maekupCommJson.getString(JSON_PROP_COMMNAME));
									maekupCommJson.put(JSON_PROP_COMMTYPE, commType);
									
									if((reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)))!=null)
									{
										JSONObject mapJson = new JSONObject(reprcSuppFaresMap.get(getRedisKeyForSeatFare(serviceJson, fareJson)));
										if(maekupCommJson.optString(JSON_PROP_COMMCURRENCY).equals(""))
										{
											maekupCommJson.put(JSON_PROP_COMMCURRENCY, mapJson.getString(JSON_PROP_CURRENCY));
										}
									}
									
									JSONObject busDetails = busServicedtlsArr.getJSONObject(q);
									clientCommMap.put(getRedisKeyForSeatFare(serviceJson, fareJson),
											busDetails.toString());

								}
							}

							JSONArray busServicedtlsArr = briJson.getJSONArray(JSON_PROP_BUSSERVICEDETAILS);
							for (int p = 0; p < busServicedtlsArr.length(); p++) {
								
								JSONObject busDetails = busServicedtlsArr.getJSONObject(p);
								clientCommMap.put(getRedisKeyForSeatFare(briJson, busDetails), busDetails.toString());

							}
							redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS)
									.concat("|").concat("clientComm");
					
							redisConn.hmset(redisKey, clientCommMap);
							redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
							RedisConfig.releaseRedisConnectionToPool(redisConn);
							
//							redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_BUS)
//									.concat("|").concat("entity-CommHead");
//							redisConn.hmset(redisKey,entityToCommHeadMap);
//							redisConn.pexpire(redisKey, (long) (BusConfig.getRedisTTLMinutes() * 60 * 1000));
//							RedisConfig.releaseRedisConnectionToPool(redisConn);
							
						}
//						
					}
				}

			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void toMap(Map<String, String> clientcommercialHeadMap,JSONObject object) {
//		 Map<String, String> map = new HashMap<String, String>();

		 Iterator<String> keysItr = object.keys();
		 while(keysItr.hasNext()) {
		 String key = keysItr.next();
		 Object value = object.get(key);

//		 if(value instanceof JSONArray) {
//		 value = toList((JSONArray) value);
//		 }
//
//		 else if(value instanceof JSONObject) {
//		 value = toMap((JSONObject) value);
//		 }
		 clientcommercialHeadMap.put(key, value.toString());
		 }
		
		}

	private static void getSupplierCommercialHeadMap(Map<String, String> commercialHeadMap, JSONArray commHeadArr) {

		for(int x=0;x<commHeadArr.length();x++)
		{
			JSONObject commHeadJson = commHeadArr.getJSONObject(x);
			commercialHeadMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME), commHeadJson.getString(JSON_PROP_COMMTYPE));
		}
		
	}

	private static void getClientCommercialHeadMap(Map<String, String> clientcommercialHeadMap,Map<String,String> entityToCommHeadMap,JSONObject cCommEntityJson) {
		
		String mapStr = entityToCommHeadMap.get(cCommEntityJson.optString(JSON_PROP_ENTITYNAME));
		JSONObject mapObject = new JSONObject(mapStr);
		toMap(clientcommercialHeadMap,mapObject);
		

	}

	public static String getRedisKeyForSeatFare(JSONObject serviceJson, JSONObject fareJson) {
		
		//TODO: add operatorid,serviceid,layoutid to key
		
		if(fareJson!=null)
		{
		try {
			
			StringBuilder strBldr = new StringBuilder(serviceJson.optString(JSON_PROP_SUPPREF));
			strBldr.append("|");
			// strBldr.append(fareJson.get("column"));
			// strBldr.append("|");
			// strBldr.append(fareJson.get("Height"));
			// strBldr.append("|");
			if(fareJson.get(JSON_PROP_SEATNO).equals("null") || fareJson.get(JSON_PROP_SEATNO).equals("") || fareJson.get(JSON_PROP_SEATNO).equals(null))
			{
				strBldr.append("");
			}
			else
			{
				strBldr.append(fareJson.opt(JSON_PROP_SEATNO));
			}
			strBldr.append("|");

			return strBldr.toString();
		}
		catch(Exception e)
		{
			
			return "";
		}
		}
		return "";
	}

	
	private static void getSupplierResponseAvailableTripsJSON(Element resBodyElem, JSONArray availabilityJsonArr,
			boolean generateBookRefIdx, int bookRefIdx) {
		// TODO Auto-generated method stub
		Element[] seatDetailElems = XMLUtils.getElementsAtXPath(resBodyElem,
				"./ota:Layout/ota:SeatDetails/ota:SeatDetail");

		

		JSONObject avail = new JSONObject();
		
		avail.put(JSON_PROP_SUPPREF,
				XMLUtils.getValueAtXPath((Element) resBodyElem.getParentNode(), "./bus:SupplierID"));

		avail.put(JSON_PROP_JOURNEYDATE, XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:JourneyDate"));
		avail.put("companyName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:CompanyName"));
		avail.put("companyName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:CompanyName"));
		avail.put("sourceName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:FromCityName"));
		avail.put("destinationName", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:ToCityName"));
		avail.put("sourceId",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:FromCityId"), 0));
		avail.put("destinationId",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:ToCityId"), 0));
		avail.put("seatLayoutId",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:SeatLayoutId"), 0));
		avail.put(JSON_PROP_SERVICEID,
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:ServiceId"), 0));
		avail.put("route", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:Route"));
		avail.put(JSON_PROP_OPERATORID,
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:OperatorId"), 0));
		avail.put("departureTime", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:DepartureTime"));
		avail.put("arrivalTime", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:ArrivalTime"));
		avail.put("fare", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:Fare"));
		avail.put(JSON_PROP_CURRENCY, XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:Currency"));
		avail.put("pickupId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:PickupID"));
		avail.put("dropId", XMLUtils.getValueAtXPath(resBodyElem, "./ota:Info/ota:DropID"));

		Element[] layoutElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:Layout");
		JSONArray serviceArr = new JSONArray();
		for (Element layout : layoutElems) {
			getSeatDetailJSON(resBodyElem, serviceArr);
		}

		avail.put(JSON_PROP_SERVICE, serviceArr);
		
		availabilityJsonArr.put(avail);

	}

	private static void getSeatDetailJSON(Element resBodyElem, JSONArray serviceArr) {

		JSONObject seatDetail = new JSONObject();
		
		seatDetail.put(JSON_PROP_SUPPREF,
				XMLUtils.getValueAtXPath((Element) resBodyElem.getParentNode(), "./bus:SupplierID"));
		seatDetail.put("upperTotalRows",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:upperTotalRows"), 0));
		seatDetail.put("lowerDividerRow",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:lowerDividerRow"), 0));
		seatDetail.put("lowerTotalColumns",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:lowerTotalColumns"), 0));
		seatDetail.put("upperDividerRow",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:upperDividerRow"), 0));
		seatDetail.put("lowerTotalRows",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:lowerTotalRows"), 0));
		seatDetail.put("tentativeSeats",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:tentativeSeats"), 0));
		seatDetail.put("maxNumberOfSeats",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:maxNumberOfSeats"), 0));
		seatDetail.put("maxRows",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:MaxRows"), 0));
		seatDetail.put("maxColumns",
				Utils.convertToInt(XMLUtils.getValueAtXPath(resBodyElem, "./ota:Layout/ota:MaxColumns"), 0));

		JSONArray fareArr = new JSONArray();
		

		Element[] seatDetailElems = XMLUtils.getElementsAtXPath(resBodyElem,
				"./ota:Layout/ota:SeatDetails/ota:SeatDetail");
		for (Element seatDetailElem : seatDetailElems) {
			getFaresArray(seatDetailElem, fareArr);
		}
		seatDetail.put("fares", fareArr);

		
		serviceArr.put(seatDetail);

	}

	private static JSONArray getFaresArray(Element seatDetailElem, JSONArray fareArr) {

		
		JSONObject seatDetail = new JSONObject();
		
		seatDetail.put("row", Utils.convertToInt(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Row").trim(), 0));
		seatDetail.put("column",
				Utils.convertToInt(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Column").trim(), 0));
		seatDetail.put("height",
				Utils.convertToInt(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Height").trim(), 0));
		seatDetail.put("width", Utils.convertToInt(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Width").trim(), 0));
		seatDetail.put("length",
				Utils.convertToInt(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:length").trim(), 0));
		seatDetail.put(JSON_PROP_SEATNO, XMLUtils.getValueAtXPath(seatDetailElem, "./ota:SeatNo"));
		seatDetail.put("availability", XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Availability"));
		seatDetail.put("gender", XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Gender"));
		seatDetail.put("AC", XMLUtils.getValueAtXPath(seatDetailElem, "./ota:IsAc"));
		seatDetail.put("fare",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Fare").trim(), 0));
		seatDetail.put("childFare",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:ChildFare").trim(), 0));
		seatDetail.put("infantFare",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:InfantFare").trim(), 0));
		seatDetail.put("baseFare",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:baseFare").trim(), 0));
		seatDetail.put(JSON_PROP_CURRENCY, XMLUtils.getValueAtXPath(seatDetailElem, "./ota:Currency"));
		seatDetail.put("SeatTypeId",
				Utils.convertToInt(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:SeatTypeId").trim(), 0));
		seatDetail.put("serviceTax",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:ServiceTax").trim(), 0));
		seatDetail.put("isAisle", Boolean.valueOf(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:IsAisle")));
		seatDetail.put("deck", XMLUtils.getValueAtXPath(seatDetailElem, "./Deck"));
		seatDetail.put("isAc", Boolean.valueOf(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:IsAc")));
		seatDetail.put("isSleeper", Boolean.valueOf(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:IsSleeper")));
		seatDetail.put("markupFareAbsolute",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:markupFareAbsolute"), 0));
		seatDetail.put("operatorServiceChargeAbsolute", Utils.convertToBigDecimal(
				XMLUtils.getValueAtXPath(seatDetailElem, "./ota:operatorServiceChargeAbsolute"), 0));
		seatDetail.put("operatorServiceChargeAbsolute",
				XMLUtils.getValueAtXPath(seatDetailElem, "./ota:operatorServiceChargeAbsolute"));
		seatDetail.put("serviceTaxPercentage", XMLUtils.getValueAtXPath(seatDetailElem, "./ota:serviceTaxPercentage"));
		seatDetail.put("ladiesSeat", Boolean.valueOf(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:ladiesSeat")));
		seatDetail.put("serviceTaxAbsolute",
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(seatDetailElem, "./ota:serviceTaxAbsolute"), 0));
		seatDetail.put("markupFarePercentage", XMLUtils.getValueAtXPath(seatDetailElem, "./ota:markupFarePercentage"));
		seatDetail.put("zIndex", XMLUtils.getValueAtXPath(seatDetailElem, "./ota:zIndex"));
		fareArr.put(seatDetail);
		return fareArr;

	}

	

}
