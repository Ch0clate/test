package com.coxandkings.travel.bookingengine.orchestrator.bus;

//import java.io.File;
//import java.io.PrintWriter;
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

import com.coxandkings.travel.bookingengine.config.bus.BusConfig;
import com.coxandkings.travel.bookingengine.orchestrator.bus.SupplierCommercials;
import com.coxandkings.travel.bookingengine.orchestrator.bus.ClientCommercials;

import com.coxandkings.travel.bookingengine.orchestrator.bus.BusConstants;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;

import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class BusSearchProcessor implements BusConstants {

	private static final Logger logger = LogManager.getLogger(BusSearchProcessor.class);
	public static String process(JSONObject reqJson) {
		try {
			OperationConfig opConfig = BusConfig.getOperationConfig("search");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true); // xmshell
			Document ownerDoc = reqElem.getOwnerDocument();

			TrackingContext.setTrackingContext(reqJson);

			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct("Transportation","Bus");

			createHeader(reqElem, sessionID, transactionID, userID);
//			createHeader(reqHdrJson,reqElem);
			
			int sequence = 1;
			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./bus:RequestHeader/com:SupplierCredentialsList");
			for (ProductSupplier prodSupplier : prodSuppliers) {
				suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc,sequence++));
			}

			
			XMLUtils.setValueAtXPath(reqElem, "./busi:RequestBody/ota:OTA_VehAvailRateRQ2/ota:sourceStationId",
					reqBodyJson.getString(JSON_PROP_SOURCE));
			XMLUtils.setValueAtXPath(reqElem, "./busi:RequestBody/ota:OTA_VehAvailRateRQ2/ota:destinationStationId",
					reqBodyJson.getString(JSON_PROP_DESTINATION));
			XMLUtils.setValueAtXPath(reqElem, "./busi:RequestBody/ota:OTA_VehAvailRateRQ2/ota:journeyDate",
					reqBodyJson.getString(JSON_PROP_JOURNEYDATE));

			// response
//			System.out.println(XMLTransformer.toString(reqElem));
//			PrintWriter pw = new PrintWriter(new File("D:\\BE\\temp\\si_xml.txt"));
//			pw.write(XMLTransformer.toString(reqElem));
//			pw.close();
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					BusConfig.getHttpHeaders(), reqElem);
//			System.out.println(XMLTransformer.toString(resElem));
			if (resElem == null) {
				logger.info("Null response received from SI ");
				throw new Exception("Null response received from SI");
			}
			
			JSONObject resBodyJson = new JSONObject();
			JSONObject availJson = new JSONObject();
			JSONArray availabilityJsonArr = new JSONArray();
			JSONArray serviceArr = new JSONArray();
			Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem,
					"./busi:ResponseBody/bus:OTA_VehAvailRateRS2Wrapper");
			for (Element wrapperElem : wrapperElems) {
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_VehAvailRateRS2");
				getSupplierResponseAvailableTripsJSON(resBodyElem, serviceArr,reqBodyJson);
			}
			availJson.put(JSON_PROP_SERVICE, serviceArr);
			availabilityJsonArr.put(availJson);
			resBodyJson.put(JSON_PROP_AVAILABILITY, availabilityJsonArr);
//			.out.println(resBodyJson.toString());
//			PrintWriter pw1 = new PrintWriter(new File("D:\\BE\\temp\\si_json.json"));
//			pw1.write(resBodyJson.toString());
//			pw1.close();
			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			// logger.trace(String.format("SI Transformed JSON Response = %s",
			// resJson.toString()));
			Map<String,Integer> BRMS2SIBusMap = new HashMap<String,Integer>();
			JSONObject resSupplierJson = SupplierCommercials.getSupplierCommercials(reqJson, resJson,BRMS2SIBusMap);
			if (BRMS_STATUS_TYPE_FAILURE.equals(resSupplierJson.getString(JSON_PROP_TYPE))) {
            	logger.error(String.format("A failure response was received from Supplier Commercials calculation engine: %s", resSupplierJson.toString()));
            	return getEmptyResponse(reqHdrJson).toString();
            }
