package com.coxandkings.travel.bookingengine.orchestrator.cruise;


import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.cruise.CruiseConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class CruiseSearchProcessor implements CruiseConstants {

	private static final Logger logger = LogManager.getLogger(CruiseSearchProcessor.class);
	private static String mRcvsPriceCompQualifier = JSON_PROP_RECEIVABLES.concat(SIGMA).concat(".").concat(JSON_PROP_RECEIVABLE).concat(".");
	 
	public static String process(JSONObject reqJson) throws Exception
	{
		OperationConfig opConfig = CruiseConfig.getOperationConfig("search");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();
		
		TrackingContext.setTrackingContext(reqJson);
		JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBdyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
		
		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		
//		UserContext userctx1 = UserContext.getUserContextForSession(reqHdrJson);
		
		UserContext userctx = UserContext.getUserContextForSession(reqHdrJson);
		List<ProductSupplier> prodSuppliers = userctx.getSuppliersForProduct(PROD_CATEG_TRANSPORT,"Cruise");
//		List<ProductSupplier> prodSuppliers = userctx.getSuppliersForProduct("Cruise");
		
		createHeader(reqElem, sessionID, transactionID, userID);
		
		Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru1:RequestHeader/com:SupplierCredentialsList");
        for (ProductSupplier prodSupplier : prodSuppliers) {
            suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
        }
        
        Element otaCruiseSailAvalElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody/ota:OTA_CruiseSailAvailRQ");
        
        Element Source = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody/ota:OTA_CruiseSailAvailRQ/ota:POS/ota:Source");
        Source.setAttribute("ISOCurrency", reqHdrJson.getJSONObject("clientContext").getString("clientCurrency"));
        
        Element RequesterID = ownerDoc.createElementNS(Constants.NS_OTA,"RequestorID");
        RequesterID.setAttribute("Type", "");
        RequesterID.setAttribute("ID", "US");
        
        Source.appendChild(RequesterID);
        
//---------------x-------------------x-------------------x----------------------x---------------------x----------------------------x--------------------x-------------------------x-----------------------x-
        
        JSONArray guestsReqJsonArr = reqBdyJson.getJSONArray("Guests");
        CruisePriceProcessor.createGuestsAndGuestCounts(ownerDoc, guestsReqJsonArr, otaCruiseSailAvalElem);
        
//---------------x------------------x-----------------x-------------------x----------------------x-------------------x-----------------x-------------------x--------------------x---------------------x----------------------x-
        JSONObject sailingDateRangeJson = reqBdyJson.getJSONObject("sailingDateRange");
        Element sailingDateRangeElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody/ota:OTA_CruiseSailAvailRQ/ota:SailingDateRange");
        Element otareqElem = (Element) sailingDateRangeElem.getParentNode();
        sailingDateRangeElem.setAttribute("End", sailingDateRangeJson.getString("endDate"));
        sailingDateRangeElem.setAttribute("Start", sailingDateRangeJson.getString("startDate"));
        
        Element cruiseLinePrefs = XMLUtils.getFirstElementAtXPath(reqElem,"./cru:RequestBody/ota:OTA_CruiseSailAvailRQ/ota:CruiseLinePrefs");
        JSONArray cruiseLinePrefArr =  reqBdyJson.getJSONArray("cruiseLinePref");
        for(int i=0;i<cruiseLinePrefArr.length();i++)
        {
        	JSONObject cruiseLinePrefJson =  (JSONObject) cruiseLinePrefArr.get(i);
        	Element cruiseLinePrefElem = getCruiseLinePref(ownerDoc, cruiseLinePrefJson);
        	cruiseLinePrefs.appendChild(cruiseLinePrefElem);
        }
        
        Element regionPref = XMLUtils.getFirstElementAtXPath(reqElem,"./cru:RequestBody/ota:OTA_CruiseSailAvailRQ/ota:RegionPref");
        regionPref.setAttribute("RegionCode", reqBdyJson.getJSONObject("regionPref").getString("regionCode"));
		
//        System.out.println(XMLTransformer.toString(reqElem));
        
        Element resElem = null;
        //logger.trace(String.format("SI XML Request = %s", XMLTransformer.toString(reqElem)));
        
//        System.out.println(XMLTransformer.toString(reqElem));
        
        resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), CruiseConfig.getHttpHeaders(), reqElem);
        if (resElem == null) {
        	throw new Exception("Null response received from SI");
        }
        
        /* System.out.println(XMLTransformer.toString(resElem));
        
        DocumentBuilderFactory docBldrFactory = DocumentBuilderFactory.newInstance();
        docBldrFactory.setNamespaceAware(true);
		DocumentBuilder docBldr = docBldrFactory.newDocumentBuilder();
        
		Document doc = docBldr.parse(new File("D:\\BookingEngine\\StandardizedStarSearchResponse.xml"));
		resElem = doc.getDocumentElement();*/
		
        JSONObject resBodyJson = new JSONObject();
        JSONObject sailingOptionsArr = new JSONObject();
        JSONArray sailingOptionJsonArr = new JSONArray();
//        System.out.println(XMLTransformer.toString(resElem));
        Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem, "./cru:ResponseBody/cru1:OTA_CruiseSailAvailRSWrapper");
        
        for(Element wrapperElem : wrapperElems)
        {
        	getSupplierResponseSailingOptionsJSON(wrapperElem,sailingOptionJsonArr);
        }
        sailingOptionsArr.put("SailingOption", sailingOptionJsonArr);
        JSONArray cruiseOptionsArr = new JSONArray();
        cruiseOptionsArr.put(sailingOptionsArr);
        resBodyJson.put("cruiseOptions", cruiseOptionsArr);
        
        System.out.println(resBodyJson.toString());
        
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, resBodyJson);
        
        System.out.println(resJson.toString());
        
//        System.out.println(reqHdrJson.toString());
//        System.out.println(resBodyJson.toString());
        
//        System.out.println(CruiseSupplierCommercials.getSupplierCommercialsV1(reqJson,resJson));
        Map<String,String> SI2BRMSSailingOptionMap = new HashMap<String,String>();
        JSONObject parentSupplTransJson = CruiseSupplierCommercials.getSupplierCommercialsV1(reqJson,resJson,SI2BRMSSailingOptionMap);
        JSONObject breResClientJson = CruiseClientCommercials.getClientCommercialsV1(parentSupplTransJson);
        
       /* Scanner in = null;
        
        in = new Scanner(new FileReader("D:\\BookingEngine\\ClientTransactionRs.json"));
 
	    StringBuilder sb = new StringBuilder();
	    while(in.hasNext()) {
	        sb.append(in.next());
	    }
	    in.close();
	    String jsonStr = sb.toString();
    	JSONObject json = new JSONObject(jsonStr);*/
        
//        calculatePricesV1(reqJson,resJson,breResClientJson,false);
//        calculatePricesV2(reqJson, resJson, parentSupplTransJson, breResClientJson, SI2BRMSSailingOptionMap, false);
        calculatePricesV3(reqJson, resJson, parentSupplTransJson, breResClientJson, SI2BRMSSailingOptionMap, false, userctx);
        
