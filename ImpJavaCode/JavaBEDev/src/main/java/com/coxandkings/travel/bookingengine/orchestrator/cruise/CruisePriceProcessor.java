package com.coxandkings.travel.bookingengine.orchestrator.cruise;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.cruise.CruiseConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class CruisePriceProcessor implements CruiseConstants{

	private static final Logger logger = LogManager.getLogger(CruisePriceProcessor.class);
	
	public static String process(JSONObject reqJson) throws Exception {
		
		OperationConfig opConfig = CruiseConfig.getOperationConfig("price");
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./cru1:OTA_CruiseCategoryAvailRQWrapper");
		reqBodyElem.removeChild(wrapperElem);

		TrackingContext.setTrackingContext(reqJson);
		JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);

		UserContext userctx = UserContext.getUserContextForSession(reqHdrJson);
		
		CruiseSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
		
		Element suppWrapperElem = null;
		int seqItr =0;
		
		JSONArray cruiseOptionsReqnArr = reqBodyJson.getJSONArray("cruiseOptions");
		
		for(int i=0;i<cruiseOptionsReqnArr.length();i++)
		{
			JSONObject supplierBody = cruiseOptionsReqnArr.getJSONObject(i);
			
			String suppID =	supplierBody.getString("SupplierID");
			
			suppWrapperElem = (Element) wrapperElem.cloneNode(true);
			reqBodyElem.appendChild(suppWrapperElem);
			
			//Request Header starts
			ProductSupplier prodSupplier = userctx.getSupplierForProduct(PROD_CATEG_TRANSPORT, "Cruise", suppID);
			if (prodSupplier == null) {
				throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
			}
			seqItr++;
			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru1:RequestHeader/com:SupplierCredentialsList");
//------------------------------------------------------------------------------------------------------------------------------------------------
			Element supplierCredentials = prodSupplier.toElement(ownerDoc);
			
//			suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
			//Request Body starts
				
			Element sequence = ownerDoc.createElementNS(Constants.NS_COM, "Sequence");
			sequence.setTextContent(String.valueOf(seqItr));
			
			Element credentialsElem = XMLUtils.getFirstElementAtXPath(supplierCredentials, "./com:Credentials");
			supplierCredentials.insertBefore(sequence, credentialsElem);
			
			suppCredsListElem.appendChild(supplierCredentials);
//------------------------------------------------------------------------------------------------------------------------------------------------
			Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./cru1:SupplierID");
			suppIDElem.setTextContent(suppID);
			
			Element sequenceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./cru1:Sequence");
			sequenceElem.setTextContent(String.valueOf(seqItr));
			
			Element otaCategoryAvail = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_CruiseCategoryAvailRQ");
			
			createPOS(ownerDoc, otaCategoryAvail);
			
			JSONArray guestsReqJsonArr = supplierBody.getJSONArray("Guests");
			createGuestsAndGuestCounts(ownerDoc,guestsReqJsonArr,otaCategoryAvail);
			
			Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:SailingInfo/ota:SelectedSailing");
			selectedSailingElem.setAttribute("VoyageID",supplierBody.getString("VoyageID"));
			
			Element cruiseLine = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:CruiseLine");
			cruiseLine.setAttribute("ShipCode", supplierBody.getString("ShipCode"));
			
			Element regionElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:Region");
			regionElem.setAttribute("RegionCode", supplierBody.getJSONObject("regionPref").getString("regionCode"));
			
			Element selectedFareElem =	XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ns:SelectedFare");
			selectedFareElem.setAttribute("FareCode", supplierBody.getJSONArray("selectedFare").getJSONObject(0).getString("fareCode"));
			
			Element tpaExtensionsElem =	XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:TPA_Extensions");
			
			Element itineraryIDElem = XMLUtils.getFirstElementAtXPath(tpaExtensionsElem, "./cru1:ItineraryID");
			itineraryIDElem.setTextContent(supplierBody.getString("ItineraryID"));
			
			Element sailingElem = XMLUtils.getFirstElementAtXPath(tpaExtensionsElem, "./cru1:Cruise/cru1:SailingDates/cru1:Sailing");
			sailingElem.setAttribute("SailingID", supplierBody.getString("SailingID"));
		}
		
		System.out.println("SIREQUEST");
		System.out.println(XMLTransformer.toString(reqElem));
		
		Element resElem = null;
        resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), CruiseConfig.getHttpHeaders(), reqElem);
        if (resElem == null) {
        	throw new Exception("Null response received from SI");
        }
        
        System.out.println("SIRESPONSE");
		System.out.println(XMLTransformer.toString(resElem));
		
		JSONObject resBodyJson = new JSONObject();
        JSONArray sailingOptionJsonArr = new JSONArray();
        Element[] otaCategoryAvailWrapperElems = XMLUtils.getElementsAtXPath(resElem, "./cru:ResponseBody/cru1:OTA_CruiseCategoryAvailRSWrapper");
        for(Element otaCategoryAvailWrapperElem : otaCategoryAvailWrapperElems)
        {
//        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_CruiseSailAvailRS");
        	getSupplierResponseJSON(otaCategoryAvailWrapperElem,sailingOptionJsonArr);
        }
        resBodyJson.put("cruiseOptions", sailingOptionJsonArr);
        
        System.out.println(resBodyJson.toString());
        
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, resBodyJson);
        
        System.out.println(resJson.toString());
        
        Map<String,String> SI2BRMSSailingOptionMap = new HashMap<String,String>();
        JSONObject parentSupplTransJson = CruiseSupplierCommercials.getSupplierCommercialsV1(reqJson,resJson,SI2BRMSSailingOptionMap);
        JSONObject breResClientJson = CruiseClientCommercials.getClientCommercialsV1(parentSupplTransJson);
        
        /*Scanner in = null;
        
        in = new Scanner(new FileReader("D:\\BookingEngine\\ClientTransactionRs.json"));
 
	    StringBuilder sb = new StringBuilder();
	    while(in.hasNext()) {
	        sb.append(in.next());
	    }
	    in.close();
	    String jsonStr = sb.toString();
    	JSONObject json = new JSONObject(jsonStr);*/
        
