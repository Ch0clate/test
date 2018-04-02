package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.TraversableResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.ServletContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class AirRepriceProcessor implements AirConstants {

	private static final Logger logger = LogManager.getLogger(AirRepriceProcessor.class);

	public static Element getAirTravelerAvailElement(Document ownerDoc, JSONObject traveller, int travellerIdx) {
		Element travellerElem = ownerDoc.createElementNS(NS_OTA, "ota:AirTravelerAvail");
		Element psgrElem = ownerDoc.createElementNS(NS_OTA, "ota:PassengerTypeQuantity");

		// TODO: Would paxType be ADT, CHD, INF in incoming request?
		psgrElem.setAttribute("Code", traveller.getString(JSON_PROP_PAXTYPE));
		//psgrElem.setAttribute("Quantity", String.valueOf(traveller.getInt("quantity")));
		psgrElem.setAttribute("Quantity", "1");
		
		travellerElem.appendChild(psgrElem);

		// TODO: Need to SSSR element in the request
		Element airTravlerElem = ownerDoc.createElementNS(NS_OTA, "ota:AirTraveler");
		
		airTravlerElem.setAttribute("BirthDate", traveller.optString(JSON_PROP_DATEOFBIRTH, ""));
		airTravlerElem.setAttribute("PassengerTypeCode", traveller.optString(JSON_PROP_PAXTYPE,""));
		airTravlerElem.setAttribute("Gender", traveller.optString(JSON_PROP_GENDER,""));

		travellerElem.appendChild(airTravlerElem);

		Element personNameElem = ownerDoc.createElementNS(NS_OTA, "ota:PersonName");
		airTravlerElem.appendChild(personNameElem);

		Element namePrefixElem = ownerDoc.createElementNS(NS_OTA, "ota:NamePrefix");
		namePrefixElem.setTextContent(traveller.optString(JSON_PROP_TITLE,""));
		
		personNameElem.appendChild(namePrefixElem);

		Element givenNameElem = ownerDoc.createElementNS(NS_OTA, "ota:GivenName");
		givenNameElem.setTextContent(traveller.optString(JSON_PROP_FIRSTNAME,""));
		
		personNameElem.appendChild(givenNameElem);

		Element surnameElem = ownerDoc.createElementNS(NS_OTA, "ota:Surname");
		surnameElem.setTextContent(traveller.optString(JSON_PROP_SURNAME,""));
		
		personNameElem.appendChild(surnameElem);

		Element travellerRefNoElem = ownerDoc.createElementNS(NS_OTA, "ota:TravelerRefNumber");
		travellerRefNoElem.setAttribute("RPH", String.valueOf(travellerIdx));
		airTravlerElem.appendChild(travellerRefNoElem);

		return travellerElem;
	}

	public static String process(JSONObject reqJson) {
		 JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
		  JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
		
		try {
			TrackingContext.setTrackingContext(reqJson);
			//AirPriceProcessor.loadConfig();
			OperationConfig opConfig = AirConfig.getOperationConfig(TrackingContext.getTrackingContext().getTrackingParameter(ServletContext.RESTPROP_OPERATION));
			//Element reqElem = (Element) mXMLPriceShellElem.cloneNode(true);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			//logger.info(String.format("Read Reprice Verify XML request template: %s\n", XMLTransformer.toEscapedString(reqElem)));
			Document ownerDoc = reqElem.getOwnerDocument();
			
			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_AirPriceRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			/*
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);*/

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			AirSearchProcessor.createHeader(reqHdrJson, reqElem);
			
			String prevSuppID = "";
			JSONArray pricedItinsJSONArr = reqBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
			for (int y=0; y < pricedItinsJSONArr.length(); y++) {
				Map<String,String> flightMap=new HashMap<String,String>();
				JSONObject pricedItinJson = pricedItinsJSONArr.getJSONObject(y);
			 
				
				String suppID = pricedItinJson.getString(JSON_PROP_SUPPREF);
				Element suppWrapperElem = null;
				Element otaReqElem = null;
				Element travelerInfoElem = null;
				Element priceInfoElem = null;
				if (suppID.equals(prevSuppID)) {
					suppWrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, String.format("./air:OTA_AirPriceRQWrapper[air:SupplierID = '%s']", suppID));
					travelerInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirPriceRQ/ota:TravelerInfoSummary");
					otaReqElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirPriceRQ");
					priceInfoElem = XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:PriceRequestInformation");
				}
				else {
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

					Element sequenceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./air:Sequence");
					sequenceElem.setTextContent(String.valueOf(y));
					
					travelerInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirPriceRQ/ota:TravelerInfoSummary");
					otaReqElem = (Element) travelerInfoElem.getParentNode();

					priceInfoElem = XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:PriceRequestInformation");
					Element tpExElem = ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
					
					// TODO: The following Reprice Flow Indicators logic will be implemented after Standardization.
					Element repriceFlowIndElem = ownerDoc.createElementNS(NS_AIR, "air:RepriceFlowIndicators");
					//Keep this sellIndicatorElem false if getSSR called before repice.
					Element sellIndicatorElem = ownerDoc.createElementNS(NS_AIR, "air:SellIndicator");
					sellIndicatorElem.setTextContent("false");
					repriceFlowIndElem.appendChild(sellIndicatorElem);
					
					Element updateContactsElem = ownerDoc.createElementNS(NS_AIR, "air:UpdateContacts");
					updateContactsElem.setTextContent("true");
					repriceFlowIndElem.appendChild(updateContactsElem);
					//Keep this sellIndicatorElem false is no SSR are passed in the Request(to be done after standardization).
					Element sellBySSRIndicatorElem = ownerDoc.createElementNS(NS_AIR, "air:SellBySSRIndicator");
					sellBySSRIndicatorElem.setTextContent("false");
					repriceFlowIndElem.appendChild(sellBySSRIndicatorElem);
					
					Element assignSeatsElem = ownerDoc.createElementNS(NS_AIR, "air:AssignSeats");
					assignSeatsElem.setTextContent("false");
					repriceFlowIndElem.appendChild(assignSeatsElem);
					
					Element updatePassengerElem = ownerDoc.createElementNS(NS_AIR, "air:UpdatePassenger");
					updatePassengerElem.setTextContent("true");
					
					
					repriceFlowIndElem.appendChild(updatePassengerElem);
					
					
					tpExElem.appendChild(repriceFlowIndElem);
					
					priceInfoElem.appendChild(tpExElem);
					
					JSONArray travellerArr = reqBodyJson.getJSONArray(JSON_PROP_PAXDETAILS);
					for (int i=0; i < travellerArr.length(); i++) {
						JSONObject traveller = (JSONObject) travellerArr.get(i);
						Element travellerElem = getAirTravelerAvailElement(ownerDoc, traveller, i);
						travelerInfoElem.insertBefore(travellerElem, priceInfoElem);
					}	
					
					/*if (suppID.equals(prevSuppID) == false) {
						JSONArray paxDetailsJsonArr = reqBodyJson.getJSONArray("paxDetails");
						for (int i=0; i < paxDetailsJsonArr.length(); i++) {
							JSONObject traveller = (JSONObject) paxDetailsJsonArr.get(i);
							Element travellerElem = getAirTravelerAvailElement(ownerDoc, traveller, i);
							travelerInfoElem.insertBefore(travellerElem, priceInfoElem);
							
							//getSSR for passengers
							if(!traveller.isNull("specialRequests"))
							{
								createSSRElem(ownerDoc, tpExElem, i, traveller);
							}
							
								
						}
							
							
						}*/
					
				
				}
				Element odosElem = null;
				if (suppID.equals(prevSuppID)) {
					odosElem = XMLUtils.getFirstElementAtXPath(otaReqElem, "./ota:AirItinerary/ota:OriginDestinationOptions");
					if (odosElem == null) {
						logger.warn(String.format("XML element for ota:OriginDestinationOptions not found for supplier %s", suppID));
					}
				}
				else {
					Element airItinElem = ownerDoc.createElementNS(NS_OTA, "ota:AirItinerary");
					airItinElem.setAttribute("DirectionInd", reqBodyJson.getString(JSON_PROP_TRIPTYPE));
					odosElem = ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOptions");
					airItinElem.appendChild(odosElem);
					otaReqElem.insertBefore(airItinElem, travelerInfoElem);
				}				
				
				flightMap=createOriginDestinationOptions(ownerDoc, pricedItinJson, odosElem);
				//TODO:complete SSR OP once standardization comes.
				//Comment below for loop for if not using SSR
				Element tpExtensionElem=XMLUtils.getFirstElementAtXPath(travelerInfoElem,"./ota:PriceRequestInformation/ota:TPA_Extensions");
				JSONArray paxDetailsJsonArr = reqBodyJson.getJSONArray(JSON_PROP_PAXDETAILS);
				Element sellBySSRElem = XMLUtils.getFirstElementAtXPath(tpExtensionElem, "./air:RepriceFlowIndicators/air:SellBySSRIndicator");
				for (int i=0; i < paxDetailsJsonArr.length(); i++) {
					JSONObject traveller = (JSONObject) paxDetailsJsonArr.get(i);
					
					//getSSR for passengers
					if(!traveller.isNull(JSON_PROP_SPECIALREQS))
					{
						sellBySSRElem.setTextContent("true");
						createSSRElem(ownerDoc, tpExtensionElem, i, traveller,flightMap);
					}
				}
		
				AirPriceProcessor.createTPA_Extensions(reqBodyJson, travelerInfoElem);
				prevSuppID = suppID;
			}
			
			//System.out.println("ReqElem->"+XMLTransformer.toString(reqElem));
            Element resElem = null;
            resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
            if (resElem == null) {
            	throw new Exception("Null response received from SI");
            }
       
            int idx = 0;
            JSONObject resBodyJson = new JSONObject();
            JSONArray bookRefsJsonArr = new JSONArray();
            JSONArray pricedItinsJsonArr = new JSONArray();
            Element[] resWrapperElems = AirPriceProcessor.sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirPriceRSWrapper"));
            Map<String,String> redisPtcPriceItinMap=new HashMap<String,String>();
            for (Element resWrapperElem : resWrapperElems) {
            	Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_AirPriceRS");
            	
            	getSupplierResponsePricedItinerariesJSON(resBodyElem, pricedItinsJsonArr, true, idx++,ownerDoc,redisPtcPriceItinMap);  
            	JSONObject bookRefJson = new JSONObject();
                JSONObject suppBookPriceJson = new JSONObject();
                suppBookPriceJson.put(JSON_PROP_AMOUNT, Utils.convertToBigDecimal(resBodyElem.getAttribute("ItinTotalPrice"), 0));
                // TODO: How should currencyCode for this amount be retrieved? 
                suppBookPriceJson.put(JSON_PROP_CCYCODE, "INR");
                bookRefJson.put(JSON_PROP_SUPPBOOKFARE, suppBookPriceJson);
                bookRefJson.put(JSON_PROP_SUPPBOOKID, resBodyElem.getAttribute("TransactionIdentifier"));
                bookRefsJsonArr.put(bookRefJson);
            }
            resBodyJson.put(JSON_PROP_PRICEDITIN, pricedItinsJsonArr);
            resBodyJson.put(JSON_PROP_BOOKREFS, bookRefsJsonArr);

            JSONObject resJson = new JSONObject();
            resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
            resJson.put(JSON_PROP_RESBODY, resBodyJson);
            logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));

			JSONObject resSupplierJson =  SupplierCommercials.getSupplierCommercialsV2(reqJson, resJson);
			JSONObject resClientJson = ClientCommercials.getClientCommercialsV2(reqJson, resJson, resSupplierJson);
			AirSearchProcessor.calculatePricesV3(reqJson, resJson, resSupplierJson, resClientJson, true, usrCtx);

			// Calculate company taxes
			TaxEngine.getCompanyTaxes(reqJson, resJson);

			pushSuppFaresToRedisAndRemove(resJson);
			pushPtcFareBrkDwntoRedis(redisPtcPriceItinMap,reqHdrJson);
			
			return resJson.toString();
		}
		catch (Exception x) {
			  logger.error("Exception received while processing", x);
	            return AirSearchProcessor.getEmptyResponse(reqHdrJson).toString();
		}
	}
	
	
	public static void  getSupplierResponsePricedItinerariesJSON(Element resBodyElem, JSONArray pricedItinsJsonArr, boolean generateBookRefIdx, int bookRefIdx, Document ownerDoc, Map<String, String> redisPtcPriceItinMap) throws Exception {
	    	boolean isCombinedReturnJourney = Boolean.valueOf(XMLUtils.getValueAtXPath(resBodyElem, "./@CombinedReturnJourney"));
	    	
	    	 
	    	 String ptcPrefixString="ptcXml";
	    	
	        Element[] pricedItinElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:PricedItineraries/ota:PricedItinerary");
	        for (Element pricedItinElem : pricedItinElems) {
	            JSONObject pricedItinJson =AirSearchProcessor.getPricedItineraryJSON(pricedItinElem);
	           
	            
	            pricedItinJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./air:SupplierID"));
	            pricedItinJson.put(JSON_PROP_ISRETURNJRNYCOMBINED, isCombinedReturnJourney);
	            if (generateBookRefIdx) {
	            	pricedItinJson.put(JSON_PROP_BOOKREFIDX, bookRefIdx);
	            }
	            
	            StringBuilder strBldr= new StringBuilder();
	            strBldr.append(ptcPrefixString+"|"+ getRedisKeyForPricedItinerary(pricedItinJson));
	            redisPtcPriceItinMap.put(strBldr.toString(), XMLTransformer.toString((getRedisPtcFareBreakDown(pricedItinElem,ownerDoc))));
	            
	            pricedItinsJsonArr.put(pricedItinJson);
	        }
	        
	        
	    }
	
	
	private static Element getRedisPtcFareBreakDown(Element priceItinElem, Document ownerDoc) {
		
		Element redisPtcFrBrkDwnsElem=XMLUtils.getFirstElementAtXPath(priceItinElem, "./ota:AirItineraryPricingInfo/ota:PTC_FareBreakdowns");
		Element[] ptcBrkDwnElems=XMLUtils.getElementsAtXPath(priceItinElem, "./ota:AirItineraryPricingInfo/ota:PTC_FareBreakdowns/ota:PTC_FareBreakdown");
		for (Element ptcBrkDwnElem : ptcBrkDwnElems) {
    		
			
		
			//Element redisPtcFrBrkDwnElem=ownerDoc.createElementNS(NS_OTA,"ota:PTC_FareBreakdown");
			
			Element paxTypeElem=XMLUtils.getFirstElementAtXPath(ptcBrkDwnElem, "./ota:PassengerTypeQuantity");
			Node[] paxTypeNodes=XMLUtils.getNodesAtXPath(ptcBrkDwnElem, "./ota:PassengerTypeQuantity");
			
			XMLUtils.removeNodes(paxTypeNodes);
			
			//redisPtcFrBrkDwnElem.appendChild(paxTypeElem);
			
			Element[] fareInfoElems=XMLUtils.getElementsAtXPath(ptcBrkDwnElem, "./ota:FareInfo");
			Element[] ptcChildElems=XMLUtils.getAllChildElements(ptcBrkDwnElem);
			XMLUtils.removeNodes(ptcChildElems);
			
			ptcBrkDwnElem.appendChild(paxTypeElem);
			
			
			for (Element fareInfoElem : fareInfoElems) {
				ptcBrkDwnElem.appendChild(fareInfoElem);
			}
			
			//redisPtcFrBrkDwnsElem.appendChild(redisPtcFrBrkDwnElem);
    		
    	}
		
		return redisPtcFrBrkDwnsElem;
    	
		
		
	}

	protected static Map<String, String> createOriginDestinationOptions(Document ownerDoc, JSONObject pricedItinJson,
			Element odosElem) {
		JSONObject airItinJson = pricedItinJson.getJSONObject(JSON_PROP_AIRITINERARY);
		JSONArray odoJsonArr = airItinJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
		Map<String,String> flightMap=new HashMap<String,String>();
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
				
				JSONObject mkAirlineJson = flSegJson.getJSONObject(JSON_PROP_MARKAIRLINE);
				Element opAirlineElem = ownerDoc.createElementNS(NS_OTA,"ota:OperatingAirline");
				JSONObject opAirlineJson = flSegJson.getJSONObject(JSON_PROP_OPERAIRLINE);
				flSegElem.setAttribute("FlightNumber", mkAirlineJson.optString((JSON_PROP_FLIGHTNBR),""));
				//flightSet.add(Integer.parseInt(opAirlineJson.getString(JSON_PROP_FLIGHTNBR).replaceAll("\\s","")));
				flightMap.put((opAirlineJson.getString(JSON_PROP_FLIGHTNBR).replaceAll("\\s","")).toString(), flSegJson.getString(JSON_PROP_RESBOOKDESIG));
				flSegElem.setAttribute("DepartureDateTime", flSegJson.getString(JSON_PROP_DEPARTDATE));
				flSegElem.setAttribute("ArrivalDateTime", flSegJson.getString(JSON_PROP_ARRIVEDATE));
				String connType = flSegJson.optString(JSON_PROP_CONNTYPE);
				if (connType != null) {
					flSegElem.setAttribute("ConnectionType", connType);
				}
				opAirlineElem.setAttribute("Code", opAirlineJson.getString(JSON_PROP_AIRLINECODE));
				opAirlineElem.setAttribute("FlightNumber", opAirlineJson.getString(JSON_PROP_FLIGHTNBR));
				String companyShortName = opAirlineJson.optString(JSON_PROP_COMPANYSHORTNAME, "");
				if (companyShortName.isEmpty() == false) {
					opAirlineElem.setAttribute("CompanyShortName", companyShortName);
				}
				flSegElem.appendChild(opAirlineElem);

				Element tpaExtsElem = ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
				Element extRPHElem = ownerDoc.createElementNS(NS_AIR, "air:ExtendedRPH");
				extRPHElem.setTextContent(flSegJson.getString(JSON_PROP_EXTENDEDRPH));
				tpaExtsElem.appendChild(extRPHElem);

				Element quoteElem = ownerDoc.createElementNS(NS_AIR, "air:QuoteID");
				quoteElem.setTextContent(flSegJson.getString(JSON_PROP_QUOTEID));
				tpaExtsElem.appendChild(quoteElem);

				flSegElem.appendChild(tpaExtsElem);

				Element mkAirlineElem = ownerDoc.createElementNS(NS_OTA, "ota:MarketingAirline");
				
				mkAirlineElem.setAttribute("Code", mkAirlineJson.getString(JSON_PROP_AIRLINECODE));
				/*String mkAirlineFlNbr = mkAirlineJson.optString(JSON_PROP_FLIGHTNBR, "");
				if (mkAirlineFlNbr.isEmpty() == false) {
					mkAirlineElem.setAttribute("FlightNumber", mkAirlineFlNbr);
				}*/
				String mkAirlineShortName = mkAirlineJson.optString(JSON_PROP_COMPANYSHORTNAME, "");
				if (mkAirlineShortName.isEmpty() == false) {
					mkAirlineElem.setAttribute(JSON_PROP_COMPANYSHORTNAME, mkAirlineShortName);
				}
				flSegElem.appendChild(mkAirlineElem);

				Element bookClsAvailsElem = ownerDoc.createElementNS(NS_OTA, "BookingClassAvails");
				bookClsAvailsElem.setAttribute("CabinType", flSegJson.getString(JSON_PROP_CABINTYPE));
				Element bookClsAvailElem = ownerDoc.createElementNS(NS_OTA, "ota:BookingClassAvail");
				bookClsAvailElem.setAttribute("ResBookDesigCode", flSegJson.getString(JSON_PROP_RESBOOKDESIG));
				bookClsAvailElem.setAttribute("RPH", flSegJson.getString(JSON_PROP_RPH));
				bookClsAvailsElem.appendChild(bookClsAvailElem);
				flSegElem.appendChild(bookClsAvailsElem);

				odoElem.appendChild(flSegElem);
			}

			odosElem.appendChild(odoElem);
		}
		return flightMap;
	}
		

	static void createSSRElem(Document ownerDoc, Element tpExElem, int travellerRPH, JSONObject traveller, Map<String, String> flightMap) {
		Element ssrElem=ownerDoc.createElementNS(NS_AIR, "air:SSR");

Element specialServiceRequestsElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequests");


JSONArray specialServiceRequests=traveller.getJSONObject("specialRequests").getJSONArray("specialRequestInfo");



for(int s=0;s<specialServiceRequests.length();s++)
{


JSONObject specialServiceRequestJson=specialServiceRequests.getJSONObject(s);

	Element specialServiceRequestElem=ownerDoc.createElementNS(NS_OTA, "ota:SpecialServiceRequest");
	
specialServiceRequestElem.setAttribute("TravelerRefNumberRPHList", Integer.toString(travellerRPH));

specialServiceRequestElem.setAttribute("FlightRefNumberRPHList", specialServiceRequestJson.optString(JSON_PROP_FLIGHREFNBR,""));

specialServiceRequestElem.setAttribute("SSRCode", specialServiceRequestJson.optString(JSON_PROP_SSRCODE, ""));

specialServiceRequestElem.setAttribute("ServiceQuantity", specialServiceRequestJson.optString(JSON_PROP_SVCQTY,""));

specialServiceRequestElem.setAttribute("Type", specialServiceRequestJson.optString(JSON_PROP_TYPE,""));

specialServiceRequestElem.setAttribute("Status", specialServiceRequestJson.optString(JSON_PROP_STATUS,""));


if(specialServiceRequestJson.has(JSON_PROP_NUMBER) && !specialServiceRequestJson.get(JSON_PROP_NUMBER).equals("")){
	//TODO:Confirm the default value for Number or remove this element if SI removes it.
	specialServiceRequestElem.setAttribute("Number", specialServiceRequestJson.optString(JSON_PROP_NUMBER,"0"));
}
//TODO: Add/Remove/Alter below Elems and more once standardization is confirmed.
Element airlineElem=ownerDoc.createElementNS(NS_OTA, "ota:Airline");

airlineElem.setAttribute("CompanyShortName", specialServiceRequestJson.optString((JSON_PROP_COMPANYSHORTNAME),""));

airlineElem.setAttribute("Code", specialServiceRequestJson.optString((JSON_PROP_AIRLINECODE),""));

specialServiceRequestElem.appendChild(airlineElem);


Element textElem=ownerDoc.createElementNS(NS_OTA, "ota:Text");

textElem.setTextContent(specialServiceRequestJson.optString((JSON_PROP_DESC),""));

specialServiceRequestElem.appendChild(textElem);


Element flightLegElem=ownerDoc.createElementNS(NS_OTA, "ota:FlightLeg");

flightLegElem.setAttribute("FlightNumber", specialServiceRequestJson.getString((JSON_PROP_FLIGHTNBR)));

flightLegElem.setAttribute("ResBookDesigCode",flightMap.get((specialServiceRequestJson.getString((JSON_PROP_FLIGHTNBR))).replaceAll("\\s","")).toString());

specialServiceRequestElem.appendChild(flightLegElem);


Element categoryCodeElem=ownerDoc.createElementNS(NS_OTA, "ota:CategoryCode");

categoryCodeElem.setTextContent(specialServiceRequestJson.optString((JSON_PROP_CATCODE),""));

specialServiceRequestElem.appendChild(categoryCodeElem);

Element travelerRefElem=ownerDoc.createElementNS(NS_OTA,"ota:TravelerRef");

travelerRefElem.setTextContent(traveller.getString(JSON_PROP_PAXTYPE));

specialServiceRequestElem.appendChild(travelerRefElem);


if(specialServiceRequestJson.has(JSON_PROP_AMOUNT))
{
Element servicePriceElem=ownerDoc.createElementNS(NS_OTA,"ota:ServicePrice");


servicePriceElem.setAttribute("Total", specialServiceRequestJson.getString(JSON_PROP_AMOUNT));
servicePriceElem.setAttribute("CurrencyCode", specialServiceRequestJson.getString(JSON_PROP_CCYCODE));

if(specialServiceRequestJson.has(JSON_PROP_SVCPRC))
{
	Element basePriceElem=ownerDoc.createElementNS(NS_OTA,"ota:BasePrice");

	basePriceElem.setAttribute("Amount",specialServiceRequestJson.getJSONObject(JSON_PROP_SVCPRC).getString(JSON_PROP_BASEPRICE));

	servicePriceElem.appendChild(basePriceElem);
}

if(specialServiceRequestJson.has(JSON_PROP_TAXES))
{
	Element taxesElem=ownerDoc.createElementNS(NS_OTA,"ota:Taxes");

	taxesElem.setAttribute("Amount",specialServiceRequestJson.getJSONObject(JSON_PROP_TAXES).getString(JSON_PROP_AMOUNT));

	servicePriceElem.appendChild(taxesElem);
}



specialServiceRequestElem.appendChild(servicePriceElem);
}

specialServiceRequestsElem.appendChild(specialServiceRequestElem);

ssrElem.appendChild(specialServiceRequestsElem);

tpExElem.appendChild(ssrElem);


}

	}

	static String getRedisKeyForPricedItinerary(JSONObject pricedItinJson) {
		StringBuilder strBldr = new StringBuilder(pricedItinJson.optString(JSON_PROP_SUPPREF));
		
		JSONObject airItinJson = pricedItinJson.optJSONObject(JSON_PROP_AIRITINERARY);
		if (airItinJson != null) {
			JSONArray origDestOptsJsonArr = airItinJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
			for (int j = 0; j < origDestOptsJsonArr.length(); j++) {
				JSONObject origDestOptJson = origDestOptsJsonArr.getJSONObject(j);
				strBldr.append('[');
				JSONArray flSegsJsonArr = origDestOptJson.optJSONArray(JSON_PROP_FLIGHTSEG);
				if (flSegsJsonArr == null) {
					break;
				}

				for (int k = 0; k < flSegsJsonArr.length(); k++) {
					JSONObject flSegJson = flSegsJsonArr.getJSONObject(k);
					JSONObject opAirlineJson = flSegJson.getJSONObject(JSON_PROP_OPERAIRLINE);
					strBldr.append(opAirlineJson.getString(JSON_PROP_AIRLINECODE).concat(opAirlineJson.getString(JSON_PROP_FLIGHTNBR)).concat("|"));
				}
				strBldr.setLength(strBldr.length() - 1);
				strBldr.append(']');
			}
		}
		return strBldr.toString();
	}
	
	 private static void pushPtcFareBrkDwntoRedis(Map<String, String> redisPtcPriceItinMap, JSONObject reqHdrJson) {
			Map<String,String> redisPtcMap=new HashMap<String,String>();
			redisPtcMap=redisPtcPriceItinMap;
		 	StringBuilder redisKeyBldr=new StringBuilder();
		 	redisKeyBldr.append("ptcXml|");
		 	redisKeyBldr.append(reqHdrJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_AIR));
		 	
			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			
			redisConn.hmset(redisKeyBldr.toString(), redisPtcMap);
			redisConn.pexpire(redisKeyBldr.toString(), (long) (AirConfig.getRedisTTLMinutes() * 60 * 1000));
			RedisConfig.releaseRedisConnectionToPool(redisConn);
			
		}

	
	private static void pushSuppFaresToRedisAndRemove(JSONObject resJson) {
		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		JSONArray pricedItinsJsonArr = resBodyJson.optJSONArray(JSON_PROP_PRICEDITIN);
		JSONArray bookRefsJsonArr = resBodyJson.optJSONArray(JSON_PROP_BOOKREFS);
		resBodyJson.remove(JSON_PROP_BOOKREFS);
		
		if (pricedItinsJsonArr == null || bookRefsJsonArr == null) {
			// TODO: This should never happen. Log a warning message here.
			return;
		}
		
		Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();
		for (int i=0; i < pricedItinsJsonArr.length(); i++) {
			JSONObject pricedItinJson = pricedItinsJsonArr.getJSONObject(i);
			JSONObject suppPriceInfoJson = pricedItinJson.optJSONObject(JSON_PROP_SUPPPRICEINFO);
			pricedItinJson.remove(JSON_PROP_SUPPPRICEINFO);

			int bookRefIdx = pricedItinJson.getInt(JSON_PROP_BOOKREFIDX);
			JSONObject bookRefJson = bookRefsJsonArr.optJSONObject(bookRefIdx);
			pricedItinJson.remove(JSON_PROP_BOOKREFIDX);
			
			//Getting ClientCommercial Info
			JSONArray clientCommercialItinInfoJsonArr = pricedItinJson.optJSONArray(JSON_PROP_CLIENTCOMMITININFO);
			pricedItinJson.remove(JSON_PROP_CLIENTCOMMITININFO);
			
			JSONArray clientCommercialItinTotalJsonArr = pricedItinJson.optJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMS);
			pricedItinJson.remove(JSON_PROP_CLIENTENTITYTOTALCOMMS);
			
			
			
			if ( suppPriceInfoJson == null || bookRefJson == null) {
				// TODO: This should never happen. Log a warning message here.
				continue;
			}
			
			JSONObject suppPriceBookInfoJson = new JSONObject();
			suppPriceBookInfoJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceInfoJson);
			suppPriceBookInfoJson.put(JSON_PROP_BOOKREFS, bookRefJson);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTCOMMITININFO, clientCommercialItinInfoJsonArr);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientCommercialItinTotalJsonArr);
			reprcSuppFaresMap.put(getRedisKeyForPricedItinerary(pricedItinJson), suppPriceBookInfoJson.toString());
			
			
			
		}
		
		String redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_AIR);
		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		redisConn.hmset(redisKey, reprcSuppFaresMap);
		redisConn.pexpire(redisKey, (long) (AirConfig.getRedisTTLMinutes() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);
	}

	private static boolean hasGetSSRBeenInvokedForPricedItin(JSONObject reqHdrJson, JSONObject pricedItinJson) {
		String pricedItinRedisKey = getRedisKeyForPricedItinerary(pricedItinJson);
		String sessionID = reqHdrJson.optString(JSON_PROP_SESSIONID);
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool()) {
			String pricedItinGetSSR = redisConn.hget(String.format("%s|AIR|GetSSR", sessionID), pricedItinRedisKey);
			return (pricedItinGetSSR != null);
		}
		catch (Exception x) {
			logger.warn(String.format("An error occurred while retrieving GetSSR state for %s in session %s", pricedItinRedisKey, sessionID), x);
			return false;
		}
	}
}
