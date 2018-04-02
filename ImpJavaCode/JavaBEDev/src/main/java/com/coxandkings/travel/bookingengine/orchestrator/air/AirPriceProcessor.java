package com.coxandkings.travel.bookingengine.orchestrator.air;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.enums.TripIndicator;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.ServletContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class AirPriceProcessor implements AirConstants  {

	private static final Logger logger = LogManager.getLogger(AirPriceProcessor.class);

	public static Element getAirTravelerAvailElement(Document ownerDoc, JSONObject traveller) {
		Element travellerElem = ownerDoc.createElementNS(NS_OTA, "ota:AirTravelerAvail");
		Element psgrElem = ownerDoc.createElementNS(NS_OTA, "ota:PassengerTypeQuantity");
		psgrElem.setAttribute("Code", traveller.getString(JSON_PROP_PAXTYPE));
		psgrElem.setAttribute("Quantity", String.valueOf(traveller.getInt(JSON_PROP_QTY)));
		travellerElem.appendChild(psgrElem);
		return travellerElem;
	}

	public static String process(JSONObject reqJson) {
		  JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
		  JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
		try {
			TrackingContext.setTrackingContext(reqJson);
			OperationConfig opConfig = AirConfig.getOperationConfig(TrackingContext.getTrackingContext().getTrackingParameter(ServletContext.RESTPROP_OPERATION));
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			
			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_AirPriceRQWrapper");
			reqBodyElem.removeChild(wrapperElem);

			//JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			//JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
            TripIndicator tripInd = AirSearchProcessor.deduceTripIndicator(reqHdrJson, reqBodyJson);
            reqBodyJson.put(JSON_PROP_TRIPIND, tripInd.toString());


			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			AirSearchProcessor.createHeader(reqHdrJson, reqElem);

			String prevSuppID = "";
			JSONArray pricedItinsJSONArr = reqBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
			for (int y=0; y < pricedItinsJSONArr.length(); y++) {
				JSONObject pricedItinJson = pricedItinsJSONArr.getJSONObject(y);
				
				//------- Loop Begin --------
				
				String suppID = pricedItinJson.getString(JSON_PROP_SUPPREF);
				Element suppWrapperElem = null;
				Element otaReqElem = null;
				Element travelerInfoElem = null;
				if (suppID.equals(prevSuppID)) {
					suppWrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, String.format("./air:OTA_AirPriceRQWrapper[air:SupplierID = '%s']", suppID));
					travelerInfoElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirPriceRQ/ota:TravelerInfoSummary");
					otaReqElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_AirPriceRQ");
				}
				else {
					suppWrapperElem = (Element) wrapperElem.cloneNode(true);
					reqBodyElem.appendChild(suppWrapperElem);
					//ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(AirConstants.PRODUCT_AIR, suppID);
					ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT, suppID);
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
		
					Element priceInfoElem = XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:PriceRequestInformation");
					JSONArray travellerArr = reqBodyJson.getJSONArray(JSON_PROP_PAXINFO);
					for (int i=0; i < travellerArr.length(); i++) {
						JSONObject traveller = (JSONObject) travellerArr.get(i);
						Element travellerElem = getAirTravelerAvailElement(ownerDoc, traveller);
						travelerInfoElem.insertBefore(travellerElem, priceInfoElem);
					}					
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

				createOriginDestinationOptions(ownerDoc, pricedItinJson, odosElem);
				createTPA_Extensions(reqBodyJson, travelerInfoElem);
	
				//------- Loop End --------
				prevSuppID = suppID;
			}

            Element resElem = null;
            resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
            if (resElem == null) {
            	throw new Exception("Null response received from SI");
            }

            JSONObject resBodyJson = new JSONObject();
            JSONArray pricedItinsJsonArr = new JSONArray();
            Element[] resWrapperElems = sortWrapperElementsBySequence(XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirPriceRSWrapper"));
            for (Element resWrapperElem : resWrapperElems) {
            	Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_AirPriceRS");
            	AirSearchProcessor.getSupplierResponsePricedItinerariesJSON(resBodyElem, pricedItinsJsonArr);
            }
            resBodyJson.put(JSON_PROP_PRICEDITIN, pricedItinsJsonArr);

            JSONObject resJson = new JSONObject();
            resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
            resJson.put(JSON_PROP_RESBODY, resBodyJson);
            
			JSONObject resSupplierJson =  SupplierCommercials.getSupplierCommercialsV2(reqJson, resJson);
			JSONObject resClientJson = ClientCommercials.getClientCommercialsV2(reqJson, resJson, resSupplierJson);
			AirSearchProcessor.calculatePricesV3(reqJson, resJson, resSupplierJson, resClientJson, false, usrCtx);

			return resJson.toString();
		}
		catch (Exception x) {
			  logger.error("Exception received while processing", x);
	            return AirSearchProcessor.getEmptyResponse(reqHdrJson).toString();
		}
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
		JSONArray odoJsonArr = airItinJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
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
				/*String mkAirlineFlNbr = mkAirlineJson.optString(JSON_PROP_FLIGHTNBR, "");*/
				/*if (mkAirlineFlNbr.isEmpty() == false) {
					mkAirlineElem.setAttribute("FlightNumber", mkAirlineFlNbr);
				}*/
				String mkAirlineShortName = mkAirlineJson.optString(JSON_PROP_COMPANYSHORTNAME, "");
				if (mkAirlineShortName.isEmpty() == false) {
					mkAirlineElem.setAttribute("CompanyShortName", mkAirlineShortName);
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
