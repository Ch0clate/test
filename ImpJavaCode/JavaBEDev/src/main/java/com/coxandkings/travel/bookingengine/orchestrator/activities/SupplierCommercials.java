package com.coxandkings.travel.bookingengine.orchestrator.activities;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.activities.ActivitiesConfig;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisCityData;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SupplierCommercials implements ActivityService{

	private static final Logger logger = LogManager.getLogger(ActivityPriceProcessor.class);

	public static JSONObject getSupplierCommercials(JSONObject req,Element reqElem, JSONObject res,
			Map<String, JSONObject> briActTourActMap,UserContext usrCtx) throws Exception {
		CommercialsConfig commConfig = ActivitiesConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig
				.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_SUPPLIER_TRANSACTIONAL);
		JSONObject breSuppReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));

		JSONObject reqHeader = req.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBody = req.getJSONObject(JSON_PROP_REQBODY);
		JSONArray reqActivityInfoJsonArr = reqBody.getJSONArray(JSON_PROP_ACTIVITYINFO);
		JSONObject resHeader = res.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBody = res.getJSONObject(JSON_PROP_RESBODY);
		JSONObject breHdrJson = new JSONObject();
		breHdrJson.put(JSON_PROP_SESSIONID, resHeader.getString(JSON_PROP_SESSIONID));
		breHdrJson.put(JSON_PROP_TRANSACTID, resHeader.getString(JSON_PROP_TRANSACTID));
		breHdrJson.put(JSON_PROP_USERID, resHeader.getString(JSON_PROP_USERID));
		// TODO take operation name as input
		breHdrJson.put(JSON_PROP_OPERATION_NAME, "Search");

		JSONObject rootJson = breSuppReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object")
				.getJSONObject("cnk.activities_commercialscalculationengine.suppliertransactionalrules.Root");
		rootJson.put("header", breHdrJson);

		JSONArray briJsonArr = new JSONArray();
		Map<String, Integer> suppBRIIndexMap = new HashMap<String, Integer>();
		JSONArray activityInfoJsonArr = resBody.getJSONArray(JSON_PROP_ACTIVITYINFO);
		for (int i = 0; i < activityInfoJsonArr.length(); i++) {
			Map<String, JSONObject> suppBRIMap = new LinkedHashMap<String, JSONObject>();
			JSONObject activityInfoJson = activityInfoJsonArr.getJSONObject(i);
			JSONObject reqActivityInfoJson = reqActivityInfoJsonArr.getJSONObject(i);
			JSONArray tourActivityJsonArr = activityInfoJson.getJSONArray(JSON_PROP_TOURACTIVITYINFO);
			int actIndex = 0;
			for (int j = 0; j < tourActivityJsonArr.length(); j++) {
				JSONObject tourActivityJson = tourActivityJsonArr.getJSONObject(j);
				String suppID = tourActivityJson.getString(SUPPLIER_ID);
				JSONObject briJson = null;
				if (suppBRIMap.containsKey(suppID)) {
					briJson = suppBRIMap.get(suppID);
				}

				else {
					briJson = new JSONObject();
					suppBRIMap.put(suppID, briJson);
					suppBRIIndexMap.put(i + "_" + suppID, briJsonArr.length());
					JSONObject clientCtx = reqHeader.getJSONObject("clientContext");
					JSONObject commonElemsJson = new JSONObject();
					commonElemsJson.put(JSON_PROP_SUPPLIER_NAME, suppID);
					// TODO: Supplier market is hard-coded below. Where will this come from? This
					// should be ideally come from supplier credentials.
					// TODO : CONFIRM THIS CHANGE
					commonElemsJson.put(JSON_PROP_SUPPLIER_MARKET, clientCtx.getString("clientMarket"));
					commonElemsJson.put(JSON_PROP_CONTRACT_VALIDITY,
							LocalDateTime.now().format(ActivityService.mDateTimeFormat));
					// TODO: Check how the value for segment should be set?
					commonElemsJson.put("segment", "Active");
					commonElemsJson.put("clientType", clientCtx.getString("clientType"));
					// TODO: Properties for clientGroup, clientName,
					// iatanumber,supplierRateType,supplierRateCode are not yet set. Are these
					// required for B2C? What will be BRMS behavior if these properties are not
					// sent.
					commonElemsJson.put("iataNumber", usrCtx.getClientIATANUmber());
					
					
					// TODO : As per discussion only the last element needs to be checked,
					// if it is ClientGroup get the CommercialEntityID. Confirm This Later
					// Once More
					if(usrCtx.getClientHierarchy()!= null && !usrCtx.getClientHierarchy().isEmpty() &&
							usrCtx.getClientHierarchy().get(usrCtx.getClientHierarchy().size()-1).getCommercialsEntityType().equals(ClientInfo.CommercialsEntityType.ClientGroup))
					commonElemsJson.put("clientGroup", usrCtx.getClientHierarchy().get(usrCtx.getClientHierarchy().size()-1).getCommercialsEntityId());
					
					// TODO : Now UserContext is containing clientName
					commonElemsJson.put("clientName", usrCtx.getClientName());
					
					commonElemsJson.put("supplierRateType", "Contracted");
					commonElemsJson.put("supplierRateCode", "CNKGTA0001");
					briJson.put("commonElements", commonElemsJson);
					JSONObject advDefJson = new JSONObject();
					
					// TODO : As per discussion, Sales Date is modified to Current Date.
					advDefJson.put("salesDate", Instant.now().toString());
					
					// TODO : As per discussion , Date of Activity/ Start Date
					advDefJson.put("travelDate", "2017-04-10T00:00:00");
					advDefJson.put("bookingType", "Online");
					advDefJson.put("connectivitySupplierType", "Direct Connection");
					advDefJson.put("connectivitySupplierName", "GTA");
					
					//TODO : Credentials name now present in formed request Xml
					advDefJson.put("credentialsName", XMLUtils.getValueAtXPath(reqElem, XPATH_SUPPLIERCREDENTIALS_CREDENTIALSNAME));
					
					// TODO : Clarification needed about from where it will come. Nationality
					// is about lead passenger's nationality. No such field in UI.
					advDefJson.put("nationality", clientCtx.getString(JSON_PROP_CLIENTMARKET));
					
					briJson.put("advancedDefinition", advDefJson);
					briJsonArr.put(briJson);
				}

				briJson.append(JSON_PROP_CCE_ACTIVITY_DETAILS, getActivityDetailsJson(tourActivityJson,reqActivityInfoJson));
				actIndex = briJson.getJSONArray(JSON_PROP_CCE_ACTIVITY_DETAILS).length();
				briActTourActMap.put(suppBRIIndexMap.get(i + "_" + suppID) + "_" + (actIndex - 1), tourActivityJson);

			}
		}
		rootJson.put(JSON_PROP_BUSINESS_RULE_INTAKE, briJsonArr);
		JSONObject breSuppResJson = null;
		try {
			// logger.trace(String.format("BRMS Supplier Commercials Request = %s",
			// breSuppReqJson.toString()));
			breSuppResJson = HTTPServiceConsumer.consumeJSONService("BRMS Supplier Commercials",
					commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breSuppReqJson);
			// logger.trace(String.format("BRMS Supplier Commercials Response = %s",
			// breSuppResJson.toString()));
		} catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}

		return breSuppResJson;
	}

	private static JSONObject getActivityDetailsJson(JSONObject tourActivityJson,JSONObject reqActivityInfoJson) {
		JSONObject activityDetJson = new JSONObject();
		activityDetJson.put("supplierProductCode",
				tourActivityJson.getJSONObject("basicInfo").getString("supplierProductCode"));
		activityDetJson.put("productCategorySubType", "Excursion");
		activityDetJson.put("productType", "");
		activityDetJson.put("productName", "Cycling");
		//activityDetJson.put("productName", tourActivityJson.getJSONObject("basicInfo").getString("name"));
		activityDetJson.put("productNameSubType", "");
		String cityCode = reqActivityInfoJson.getString(JSON_PROP_CITYCODE);
    	Map<String,String> cityInfo = RedisCityData.getCityCodeInfo(cityCode);
		activityDetJson.put("continent",cityInfo.getOrDefault("continent", ""));
		activityDetJson.put("country",cityInfo.getOrDefault("country", ""));
		activityDetJson.put("state", cityInfo.getOrDefault("state", ""));
		activityDetJson.put("city", cityInfo.getOrDefault("value", ""));
		activityDetJson.put(JSON_PROP_CCE_PRICING, getPricingDetailsJson(tourActivityJson.getJSONArray(JSON_PROP_PRICING)));
		return activityDetJson;
	}

	private static JSONArray getPricingDetailsJson(JSONArray pricingJsonArr) {
		JSONArray pricingDetailsJsonArr = new JSONArray();
		boolean paxIndicator=false;
		String participantCategory;
		for (int i = 0; i < pricingJsonArr.length(); i++) {
			JSONObject pricingJson = pricingJsonArr.getJSONObject(i);
			JSONObject pricingDetailsJson = new JSONObject();
			participantCategory = pricingJson.getString(JSON_PROP_PARTICIPANTCATEGORY);
			if (pricingJsonArr.length()>1) {
				paxIndicator=true;
		}
			
			if(paxIndicator && participantCategory.equals(JSON_PROP_SUMMARY)) {
				continue;
			}
			pricingDetailsJson.put(JSON_PROP_PARTICIPANTCATEGORY, pricingJson.getString(JSON_PROP_PARTICIPANTCATEGORY));
			pricingDetailsJson.put(JSON_PROP_TOTAL_FARE, pricingJson.get(JSON_PROP_TOTALPRICE));
			if (pricingJson.has(JSON_PROP_PRICE_BREAKUP))
				pricingDetailsJson.put(JSON_PROP_CCE_FAREBREAKUP, pricingJson.get(JSON_PROP_PRICE_BREAKUP));
			pricingDetailsJsonArr.put(pricingDetailsJson);
		}
		return pricingDetailsJsonArr;
	}

}
