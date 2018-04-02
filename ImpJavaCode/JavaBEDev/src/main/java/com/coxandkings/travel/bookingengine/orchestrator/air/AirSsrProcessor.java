package com.coxandkings.travel.bookingengine.orchestrator.air;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.ServletContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;


public class AirSsrProcessor implements AirConstants {
	
	private static final Logger logger = LogManager.getLogger(AirSsrProcessor.class);
	
	public static String process(JSONObject reqJson) throws Exception {
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
		  JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
		try {
			TrackingContext.setTrackingContext(reqJson);
			OperationConfig opConfig = AirConfig.getOperationConfig(TrackingContext.getTrackingContext().getTrackingParameter(ServletContext.RESTPROP_OPERATION));
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./air:OTA_AirGetSSRQWrapper");
			reqBodyElem.removeChild(wrapperElem);

		/*	JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);*/

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			AirSearchProcessor.createHeader(reqHdrJson, reqElem);

			
			// AirTravelAvail array
			String prevSuppID = "";
			JSONArray pricedItinsJSONArr = reqBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
			for (int x = 0; x < pricedItinsJSONArr.length(); x++) {
				JSONObject pricedItinJson = pricedItinsJSONArr.getJSONObject(x);
				String supplierID = pricedItinJson.getString(JSON_PROP_SUPPREF);
				Element ssrRQWrapper = null;
				Element ssrRQ = null;
				Element travelInfoElem = null;
				if (supplierID.equals(prevSuppID)) {
					ssrRQWrapper = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestBody/air:OTA_AirGetSSRQWrapper");
					ssrRQ = XMLUtils.getFirstElementAtXPath(ssrRQWrapper, "./ota:OTA_AirGetSSRQ");
					travelInfoElem = XMLUtils.getFirstElementAtXPath(ssrRQ, "./ota:TravelerInfoSummary");
				} 
				else {
					ssrRQWrapper = (Element) wrapperElem.cloneNode(true);
					reqBodyElem.appendChild(ssrRQWrapper);

					ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT, supplierID);
					if (prodSupplier == null) {
						throw new Exception(String.format("Product supplier %s not found for user/client", supplierID));
					}

					Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
					Element suppCredsElem = XMLUtils.getFirstElementAtXPath(suppCredsListElem, String.format("./com:SupplierCredentials[com:SupplierID = '%s']", supplierID));
					if (suppCredsElem == null) {
						suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
					}

					ssrRQ = XMLUtils.getFirstElementAtXPath(ssrRQWrapper, "./ota:OTA_AirGetSSRQ");
					travelInfoElem = XMLUtils.getFirstElementAtXPath(ssrRQ, "./ota:TravelerInfoSummary");
					Element suppID = ownerDoc.createElementNS(NS_AIR, "air:SupplierID");

					suppID.setTextContent(pricedItinJson.getString(JSON_PROP_SUPPREF));
					Element sequence = ownerDoc.createElementNS(NS_AIR, "air:Sequence");
					sequence.setTextContent(Integer.toString(x));

					ssrRQWrapper.insertBefore(suppID, ssrRQ);
					ssrRQWrapper.insertBefore(sequence, ssrRQ);

					// loop for airTravelerAvail
					JSONArray airTravelerAvailArr = reqBodyJson.getJSONArray(JSON_PROP_PAXINFO);
					Element priceReqElem = XMLUtils.getFirstElementAtXPath(travelInfoElem, "./ota:PriceRequestInformation");
					for (int i = 0; i < airTravelerAvailArr.length(); i++) {
						JSONObject airTravlerAvailJson = airTravelerAvailArr.getJSONObject(i);

						int travellerQuantity = airTravlerAvailJson.getInt(JSON_PROP_QTY);
						for (int rphCount = 0; rphCount < travellerQuantity; rphCount++) {
							Element airTravelerAvail = ownerDoc.createElementNS(Constants.NS_OTA, "ota:AirTravelerAvail");
							Element airTraveler = ownerDoc.createElementNS(Constants.NS_OTA, "ota:AirTraveler");
							airTraveler.setAttribute("PassengerTypeCode", airTravlerAvailJson.getString(JSON_PROP_PAXTYPE));

							Element travelerRefNo = ownerDoc.createElementNS(Constants.NS_OTA, "ota:TravelerRefNumber");
							travelerRefNo.setAttribute("RPH", Integer.toString(rphCount));
							airTraveler.appendChild(travelerRefNo);
							airTravelerAvail.appendChild(airTraveler);
							travelInfoElem.insertBefore(airTravelerAvail, priceReqElem);
						}
					}
				}
				Element originDestinationInformation = null;
				Element odosElem = null;
				if (supplierID.equals(prevSuppID)) {
					originDestinationInformation = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody/air:OTA_AirGetSSRQWrapper/ota:OTA_AirGetSSRQ/ota:OriginDestinationInformation");
					odosElem = XMLUtils.getFirstElementAtXPath(originDestinationInformation, "./ota:OriginDestinationOptions");
					if (odosElem == null) {
						logger.warn(String.format("XML element for ota:OriginDestinationOptions not found for supplier %s", supplierID));
					}
				} 
				else {
					originDestinationInformation = XMLUtils.getFirstElementAtXPath(ssrRQ, "./ota:OriginDestinationInformation");
					odosElem = ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationOptions");
				}

				AirPriceProcessor.createOriginDestinationOptions(ownerDoc, pricedItinJson, odosElem);
				originDestinationInformation.appendChild(odosElem);
				prevSuppID = supplierID;

			}

			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			// Response XML To JSON

			JSONObject resBodyJson = new JSONObject();
			JSONArray pricedItinsJsonArr = new JSONArray();

			Element[] resWrapperElems = XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirGetSSRSWrapper");
			for (Element resWrapperElem : resWrapperElems) {
				JSONObject priceItinJson = new JSONObject();
				priceItinJson = getSupplierPriceItin(priceItinJson, resWrapperElem);
				pricedItinsJsonArr.put(priceItinJson);
			}
			resBodyJson.put(JSON_PROP_PRICEDITIN, pricedItinsJsonArr);

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);