//			PrintWriter pw2 = new PrintWriter(new File("D:\\BE\\temp\\suppJson.txt"));
//			pw2.write(resSupplierJson.toString());
//			pw2.close();
			
			JSONObject resClientJson = ClientCommercials.getClientCommercials(resSupplierJson);
			if (BRMS_STATUS_TYPE_FAILURE.equals(resClientJson.getString(JSON_PROP_TYPE))) {
            	logger.error(String.format("A failure response was received from Client Commercials calculation engine: %s", resClientJson.toString()));
            	return getEmptyResponse(reqHdrJson).toString();
            }
//			PrintWriter pw3 = new PrintWriter(new File("D:\\BE\\temp\\clientJson.txt"));
//			pw3.write(resClientJson.toString());
//			pw3.close();
//			.out.println(resClientJson.toString());
			
			calculatePrices(reqJson, resJson, resClientJson, BRMS2SIBusMap,false);
//			PrintWriter pw4 = new PrintWriter(new File("D:\\BE\\temp\\final_json.json"));
//			pw4.write(resBodyJson.toString());
//			pw4.close();
			
			return resJson.toString();

		} catch (Exception e) {
			logger.error("Exception received while processing", e);
			e.printStackTrace();
			return null;
		}
	}

	private static Object getEmptyResponse(JSONObject reqHdrJson) {
		JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, new JSONObject());
        return resJson;
	}

	public static void calculatePrices(JSONObject reqJson, JSONObject resJson, JSONObject resClientJson,
			Map<String,Integer> BRMS2SIBusMap,boolean retainSuppFares) {

		JSONArray entityCommJsonArr = new JSONArray();
		JSONArray passDetailsArr = new JSONArray();
		JSONArray busServiceArr = new JSONArray();
		JSONArray serviceArr = new JSONArray();
		JSONArray availArr = new JSONArray();
		JSONArray fareArr = new JSONArray();

		Map<String, String> commToTypeMap = getClientCommercialsAndTheirType(resClientJson);
		
		Map<String,Integer> suppIndexMap = new HashMap<String,Integer>();
		
		 String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
	     String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
	    
	     JSONArray ccommSuppBRIJsonArr = resClientJson.getJSONObject(JSON_PROP_RESULT)
					.getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0)
					.getJSONObject("value")
					.getJSONObject(JSON_PROP_CLIENTTRANRULES)
					.getJSONArray(JSON_PROP_BUSSRULEINTAKE);
	     
	     Map<Integer, JSONArray> ccommSuppBRIJsonMap = new HashMap<Integer, JSONArray>();
	     Integer briNo = 1;
	     for (int m = 0; m < ccommSuppBRIJsonArr.length(); m++)
			{
				busServiceArr = ccommSuppBRIJsonArr.getJSONObject(m).getJSONArray(JSON_PROP_BUSSERVICEDETAILS);
				 ccommSuppBRIJsonMap.put(briNo, busServiceArr);
             briNo++;
			}
		availArr = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_AVAILABILITY);
        
		for (int p = 0; p < availArr.length(); p++) {
		
			serviceArr = availArr.getJSONObject(p).optJSONArray(JSON_PROP_SERVICE);
			if(serviceArr!=null) {
			for (int q = 0; q < serviceArr.length(); q++) {
				
				JSONArray suppFareArr = new JSONArray();
				fareArr = serviceArr.getJSONObject(q).getJSONArray(JSON_PROP_FARESARRAY);
				BigDecimal suppFare = new BigDecimal(0);
				JSONObject serviceJson = serviceArr.getJSONObject(q);
				String suppId = serviceJson.getString(JSON_PROP_SUPPREF);
				
				for (int r = 0; r < fareArr.length(); r++) {
					BigDecimal totalFareAmt = new BigDecimal(0);
					briNo = BRMS2SIBusMap.get(String.format("%d%c%d%c%d", p, '|', q,'|', r));
					JSONArray ccommBusDtlsJsonArr = ccommSuppBRIJsonMap.get(briNo);
					if (ccommBusDtlsJsonArr == null) {
						logger.info(String.format(
                                "BRMS/ClientComm: \"businessRuleIntake\" JSON element for supplier %s not found.", suppId));
						continue;
					}
					// Required for search response,busdetails Objects mapped to Different BRI's
                    // as per supplier
					int idx = 0;
                    if (suppIndexMap.containsKey(briNo)) {
                                    idx = suppIndexMap.get(briNo) + 1;
                    }
                    suppIndexMap.put(suppId, idx);
                    JSONObject busDtlsJSon = ccommBusDtlsJsonArr.getJSONObject(idx);
                    
//					for (int m = 0; m < ccommSuppBRIJsonArr.length(); m++) {
						
						
//						JSONObject busDtlsJSon = BRMS2SIBusDetailsMap.get(String.format("%d%c%d%c%d",p,'|',q,'|',r));
//						busServiceArr = busDtlsJSon.getJSONArray(JSON_PROP_BUSSERVICEDETAILS);
//						for (int i = 0; i < busServiceArr.length(); i++) {
							passDetailsArr = busDtlsJSon.getJSONArray(JSON_PROP_PASSANGERDETAILS);

							
							// Assumptions - only one fare present in SI response. not able to handle
							// multiple fares because identification of these fares is not possible.
							
							//TODO: supplierfare added?? or replaced by commercial amts from addition and markup
//							totalFareAmt = serviceArr.getJSONObject(q).getJSONArray(JSON_PROP_FARESARRAY).getJSONObject(r)
//									.getBigDecimal("fare");

							for (int j = 0; j < passDetailsArr.length(); j++) {
								entityCommJsonArr = passDetailsArr.getJSONObject(j).getJSONArray(JSON_PROP_ENTITYCOMMS);
								for (int k = entityCommJsonArr.length() - 1; k >= 0; k--) {
									JSONObject clientEntityCommJson = entityCommJsonArr.getJSONObject(k);
									JSONArray additionalCommArr = clientEntityCommJson.getJSONArray(JSON_PROP_ADDITIONCOMMDETAILS);
									for(int x=0;x<additionalCommArr.length();x++)
									{
										JSONObject additionalCommsJson = additionalCommArr.getJSONObject(x);
			    						String additionalCommName = additionalCommsJson.optString(JSON_PROP_COMMNAME);//fetch comm	Name from additionalcommercialDetails object
			    						if (COMM_TYPE_RECEIVABLE.equals(commToTypeMap.get(additionalCommName))) {//is the additionalCommName receivable?
			    							String additionalCommCcy = additionalCommsJson.getString(JSON_PROP_COMMCCY);//get comm currency from additionalcommercialDetails Object
			    							BigDecimal additionalCommAmt = additionalCommsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
			    							totalFareAmt = totalFareAmt.add(additionalCommAmt);
			    							
			    						}
										
									}
									JSONObject markupCalcJson = clientEntityCommJson
											.optJSONObject(JSON_PROP_MARKUPCOMMERCIALS);
									if (markupCalcJson == null) {
										continue;
									}
                                    
									totalFareAmt = totalFareAmt.add(markupCalcJson.getBigDecimal("totalFare"));
								}

								
								suppFare = fareArr.getJSONObject(r).getBigDecimal("fare");
								fareArr.getJSONObject(r).put("fare", totalFareAmt);
							}

							
//						}
//					}
					
					if (retainSuppFares) {
						
						JSONObject suppFaresJson = new JSONObject();
						JSONObject seatFare = fareArr.getJSONObject(r);
						suppFaresJson.put(JSON_PROP_SEATNO, seatFare.get(JSON_PROP_SEATNO));
						suppFaresJson.put("fare", suppFare);
						suppFaresJson.put(JSON_PROP_CURRENCY, seatFare.getString(JSON_PROP_CURRENCY));
						suppFareArr.put(suppFaresJson);
						serviceArr.getJSONObject(q).put(JSON_PROP_SUPPLIERFARES, suppFareArr);
					}
				}

			}
		}
		}
