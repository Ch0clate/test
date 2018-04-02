package com.coxandkings.travel.bookingengine.orchestrator.car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.car.CarConfig;
import com.coxandkings.travel.bookingengine.orchestrator.car.CarSearchProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.car.ClientCommercials;
import com.coxandkings.travel.bookingengine.orchestrator.car.SupplierCommercials;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class CarPriceProcessor implements CarConstants{
	
	private static final Logger logger = LogManager.getLogger(CarPriceProcessor.class);
	
	public static String process(JSONObject reqJson) {
		
		try {
			OperationConfig opConfig = CarConfig.getOperationConfig("price");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cari:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./car:OTA_VehAvailRateRQWrapper");
			reqBodyElem.removeChild(wrapperElem);

			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);

			CarSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);

			JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_CARRENTALARR);
			for (int y = 0; y < multiReqArr.length(); y++) {
				
				JSONObject carRentalReq = multiReqArr.getJSONObject(y);
				Element suppWrapperElem = null;
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				XMLUtils.setValueAtXPath(suppWrapperElem, "./car:Sequence", String.valueOf(y));
				reqBodyElem.appendChild(suppWrapperElem);
				populateWrapperElement(reqElem, ownerDoc, suppWrapperElem, usrCtx, carRentalReq);
			}
			
	        
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(),
					CarConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			
	        
	        int idx=0;
			JSONObject resBodyJson = new JSONObject();
			JSONArray vehicleAvailJsonArray = null;
			Element[] resWrapperElems = XMLUtils.getElementsAtXPath(resElem,
					"./cari:ResponseBody/car:OTA_VehAvailRateRSWrapper");
			int sequence = 0;
			String sequence_str;
			JSONArray carRentalArr = new JSONArray();
			for (Element resWrapperElem : resWrapperElems) {
				
				vehicleAvailJsonArray = new JSONArray();
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_VehAvailRateRS/ota:VehAvailRSCore");
				CarSearchProcessor.getSupplierResponseVehicleAvailJSON(resBodyElem, vehicleAvailJsonArray, true, idx++);
				sequence = (sequence_str = XMLUtils.getValueAtXPath(resWrapperElem, "./car:Sequence")).isEmpty()? sequence: Integer.parseInt(sequence_str);
				carRentalArr.put(sequence, (new JSONObject()).put(JSON_PROP_VEHICLEAVAIL, vehicleAvailJsonArray));
			}			
		
			for (int y = 0; y < multiReqArr.length(); y++) {
				String city = multiReqArr.getJSONObject(y).optString(JSON_PROP_CITY);
				JSONArray vehicleAvailArr = carRentalArr.getJSONObject(y).getJSONArray(JSON_PROP_VEHICLEAVAIL);
				for(int i=0;i < vehicleAvailArr.length();i++) {
					
					JSONObject resvehicleAvail = vehicleAvailArr.getJSONObject(i);
					resvehicleAvail.put(JSON_PROP_CITY, city);
				}	
			}

			resBodyJson.put(JSON_PROP_CARRENTALARR, carRentalArr);
			
			JSONObject resJson = new JSONObject();
			resJson.put("responseHeader", reqHdrJson);
			resJson.put("responseBody", resBodyJson);
			
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));
	        
	        Map<String,Integer> suppResToBRIIndex = new HashMap<String, Integer>();
			JSONObject resSuppCommJson = SupplierCommercials.getSupplierCommercials(reqJson, resJson, suppResToBRIIndex);
			JSONObject resClientCommJson = ClientCommercials.getClientCommercials(resSuppCommJson);
			CarSearchProcessor.calculatePricesV2(reqJson, resJson, resSuppCommJson, resClientCommJson, suppResToBRIIndex, true, usrCtx, true);
			
			pushSuppFaresToRedisAndRemove(resJson);
			
			return resJson.toString();
			
		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}


	private static void pushSuppFaresToRedisAndRemove(JSONObject resJson) {
		
		JSONObject resHdrJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		JSONArray carRentalInfoArr = resBodyJson.optJSONArray(JSON_PROP_CARRENTALARR);
		
		Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();
		for(Object carRentalInfo : carRentalInfoArr) {
			JSONArray vehicleAvailJsonArr = ((JSONObject) carRentalInfo).getJSONArray(JSON_PROP_VEHICLEAVAIL);
			for (int i=0; i < vehicleAvailJsonArr.length(); i++) {
				JSONObject suppPriceBookInfoJson = new JSONObject();
				JSONObject vehicleAvailJson = vehicleAvailJsonArr.getJSONObject(i);
				JSONObject suppPriceInfoJson = vehicleAvailJson.optJSONObject(JSON_PROP_SUPPPRICEINFO);
				vehicleAvailJson.remove(JSON_PROP_SUPPPRICEINFO);
				vehicleAvailJson.remove(JSON_PROP_BOOKREFIDX);
				
				//Getting ClientCommercials Info
				JSONObject totalPriceInfo = vehicleAvailJson.getJSONObject(JSON_PROP_TOTALPRICEINFO);
				JSONArray clientCommercialsTotalJsonArr = totalPriceInfo.optJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMS);
				totalPriceInfo.remove(JSON_PROP_CLIENTENTITYTOTALCOMMS);
				
				if ( suppPriceInfoJson == null) {
					// TODO: This should never happen. Log a warning message here.
					continue;
				}
				suppPriceBookInfoJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceInfoJson);
				suppPriceBookInfoJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientCommercialsTotalJsonArr);
				reprcSuppFaresMap.put(getRedisKeyForVehicleAvail(vehicleAvailJson), suppPriceBookInfoJson.toString());
			}
		}
		
		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		String redisKey = String.format("%s%c%s", resHdrJson.optString(JSON_PROP_SESSIONID), KEYSEPARATOR, PRODUCT_CAR);
		redisConn.hmset(redisKey, reprcSuppFaresMap);
		redisConn.pexpire(redisKey, (long) (CarConfig.getRedisTTLMinutes() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);
	}
	
	/*private static void pushClientCommericalsToRedis(JSONObject resJson, JSONObject resClientCommJson, Map<String,Integer> suppResToBRIIndex) {
		
		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		JSONArray carRentalInfoJsonArr = resBodyJson.optJSONArray(JSON_PROP_CARRENTALARR);
		JSONArray ccommSuppBRIJsonArr = CarSearchProcessor.getClientCommercialsBusinessRuleIntakeJSONArray(resClientCommJson);
		Map<String, Map<String,String>> entityToCommHeadMap = new HashMap<String, Map<String,String>>();
		Map<Integer, JSONArray> cCommSuppBRIJsonMap = new HashMap<Integer, JSONArray>();
		Map<Integer, Object> cCommBRICommHeadJsonMap = new HashMap<Integer, Object>();
		Map<String, String> clientCommMap = new HashMap<String, String>();
		Map<String, String> totalPriceInfoMap = new HashMap<String, String>();
		
		Integer briNo = 1;
		for (int i = 0; i < ccommSuppBRIJsonArr.length(); i++) {
			JSONObject ccommSuppBRIJson = ccommSuppBRIJsonArr.getJSONObject(i);
			entityToCommHeadMap = CarSearchProcessor.getClientCommercialsAndTheirTypeFor(ccommSuppBRIJson);
			JSONArray ccommVehDtlsJsonArr = ccommSuppBRIJson.getJSONArray(JSON_PROP_VEHICLEDETAILS);
			cCommSuppBRIJsonMap.put(briNo, ccommVehDtlsJsonArr);
			cCommBRICommHeadJsonMap.put(briNo, entityToCommHeadMap);
			briNo++;
		}
		for(int x=0;x<carRentalInfoJsonArr.length();x++) {
			Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();
			
			JSONArray vehicleAvailJsonArr = carRentalInfoJsonArr.getJSONObject(x).getJSONArray(JSON_PROP_VEHICLEAVAIL);
			for (int i=0; i < vehicleAvailJsonArr.length(); i++) {
				JSONObject vehicleAvailJson = vehicleAvailJsonArr.getJSONObject(i);
				String suppID = vehicleAvailJson.getString(JSON_PROP_SUPPREF);
				briNo = suppResToBRIIndex.get(String.format("%d%c%d", x, KEYSEPERATOR, i));
				JSONObject totalPriceInfoJson = vehicleAvailJson.getJSONObject(JSON_PROP_TOTALPRICEINFO);
				String cyCode = totalPriceInfoJson.getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CCYCODE);
				JSONArray ccommVehDtlsJsonArr = cCommSuppBRIJsonMap.get(briNo);
				entityToCommHeadMap = (Map<String,Map<String,String>>) cCommBRICommHeadJsonMap.get(briNo);
				if (ccommVehDtlsJsonArr == null) {
					logger.info(String.format("BRMS/ClientComm: \"businessRuleIntake\" JSON element for supplier %s not found.", suppID));
					continue;
				}
				//Required for search response,VehicleAvail Objects mapped to Different BRI's as per supplier
				int idx = 0;
				if (suppIndexMap.containsKey(suppID)) {
					idx = suppIndexMap.get(suppID) + 1;
				}
				suppIndexMap.put(suppID, idx);
				JSONObject ccommVehDtlsJson = ccommVehDtlsJsonArr.getJSONObject(idx);
				JSONObject ccommVehPsgrDtlJson = ccommVehDtlsJson.getJSONArray("passengerDetails").getJSONObject(0);
				if (ccommVehPsgrDtlJson == null) {
					continue;
				}
				//Retrieving Entity Commercials from PassengerDtls
				JSONArray clientEntityCommJsonArr = ccommVehPsgrDtlJson.optJSONArray("entityCommercials");
				if (clientEntityCommJsonArr == null) {
					logger.warn("Client commercials calculations not found");
					continue;
				}
				JSONArray entityCommercialInfo = new JSONArray();
				for(int j=0;j<clientEntityCommJsonArr.length();j++) {
					JSONObject clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(j);
					Map<String,String> cCommToTypeMap = entityToCommHeadMap.get(clientEntityCommJson.getString("entityName"));
					JSONObject entityCommJson = new JSONObject(clientEntityCommJson.toString());
					JSONArray addCommDetailsArr = entityCommJson.optJSONArray("additionalCommercialDetails");
					int k;
					if(addCommDetailsArr!=null) {
						for(k=0;k<addCommDetailsArr.length();k++) {
							JSONObject addCommDetailsJson = addCommDetailsArr.getJSONObject(k);
							addCommDetailsJson.remove("commercialCalculationAmount");
							addCommDetailsJson.remove("commercialFareComponent");
							addCommDetailsJson.remove("commercialCalculationPercentage");
							addCommDetailsJson.put(JSON_PROP_COMMTYPE, cCommToTypeMap.get(addCommDetailsJson.getString(JSON_PROP_COMMNAME)));
							if(addCommDetailsJson.optString("commercialCurrency").equals("")) {
								addCommDetailsJson.put("commercialCurrency", cyCode);
							}
						}
					}
					JSONArray fixedCommDetailsArr = entityCommJson.optJSONArray("fixedCommercialDetails");
					if(fixedCommDetailsArr!=null) {
						for(k=0;k<fixedCommDetailsArr.length();k++) {
							JSONObject fixedCommDetailsJson = fixedCommDetailsArr.getJSONObject(k);
							fixedCommDetailsJson.remove("calculationAmount");
							fixedCommDetailsJson.remove("calculationPercentage");
							fixedCommDetailsJson.remove("commercialFareComponent");
							fixedCommDetailsJson.remove("commercialCalculationPercentage");
							fixedCommDetailsJson.put(JSON_PROP_COMMTYPE, cCommToTypeMap.get(fixedCommDetailsJson.getString(JSON_PROP_COMMNAME)));
							if(fixedCommDetailsJson.optString("commercialCurrency").equals("")) {
								fixedCommDetailsJson.put("commercialCurrency", cyCode);
							}
						}
					}
					JSONArray retentionCommDetailsArr = entityCommJson.optJSONArray("retentionCommercialDetails");
					if(retentionCommDetailsArr!=null) {
						for(k=0;k<retentionCommDetailsArr.length();k++) {
							JSONObject retentionCommDetailsJson = retentionCommDetailsArr.getJSONObject(k);
							retentionCommDetailsJson.remove("commercialCalculationAmount");
							retentionCommDetailsJson.remove("commercialFareComponent");
							retentionCommDetailsJson.remove("commercialCalculationPercentage");
							retentionCommDetailsJson.remove("remainingAmount");
							retentionCommDetailsJson.remove("retentionAmountPercentage");
							retentionCommDetailsJson.remove("remainingPercentageAmount");
							retentionCommDetailsJson.remove("retentionPercentage");
							retentionCommDetailsJson.put(JSON_PROP_COMMTYPE, cCommToTypeMap.get(retentionCommDetailsJson.getString(JSON_PROP_COMMNAME)));
							if(retentionCommDetailsJson.optString("commercialCurrency").equals("")) {
								retentionCommDetailsJson.put("commercialCurrency", cyCode);
							}
						}
					}
					JSONObject markUpCommDetailsJson = entityCommJson.optJSONObject("markUpCommercialDetails");
					if(markUpCommDetailsJson!=null) {
							markUpCommDetailsJson.remove("commercialCalculationAmount");
							markUpCommDetailsJson.remove("commercialFareComponent");
							markUpCommDetailsJson.remove("commercialCalculationPercentage");
							markUpCommDetailsJson.remove("fareBreakUp");
							markUpCommDetailsJson.remove("totalFare");
							markUpCommDetailsJson.put(JSON_PROP_COMMTYPE, cCommToTypeMap.get(markUpCommDetailsJson.getString(JSON_PROP_COMMNAME)));
							if(markUpCommDetailsJson.optString("commercialCurrency").equals("")) {
								markUpCommDetailsJson.put("commercialCurrency", cyCode);
							}
					}
					entityCommercialInfo.put(entityCommJson);
				}
				clientCommMap.put(getRedisKeyForVehicleAvail(vehicleAvailJson), entityCommercialInfo.toString());
				totalPriceInfoMap.put(getRedisKeyForVehicleAvail(vehicleAvailJson), totalPriceInfoJson.toString());
			}
		}
		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		
		String redisKey = String.format("%s%c%s%c%s", resHeaderJson.optString(JSON_PROP_SESSIONID), KEYSEPARATOR, PRODUCT_CAR, KEYSEPARATOR, JSON_PROP_CLIENTCOMMINFO);
		redisConn.hmset(redisKey, clientCommMap);
		redisConn.pexpire(redisKey, (long) (CarConfig.getRedisTTLMinutes() * 60 * 1000));
		redisKey = String.format("%s%c%s%c%s", resHeaderJson.optString(JSON_PROP_SESSIONID), KEYSEPARATOR, PRODUCT_CAR, KEYSEPARATOR, JSON_PROP_TOTALPRICEINFO);
		redisConn.hmset(redisKey, totalPriceInfoMap);
		redisConn.pexpire(redisKey, (long) (CarConfig.getRedisTTLMinutes() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);
	}*/
	
	static String getRedisKeyForVehicleAvail(JSONObject vehicleAvailJson) {
		
		List<String> keys = new ArrayList<>();
		String suppId = vehicleAvailJson.optString(JSON_PROP_SUPPREF);
		keys.add(String.format("%s%s", suppId.substring(0,1).toUpperCase(), suppId.substring(1).toLowerCase()));
		keys.add(vehicleAvailJson.optString(JSON_PROP_PICKUPDATE));
		keys.add(vehicleAvailJson.optString(JSON_PROP_PICKUPLOCCODE));
		keys.add(vehicleAvailJson.optString(JSON_PROP_RETURNDATE));
		keys.add(vehicleAvailJson.optString(JSON_PROP_RETURNLOCCODE));
			//Required Only For Indian Suppliers
			keys.add(vehicleAvailJson.optString(JSON_PROP_TRIPTYPE));
		keys.add(vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO).optString(JSON_PROP_VEHMAKEMODELNAME));
		keys.add(vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO).optString(JSON_PROP_VEHMAKEMODELCODE));
		keys.add(vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO).optString(JSON_PROP_VEHICLECATEGORY));
		keys.add(vehicleAvailJson.optString(JSON_PROP_ISPREPAID));
		keys.add(vehicleAvailJson.optString(JSON_PROP_VENDORDIVISION));
		keys.add(vehicleAvailJson.getJSONObject(JSON_PROP_REFERENCE).optString(JSON_PROP_ID));
		
		// TODO : Find if we should add equipments/Coverages in key as same Car with equipments/Coverages will have different prices
		JSONArray equipsArr = vehicleAvailJson.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_SPLEQUIPS).optJSONArray(JSON_PROP_SPLEQUIP);
		JSONArray covrgsArr = vehicleAvailJson.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_PRICEDCOVRGS).optJSONArray(JSON_PROP_PRICEDCOVRG);
		for(int i=0;equipsArr!=null && i<equipsArr.length();i++) {
			JSONObject equipJson = equipsArr.getJSONObject(i);
			keys.add(String.format("%s:%s","equipType", equipJson.optString(JSON_PROP_EQUIPTYPE)));
			keys.add(String.format("%s:%s","quantity", String.valueOf(equipJson.optInt(JSON_PROP_QTY, 1))));
		}
		for(int i=0;covrgsArr!=null && i<covrgsArr.length();i++) {
			JSONObject covrgsJson = covrgsArr.getJSONObject(i);
			keys.add(String.format("%s:%s","coverageType", covrgsJson.optString(JSON_PROP_COVERAGETYPE)));
		}
		String key = keys.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(Character.valueOf(KEYSEPARATOR).toString()));
		return key;
		
		/*StringBuilder strBldr = new StringBuilder(vehicleAvailJson.optString(JSON_PROP_SUPPREF));
		strBldr.append(vehicleAvailJson.optString(JSON_PROP_PICKUPDATE));
		strBldr.append(vehicleAvailJson.optString(JSON_PROP_PICKUPLOCCODE));
		strBldr.append(vehicleAvailJson.optString(JSON_PROP_RETURNDATE));
		strBldr.append(vehicleAvailJson.optString(JSON_PROP_RETURNLOCCODE));
		strBldr.append(vehicleAvailJson.optString(JSON_PROP_ONEWAYINDC));
		strBldr.append(vehicleAvailJson.optString(JSON_PROP_PICKUPDATE));
		strBldr.append(vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO).optString("VehMakeModelName"));
		strBldr.append(vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO).optString("VehMakeModelCode"));
		strBldr.append(vehicleAvailJson.getJSONObject(JSON_PROP_VEHICLEINFO).optString("VehicleCategory"));
		strBldr.append(vehicleAvailJson.getJSONObject(JSON_PROP_REFERENCE).optString("Id"));*/
		
