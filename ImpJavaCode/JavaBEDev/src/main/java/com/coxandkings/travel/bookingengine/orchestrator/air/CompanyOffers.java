package com.coxandkings.travel.bookingengine.orchestrator.air;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.common.PassengerType;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo;
import com.coxandkings.travel.bookingengine.userctx.OrgHierarchy;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisAirlineData;
import com.coxandkings.travel.bookingengine.utils.RedisAirportDataV2;

public class CompanyOffers implements AirConstants {
	
	private static final Logger logger = LogManager.getLogger(CompanyOffers.class);
	
	public static void getCompanyOffers(JSONObject req, JSONObject res, OffersConfig.Type invocationType) {
        JSONObject reqHdrJson = req.getJSONObject(JSON_PROP_REQHEADER);
        JSONObject reqBodyJson = req.getJSONObject(JSON_PROP_REQBODY);
        JSONObject clientCtxJson = reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT);

        JSONObject resBodyJson = res.getJSONObject(JSON_PROP_RESBODY);
        JSONArray origDestJsonArr = reqBodyJson.getJSONArray(JSON_PROP_ORIGDESTINFO);
        
        OffersConfig offConfig = AirConfig.getOffersConfig();
		CommercialTypeConfig offTypeConfig = offConfig.getOfferTypeConfig(invocationType);

		JSONObject breCpnyOffReqJson = new JSONObject(new JSONTokener(offTypeConfig.getRequestJSONShell()));
		JSONArray briJsonArr = breCpnyOffReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.air_companyoffers.withoutredemption.Root").getJSONArray("businessRuleIntake");
		JSONObject briJson = new JSONObject();
		
		UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
		List<ClientInfo> clientCommHierarchyList = usrCtx.getClientHierarchy();
		String clientGroup = "";
		if (clientCommHierarchyList != null && clientCommHierarchyList.size() > 0) {
			ClientInfo clInfo = clientCommHierarchyList.get(clientCommHierarchyList.size() - 1);
			if (clInfo.getCommercialsEntityType() == ClientInfo.CommercialsEntityType.ClientGroup) {
				clientGroup = clInfo.getCommercialsEntityId();
			}
		}
		
		JSONObject clientDtlsJson = new JSONObject();
		clientDtlsJson.put(JSON_PROP_CLIENTNAME, usrCtx.getClientName());
		clientDtlsJson.put(JSON_PROP_CLIENTCAT, usrCtx.getClientCategory());
		clientDtlsJson.put(JSON_PROP_CLIENTSUBCAT, usrCtx.getClientSubCategory());
		clientDtlsJson.put(JSON_PROP_CLIENTTYPE, clientCtxJson.getString(JSON_PROP_CLIENTTYPE));
		clientDtlsJson.put(JSON_PROP_POS, clientCtxJson.optString(JSON_PROP_POS, ""));
		clientDtlsJson.put(JSON_PROP_CLIENTGROUP, clientGroup);
		// TODO: Check if this is correct
		clientDtlsJson.put(JSON_PROP_NATIONALITY, clientCtxJson.getString(JSON_PROP_CLIENTMARKET));
		briJson.put(JSON_PROP_CLIENTDETAILS, clientDtlsJson);
		
		OrgHierarchy orgHier = usrCtx.getOrganizationHierarchy();
		JSONObject cpnyDtlsJson = new JSONObject();
		cpnyDtlsJson.put(JSON_PROP_SBU, orgHier.getSBU());
		cpnyDtlsJson.put(JSON_PROP_BU, orgHier.getBU());
		cpnyDtlsJson.put(JSON_PROP_DIVISION, orgHier.getDivision());
		cpnyDtlsJson.put(JSON_PROP_SALESOFFICELOC, orgHier.getSalesOfficeLoc());
		cpnyDtlsJson.put(JSON_PROP_SALESOFFICE, orgHier.getSalesOfficeName());
		cpnyDtlsJson.put(JSON_PROP_COMPANYMKT, clientCtxJson.getString(JSON_PROP_CLIENTMARKET));
		briJson.put(JSON_PROP_COMPANYDETAILS, cpnyDtlsJson);
		
		JSONObject commonElemsJson = new JSONObject();
		commonElemsJson.put(JSON_PROP_BOOKINGDATE, DATE_FORMAT.format(new Date()));
		// Following is discussed and confirmed with Offers team. The travelDate is the 
		commonElemsJson.put(JSON_PROP_TRAVELDATE, origDestJsonArr.getJSONObject(0).getString(JSON_PROP_DEPARTDATE).concat(TIME_ZERO_SUFFIX));
		// TODO: Populate Target Set (Slabs)
		briJson.put(JSON_PROP_COMMONELEMS, commonElemsJson);
		
