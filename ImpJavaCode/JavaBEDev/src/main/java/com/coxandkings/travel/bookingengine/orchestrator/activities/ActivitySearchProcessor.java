package com.coxandkings.travel.bookingengine.orchestrator.activities;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.activities.ActivitiesConfig;
import com.coxandkings.travel.bookingengine.orchestrator.activities.SupplierCommercials;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ClientCommercials;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class ActivitySearchProcessor implements ActivityService {

	private static final String XPATH_RESPONSEBODY_OTA_TOURACTIVITY = "./sig:ResponseBody/sig1:OTA_TourActivityAvailRSWrapper";
	private static final String DEFAULT_PARTICIPANTCOUNT = "1";
	private static final String XML_ATTR_ENDPERIOD = "EndPeriod";
	private static final String XML_ATTR_STARTPERIOD = "StartPeriod";
	private static final String XPATH_REQUESTBODY_NS_QUALIFIERINFO = "./sig:RequestBody/ns:OTA_TourActivityAvailRQ/ns:TourActivity/ns:ParticipantCount/ns:QualifierInfo";
	private static final String XPATH_REQUESTBODY_NS_PARTICIPANTCOUNT = "./sig:RequestBody/ns:OTA_TourActivityAvailRQ/ns:TourActivity/ns:ParticipantCount/@Quantity";
	private static final String XPATH_REQUESTBODY_NS_SCHEDULE = "./sig:RequestBody/ns:OTA_TourActivityAvailRQ/ns:TourActivity/ns:Schedule";
	private static final String XPATH_REQUESTBODY_NS_REGIONCODE = "./sig:RequestBody/ns:OTA_TourActivityAvailRQ/ns:TourActivity/ns:Location/ns:Region/@RegionCode";
	private static final String XPATH_REQUESTBODY_NS_COUNTRYCODE = "./sig:RequestBody/ns:OTA_TourActivityAvailRQ/ns:TourActivity/ns:Location/ns:Address/ns:CountryName/@Code";
	
	private static final Logger logger = LogManager.getLogger(ActivitySearchProcessor.class); 
	
	public static String process(JSONObject reqJson) throws Exception{
	try{
		 OperationConfig opConfig = ActivitiesConfig.getOperationConfig("search");
         Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
         Document ownerDoc = reqElem.getOwnerDocument();
         TrackingContext.setTrackingContext(reqJson);
         JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
         JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
         JSONObject activityInfoJson = reqBodyJson.getJSONArray(JSON_PROP_ACTIVITYINFO).getJSONObject(0);
         

         String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
         String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
         String userID = reqHdrJson.getString(JSON_PROP_USERID);

         UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
         List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(ActivityService.PRODUCT_CATEGORY,ActivityService.PRODUCT_SUBCATEGORY);

         XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_SESSIONID,sessionID);
         XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_TRANSACTIONID,transactionID);
         XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_USERID,userID);
         
         Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, XPATH_REQUESTHEADER_SUPPLIERCREDENTIALSLIST);
         for (ProductSupplier prodSupplier : prodSuppliers) {
             suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc,prodSuppliers.indexOf(prodSupplier)));
         }
         
        if(activityInfoJson.has(JSON_PROP_COUNTRYCODE)){
        	XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTBODY_NS_COUNTRYCODE,activityInfoJson.getString(JSON_PROP_COUNTRYCODE));
        }
        
        XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTBODY_NS_REGIONCODE, activityInfoJson.getString(JSON_PROP_CITYCODE));
        Element scheduleElem = XMLUtils.getFirstElementAtXPath(reqElem, XPATH_REQUESTBODY_NS_SCHEDULE);
        //TODO seperate start and end date
        if(activityInfoJson.has(JSON_PROP_STARTDATE) && activityInfoJson.has(JSON_PROP_ENDDATE)){
        scheduleElem.setAttribute(XML_ATTR_STARTPERIOD, activityInfoJson.getString(JSON_PROP_STARTDATE));
        scheduleElem.setAttribute(XML_ATTR_ENDPERIOD, activityInfoJson.getString(JSON_PROP_ENDDATE));
        }
        else {
        	scheduleElem.setAttribute(XML_ATTR_STARTPERIOD, LocalDate.now().plusDays(1).format(ActivityService.mDateFormat));
            scheduleElem.setAttribute(XML_ATTR_ENDPERIOD, LocalDate.now().plusMonths(1).format(ActivityService.mDateFormat));
        }
        
        //TODO add logic to take input from WEM
        XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTBODY_NS_PARTICIPANTCOUNT,DEFAULT_PARTICIPANTCOUNT);
        XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTBODY_NS_QUALIFIERINFO,JSON_PROP_ADULT);
        
        logger.info("Before opening HttpURLConnection");
        Element resElem = null;
         logger.trace(String.format("SI XML Request = %s", XMLTransformer.toString(reqElem)));
         resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), ActivitiesConfig.getHttpHeaders(), reqElem);
         if (resElem == null) {
         	throw new Exception(ERROR_MSG_NULL_RESPONSE_SI);	
         }
        logger.trace(String.format("SI XML Response = %s", XMLTransformer.toString(resElem)));
        Element[] resBodyElem = XMLUtils.getElementsAtXPath(resElem, XPATH_RESPONSEBODY_OTA_TOURACTIVITY);
 		JSONObject resBodyJson = getSupplierResponseJSON(resBodyElem);
 		JSONObject resJson = new JSONObject();
 		resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
 		resJson.put(JSON_PROP_RESBODY, resBodyJson);
        logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));
        Map<String,JSONObject> briActTourActMap = new HashMap<String, JSONObject>();
        JSONObject resSupplierCommJson =  SupplierCommercials.getSupplierCommercials(reqJson,reqElem, resJson,briActTourActMap,usrCtx);
        JSONObject resClientCommJson = ClientCommercials.getClientCommercials(resSupplierCommJson);
        calculatePrices(reqJson, resJson, resSupplierCommJson, resClientCommJson, briActTourActMap,usrCtx,false);
        return resJson.toString();
	   }
		catch (Exception x) {
			x.printStackTrace();
			return STATUS_ERROR;
		}
		
	}
	
	
	public static void calculatePrices(JSONObject reqJson, JSONObject resJson, JSONObject suppCommResJson, JSONObject clientCommResJson,
		Map<String, JSONObject> briActTourActMap, UserContext userContext, boolean retainSuppFares) {
		
		JSONArray clientBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(clientCommResJson);
		JSONArray suppBRIJsonArr = getSupplierCommercialsBusinessRuleIntakeJSONArray(suppCommResJson);
		String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
    	String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
		Map<String,BigDecimal> paxInfoMap = null;
		Map<String, String> suppCommToTypeMap =null;
		Map<String, HashMap<String,String>> clientEntityToCommToTypeMap =null;
		JSONObject userCtxJson = userContext.toJSON();
		JSONArray clientEntityDetailsArr=userCtxJson.getJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS);
		
		for (int i = 0; i < clientBRIJsonArr.length(); i++) {
			JSONObject clientBRI = clientBRIJsonArr.getJSONObject(i);
			if(retainSuppFares) {
				JSONObject tourActivityRequestJson =  reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ACTIVITYINFO).getJSONObject(i);
				 paxInfoMap = getPaxCountsFromRequest(tourActivityRequestJson);
				 suppCommToTypeMap = getSupplierCommercialsAndTheirType(suppBRIJsonArr.getJSONObject(i));
				 clientEntityToCommToTypeMap = getClientCommercialsAndTheirType(clientBRI);
				}
			else {
					paxInfoMap=new LinkedHashMap<String,BigDecimal>();
		        	paxInfoMap.put(JSON_PROP_ADULT, new BigDecimal(1));
			}
			
			JSONArray clientActivityDetJsonArr = clientBRI.getJSONArray(JSON_PROP_CCE_ACTIVITY_DETAILS);
			for (int j = 0; j < clientActivityDetJsonArr.length(); j++) {

				iterateClientCommercialActivityDetails(briActTourActMap, retainSuppFares, clientMarket, clientCcyCode,
						paxInfoMap, suppCommToTypeMap, clientEntityToCommToTypeMap, clientEntityDetailsArr, i,
						clientActivityDetJsonArr, j);
				}

		}
	}


	/**
	 * @param briActTourActMap
	 * @param retainSuppFares
	 * @param clientMarket
	 * @param clientCcyCode
	 * @param paxInfoMap
	 * @param suppCommToTypeMap
	 * @param clientEntityToCommToTypeMap
	 * @param clientEntityDetailsArr
	 * @param i
	 * @param clientActivityDetJsonArr
	 * @param j
	 */
	private static void iterateClientCommercialActivityDetails(Map<String, JSONObject> briActTourActMap,
			boolean retainSuppFares, String clientMarket, String clientCcyCode, Map<String, BigDecimal> paxInfoMap,
			Map<String, String> suppCommToTypeMap, Map<String, HashMap<String, String>> clientEntityToCommToTypeMap,
			JSONArray clientEntityDetailsArr, int i, JSONArray clientActivityDetJsonArr, int j) {
		String suppCcyCode;
		JSONObject clientActivityDetJson = clientActivityDetJsonArr.getJSONObject(j);
		JSONObject tourActivityResponseJson = briActTourActMap.get(i + "_" + j);
		
		JSONArray respPricingArr = tourActivityResponseJson.getJSONArray(JSON_PROP_PRICING);
		JSONArray clientCommPricingArr = clientActivityDetJson.getJSONArray(JSON_PROP_CCE_PRICING);
		
		if(retainSuppFares) {
		JSONObject suppPriceJSON = new JSONObject();
		suppPriceJSON.put(JSON_PROP_PRICING, new JSONArray());
		tourActivityResponseJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceJSON);		
		}
		for (int k = 0; k < clientCommPricingArr.length(); k++) {
			iterateClientCommercialActivityPricingDetails(retainSuppFares, clientMarket, clientCcyCode,
					suppCommToTypeMap, clientEntityToCommToTypeMap, clientEntityDetailsArr, tourActivityResponseJson,
					respPricingArr, clientCommPricingArr, k);

		}
			
			calculateSummary( paxInfoMap, tourActivityResponseJson, retainSuppFares);
	}


	/**
	 * @param retainSuppFares
	 * @param clientMarket
	 * @param clientCcyCode
	 * @param suppCommToTypeMap
	 * @param clientEntityToCommToTypeMap
	 * @param clientEntityDetailsArr
	 * @param tourActivityResponseJson
	 * @param respPricingArr
	 * @param clientCommPricingArr
	 * @param k
	 */
	private static void iterateClientCommercialActivityPricingDetails(boolean retainSuppFares, String clientMarket,
			String clientCcyCode, Map<String, String> suppCommToTypeMap,
			Map<String, HashMap<String, String>> clientEntityToCommToTypeMap, JSONArray clientEntityDetailsArr,
			JSONObject tourActivityResponseJson, JSONArray respPricingArr, JSONArray clientCommPricingArr, int k) {
		String suppCcyCode;
		JSONObject clientCommPricing = clientCommPricingArr.getJSONObject(k);
		JSONObject respPricing = respPricingArr.getJSONObject(k);
		JSONObject suppPricing=null;
		String participantCategory=respPricing.getString(JSON_PROP_PARTICIPANTCATEGORY);
		suppCcyCode=respPricing.getString(JSON_PROP_CCYCODE);
		JSONArray clientEntityCommJsonArr = clientCommPricing.optJSONArray(JSON_PROP_ENTITYCOMMERCIALS);

		if (retainSuppFares) {
			suppPricing = createSupplierCommercial(suppCommToTypeMap, tourActivityResponseJson, clientCommPricing,
					respPricing, participantCategory);
		}
		
		
		
		if (clientEntityCommJsonArr == null) {
			// TODO: Refine this warning message. Maybe log some context information also.
			logger.warn(String.format("No Client commercials found for supplier %s and participant category %s", tourActivityResponseJson.getString(SUPPLIER_ID),participantCategory));
			return;
		}

		for (int l = (clientEntityCommJsonArr.length() - 1); l >= 0; l--) {
			
			iterateClientEntityCommercials(retainSuppFares, clientEntityToCommToTypeMap, clientEntityDetailsArr,
					respPricing, suppPricing, clientEntityCommJsonArr, l);

		}
		
		respPricing.put(JSON_PROP_TOTALPRICE,respPricing.getBigDecimal(JSON_PROP_TOTALPRICE).multiply(RedisRoEData.getRateOfExchange(suppCcyCode, clientCcyCode, clientMarket)));
	}


	/**
	 * @param retainSuppFares
	 * @param clientEntityToCommToTypeMap
	 * @param clientEntityDetailsArr
	 * @param respPricing
	 * @param suppPricing
	 * @param clientEntityCommJsonArr
	 * @param l
	 */
	private static void iterateClientEntityCommercials(boolean retainSuppFares,
			Map<String, HashMap<String, String>> clientEntityToCommToTypeMap, JSONArray clientEntityDetailsArr,
			JSONObject respPricing, JSONObject suppPricing, JSONArray clientEntityCommJsonArr, int l) {
		JSONObject clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(l);
		String entityName=clientEntityCommJson.getString(JSON_PROP_ENTITYNAME);
		JSONObject clientEntityDetailsJson=new JSONObject();
		for(int y=0;y<clientEntityDetailsArr.length();y++) {
			
			
			// TODO : This Condition will be later uncommented, when we will receive the real time data 
			
//	    					if(clientEntityDetailsArr.getJSONObject(y).opt(JSON_PROP_COMMENTITYID)!=null)
//	    					{
//	    					if(clientEntityDetailsArr.getJSONObject(y).opt(JSON_PROP_COMMENTITYID).toString().equalsIgnoreCase(clientEntityCommJson.get(JSON_PROP_ENTITYNAME).toString()))
//	    					{
//	    						clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
//	    					}
//	    					}
			
			// TODO : This line will be removed, When the upper changes will be uncommented
			clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
			
		}
		
		System.out.println("User Context Extracted : "+clientEntityDetailsJson);
		if(retainSuppFares) {
		createClientCommercials(clientEntityToCommToTypeMap, suppPricing, clientEntityCommJson, entityName,
					clientEntityDetailsJson,respPricing);
		}
		
		JSONObject markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMMERCIALDETAILS);
		if (markupCalcJson == null) {
			return;
		}
		
       respPricing.put(JSON_PROP_TOTALPRICE, respPricing.getBigDecimal(JSON_PROP_TOTALPRICE).add(markupCalcJson.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT)));
	}


	/**
	 * @param suppCommToTypeMap
	 * @param tourActivityResponseJson
	 * @param clientCommPricing
	 * @param respPricing
	 * @param participantCategory
	 * @return
	 */
	private static JSONObject createSupplierCommercial(Map<String, String> suppCommToTypeMap,
			JSONObject tourActivityResponseJson, JSONObject clientCommPricing, JSONObject respPricing,
			String participantCategory) {
		JSONObject suppPricing;
		suppPricing = new JSONObject();
		suppPricing.put(JSON_PROP_TOTALPRICE, respPricing.getBigDecimal(JSON_PROP_TOTALPRICE));
		suppPricing.put(JSON_PROP_CCYCODE, respPricing.getString(JSON_PROP_CCYCODE));
		suppPricing.put(JSON_PROP_PARTICIPANTCATEGORY,participantCategory );
		// Append calculated supplier commercials in pax type fares
		JSONArray suppCommJsonArr = new JSONArray();
		JSONArray ccommSuppCommJsonArr = clientCommPricing.optJSONArray(JSON_PROP_COMMERCIAL_DETAILS);
		// If no supplier commercials have been defined in BRMS, the JSONArray for ccommSuppCommJsonArr will be null.
		// In this case, log a message and proceed with other calculations.
		if (ccommSuppCommJsonArr == null) {
			logger.warn(String.format("No supplier commercials found for supplier %s and participant category %s", tourActivityResponseJson.getString(SUPPLIER_ID),participantCategory));
		}
		else {
			// TODO : SupplierCommercial Changes here. Added Commmercial currency here
			for (int x=0; x < ccommSuppCommJsonArr.length(); x++) {
				JSONObject ccommSuppCommJson = ccommSuppCommJsonArr.getJSONObject(x);
				JSONObject suppCommJson = new JSONObject();
				suppCommJson.put(JSON_PROP_COMMERCIALNAME, ccommSuppCommJson.getString(JSON_PROP_COMMERCIALNAME));
				suppCommJson.put(JSON_PROP_COMMERCIALTYPE, suppCommToTypeMap.get(ccommSuppCommJson.getString(JSON_PROP_COMMERCIALNAME)));
				suppCommJson.put(JSON_PROP_COMMERCIALAMOUNT, ccommSuppCommJson.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT));
				suppCommJson.put(JSON_PROP_COMMERCIALCURRENCY, ccommSuppCommJson.optString(JSON_PROP_COMMERCIALCURRENCY));
				suppCommJsonArr.put(suppCommJson);
			}
			suppPricing.put(JSON_PROP_SUPPLIERCOMMERCIALS, suppCommJsonArr);
		}
		tourActivityResponseJson.getJSONObject(JSON_PROP_SUPPPRICEINFO).getJSONArray(JSON_PROP_PRICING).put(suppPricing);
		return suppPricing;
	}


	/**
	 * @param clientEntityToCommToTypeMap
	 * @param suppPricing
	 * @param clientEntityCommJson
	 * @param entityName
	 * @param clientEntityDetailsJson
	 */
	private static void createClientCommercials(Map<String, HashMap<String, String>> clientEntityToCommToTypeMap,
			JSONObject suppPricing, JSONObject clientEntityCommJson, String entityName,
			JSONObject clientEntityDetailsJson,JSONObject respPricing) {
		
		/** Additional Commercial needs to be added along with Markup in totalPrice */
		BigDecimal additionalCommercialPrice = new BigDecimal(0);
		JSONObject clientComm=new JSONObject();
		JSONArray entityCommJSONArr=new JSONArray();
		clientComm.put(JSON_PROP_ENTITYCOMMERCIALS, entityCommJSONArr);
		clientComm.put(JSON_PROP_ENTITYNAME, entityName);
		HashMap<String,String> clientCommToTypeMap= clientEntityToCommToTypeMap.get(entityName);
		JSONArray retentionCommJSONArr=clientEntityCommJson.optJSONArray("retentionCommercialDetails");
		JSONArray additionalCommJSONArr=clientEntityCommJson.optJSONArray("additionalCommercialDetails");
		JSONArray fixedCommJSONArr=clientEntityCommJson.optJSONArray("fixedCommercialDetails");
		JSONObject markupCommJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMMERCIALDETAILS);

		// TODO : Client Commercial Changes
		if(retentionCommJSONArr!=null) {
		for(int commIdx=0;commIdx<retentionCommJSONArr.length();commIdx++) {
			JSONObject retentionCommJSON = retentionCommJSONArr.getJSONObject(commIdx);
			System.out.println("Retention JSON for SupplierCommercials : "+retentionCommJSON.toString());
			JSONObject commJSON=new JSONObject();
			getClientCommecialElements(clientCommToTypeMap, retentionCommJSON, commJSON);
			putUserContextDetailsintoClientCommercials(clientEntityDetailsJson, commJSON);
			
			entityCommJSONArr.put(commJSON);
		}}
		
		if(additionalCommJSONArr!=null) {
		for(int commIdx=0;commIdx<additionalCommJSONArr.length();commIdx++) {	
			JSONObject additionalCommJSON = additionalCommJSONArr.getJSONObject(commIdx);
			System.out.println("Additional JSON for SupplierCommercials : "+additionalCommJSON.toString());
			JSONObject commJSON=new JSONObject();
			getClientCommecialElements(clientCommToTypeMap, additionalCommJSON, commJSON);
			putUserContextDetailsintoClientCommercials(clientEntityDetailsJson, commJSON);
			if(COMM_TYPE_RECEIVABLE.equals(commJSON.get(JSON_PROP_COMMTYPE))) {
				additionalCommercialPrice = additionalCommercialPrice.add(commJSON.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT));
			}
			entityCommJSONArr.put(commJSON);
		}}

		if(fixedCommJSONArr!=null) {
		for(int commIdx=0;commIdx<fixedCommJSONArr.length();commIdx++) {
			JSONObject fixedCommJSON = fixedCommJSONArr.getJSONObject(commIdx);
			System.out.println("Fixed JSON for SupplierCommercials : "+fixedCommJSON.toString());
			JSONObject commJSON=new JSONObject();
			getClientCommecialElements(clientCommToTypeMap, fixedCommJSON, commJSON);
			putUserContextDetailsintoClientCommercials(clientEntityDetailsJson, commJSON);
			
			entityCommJSONArr.put(commJSON);
		}}
		if(markupCommJson!=null) {
			JSONObject commJSON=new JSONObject();
			System.out.println("Markup JSON for SupplierCommercials : "+markupCommJson.toString());
			getClientCommecialElements(clientCommToTypeMap, markupCommJson, commJSON);
			putUserContextDetailsintoClientCommercials(clientEntityDetailsJson, commJSON);
			
			entityCommJSONArr.put(commJSON);
		}

		suppPricing.append(JSON_PROP_CLIENTCOMMERCIALS, clientComm);
		respPricing.put(JSON_PROP_TOTALPRICE, respPricing.getBigDecimal(JSON_PROP_TOTALPRICE).add(additionalCommercialPrice));
	}


	/**
	 * @param clientCommToTypeMap
	 * @param retentionCommJSON
	 * @param commJSON
	 */
	private static void getClientCommecialElements(HashMap<String, String> clientCommToTypeMap,
			JSONObject retentionCommJSON, JSONObject commJSON) {
		commJSON.put(JSON_PROP_COMMERCIALNAME, retentionCommJSON.getString(JSON_PROP_COMMERCIALNAME));
		commJSON.put(JSON_PROP_COMMERCIALTYPE, clientCommToTypeMap.get(retentionCommJSON.getString(JSON_PROP_COMMERCIALNAME)));
		commJSON.put(JSON_PROP_COMMERCIALAMOUNT, retentionCommJSON.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT));
		commJSON.put(JSON_PROP_COMMERCIALCURRENCY, retentionCommJSON.optString(JSON_PROP_COMMERCIALCURRENCY));
	}


	/**
	 * @param clientEntityDetailsJson
	 * @param commJSON
	 */
	private static void putUserContextDetailsintoClientCommercials(JSONObject clientEntityDetailsJson,
			JSONObject commJSON) {
		commJSON.put(JSON_PROP_COMMENTITYTYPE, clientEntityDetailsJson.get(JSON_PROP_COMMENTITYTYPE));
		commJSON.put(JSON_PROP_COMMENTITYID, clientEntityDetailsJson.getString(JSON_PROP_COMMENTITYID));
		commJSON.put(JSON_PROP_PARENTCLIENTID, clientEntityDetailsJson.getString(JSON_PROP_PARENTCLIENTID));
		commJSON.put(JSON_PROP_CLIENTID, clientEntityDetailsJson.getString(JSON_PROP_CLIENTID));
	}
	
	private static Map<String, HashMap<String,String>> getClientCommercialsAndTheirType(JSONObject clientCommBRIJson){
		JSONArray entityDetailsJsonArr = null;
		JSONObject entityDetailsJson = null;
		entityDetailsJsonArr = clientCommBRIJson.optJSONArray(JSON_PROP_ENTITY_DETAILS);
		if (entityDetailsJsonArr == null) {
			logger.warn("No Entity found in client commercials");
			return null;
		}
		//String supplierName=clientCommBRIJson.getJSONObject("commonElements").getString("supplierName");
		Map<String, HashMap<String,String>> clientEntityToCommToTypeMap = new HashMap<String, HashMap <String,String>>();
		for(int i=0;i<entityDetailsJsonArr.length();i++) {
			entityDetailsJson=entityDetailsJsonArr.getJSONObject(i);
			String entityName=entityDetailsJson.getString(JSON_PROP_ENTITYNAME);
			//String entityMarket=entityDetailsJson.getString("entityMarket");
			JSONArray commHeadJsonArr = null;
			commHeadJsonArr = entityDetailsJson.optJSONArray(JSON_PROP_COMMERCIAL_HEAD);
			if (commHeadJsonArr == null) {
				logger.warn("No commercial heads found in entity "+entityName);
				continue;
			}
			clientEntityToCommToTypeMap.put(entityName, getEntityCommercialsHeadsAndTheirType(commHeadJsonArr) );
		}
		return clientEntityToCommToTypeMap;
	}
	
	private static HashMap<String, String> getEntityCommercialsHeadsAndTheirType(JSONArray entityCommHeadJsonArr){
		
		HashMap<String, String> commToTypeMap = new HashMap<String, String>();
		JSONObject commHeadJson;
		for(int i=0;i<entityCommHeadJsonArr.length();i++) {
			commHeadJson=entityCommHeadJsonArr.getJSONObject(i);
			commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMERCIAL_HEAD_NAME), commHeadJson.getString(JSON_PROP_COMMERCIALTYPE));
		}
		return commToTypeMap;
	}
	
	 private static Map<String,BigDecimal> getPaxCountsFromRequest(JSONObject tourActivityRequestJson) {
	    	Map<String,BigDecimal> paxInfoMap=new LinkedHashMap<String,BigDecimal>();
	        JSONObject paxInfo=null;

	        JSONArray reqPaxInfoJsonArr = tourActivityRequestJson.getJSONArray("paxInfo");
	        for(int i=0;i<reqPaxInfoJsonArr.length();i++) {
	        	paxInfo = reqPaxInfoJsonArr.getJSONObject(i);
	        	paxInfoMap.put(paxInfo.getString(JSON_PROP_PAXTYPE), new BigDecimal(paxInfo.getInt("quantity")));
	        }

	        return paxInfoMap;
	    }
	 private static JSONArray getClientCommercialsBusinessRuleIntakeJSONArray(JSONObject commResJson) {
	    	return commResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject(JSON_PROP_VALUE).getJSONObject("cnk.activities_commercialscalculationengine.clienttransactionalrules.Root").getJSONArray("businessRuleIntake");
	    }
	    
	 private static JSONArray getSupplierCommercialsBusinessRuleIntakeJSONArray(JSONObject commResJson) {
	    	return commResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject(JSON_PROP_VALUE).getJSONObject("cnk.activities_commercialscalculationengine.suppliertransactionalrules.Root").getJSONArray("businessRuleIntake");
	    }

	 private static Map<String, String> getSupplierCommercialsAndTheirType(JSONObject suppCommBRIJson) {
		// ----------------------------------------------------------------------
		// Retrieve commercials head array from supplier commercials and find type
		// (Receivable, Payable) for commercials
		JSONArray commHeadJsonArr = null;
		JSONObject commHeadJson = null;
		Map<String, String> commToTypeMap = new HashMap<String, String>();
		commHeadJsonArr = suppCommBRIJson.optJSONArray(JSON_PROP_COMMERCIAL_HEAD);
		if (commHeadJsonArr == null) {
			logger.warn("No commercial heads found in supplier commercials");
			return null;
		}

		for (int j = 0; j < commHeadJsonArr.length(); j++) {
			commHeadJson = commHeadJsonArr.getJSONObject(j);
			commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMERCIAL_HEAD_NAME), commHeadJson.getString(JSON_PROP_COMMERCIALTYPE));
		}

		return commToTypeMap;
    }


	private static void calculateSummary(Map<String, BigDecimal> paxInfoMap, JSONObject tourActivityResponseJson,
			boolean retainSuppFares) {
		JSONArray respPricingArr = tourActivityResponseJson.optJSONArray(JSON_PROP_PRICING);
		String currencyCode = null;
		if(null != respPricingArr && respPricingArr.length() > 0) {
			currencyCode = respPricingArr.getJSONObject(0).optString(JSON_PROP_CCYCODE);
		}
		
		
		
		/** IMPORTANT NOTE : LENGTH > 1 IS CHECKED FOR THE PURPOSE THAT IT IS NOT AMONG THE SUPPLIERS WHERE SUMMARY IS ONLY COMING AND HENCE ALREADY 
		 * HANDLED PRIOR TO IT. IF ADULT AND CHILD IS COMING LENGTH OF THE ARRAY WILL BE GREATER THEN ONE AND THE SUMMARY IS CALCULATED BASED ON COUNT 
		 * OF PAX AND COMMERCIALS APPLIED ON EACH ONE OF THEM.*/
		if (respPricingArr.length() > 1) {
			JSONObject respSummaryPricing = new JSONObject();
			respSummaryPricing.put(JSON_PROP_PARTICIPANTCATEGORY, JSON_PROP_SUMMARY);
			respSummaryPricing.put(JSON_PROP_TOTALPRICE, new BigDecimal(0));
			respSummaryPricing.put(JSON_PROP_CCYCODE, currencyCode);
			
			JSONObject suppSummaryPricing = null;
			Map<String, JSONObject> suppCommTotalsMap = null;
			Map<String, HashMap<String,JSONObject>> clientCommTotalsMap = null;
			if (retainSuppFares) {
				suppSummaryPricing = new JSONObject();
				suppSummaryPricing.put(JSON_PROP_PARTICIPANTCATEGORY, JSON_PROP_SUMMARY);
				suppSummaryPricing.put(JSON_PROP_TOTALPRICE, new BigDecimal(0));
				suppSummaryPricing.put(JSON_PROP_CCYCODE, currencyCode);
				suppCommTotalsMap = new HashMap<String, JSONObject>();
				clientCommTotalsMap = new HashMap<String, HashMap<String,JSONObject>>();
			}
			for (int i = 0; i < respPricingArr.length(); i++) {
				JSONObject respPricing = respPricingArr.getJSONObject(i);
				String participantCategory = respPricing.getString(JSON_PROP_PARTICIPANTCATEGORY);

				if (JSON_PROP_SUMMARY.equals(participantCategory)) {
					respPricingArr.put(i, respSummaryPricing);
					if (retainSuppFares) {
						tourActivityResponseJson.getJSONObject(JSON_PROP_SUPPPRICEINFO).getJSONArray(JSON_PROP_PRICING).put(i,
								suppSummaryPricing);
					}
					continue;
				}

				if (paxInfoMap.containsKey(participantCategory)) {
					BigDecimal participantCategoryCount = paxInfoMap.get(participantCategory);
					respSummaryPricing.put(JSON_PROP_TOTALPRICE, respSummaryPricing.getBigDecimal(JSON_PROP_TOTALPRICE)
							.add(respPricing.getBigDecimal(JSON_PROP_TOTALPRICE).multiply(participantCategoryCount)));
					if (retainSuppFares) {
						JSONObject suppPricing = tourActivityResponseJson.getJSONObject(JSON_PROP_SUPPPRICEINFO)
								.getJSONArray(JSON_PROP_PRICING).getJSONObject(i);
						suppSummaryPricing.put(JSON_PROP_TOTALPRICE, suppSummaryPricing.getBigDecimal(JSON_PROP_TOTALPRICE)
								.add(suppPricing.getBigDecimal(JSON_PROP_TOTALPRICE).multiply(participantCategoryCount)));
						
						JSONArray suppCommJsonArr = suppPricing.optJSONArray(JSON_PROP_SUPPLIERCOMMERCIALS);
						JSONArray clientCommJsonArr = suppPricing.optJSONArray(JSON_PROP_CLIENTCOMMERCIALS);	
						// If no supplier commercials have been defined in BRMS, the JSONArray for suppCommJsonArr will be null.
						// In this case, log a message and proceed with other calculations.
						if (suppCommJsonArr == null) {
							logger.warn("No supplier commercials found");
						}
						else {
							for (int j=0; j < suppCommJsonArr.length(); j++) {
								JSONObject suppCommJson = suppCommJsonArr.getJSONObject(j);
								String suppCommName = suppCommJson.getString(JSON_PROP_COMMERCIALNAME);
								JSONObject suppCommTotalsJson = null;
								if (suppCommTotalsMap.containsKey(suppCommName)) {
									suppCommTotalsJson = suppCommTotalsMap.get(suppCommName);
									suppCommTotalsJson.put(JSON_PROP_COMMERCIALAMOUNT, suppCommTotalsJson.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT).add(suppCommJson.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT).multiply(participantCategoryCount)));
								}
								else {
									suppCommTotalsJson = new JSONObject();
									suppCommTotalsJson.put(JSON_PROP_COMMERCIALNAME, suppCommName);
									suppCommTotalsJson.put(JSON_PROP_COMMERCIALTYPE, suppCommJson.getString(JSON_PROP_COMMERCIALTYPE));
									suppCommTotalsJson.put(JSON_PROP_COMMERCIALAMOUNT, suppCommJson.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT).multiply(participantCategoryCount));
									suppCommTotalsJson.put(JSON_PROP_COMMERCIALCURRENCY, suppCommJson.optString(JSON_PROP_COMMERCIALCURRENCY));
									suppCommTotalsMap.put(suppCommName, suppCommTotalsJson);
								}
								
							}
							JSONArray suppCommTotalsJsonArr = new JSONArray();
							Iterator<Entry<String, JSONObject>> suppCommTotalsIter = suppCommTotalsMap.entrySet().iterator();
							while (suppCommTotalsIter.hasNext()) {
								suppCommTotalsJsonArr.put(suppCommTotalsIter.next().getValue());
							}
							suppSummaryPricing.put(JSON_PROP_SUPPLIERCOMMERCIALS, suppCommTotalsJsonArr);
						}
					
						if (clientCommJsonArr == null) {
							logger.warn("No Client commercials found");
						}
						else {

							for (int j = 0; j < clientCommJsonArr.length(); j++) {

								JSONObject clientCommJson = clientCommJsonArr.getJSONObject(j);
								String entityName = clientCommJson.getString(JSON_PROP_ENTITYNAME);
								JSONArray entityCommercialsJSONArr = clientCommJson.getJSONArray(JSON_PROP_ENTITYCOMMERCIALS);
								JSONObject entityCommTotalsJson = null;
								HashMap<String, JSONObject> entityCommTotalsMap = null;

								if (clientCommTotalsMap.containsKey(entityName)) {

									entityCommTotalsMap = clientCommTotalsMap.get(entityName);
									for (int k = 0; k < entityCommercialsJSONArr.length(); k++) {
										JSONObject entityComm = entityCommercialsJSONArr.getJSONObject(k);
										String entityCommName = entityComm.getString(JSON_PROP_COMMERCIALNAME);
										entityCommTotalsJson = entityCommTotalsMap.get(entityCommName);
										entityCommTotalsJson.put(JSON_PROP_COMMERCIALAMOUNT,
												entityCommTotalsJson.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT)
														.add(entityComm.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT)
																.multiply(participantCategoryCount)));
										
									}
								}

								else {
									entityCommTotalsMap = new HashMap<String, JSONObject>();
									clientCommTotalsMap.put(entityName, entityCommTotalsMap);
									for (int k = 0; k < entityCommercialsJSONArr.length(); k++) {
										JSONObject entityComm = entityCommercialsJSONArr.getJSONObject(k);
										String entityCommName = entityComm.getString(JSON_PROP_COMMERCIALNAME);
										entityCommTotalsJson = new JSONObject();
										entityCommTotalsJson.put(JSON_PROP_COMMERCIALNAME, entityCommName);
										entityCommTotalsJson.put(JSON_PROP_COMMERCIALTYPE,
												entityComm.getString(JSON_PROP_COMMERCIALTYPE));
										entityCommTotalsJson.put(JSON_PROP_COMMERCIALAMOUNT, entityComm
												.getBigDecimal(JSON_PROP_COMMERCIALAMOUNT).multiply(participantCategoryCount));
										entityCommTotalsJson.put(JSON_PROP_COMMERCIALCURRENCY, entityComm.optString(JSON_PROP_COMMERCIALCURRENCY));
										entityCommTotalsJson.put(JSON_PROP_COMMENTITYTYPE, entityComm.opt(JSON_PROP_COMMENTITYTYPE));
										entityCommTotalsJson.put(JSON_PROP_COMMENTITYID, entityComm.optString(JSON_PROP_COMMENTITYID));
										entityCommTotalsJson.put(JSON_PROP_PARENTCLIENTID, entityComm.optString(JSON_PROP_PARENTCLIENTID));
										entityCommTotalsJson.put(JSON_PROP_CLIENTID, entityComm.optString(JSON_PROP_CLIENTID));
										entityCommTotalsMap.put(entityCommName, entityCommTotalsJson);
									}

								}

							}
							
							JSONArray clientCommTotalsJsonArr = new JSONArray();
							for(String entityName:clientCommTotalsMap.keySet()) {
								JSONObject entityCommercials=new JSONObject();
								entityCommercials.put(JSON_PROP_ENTITYNAME, entityName);
								Map<String,JSONObject> entityCommercialsMap=clientCommTotalsMap.get(entityName);
								for(Map.Entry<String,JSONObject> entityCommercial : entityCommercialsMap.entrySet()) {
									entityCommercials.append(JSON_PROP_ENTITYCOMMERCIALS, entityCommercial.getValue());
								}
								clientCommTotalsJsonArr.put(entityCommercials);
							}
							suppSummaryPricing.put(JSON_PROP_CLIENTCOMMERCIALS, clientCommTotalsJsonArr);
						}

					}
				}
			}
		}
	}


	private static JSONObject getSupplierResponseJSON(Element[] resBodyElems) {
		  
		JSONObject resJson = new JSONObject();
		Element[] tourActivityElems;
		JSONArray activityInfoJsonArr = new JSONArray();
		resJson.put(JSON_PROP_ACTIVITYINFO, activityInfoJsonArr);
		JSONObject activityInfoJson = new JSONObject();
		JSONArray tourActivityJsonArr = new JSONArray();
		activityInfoJson.put(JSON_PROP_TOURACTIVITYINFO, tourActivityJsonArr);
		activityInfoJsonArr.put(activityInfoJson);
		for(Element resBodyElem:resBodyElems) {
			String supplierID=XMLUtils.getValueAtXPath(resBodyElem, "./sig1:SupplierID");
			tourActivityElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ns:OTA_TourActivityAvailRS/ns:TourActivityInfo");
			for (Element tourActivityElem : tourActivityElems) {
				JSONObject tourActivityJson = getTourActivityJSON(tourActivityElem,supplierID);
				tourActivityJsonArr.put(tourActivityJson);
			}		
		}		
		return resJson;
	}


	private static JSONObject getTourActivityJSON(Element tourActivityElem,String supplierID) {
		JSONObject tourActivityJson = new JSONObject();
		tourActivityJson.put(SUPPLIER_ID, supplierID);
		tourActivityJson.put(JSON_PROP_BASICINFO, getBasicInfoJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:BasicInfo")));
		tourActivityJson.put(JSON_PROP_SCHEDULE, getScheduleJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:Schedule")));
		tourActivityJson.put(JSON_PROP_DESCRIPTION, getDescriptionJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:Description")));
		tourActivityJson.put(JSON_PROP_CATEGORYANDTYPE, getCategoryTypeJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:CategoryAndType")));
		tourActivityJson.put(JSON_PROP_PRICING, getPricingJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:Pricing"),supplierID));
		return tourActivityJson;
	}


	private static JSONArray getPricingJson(Element pricingElem, String supplierID) {
		JSONArray pricingJsonArr = new JSONArray();
		if (XMLUtils.getValueAtXPath(pricingElem, "./@PerPaxPriceInd").equalsIgnoreCase("true"))
			pricingJsonArr = ActivityPricing.paxPricing.getPricingJson(pricingJsonArr, pricingElem);
		else
			pricingJsonArr = ActivityPricing.summaryPricing.getPricingJson(pricingJsonArr, pricingElem);
		return pricingJsonArr;
	}


	private static JSONObject getCategoryTypeJson(Element categoryTypeElem) {
		JSONObject categoryTypeJson=new JSONObject();
		Element[] categoryElems = XMLUtils.getElementsAtXPath(categoryTypeElem, "./ns:Category");
		Element[] typeElems = XMLUtils.getElementsAtXPath(categoryTypeElem, "./ns:Type");
		
		for(Element categoryElem:categoryElems) {
			categoryTypeJson.append(JSON_PROP_CATEGORY, XMLUtils.getValueAtXPath(categoryElem, "./@Code"));
		}		
		for(Element typeElem:typeElems) {
			categoryTypeJson.append(JSON_PROP_TYPE, XMLUtils.getValueAtXPath(typeElem, "./@Code"));
		}
		return categoryTypeJson;
	}


	private static JSONObject getDescriptionJson(Element descElem) {
		JSONObject descJson=new JSONObject();
		descJson.put(JSON_PROP_SHORTDESCRIPTION,XMLUtils.getValueAtXPath(descElem, "./ns:ShortDescription"));
		descJson.put(JSON_PROP_LONGDESCRIPTION,XMLUtils.getValueAtXPath(descElem, "./ns:LongDescription"));
		return descJson;
	}


	private static JSONObject getScheduleJson(Element scheduleElem) {
		JSONObject scheduleJson = new JSONObject();
		
		Element[] detailElems = XMLUtils.getElementsAtXPath(scheduleElem, "./ns:Detail");
		
		for(Element detail : detailElems ) {
			JSONObject detailJson=new JSONObject();
			detailJson.put(JSON_PROP_STARTDATE,XMLUtils.getValueAtXPath(detail, "./@Start"));
			detailJson.put(JSON_PROP_ENDDATE,XMLUtils.getValueAtXPath(detail, "./@End"));
			detailJson.put(JSON_PROP_DURATION,XMLUtils.getValueAtXPath(detail, "./@Duration"));
			
			Element[] operationTimeElems = XMLUtils.getElementsAtXPath(detail, "./ns:OperationTimes/ns:OperationTime");
			for(Element opTime : operationTimeElems) {
				JSONObject opTimeJson = new JSONObject();
				opTimeJson.put(JSON_PROP_STARTTIME, XMLUtils.getValueAtXPath(opTime, "./@Start"));
				String[] days = { "Mon", "Tue", "Weds", "Thur", "Fri", "Sat", "Sun" };
				for (String day : days) {
					if ("true".equals(XMLUtils.getValueAtXPath(opTime, "./@" + day))) {
						opTimeJson.append(JSON_PROP_DAYS,day);
					}
				}
				detailJson.append(JSON_PROP_OPERATIONTIMES,opTimeJson);
			}
			scheduleJson.append(JSON_PROP_DETAILS,detailJson);
		}
		return scheduleJson;
	}


	private static JSONObject getBasicInfoJson(Element basicInfoElem) {
		JSONObject basicInfoJson = new JSONObject();
		basicInfoJson.put(JSON_PROP_NAME,  XMLUtils.getValueAtXPath(basicInfoElem, "./@Name"));
		basicInfoJson.put(JSON_PROP_SUPPLIERPRODUCTCODE,  XMLUtils.getValueAtXPath(basicInfoElem, "./@SupplierProductCode"));
		basicInfoJson.put(JSON_PROP_AVAILABILITYSTATUS,  XMLUtils.getValueAtXPath(basicInfoElem, "./ns:TPA_Extensions/sig1:Activity_TPA/sig1:Availability_Status"));
		basicInfoJson.put(JSON_PROP_REFERENCE,  XMLUtils.getValueAtXPath(basicInfoElem, "./ns:TPA_Extensions/sig1:Activity_TPA/sig1:Supplier_Details/sig1:Reference"));
		basicInfoJson.put(JSON_PROP_RATEKEY,  XMLUtils.getValueAtXPath(basicInfoElem, "./ns:TPA_Extensions/sig1:Activity_TPA/sig1:Supplier_Details/sig1:RateKey"));
		basicInfoJson.put(JSON_PROP_UNIQUEKEY,  XMLUtils.getValueAtXPath(basicInfoElem, "./ns:TPA_Extensions/sig1:Activity_TPA/sig1:UniqueKey"));
		return basicInfoJson;
		
	}
}