/*		strBldr.append(vehicleAvailJson.optString("IsPrepaid"));
		strBldr.append(vehicleAvailJson.optString("VendorDivision"));*/
		
//		return strBldr.toString();
	}
	
	private static void populateWrapperElement(Element reqElem, Document ownerDoc, Element suppWrapperElem,
			UserContext usrCtx, JSONObject carRentalReq) throws Exception {
	
		
		//TODO : Need To Change later
		Element sourceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehAvailRateRQ/ota:POS/ota:Source");
		sourceElem.setAttribute("ISOCurrency", "EUR");
		sourceElem.setAttribute("ISOCountry", "IE");
		
		String suppID = carRentalReq.getString(JSON_PROP_SUPPREF);
		ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_CAR, suppID);
		if (prodSupplier == null) {
			throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
		}

		Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cari:RequestHeader/com:SupplierCredentialsList");
		Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem, String.format("./com:SupplierCredentials[com:SupplierID = '%s']", suppID));
		if (suppCredsElem == null) {
			suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
		}

		Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./car:SupplierID");
		suppIDElem.setTextContent(suppID);
		
		 Element vehAvailRQCoreElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehAvailRateRQ/ota:VehAvailRQCore");
		 Element vehAvailRQInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_VehAvailRateRQ/ota:VehAvailRQInfo");