		JSONArray prodDtlsJsonArr = new JSONArray();
		JSONArray pricedItinsJsonArr = resBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
		for (int i=0; i < pricedItinsJsonArr.length(); i++) {
			JSONObject pricedItinJson = pricedItinsJsonArr.getJSONObject(i);
			JSONObject prodDtlsJson = new JSONObject();
			
			prodDtlsJson.put(JSON_PROP_PRODCATEG, PROD_CATEG_TRANSPORT);
			prodDtlsJson.put(JSON_PROP_PRODCATEGSUBTYPE, PROD_CATEG_SUBTYPE_FLIGHT);
			prodDtlsJson.put(JSON_PROP_JOURNEYTYPE, reqBodyJson.getString(JSON_PROP_TRIPTYPE));
			prodDtlsJson.put(JSON_PROP_FLIGHTDETAILS, getFlightDetailsJsonArray(reqBodyJson, pricedItinJson.getJSONObject(JSON_PROP_AIRITINERARY)));
			
			JSONObject airPriceInfoJson = pricedItinJson.getJSONObject(JSON_PROP_AIRPRICEINFO);
			JSONObject itinTotalJson = airPriceInfoJson.getJSONObject(JSON_PROP_ITINTOTALFARE);
			prodDtlsJson.put(JSON_PROP_TOTALFARE2, itinTotalJson.getBigDecimal(JSON_PROP_AMOUNT));
			prodDtlsJson.put(JSON_PROP_FAREDETAILS, getFareDetailsJsonArray(itinTotalJson));
			
			JSONArray psgrDtlsJsonArr = new JSONArray();
			Map<String,BigDecimal> reqPaxCounts = AirSearchProcessor.getPaxCountsFromRequest(req);
			JSONArray paxTypeFaresJsonArr = airPriceInfoJson.getJSONArray(JSON_PROP_PAXTYPEFARES);
			for (int j=0; j < paxTypeFaresJsonArr.length(); j++) {
				JSONObject paxTypeFareJson = paxTypeFaresJsonArr.getJSONObject(j);
				String paxType = paxTypeFareJson.getString(JSON_PROP_PAXTYPE);
				int paxCount = (reqPaxCounts.containsKey(paxType)) ? reqPaxCounts.get(paxType).intValue() : 0;
				for (int k=0; k < paxCount; k++) {
					JSONObject psgrDtlsJson = new JSONObject();
					psgrDtlsJson.put(JSON_PROP_PSGRTYPE, paxType);
					psgrDtlsJson.put(JSON_PROP_FAREDETAILS, getFareDetailsJsonArray(paxTypeFareJson));
					psgrDtlsJsonArr.put(psgrDtlsJson);
				}
			}
			prodDtlsJson.put(JSON_PROP_PSGRDETAILS, psgrDtlsJsonArr);
			
			prodDtlsJsonArr.put(prodDtlsJson);
		}
		briJson.put(JSON_PROP_PRODDETAILS, prodDtlsJsonArr);
		briJsonArr.put(briJson);
		
        JSONObject breOffResJson = null;
        try {
            breOffResJson = HTTPServiceConsumer.consumeJSONService("BRMS/ComOffers", offTypeConfig.getServiceURL(), offConfig.getHttpHeaders(), breCpnyOffReqJson);
        }
        catch (Exception x) {
            logger.warn("An exception occurred when calling company offers", x);
        }

        if (BRMS_STATUS_TYPE_FAILURE.equals(breOffResJson.getString(JSON_PROP_TYPE))) {
        	logger.warn(String.format("A failure response was received from Company Offers calculation engine: %s", breOffResJson.toString()));
        	return;
        }