			return resJson.toString();
		} 
		catch (Exception x) {
			logger.error("Exception received while processing", x);
            return AirSearchProcessor.getEmptyResponse(reqHdrJson).toString();
		}
	}

	private static JSONObject getSupplierPriceItin(JSONObject priceItinJson, Element resWrapperElem) {
		// TODO Auto-generated method stub
		Element supplierRefElem= XMLUtils.getFirstElementAtXPath(resWrapperElem, "./air:SupplierID");
		priceItinJson.put(JSON_PROP_SUPPREF, supplierRefElem.getTextContent());
		
		Element airItinElem=XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_AirGetSSRS/ota:AirItinerary");
		
		JSONObject airItinJson=new JSONObject();
		airItinJson = getAirItinJson(airItinElem,airItinJson);
		priceItinJson.put(JSON_PROP_AIRITINERARY, airItinJson);
		
		//SSR info
		JSONArray ssrJsonArr=new JSONArray();
		//Element specialRq=XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ota:OTA_AirGetSSRS/ota:TravelerInfo/ota:SpecialReqDetails");
		Element specialRqElems[]= XMLUtils.getElementsAtXPath(resWrapperElem, "./ota:OTA_AirGetSSRS/ota:TravelerInfo/ota:SpecialReqDetails");
		
		for (Element specialRqElem : specialRqElems) 
		{
		Element ssrElems[]= XMLUtils.getElementsAtXPath(specialRqElem, "./ota:SpecialServiceRequests/ota:SpecialServiceRequest");
        for (Element ssrElem : ssrElems) {
			JSONObject ssrJson=new JSONObject();
			//TODO:Temporary fix for removing seats from SSRS
			if(ssrElem.getAttribute("SSRCode").equals("SEAT")) {
				continue;
			}
			
			ssrJson.put(JSON_PROP_SSRCODE,ssrElem.getAttribute("SSRCode"));
			ssrJson.put(JSON_PROP_NUMBER,ssrElem.getAttribute("Number"));
			ssrJson.put(JSON_PROP_SVCQTY,ssrElem.getAttribute("ServiceQuantity"));
			ssrJson.put(JSON_PROP_STATUS,ssrElem.getAttribute("Status"));
			ssrJson.put(JSON_PROP_TYPE,ssrElem.getAttribute("Type"));
			
			if(ssrElem.getAttribute("FlightRefNumberRPHList")!=null)
			{
				ssrJson.put(JSON_PROP_FLIGHREFNBR, ssrElem.getAttribute("FlightRefNumberRPHList"));
			}
			
			Element flightLegElem=XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:FlightLeg");
			
			if(flightLegElem.getAttribute("FlightNumber")!=null)
			{
				ssrJson.put(JSON_PROP_FLIGHTNBR, flightLegElem.getAttribute("FlightNumber"));
			}
			
			ssrJson.put(JSON_PROP_DATE, flightLegElem.getAttribute("Date"));
			
			Element departureElem=XMLUtils.getFirstElementAtXPath(flightLegElem, "./ota:DepartureAirport");
			Element arrivalElem=XMLUtils.getFirstElementAtXPath(flightLegElem, "./ota:ArrivalAirport");
			
			if(arrivalElem!=null)
			{
				ssrJson.put(JSON_PROP_DESTLOC, arrivalElem.getAttribute("LocationCode"));
			}
			if(departureElem!=null)
			{
				ssrJson.put(JSON_PROP_ORIGLOC, departureElem.getAttribute("LocationCode"));
			}
			
			Element airLineElem=XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:Airline");
			
			if(airLineElem!=null)
			{
				ssrJson.put(JSON_PROP_AIRLINECODE, airLineElem.getAttribute("Code"));
				ssrJson.put(JSON_PROP_COMPANYSHORTNAME, airLineElem.getAttribute("CompanyShortName"));
			}
			
			
			Element servicePrice=XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:ServicePrice");
			Element taxesElem=XMLUtils.getFirstElementAtXPath(servicePrice, "./ota:Taxes");
			if (servicePrice != null) {
				JSONObject servicePriceJson=new JSONObject();
				JSONObject taxesJson=new JSONObject();
				Element basePrice=XMLUtils.getFirstElementAtXPath(servicePrice, "./ota:BasePrice");
			
				ssrJson.put(JSON_PROP_AMOUNT, XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:Amount").getTextContent());
				ssrJson.put(JSON_PROP_CCYCODE, XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:currencyCode").getTextContent());
				
				servicePriceJson.put(JSON_PROP_BASEPRICE, basePrice.getAttribute(XML_ATTR_AMOUNT));
				
				if (taxesElem != null) {
					taxesJson.put(JSON_PROP_AMOUNT, taxesElem.getAttribute(XML_ATTR_AMOUNT));
					ssrJson.put(JSON_PROP_TAXES, taxesJson);
				}
				
				ssrJson.put(JSON_PROP_SVCPRC, servicePriceJson);
			}
			
			Element categoryCode=XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:CategoryCode");
			if(categoryCode!=null)
			{
				ssrJson.put(JSON_PROP_CATCODE, categoryCode.getTextContent());
			}
			
			Element textElem=XMLUtils.getFirstElementAtXPath(ssrElem, "./ota:Text");
			if(textElem!=null)
			{
				ssrJson.put(JSON_PROP_DESC, textElem.getTextContent());
			}
		
			
			ssrJsonArr.put(ssrJson);
			
		}
		}
        priceItinJson.put(JSON_PROP_SPECIALSVCREQS, ssrJsonArr);
		return priceItinJson;
	}

	private static JSONObject getAirItinJson(Element airItinElem, JSONObject airItinJson) {
		// TODO Auto-generated method stub
		JSONArray odoJsonArray= new JSONArray();
		Element odosElem[]=XMLUtils.getElementsAtXPath(airItinElem, "./ota:OriginDestinationOptions/ota:OriginDestinationOption");
		 
		for (Element odoElem : odosElem) {
			JSONObject odoJson= new JSONObject();
			JSONArray flightSegArrJson=new JSONArray();
			Element flightSegElemArr[]=XMLUtils.getElementsAtXPath(odoElem, "./ota:FlightSegment");
				for (Element flightSegElem : flightSegElemArr) {
					JSONObject flightSegJson=new JSONObject();
					
					flightSegJson.put(JSON_PROP_FLIGHTNBR, flightSegElem.getAttribute("FlightNumber"));
					flightSegJson.put(JSON_PROP_DEPARTDATE, flightSegElem.getAttribute("DepartureDateTime"));
					
					Element departureAirportElem=XMLUtils.getFirstElementAtXPath(flightSegElem, "./ota:DepartureAirport");
					Element arrivalAirportElem=XMLUtils.getFirstElementAtXPath(flightSegElem, "./ota:ArrivalAirport");
					flightSegJson.put(JSON_PROP_ORIGLOC, departureAirportElem.getAttribute("LocationCode"));
					flightSegJson.put(JSON_PROP_DESTLOC, arrivalAirportElem.getAttribute("LocationCode"));
					
					JSONObject operatingAirlineJson=new JSONObject();
					Element operatingAirlineElem=XMLUtils.getFirstElementAtXPath(flightSegElem, "./ota:OperatingAirline");
					operatingAirlineJson.put(JSON_PROP_AIRLINECODE,operatingAirlineElem.getAttribute("Code"));
					operatingAirlineJson.put(JSON_PROP_FLIGHTNBR,operatingAirlineElem.getAttribute("FlightNumber"));
					
					flightSegJson.put(JSON_PROP_OPERAIRLINE, operatingAirlineJson );
					flightSegArrJson.put(flightSegJson);
				}
				odoJson.put(JSON_PROP_FLIGHTSEG, flightSegArrJson);
				odoJsonArray.put(odoJson);
		}
		airItinJson.append(JSON_PROP_ORIGDESTOPTS, odoJsonArray);
		return airItinJson;
	}
	
}