//TODO:
		// StringBuilder key = new StringBuilder();
		// //key for repeating element
		// for(int i=0;i<serviceArr.length();i++)
		// {
		// key.append("routeId");
		// key.append("operatorId");
		// }

	}

	private static Map<String, String> getClientCommercialsAndTheirType(JSONObject resClientJson) {
		JSONArray commHeadJsonArr = null;
		 JSONArray entDetaiJsonArray= null;
	        JSONObject commHeadJson = null;
	        JSONObject scommBRIJson = null;
	        Map<String, String> commToTypeMap = new HashMap<String, String>();
	        JSONArray scommBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(resClientJson);
	        
	        for (int i=0; i < scommBRIJsonArr.length(); i++) {
	        	scommBRIJson = scommBRIJsonArr.getJSONObject(i);
	         entDetaiJsonArray = scommBRIJson.getJSONArray(JSON_PROP_ENTITYDETAILS);
	        	for(int j=0;j<entDetaiJsonArray.length();j++)
	        	{
	        		commHeadJsonArr=entDetaiJsonArray.getJSONObject(j).getJSONArray(JSON_PROP_COMMHEAD);
	        	if (commHeadJsonArr == null) {
	        		logger.warn("No commercial heads found in supplier commercials");
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

	private static void getSupplierResponseAvailableTripsJSON(Element resBodyElem, JSONArray serviceArr,JSONObject reqBodyJson) {

		getSupplierResponseAvailableTripsJSON(reqBodyJson,resBodyElem, serviceArr, false, 0);

	}

	private static void getSupplierResponseAvailableTripsJSON(JSONObject reqBodyJson,Element resBodyElem, JSONArray serviceArr,
			boolean generateBookRefIdx, int bookRefIdx) {
		

		
		Element[] serviceElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:availableTrips/ota:Service");

		for (Element serviceElem : serviceElems) {
			JSONObject serviceJson = getServiceJSON(resBodyElem, serviceElem);
			serviceJson.put(JSON_PROP_SUPPREF,XMLUtils.getValueAtXPath((Element) resBodyElem.getParentNode(), "./bus:SupplierID"));
			serviceJson.put(JSON_PROP_SOURCE, reqBodyJson.get(JSON_PROP_SOURCE));
			serviceJson.put(JSON_PROP_DESTINATION, reqBodyJson.get(JSON_PROP_DESTINATION));
			serviceJson.put(JSON_PROP_JOURNEYDATE, reqBodyJson.get(JSON_PROP_JOURNEYDATE));
			serviceArr.put(serviceJson);
//			serviceArr.put(seviceJson);
//			if (generateBookRefIdx) {
//				seviceJson.put(JSON_PROP_BOOKREFIDX, bookRefIdx);
//			}
		}
		

	}

	private static JSONObject getServiceJSON(Element resBodyElem, Element serviceElem) {
		
		JSONObject serviceJson = new JSONObject();
		serviceJson.put(JSON_PROP_SUPPREF,XMLUtils.getValueAtXPath((Element) resBodyElem.getParentNode(), "./bus:SupplierID"));
		serviceJson.put("AC", XMLUtils.getValueAtXPath(serviceElem, "./AC"));

		serviceJson.put("arrivalTime", XMLUtils.getValueAtXPath(serviceElem, "./ota:arrivalTime"));

		serviceJson.put("availableSeats", XMLUtils.getValueAtXPath(serviceElem, "./ota:availableSeats"));
		Element boardingTimeElems[] = XMLUtils.getElementsAtXPath(serviceElem, "./ota:boardingTimes");
		JSONArray boardingDetailsArr = new JSONArray();
		for (Element boardingElem : boardingTimeElems) {
			JSONObject getBoardingJson = new JSONObject();

			getBoardingJson = getDetailsJson(boardingElem);
			if (getBoardingJson == null)
				serviceJson.put("boardingTimes", "");
			else {
				boardingDetailsArr.put(getBoardingJson);

			}
		}
		serviceJson.put("boardingTimes", boardingDetailsArr);
		Element droppingTimesElems[] = XMLUtils.getElementsAtXPath(serviceElem, "./ota:droppingTimes");
		JSONArray droppingDetailsArr = new JSONArray();
		for (Element droppingTimeElem : droppingTimesElems) {
			JSONObject getdroppingJson = new JSONObject();

			getdroppingJson = getDetailsJson(droppingTimeElem);
			if (getdroppingJson == null)
				serviceJson.put("droppingTimes", "");
			else {
				droppingDetailsArr.put(getdroppingJson);
			}
		}
		serviceJson.put("droppingTimes", droppingDetailsArr);
		
		//TODO: how to handle this cancellation policy 

		serviceJson.put("cancellationPolicy", XMLUtils.getValueAtXPath(serviceElem, "./ota:cancellationPolicy"));

		serviceJson.put("departureTime", XMLUtils.getValueAtXPath(serviceElem, "./ota:departureTime"));

		serviceJson.put("doj", XMLUtils.getValueAtXPath(serviceElem, "./ota:doj"));

		serviceJson.put(JSON_PROP_OPERATORID, XMLUtils.getValueAtXPath(serviceElem, "./ota:operatorId"));
		
		serviceJson.put(JSON_PROP_SERVICEID, XMLUtils.getValueAtXPath(serviceElem, "./ota:ServiceId"));
		// serviceJson.put("fares", getFaresArray(serviceElem));

		Element faressElems[] = XMLUtils.getElementsAtXPath(serviceElem, "./ota:fares/ota:fare");
		for (Element faresElem : faressElems) {
			JSONArray fareArr = new JSONArray();
			getFaresArray(resBodyElem, serviceElem, faresElem,fareArr);
			serviceJson.put(JSON_PROP_FARESARRAY, fareArr);

		}
		
		serviceJson.put("serviceTaxAbsolute", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:serviceTaxAbsolute"), 0));
		serviceJson.put("baseFare", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:baseFare"), 0));
		serviceJson.put("markupFareAbsolute", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:markupFareAbsolute"), 0));
		serviceJson.put("operatorServiceChargePercentage", XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:operatorServiceChargePercentage"));
		serviceJson.put("totalFare", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:totalFare"), 0));
		serviceJson.put("markupFarePercentage", XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:markupFarePercentage"));
		serviceJson.put("operatorServiceChargeAbsolute", Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:operatorServiceChargeAbsolute"), 0));
		serviceJson.put("serviceTaxPercentage", XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:serviceTaxPercentage"));
		serviceJson.put("currency", XMLUtils.getValueAtXPath(serviceElem, "./ota:fareDetails/ota:currency"));

		serviceJson.put("serviceId", XMLUtils.getValueAtXPath(serviceElem, "./ota:ServiceId"));

		
		return serviceJson;
	}

	private static void getFaresArray(Element resBodyElem, Element serviceElem, Element faresElem,JSONArray fareArr) {


		JSONObject fare = new JSONObject();
		
		fare.put("fare", new BigDecimal(faresElem.getTextContent()));
		fare.put(JSON_PROP_SERVICEID, XMLUtils.getValueAtXPath(serviceElem, "./ota:ServiceId"));
		fare.put("currencyCode", XMLUtils.getValueAtXPath(faresElem, "./@currency"));
		fare.put("routeId", XMLUtils.getValueAtXPath(serviceElem, "./ota:id"));
		fare.put("mTicketEnabled", XMLUtils.getValueAtXPath(serviceElem, "./ota:mTicketEnabled"));
		fare.put("nonAC", XMLUtils.getValueAtXPath(serviceElem, "./ota:nonAC"));
		fare.put("tatkalTime", XMLUtils.getValueAtXPath(serviceElem, "./ota:tatkalTime"));
		fare.put(JSON_PROP_OPERATORID, XMLUtils.getValueAtXPath(serviceElem, "./ota:operatorId"));
		fare.put("operatorName", XMLUtils.getValueAtXPath(serviceElem, "./ota:operatorName"));
		fare.put("busLabel", XMLUtils.getValueAtXPath(serviceElem, "./ota:busLabel"));
		fare.put("busType", XMLUtils.getValueAtXPath(serviceElem, "./ota:busType"));
		fare.put("busTypeId", XMLUtils.getValueAtXPath(serviceElem, "./ota:busTypeId"));
		fare.put("busNumber", XMLUtils.getValueAtXPath(serviceElem, "./ota:busNumber"));
		fare.put("serviceNumber", XMLUtils.getValueAtXPath(serviceElem, "./ota:ServiceNumber"));
		fare.put("travelTime", XMLUtils.getValueAtXPath(serviceElem, "./ota:TravelTime"));
		fare.put("amenities", XMLUtils.getValueAtXPath(serviceElem, "./ota:Amenities"));
		fare.put("isRtc", XMLUtils.getValueAtXPath(serviceElem, "./ota:isRtc"));
		fare.put("routeId", XMLUtils.getValueAtXPath(serviceElem, "./ota:routeId"));
		fare.put("seater", XMLUtils.getValueAtXPath(serviceElem, "./ota:Seater"));
		fare.put("seaterFareNAC", XMLUtils.getValueAtXPath(serviceElem, "./ota:SeaterFareNAC"));
		fare.put("seaterFareAC", XMLUtils.getValueAtXPath(serviceElem, "./ota:SeaterFareAC"));
		fare.put("partialCancellationAllowed",
				Boolean.valueOf(XMLUtils.getValueAtXPath(serviceElem, "./ota:partialCancellationAllowed")));
		fare.put("sleeper", XMLUtils.getValueAtXPath(serviceElem, "./ota:sleeper"));
		fare.put("sleeperFareNAC", XMLUtils.getValueAtXPath(serviceElem, "./ota:SleeperFareNAC"));
		fare.put("sleeperFareAC", XMLUtils.getValueAtXPath(serviceElem, "./ota:SleeperFareAC"));
		fare.put(JSON_PROP_LAYOUTID, XMLUtils.getValueAtXPath(serviceElem, "./ota:layOutId"));
		fare.put("commPCT", XMLUtils.getValueAtXPath(serviceElem, "./ota:CommPCT"));
		fare.put("commAmount", XMLUtils.getValueAtXPath(serviceElem, "./ota:CommAmount"));
		fare.put("routeCode", XMLUtils.getValueAtXPath(serviceElem, "./ota:RouteCode"));
		fare.put("vehicleType", XMLUtils.getValueAtXPath(serviceElem, "./ota:vehicleType"));
		
		fareArr.put(fare);

	}

	

	private static JSONObject getDetailsJson(Element detailElem) {
		JSONObject getDetailsJson = new JSONObject();

		getDetailsJson.put("Id", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:bpId"));

		getDetailsJson.put("Name", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:bpName"));

		getDetailsJson.put("location", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:location"));

		getDetailsJson.put("prime", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:prime"));

		getDetailsJson.put("time", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:time"));

		getDetailsJson.put("address", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:address"));

		getDetailsJson.put("landMark", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:landMark"));

		getDetailsJson.put("Telephone", XMLUtils.getValueAtXPath(detailElem, "./ota:Details/ota:Telephone"));
		return getDetailsJson;
	}

	protected static void createHeader(Element reqElem, String sessionID, String transactionID, String userID) {
		

		Element sessionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./bus:RequestHeader/com:SessionID");
		sessionElem.setTextContent(sessionID);

		Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./bus:RequestHeader/com:TransactionID");
		transactionElem.setTextContent(transactionID);

		Element userElem = XMLUtils.getFirstElementAtXPath(reqElem, "./bus:RequestHeader/com:UserID");
		userElem.setTextContent(userID);

	}
	
	public static void createHeader(JSONObject reqHdrJson, Element reqElem) {
		Element sessionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SessionID");
		sessionElem.setTextContent(reqHdrJson.getString(JSON_PROP_SESSIONID));

		Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:TransactionID");
		transactionElem.setTextContent(reqHdrJson.getString(JSON_PROP_TRANSACTID));

		Element userElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:UserID");
		userElem.setTextContent(reqHdrJson.getString(JSON_PROP_USERID));
	}
}