        // Check offers invocation type
        if (OffersConfig.Type.COMPANY_SEARCH_TIME == invocationType) {
        	appendOffersToResults(resBodyJson, breOffResJson);
        }
	}

	private static JSONArray getFareDetailsJsonArray(JSONObject fareJson) {
		JSONArray fareDtlsJsonArr = new JSONArray();
		JSONObject fareDtlsJson = new JSONObject();
		JSONObject baseFareJson = fareJson.getJSONObject(JSON_PROP_BASEFARE);
		fareDtlsJson.put(JSON_PROP_FARENAME, JSON_VAL_BASE);
		fareDtlsJson.put(JSON_PROP_FAREVAL, baseFareJson.getBigDecimal(JSON_PROP_AMOUNT));
		fareDtlsJsonArr.put(fareDtlsJson);
		
		JSONObject taxesJson = fareJson.getJSONObject(JSON_PROP_TAXES);
		JSONArray taxJsonArr = taxesJson.optJSONArray(JSON_PROP_TAX);
		for (int j=0; taxJsonArr != null && j < taxJsonArr.length(); j++) {
			JSONObject taxJson = taxJsonArr.getJSONObject(j);
			fareDtlsJson = new JSONObject();
			fareDtlsJson.put(JSON_PROP_FARENAME, taxJson.getString(JSON_PROP_TAXCODE));
			fareDtlsJson.put(JSON_PROP_FAREVAL, taxJson.getBigDecimal(JSON_PROP_AMOUNT));
			fareDtlsJsonArr.put(fareDtlsJson);
		}
		
		JSONObject feesJson = fareJson.getJSONObject(JSON_PROP_FEES);
		JSONArray feeJsonArr = feesJson.optJSONArray(JSON_PROP_FEE);
		for (int j=0; feeJsonArr != null && j < feeJsonArr.length(); j++) {
			JSONObject feeJson = feeJsonArr.getJSONObject(j);
			fareDtlsJson = new JSONObject();
			fareDtlsJson.put(JSON_PROP_FARENAME, feeJson.getString(JSON_PROP_FEECODE));
			fareDtlsJson.put(JSON_PROP_FAREVAL, feeJson.getBigDecimal(JSON_PROP_AMOUNT));
			fareDtlsJsonArr.put(fareDtlsJson);
		}

		JSONObject rcvblsJson = fareJson.optJSONObject(JSON_PROP_RECEIVABLES);
		if (rcvblsJson != null) {
			JSONArray rcvblJsonArr = rcvblsJson.optJSONArray(JSON_PROP_RECEIVABLE);
			for (int j=0; rcvblJsonArr != null && j < rcvblJsonArr.length(); j++) {
				JSONObject rcvblJson = rcvblJsonArr.getJSONObject(j);
				fareDtlsJson = new JSONObject();
				fareDtlsJson.put(JSON_PROP_FARENAME, rcvblJson.getString(JSON_PROP_CODE));
				fareDtlsJson.put(JSON_PROP_FAREVAL, rcvblJson.getBigDecimal(JSON_PROP_AMOUNT));
				fareDtlsJsonArr.put(fareDtlsJson);
			}
		}
		
		return fareDtlsJsonArr;
	}
	
	private static JSONArray getFlightDetailsJsonArray(JSONObject reqBodyJson, JSONObject airItinJson) {
		JSONArray flDtlsJsonArr = new JSONArray();
		JSONArray odosJsonArr = airItinJson.getJSONArray(JSON_PROP_ORIGDESTOPTS);
		
		for (int j=0; j < odosJsonArr.length(); j++) {
			JSONObject odosJson = odosJsonArr.getJSONObject(j);
			JSONArray flSegsJsonArr = odosJson.getJSONArray(JSON_PROP_FLIGHTSEG);
			for (int k=0; k < flSegsJsonArr.length(); k++) {
				Map<String,Object> airportInfo = null;
				JSONObject flDtlJson = new JSONObject();
				JSONObject fltSegJson = flSegsJsonArr.getJSONObject(k);
				
            	String origLoc = fltSegJson.getString(JSON_PROP_ORIGLOC);
            	airportInfo = RedisAirportDataV2.getAirportInfo(origLoc);
				flDtlJson.put(JSON_PROP_CITYFROM, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CITY, ""));
				flDtlJson.put(JSON_PROP_COUNTRYFROM, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_COUNTRY, ""));
				
            	String destLoc = fltSegJson.getString(JSON_PROP_DESTLOC);
            	airportInfo = RedisAirportDataV2.getAirportInfo(destLoc);
				flDtlJson.put(JSON_PROP_CITYTO, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CITY, ""));
				flDtlJson.put(JSON_PROP_COUNTRYTO, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_COUNTRY, ""));
				
				flDtlJson.put(JSON_PROP_CABINCLASS, reqBodyJson.getString(JSON_PROP_CABINTYPE));
				JSONObject mrktAirlineJson = fltSegJson.getJSONObject(JSON_PROP_MARKAIRLINE);
				String mrktAirlineCode = mrktAirlineJson.optString(JSON_PROP_AIRLINECODE);
				JSONObject operAirlineJson = fltSegJson.getJSONObject(JSON_PROP_OPERAIRLINE);
				String operAirlineCode = operAirlineJson.getString(JSON_PROP_AIRLINECODE);
				
				Map<String,Object> airlineData = RedisAirlineData.getAirlineDetails(operAirlineCode);
				flDtlJson.put(JSON_PROP_AIRLINENAME, airlineData.getOrDefault(RedisAirlineData.AIRLINE_NAME, ""));
				flDtlJson.put(JSON_PROP_AIRLINETYPE, airlineData.getOrDefault(RedisAirlineData.AIRLINE_TYPE, ""));
				// Comparing only marketing and operating airline code. If need be, flight number also can be compared.
				flDtlJson.put(JSON_PROP_ISCODESHARE, (operAirlineCode.equals(mrktAirlineCode) == false));
				flDtlJson.put(JSON_PROP_FLIGHTNBR, operAirlineJson.getString(JSON_PROP_FLIGHTNBR));
				
				flDtlsJsonArr.put(flDtlJson);
			}
		}
		
		return flDtlsJsonArr;
	}
	
	public static void appendOffersToResults(JSONObject resBodyJson, JSONObject offResJson) {
        JSONArray pricedItinsJsonArr = resBodyJson.getJSONArray(JSON_PROP_PRICEDITIN);
        
        // When invoking offers engine, only one businessRuleIntake is being sent. Therefore, here retrieve the first 
        // businessRuleIntake item and process results from that.
        JSONArray prodDtlsJsonArr = null;
        try {
        	prodDtlsJsonArr = offResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONArray("value").getJSONObject(0).getJSONObject("cnk.air_companyoffers.withoutredemption.Root").getJSONArray("businessRuleIntake").getJSONObject(0).getJSONArray("productDetails");
        }
        catch (Exception x) {
        	logger.warn("Unable to retrieve first businessRuleIntake item. Aborting Company Offers response processing", x);
        	return;
        }

        if (pricedItinsJsonArr.length() != prodDtlsJsonArr.length()) {
        	logger.warn("Number of pricedItinerary elements in BE result and number of productDetails elements in company offers result is different. Aborting Company Offers response processing.");
        	return;
        }
        
        for (int i=0; i < pricedItinsJsonArr.length(); i++) {
        	JSONObject pricedItinJson = pricedItinsJsonArr.getJSONObject(i);
        	JSONObject prodDtlsJson = prodDtlsJsonArr.getJSONObject(i);
        	
        	// Append search result level offers to search result
        	JSONArray offersJsonArr = prodDtlsJson.optJSONArray(JSON_PROP_OFFERDETAILSSET);
        	if (offersJsonArr != null) {
        		pricedItinJson.put(JSON_PROP_OFFERS, offersJsonArr);
        	}
        	
        	// Retrieve and de-duplicate offers that are at passenger level
        	Map<PassengerType, Map<String, JSONObject>> psgrOffers = new LinkedHashMap<PassengerType, Map<String, JSONObject>>();
        	JSONArray psgrDtlsJsonArr = prodDtlsJson.getJSONArray(JSON_PROP_PSGRDETAILS);
        	for (int j=0; j < psgrDtlsJsonArr.length(); j++) {
        		JSONObject psgrDtlsJson = psgrDtlsJsonArr.getJSONObject(j);
        		
        		PassengerType psgrType = PassengerType.forString(psgrDtlsJson.getString(JSON_PROP_PSGRTYPE));
        		if (psgrType == null) {
        			continue;
        		}
        		
        		Map<String,JSONObject> psgrTypeOffers = (psgrOffers.containsKey(psgrType)) ? psgrOffers.get(psgrType) : new LinkedHashMap<String,JSONObject>();
        		JSONArray psgrOffersJsonArr = psgrDtlsJson.optJSONArray(JSON_PROP_OFFERDETAILSSET);
        		if (psgrOffersJsonArr == null) {
        			continue;
        		}
        		for (int k=0; k < psgrOffersJsonArr.length(); k++) {
        			JSONObject psgrOfferJson = psgrOffersJsonArr.getJSONObject(k);
        			String offerId = psgrOfferJson.getString(JSON_PROP_OFFERID);
        			psgrTypeOffers.put(offerId, psgrOfferJson);
        		}
        		psgrOffers.put(psgrType, psgrTypeOffers);
        	}
        	
        	
        	JSONObject airPriceInfoJson = pricedItinJson.getJSONObject(JSON_PROP_AIRPRICEINFO);
        	JSONArray paxTypeFaresJsonArr = airPriceInfoJson.getJSONArray(JSON_PROP_PAXTYPEFARES);
        	for (int j=0; j <paxTypeFaresJsonArr.length(); j++) {
        		JSONObject paxTypeFareJson = paxTypeFaresJsonArr.getJSONObject(j);
        		PassengerType psgrType = PassengerType.forString(paxTypeFareJson.getString(JSON_PROP_PAXTYPE));
        		if (psgrType == null) {
        			continue;
        		}
        		
        		JSONArray psgrTypeOffsJsonArr = new JSONArray();
        		Map<String,JSONObject> psgrTypeOffers = psgrOffers.get(psgrType);
        		if (psgrTypeOffers == null) {
        			continue;
        		}
        		
        		Collection<JSONObject> psgrTypeOffersColl = psgrTypeOffers.values();
        		for (JSONObject psgrTypeOffer : psgrTypeOffersColl) {
        			psgrTypeOffsJsonArr.put(psgrTypeOffer);
        		}
        		paxTypeFareJson.put(JSON_PROP_OFFERS, psgrTypeOffsJsonArr);
        	}
        }
	}
	
}
