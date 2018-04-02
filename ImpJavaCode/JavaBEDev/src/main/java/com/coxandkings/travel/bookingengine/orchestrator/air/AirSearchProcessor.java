package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.enums.TripIndicator;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.RedisAirportDataV2;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.ServletContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class AirSearchProcessor implements AirConstants {

    private static final Logger logger = LogManager.getLogger(AirSearchProcessor.class);
    private static String mFeesPriceCompQualifier = JSON_PROP_FEES.concat(SIGMA).concat(".").concat(JSON_PROP_FEE).concat(".");
    private static String mRcvsPriceCompQualifier = JSON_PROP_RECEIVABLES.concat(SIGMA).concat(".").concat(JSON_PROP_RECEIVABLE).concat(".");
    private static String mTaxesPriceCompQualifier = JSON_PROP_TAXES.concat(SIGMA).concat(".").concat(JSON_PROP_TAX).concat(".");
    
    public static Element getAirTravelerAvailElement(Document ownerDoc, JSONObject traveller) throws Exception{
        Element travellerElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:AirTravelerAvail");
        Element psgrElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:PassengerTypeQuantity");
        psgrElem.setAttribute("Code", traveller.getString(JSON_PROP_PAXTYPE));
        psgrElem.setAttribute("Quantity", String.valueOf(traveller.getInt(JSON_PROP_QTY)));
        travellerElem.appendChild(psgrElem);
        return travellerElem;
    }

    public static Element getOriginDestinationElement(Document ownerDoc, JSONObject origDest) throws Exception{
        Element origDestElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:OriginDestinationInformation");
        Element depatureElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:DepartureDateTime");
        // TODO: Would the format of departure date in incoming request be yyyy-mm-dd?
        depatureElem.setTextContent(origDest.getString(JSON_PROP_DEPARTDATE).concat("T00:00:00"));
        origDestElem.appendChild(depatureElem);
        Element originElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:OriginLocation");
        originElem.setAttribute("LocationCode", origDest.getString(JSON_PROP_ORIGLOC));
        origDestElem.appendChild(originElem);
        Element destElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:DestinationLocation");
        destElem.setAttribute("LocationCode", origDest.getString(JSON_PROP_DESTLOC));
        origDestElem.appendChild(destElem);
        return origDestElem;
    }

    public static JSONObject getAirItineraryJSON(Element airItinElem)throws Exception {
        JSONObject airItinJson = new JSONObject();

        if (airItinElem != null) {
//            // TODO: RPH is different for each flight segment. What to map here?
//            airItinJson.put("rph", "");
            JSONArray odOptJsonArr = new JSONArray();
            Element[] odOptElems = XMLUtils.getElementsAtXPath(airItinElem, "./ota:OriginDestinationOptions/ota:OriginDestinationOption");
            for (Element odOptElem : odOptElems) {
                odOptJsonArr.put(getOriginDestinationOptionJSON(odOptElem));
            }

            airItinJson.put(JSON_PROP_ORIGDESTOPTS, odOptJsonArr);
        }

        return airItinJson;
    }

    public static JSONObject getAirItineraryPricingJSON(Element airItinPricingElem)throws Exception {
        JSONObject airItinPricingJson = new JSONObject();

        JSONObject itinTotalFareJson = new JSONObject();
        airItinPricingJson.put(JSON_PROP_ITINTOTALFARE, itinTotalFareJson);

        JSONArray ptcFaresJsonArr = new JSONArray();
        Element[] ptcFareBkElems = XMLUtils.getElementsAtXPath(airItinPricingElem, "./ota:PTC_FareBreakdowns/ota:PTC_FareBreakdown");
        for (Element ptcFareBkElem : ptcFareBkElems) {
            ptcFaresJsonArr.put(getPTCFareBreakdownJSON(ptcFareBkElem));
        }
        airItinPricingJson.put(JSON_PROP_PAXTYPEFARES, ptcFaresJsonArr);
        
       
        if((XMLUtils.getFirstElementAtXPath(airItinPricingElem, "./ota:PriceRequestInformation/ota:TPA_Extensions/air:SSR"))!=null) {
        	
       
        JSONArray ssrJsonArr = new JSONArray();
        Element[] ssrElems = XMLUtils.getElementsAtXPath(airItinPricingElem, "./ota:PriceRequestInformation/ota:TPA_Extensions/air:SSR/ota:SpecialServiceRequests/ota:SpecialServiceRequest");
        
        for (Element ssrElem : ssrElems) {
        	ssrJsonArr.put(getSSRJson(ssrElem));
        }
        airItinPricingJson.put(JSON_PROP_SPECIALSVCREQS, ssrJsonArr);
        }
        
        return airItinPricingJson;
    }

    private static JSONObject getSSRJson(Element ssrElem) {
		
    	JSONObject ssrJson= new JSONObject();
    	
    	ssrJson.put(JSON_PROP_SSRCODE, ssrElem.getAttribute("SSRCode"));
    	ssrJson.put(JSON_PROP_AMOUNT, XMLUtils.getValueAtXPath(ssrElem, "./ota:ServicePrice/@Total"));
    	JSONObject servicePriceJson= new JSONObject();
    	servicePriceJson.put(JSON_PROP_CCYCODE,XMLUtils.getValueAtXPath(ssrElem, "./ota:ServicePrice/@CurrencyCode"));
    	servicePriceJson.put(JSON_PROP_BASEFARE, XMLUtils.getValueAtXPath(ssrElem, "./ota:ServicePrice/ota:BasePrice/@Amount"));
    	servicePriceJson.put(JSON_PROP_TAXES, XMLUtils.getValueAtXPath(ssrElem, "./ota:ServicePrice/ota:Taxes/@Amount"));
    	
    	ssrJson.put(JSON_PROP_SVCPRC,servicePriceJson );
    	
		return ssrJson;
	}

	public static JSONObject getFlightSegmentJSON(Element flightSegElem)throws Exception {
        JSONObject flightSegJson = new JSONObject();

        flightSegJson.put(JSON_PROP_DEPARTDATE, XMLUtils.getValueAtXPath(flightSegElem, "./@DepartureDateTime"));
        flightSegJson.put(JSON_PROP_ARRIVEDATE, XMLUtils.getValueAtXPath(flightSegElem, "./@ArrivalDateTime"));
        flightSegJson.put(JSON_PROP_ORIGLOC, XMLUtils.getValueAtXPath(flightSegElem, "./ota:DepartureAirport/@LocationCode"));
        flightSegJson.put(JSON_PROP_DEPARTTERMINAL, XMLUtils.getValueAtXPath(flightSegElem, "./ota:DepartureAirport/@Terminal"));
        flightSegJson.put(JSON_PROP_ARRIVETERMINAL, XMLUtils.getValueAtXPath(flightSegElem, "./ota:ArrivalAirport/@Terminal"));
        flightSegJson.put(JSON_PROP_DESTLOC, XMLUtils.getValueAtXPath(flightSegElem, "./ota:ArrivalAirport/@LocationCode"));
        flightSegJson.put(JSON_PROP_OPERAIRLINE, getAirlineJSON((XMLUtils.getFirstElementAtXPath(flightSegElem, "./ota:OperatingAirline")),JSON_PROP_OPERAIRLINE.toString()));
        flightSegJson.put(JSON_PROP_MARKAIRLINE, getAirlineJSON((flightSegElem),JSON_PROP_MARKAIRLINE.toString()));
        flightSegJson.put(JSON_PROP_JOURNEYDUR, Utils.convertToInt(XMLUtils.getValueAtXPath(flightSegElem, "./ota:TPA_Extensions/air:JourneyDuration"), 0));
        flightSegJson.put(JSON_PROP_QUOTEID, XMLUtils.getValueAtXPath(flightSegElem, "./ota:TPA_Extensions/air:QuoteID"));
        flightSegJson.put(JSON_PROP_EXTENDEDRPH, XMLUtils.getValueAtXPath(flightSegElem, "./ota:TPA_Extensions/air:ExtendedRPH"));
        flightSegJson.put(JSON_PROP_AVAILCOUNT, Utils.convertToInt(XMLUtils.getValueAtXPath(flightSegElem, "./ota:TPA_Extensions/air:AvailableCount"), 0));
        flightSegJson.put(JSON_PROP_REFUNDIND, Boolean.valueOf(XMLUtils.getValueAtXPath(flightSegElem, "./ota:TPA_Extensions/air:RefundableIndicator")));
        flightSegJson.put(JSON_PROP_CABINTYPE, XMLUtils.getValueAtXPath(flightSegElem, "./ota:BookingClassAvails/@CabinType"));
        flightSegJson.put(JSON_PROP_RESBOOKDESIG, XMLUtils.getValueAtXPath(flightSegElem, "./ota:BookingClassAvails/ota:BookingClassAvail/@ResBookDesigCode"));
        flightSegJson.put(JSON_PROP_RPH, XMLUtils.getValueAtXPath(flightSegElem, "./ota:BookingClassAvails/ota:BookingClassAvail/@RPH"));
        flightSegJson.put(JSON_PROP_CONNTYPE, XMLUtils.getValueAtXPath(flightSegElem, "./@ConnectionType")); 
        // TODO: This is TBD in case of INDIGO
//        flightSegJson.put("comments", new JSONArray());

        return flightSegJson;
    }

    public static JSONObject getAirlineJSON(Element airlineElem,String airlineType)throws Exception {
        JSONObject airlineJson = new JSONObject();
        if (airlineElem != null) {
        	if(airlineType.equals(JSON_PROP_MARKAIRLINE))
        	{
        		  airlineJson.put(JSON_PROP_FLIGHTNBR, airlineElem.getAttribute("FlightNumber"));
        		  airlineJson.put(JSON_PROP_AIRLINECODE, XMLUtils.getValueAtXPath(airlineElem, "./ota:MarketingAirline/@Code"));
        	}
        	else
        	{
            airlineJson.put(JSON_PROP_FLIGHTNBR, XMLUtils.getValueAtXPath(airlineElem, "./@FlightNumber"));
            airlineJson.put(JSON_PROP_AIRLINECODE, XMLUtils.getValueAtXPath(airlineElem, "./@Code"));
        	}
        }
        return airlineJson;
    }

    public static JSONObject getOriginDestinationOptionJSON(Element odOptElem)throws Exception {
        JSONObject odOptJson = new JSONObject();

        JSONArray flightSegsJsonArr = new JSONArray();
        Element[] flightSegElems = XMLUtils.getElementsAtXPath(odOptElem, "./ota:FlightSegment");
        for (Element flightSegElem : flightSegElems) {
            flightSegsJsonArr.put(getFlightSegmentJSON(flightSegElem));
        }

        odOptJson.put(JSON_PROP_FLIGHTSEG, flightSegsJsonArr);
        return odOptJson;
    }

    public static JSONObject getPricedItineraryJSON(Element pricedItinElem) throws Exception{
        JSONObject pricedItinJson = new JSONObject();

        Element airItinElem = XMLUtils.getFirstElementAtXPath(pricedItinElem, "./ota:AirItinerary");
        pricedItinJson.put(JSON_PROP_AIRITINERARY, getAirItineraryJSON(airItinElem));
        Element airItinPricingElem = XMLUtils.getFirstElementAtXPath(pricedItinElem, "./ota:AirItineraryPricingInfo");
        pricedItinJson.put(JSON_PROP_AIRPRICEINFO, getAirItineraryPricingJSON(airItinPricingElem));

        return pricedItinJson;
    }

    public static JSONObject getPTCFareBreakdownJSON(Element ptcFareBkElem) throws Exception{
        JSONObject paxFareJson = new JSONObject();

        paxFareJson.put(JSON_PROP_PAXTYPE, XMLUtils.getValueAtXPath(ptcFareBkElem, "./ota:PassengerTypeQuantity/@Code"));
        Element baseFareElem = XMLUtils.getFirstElementAtXPath(ptcFareBkElem, "./ota:PassengerFare/ota:BaseFare");
        JSONObject baseFareJson = new JSONObject();
        BigDecimal baseFareAmt = new BigDecimal(0);
        String baseFareCurrency = "";
        if (baseFareElem != null) {
            baseFareAmt = Utils.convertToBigDecimal(baseFareElem.getAttribute(XML_ATTR_AMOUNT), 0);
            baseFareCurrency = baseFareElem.getAttribute(XML_ATTR_CURRENCYCODE);
            baseFareJson.put(JSON_PROP_AMOUNT, baseFareAmt);
            baseFareJson.put(JSON_PROP_CCYCODE, baseFareCurrency);
        }
        paxFareJson.put(JSON_PROP_BASEFARE, baseFareJson);

        //----------------------------------------------------------------
        // Taxes
        // This code always calculates taxes/fees totals and retrieve currencycode. In SI standardization 3.0, the top level elements for
        // ota:Taxes and ota:Fees parent elements will never have Amount and CurrencyCode attributes. Total amount and currency code for
        // taxes and fees will need to be calculated from child tax/fee elements.

        Element taxesElem = XMLUtils.getFirstElementAtXPath(ptcFareBkElem, "./ota:PassengerFare/ota:Taxes");
        JSONObject taxesJson = new JSONObject();
        BigDecimal taxesAmount = new BigDecimal(0);
        String taxesCurrency = "";
        JSONArray taxJsonArr = new JSONArray();
        Element[] taxElems = XMLUtils.getElementsAtXPath(taxesElem, "./ota:Tax");
        for (Element taxElem : taxElems) {
            JSONObject taxJson = new JSONObject();
            BigDecimal taxAmt = Utils.convertToBigDecimal(taxElem.getAttribute(XML_ATTR_AMOUNT), 0);
            String taxCurrency = taxElem.getAttribute(XML_ATTR_CURRENCYCODE);
            taxJson.put(JSON_PROP_TAXCODE, taxElem.getAttribute("TaxCode"));
            taxJson.put(JSON_PROP_AMOUNT, taxAmt);
            taxJson.put(JSON_PROP_CCYCODE, taxCurrency);
            taxJsonArr.put(taxJson);
            taxesAmount = taxesAmount.add(taxAmt);
            taxesCurrency = taxCurrency;
        }

        taxesJson.put(JSON_PROP_AMOUNT, taxesAmount);
        taxesJson.put(JSON_PROP_CCYCODE, taxesCurrency);
        taxesJson.put(JSON_PROP_TAX, taxJsonArr);
        paxFareJson.put(JSON_PROP_TAXES, taxesJson);

        //----------------------------------------------------------------
        // Fees
        // This code always calculates taxes/fees totals and retrieve currencycode. In SI standardization 3.0, the top level elements for
        // ota:Taxes and ota:Fees parent elements will never have Amount and CurrencyCode attributes. Total amount and currency code for
        // taxes and fees will need to be calculated from child tax/fee elements.

        Element feesElem = XMLUtils.getFirstElementAtXPath(ptcFareBkElem, "./ota:PassengerFare/ota:Fees");
        JSONObject feesJson = new JSONObject();
        BigDecimal feesAmount = new BigDecimal(0);
        String feesCurrency = "";

        JSONArray feeJsonArr = new JSONArray();
        Element[] feeElems = XMLUtils.getElementsAtXPath(feesElem, "./ota:Fee");
        for (Element feeElem : feeElems) {
            JSONObject feeJson = new JSONObject();
            BigDecimal feeAmt = Utils.convertToBigDecimal(feeElem.getAttribute(XML_ATTR_AMOUNT), 0);
            String feeCurrency = feeElem.getAttribute(XML_ATTR_CURRENCYCODE);
            feeJson.put(JSON_PROP_FEECODE, feeElem.getAttribute("FeeCode"));
            feeJson.put(JSON_PROP_AMOUNT, feeAmt);
            feeJson.put(JSON_PROP_CCYCODE, feeCurrency);
            feeJsonArr.put(feeJson);
            feesAmount = feesAmount.add(feeAmt);
            feesCurrency = feeCurrency;
        }

        feesJson.put(JSON_PROP_AMOUNT, feesAmount);
        feesJson.put(JSON_PROP_CCYCODE, feesCurrency);
        feesJson.put(JSON_PROP_FEE, feeJsonArr);
        paxFareJson.put(JSON_PROP_FEES, feesJson);
        //----------------------------------------------------------------

        JSONObject totalFareJson = new JSONObject();
        totalFareJson.put(JSON_PROP_AMOUNT, baseFareAmt.add(taxesAmount).add(feesAmount));
        totalFareJson.put(JSON_PROP_CCYCODE, baseFareCurrency);
        paxFareJson.put(JSON_PROP_TOTALFARE, totalFareJson);

        return paxFareJson;
    }

    public static void getSupplierResponsePricedItinerariesJSON(Element resBodyElem, JSONArray pricedItinsJsonArr) throws Exception {
    	getSupplierResponsePricedItinerariesJSON(resBodyElem, pricedItinsJsonArr, false, 0);
    }
    
    public static void getSupplierResponsePricedItinerariesJSON(Element resBodyElem, JSONArray pricedItinsJsonArr, boolean generateBookRefIdx, int bookRefIdx) throws Exception {
    	boolean isCombinedReturnJourney = Boolean.valueOf(XMLUtils.getValueAtXPath(resBodyElem, "./@CombinedReturnJourney"));
        Element[] pricedItinElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:PricedItineraries/ota:PricedItinerary");
        
        for (Element pricedItinElem : pricedItinElems) {
            JSONObject pricedItinJson = getPricedItineraryJSON(pricedItinElem);

            pricedItinJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath((Element)resBodyElem.getParentNode(), "./air:SupplierID"));
            pricedItinJson.put(JSON_PROP_ISRETURNJRNYCOMBINED, isCombinedReturnJourney);
            if (generateBookRefIdx) {
            	pricedItinJson.put(JSON_PROP_BOOKREFIDX, bookRefIdx);
            }
            pricedItinsJsonArr.put(pricedItinJson);
        }
    }
    
	public static String process(JSONObject reqJson) throws Exception {
        JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
        JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
        
        try {
        	TrackingContext.setTrackingContext(reqJson);
            OperationConfig opConfig = AirConfig.getOperationConfig(TrackingContext.getTrackingContext().getTrackingParameter(ServletContext.RESTPROP_OPERATION));
            Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
            Document ownerDoc = reqElem.getOwnerDocument();

            UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
            List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_TRANSPORT, PROD_CATEG_SUBTYPE_FLIGHT);

            createHeader(reqHdrJson, reqElem);

            int sequence = 1;
            Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
            for (ProductSupplier prodSupplier : prodSuppliers) {
                suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc, sequence++));
            }

            Element travelPrefsElem = XMLUtils.getFirstElementAtXPath(reqElem, "./airi:RequestBody/ota:OTA_AirLowFareSearchRQ/ota:TravelPreferences");
            Element otaReqElem = (Element) travelPrefsElem.getParentNode();
            JSONArray origDestArr = reqBodyJson.getJSONArray(JSON_PROP_ORIGDESTINFO);
            for (int i=0; i < origDestArr.length(); i++) {
                JSONObject origDest = (JSONObject) origDestArr.get(i);
                Element origDestElem = getOriginDestinationElement(ownerDoc, origDest);
                otaReqElem.insertBefore(origDestElem, travelPrefsElem);
            }

            Element cabinPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CabinPref");
            cabinPrefElem.setAttribute("Cabin", reqBodyJson.getString(JSON_PROP_CABINTYPE));
            travelPrefsElem.appendChild(cabinPrefElem);

            Element priceInfoElem = XMLUtils.getFirstElementAtXPath(otaReqElem, "./ota:TravelerInfoSummary/ota:PriceRequestInformation");
            Element travelerInfoElem = (Element) priceInfoElem.getParentNode();
            JSONArray travellerArr = reqBodyJson.getJSONArray(JSON_PROP_PAXINFO);
            for (int i=0; i < travellerArr.length(); i++) {
                JSONObject traveller = (JSONObject) travellerArr.get(i);
                Element travellerElem = getAirTravelerAvailElement(ownerDoc, traveller);
                travelerInfoElem.insertBefore(travellerElem, priceInfoElem);
            }

            Element nbyDepsElem = XMLUtils.getFirstElementAtXPath(travelerInfoElem, "./ota:TPA_Extensions/air:NearbyDepartures");
            Element tpaExtnsElem = (Element) nbyDepsElem.getParentNode();

            Element tripTypeElem = ownerDoc.createElementNS(Constants.NS_AIR, "air:TripType");
            tripTypeElem.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPTYPE));
            tpaExtnsElem.insertBefore(tripTypeElem, nbyDepsElem);

            Element tripIndElem = ownerDoc.createElementNS(Constants.NS_AIR, "air:TripIndicator");
            tripIndElem.setTextContent(reqBodyJson.getString(JSON_PROP_TRIPIND));
            tpaExtnsElem.insertBefore(tripIndElem, nbyDepsElem);
            
            
            Element resElem = null;
            resElem = HTTPServiceConsumer.consumeXMLService(TARGET_SYSTEM_SUPPINTEG, opConfig.getSIServiceURL(), AirConfig.getHttpHeaders(), reqElem);
            if (resElem == null) {
            	throw new Exception("Null response received from SI");
            }
            JSONObject resBodyJson = new JSONObject();
            JSONArray pricedItinsJsonArr = new JSONArray();
            Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./airi:ResponseBody/air:OTA_AirLowFareSearchRSWrapper");
            for (Element wrapperElem : wrapperElems) {
            	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_AirLowFareSearchRS");
            	getSupplierResponsePricedItinerariesJSON(resBodyElem, pricedItinsJsonArr);
            }
            resBodyJson.put(JSON_PROP_PRICEDITIN, pricedItinsJsonArr);
            
            JSONObject resJson = new JSONObject();
            resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
            resJson.put(JSON_PROP_RESBODY, resBodyJson);

            // Call BRMS Supplier and Client Commercials
            JSONObject resSupplierJson =  SupplierCommercials.getSupplierCommercialsV2(reqJson, resJson);
            if (BRMS_STATUS_TYPE_FAILURE.equals(resSupplierJson.getString(JSON_PROP_TYPE))) {
            	logger.error(String.format("A failure response was received from Supplier Commercials calculation engine: %s", resSupplierJson.toString()));
            	return getEmptyResponse(reqHdrJson).toString();
            }
            //**********************************************************************
            // There are no supplier offers for Air. As communicated by Offers team.
            
            JSONObject resClientJson = ClientCommercials.getClientCommercialsV2(reqJson, resJson, resSupplierJson);
            if (BRMS_STATUS_TYPE_FAILURE.equals(resClientJson.getString(JSON_PROP_TYPE))) {
            	logger.error(String.format("A failure response was received from Client Commercials calculation engine: %s", resClientJson.toString()));
            	return getEmptyResponse(reqHdrJson).toString();
            }
            
            calculatePricesV3(reqJson, resJson, resSupplierJson, resClientJson, false,usrCtx);
            
            // Apply company offers
            CompanyOffers.getCompanyOffers(reqJson, resJson, OffersConfig.Type.COMPANY_SEARCH_TIME);

            return resJson.toString();
        }
        catch (Exception x) {
            logger.error("Exception received while processing", x);
            return getEmptyResponse(reqHdrJson).toString();
        }

    }

	public static void createHeader(JSONObject reqHdrJson, Element reqElem) {
		Element sessionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SessionID");
		sessionElem.setTextContent(reqHdrJson.getString(JSON_PROP_SESSIONID));

		Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:TransactionID");
		transactionElem.setTextContent(reqHdrJson.getString(JSON_PROP_TRANSACTID));

		Element userElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:UserID");
		userElem.setTextContent(reqHdrJson.getString(JSON_PROP_USERID));
	}
    
     public static void calculatePricesV3(JSONObject reqJson, JSONObject resJson, JSONObject suppCommResJson, JSONObject clientCommResJson, boolean retainSuppFares, UserContext usrCtx) {
    	//String suppCcyCode = "";
    	Map<String,BigDecimal> paxCountsMap = getPaxCountsFromRequest(reqJson);

        Map<String, String> suppCommToTypeMap = getSupplierCommercialsAndTheirType(suppCommResJson);
        Map<String, String> clntCommToTypeMap = getClientCommercialsAndTheirType(clientCommResJson);
        
        String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
    	String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
    	
    	//----------------------------------------------------------------------
    	// Retrieve array of pricedItinerary from Booking Engine response JSON
    	JSONArray resPricedItinsJsonArr = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_PRICEDITIN);
    	Map<String, JSONArray> ccommSuppBRIJsonMap = getSupplierWiseJourneyDetailsFromClientCommercials(clientCommResJson);
    	Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();
    	
    	// For each pricedItinerary in search results
    	for (int i=0; i < resPricedItinsJsonArr.length(); i++) {
    		JSONObject resPricedItinsJson = resPricedItinsJsonArr.getJSONObject(i);
    		String suppID = resPricedItinsJson.getString(JSON_PROP_SUPPREF);
    		JSONArray ccommJrnyDtlsJsonArr = ccommSuppBRIJsonMap.get(suppID);
    		if (ccommJrnyDtlsJsonArr == null) {
    			logger.info(String.format("BRMS/ClientComm: \"businessRuleIntake\" JSON element for supplier %s not found.", suppID));
    			continue;
    		}
    		String commercialCurrency="";
    		
    		// The BRMS client commercials JSON contains one businessRuleIntake for each supplier. Inside businessRuleIntake, the order
    		// of each supplier search result is maintained within journeyDetails child array. Therefore, keep track of index for each 
    		// businessRuleIntake.journeyDetails for each supplier.
    		int idx = (suppIndexMap.containsKey(suppID)) ? (suppIndexMap.get(suppID) + 1) : 0;
    		suppIndexMap.put(suppID, idx);
    		JSONObject ccommJrnyDtlsJson = ccommJrnyDtlsJsonArr.getJSONObject(idx);
    		
    		// The following PriceComponentsGroup accepts price subcomponents one-by-one and automatically calculates totals
    		PriceComponentsGroup totalFareCompsGroup = new PriceComponentsGroup(JSON_PROP_ITINTOTALFARE, clientCcyCode, new BigDecimal(0), true);
    		JSONArray suppPaxTypeFaresJsonArr = new JSONArray();
    		
    		JSONObject resAirItinPricingInfoJson = resPricedItinsJson.getJSONObject(JSON_PROP_AIRPRICEINFO); 
    		JSONArray paxTypeFaresJsonArr = resAirItinPricingInfoJson.getJSONArray(JSON_PROP_PAXTYPEFARES);
    		
    		//Adding clientCommercialInfo
    		JSONArray clientCommercialItinInfoArr= new JSONArray();
    		
    		for (int j=0; j < paxTypeFaresJsonArr.length(); j++) {
    			JSONObject paxTypeFareJson = paxTypeFaresJsonArr.getJSONObject(j);
    			String paxType = paxTypeFareJson.getString(JSON_PROP_PAXTYPE);
    			String suppCcyCode = paxTypeFareJson.getJSONObject(JSON_PROP_BASEFARE).getString(JSON_PROP_CCYCODE);
    			
    			JSONObject clientCommercialItinInfoJson= new JSONObject();
    			clientCommercialItinInfoJson.put(JSON_PROP_PAXTYPE, paxType);
    			
    			BigDecimal paxCount = paxCountsMap.get(paxType);
    			JSONObject ccommJrnyPsgrDtlJson = getClientCommercialsJourneyDetailsForPassengerType(ccommJrnyDtlsJson, paxType);
    			if (ccommJrnyPsgrDtlJson == null) {
    				// TODO: Log a crying message here. Ideally this part of the code will never be reached.
    				continue;
    			}
    			commercialCurrency = paxTypeFareJson.getJSONObject(JSON_PROP_TOTALFARE).optString(JSON_PROP_CCYCODE);
    			
    			// TODO: Should this be made conditional? Only in case of reprice when (retainSuppFares == true)
    			appendSupplierCommercialsToPaxTypeFares(paxTypeFareJson, ccommJrnyPsgrDtlJson, suppID, suppCcyCode, suppCommToTypeMap);
    			
    			
    			BigDecimal paxTypeTotalFare = new BigDecimal(0);
    			JSONObject commPriceJson = new JSONObject();
    			//suppPaxTypeFaresJsonArr.put(paxTypeFareJson);

    			// From the passenger type client commercial JSON, retrieve calculated client entity commercials
    			JSONArray clientEntityCommJsonArr = ccommJrnyPsgrDtlJson.optJSONArray(JSON_PROP_ENTITYCOMMS);
    			if (clientEntityCommJsonArr == null) {
    				// TODO: Refine this warning message. Maybe log some context information also.
    				logger.warn("Client commercials calculations not found");
    				continue;
    			}
    			
    			// Reference CKIL_323141 - There are three types of client commercials that are 
    			// receivable from clients: Markup, Service Charge (Transaction Fee) & Look-to-Book.
    			// Of these, Markup and Service Charge (Transaction Fee) are transactional and need 
    			// to be considered for selling price.
    			// Reference CKIL_231556 (1.1.2.3/BR04) The display price will be calculated as - 
    			// (Total Supplier Price + Markup + Additional Company Receivable Commercials)
    			
    			JSONArray clientCommercials= new JSONArray();
    			for (int k = (clientEntityCommJsonArr.length() - 1); k >= 0; k--) {
    				JSONObject clientCommercial= new JSONObject();
    				JSONArray clientEntityCommercialsJsonArr=new JSONArray();
    				//JSONObject clientEntityDetail=new JSONObject();
    				JSONObject clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(k);
    				
    				// TODO: In case of B2B, do we need to add additional receivable commercials for all client hierarchy levels?  
    				JSONArray additionalCommsJsonArr = clientEntityCommJson.optJSONArray(JSON_PROP_ADDCOMMDETAILS);
    				if (additionalCommsJsonArr != null) {
    					for (int x=0; x < additionalCommsJsonArr.length(); x++) {
    						JSONObject additionalCommsJson = additionalCommsJsonArr.getJSONObject(x);
    						String additionalCommName = additionalCommsJson.optString(JSON_PROP_COMMNAME);
    						if (COMM_TYPE_RECEIVABLE.equals(clntCommToTypeMap.get(additionalCommName))) {
    							String additionalCommCcy = additionalCommsJson.getString(JSON_PROP_COMMCCY);
    							BigDecimal additionalCommAmt = additionalCommsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
    							paxTypeTotalFare = paxTypeTotalFare.add(additionalCommAmt);
    							totalFareCompsGroup.add(mRcvsPriceCompQualifier.concat(additionalCommName).concat("@").concat(JSON_PROP_CODE), clientCcyCode, additionalCommAmt.multiply(paxCount));
    						}
    					}
    				}
    				
    				// TODO: In case of B2B, do we need to add markups for all client hierarchy levels?
    				JSONObject markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMMDTLS);
    				if (markupCalcJson == null) {
    					continue;
    				}
    				
    				//----------------------------------------------------------------------------------------------------------------------
    				JSONObject clientEntityDetailsJson=new JSONObject();
    		    	JSONObject userCtxJson=usrCtx.toJSON();
    		    	JSONArray clientEntityDetailsArr = userCtxJson.getJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS);
    				for(int y=0;y<clientEntityDetailsArr.length();y++) {
    					/*if(clientEntityDetailsArr.getJSONObject(y).opt(JSON_PROP_COMMENTITYID)!=null)
    					{
    					if(clientEntityDetailsArr.getJSONObject(y).opt(JSON_PROP_COMMENTITYID).toString().equalsIgnoreCase(clientEntityCommJson.get("entityName").toString()))
    					{
    						clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
    					}
    					}*/
					//TODO:Add a check later
    					
    					clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
    					
    					
    				}
    				//TODO:Add client ID, if not found?
    			
    				
    				
    				//markup commercialcalc clientCommercial
    				clientCommercial.put(JSON_PROP_COMMTYPE, clntCommToTypeMap.get(markupCalcJson.get(JSON_PROP_COMMNAME).toString()));
    				clientCommercial.put(JSON_PROP_COMMAMOUNT, markupCalcJson.get(JSON_PROP_COMMAMOUNT).toString());
    				clientCommercial.put(JSON_PROP_COMMNAME,markupCalcJson.get(JSON_PROP_COMMNAME));
    				
    				/*if((markupCalcJson.get(JSON_PROP_COMMCALCPCT).toString())!=null)
    				{
    					clientCommercial.put(JSON_PROP_COMMCCY,markupCalcJson.get(JSON_PROP_COMMCCY).toString());
    				}
    				else {
    					clientCommercial.put(JSON_PROP_COMMCCY,commercialCurrency);
    				}*/
    				clientCommercial.put(JSON_PROP_COMMCCY,markupCalcJson.optString(JSON_PROP_COMMCCY, commercialCurrency));
    				clientEntityCommercialsJsonArr.put(clientCommercial);
    				
    				
    				//Additional commercialcalc clientCommercial
    				
    				if((additionalCommsJsonArr!=null)&& (!(additionalCommsJsonArr.length()<0))){
    					for(int p=0;p<additionalCommsJsonArr.length();p++) {
    						clientCommercial=new JSONObject();
    						JSONObject additionalCommJson=additionalCommsJsonArr.getJSONObject(p);
    						clientCommercial.put(JSON_PROP_COMMTYPE, clntCommToTypeMap.get(markupCalcJson.get(JSON_PROP_COMMNAME).toString()));
    	    				clientCommercial.put(JSON_PROP_COMMAMOUNT, additionalCommJson.get(JSON_PROP_COMMAMOUNT).toString());
    	    				clientCommercial.put(JSON_PROP_COMMNAME,additionalCommJson.get(JSON_PROP_COMMNAME));
    	    				clientCommercial.put(JSON_PROP_COMMCCY,additionalCommJson.get(JSON_PROP_COMMCCY).toString());
    	    				clientEntityCommercialsJsonArr.put(clientCommercial);
    					}
    				}
    				
    				
    				clientEntityDetailsJson.put(JSON_PROP_CLIENTCOMM, clientEntityCommercialsJsonArr);
    				clientCommercials.put(clientEntityDetailsJson);
    				//----------------------------------------------------------------------------------------------------------------------
    				
    				JSONObject fareBreakupJson = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP); 
    				JSONArray ccommTaxDetailsJsonArr = fareBreakupJson.getJSONArray(JSON_PROP_TAXDETAILS);
    				commPriceJson.put(JSON_PROP_PAXTYPE, paxTypeFareJson.getString(JSON_PROP_PAXTYPE));
    				
    				JSONObject baseFareJson = new JSONObject();
    				baseFareJson.put(JSON_PROP_AMOUNT, fareBreakupJson.getBigDecimal(JSON_PROP_BASEFARE_COMM).multiply(RedisRoEData.getRateOfExchange(suppCcyCode, clientCcyCode, clientMarket)));
    				baseFareJson.put(JSON_PROP_CCYCODE, clientCcyCode);
    				commPriceJson.put(JSON_PROP_BASEFARE, baseFareJson);
    				paxTypeTotalFare = paxTypeTotalFare.add(baseFareJson.getBigDecimal(JSON_PROP_AMOUNT));
    				totalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, baseFareJson.getBigDecimal(JSON_PROP_AMOUNT).multiply(paxCount));
    				
    				int offset = 0;
    				JSONArray paxTypeTaxJsonArr = paxTypeFareJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
    				JSONObject taxesJson = getCommercialPricesTaxesJson(paxTypeTaxJsonArr, ccommTaxDetailsJsonArr, offset, totalFareCompsGroup, paxCount, clientCcyCode, clientMarket);
    				commPriceJson.put(JSON_PROP_TAXES,  taxesJson);
    				paxTypeTotalFare = paxTypeTotalFare.add(taxesJson.getBigDecimal(JSON_PROP_TOTAL));
    				
    				offset = paxTypeTaxJsonArr.length();
    				JSONArray paxTypeFeeJsonArr = paxTypeFareJson.getJSONObject(JSON_PROP_FEES).getJSONArray(JSON_PROP_FEE);
    				JSONObject feesJson = getCommercialPricesFeesJson(paxTypeFeeJsonArr, ccommTaxDetailsJsonArr, offset, totalFareCompsGroup, paxCount, clientCcyCode, clientMarket);
    				commPriceJson.put(JSON_PROP_FEES,  feesJson);
    				paxTypeTotalFare = paxTypeTotalFare.add(feesJson.getBigDecimal(JSON_PROP_TOTAL));
    			}
    			
    			clientCommercialItinInfoJson.put(JSON_PROP_CLIENTENTITYCOMMS, clientCommercials);
    			clientCommercialItinInfoArr.put(clientCommercialItinInfoJson);
    			
    			suppPaxTypeFaresJsonArr.put(paxTypeFareJson);
    			JSONObject totalFareJson = new JSONObject();
    			totalFareJson.put(JSON_PROP_AMOUNT, paxTypeTotalFare);
    			totalFareJson.put(JSON_PROP_CCYCODE, clientCcyCode);
    			commPriceJson.put(JSON_PROP_TOTALFARE, totalFareJson);
    			
    			paxTypeFaresJsonArr.put(j, commPriceJson);
    		}
    		
    		//logger.trace(String.format("Calculated Total Fare: %s", totalFareCompsGroup.toJSON().toString()));
    		// Calculate ItinTotalFare. This fare will be the one used for sorting.
    		resAirItinPricingInfoJson.put(JSON_PROP_ITINTOTALFARE, totalFareCompsGroup.toJSON());

    		// The supplier fares will be retained only in reprice operation. In reprice, after calculations, supplier
    		// prices are saved in Redis cache to be used im book operation.
    		if (retainSuppFares) {
	    		JSONObject suppItinPricingInfoJson  = new JSONObject();
	    		JSONObject suppItinTotalFareJson = new JSONObject();
	    		suppItinPricingInfoJson.put(JSON_PROP_ITINTOTALFARE, suppItinTotalFareJson);
	    		suppItinPricingInfoJson.put(JSON_PROP_PAXTYPEFARES, suppPaxTypeFaresJsonArr);
	    		resPricedItinsJson.put(JSON_PROP_CLIENTCOMMITININFO, clientCommercialItinInfoArr);
	    		resPricedItinsJson.put(JSON_PROP_SUPPPRICEINFO, suppItinPricingInfoJson);
	    		addSupplierItinTotalFare(resPricedItinsJson, paxCountsMap);
    		}    		 
    	}
    }
    
     public static void calculatePricesV4(JSONObject reqJson, JSONObject resJson, JSONObject suppCommResJson, JSONObject clientCommResJson, boolean retainSuppFares, UserContext usrCtx) {
    	Map<String,BigDecimal> paxCountsMap = getPaxCountsFromRequest(reqJson);
        Map<String, String> suppCommToTypeMap = getSupplierCommercialsAndTheirType(suppCommResJson);
        Map<String, String> clntCommToTypeMap = getClientCommercialsAndTheirType(clientCommResJson);
        
        String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
    	String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
    	
    	//----------------------------------------------------------------------
    	// Retrieve array of pricedItinerary from SI XML converted response JSON
    	JSONArray resPricedItinsJsonArr = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_PRICEDITIN);
    	Map<String, JSONArray> ccommSuppBRIJsonMap = getSupplierWiseJourneyDetailsFromClientCommercials(clientCommResJson);
    	Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();
    	
    	// For each pricedItinerary in search results
    	for (int i=0; i < resPricedItinsJsonArr.length(); i++) {
    		JSONObject resPricedItinsJson = resPricedItinsJsonArr.getJSONObject(i);
    		String suppID = resPricedItinsJson.getString(JSON_PROP_SUPPREF);
    		JSONObject resAirItinPricingInfoJson = resPricedItinsJson.getJSONObject(JSON_PROP_AIRPRICEINFO); 
    		
    		// The BRMS client commercials response JSON contains one businessRuleIntake for each supplier. Inside businessRuleIntake, the 
    		// order of each supplier search result is maintained within journeyDetails child array. Therefore, keep track of index for each 
    		// businessRuleIntake.journeyDetails for each supplier.
    		int idx = (suppIndexMap.containsKey(suppID)) ? (suppIndexMap.get(suppID) + 1) : 0;
    		suppIndexMap.put(suppID, idx);
    		JSONObject ccommJrnyDtlsJson = (ccommSuppBRIJsonMap.containsKey(suppID)) ? ccommSuppBRIJsonMap.get(suppID).getJSONObject(idx) : null;
    		if (ccommJrnyDtlsJson == null) {
    			logger.info(String.format("BRMS/ClientComm: \"businessRuleIntake\" JSON element for supplier %s not found.", suppID));
    		}

    		
    		// The following PriceComponentsGroup accepts price subcomponents one-by-one and automatically calculates totals
    		PriceComponentsGroup totalFareCompsGroup = new PriceComponentsGroup(JSON_PROP_ITINTOTALFARE, clientCcyCode, new BigDecimal(0), true);
    		JSONArray suppPaxTypeFaresJsonArr = new JSONArray();
    		
    		//Adding clientCommercialInfo
    		JSONArray clientCommercialItinInfoArr= new JSONArray();
    		JSONArray paxTypeFaresJsonArr = resAirItinPricingInfoJson.getJSONArray(JSON_PROP_PAXTYPEFARES);
    		for (int j=0; j < paxTypeFaresJsonArr.length(); j++) {
    			JSONObject paxTypeFareJson = paxTypeFaresJsonArr.getJSONObject(j);
    			JSONObject paxTypeBaseFareJson = paxTypeFareJson.getJSONObject(JSON_PROP_BASEFARE);
    			String paxType = paxTypeFareJson.getString(JSON_PROP_PAXTYPE);
    			BigDecimal paxCount = paxCountsMap.get(paxType);
    			String suppCcyCode = paxTypeBaseFareJson.getString(JSON_PROP_CCYCODE);
    			
    			JSONObject ccommJrnyPsgrDtlJson = getClientCommercialsJourneyDetailsForPassengerType(ccommJrnyDtlsJson, paxType);
    			JSONArray clientEntityCommJsonArr = null;
    			if (ccommJrnyPsgrDtlJson == null) {
    				logger.info(String.format("Passenger type %s details not found client commercial journeyDetails", paxType));
    			}
    			else {
        			// Only in case of reprice when (retainSuppFares == true), retain supplier commercial
        			if (retainSuppFares) {
        				appendSupplierCommercialsToPaxTypeFares(paxTypeFareJson, ccommJrnyPsgrDtlJson, suppID, suppCcyCode, suppCommToTypeMap);
        			}
        			// From the passenger type client commercial JSON, retrieve calculated client entity commercials
        			//JSONArray clientEntityCommJsonArr = ccommJrnyPsgrDtlJson.optJSONArray(JSON_PROP_ENTITYCOMMS);
    				clientEntityCommJsonArr = ccommJrnyPsgrDtlJson.optJSONArray(JSON_PROP_ENTITYCOMMS);
        			if (clientEntityCommJsonArr == null) {
        				logger.warn("Client commercials calculations not found");
        			}
    			}
    			
    			BigDecimal paxTypeTotalFare = new BigDecimal(0);
    			JSONObject commPriceJson = new JSONObject();
    			//suppPaxTypeFaresJsonArr.put(paxTypeFareJson);

    			// Reference CKIL_323141 - There are three types of client commercials that are 
    			// receivable from clients: Markup, Service Charge (Transaction Fee) & Look-to-Book.
    			// Of these, Markup and Service Charge (Transaction Fee) are transactional and need 
    			// to be considered for selling price.
    			// Reference CKIL_231556 (1.1.2.3/BR04) The display price will be calculated as - 
    			// (Total Supplier Price + Markup + Additional Company Receivable Commercials)
    			
    			JSONObject markupCalcJson = null;
    			JSONArray clientCommercials= new JSONArray();
    			PriceComponentsGroup paxReceivablesCompsGroup = null;
    			PriceComponentsGroup totalPaxReceivablesCompsGroup = null;
    			for (int k = 0; clientEntityCommJsonArr != null && k < clientEntityCommJsonArr.length(); k++) {
    				JSONArray clientEntityCommercialsJsonArr=new JSONArray();
    				JSONObject clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(k);
    				
    				// TODO: In case of B2B, do we need to add markups for all client hierarchy levels?
    				if (clientEntityCommJson.has(JSON_PROP_MARKUPCOMMDTLS)) {
    					markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMMDTLS);
    					clientEntityCommercialsJsonArr.put(convertToClientEntityCommercialJson(markupCalcJson, clntCommToTypeMap, suppCcyCode));
    				}
    				
    		    	
    				//Additional commercialcalc clientCommercial
    				// TODO: In case of B2B, do we need to add additional receivable commercials for all client hierarchy levels?  
    				JSONArray additionalCommsJsonArr = clientEntityCommJson.optJSONArray(JSON_PROP_ADDCOMMDETAILS);
    				// If totals of receivables at all levels is required, the following instance creation needs to move where
    				// variable 'paxReceivablesCompsGroup' is declared
					paxReceivablesCompsGroup = new PriceComponentsGroup(JSON_PROP_RECEIVABLES, clientCcyCode, new BigDecimal(0), true);
					totalPaxReceivablesCompsGroup = new PriceComponentsGroup(JSON_PROP_RECEIVABLES, clientCcyCode, new BigDecimal(0), true);
    				if (additionalCommsJsonArr != null) {
    					for(int p=0; p < additionalCommsJsonArr.length(); p++) {
    						JSONObject additionalCommJson = additionalCommsJsonArr.getJSONObject(p);
    						String additionalCommName = additionalCommJson.optString(JSON_PROP_COMMNAME);
    						String additionalCommType = clntCommToTypeMap.get(additionalCommName);
    						clientEntityCommercialsJsonArr.put(convertToClientEntityCommercialJson(additionalCommJson, clntCommToTypeMap, suppCcyCode));
    	    				
    						if (COMM_TYPE_RECEIVABLE.equals(additionalCommType)) {
    							String additionalCommCcy = additionalCommJson.getString(JSON_PROP_COMMCCY);
    							BigDecimal additionalCommAmt = additionalCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
    							paxReceivablesCompsGroup.add(JSON_PROP_RECEIVABLE.concat(".").concat(additionalCommName).concat("@").concat(JSON_PROP_CODE), clientCcyCode, additionalCommAmt);
    							totalPaxReceivablesCompsGroup.add(JSON_PROP_RECEIVABLE.concat(".").concat(additionalCommName).concat("@").concat(JSON_PROP_CODE), clientCcyCode, additionalCommAmt.multiply(paxCount));
    						}
    					}
    				}

    				JSONObject clientEntityDetailsJson = new JSONObject();
    				JSONArray clientEntityDetailsArr = usrCtx.getClientCommercialsHierarchy();
    				clientEntityDetailsJson = clientEntityDetailsArr.getJSONObject(k);
    				clientEntityDetailsJson.put(JSON_PROP_CLIENTCOMM, clientEntityCommercialsJsonArr);
    				clientCommercials.put(clientEntityDetailsJson);
    			}

				//------------------------BEGIN----------------------------------
				BigDecimal baseFareAmt = paxTypeBaseFareJson.getBigDecimal(JSON_PROP_AMOUNT);
				JSONArray ccommTaxDetailsJsonArr = null; 
				if (markupCalcJson != null) {
					JSONObject fareBreakupJson = markupCalcJson.optJSONObject(JSON_PROP_FAREBREAKUP);
					if (fareBreakupJson != null) {
						baseFareAmt = fareBreakupJson.optBigDecimal(JSON_PROP_BASEFARE_COMM, baseFareAmt);
						ccommTaxDetailsJsonArr = fareBreakupJson.optJSONArray(JSON_PROP_TAXDETAILS);
					}
				}
				
				commPriceJson.put(JSON_PROP_PAXTYPE, paxTypeFareJson.getString(JSON_PROP_PAXTYPE));
				
				JSONObject baseFareJson = new JSONObject();
				baseFareJson.put(JSON_PROP_AMOUNT, baseFareAmt.multiply(RedisRoEData.getRateOfExchange(suppCcyCode, clientCcyCode, clientMarket)));
				baseFareJson.put(JSON_PROP_CCYCODE, clientCcyCode);
				commPriceJson.put(JSON_PROP_BASEFARE, baseFareJson);
				paxTypeTotalFare = paxTypeTotalFare.add(baseFareJson.getBigDecimal(JSON_PROP_AMOUNT));
				totalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, baseFareJson.getBigDecimal(JSON_PROP_AMOUNT).multiply(paxCount));
				
				int offset = 0;
				JSONArray paxTypeTaxJsonArr = paxTypeFareJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
				JSONObject taxesJson = getCommercialPricesTaxesJson(paxTypeTaxJsonArr, ccommTaxDetailsJsonArr, offset, totalFareCompsGroup, paxCount, clientCcyCode, clientMarket);
				commPriceJson.put(JSON_PROP_TAXES,  taxesJson);
				paxTypeTotalFare = paxTypeTotalFare.add(taxesJson.getBigDecimal(JSON_PROP_TOTAL));
				
				offset = paxTypeTaxJsonArr.length();
				JSONArray paxTypeFeeJsonArr = paxTypeFareJson.getJSONObject(JSON_PROP_FEES).getJSONArray(JSON_PROP_FEE);
				JSONObject feesJson = getCommercialPricesFeesJson(paxTypeFeeJsonArr, ccommTaxDetailsJsonArr, offset, totalFareCompsGroup, paxCount, clientCcyCode, clientMarket);
				commPriceJson.put(JSON_PROP_FEES,  feesJson);
				paxTypeTotalFare = paxTypeTotalFare.add(feesJson.getBigDecimal(JSON_PROP_TOTAL));
				
				// If amount of receivables group is greater than zero, then append to commercial prices
				if (paxReceivablesCompsGroup != null && paxReceivablesCompsGroup.getComponentAmount().compareTo(BigDecimal.ZERO) == 1) {
					paxTypeTotalFare = paxTypeTotalFare.add(paxReceivablesCompsGroup.getComponentAmount());
					totalFareCompsGroup.addSubComponent(totalPaxReceivablesCompsGroup, null);
					commPriceJson.put(JSON_PROP_RECEIVABLES, paxReceivablesCompsGroup.toJSON());
				}
				//-------------------------END-----------------------------------

    			JSONObject clientCommercialItinInfoJson= new JSONObject();
    			clientCommercialItinInfoJson.put(JSON_PROP_PAXTYPE, paxType);
    			clientCommercialItinInfoJson.put(JSON_PROP_CLIENTENTITYCOMMS, clientCommercials);
    			clientCommercialItinInfoArr.put(clientCommercialItinInfoJson);
    			
    			suppPaxTypeFaresJsonArr.put(paxTypeFareJson);
    			JSONObject totalFareJson = new JSONObject();
    			totalFareJson.put(JSON_PROP_AMOUNT, paxTypeTotalFare);
    			totalFareJson.put(JSON_PROP_CCYCODE, clientCcyCode);
    			commPriceJson.put(JSON_PROP_TOTALFARE, totalFareJson);
    			
    			paxTypeFaresJsonArr.put(j, commPriceJson);
    		}
    		
    		// Calculate ItinTotalFare. This fare will be the one used for sorting.
    		resAirItinPricingInfoJson.put(JSON_PROP_ITINTOTALFARE, totalFareCompsGroup.toJSON());

    		// The supplier fares will be retained only in reprice operation. In reprice, after calculations, supplier
    		// prices are saved in Redis cache to be used im book operation.
    		if (retainSuppFares) {
	    		JSONObject suppItinPricingInfoJson  = new JSONObject();
	    		JSONObject suppItinTotalFareJson = new JSONObject();
	    		suppItinPricingInfoJson.put(JSON_PROP_ITINTOTALFARE, suppItinTotalFareJson);
	    		suppItinPricingInfoJson.put(JSON_PROP_PAXTYPEFARES, suppPaxTypeFaresJsonArr);
	    		resPricedItinsJson.put(JSON_PROP_CLIENTCOMMITININFO, clientCommercialItinInfoArr);
	    		resPricedItinsJson.put(JSON_PROP_SUPPPRICEINFO, suppItinPricingInfoJson);
	    		addSupplierItinTotalFare(resPricedItinsJson, paxCountsMap);
    		}    		 
    	}
    }
    

    private static void addSupplierItinTotalFare(JSONObject pricedItinJson, Map<String, BigDecimal> paxInfoMap) {
		JSONObject suppItinPricingInfoJson = pricedItinJson.getJSONObject(JSON_PROP_SUPPPRICEINFO);
		JSONArray paxTypeFaresArr = suppItinPricingInfoJson.getJSONArray(JSON_PROP_PAXTYPEFARES);

		Map<String, JSONObject> suppCommTotalsMap = new HashMap<String, JSONObject>();
		Map<String, JSONObject> clientCommTotalsMap = new HashMap<String, JSONObject>();
		BigDecimal totalFareAmt = new BigDecimal(0);
	
		String ccyCode = null;
		JSONObject clientEntityTotalCommercials=null;
		JSONArray totalClientArr= new JSONArray();
		for (int i = 0; i < paxTypeFaresArr.length(); i++) {
			JSONObject paxTypeFare = paxTypeFaresArr.getJSONObject(i);
			JSONObject paxTypeTotalFareJson = paxTypeFare.getJSONObject(JSON_PROP_TOTALFARE);
			totalFareAmt = totalFareAmt.add(paxTypeTotalFareJson.getBigDecimal(JSON_PROP_AMOUNT).multiply(paxInfoMap.get(paxTypeFare.get(JSON_PROP_PAXTYPE))));
			ccyCode = (ccyCode == null) ? paxTypeTotalFareJson.getString(JSON_PROP_CCYCODE) : ccyCode;
			
			JSONArray suppCommJsonArr = paxTypeFare.optJSONArray(JSON_PROP_SUPPCOMM);
			//the order of clientCommercialItinInfo will same as that of normal paxTypeFares
			JSONObject clientItinInfoJson=pricedItinJson.getJSONArray(JSON_PROP_CLIENTCOMMITININFO).getJSONObject(i);
			JSONArray clientCommJsonArr=clientItinInfoJson.optJSONArray(JSON_PROP_CLIENTENTITYCOMMS);
			
			// If no supplier commercials have been defined in BRMS, the JSONArray for suppCommJsonArr will be null.
			// In this case, log a message and proceed with other calculations.
			if (suppCommJsonArr == null) {
				logger.warn("No supplier commercials found");
			}
			else {
				for (int j=0; j < suppCommJsonArr.length(); j++) {
					JSONObject suppCommJson = suppCommJsonArr.getJSONObject(j);
					String suppCommName = suppCommJson.getString(JSON_PROP_COMMNAME);
					JSONObject suppCommTotalsJson = null;
					if (suppCommTotalsMap.containsKey(suppCommName)) {
						suppCommTotalsJson = suppCommTotalsMap.get(suppCommName);
						suppCommTotalsJson.put(JSON_PROP_COMMAMOUNT, suppCommTotalsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).add(suppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(paxInfoMap.get(paxTypeFare.get(JSON_PROP_PAXTYPE)))));
					}
					else {
						suppCommTotalsJson = new JSONObject();
						suppCommTotalsJson.put(JSON_PROP_COMMNAME, suppCommName);
						suppCommTotalsJson.put(JSON_PROP_COMMTYPE, suppCommJson.getString(JSON_PROP_COMMTYPE));
						suppCommTotalsJson.put(JSON_PROP_COMMAMOUNT, suppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(paxInfoMap.get(paxTypeFare.get(JSON_PROP_PAXTYPE))));
						suppCommTotalsJson.put(JSON_PROP_COMMCCY,suppCommJson.get(JSON_PROP_COMMCCY).toString());
						suppCommTotalsMap.put(suppCommName, suppCommTotalsJson);
					}
				}
			}
			
			if (clientCommJsonArr == null) {
				logger.warn("No client commercials found");
			}
			else {
				for (int l=0; l < clientCommJsonArr.length(); l++) {
					//TODO:Add jsonElements from clientEntity once they can be fetched.
					JSONObject clientCommJson = clientCommJsonArr.getJSONObject(l);
					JSONObject clientCommEntJson= new JSONObject();
					
					//JSONObject clientCommJson =clientEntityCommJson.getJSONObject("clientCommercial");
					//clientCommEntJson
					JSONArray clientEntityCommJsonArr=clientCommJson.getJSONArray(JSON_PROP_CLIENTCOMM);
					
					
					
					JSONObject clientCommTotalsJson = null;
					JSONArray clientTotalEntityArray=new JSONArray();
					
					for(int m=0;m<clientEntityCommJsonArr.length();m++) {
						
						JSONObject clientCommEntityJson=clientEntityCommJsonArr.getJSONObject(m);
						String clientCommName = clientCommEntityJson.getString(JSON_PROP_COMMNAME);
						
						
						if (clientCommTotalsMap.containsKey(clientCommName)) {
							clientEntityTotalCommercials= clientCommTotalsMap.get(clientCommName);
							//clientCommTotalsJson=clientEntityTotalCommercials.getJSONObject("clientCommercialsTotal");
							//clientCommTotalsJson = clientCommTotalsMap.get(clientCommName);
							clientEntityTotalCommercials.put(JSON_PROP_COMMAMOUNT, clientEntityTotalCommercials.getBigDecimal(JSON_PROP_COMMAMOUNT).add(clientCommEntityJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(paxInfoMap.get(paxTypeFare.get(JSON_PROP_PAXTYPE)))));
						}
						else {
							clientEntityTotalCommercials= new JSONObject();
							clientCommTotalsJson = new JSONObject();
							clientCommTotalsJson.put(JSON_PROP_COMMNAME, clientCommName);
							clientCommTotalsJson.put(JSON_PROP_COMMTYPE, clientCommEntityJson.getString(JSON_PROP_COMMTYPE));
							clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, clientCommEntityJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(paxInfoMap.get(paxTypeFare.get(JSON_PROP_PAXTYPE))));
							clientCommTotalsJson.put(JSON_PROP_COMMCCY,clientCommEntityJson.get(JSON_PROP_COMMCCY).toString());
							clientTotalEntityArray.put(clientCommTotalsJson);
						
						
							clientCommTotalsMap.put(clientCommName, clientCommTotalsJson);
						}
					}
					
					if((clientTotalEntityArray!=null) && (clientTotalEntityArray.length()>0) ) {
						clientCommEntJson.put("clientCommercialsTotal", clientTotalEntityArray);
						clientCommEntJson.put(JSON_PROP_CLIENTID, clientCommJson.optString(JSON_PROP_CLIENTID,""));
						clientCommEntJson.put(JSON_PROP_PARENTCLIENTID, clientCommJson.optString(JSON_PROP_PARENTCLIENTID,""));
						clientCommEntJson.put(JSON_PROP_COMMENTITYTYPE, clientCommJson.optString(JSON_PROP_COMMENTITYTYPE,""));
						clientCommEntJson.put(JSON_PROP_COMMENTITYID, clientCommJson.optString(JSON_PROP_COMMENTITYID,""));
						
						totalClientArr.put(clientCommEntJson);
					}
				}
			}
		}

		// Convert map of Commercial Head to Commercial Amount to JSONArray and append in suppItinPricingInfoJson
		JSONArray suppCommTotalsJsonArr = new JSONArray();
		Iterator<Entry<String, JSONObject>> suppCommTotalsIter = suppCommTotalsMap.entrySet().iterator();
		while (suppCommTotalsIter.hasNext()) {
			suppCommTotalsJsonArr.put(suppCommTotalsIter.next().getValue());
		}
		suppItinPricingInfoJson.put(JSON_PROP_SUPPCOMMTOTALS, suppCommTotalsJsonArr);
		
	/*	JSONArray clientCommTotalsJsonArr = new JSONArray();
		Iterator<Entry<String, JSONObject>> clientCommTotalsIter = clientCommTotalsMap.entrySet().iterator();
		while (clientCommTotalsIter.hasNext()) {
			clientCommTotalsJsonArr.put(clientCommTotalsIter.next().getValue());
		}*/
		pricedItinJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, totalClientArr);
		
		JSONObject itinTotalFare = suppItinPricingInfoJson.getJSONObject(JSON_PROP_ITINTOTALFARE);
		itinTotalFare.put(JSON_PROP_CCYCODE, ccyCode);
		itinTotalFare.put(JSON_PROP_AMOUNT, totalFareAmt);
	}

    private static JSONArray getClientCommercialsBusinessRuleIntakeJSONArray(JSONObject commResJson) {
    	return commResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.air_commercialscalculationengine.clienttransactionalrules.Root").getJSONArray("businessRuleIntake");
    }
    
    private static JSONArray getSupplierCommercialsBusinessRuleIntakeJSONArray(JSONObject commResJson) {
    	return commResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.air_commercialscalculationengine.suppliertransactionalrules.Root").getJSONArray("businessRuleIntake");
    }

    // Retrieve commercials head array from client commercials and find type (Receivable, Payable) for commercials 
    private static Map<String, String> getClientCommercialsAndTheirType(JSONObject clientCommResJson) {
        JSONArray commHeadJsonArr = null;
        JSONObject commHeadJson = null;
        JSONArray entityDtlsJsonArr = null;
        Map<String, String> commToTypeMap = new HashMap<String, String>();
        JSONArray ccommBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(clientCommResJson);
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

    // Retrieve commercials head array from supplier commercials and find type (Receivable, Payable) for commercials 
    private static Map<String, String> getSupplierCommercialsAndTheirType(JSONObject suppCommResJson) {
        JSONArray commHeadJsonArr = null;
        JSONObject commHeadJson = null;
        Map<String, String> commToTypeMap = new HashMap<String, String>();
        JSONArray scommBRIJsonArr = getSupplierCommercialsBusinessRuleIntakeJSONArray(suppCommResJson);
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

	// Retrieve array of businessRuleIntake.journeyDetails for each supplier from Client Commercials response JSON
    private static Map<String, JSONArray> getSupplierWiseJourneyDetailsFromClientCommercials(JSONObject clientCommResJson) {
    	JSONArray ccommSuppBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(clientCommResJson);
    	Map<String, JSONArray> ccommSuppBRIJsonMap = new HashMap<String, JSONArray>();
    	for (int i=0; i < ccommSuppBRIJsonArr.length(); i++) {
    		JSONObject ccommSuppBRIJson = ccommSuppBRIJsonArr.getJSONObject(i);
    		String suppID = ccommSuppBRIJson.getJSONObject(JSON_PROP_COMMONELEMS).getString(JSON_PROP_SUPP);
    		JSONArray ccommJrnyDtlsJsonArr = ccommSuppBRIJson.getJSONArray(JSON_PROP_JOURNEYDETAILS);
    		ccommSuppBRIJsonMap.put(suppID, ccommJrnyDtlsJsonArr);
    	}
    	
    	return ccommSuppBRIJsonMap;
    }
    
    static Map<String,BigDecimal> getPaxCountsFromRequest(JSONObject reqJson) {
    	Map<String,BigDecimal> paxInfoMap=new LinkedHashMap<String,BigDecimal>();
        JSONObject paxInfo=null;

        JSONArray reqPaxInfoJsonArr = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXINFO);
        for(int i=0;i<reqPaxInfoJsonArr.length();i++) {
        	paxInfo = reqPaxInfoJsonArr.getJSONObject(i);
        	paxInfoMap.put(paxInfo.getString(JSON_PROP_PAXTYPE), new BigDecimal(paxInfo.getInt(JSON_PROP_QTY)));
        }

        return paxInfoMap;
    }
    
    private static JSONObject getClientCommercialsJourneyDetailsForPassengerType(JSONObject ccommJrnyDtlsJson, String paxType) {
		if (ccommJrnyDtlsJson == null || paxType == null) {
			return null;
		}
		
		// Search this paxType in client commercials journeyDetails 
		JSONArray ccommJrnyPsgrDtlsJsonArr = ccommJrnyDtlsJson.getJSONArray(JSON_PROP_PSGRDETAILS);
		for (int k=0; k < ccommJrnyPsgrDtlsJsonArr.length(); k++) {
			JSONObject ccommJrnyPsgrDtlsJson = ccommJrnyPsgrDtlsJsonArr.getJSONObject(k);
			if (paxType.equals(ccommJrnyPsgrDtlsJson.getString(JSON_PROP_PSGRTYPE))) {
				return ccommJrnyPsgrDtlsJson;
			}
		}
		
		return null;
    }
    
    private static JSONObject getCommercialPricesTaxesJson(JSONArray paxTypeTaxJsonArr, JSONArray ccommTaxDetailsJsonArr, int offset, PriceComponentsGroup totalFareCompsGroup, BigDecimal paxCount, String clientCcyCode, String clientMarket) {
		BigDecimal taxesTotal = new BigDecimal(0);
		JSONObject taxesJson = new JSONObject();
		JSONArray taxJsonArr = new JSONArray();
		String suppCcyCode = null;
		String taxCode = null;
		
		JSONObject ccommTaxDetailJson = null; 
		JSONObject paxTypeTaxJson = null;
		JSONObject taxJson = null;
		for (int l=0; l < paxTypeTaxJsonArr.length(); l++) {
			paxTypeTaxJson = paxTypeTaxJsonArr.getJSONObject(l);
			suppCcyCode = paxTypeTaxJson.getString(JSON_PROP_CCYCODE);
			taxCode = paxTypeTaxJson.getString(JSON_PROP_TAXCODE);

			// Access the tax array returned by client commercials in a positional manner instead of 
			// searching in that array using taxcode/feecode.
				taxJson = paxTypeTaxJson;
			ccommTaxDetailJson = (ccommTaxDetailsJsonArr != null) ? ccommTaxDetailsJsonArr.optJSONObject(offset + l) : null;
			if (ccommTaxDetailJson != null) {
				// If tax JSON is found in commercials, replace existing tax details with one from commercials
			BigDecimal taxAmt = ccommTaxDetailJson.getBigDecimal(JSON_PROP_TAXVALUE);
			taxJson = new JSONObject();
			taxJson.put(JSON_PROP_TAXCODE, taxCode);
			taxJson.put(JSON_PROP_AMOUNT, taxAmt.multiply(RedisRoEData.getRateOfExchange(suppCcyCode, clientCcyCode, clientMarket)));
			taxJson.put(JSON_PROP_CCYCODE, clientCcyCode);
			}

			taxJsonArr.put(taxJson);
			taxesTotal = taxesTotal.add(taxJson.getBigDecimal(JSON_PROP_AMOUNT));
			totalFareCompsGroup.add(mTaxesPriceCompQualifier.concat(taxCode).concat("@").concat(JSON_PROP_TAXCODE), clientCcyCode, taxJson.getBigDecimal(JSON_PROP_AMOUNT).multiply(paxCount));
		}
		
		taxesJson.put(JSON_PROP_TAX, taxJsonArr);
		taxesJson.put(JSON_PROP_TOTAL, taxesTotal);
		taxesJson.put(JSON_PROP_CCYCODE, clientCcyCode);
		return taxesJson;
    }
    
    private static JSONObject getCommercialPricesFeesJson(JSONArray paxTypeFeeJsonArr, JSONArray ccommTaxDetailsJsonArr, int offset, PriceComponentsGroup totalFareCompsGroup, BigDecimal paxCount, String clientCcyCode, String clientMarket) {
		BigDecimal feesTotal = new BigDecimal(0);
		JSONObject feesJson = new JSONObject();
		JSONArray feeJsonArr = new JSONArray();
		String suppCcyCode = null;
		String feeCode = null;

		JSONObject paxTypeFeeJson = null;
		JSONObject feeJson = null;
		for (int l=0; l < paxTypeFeeJsonArr.length(); l++) {
			paxTypeFeeJson = paxTypeFeeJsonArr.getJSONObject(l);
			suppCcyCode = paxTypeFeeJson.getString(JSON_PROP_CCYCODE);
			feeCode = paxTypeFeeJson.getString(JSON_PROP_FEECODE);
			// Access the tax array returned by client commercials in a positional manner instead of 
			// searching in that array using taxcode/feecode.
				feeJson = paxTypeFeeJson;
			JSONObject ccommFeeDetailJson = (ccommTaxDetailsJsonArr != null) ? ccommTaxDetailsJsonArr.optJSONObject(offset + l) : null;
			if (ccommFeeDetailJson != null) {
				feeJson = new JSONObject();
			BigDecimal feeAmt = ccommFeeDetailJson.getBigDecimal(JSON_PROP_TAXVALUE);
			feeJson.put(JSON_PROP_FEECODE, feeCode);
			feeJson.put(JSON_PROP_AMOUNT, feeAmt.multiply(RedisRoEData.getRateOfExchange(suppCcyCode, clientCcyCode, clientMarket)));
			feeJson.put(JSON_PROP_CCYCODE, clientCcyCode);
			}
			feeJsonArr.put(feeJson);
			feesTotal = feesTotal.add(feeJson.getBigDecimal(JSON_PROP_AMOUNT));
			totalFareCompsGroup.add(mFeesPriceCompQualifier.concat(feeCode).concat("@").concat(JSON_PROP_FEECODE), clientCcyCode, feeJson.getBigDecimal(JSON_PROP_AMOUNT).multiply(paxCount));
		}
    				
		feesJson.put(JSON_PROP_FEE, feeJsonArr);
		feesJson.put(JSON_PROP_TOTAL, feesTotal);
		feesJson.put(JSON_PROP_CCYCODE, clientCcyCode);
    	return feesJson;
    }
    
    public static JSONObject getEmptyResponse(JSONObject reqHdrJson) {
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, new JSONObject());
        return resJson;
    }
    
    // Append the supplier commercials returned by commercials engine in supplier pax type fares. 
    // This is required only at reprice/book time for financial consumption and supplier settlement purpose.
    private static void appendSupplierCommercialsToPaxTypeFares(JSONObject paxTypeFareJson, JSONObject ccommJrnyPsgrDtlJson, String suppID, String suppCcyCode, Map<String,String> suppCommToTypeMap) {
		JSONArray suppCommJsonArr = new JSONArray();
		JSONArray ccommSuppCommJsonArr = ccommJrnyPsgrDtlJson.optJSONArray(JSON_PROP_COMMDETAILS);
		// If no supplier commercials have been defined in BRMS, the JSONArray for ccommSuppCommJsonArr will be null.
		// In this case, log a message and proceed with other calculations.
		if (ccommSuppCommJsonArr == null) {
			logger.warn(String.format("No supplier commercials found for supplier %s", suppID));
			return;
		}

		for (int x=0; x < ccommSuppCommJsonArr.length(); x++) {
			JSONObject ccommSuppCommJson = ccommSuppCommJsonArr.getJSONObject(x);
			JSONObject suppCommJson = new JSONObject();
			suppCommJson.put(JSON_PROP_COMMNAME, ccommSuppCommJson.getString(JSON_PROP_COMMNAME));
			suppCommJson.put(JSON_PROP_COMMTYPE, suppCommToTypeMap.get(ccommSuppCommJson.getString(JSON_PROP_COMMNAME)));
			suppCommJson.put(JSON_PROP_COMMAMOUNT, ccommSuppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
			suppCommJson.put(JSON_PROP_COMMCCY, ccommSuppCommJson.optString(JSON_PROP_COMMCCY, suppCcyCode));
			suppCommJsonArr.put(suppCommJson);
		}
		paxTypeFareJson.put(JSON_PROP_SUPPCOMM, suppCommJsonArr);
    }
    
    private static JSONObject convertToClientEntityCommercialJson(JSONObject clientCommJson, Map<String,String> clntCommToTypeMap, String suppCcyCode) {
    	JSONObject clientCommercial= new JSONObject();
    	String commercialName = clientCommJson.getString(JSON_PROP_COMMNAME);
		clientCommercial.put(JSON_PROP_COMMTYPE, clntCommToTypeMap.getOrDefault(commercialName, "?"));
		clientCommercial.put(JSON_PROP_COMMAMOUNT, clientCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
		clientCommercial.put(JSON_PROP_COMMNAME,clientCommJson.getString(JSON_PROP_COMMNAME));
		clientCommercial.put(JSON_PROP_COMMCCY,clientCommJson.optString(JSON_PROP_COMMCCY, suppCcyCode));
    	return clientCommercial;
    }
    
	public static TripIndicator deduceTripIndicator(JSONObject reqHdrJson, JSONObject reqBodyJson) {
		JSONArray odoInfoJsonArr = reqBodyJson.optJSONArray(JSON_PROP_ORIGDESTINFO);
		return (odoInfoJsonArr != null) ? deduceSearchOperationTripIndicator(odoInfoJsonArr) : deduceOtherOperationsTripIndicator(reqBodyJson);
	}
	
	private static TripIndicator deduceSearchOperationTripIndicator(JSONArray odoInfoJsonArr) {
		TripIndicator tripIndicator = TripIndicator.DOMESTIC;
		JSONObject odoInfoJson = null;
		String prevCountry = null;
		String origCountry = null;
		String destCountry = null;
		// TODO: Check how user's country will affect tripIndicator
		for (int i=0; i < odoInfoJsonArr.length(); i++) {
			odoInfoJson = odoInfoJsonArr.getJSONObject(i);
			origCountry = RedisAirportDataV2.getAirportInfo(odoInfoJson.getString(JSON_PROP_ORIGLOC), RedisAirportDataV2.AIRPORT_COUNTRY);
			if (prevCountry != null && REDIS_VALUE_NIL.equals(origCountry) == false && prevCountry.equals(origCountry) == false) {
				tripIndicator = TripIndicator.INTERNATIONAL;
				break;
			}
			prevCountry = origCountry;
			
			destCountry = RedisAirportDataV2.getAirportInfo(odoInfoJson.getString(JSON_PROP_DESTLOC), RedisAirportDataV2.AIRPORT_COUNTRY);
			if (prevCountry != null && REDIS_VALUE_NIL.equals(destCountry) == false && prevCountry.equals(destCountry) == false) {
				tripIndicator = TripIndicator.INTERNATIONAL;
				break;
			}
			prevCountry = destCountry;
		}
		
		return tripIndicator;
	}

	private static TripIndicator deduceOtherOperationsTripIndicator(JSONObject reqBodyJson) {
		TripIndicator tripIndicator = TripIndicator.DOMESTIC;
		JSONObject pricedItinsJson = null;
		String prevCountry = null;
		String origCountry = null;
		String destCountry = null;
		
		JSONArray pricedItinsJsonArr = reqBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
		for (int i=0; i < pricedItinsJsonArr.length(); i++) {
			pricedItinsJson = pricedItinsJsonArr.getJSONObject(i);
			JSONObject airItinJson = pricedItinsJson.optJSONObject(JSON_PROP_AIRITINERARY);
			if (airItinJson == null) {
				continue;
			}
			
			JSONArray odoJsonArr = airItinJson.optJSONArray(JSON_PROP_ORIGDESTOPTS);
			if (odoJsonArr == null) {
				continue;
			}
			
			for (int j=0; j < odoJsonArr.length(); j++) {
				JSONObject odoJson = odoJsonArr.getJSONObject(j);
				JSONArray flSegsJsonArr = odoJson.getJSONArray(JSON_PROP_FLIGHTSEG);
				if (flSegsJsonArr == null) {
					continue;
				}
				
				for (int k=0; k < flSegsJsonArr.length(); k++) {
					JSONObject flSegJson = flSegsJsonArr.getJSONObject(k);
					
					origCountry = RedisAirportDataV2.getAirportInfo(flSegJson.getString(JSON_PROP_ORIGLOC), RedisAirportDataV2.AIRPORT_COUNTRY);
					if (prevCountry != null && REDIS_VALUE_NIL.equals(origCountry) == false && prevCountry.equals(origCountry) == false) {
						tripIndicator = TripIndicator.INTERNATIONAL;
						break;
					}
					prevCountry = origCountry;
					
					destCountry = RedisAirportDataV2.getAirportInfo(flSegJson.getString(JSON_PROP_DESTLOC), RedisAirportDataV2.AIRPORT_COUNTRY);
					if (prevCountry != null && REDIS_VALUE_NIL.equals(destCountry) == false && prevCountry.equals(destCountry) == false) {
						tripIndicator = TripIndicator.INTERNATIONAL;
						break;
					}
					prevCountry = destCountry;
					
				}
			}
			
		}
		
		return tripIndicator;
	}

}
