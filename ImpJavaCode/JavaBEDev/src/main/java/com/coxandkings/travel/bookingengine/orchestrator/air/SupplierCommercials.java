package com.coxandkings.travel.bookingengine.orchestrator.air;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.utils.RedisAirportDataV2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SupplierCommercials implements AirConstants {

    private static final Logger logger = LogManager.getLogger(AirSearchProcessor.class);

    public static JSONObject getSupplierCommercialsV2(JSONObject req, JSONObject res) throws Exception{
    	Map<String, JSONObject> bussRuleIntakeBySupp = new HashMap<String, JSONObject>();
    	
        CommercialsConfig commConfig = AirConfig.getCommercialsConfig();
        CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_SUPPLIER_TRANSACTIONAL);
        JSONObject breSuppReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));

        JSONObject reqHeader = req.getJSONObject(JSON_PROP_REQHEADER);
        JSONObject reqBody = req.getJSONObject(JSON_PROP_REQBODY);

        JSONObject resHeader = res.getJSONObject(JSON_PROP_RESHEADER);
        JSONObject resBody = res.getJSONObject(JSON_PROP_RESBODY);

        JSONObject breHdrJson = new JSONObject();
        breHdrJson.put(JSON_PROP_SESSIONID, resHeader.getString(JSON_PROP_SESSIONID));
        breHdrJson.put(JSON_PROP_TRANSACTID, resHeader.getString(JSON_PROP_TRANSACTID));
        breHdrJson.put(JSON_PROP_USERID, resHeader.getString(JSON_PROP_USERID));
        breHdrJson.put(JSON_PROP_OPERATIONNAME, "Search");

        JSONObject rootJson = breSuppReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.air_commercialscalculationengine.suppliertransactionalrules.Root");
        rootJson.put(JSON_PROP_HEADER, breHdrJson);

        JSONArray briJsonArr = new JSONArray();
        JSONArray pricedItinJsonArr = resBody.getJSONArray(JSON_PROP_PRICEDITIN);
        JSONArray journeyDetailsJsonArr = null;
        for (int i=0; i < pricedItinJsonArr.length(); i++) {
            JSONObject pricedItinJson = pricedItinJsonArr.getJSONObject(i);
            String suppID = pricedItinJson.getString(JSON_PROP_SUPPREF);
            JSONObject briJson = null;
            if (bussRuleIntakeBySupp.containsKey(suppID)) {
            	briJson = bussRuleIntakeBySupp.get(suppID);
            }
            else {
            	briJson = createBusinessRuleIntakeForSupplier(reqHeader, reqBody, resHeader, resBody, pricedItinJson);
            	bussRuleIntakeBySupp.put(suppID, briJson);
            }
            
            journeyDetailsJsonArr = briJson.getJSONArray(JSON_PROP_JOURNEYDETAILS);
            journeyDetailsJsonArr.put(getBRMSFlightDetailsJSON(pricedItinJson));
        }

        Iterator<Map.Entry<String, JSONObject>> briEntryIter = bussRuleIntakeBySupp.entrySet().iterator();
        while (briEntryIter.hasNext()) {
        	Map.Entry<String, JSONObject> briEntry = briEntryIter.next();
        	briJsonArr.put(briEntry.getValue());
        }
        rootJson.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);

        JSONObject breSuppResJson = null;
        try {
            breSuppResJson = HTTPServiceConsumer.consumeJSONService(TARGET_SYSTEM_SUPPCOMM, commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breSuppReqJson);
        }
        catch (Exception x) {
            logger.warn("An exception occurred when calling supplier commercials", x);
        }

        return breSuppResJson;
    }
    
    public static JSONObject getBRMSFlightDetailsJSON(JSONObject pricedItinJson)throws Exception {
        boolean isVia = false;
        JSONArray origDestJsonArr =  pricedItinJson.getJSONObject(JSON_PROP_AIRITINERARY).getJSONArray(JSON_PROP_ORIGDESTOPTS);
        JSONArray fltDtlsJsonArr = new JSONArray();
        JSONArray tvlDtlsJsonArr = new JSONArray();
        for (int i=0; i < origDestJsonArr.length(); i++) {
            JSONObject origDestJson = origDestJsonArr.getJSONObject(i);
            JSONArray fltSegsJsonArr = origDestJson.getJSONArray(JSON_PROP_FLIGHTSEG);
            JSONObject tvlDtlsJson = new JSONObject();
            
            isVia = (fltSegsJsonArr.length() > 1);
            for (int j=0; j < fltSegsJsonArr.length(); j++) {
                JSONObject fltSegJson = fltSegsJsonArr.getJSONObject(j);
                JSONObject fltDtlJson = new JSONObject();
                
                JSONObject opAirlineJson = fltSegJson.getJSONObject(JSON_PROP_OPERAIRLINE);
                fltDtlJson.put(JSON_PROP_FLIGHTNBR, opAirlineJson.getString(JSON_PROP_FLIGHTNBR));
                fltDtlJson.put(JSON_PROP_FLIGHTTIMIMNG, fltSegJson.getString(JSON_PROP_DEPARTDATE));
                fltDtlJson.put(JSON_PROP_CABINCLASS, fltSegJson.getString(JSON_PROP_CABINTYPE));
                fltDtlJson.put(JSON_PROP_RBD, fltSegJson.getString(JSON_PROP_RESBOOKDESIG));
                fltDtlsJsonArr.put(fltDtlJson);
                
                if (i == 0) {
                	String origLoc = fltSegJson.getString(JSON_PROP_ORIGLOC);
                	Map<String,Object> airportInfo = RedisAirportDataV2.getAirportInfo(origLoc);
                	tvlDtlsJson.put(JSON_PROP_FROMCITY, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CITY, ""));
                	tvlDtlsJson.put(JSON_PROP_FROMCOUNTRY, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_COUNTRY, ""));
                	tvlDtlsJson.put(JSON_PROP_FROMCONTINENT, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CONTINENT, ""));
                }
                
                if (i == (fltSegsJsonArr.length() - 1)) {
                	String destLoc = fltSegJson.getString(JSON_PROP_DESTLOC);
                	Map<String,Object> airportInfo = RedisAirportDataV2.getAirportInfo(destLoc);
                	tvlDtlsJson.put(JSON_PROP_TOCITY, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CITY, ""));
                	tvlDtlsJson.put(JSON_PROP_TOCOUNTRY, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_COUNTRY, ""));
                	tvlDtlsJson.put(JSON_PROP_TOCONTINENT, airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CONTINENT, ""));
                }
                
                if (isVia && (i > 0 && i < (fltSegsJsonArr.length() - 1))) {
                    String destLoc = fltSegJson.getString(JSON_PROP_DESTLOC);
                    Map<String,Object> airportInfo = RedisAirportDataV2.getAirportInfo(destLoc);

                    String city = airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CITY, "").toString();
                    String viaCity = tvlDtlsJson.optString(JSON_PROP_VIACITY, "");
                    tvlDtlsJson.put(JSON_PROP_VIACITY, viaCity.concat((viaCity.length() > 0) ? "|" : "").concat(city));
                    String country = airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_COUNTRY, "").toString();
                    String viaCountry = tvlDtlsJson.optString(JSON_PROP_VIACOUNTRY, "");
                    tvlDtlsJson.put(JSON_PROP_VIACOUNTRY, viaCountry.concat((viaCountry.length() > 0) ? "|" : "").concat(country));
                    String continent = airportInfo.getOrDefault(RedisAirportDataV2.AIRPORT_CONTINENT, "").toString();
                    String viaContinent = tvlDtlsJson.optString(JSON_PROP_VIACONTINENT, "");
                    tvlDtlsJson.put(JSON_PROP_VIACONTINENT, viaContinent.concat((viaContinent.length() > 0) ? "|" : "").concat(continent));
                }

            }
            
            tvlDtlsJsonArr.put(tvlDtlsJson);
        }

        JSONObject jrnyDtlsJson = new JSONObject();
        jrnyDtlsJson.put(JSON_PROP_FLIGHTTYPE, ((isVia) ? "Via" : "Direct"));
        // TODO: Check if this hard-coding is alright...
        jrnyDtlsJson.put(JSON_PROP_FLIGHTLINETYPE, "Online");
        // TODO: Check if this hard-coding is alright...
        jrnyDtlsJson.put(JSON_PROP_CODESHAREFLIGHTINC, Boolean.TRUE.booleanValue());
        // TODO: Check if this hard-coding is alright...
        jrnyDtlsJson.put(JSON_PROP_TRAVELPRODNAME, "Flights");
        jrnyDtlsJson.put(JSON_PROP_FLIGHTDETAILS, fltDtlsJsonArr);
        jrnyDtlsJson.put(JSON_PROP_TRAVELDTLS, tvlDtlsJsonArr);

        JSONArray psgrDtlsJsonArr = new JSONArray();
        JSONArray paxPricingJsonArr = pricedItinJson.getJSONObject(JSON_PROP_AIRPRICEINFO).getJSONArray(JSON_PROP_PAXTYPEFARES);
        for (int i=0; i < paxPricingJsonArr.length(); i++) {
            JSONObject psgrDtlsJson = new JSONObject();
            JSONObject paxPricingJson = paxPricingJsonArr.getJSONObject(i);
            psgrDtlsJson.put(JSON_PROP_PSGRTYPE, paxPricingJson.getString(JSON_PROP_PAXTYPE));
            // TODO: Map fareBasisValue
            // TODO: Map dealCode
            psgrDtlsJson.put(JSON_PROP_TOTALFARE2, paxPricingJson.getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_AMOUNT));

            JSONObject fareBreakupJson = new JSONObject();
            fareBreakupJson.put(JSON_PROP_BASEFARE_COMM, paxPricingJson.getJSONObject(JSON_PROP_BASEFARE).getBigDecimal(JSON_PROP_AMOUNT));

            JSONArray taxDetailsJsonArr = new JSONArray();
            JSONArray taxesJsonArr = paxPricingJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
            for (int j=0; j < taxesJsonArr.length(); j++) {
                JSONObject taxJson = taxesJsonArr.getJSONObject(j);
                JSONObject taxDetailJson = new JSONObject();
                taxDetailJson.put(JSON_PROP_TAXNAME, taxJson.getString(JSON_PROP_TAXCODE));
                taxDetailJson.put(JSON_PROP_TAXVALUE, taxJson.getBigDecimal(JSON_PROP_AMOUNT));
                taxDetailsJsonArr.put(taxDetailJson);
            }

            JSONArray feesJsonArr = paxPricingJson.getJSONObject(JSON_PROP_FEES).getJSONArray(JSON_PROP_FEE);
            for (int j=0; j < feesJsonArr.length(); j++) {
                JSONObject feeJson = feesJsonArr.getJSONObject(j);
                JSONObject feeDetailJson = new JSONObject();
                feeDetailJson.put(JSON_PROP_TAXNAME, feeJson.getString(JSON_PROP_FEECODE));
                feeDetailJson.put(JSON_PROP_TAXVALUE, feeJson.getBigDecimal(JSON_PROP_AMOUNT));
                taxDetailsJsonArr.put(feeDetailJson);
            }

            fareBreakupJson.put(JSON_PROP_TAXDETAILS, taxDetailsJsonArr);
            psgrDtlsJson.put(JSON_PROP_FAREBREAKUP, fareBreakupJson);
            psgrDtlsJsonArr.put(psgrDtlsJson);
        }

        jrnyDtlsJson.put(JSON_PROP_PSGRDETAILS, psgrDtlsJsonArr);
        return jrnyDtlsJson;
    }
    
    private static JSONObject createBusinessRuleIntakeForSupplier(JSONObject reqHeader, JSONObject reqBody, JSONObject resHeader, JSONObject resBody, JSONObject pricedItinJson) {
        JSONObject briJson = new JSONObject();
        JSONObject commonElemsJson = new JSONObject();
        String suppID = pricedItinJson.getString(JSON_PROP_SUPPREF);
        commonElemsJson.put(JSON_PROP_SUPP, suppID);

        UserContext usrCtx = UserContext.getUserContextForSession(reqHeader);
        
        // TODO: Supplier market is hard-coded below. Where will this come from? This should be ideally come from supplier credentials.
        commonElemsJson.put(JSON_PROP_SUPPMARKET, "India");
        commonElemsJson.put("contractValidity", DATE_FORMAT.format(new Date()));
        commonElemsJson.put(JSON_PROP_PRODNAME, PRODUCT_NAME_BRMS);
        // TODO: Check how the value for segment should be set?
        commonElemsJson.put(JSON_PROP_SEGMENT, "Active");
        JSONObject clientCtx = reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT);
        commonElemsJson.put(JSON_PROP_CLIENTTYPE, clientCtx.getString(JSON_PROP_CLIENTTYPE));
        if (usrCtx != null) {
        	commonElemsJson.put(JSON_PROP_CLIENTNAME, (usrCtx != null) ? usrCtx.getClientName() : "");
        	commonElemsJson.put(JSON_PROP_IATANBR, (usrCtx != null) ? usrCtx.getClientIATANUmber() : "");
        	List<ClientInfo> clientHierarchyList = usrCtx.getClientHierarchy();
        	if (clientHierarchyList != null && clientHierarchyList.size() > 0) {
        		ClientInfo clInfo = clientHierarchyList.get(clientHierarchyList.size() - 1);
        		if (clInfo.getCommercialsEntityType() == ClientInfo.CommercialsEntityType.ClientGroup) {
        			commonElemsJson.put(JSON_PROP_CLIENTGROUP, clInfo.getCommercialsEntityId());
        		}
        	}
        }
        briJson.put(JSON_PROP_COMMONELEMS, commonElemsJson);
        

        JSONObject advDefnJson = new JSONObject();
        advDefnJson.put(JSON_PROP_TICKETINGDATE, DATE_FORMAT.format(new Date()));
        // TODO: How to set travelType?
        advDefnJson.put(JSON_PROP_TRAVELTYPE, "SITI");
        advDefnJson.put(JSON_PROP_JOURNEYTYPE, reqBody.getString(JSON_PROP_TRIPTYPE));

        // TODO: connectivitySupplierType hard-coded to 'LCC'. How should this value be assigned?
        advDefnJson.put(JSON_PROP_CONNECTSUPPTYPE, "LCC");
        // TODO: What value to set for connectivitySupplier? For now, it is set to the same value as supplierID.
        //advDefnJson.put("connectivitySupplier", resBody.getString("supplierRef"));
        advDefnJson.put(JSON_PROP_CONNECTSUPP, suppID);
        // TODO: credentialsName hard-coded to 'Indigo'. This should come from product suppliers list in user context.
        advDefnJson.put(JSON_PROP_CREDSNAME, "Indigo");
        // TODO: bookingType hard-coded to 'Online'. How should this value be assigned?
        advDefnJson.put(JSON_PROP_BOOKINGTYPE, "Online");
        briJson.put(JSON_PROP_ADVANCEDDEF, advDefnJson);
        
        JSONArray jrnyDtlsJsonArr = new JSONArray();
        briJson.put(JSON_PROP_JOURNEYDETAILS, jrnyDtlsJsonArr);
    
        return briJson;
    }

}