//		         JSONObject vehRentalObj = vehAvailRQCore.getJSONObject(JSON_PROP_VEHRENTAL);
		
		 Element vehRentalElem =  CarSearchProcessor.getVehRentalCoreElement(ownerDoc, carRentalReq);
		 vehAvailRQCoreElem.appendChild(vehRentalElem);
		 
		 if(carRentalReq.optString("vendorPrefCode")!=null && !carRentalReq.optString("vendorPrefCode").equals("")) {
			 Element vendorPrefsElem = CarSearchProcessor.getVendorPrefsElement(ownerDoc, carRentalReq);
			 vehAvailRQCoreElem.appendChild(vendorPrefsElem);
		 }
		  
		 Element VehPrefsElem =  ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehPrefs");
		 Element VehPrefElem =  ownerDoc.createElementNS(Constants.NS_OTA, "ota:VehPref");
		 VehPrefElem.setAttribute("CodeContext", carRentalReq.getString(JSON_PROP_CODECONTEXT));
		 VehPrefsElem.appendChild(VehPrefElem);
		 vehAvailRQCoreElem.appendChild(VehPrefsElem);
		 
		 Element driverAge = ownerDoc.createElementNS(Constants.NS_OTA, "ota:DriverType");
		 driverAge.setAttribute("Age", CARRENTAL_DRIVER_AGE);
		 vehAvailRQCoreElem.appendChild(driverAge);
		 
		 Element specialEquipsElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPrefs");
		 JSONArray specialEquipsArr = carRentalReq.optJSONArray(JSON_PROP_SPLEQUIPS);
		 if(specialEquipsArr!=null) {
			 for(int i=0;i<specialEquipsArr.length();i++) {
				 JSONObject specialEquipJson = specialEquipsArr.optJSONObject(i);
				 Element specialEquipElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:SpecialEquipPref");
				 //TODO: Better Way to handle this
				 specialEquipElem.setAttribute("Quantity", String.valueOf(specialEquipJson.optInt(JSON_PROP_QTY, 1)));
				 specialEquipElem.setAttribute("EquipType", specialEquipJson.getString("equipType"));
				 specialEquipsElem.appendChild(specialEquipElem);
			 }
		 }
		 vehAvailRQCoreElem.appendChild(specialEquipsElem);
		 
         JSONArray customerJsonArr = carRentalReq.optJSONArray(JSON_PROP_PAXDETAILS);
         if(customerJsonArr!=null && customerJsonArr.length()!=0) {
       	  	Element customerElem = CarSearchProcessor.populateCustomerElement(ownerDoc, customerJsonArr);
       	  	vehAvailRQInfoElem.appendChild(customerElem);
       	 }
         
         JSONArray pricedCovrgsArr = carRentalReq.optJSONArray(JSON_PROP_PRICEDCOVRGS);
         if(pricedCovrgsArr!=null && pricedCovrgsArr.length()!=0) {
        	 Element pricedCovrgsElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CoveragePrefs");
        	 for(int i=0;i<pricedCovrgsArr.length();i++) {
				 JSONObject pricedCovrgJson = pricedCovrgsArr.optJSONObject(i);
				 Element pricedCovrgElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CoveragePref");
				 //TODO: Better Way to handle this
				 pricedCovrgElem.setAttribute("CoverageType", pricedCovrgJson.getString(JSON_PROP_COVERAGETYPE));
				 pricedCovrgsElem.appendChild(pricedCovrgElem);
			 }
        	 vehAvailRQInfoElem.appendChild(pricedCovrgsElem);
         }
	} 	  
}
	