//        CruiseSearchProcessor.calculatePricesV1(reqJson,resJson,json,false);
        CruiseSearchProcessor.calculatePricesV2(reqJson, resJson, parentSupplTransJson, breResClientJson, SI2BRMSSailingOptionMap, false);
        System.out.println(sailingOptionJsonArr);
        
		return resJson.toString();
	}

	 public static void calculatePricesV1(JSONObject reqJson, JSONObject resJson, JSONObject clientCommResJson, boolean retainSuppFares) {
		 
		 	JSONArray sailingOptionsArr = resJson.getJSONObject("responseBody").getJSONArray("cruiseOptions");
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
				
				String suppID = sailingOptionsJson.getString(JSON_PROP_SUPPREF);
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
	    		
	    		JSONArray entityCommercialsArr = ccommCruDtlsJson.getJSONArray("passengerDetails").getJSONObject(0).getJSONArray("entityCommercials");
	    		JSONObject markUpCommercialJson = entityCommercialsArr.getJSONObject(entityCommercialsArr.length()-1).getJSONObject("markUpCommercialDetails");
	    		
	    		sailingOptionsJson.getJSONObject("Prices").put("TotalPrice", markUpCommercialJson.getInt("totalFare"));
	    		
			}
	 }
	
	public static void getSupplierResponseJSON(Element otaCategoryAvailWrapperElem,JSONArray sailingOptionJsonArr) {
		
		Random random = new Random();
		Element otaCruiseCategoryAvailRs = XMLUtils.getFirstElementAtXPath(otaCategoryAvailWrapperElem, "./ota:OTA_CruiseCategoryAvailRS");
		String suppID = XMLUtils.getValueAtXPath(otaCategoryAvailWrapperElem, "./cru1:SupplierID");
		Element[] categoryOptions = XMLUtils.getElementsAtXPath(otaCruiseCategoryAvailRs, "./ota:FareOption/ota:CategoryOptions/ota:CategoryOption");
		Element[] taxesElems = XMLUtils.getElementsAtXPath(otaCruiseCategoryAvailRs, "./ota:Taxes/ota:Tax");
		Map<String,String> pcCodeTaxMap = new HashMap<String,String>();
		
		JSONObject priceJson = new JSONObject();
		
		JSONArray sailingOptionsArr = new JSONArray();
		
		JSONObject sailingOptionsJson = new JSONObject();
		
		if(taxesElems!=null)
		{
			JSONArray taxesJson = getTaxes(taxesElems,pcCodeTaxMap);
			sailingOptionsJson.put("Taxes", taxesJson);
		}
		priceJson.put("Start", "2018-03-16");
		priceJson.put("supplierRef", suppID);
		
		Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(otaCruiseCategoryAvailRs, "./ota:SailingInfo/ota:SelectedSailing");
		if(selectedSailingElem!=null)
		{
			sailingOptionsJson.put("SelectedSailing", CruiseSearchProcessor.getSelectedSailingJSON(selectedSailingElem));
		}
		
		JSONArray categoryJson = getCategoryOptions(categoryOptions,pcCodeTaxMap);
		sailingOptionsJson.put("supplierRef", suppID);
		sailingOptionsJson.put("startDate", "2018-03-16");
		sailingOptionsJson.put("Category", categoryJson);
		
		sailingOptionsArr.put(sailingOptionsJson);
		
		priceJson.put("SailingOption", sailingOptionsArr);
		
		sailingOptionJsonArr.put(priceJson);
	}
	
	public static JSONObject getSailingOptions(Element sailingInfoElem) {
		
		Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:SelectedSailing");
		JSONObject selectedSailingJson = new JSONObject();
		
		if(selectedSailingElem!=null)
		{
			selectedSailingJson.put("VoyageID", XMLUtils.getValueAtXPath(selectedSailingElem, "./@VoyageID"));
			selectedSailingJson.put("Start", XMLUtils.getValueAtXPath(selectedSailingElem, "./@Start"));
			selectedSailingJson.put("Duration", XMLUtils.getValueAtXPath(selectedSailingElem, "./@Duration"));
			selectedSailingJson.put("End", XMLUtils.getValueAtXPath(selectedSailingElem, "./@End"));
			
			
			
			
		}
		return null;
	}
	
	public static JSONArray getTaxes(Element[] taxesElems,Map<String,String> pcCodeTaxMap) {
		JSONArray taxesJsonArr = new JSONArray();
		for(Element taxesElem : taxesElems)
		{
			JSONObject taxesJson = new JSONObject();
			String code = XMLUtils.getValueAtXPath(taxesElem, "./@Code");
			String amount = XMLUtils.getValueAtXPath(taxesElem, "./@Amount");
			
			pcCodeTaxMap.put(code, amount);
			
			taxesJson.put("Code",code);
			taxesJson.put("Amount",amount);
			taxesJson.put("CurrencyCode", XMLUtils.getValueAtXPath(taxesElem, "./@CurrencyCode"));
			
			taxesJsonArr.put(taxesJson);
		}
		
		return taxesJsonArr;
		
	}
	
	public static JSONArray getCategoryOptions(Element[] categoryOptions,Map<String,String> pcCodeTaxMap) {
		
		JSONArray categoryOptionsJsonArr = new JSONArray();
		
		for(Element categoryOption : categoryOptions)
		{
			JSONObject categoryOptionsJson = new JSONObject();
			
			categoryOptionsJson.put("CategoryLocation", XMLUtils.getValueAtXPath(categoryOption, "./@CategoryLocation"));
			String pcCode= XMLUtils.getValueAtXPath(categoryOption, "./@PricedCategoryCode");
			categoryOptionsJson.put("PricedCategoryCode",pcCode);
			categoryOptionsJson.put("Status", XMLUtils.getValueAtXPath(categoryOption, "./@Status"));
			categoryOptionsJson.put("MaxOccupancy", XMLUtils.getValueAtXPath(categoryOption, "./@MaxOccupancy"));
			categoryOptionsJson.put("ListOfCategoryQualifierCodes", XMLUtils.getValueAtXPath(categoryOption, "./@ListOfCategoryQualifierCodes"));
			categoryOptionsJson.put("Availability", XMLUtils.getValueAtXPath(categoryOption, "./@Availability"));
			categoryOptionsJson.put("BerthedCategoryCode", XMLUtils.getValueAtXPath(categoryOption, "./@BerthedCategoryCode"));
			categoryOptionsJson.put("GroupCode", XMLUtils.getValueAtXPath(categoryOption, "./@GroupCode"));
			categoryOptionsJson.put("AvailableGroupAllocationQty", XMLUtils.getValueAtXPath(categoryOption, "./@AvailableGroupAllocationQty"));
			
			double tax = Double.valueOf(pcCodeTaxMap.getOrDefault(pcCode, "0.00"));
			boolean taxIncluded = false;
			String check =	XMLUtils.getValueAtXPath(categoryOption, "./ota:TPA_Extensions/cru1:Cruise/@TaxesIncluded");//because MSC and Star dont give tax
			if(!check.isEmpty() && check!=null)
			{
				taxIncluded = Boolean.valueOf(check);
			}
			
			Element[] priceInfosElems =	XMLUtils.getElementsAtXPath(categoryOption, "./ota:PriceInfos/ota:PriceInfo");
			if(priceInfosElems!=null)
			{
				JSONArray passengerPricesArr = CruiseSearchProcessor.getPassengerPrices(priceInfosElems,taxIncluded,tax);
				categoryOptionsJson.put("PassengerPrices", passengerPricesArr);
				
				JSONArray priceInfosJsonArr = getPriceInfos(priceInfosElems);
				categoryOptionsJson.put("PriceInfo", priceInfosJsonArr);
			}
			categoryOptionsJsonArr.put(categoryOptionsJson);
		}
		return categoryOptionsJsonArr;
	}
	
	/*private static JSONArray getPassengerPrices(Element[] priceInfosElems,double tax)
	 {
		JSONArray passengerJsonArr = new JSONArray();
		
		for(Element priceInfosElem : priceInfosElems)
		{
			String totalFareName = XMLUtils.getValueAtXPath(priceInfosElem, "./ota:PriceDescription");//X path will change whenever SI gives me a tag with the same value in every supplier! currently this x path is for tourico
			
			if(totalFareName.equalsIgnoreCase("Special Bonus Fare"))
			{
				JSONObject passengerJson = new JSONObject();
				
				passengerJson.put("PassengerType", "ADT");
				Double amount = Double.valueOf(XMLUtils.getValueAtXPath(priceInfosElem, "./@Amount"));
			}
		}
		
		return passengerJsonArr;
	 }*/
	
	private static JSONArray getPriceInfos(Element[] priceInfosElems)
	 {
		JSONArray priceInfosJsonArr = new JSONArray();
		
		for(Element priceInfosElem : priceInfosElems)
		{
			JSONObject priceInfosJson = new JSONObject();
			
			priceInfosJson.put("BreakdownType", XMLUtils.getValueAtXPath(priceInfosElem, "./@BreakdownType"));
			priceInfosJson.put("ChargeTypeCode", XMLUtils.getValueAtXPath(priceInfosElem, "./@ChargeTypeCode"));
			priceInfosJson.put("CurrencyCode", XMLUtils.getValueAtXPath(priceInfosElem, "./@CurrencyCode"));
			priceInfosJson.put("Amount", XMLUtils.getValueAtXPath(priceInfosElem, "./@Amount"));
			priceInfosJson.put("FareCode", XMLUtils.getValueAtXPath(priceInfosElem, "./@FareCode"));
			priceInfosJson.put("GroupCode", XMLUtils.getValueAtXPath(priceInfosElem, "./@GroupCode"));
			priceInfosJson.put("AgeQualifyingCode", XMLUtils.getValueAtXPath(priceInfosElem, "./@AgeQualifyingCode"));
			priceInfosJson.put("PriceDescription", XMLUtils.getValueAtXPath(priceInfosElem, "./ota:PriceDescription"));
			
			priceInfosJsonArr.put(priceInfosJson);
		}
		
		return priceInfosJsonArr;
	 }
	
	private static JSONObject getPrices(Element sailingElem,Element cruiseElem ,Element otaCruiseCategoryAvailRs)
	 {
		 JSONObject pricesJson = new JSONObject();
		 
		 String taxCode = XMLUtils.getValueAtXPath(sailingElem, "./@PricedCategoryCode");
		 
		 String totalFare = XMLUtils.getValueAtXPath(sailingElem, "./ota:PriceInfos/ota:PriceInfo/@Amount");
		 String tax = XMLUtils.getValueAtXPath(otaCruiseCategoryAvailRs, String.format("./ota:Taxes/ota:Tax[@Code='%s']/@Amount",taxCode));
		 
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
	
	public static void createGuestsAndGuestCounts(Document ownerDoc,JSONArray guestsReqJsonArr,Element otaCategoryAvail) {
		
		int adt=0,inf=0;
		
		Element guestCountsElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:GuestCounts");
		for(int i=0;i<guestsReqJsonArr.length();i++)
		{
			JSONObject guestJson = guestsReqJsonArr.getJSONObject(i);
			String age = guestJson.getString("age");
			
			Element guestElem = ownerDoc.createElementNS(Constants.NS_OTA,"Guest");
			guestElem.setAttribute("PersonBirthDate", guestJson.optString("personBirthDate"));
			guestElem.setAttribute("Gender", guestJson.optString("gender"));
			guestElem.setAttribute("GuestRefNumber", guestJson.optString("guestRefNumber"));
//			guestElem.setAttribute("LoyaltyMembershipId", guestJson.optString("loyaltyMembershipId"));
			guestElem.setAttribute("Age", guestJson.optString("age"));
			
			JSONObject guestNameJson = guestJson.getJSONObject("guestName");
			
			Element	guestNameElem = ownerDoc.createElementNS(Constants.NS_OTA,"GuestName");
			guestNameElem.setAttribute("NameType", guestNameJson.optString("nameType"));
			
			Element givenNameElem =	ownerDoc.createElementNS(Constants.NS_OTA,"GivenName");
			givenNameElem.setTextContent(guestNameJson.optString("givenName"));
			
			Element middleNameElem = ownerDoc.createElementNS(Constants.NS_OTA,"MiddleName");
			middleNameElem.setTextContent(guestNameJson.optString("middleName"));
			
			Element surNameElem = ownerDoc.createElementNS(Constants.NS_OTA,"Surname");
			surNameElem.setTextContent(guestNameJson.optString("surName"));
			
			guestNameElem.appendChild(surNameElem);
			guestNameElem.insertBefore(middleNameElem, surNameElem);
			guestNameElem.insertBefore(givenNameElem, middleNameElem);
			
			guestElem.appendChild(guestNameElem);
			
//			otaCategoryAvail.appendChild(guestElem);
			otaCategoryAvail.insertBefore(guestElem, guestCountsElem);
			
			if(Integer.parseInt(age)>=18)
			{
				adt++;
			}else {
				inf++;
			}
		}
//		Element guestCountsElem = ownerDoc.createElementNS(Constants.NS_OTA, "GuestCounts");
		{
			Element guestCountElem = ownerDoc.createElementNS(Constants.NS_OTA, "GuestCount");
			guestCountElem.setAttribute("Code", "10");
			guestCountElem.setAttribute("Quantity", String.valueOf(adt));
			guestCountsElem.appendChild(guestCountElem);
		}
		
		{
			Element guestCountElem = ownerDoc.createElementNS(Constants.NS_OTA, "GuestCount");
			guestCountElem.setAttribute("Code", "08");
			guestCountElem.setAttribute("Quantity", String.valueOf(inf));
			guestCountsElem.appendChild(guestCountElem);
		}
	}

	public static void createPOS(Document ownerDoc, Element otaCategoryAvail) {
		Element Source = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:POS/ota:Source");
		Source.setAttribute("ISOCurrency", "AUD");
		
		Element RequesterID = ownerDoc.createElementNS(Constants.NS_OTA,"RequestorID");
//		RequesterID.setAttribute("ID", "US");
		RequesterID.setAttribute("Type", "A");
		
		Source.appendChild(RequesterID);
	}
	
}