//      JSONObject resClientJson = CruiseClientCommercials.getClientCommercialsV1(resSupplierJson);
        
        /*CommercialsConfig commConfig = CruiseConfig.getCommercialsConfig();
        CommercialTypeConfig commTypeConfig = commConfig.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_SUPPLIER_TRANSACTIONAL);
        
        Scanner in = null;
        
        in = new Scanner(new FileReader("D:\\BookingEngine\\TransactionReq.txt"));
 
	    StringBuilder sb = new StringBuilder();
	    while(in.hasNext()) {
	        sb.append(in.next());
	    }
	    in.close();
	    String jsonStr = sb.toString();
   
    	JSONObject json = new JSONObject(jsonStr);
        
    	json = HTTPServiceConsumer.consumeJSONService("BRMS/SuppComm", commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), json);
    	
    	System.out.println(json.toString());*/
        
		return resJson.toString();
	}
	
	@Deprecated
	 public static void calculatePricesV1(JSONObject reqJson, JSONObject resJson, JSONObject clientCommResJson, boolean retainSuppFares) {
		 
		 	JSONArray sailingOptionsArr = resJson.getJSONObject("responseBody").getJSONArray("cruiseOptions").getJSONObject(0).getJSONArray("SailingOption");
			JSONArray ccommSuppBRIJsonArr = clientCommResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONArray("value").getJSONObject(0).getJSONObject("cnk.cruise_commercialscalculationengine.clienttransactionalrules.Root").getJSONArray("businessRuleIntake");
			
			Map<String, JSONArray> ccommSuppBRIJsonMap = new HashMap<String, JSONArray>();
	    	for (int i=0; i < ccommSuppBRIJsonArr.length(); i++) {
	    		JSONObject ccommSuppBRIJson = ccommSuppBRIJsonArr.getJSONObject(i);
	    		String suppID = ccommSuppBRIJson.getJSONObject("commonElements").getString("supplier");
	    		JSONArray ccommJrnyDtlsJsonArr = ccommSuppBRIJson.getJSONArray("cruiseDetails");
	    		
	    		ccommSuppBRIJsonMap.put(suppID, ccommJrnyDtlsJsonArr);
	    	}
			
	    	Map<String, Integer> suppIndexMap = new HashMap<String, Integer>(); 
			for(int i=0;i<sailingOptionsArr.length();i++)
			{
				JSONObject sailingOptionsJson = sailingOptionsArr.getJSONObject(i);
				
				String suppID = sailingOptionsJson.optString(JSON_PROP_SUPPREF);
				if(suppID==null || suppID.isEmpty())
					suppID = resJson.getJSONObject("responseBody").getJSONArray("cruiseOptions").getJSONObject(0).optString(JSON_PROP_SUPPREF);
					suppID= "Tourico";
	    		JSONArray ccommCruDtlsJsonArr = ccommSuppBRIJsonMap.get(suppID.substring(0,1).toUpperCase()+suppID.substring(1).toLowerCase());
				
	    		if (ccommCruDtlsJsonArr == null) {
	    			// TODO: This should never happen. Log a information message here.
	    			logger.info(String.format("BRMS/ClientComm: \"businessRuleIntake\" JSON element for supplier %s not found.", suppID));
	    			continue;
	    		}
	    		
	    		int idx = 0;
	    		if (suppIndexMap.containsKey(suppID)) {
	    			idx = suppIndexMap.get(suppID) + 1; 
	    		}
	    		suppIndexMap.put(suppID, idx);
	    		JSONObject ccommCruDtlsJson = ccommCruDtlsJsonArr.getJSONObject(idx);
	    		
	    		JSONArray ccommCruCabDtlsJsonArr =	ccommCruDtlsJson.getJSONArray("cabinDetails");
	    		JSONArray categoryJsonArr =	sailingOptionsJson.getJSONArray("Category");
	    		
	    		for(int j=0; j<categoryJsonArr.length();j++)
	    		{
	    			JSONObject rsCategoryJson = categoryJsonArr.getJSONObject(j);
	    			JSONObject ccommCabDtlsJson = ccommCruCabDtlsJsonArr.getJSONObject(j);
	    			
	    			JSONArray rsPassengerPriceJsonArr = rsCategoryJson.getJSONArray("PassengerPrices");
	    			
	    			for(int k=0;k<rsPassengerPriceJsonArr.length();k++)
	    			{
		    			JSONObject passengerDetailsJson = ccommCabDtlsJson.getJSONArray("passengerDetails").getJSONObject(k);
		    			String passengerType = passengerDetailsJson.getString("passengerType");
		    			
		    			JSONArray ccommEntityCommercialsJsonArr = passengerDetailsJson.getJSONArray("entityCommercials");
		    			JSONObject ccommEntityCommercialsJson = ccommEntityCommercialsJsonArr.getJSONObject(ccommEntityCommercialsJsonArr.length()-1); //always get the last entity commercials
		    			
		    			JSONObject markUpCommJson = ccommEntityCommercialsJson.getJSONObject("markUpCommercialDetails");
		    			double markupTotalFare = markUpCommJson.getDouble("totalFare");
		    			
		    			JSONObject rsPassengerPriceJson = rsCategoryJson.getJSONArray("PassengerPrices").getJSONObject(k);
		    			
		    			if(retainSuppFares)
		    			{
			    			JSONArray clientCommJsonArr = rsCategoryJson.optJSONArray("suppPassengerPrices");
			    			
			    			JSONObject clientCommPriceJson = new JSONObject(rsPassengerPriceJson.toString());
			    			/*clientCommPriceJson.put("PassengerType", passengerType);
			    			clientCommPriceJson.put("Total fare", markupTotalFare);
			    			clientCommPriceJson.put("fareBreakUp", markUpCommJson.getJSONObject("fareBreakUp"));*/
			    			
			    			if(clientCommJsonArr==null)
			    			{
			    				JSONArray suppPassJsonArr = new JSONArray();
			    				suppPassJsonArr.put(clientCommPriceJson);
			    				
			    				rsCategoryJson.put("suppPassengerPrices", suppPassJsonArr);
			    			}
			    			else
			    			{
			    				clientCommJsonArr.put(clientCommPriceJson);
			    			}
		    			}
		    			
		    			rsPassengerPriceJson.put("PassengerType", passengerType);
		    			rsPassengerPriceJson.put("Total fare", markupTotalFare);
		    			rsPassengerPriceJson.put("fareBreakUp", markUpCommJson.getJSONObject("fareBreakUp"));
		    			
		    			//changing the total fare ka value in Price Infos also
	//----------------------------------x--------------------------------------x--------------------------------------x------------------------------------x-------------------------------x--------------------------x
		    			
		    			/*JSONArray priceInfosJsonArr = rsCategoryJson.getJSONArray("PriceInfo");
		    			
		    			for(int k=0; i<priceInfosJsonArr.length();i++)
		    			{
		    				JSONObject priceInfosJson =	priceInfosJsonArr.getJSONObject(k);
		    				
		    				if(priceInfosJson.getString("ChargeTypeCode").equalsIgnoreCase("Total Fare"))
		    				{
		    					priceInfosJson.put("Amount", markupTotalFare);
		    					break;
		    				}
		    			}*/
	    			}
	    		}
			}
	 }
	
	 public static void calculatePricesV2(JSONObject reqJson, JSONObject resJson,JSONObject suppCommResJson, JSONObject clientCommResJson, Map<String,String> SI2BRMSSailingOptionMap,boolean retainSuppFares) {
		 
		 JSONArray rsCruiseOptionsJsonArr = resJson.getJSONObject("responseBody").getJSONArray("cruiseOptions");
		 JSONArray ccommSuppBRIJsonArr = clientCommResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONArray("value").getJSONObject(0).getJSONObject("cnk.cruise_commercialscalculationengine.clienttransactionalrules.Root").getJSONArray("businessRuleIntake");
		 
		 Map<Integer,JSONObject> idxToBriMap = new HashMap<Integer,JSONObject>();
		 Map<Integer,Map<String,String>> idxToBriCommHeadMap = new HashMap<Integer,Map<String,String>>();
		 
		 getCommHeadMap(ccommSuppBRIJsonArr, idxToBriMap, idxToBriCommHeadMap);
		 
		 for(int i=0;i<rsCruiseOptionsJsonArr.length();i++)
		 {
			 JSONObject rsCruiseOptionsJson = rsCruiseOptionsJsonArr.getJSONObject(i);
			 JSONArray rsSailingOptionsArr = rsCruiseOptionsJson.getJSONArray("SailingOption");
			 BigDecimal paymentAmount = new BigDecimal(0);
			 
			 for(int j=0;j<rsSailingOptionsArr.length();j++)
			 {
				 String[] valueIdx = SI2BRMSSailingOptionMap.get(String.format("%s|%s",i,j)).split("|");
				 int briIdx = Integer.parseInt(valueIdx[0]);
				 int commCruDtlsIdx = Integer.parseInt(valueIdx[2]);
				 
				 JSONObject ccommBriJson = idxToBriMap.get(briIdx);
				 if(ccommBriJson==null)
					 break;//should never happen
				 
				 JSONArray ccommCruiseDetailsJsonArr = ccommBriJson.getJSONArray("cruiseDetails");
				 Map<String,String> commercialHeadMap = idxToBriCommHeadMap.get(briIdx);
				 
				 JSONArray rsCategoryJsonArr = rsSailingOptionsArr.getJSONObject(j).getJSONArray("Category");
				 JSONArray ccommCabinDetailsJsonArr = ccommCruiseDetailsJsonArr.getJSONObject(commCruDtlsIdx).getJSONArray("cabinDetails");
				 
				 for(int k=0;k<rsCategoryJsonArr.length();k++)
				 {
					 JSONArray rsPassPriceJsonArr =	rsCategoryJsonArr.getJSONObject(k).getJSONArray("PassengerPrices");
					 JSONArray ccommPassDetailsJsonArr = ccommCabinDetailsJsonArr.getJSONObject(k).getJSONArray("passengerDetails");
					 
					 if(retainSuppFares)
					 {
						 //TODO
						 JSONArray rsSuppPassPriceJsonArr = new JSONArray(rsPassPriceJsonArr.toString());
						 rsCategoryJsonArr.getJSONObject(k).put("SuppPassengerPrices", rsSuppPassPriceJsonArr);
					 }
					 
					 for(int l=0;l<rsPassPriceJsonArr.length();l++)
					 {
						 JSONObject rsPassPriceJson = rsPassPriceJsonArr.getJSONObject(l);
						 JSONObject ccommPassDetailsJson = ccommPassDetailsJsonArr.getJSONObject(l);
						 
						 JSONArray commEntityCommercialsJsonArr = ccommPassDetailsJson.getJSONArray("entityCommercials");
						 
						 JSONObject	additionalCommercialsPriceJson = getAdditionalCommercialDetailsPrice(commEntityCommercialsJsonArr,commercialHeadMap); //Calculates all the receivables from additionalDetails in every
						 BigDecimal additionalCommercialsPrice = new BigDecimal(0);
						 additionalCommercialsPrice=additionalCommercialsPriceJson.getBigDecimal("total");
						 																												   //entityCommercials
						//Now we need to get the entity commercial with Markup 
						 JSONObject commMrkupEntyCommJson = getMrkUpCommDtls(commEntityCommercialsJsonArr);
						 
						 if(commMrkupEntyCommJson!=null)//what to do if mark up never comes
						 {
							 BigDecimal baseFare = new BigDecimal(0);
							 BigDecimal totalFare = new BigDecimal(0);
							 baseFare = commMrkupEntyCommJson.getJSONObject("fareBreakUp").getBigDecimal("baseFare");
							 totalFare = baseFare.add(additionalCommercialsPrice);// Markup BaseFare + AdditionalCommDtls = new Total Fare
							 
							 rsPassPriceJson.put("fareBreakUp", commMrkupEntyCommJson.getJSONObject("fareBreakUp"));
							 rsPassPriceJson.put(PRODUCT_CRUISE_TOTALFARE, totalFare);
							 rsPassPriceJson.put("receivables", additionalCommercialsPriceJson.getJSONArray("receivables"));
							 paymentAmount = paymentAmount.add(totalFare);
						 }
						 else
						 {
							 BigDecimal baseFare = new BigDecimal(0);
							 BigDecimal totalFare = new BigDecimal(0);
							 baseFare =	BigDecimal.valueOf(Double.parseDouble(rsPassPriceJson.getJSONObject("fareBreakUp").getString("baseFare")));
							 totalFare = baseFare.add(additionalCommercialsPrice);
							 
							 rsPassPriceJson.put(PRODUCT_CRUISE_TOTALFARE, totalFare);
							 rsPassPriceJson.put("receivables", additionalCommercialsPriceJson.getJSONArray("receivables"));
							 paymentAmount = paymentAmount.add(totalFare);
						 }
					 }
				 }
			 }
			 
			 if(retainSuppFares)
			 {
				 JSONObject bookingPaymentJson = rsCruiseOptionsJson.getJSONObject("BookingPayment");
				 JSONObject paymentScheduleJson = bookingPaymentJson.getJSONObject("PaymentSchedule");
				 JSONObject paymentJson = paymentScheduleJson.getJSONArray("Payment").getJSONObject(0);
				 
				 JSONObject suppPaymentScheduleJson = new JSONObject(paymentScheduleJson.toString());
				 bookingPaymentJson.put("suppPaymentSchedule", suppPaymentScheduleJson);
				 
				 paymentJson.put("Amount", paymentAmount);
			 }
			 
			 
		 }
	 }
	 
 	public static void calculatePricesV3(JSONObject reqJson, JSONObject resJson,JSONObject suppCommResJson, JSONObject clientCommResJson, Map<String,String> SI2BRMSSailingOptionMap,boolean retainSuppFares,UserContext userCtx) {
	 
		 JSONArray rsCruiseOptionsJsonArr = resJson.getJSONObject("responseBody").getJSONArray("cruiseOptions");
		 JSONArray clientCommBRIJsonArr = clientCommResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONArray("value").getJSONObject(0).getJSONObject("cnk.cruise_commercialscalculationengine.clienttransactionalrules.Root").getJSONArray("businessRuleIntake");
		 JSONArray suppClientBRIJsonArr = suppCommResJson.getJSONObject(PRODUCT_CRUISE_SUPPLTRANSRS).getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.cruise_commercialscalculationengine.suppliertransactionalrules.Root").getJSONArray("businessRuleIntake");
		 
		 Map<Integer,JSONObject> clientIdxToBriMap = new HashMap<Integer,JSONObject>();
		 Map<Integer,Map<String,String>> clientIdxToBriCommHeadMap = new HashMap<Integer,Map<String,String>>();
		 Map<Integer,JSONObject> suppIdxToBriMap = new HashMap<Integer,JSONObject>();
		 Map<Integer,Map<String,String>> suppIdxToBriCommHeadMap = new HashMap<Integer,Map<String,String>>();
		 
		 String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
		 String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
		 
		 getCommHeadMap(clientCommBRIJsonArr, clientIdxToBriMap, clientIdxToBriCommHeadMap);
		 getSuppCommHeadMap(suppClientBRIJsonArr,suppIdxToBriMap, suppIdxToBriCommHeadMap);
		 
		 for(int i=0;i<rsCruiseOptionsJsonArr.length();i++)
		 {
			 JSONObject rsCruiseOptionsJson = rsCruiseOptionsJsonArr.getJSONObject(i);
			 JSONArray rsSailingOptionsArr = rsCruiseOptionsJson.getJSONArray("SailingOption");
			 
			 PriceComponentsGroup commTotalFareCompsGroup = new PriceComponentsGroup(JSON_PROP_CRUISETOTALFARE, clientCcyCode, new BigDecimal(0), true);
			 JSONArray commPaxTypeFaresArr = new JSONArray();
			 
			 PriceComponentsGroup suppTotalFareCompsGroup = new PriceComponentsGroup(JSON_PROP_CRUISETOTALFARE, clientCcyCode, new BigDecimal(0), true);
			 JSONArray suppPaxTypeFaresArr = new JSONArray();
			 
			 JSONArray supplierCommercialsTotalsArr = new JSONArray();
			 JSONArray clientCommercialsTotalsArr = new JSONArray();
			 
			 for(int j=0;j<rsSailingOptionsArr.length();j++)
			 {
				 String[] valueIdx = SI2BRMSSailingOptionMap.get(String.format("%s|%s",i,j)).split("|");
				 int briIdx = Integer.parseInt(valueIdx[0]);
				 int commCruDtlsIdx = Integer.parseInt(valueIdx[2]);
				 
				 JSONObject ccommBriJson = clientIdxToBriMap.get(briIdx);
				 if(ccommBriJson==null)
					 break;//should never happen
				 
				 JSONArray ccommCruiseDetailsJsonArr = ccommBriJson.getJSONArray("cruiseDetails");
				 Map<String,String> clientCommercialHeadMap = clientIdxToBriCommHeadMap.get(briIdx);
				 
				 JSONObject scommBriJson = suppIdxToBriMap.get(briIdx);
				 if(scommBriJson==null)
					 break;//should never happen
				 
				 JSONArray scommCruiseDetailsJsonArr = scommBriJson.getJSONArray("cruiseDetails");
				 Map<String,String> suppCommercialHeadMap = suppIdxToBriCommHeadMap.get(briIdx);
				 
				 JSONArray rsCategoryJsonArr = rsSailingOptionsArr.getJSONObject(j).getJSONArray("Category");
				 JSONArray ccommCabinDetailsJsonArr = ccommCruiseDetailsJsonArr.getJSONObject(commCruDtlsIdx).getJSONArray("cabinDetails");
				 JSONArray scommCabinDetailsJsonArr = scommCruiseDetailsJsonArr.getJSONObject(commCruDtlsIdx).getJSONArray("cabinDetails");
				 
				 for(int k=0;k<rsCategoryJsonArr.length();k++)
				 {
					 JSONArray rsPassPriceJsonArr =	rsCategoryJsonArr.getJSONObject(k).getJSONArray("PassengerPrices");
					 JSONArray ccommPassDetailsJsonArr = ccommCabinDetailsJsonArr.getJSONObject(k).getJSONArray("passengerDetails");
					 JSONArray scommPassDetailsJsonArr = scommCabinDetailsJsonArr.getJSONObject(k).getJSONArray("passengerDetails");
					 
					 if(retainSuppFares)
					 {
						 //TODO
						 JSONArray rsSuppPassPriceJsonArr = new JSONArray(rsPassPriceJsonArr.toString());
						 rsCategoryJsonArr.getJSONObject(k).put("SuppPassengerPrices", rsSuppPassPriceJsonArr);
					 }
					 
					 for(int l=0;l<rsPassPriceJsonArr.length();l++)
					 {
						 JSONObject clientEntityDetailsJson=new JSONObject();
						 JSONObject userCtxJson=userCtx.toJSON();
		    		    	JSONArray clientEntityDetailsArr = userCtxJson.getJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS);
		    				for(int y=0;y<clientEntityDetailsArr.length();y++) {
		    					
		    					clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
		    				}
						 
						 JSONObject rsPassPriceJson = rsPassPriceJsonArr.getJSONObject(l);
						 JSONObject ccommPassDetailsJson = ccommPassDetailsJsonArr.getJSONObject(l);
						 JSONObject scommPassDetailsJson = scommPassDetailsJsonArr.getJSONObject(l);
						 
						 JSONArray commEntityCommercialsJsonArr = ccommPassDetailsJson.getJSONArray("entityCommercials");
						 JSONArray suppCommDtlsArr = scommPassDetailsJson.getJSONArray("commercialDetails");
						 
						 JSONObject	clientAddCommPriceJson = getClientCommercialDetailsPriceV2(commEntityCommercialsJsonArr,clientCommercialHeadMap,commTotalFareCompsGroup,clientCcyCode,clientMarket); //Calculates all the receivables from additionalDetails in every
						 JSONObject suppAddCommPriceJson = getSuppCommercialDetailsPrice(suppCommDtlsArr,suppCommercialHeadMap,suppTotalFareCompsGroup,clientCcyCode,clientMarket);
						 
						//Now we need to get the entity commercial with Markup 
						 JSONObject commMrkupEntyCommJson = getMrkUpCommDtls(commEntityCommercialsJsonArr);
						 
						 if(commMrkupEntyCommJson!=null)//what to do if mark up never comes
						 {
							 BigDecimal suppBaseFare = new BigDecimal(0);
							 suppBaseFare =	BigDecimal.valueOf(Double.valueOf(rsPassPriceJson.getJSONObject("fareBreakUp").getString("baseFare")));
							 
							 rsPassPriceJson.put("supplierCommercials", suppAddCommPriceJson.getJSONArray("supplierCommercials"));
							 
							 JSONArray suppCommArr = suppAddCommPriceJson.getJSONArray("supplierCommercials");
							 for(int y=0;y<suppCommArr.length();y++)
							 {
								 supplierCommercialsTotalsArr.put(suppCommArr.getJSONObject(y));
							 }
							 
							 suppTotalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, suppBaseFare);
							 suppPaxTypeFaresArr.put(new JSONObject(rsPassPriceJson.toString()));
							 
							 BigDecimal baseFare = new BigDecimal(0);
							 BigDecimal totalFare = new BigDecimal(0);
							 baseFare = commMrkupEntyCommJson.getJSONObject("fareBreakUp").getBigDecimal("baseFare");
							 totalFare = baseFare.add(clientAddCommPriceJson.getBigDecimal("total"));// Markup BaseFare + AdditionalCommDtls = new Total Fare
							 
							 rsPassPriceJson.put("fareBreakUp", commMrkupEntyCommJson.getJSONObject("fareBreakUp"));
							 rsPassPriceJson.put(PRODUCT_CRUISE_TOTALFARE, totalFare);
							 rsPassPriceJson.put("receivables", clientAddCommPriceJson.getJSONArray("receivables"));
							 
							 clientEntityDetailsJson.put("clientCommercials", clientAddCommPriceJson.getJSONArray("clientCommercials"));
							 rsPassPriceJson.put("clientEntityCommercials", clientEntityDetailsJson);
							 clientCommercialsTotalsArr.put(clientEntityDetailsJson);
							 
							 commTotalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, totalFare);
							 commPaxTypeFaresArr.put(new JSONObject(rsPassPriceJson.toString()));
						 }
						 else
						 {
							 BigDecimal suppBaseFare = new BigDecimal(0);
							 BigDecimal totalFare = new BigDecimal(0);
							 suppBaseFare =	BigDecimal.valueOf(Double.parseDouble(rsPassPriceJson.getJSONObject("fareBreakUp").getString("baseFare")));
							 
							 rsPassPriceJson.put("supplierCommercials", suppAddCommPriceJson.getJSONArray("supplierCommercials"));
							 
							 JSONArray suppCommArr = suppAddCommPriceJson.getJSONArray("supplierCommercials");
							 for(int y=0;y<suppCommArr.length();y++)
							 {
								 supplierCommercialsTotalsArr.put(suppCommArr.getJSONObject(y));
							 }
							 
							 suppTotalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, suppBaseFare);
							 suppPaxTypeFaresArr.put(new JSONObject(rsPassPriceJson.toString()));
							 
							 totalFare = suppBaseFare.add(clientAddCommPriceJson.getBigDecimal("total"));
							 
							 rsPassPriceJson.put(PRODUCT_CRUISE_TOTALFARE, totalFare);
							 rsPassPriceJson.put("receivables", clientAddCommPriceJson.getJSONArray("receivables"));
							 
							 clientEntityDetailsJson.put("clientCommercials", clientAddCommPriceJson.getJSONArray("clientCommercials"));
							 rsPassPriceJson.put("clientEntityCommercials", clientEntityDetailsJson);
							 clientCommercialsTotalsArr.put(clientEntityDetailsJson);
							 
							 commTotalFareCompsGroup.add(JSON_PROP_BASEFARE, clientCcyCode, totalFare);
							 commPaxTypeFaresArr.put(new JSONObject(rsPassPriceJson.toString()));
						 }
					 }
				 }
			 }
			 System.out.println("PRICES WITH COMMERCIALS ADDED");
			 System.out.println(suppTotalFareCompsGroup.toJSON());
			 System.out.println(suppPaxTypeFaresArr);
			 System.out.println("SUPPLIER PRICES");
			 System.out.println(commTotalFareCompsGroup.toJSON());
			 System.out.println(commPaxTypeFaresArr);
			 
			 if(retainSuppFares)
			 {
				 JSONArray suppTotComm = new JSONArray();
				 Map<String, JSONObject> suppTotMap = new HashMap<String, JSONObject>();
				 for(int x=0;x<supplierCommercialsTotalsArr.length();x++){
					 JSONObject suppCommTotJson = supplierCommercialsTotalsArr.getJSONObject(x);
					 
					 if(suppTotMap.containsKey(suppCommTotJson.getString("commercialName"))){
						 JSONObject suppCommMapJson = suppTotMap.get(suppCommTotJson.getString("commercialName"));
						 suppCommMapJson.put("commercialAmount", suppCommMapJson.getBigDecimal("commercialAmount").add(suppCommTotJson.getBigDecimal("commercialAmount")));
					 }
					 else{
						 suppTotMap.put(suppCommTotJson.getString("commercialName"), suppCommTotJson);
					 }
				 }
				 
				 for (Entry<String, JSONObject> entry : suppTotMap.entrySet()){
					 suppTotComm.put(entry.getValue());
				 }
				 
				 JSONObject suppPricingInfoJson = new JSONObject();
				 suppPricingInfoJson.put("suppTotalInfo", suppTotalFareCompsGroup.toJSON());
				 suppPricingInfoJson.put("supplierCommercialsTotals", suppTotComm);
				 suppPricingInfoJson.put("suppPaxTypeFare", suppPaxTypeFaresArr);
				 
				 rsCruiseOptionsJson.put("suppPricingInfo", suppPricingInfoJson);
				 
				 //Supplier construction ends here-------------x----------------------x--------------------------x
				 
				 JSONObject commPricingInfoJson = new JSONObject();
				 JSONArray clientTotComm = new JSONArray();
				 Map<String, JSONObject> clientTotMap = new HashMap<String, JSONObject>();
				 String clientID="";String parentClientID="";String commercialEntityType="";String commercialEntityID="";
				 for(int x=0;x<clientCommercialsTotalsArr.length();x++){
					 JSONObject clientCommTotJson = clientCommercialsTotalsArr.getJSONObject(x);
					 
					 clientID = clientCommTotJson.getString("clientID");
					 parentClientID = clientCommTotJson.getString("parentClientID");
					 commercialEntityType = clientCommTotJson.get("commercialEntityType").toString();
					 commercialEntityID = clientCommTotJson.getString("commercialEntityID");
					 
					 JSONArray clientCommArr = clientCommTotJson.getJSONArray("clientCommercials");
					 for(int y=0;y<clientCommArr.length();y++)
					 {
						 JSONObject clientCommJson = clientCommArr.getJSONObject(y);
						 
						 if(clientTotMap.containsKey(clientCommJson.getString("commercialName"))){
							 JSONObject clientCommMapJson = clientTotMap.get(clientCommJson.getString("commercialName"));
							 clientCommMapJson.put("commercialAmount", clientCommMapJson.getBigDecimal("commercialAmount").add(clientCommJson.getBigDecimal("commercialAmount")));
						 }
						 else{
							 clientTotMap.put(clientCommJson.getString("commercialName"), clientCommJson);
						 }
					 }
				 }
				 
				 for (Entry<String, JSONObject> entry : clientTotMap.entrySet()){
					 clientTotComm.put(entry.getValue());
				 }
				 
				 JSONObject cliEntTotCommJson = new JSONObject();
				 cliEntTotCommJson.put("clientID", clientID);
				 cliEntTotCommJson.put("parentClientID", parentClientID);
				 cliEntTotCommJson.put("commercialEntityType", commercialEntityType);
				 cliEntTotCommJson.put("commercialEntityID", commercialEntityID);
				 cliEntTotCommJson.put("clientCommercialsTotal", clientTotComm);
				 
				 commPricingInfoJson.put("clientEntityTotalCommercials", cliEntTotCommJson);
				 commPricingInfoJson.put("TotalInfo", commTotalFareCompsGroup.toJSON());
				 if(!commPricingInfoJson.getJSONObject("TotalInfo").has("receivables")){
					 JSONArray receivablesArr = new JSONArray();
					 commPricingInfoJson.getJSONObject("TotalInfo").put("receivables", receivablesArr);
				 }
				 
				 commPricingInfoJson.put("PaxTypeFare", commPaxTypeFaresArr);
				 rsCruiseOptionsJson.put("PricingInfo", commPricingInfoJson);
			 }
		 }
	 }

	private static void getCommHeadMap(JSONArray ccommSuppBRIJsonArr, Map<Integer, JSONObject> idxToBriMap,
			Map<Integer, Map<String, String>> idxToBriCommHeadMap) {
		for(int i=0;i<ccommSuppBRIJsonArr.length();i++)
		 {
			 JSONObject ccommSuppBRIJson = ccommSuppBRIJsonArr.getJSONObject(i); 
			 idxToBriMap.put(i, ccommSuppBRIJson);
			 
			 JSONArray ccommEntityDetailsJsonArr = ccommSuppBRIJson.getJSONArray("entityDetails");
			 Map<String,String> commercialHeadMap = new HashMap<String,String>();
			 for(int j=0;j<ccommEntityDetailsJsonArr.length();j++)
			 {
				 JSONArray ccommCommercialHeadJsonArr =	ccommEntityDetailsJsonArr.getJSONObject(j).getJSONArray("commercialHead");
				 
				 for(int k=0;k<ccommCommercialHeadJsonArr.length();k++)
				 {
					 JSONObject ccommCommercialHeadJson = ccommCommercialHeadJsonArr.getJSONObject(k);
					 
					 if(commercialHeadMap.containsKey(ccommCommercialHeadJson.getString("commercialHeadName")))
					 {
						 if(!commercialHeadMap.get(ccommCommercialHeadJson.getString("commercialHeadName")).equalsIgnoreCase(ccommCommercialHeadJson.getString("commercialType")))
							 continue;//should never happen...not sure what to do if this ever happens
						 
						 commercialHeadMap.put(ccommCommercialHeadJson.getString("commercialHeadName"), ccommCommercialHeadJson.getString("commercialType"));
					 }
					 else
					 {
						 commercialHeadMap.put(ccommCommercialHeadJson.getString("commercialHeadName"), ccommCommercialHeadJson.getString("commercialType"));
					 }
				 }
			 }
			 idxToBriCommHeadMap.put(i, commercialHeadMap);
		 }
	}
	
	private static void getSuppCommHeadMap(JSONArray ccommSuppBRIJsonArr,Map<Integer,JSONObject> suppIdxToBriMap,Map<Integer, Map<String, String>> idxToBriCommHeadMap) {
		for(int i=0;i<ccommSuppBRIJsonArr.length();i++)
		 {
			 JSONObject ccommSuppBRIJson = ccommSuppBRIJsonArr.getJSONObject(i); 
			 suppIdxToBriMap.put(i, ccommSuppBRIJson);
			 
			 Map<String,String> commercialHeadMap = new HashMap<String,String>();
			 JSONArray ccommCommercialHeadJsonArr =	ccommSuppBRIJson.getJSONArray("commercialHead");
			 
			 for(int k=0;k<ccommCommercialHeadJsonArr.length();k++)
			 {
				 JSONObject ccommCommercialHeadJson = ccommCommercialHeadJsonArr.getJSONObject(k);
				 
				 if(commercialHeadMap.containsKey(ccommCommercialHeadJson.getString("commercialHeadName")))
				 {
					 if(!commercialHeadMap.get(ccommCommercialHeadJson.getString("commercialHeadName")).equalsIgnoreCase(ccommCommercialHeadJson.getString("commercialType")))
						 continue;//should never happen...not sure what to do if this ever happens
					 
					 commercialHeadMap.put(ccommCommercialHeadJson.getString("commercialHeadName"), ccommCommercialHeadJson.getString("commercialType"));
				 }
				 else
				 {
					 commercialHeadMap.put(ccommCommercialHeadJson.getString("commercialHeadName"), ccommCommercialHeadJson.getString("commercialType"));
				 }
			 }
			 idxToBriCommHeadMap.put(i, commercialHeadMap);
		 }
	}
	 
	 public static JSONObject getMrkUpCommDtls(JSONArray commEntityCommercialsJsonArr)
	 {
		 for(int m=commEntityCommercialsJsonArr.length()-1;m>=0;m--)//iterating the loop from the back to get the latest Markup among the rest
		 {
			 JSONObject commMrkupEntyCommJson = commEntityCommercialsJsonArr.getJSONObject(m).optJSONObject("markUpCommercialDetails");
			 
			 if(commMrkupEntyCommJson==null)
				 continue;
			 
			 return commMrkupEntyCommJson;
		 }
		 return null;
	 }
	 public static JSONObject getClientCommercialDetailsPriceV2(JSONArray commEntityCommercialsJsonArr, Map<String,String> clientCommercialHeadMap,PriceComponentsGroup commTotalFareCompsGroup,String clientCcyCode,String clientMarket)
	 {
		 BigDecimal additionalCommercialsPrice = new BigDecimal(0);
		 JSONObject	clientAdditionalCommercialsPriceJson = new JSONObject();
		 BigDecimal paxTypeTotalFare = new BigDecimal(0);
//		 JSONObject additonalCommercialJson = new JSONObject();
		 JSONArray additionalReceivableCommercialsArr = new JSONArray();
		 JSONArray clientEntityCommercialsJsonArr = new JSONArray();
		 for(int x=0;x<commEntityCommercialsJsonArr.length();x++)
		 {
			 JSONArray commAddCommDtlsJsonArr = commEntityCommercialsJsonArr.getJSONObject(x).getJSONArray("additionalCommercialDetails");
			 if(commAddCommDtlsJsonArr!=null)
			 {
				 for(int y=0;y<commAddCommDtlsJsonArr.length();y++)
				 {
					 JSONObject commAddCommDtlsJson = commAddCommDtlsJsonArr.getJSONObject(y);
					 String additionalCommName = commAddCommDtlsJson.getString("commercialName");
					 if(clientCommercialHeadMap.get(commAddCommDtlsJson.getString("commercialName")).equalsIgnoreCase("Receivable"))
					 {
				 		String additionalCommCcy = commAddCommDtlsJson.getString(JSON_PROP_COMMCCY);
						BigDecimal additionalCommAmt = commAddCommDtlsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
						paxTypeTotalFare = paxTypeTotalFare.add(additionalCommAmt);
						commTotalFareCompsGroup.add(mRcvsPriceCompQualifier.concat(additionalCommName).concat("@").concat(JSON_PROP_CODE), clientCcyCode, additionalCommAmt);
						 
						additionalReceivableCommercialsArr.put(commAddCommDtlsJson);
						additionalCommercialsPrice = additionalCommercialsPrice.add(BigDecimal.valueOf(Double.valueOf(commAddCommDtlsJson.getString("commercialAmount"))));
						
					 }
					 JSONObject clientEntityCommercialsJson = new JSONObject();
					 
					 clientEntityCommercialsJson.put("commercialType", clientCommercialHeadMap.get(commAddCommDtlsJson.getString("commercialName")));
					 clientEntityCommercialsJson.put("commercialCurrency", clientCcyCode);
					 clientEntityCommercialsJson.put("commercialAmount", commAddCommDtlsJson.getBigDecimal("commercialAmount"));
					 clientEntityCommercialsJson.put("commercialName", additionalCommName);
					 
					 clientEntityCommercialsJsonArr.put(clientEntityCommercialsJson);
				 }
			 }
			 
			 JSONArray commFixCommDtlsJsonArr = commEntityCommercialsJsonArr.getJSONObject(x).optJSONArray("fixedCommercialDetails");
			 if(commFixCommDtlsJsonArr!=null)
			 {
				 for(int y=0;y<commFixCommDtlsJsonArr.length();y++)
				 {
					 JSONObject commFixCommDtlsJson = commFixCommDtlsJsonArr.getJSONObject(y);
					 String CommName = commFixCommDtlsJson.getString("commercialName");
					 
					 JSONObject clientEntityCommercialsJson = new JSONObject();
					 
					 clientEntityCommercialsJson.put("commercialType", clientCommercialHeadMap.get(commFixCommDtlsJson.getString("commercialName")));
					 clientEntityCommercialsJson.put("commercialCurrency", clientCcyCode);
					 clientEntityCommercialsJson.put("commercialAmount", commFixCommDtlsJson.getBigDecimal("commercialAmount"));
					 clientEntityCommercialsJson.put("commercialName", CommName);
					 
					 clientEntityCommercialsJsonArr.put(clientEntityCommercialsJson);
				 }
			 }
			 
			 JSONArray commRetCommDtlsJsonArr = commEntityCommercialsJsonArr.getJSONObject(x).optJSONArray("retentionCommercialDetails");
			 if(commRetCommDtlsJsonArr!=null)
			 {
				 for(int y=0;y<commRetCommDtlsJsonArr.length();y++)
				 {
					 JSONObject commRetCommDtlsJson = commRetCommDtlsJsonArr.getJSONObject(y);
					 String CommName = commRetCommDtlsJson.getString("commercialName");
					 
					 JSONObject clientEntityCommercialsJson = new JSONObject();
					 
					 clientEntityCommercialsJson.put("commercialType", clientCommercialHeadMap.get(commRetCommDtlsJson.getString("commercialName")));
					 clientEntityCommercialsJson.put("commercialCurrency", clientCcyCode);
					 clientEntityCommercialsJson.put("commercialAmount", commRetCommDtlsJson.getBigDecimal("commercialAmount"));
					 clientEntityCommercialsJson.put("commercialName", CommName);
					 
					 clientEntityCommercialsJsonArr.put(clientEntityCommercialsJson);
				 }
			 }
			 
			 JSONObject commMarkUpCommDtlsJson = commEntityCommercialsJsonArr.getJSONObject(x).optJSONObject("markUpCommercialDetails");
			 if(commMarkUpCommDtlsJson!=null)
			 {
					 String CommName = commMarkUpCommDtlsJson.getString("commercialName");
					 
					 JSONObject clientEntityCommercialsJson = new JSONObject();
					 
					 clientEntityCommercialsJson.put("commercialType", clientCommercialHeadMap.get(commMarkUpCommDtlsJson.getString("commercialName")));
					 clientEntityCommercialsJson.put("commercialCurrency", clientCcyCode);
					 clientEntityCommercialsJson.put("commercialAmount", commMarkUpCommDtlsJson.getBigDecimal("commercialAmount"));
					 clientEntityCommercialsJson.put("commercialName", CommName);
					 
					 clientEntityCommercialsJsonArr.put(clientEntityCommercialsJson);
			 }
		 }
		 clientAdditionalCommercialsPriceJson.put("total", additionalCommercialsPrice);
		 clientAdditionalCommercialsPriceJson.put("receivables", additionalReceivableCommercialsArr);
		 clientAdditionalCommercialsPriceJson.put("clientCommercials", clientEntityCommercialsJsonArr);
		 return clientAdditionalCommercialsPriceJson;
	 }
	 
	 public static JSONObject getAdditionalCommercialDetailsPrice(JSONArray commEntityCommercialsJsonArr, Map<String,String> commercialHeadMap)
	 {
		 BigDecimal additionalCommercialsPrice= new BigDecimal(0);
		 JSONObject additonalCommercialJson = new JSONObject();
		 JSONArray additionalCommercialsArr = new JSONArray();
		 for(int i=0;i<commEntityCommercialsJsonArr.length();i++)
		 {
			 JSONArray commAddCommDtlsJsonArr = commEntityCommercialsJsonArr.getJSONObject(i).getJSONArray("additionalCommercialDetails");
			 for(int j=0;j<commAddCommDtlsJsonArr.length();j++)
			 {
				 JSONObject commAddCommDtlsJson = commAddCommDtlsJsonArr.getJSONObject(j);
				 if(commercialHeadMap.get(commAddCommDtlsJson.getString("commercialName")).equalsIgnoreCase("Receivable"))
				 {
					 additionalCommercialsArr.put(commAddCommDtlsJson);
					 additionalCommercialsPrice = additionalCommercialsPrice.add(BigDecimal.valueOf(Double.valueOf(commAddCommDtlsJson.getString("commercialAmount"))));
				 }
			 }
		 }
		 
		 additonalCommercialJson.put("total", additionalCommercialsPrice);
		 additonalCommercialJson.put("receivables", additionalCommercialsArr);
		 return additonalCommercialJson;
	 }
	 
	 public static JSONObject getSuppCommercialDetailsPrice(JSONArray scommPassDetailsJson, Map<String,String> suppCommercialHeadMap, PriceComponentsGroup suppTotalFareCompsGroup,String clientCcyCode,String clientMarket)
	 {
		 JSONObject supplierCommercialJson = new JSONObject();
		 JSONArray supplierCommercialsArr = new JSONArray();
		 for(int i=0;i<scommPassDetailsJson.length();i++)
		 {
			 JSONObject commAddCommDtlsJson = scommPassDetailsJson.getJSONObject(i);
			 String additionalCommName = commAddCommDtlsJson.getString("commercialName");
//			 if(suppCommercialHeadMap.get(commAddCommDtlsJson.getString("commercialName")).equalsIgnoreCase("Receivable"))
			 {
				 JSONObject suppCommJson = new JSONObject();
				 
				 suppCommJson.put("commercialName", additionalCommName);
				 suppCommJson.put("commercialAmount", commAddCommDtlsJson.getBigDecimal("commercialAmount"));
				 suppCommJson.put("commercialType", suppCommercialHeadMap.get(commAddCommDtlsJson.getString("commercialName")));
				 suppCommJson.put("commercialCurrency", clientCcyCode);
				 
				 supplierCommercialsArr.put(suppCommJson);
				 
				 /*String additionalCommCcy = commAddCommDtlsJson.optString(JSON_PROP_COMMCCY);
				 BigDecimal additionalCommAmt = new BigDecimal(0);
				 if(additionalCommCcy!=null && !additionalCommCcy.isEmpty())
				 additionalCommAmt = commAddCommDtlsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
				 else
				 additionalCommAmt = commAddCommDtlsJson.getBigDecimal(JSON_PROP_COMMAMOUNT);
				 
				 suppTotalFareCompsGroup.add(mRcvsPriceCompQualifier.concat(additionalCommName).concat("@").concat(JSON_PROP_CODE), clientCcyCode, additionalCommAmt);*/
				 
			 }
		 }
		 
		 supplierCommercialJson.put("supplierCommercials", supplierCommercialsArr);
		 return supplierCommercialJson;
	 }
	 
	 public static void getSupplierResponseSailingOptionsJSON(Element wrapperElem, JSONArray sailingOptionJsonArr) throws Exception {
		 
//		 getSupplierResponseSailingOptionsJSONV2(wrapperElem, sailingOptionJsonArr,false);
		 getSupplierResponseSailingOptionsJSON(wrapperElem, sailingOptionJsonArr,false);
    }
	 
	 public static void getSupplierResponseSailingOptionsJSONV2(Element wrapperElem, JSONArray sailingOptionJsonArr,Boolean value) throws Exception {
		 
		 Random random = new Random();
		 String suppID = XMLUtils.getValueAtXPath(wrapperElem, "./cru1:SupplierID");
		 Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_CruiseSailAvailRS");
		 Element[] sailingOptionElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:SailingOptions/ota:SailingOption");
		 for(Element sailingOptionElem : sailingOptionElems)
		 {
			 JSONObject cruiseOption = new JSONObject();
			 
			 Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(sailingOptionElem, "./ota:SelectedSailing");
			 
			 cruiseOption.put("VoyageID", XMLUtils.getValueAtXPath(selectedSailingElem, "./@VoyageID"));
			 cruiseOption.put("Start", XMLUtils.getValueAtXPath(selectedSailingElem, "./@Start"));
			 cruiseOption.put("End", XMLUtils.getValueAtXPath(selectedSailingElem, "./@End"));
			 
			 Element cruiseElem = XMLUtils.getFirstElementAtXPath(sailingOptionElem, "./ota:TPA_Extensions/cru1:Cruise");
			 cruiseOption.put("ItineraryId", XMLUtils.getValueAtXPath(cruiseElem, "./cru1:Itinerary/@ItineraryId"));
			 
			 Element sailingElem = XMLUtils.getFirstElementAtXPath(cruiseElem, "./cru1:SailingDates/cru1:Sailing");
			 cruiseOption.put("SailingID", XMLUtils.getValueAtXPath(sailingElem, "./@SailingID"));
			 
			 cruiseOption.put("CurrencyCode", XMLUtils.getValueAtXPath(sailingOptionElem, "./ota:Currency/@CurrencyCode"));
			 
			 cruiseOption.put("CabinType", "CC");
			 cruiseOption.put("cruiseNumber",String.valueOf(random.nextInt(900) + 100 ));
			 cruiseOption.put("supplierRef", suppID);
			 
			 JSONObject pricesJson = getPrices(sailingElem,cruiseElem);
			 
			 cruiseOption.put("Prices", pricesJson);
			 
			 JSONObject sailingOption = getsailingOptionJSON(sailingOptionElem);
			 cruiseOption.put("sailingOption", sailingOption);
			 
			 sailingOptionJsonArr.put(cruiseOption);
		 }
	 }
	 
	 private static JSONObject getPrices(Element sailingElem,Element cruiseElem)
	 {
		 JSONObject pricesJson = new JSONObject();
		 
		 String totalFare = XMLUtils.getValueAtXPath(sailingElem, "./@BL_Price");
		 String tax = XMLUtils.getValueAtXPath(cruiseElem, "./@Tax");
		 
		 pricesJson.put("TotalPrice",totalFare);
		 pricesJson.put("TaxIncluded", XMLUtils.getValueAtXPath(cruiseElem, "./@TaxesIncluded"));
		 pricesJson.put("BaseFare", String.valueOf(Double.valueOf(totalFare)-Double.valueOf(tax)));
		 
		 JSONArray taxJsonArr = new JSONArray();
		 
		 JSONObject taxJson = new JSONObject();
		 taxJson.put("TaxName", "ABC");
		 taxJson.put("TaxValue", tax);
		 taxJsonArr.put(taxJson);
		 
		 pricesJson.put("TaxDetails", taxJson);
		 
		 return pricesJson;
	 }
	 
	 public static void getSupplierResponseSailingOptionsJSON(Element resBodyElem, JSONArray sailingOptionJsonArr,Boolean value) throws Exception {
	    	
		 Element[] sailingOptionElems = XMLUtils.getElementsAtXPath(resBodyElem, "./ota:OTA_CruiseSailAvailRS/ota:SailingOptions/ota:SailingOption");
		 
		 for(Element sailingOptionElem : sailingOptionElems)
		 {
			 JSONObject sailingOptionJson = getsailingOptionJSON(sailingOptionElem);
			 
			 sailingOptionJson.put("supplierRef", XMLUtils.getValueAtXPath(resBodyElem, "./cru1:SupplierID"));
			 sailingOptionJson.put("MaxCabinOccupancy", XMLUtils.getValueAtXPath(sailingOptionElem, "./@MaxCabinOccupancy"));
			 sailingOptionJson.put("CategoryLocation", XMLUtils.getValueAtXPath(sailingOptionElem, "./@CategoryLocation"));
			 
			 sailingOptionJsonArr.put(sailingOptionJson);
		 }
    }
	 
	 
	 private static JSONObject getsailingOptionJSON(Element sailingOptionElem)
	 {
		 Random random = new Random();
		 JSONObject sailingOptionJSON = new JSONObject();
		 
		 Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(sailingOptionElem, "./ota:SelectedSailing");
		 if(selectedSailingElem!=null)
		 {
			 sailingOptionJSON.put("SelectedSailing", getSelectedSailingJSON(selectedSailingElem));
			 sailingOptionJSON.put("startDate",XMLUtils.getValueAtXPath(selectedSailingElem, "./@Start"));
		 }
		 Element inclusivePackageOptionElem =	XMLUtils.getFirstElementAtXPath(sailingOptionElem, "./ota:InclusivePackageOption");
		 if(inclusivePackageOptionElem!=null)
		 sailingOptionJSON.put("InclusivePackageOption", getInclusivePackageOptionJson(inclusivePackageOptionElem));
		 
		 Element currencyElem =	XMLUtils.getFirstElementAtXPath(sailingOptionElem, "./ota:Currency");
		 if(currencyElem!=null)
		 sailingOptionJSON.put("CurrencyCode", XMLUtils.getValueAtXPath(currencyElem, "./@CurrencyCode"));
		 
		 Element[] informationElems = XMLUtils.getElementsAtXPath(sailingOptionElem, "./ota:Information");
		 if(informationElems!=null)
		 sailingOptionJSON.put("Information", getInformationJsonArr(informationElems));
		 
		 Element tpa_Extensions = XMLUtils.getFirstElementAtXPath(sailingOptionElem, "./ota:TPA_Extensions");
		 if(tpa_Extensions!=null)
		 {
			 sailingOptionJSON.put("ItineraryID", XMLUtils.getValueAtXPath(sailingOptionElem, "./cru1:ItineraryID"));
			 sailingOptionJSON.put("UsesDynamicPricing", XMLUtils.getValueAtXPath(sailingOptionElem, "./cru1:UsesDynamicPricing"));
			 sailingOptionJSON.put("ShopInsurance", XMLUtils.getValueAtXPath(sailingOptionElem, "./cru1:PricingShopInfo/cru1:ShopInsurance"));
			 sailingOptionJSON.put("Discountable", XMLUtils.getValueAtXPath(sailingOptionElem, "./cru1:Discountable"));
			 
			 sailingOptionJSON.put("Category", getCategoryJsonArr(tpa_Extensions));
			 
//			 sailingOptionJSON.put("FareCodeOption", getFareCodeOptionJsonArr(tpa_Extensions));
		 }
		 
		 sailingOptionJSON.put("cruiseNumber",String.valueOf(random.nextInt(900) + 100 ));
		 
		 return sailingOptionJSON;
	}
	
	private static JSONArray getFareCodeOptionJsonArr(Element tpa_Extensions)
    {
		JSONArray fareCodeOptionJsonArr = new JSONArray();
		
		Element[] fareCodeOptionElems =	XMLUtils.getElementsAtXPath(tpa_Extensions, "./cru1:FareCodeOptions/cru1:FareCodeOption");
		
		for(Element fareCodeOptionElem : fareCodeOptionElems)
		{
			JSONObject fareCodeOptionJson = new JSONObject();
			
			fareCodeOptionJson.put("Status", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./@Status"));
			fareCodeOptionJson.put("FareDescription", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./@FareDescription"));
			fareCodeOptionJson.put("FareCode", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./@FareCode"));
			fareCodeOptionJson.put("ListOfFareQualifierCode", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./@ListOfFareQualifierCode"));
			fareCodeOptionJson.put("FareRemark", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./cru1:FareRemark"));
			fareCodeOptionJson.put("Discountable", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./cru1:TPA_Extensions/cru1:Discountable"));
			fareCodeOptionJson.put("PromotionTypes", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./cru1:TPA_Extensions/cru1:PromotionTypes"));
			fareCodeOptionJson.put("PricingMethod", XMLUtils.getValueAtXPath(fareCodeOptionElem, "./cru1:TPA_Extensions/cru1:PricingMethod"));
			
			fareCodeOptionJsonArr.put(fareCodeOptionJson);
		}
		
		return fareCodeOptionJsonArr;
    }
	 
	private static JSONArray getCategoryJsonArr(Element tpa_Extensions)
    {
		JSONArray categoryJsonArr = new JSONArray();
		
		Element[] fareCodeOptionElems =	XMLUtils.getElementsAtXPath(tpa_Extensions, "./cru1:FareCodeOptions/cru1:FareCodeOption");
		
		for(Element fareCodeOptionElem : fareCodeOptionElems)
		{
			String fareCode = XMLUtils.getValueAtXPath(fareCodeOptionElem, "./@FareCode");
			Element[] categoryElems = XMLUtils.getElementsAtXPath(fareCodeOptionElem, "./cru1:CategoryOptions/cru1:CategoryOption");
			for(Element categoryElem : categoryElems)
			{
				JSONObject categoryJson = new JSONObject();
				
				categoryJson.put("FareCode", fareCode);
				categoryJson.put("PricedCategoryCode", XMLUtils.getValueAtXPath(categoryElem, "./@PricedCategoryCode"));
				categoryJson.put("CategoryLocation", XMLUtils.getValueAtXPath(categoryElem, "./@CategoryLocation"));
				categoryJson.put("MaxOccupancy", XMLUtils.getValueAtXPath(categoryElem, "./@MaxOccupancy"));
				
				boolean taxIncluded=false;double tax = 0.0;
				
				String check =	XMLUtils.getValueAtXPath(tpa_Extensions, "./cru1:Cruise/@TaxesIncluded");//because MSC and Star dont give tax
				if(!check.isEmpty() && check!=null)
				{
					taxIncluded = Boolean.valueOf(check);
				}
				
				String checkTax = XMLUtils.getValueAtXPath(tpa_Extensions, "./cru1:Cruise/@Tax");	//because MSC and Star dont give tax
				if(!checkTax.isEmpty() && checkTax!=null)
				{
					tax = Double.valueOf(checkTax);
				}
				
				Element[] priceInfosElems = XMLUtils.getElementsAtXPath(categoryElem, "./cru1:PriceInfos/cru1:PriceInfo");
				
				if(priceInfosElems!=null)
				{
					categoryJson.put("PassengerPrices", getPassengerPrices(priceInfosElems,taxIncluded,tax));
					
					categoryJson.put("PriceInfo", getPriceInfosJsonArr(priceInfosElems));
				}
				categoryJsonArr.put(categoryJson);
			}
		}
		
		return categoryJsonArr;
    }
	 
	public static JSONArray getPassengerPrices(Element[] priceInfosElems,boolean taxIncluded,double tax)
    {
		JSONArray passengerPriceJsonArr = new JSONArray();
		
		for(Element priceInfoElem : priceInfosElems)
		{
			String totalFareName = XMLUtils.getValueAtXPath(priceInfoElem, "./ota:PriceDescription");
			if(totalFareName==null ||totalFareName.isEmpty())
				totalFareName = XMLUtils.getValueAtXPath(priceInfoElem, "./cru1:PriceDescription");//for search
			if(totalFareName==null ||totalFareName.isEmpty())
				totalFareName = XMLUtils.getValueAtXPath(priceInfoElem, "./@PriceTypeCode");//for re-price
			if(totalFareName==null ||totalFareName.isEmpty())
				totalFareName = XMLUtils.getValueAtXPath(priceInfoElem, "./@CodeDetail");//for re-price
			if(totalFareName==null ||totalFareName.isEmpty())
				totalFareName = XMLUtils.getValueAtXPath(priceInfoElem, "./@ChargeTypeCode");//for re-price
				
			JSONObject passengerPriceJson = new JSONObject();
			
			if(totalFareName.equalsIgnoreCase("Lowest Available Fare") || totalFareName.equalsIgnoreCase("8") || totalFareName.equalsIgnoreCase("TotalPrice") || totalFareName.equalsIgnoreCase("TotalCabinPrice") || totalFareName.equalsIgnoreCase("30"))	//if(totalFareName.equalsIgnoreCase("Total Fare"))
			{
				passengerPriceJson.put("PassengerType", "ADT");
				double baseFare=0;
				double totalFare = Double.valueOf(XMLUtils.getValueAtXPath(priceInfoElem, "./@Amount"));
				
				String currencyCode = XMLUtils.getValueAtXPath(priceInfoElem, "./@CurrencyCode");
				passengerPriceJson.put(XML_ATTR_CURRENCYCODE, currencyCode);
				if(taxIncluded)
				{
					passengerPriceJson.put(PRODUCT_CRUISE_TOTALFARE, String.valueOf(totalFare));
					baseFare = totalFare - tax;
				}
				else
				{
					passengerPriceJson.put(PRODUCT_CRUISE_TOTALFARE, String.valueOf(totalFare + tax));
					baseFare = totalFare;
				}
				
				JSONObject fareBreakUpJson = getFareBreakUp(baseFare,tax);
				passengerPriceJson.put("fareBreakUp", fareBreakUpJson);
				passengerPriceJsonArr.put(passengerPriceJson);
				break;
			}
		}
		
		return passengerPriceJsonArr;
    }
	
	private static JSONObject getFareBreakUp(double baseFare,double tax)
    {
		JSONObject fareBreakUpJson = new JSONObject();
		
		fareBreakUpJson.put("baseFare", String.valueOf(baseFare));
		
		fareBreakUpJson.put("taxDetails", new JSONArray());
		
		return fareBreakUpJson;
    }
	
	private static JSONArray getPriceInfosJsonArr(Element[] priceInfosElems)
    {
		JSONArray priceInfoJsonArr = new JSONArray();
		
		for(Element priceInfoElem : priceInfosElems)
		{
			JSONObject priceInfoJson = new JSONObject();
			
			priceInfoJson.put("BreakdownType", XMLUtils.getValueAtXPath(priceInfoElem, "./@BreakdownType"));
			priceInfoJson.put("ChargeTypeCode", XMLUtils.getValueAtXPath(priceInfoElem, "./@ChargeTypeCode"));
			priceInfoJson.put("CurrencyCode", XMLUtils.getValueAtXPath(priceInfoElem, "./@CurrencyCode"));
			priceInfoJson.put("Amount", XMLUtils.getValueAtXPath(priceInfoElem, "./@Amount"));
			priceInfoJson.put("FareCode", XMLUtils.getValueAtXPath(priceInfoElem, "./@FareCode"));
			priceInfoJson.put("GroupCode", XMLUtils.getValueAtXPath(priceInfoElem, "./@GroupCode"));
			priceInfoJson.put("AgeQualifyingCode", XMLUtils.getValueAtXPath(priceInfoElem, "./@AgeQualifyingCode"));
			priceInfoJson.put("PriceDescription", XMLUtils.getValueAtXPath(priceInfoElem, "./cru1:PriceDescription"));
			
			priceInfoJsonArr.put(priceInfoJson);
		}
		
		return priceInfoJsonArr;
    }
	
    private static JSONObject getInclusivePackageOptionJson(Element inclusivePackageOptionElem)
    {
		 JSONObject getInclusivePackageOptionJsonJSON =new JSONObject();
		 
		 getInclusivePackageOptionJsonJSON.put("CruisePackageCode", XMLUtils.getValueAtXPath(inclusivePackageOptionElem, "./@Cruise"));
		 getInclusivePackageOptionJsonJSON.put("InclusiveIndicator", XMLUtils.getValueAtXPath(inclusivePackageOptionElem, "./@InclusiveIndicator"));
		 getInclusivePackageOptionJsonJSON.put("Start", XMLUtils.getValueAtXPath(inclusivePackageOptionElem, "./@:Start"));
		 getInclusivePackageOptionJsonJSON.put("End", XMLUtils.getValueAtXPath(inclusivePackageOptionElem, "./@End"));
		 
		 return getInclusivePackageOptionJsonJSON;
    } 
	 
	private static JSONObject getTpa_ExtensionsJSON(Element tpa_Extensions)
    {
		 JSONObject tpa_ExtensionsJSON =new JSONObject();
		 
		 Element cruiseElem = XMLUtils.getFirstElementAtXPath(tpa_Extensions, "./cru1:Cruise");
		 if(cruiseElem!=null)
		 tpa_ExtensionsJSON.put("Cruise", getCruiseJSON(cruiseElem));
		 
		 return tpa_ExtensionsJSON;
    }
	private static JSONObject getCruiseJSON(Element cruiseElem)
	{
		JSONObject cruiseJSON = new JSONObject();
		 
		Element itineraryElem = XMLUtils.getFirstElementAtXPath(cruiseElem, "./cru1:Itinerary");
		if(itineraryElem!=null)
		cruiseJSON.put("Itinerary", getItineraryJSON(itineraryElem));
		 
		Element sailingDatesElem = XMLUtils.getFirstElementAtXPath(cruiseElem, "./cru1:SailingDates");
		if(sailingDatesElem!=null)
		cruiseJSON.put("SailingDates", getSailingDates(sailingDatesElem));
		 
		return cruiseJSON;
	}
	 
	private static JSONObject getItineraryJSON(Element itineraryElem)
    {
		 JSONObject itineraryJSON = new JSONObject();
		 
		 itineraryJSON.put("ItineraryId", XMLUtils.getValueAtXPath(itineraryElem, "./@ItineraryId"));
		 itineraryJSON.put("Name", XMLUtils.getValueAtXPath(itineraryElem, "./@Name"));
		 
		 return itineraryJSON;
    }
	 
	private static JSONObject getSailingDates(Element sailingDatesElem)
    {
		JSONObject sailingDatesJSON = new JSONObject();
		
		Element sailingPriceElem = XMLUtils.getFirstElementAtXPath(sailingDatesElem, "./cru1:Sailing");
		if(sailingPriceElem!=null)
		sailingDatesJSON.put("Sailing", getSailingPriceElem(sailingPriceElem));
		
		return sailingDatesJSON;
    }
	 
	private static JSONObject getSailingPriceElem(Element sailingPriceElem)
    {
		JSONObject pricesJSON = new JSONObject();
		
		pricesJSON.put("BL_Price", XMLUtils.getValueAtXPath(sailingPriceElem, "./@BL_Price"));
		pricesJSON.put("BL_PricePublish", XMLUtils.getValueAtXPath(sailingPriceElem, "./@BL_PricePublish"));
		pricesJSON.put("IN_Price", XMLUtils.getValueAtXPath(sailingPriceElem, "./@IN_Price"));
		pricesJSON.put("IN_PricePublish", XMLUtils.getValueAtXPath(sailingPriceElem, "./@IN_PricePublish"));
		pricesJSON.put("OV_Price", XMLUtils.getValueAtXPath(sailingPriceElem, "./@OV_Price"));
		pricesJSON.put("OV_PricePublish", XMLUtils.getValueAtXPath(sailingPriceElem, "./@OV_PricePublish"));
		pricesJSON.put("ST_Price", XMLUtils.getValueAtXPath(sailingPriceElem, "./@ST_Price"));
		pricesJSON.put("ST_PricePublish", XMLUtils.getValueAtXPath(sailingPriceElem, "./@ST_PricePublish"));
		pricesJSON.put("SailingID", XMLUtils.getValueAtXPath(sailingPriceElem, "./@SailingID"));
		
		return pricesJSON;
    }
	
	private static JSONArray getInformationJsonArr(Element[] informationElems)
    {
		JSONArray informationJsonArr = new JSONArray();
		
		for(Element informationElem : informationElems)
		{
			JSONObject informationJson = new JSONObject();
			
			informationJson.put("Name", XMLUtils.getValueAtXPath(informationElem, "./@Name"));
			
			Element[] textElems = XMLUtils.getElementsAtXPath(informationElem, "./Text");
			informationJson.put("Text", getTextArr(textElems));
			
			informationJsonArr.put(informationJson);
		}
		
		return informationJsonArr;
    }
	 
	private static JSONArray getTextArr(Element[] textElems)
    {
		JSONArray textJsonArr = new JSONArray();
		
		for(Element textElem : textElems)
		{
			JSONObject textJson = new JSONObject();
			textJson.put("Text", XMLUtils.getValueAtXPath(textElem, "/@Formatted"));
			
			textJsonArr.put(textJson);
		}
		
		return textJsonArr;
    }
    public static JSONObject getSelectedSailingJSON(Element selectedSailingElem)
    {
    	JSONObject selectedSailingJSON = new JSONObject();
    	
    	selectedSailingJSON.put("VoyageID", XMLUtils.getValueAtXPath(selectedSailingElem, "./@VoyageID"));
    	selectedSailingJSON.put("Status", XMLUtils.getValueAtXPath(selectedSailingElem, "./Status"));
    	selectedSailingJSON.put("PortsOfCallQuantity", XMLUtils.getValueAtXPath(selectedSailingElem, "./@PortsOfCallQuantity"));
    	selectedSailingJSON.put("Start", XMLUtils.getValueAtXPath(selectedSailingElem, "./@Start"));
    	selectedSailingJSON.put("Duration", XMLUtils.getValueAtXPath(selectedSailingElem, "./@Duration"));
    	selectedSailingJSON.put("End", XMLUtils.getValueAtXPath(selectedSailingElem, "./@End"));
    	
    	Element cruiseLineElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:CruiseLine");
    	if(cruiseLineElem!=null)
    	selectedSailingJSON.put("CruiseLine", getCruiseLineJSON(cruiseLineElem));
    	
    	Element regionElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:Region");
    	if(regionElem!=null)
    	selectedSailingJSON.put("Region", getRegionJSON(regionElem));
    	
    	Element departurePortElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:DeparturePort");
    	if(departurePortElem!=null)
    	selectedSailingJSON.put("DeparturePort", getDeparturePortJSON(departurePortElem));
    	
    	return selectedSailingJSON;
    }
	 
    public static JSONObject getCruiseLineJSON(Element cruiseLineElem)
    {
    	JSONObject cruiseLineJSON = new JSONObject();
    	
		cruiseLineJSON.put("VendorCode", XMLUtils.getValueAtXPath(cruiseLineElem, "./@VendorCode"));
		cruiseLineJSON.put("ShipCode", XMLUtils.getValueAtXPath(cruiseLineElem, "./@ShipCode"));
		cruiseLineJSON.put("ShipName", XMLUtils.getValueAtXPath(cruiseLineElem, "./@ShipName"));
		cruiseLineJSON.put("VendorName", XMLUtils.getValueAtXPath(cruiseLineElem, "./@VendorName"));
		cruiseLineJSON.put("VendorCodeContext", XMLUtils.getValueAtXPath(cruiseLineElem, "./@VendorCodeContext"));
    	
    	return cruiseLineJSON;
    }
    
    public static JSONObject getRegionJSON(Element regionElem)
    {
    	JSONObject regionJSON = new JSONObject();
    	
		regionJSON.put("RegionName", XMLUtils.getValueAtXPath(regionElem, "./@RegionName"));
		regionJSON.put("RegionCode", XMLUtils.getValueAtXPath(regionElem, "./@RegionCode"));
		regionJSON.put("SubRegionName", XMLUtils.getValueAtXPath(regionElem, "./@SubRegionName"));
    	
    	return regionJSON;
    }
    
    public static JSONObject getDeparturePortJSON(Element departurePortElem)
    {
    	JSONObject departurePortJSON = new JSONObject();
    	
		departurePortJSON.put("EmbarkationTime", XMLUtils.getValueAtXPath(departurePortElem, "./@EmbarkationTime"));
		departurePortJSON.put("LocationCode", XMLUtils.getValueAtXPath(departurePortElem, "./@LocationCode"));
		departurePortJSON.put("CodeContext", XMLUtils.getValueAtXPath(departurePortElem, "./@CodeContext"));
    	
    	return departurePortJSON;
    }
    
    
	private static Element getCruiseLinePref(Document ownerDoc, JSONObject cruiseLinePrefJson) {
		
		JSONArray portCodesJsonArr = cruiseLinePrefJson.getJSONObject("searchQualifier").getJSONArray("port");
		Element searchQualifiersElem = ownerDoc.createElementNS(Constants.NS_OTA,"SearchQualifiers");
		
		for(int i=0;i<portCodesJsonArr.length();i++)
		{
			JSONObject portCode =  (JSONObject) portCodesJsonArr.get(i);
			
			Element port = ownerDoc.createElementNS(Constants.NS_OTA,"Port");
			port.setAttribute("PortCode", portCode.getString("portCode"));
			
			searchQualifiersElem.appendChild(port);
			
		}
		
		Element cruiseLinePrefElem = ownerDoc.createElementNS(Constants.NS_OTA,"CruiseLinePref");
		cruiseLinePrefElem.appendChild(searchQualifiersElem);
		return cruiseLinePrefElem;
	}
	
	protected static void createHeader(Element reqElem, String sessionID, String transactionID, String userID) {
		Element sessionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru1:RequestHeader/com:SessionID");
		sessionElem.setTextContent(sessionID);

		Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru1:RequestHeader/com:TransactionID");
		transactionElem.setTextContent(transactionID);

		Element userElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru1:RequestHeader/com:UserID");
		userElem.setTextContent(userID);
	}
	
}
