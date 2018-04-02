package com.coxandkings.travel.bookingengine.orchestrator.cruise;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
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
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.cruise.CruiseConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class CruiseRePriceProcessor implements CruiseConstants {
	
	private static final Logger logger = LogManager.getLogger(CruiseSearchProcessor.class);
	
	public static String process(JSONObject reqJson) throws Exception
	{
		OperationConfig opConfig = CruiseConfig.getOperationConfig("reprice");
		//Element reqElem = (Element) mXMLPriceShellElem.cloneNode(true);
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		//logger.info(String.format("Read Reprice Verify XML request template: %s\n", XMLTransformer.toEscapedString(reqElem)));
		Document ownerDoc = reqElem.getOwnerDocument();
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./cru1:OTA_CruisePriceBookingRQWrapper");
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
		String prevSuppID = "";
		JSONArray cruisePriceDetailsArr = reqBodyJson.getJSONArray("cruiseOptions");
		
		for(int i=0;i<cruisePriceDetailsArr.length();i++)
		{
			JSONObject supplierBody = cruisePriceDetailsArr.getJSONObject(i);
			
			String suppID =	supplierBody.getString("supplierRef");
			
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
			
			Element otaCategoryAvail = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_CruisePriceBookingRQ");
			CruisePriceProcessor.createPOS(ownerDoc, otaCategoryAvail);
			
			Element guestCountsElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:GuestCounts");
			
			JSONArray guestJsonArr = supplierBody.getJSONArray("Guests");
			
			int adt=0,inf=0;
			for(int k=0;k<guestJsonArr.length();k++)
			{
				JSONObject guestJson = guestJsonArr.getJSONObject(k);
				int age = guestJson.getInt("age");
				if(age>=18)
				{
					adt++;
				}else {
					inf++;
				}
			}
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
			
//=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=SailingInfo creation Starts-=-=-=-==-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
			
			Element sailingInfoElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:SailingInfo");
			JSONObject sailingInfoJson = supplierBody.getJSONObject("sailingInfo");
			
			Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:SelectedSailing");
			selectedSailingElem.setAttribute("VoyageID", sailingInfoJson.getJSONObject("selectedSailing").getString("voyageId"));
			
			Element cruiseLineElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:CruiseLine");
			cruiseLineElem.setAttribute("VendorCodeContext", sailingInfoJson.getJSONObject("selectedSailing").getJSONObject("cruiseLine").getString("vendorCodeCotext"));
			
			Element currencyCodeElem = XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:Currency");
			currencyCodeElem.setAttribute("CurrencyCode", sailingInfoJson.getJSONObject("selectedSailing").getJSONObject("cruiseLine").getString("currencyCode"));
			
			Element selectedCategoryElem = XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:SelectedCategory");
			selectedCategoryElem.setAttribute("PricedCategoryCode", sailingInfoJson.getJSONObject("selectedCategory").getString("pricedCategoryCode"));
			selectedCategoryElem.setAttribute("FareCode", sailingInfoJson.getJSONObject("selectedCategory").getString("fareCode"));
			
			Element selectedCabin = XMLUtils.getFirstElementAtXPath(selectedCategoryElem, "./ota:SelectedCabin");
			selectedCabin.setAttribute("CabinNumber", sailingInfoJson.getJSONObject("selectedCategory").getJSONObject("selectedCabin").getString("CabinNumber"));
			selectedCabin.setAttribute("Status", sailingInfoJson.getJSONObject("selectedCategory").getJSONObject("selectedCabin").getString("Status"));
			selectedCabin.setAttribute("CabinCategoryStatusCode", sailingInfoJson.getJSONObject("selectedCategory").getJSONObject("selectedCabin").getString("cabinCategoryStatusCode"));
			selectedCabin.setAttribute("MaxOccupancy", sailingInfoJson.getJSONObject("selectedCategory").getJSONObject("selectedCabin").getString("maxOccupancy"));
			
			Element cruiseElem = XMLUtils.getFirstElementAtXPath(selectedCategoryElem, "./ota:TPA_Extensions/cru1:Cruise");
			cruiseElem.setAttribute("Tax", sailingInfoJson.getJSONObject("selectedCategory").getString("tax"));
			
			Element sailingElem = XMLUtils.getFirstElementAtXPath(cruiseElem, "./cru1:SailingDates/cru1:Sailing");
			sailingElem.setAttribute("SailingID", sailingInfoJson.getJSONObject("selectedCategory").getString("sailingID"));
			
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-Guest Details Creation=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-==-=-=-==-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
			
			Element guestDetailsElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:ReservationInfo/ota:GuestDetails");
			
			for(int j=0;j<guestJsonArr.length();j++)
			{
				JSONObject guestJson = guestJsonArr.getJSONObject(j);
				
				Element guestDetailElem = ownerDoc.createElementNS(NS_OTA, "GuestDetail");
				
				Element contactInfo = ownerDoc.createElementNS(NS_OTA, "ContactInfo");
				contactInfo.setAttribute("GuestRefNumber",guestJson.getString("guestRefNumber"));
				contactInfo.setAttribute("LoyaltyMembershipID",guestJson.getString("loyaltyMembershipId"));
				contactInfo.setAttribute("PersonBirthDate",guestJson.getString("personBirthDate"));
				contactInfo.setAttribute("Gender", guestJson.getString("gender"));
				contactInfo.setAttribute("Age", guestJson.getString("age"));
				
				Element personName = ownerDoc.createElementNS(NS_OTA, "PersonName");
				
				Element surName = ownerDoc.createElementNS(NS_OTA, "Surname");
				surName.setTextContent(guestJson.getJSONObject("guestName").getString("surName"));
				
				Element middleName = ownerDoc.createElementNS(NS_OTA, "MiddleName");
				middleName.setTextContent(guestJson.getJSONObject("guestName").getString("middleName"));
				
				Element givenName = ownerDoc.createElementNS(NS_OTA, "GivenName");
				givenName.setTextContent(guestJson.getJSONObject("guestName").getString("givenName"));
				
				Element namePrefix = ownerDoc.createElementNS(NS_OTA, "NamePrefix");
				namePrefix.setTextContent(guestJson.getJSONObject("guestName").getString("namePrefix"));
				
				personName.appendChild(surName);
				personName.insertBefore(middleName, surName);
				personName.insertBefore(givenName, middleName);
				personName.insertBefore(namePrefix, givenName);
				
				Element telephoneElem =	ownerDoc.createElementNS(NS_OTA, "Telephone");
				telephoneElem.setAttribute("CountryAccessCode", guestJson.getJSONObject("guestName").getJSONObject("Telephone").getString("CountryAccessCode"));
				telephoneElem.setAttribute("PhoneNumber", guestJson.getJSONObject("guestName").getJSONObject("Telephone").getString("PhoneNumber"));
				
				contactInfo.appendChild(telephoneElem);
				contactInfo.insertBefore(personName, telephoneElem);
				
				Element travelDocument = ownerDoc.createElementNS(NS_OTA, "TravelDocument");
				travelDocument.setAttribute("DocIssueCountry",guestJson.getJSONObject("guestName").getJSONObject("TravelDocument").getString("DocIssueCountry"));
				travelDocument.setAttribute("DocIssueStateProv",guestJson.getJSONObject("guestName").getJSONObject("TravelDocument").getString("DocIssueStateProv"));
				
				Element selectedDining = ownerDoc.createElementNS(NS_OTA, "SelectedDining");
				selectedDining.setAttribute("Sitting", guestJson.getJSONObject("guestName").getJSONObject("SelectedDining").getString("Sitting"));
				selectedDining.setAttribute("Status", guestJson.getJSONObject("guestName").getJSONObject("SelectedDining").getString("Status"));
				
				guestDetailElem.appendChild(selectedDining);
				guestDetailElem.insertBefore(travelDocument, selectedDining);
				guestDetailElem.insertBefore(contactInfo, travelDocument);
				
				guestDetailsElem.appendChild(guestDetailElem);
			}
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=Payment Options Creations-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=			
			Element paymentOptionsElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:ReservationInfo/ota:PaymentOptions");
			
			Element paymentOption =	ownerDoc.createElementNS(NS_OTA,"PaymentOption");
			paymentOption.setAttribute("SplitPaymentInd", supplierBody.getJSONObject("PaymentOptions").getString("SplitPaymentInd"));
			
			Element paymentAmount = ownerDoc.createElementNS(NS_OTA, "PaymentAmount");
			paymentAmount.setAttribute("Amount", supplierBody.getJSONObject("PaymentOptions").getString("PaymentAmount"));
			
			paymentOption.appendChild(paymentAmount);
			paymentOptionsElem.appendChild(paymentOption);
		}
		System.out.println(XMLTransformer.toString(reqElem));
		
		Element resElem = null;
        resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), CruiseConfig.getHttpHeaders(), reqElem);
        if (resElem == null) {
        	throw new Exception("Null response received from SI");
        }
		
        System.out.println(XMLTransformer.toString(resElem));
        
       /* DocumentBuilderFactory docBldrFactory = DocumentBuilderFactory.newInstance();
        docBldrFactory.setNamespaceAware(true);
		DocumentBuilder docBldr = docBldrFactory.newDocumentBuilder();
        
		Document doc = docBldr.parse(new File("D:\\Cruise\\StandardizedRepriceXmlRs.xml"));
		Element resElemTest = doc.getDocumentElement();*/
        
        JSONObject resBodyJson = new JSONObject();
        JSONArray cruiseRepriceDetailsJsonArr = new JSONArray();
        Element[] otaCategoryAvailWrapperElems = XMLUtils.getElementsAtXPath(resElem, "./cru:ResponseBody/cru1:OTA_CruisePriceBookingRSWrapper");
        for(Element otaCategoryAvailWrapperElem : otaCategoryAvailWrapperElems)
        {
//        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_CruiseSailAvailRS");
        	getSupplierResponseJSON(otaCategoryAvailWrapperElem,cruiseRepriceDetailsJsonArr,"./ota:OTA_CruisePriceBookingRS");
        }
        
        resBodyJson.put("cruiseOptions", cruiseRepriceDetailsJsonArr);
        
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, resBodyJson);
        
        System.out.println(resJson.toString());
        
        /*Scanner in = null;
        
        in = new Scanner(new FileReader("D:\\AVAMAR BACKUP\\ASHLEY MUNDADEN\\Desktop\\JSON FOR SWAGGER CRUISE\\RePriceRSJson.json"));
 
	    StringBuilder sb = new StringBuilder();
	    while(in.hasNext()) {
	        sb.append(in.next());
	    }
	    in.close();
	    String jsonStr = sb.toString();
   
    	JSONObject json = new JSONObject(jsonStr);
    	
    	Scanner in1 = null;
        
    	in1 = new Scanner(new FileReader("D:\\BookingEngine\\ClientTransactionRs.json"));
 
	    StringBuilder sb1 = new StringBuilder();
	    while(in1.hasNext()) {
	    	sb1.append(in1.next());
	    }
	    in1.close();
	    String jsonStr1 = sb1.toString();
    	JSONObject json1 = new JSONObject(jsonStr1);*/
        
    	Map<String,String> SI2BRMSSailingOptionMap = new HashMap<String,String>();
    	JSONObject parentSupplTransJson = CruiseSupplierCommercials.getSupplierCommercialsV1(reqJson,resJson,SI2BRMSSailingOptionMap);
        JSONObject breResClientJson = CruiseClientCommercials.getClientCommercialsV1(parentSupplTransJson);
//    	CruiseSearchProcessor.calculatePricesV2(reqJson, resJson, parentSupplTransJson, breResClientJson, SI2BRMSSailingOptionMap, true);
        CruiseSearchProcessor.calculatePricesV3(reqJson, resJson, parentSupplTransJson, breResClientJson, SI2BRMSSailingOptionMap, true,userctx);
        pushSuppFaresToRedisAndRemove(resJson);
        
		return resJson.toString();
		
	}
	
	private static void pushSuppFaresToRedisAndRemove(JSONObject resJson) {
		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
		Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();
		
		JSONArray cruiseOptionsJsonArr = resBodyJson.optJSONArray("cruiseOptions");
		
		if (cruiseOptionsJsonArr == null) {
			// TODO: This should never happen. Log a warning message here.
			return;
		}
		for(int j=0;j<cruiseOptionsJsonArr.length();j++)
		{
			JSONObject redisJson = new JSONObject();
			
			JSONObject cruiseOptionsJson = cruiseOptionsJsonArr.getJSONObject(j);
			JSONArray sailingOptionsJsonArr = cruiseOptionsJson.optJSONArray("SailingOption");//will always be Object in this array
	//		JSONArray bookRefsJsonArr = resBodyJson.optJSONArray(JSON_PROP_BOOKREFS);
	//		resBodyJson.remove(JSON_PROP_BOOKREFS);
			
			/*JSONObject suppPriceInfoJson = cruiseOptionsJson.optJSONObject("BookingPayment").getJSONObject("suppPaymentSchedule");
			redisJson.put("paymentSchedule", suppPriceInfoJson);
			
			if (sailingOptionsJsonArr == null) {
				// TODO: This should never happen. Log a warning message here.
				return;
			}
			
			for (int i=0; i < sailingOptionsJsonArr.length(); i++) {//will iterate once
				JSONObject sailingOptionJson = sailingOptionsJsonArr.getJSONObject(i);
//				sailingOptionJson.getJSONObject("BookingPayment").remove("PaymentSchedule");
				
				JSONArray categoryJsonArr =	sailingOptionJson.getJSONArray("Category");
				
				for(int k=0;k<categoryJsonArr.length();k++) //will iterate once
				{
					JSONObject categoryJson = categoryJsonArr.getJSONObject(k);
					
					redisJson.put("SuppPassengerPrices", categoryJson.getJSONArray("SuppPassengerPrices"));
					redisJson.put("PassengerPrices", categoryJson.getJSONArray("PassengerPrices"));
				}
				
				if (suppPriceInfoJson == null) {
					// TODO: This should never happen. Log a warning message here.
					continue;
				}
				
			}*/
			
			redisJson.put("PricingInfo",cruiseOptionsJson.get("PricingInfo"));
			redisJson.put("suppPricingInfo", cruiseOptionsJson.get("suppPricingInfo"));
			redisJson.put("sailingInfo", sailingOptionsJsonArr.getJSONObject(0).getJSONObject("SelectedSailing"));
			
			reprcSuppFaresMap.put(getRedisKeyForSailingOption(cruiseOptionsJson), redisJson.toString());
		}
		String redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_CRUISE);
		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		redisConn.hmset(redisKey, reprcSuppFaresMap);
		redisConn.pexpire(redisKey, (long) (CruiseConfig.getRedisTTLMinutes() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);
	}
	
	static String getRedisKeyForSailingOption(JSONObject sailingOptionJson) {
		
		String suppID;String voyageID;String itineraryID;String pricedCategoryCode=new String();String categoryName=new String();String cabinNo=new String();
		
		suppID = sailingOptionJson.optString("supplierRef");
		
		voyageID = sailingOptionJson.optJSONArray("SailingOption").optJSONObject(0).optJSONObject("SelectedSailing").optString("VoyageID");
		itineraryID = sailingOptionJson.optString("ItineraryID");
		
		JSONArray categoryJsonArr = sailingOptionJson.optJSONArray("SailingOption").optJSONObject(0).optJSONArray("Category");
		
		for(int i=0;i<categoryJsonArr.length();i++)//will always iterate once
		{
			JSONObject categoryJson =	categoryJsonArr.getJSONObject(i);
			JSONArray cabinOptionsJsonArr =	categoryJson.getJSONArray("CabinOptions");
			
			pricedCategoryCode = categoryJson.optString("PricedCategoryCode");
			
			for(int j=0;j<cabinOptionsJsonArr.length();j++)//will iterate once
			{
				JSONObject cabinOptionsJson = cabinOptionsJsonArr.getJSONObject(j);
				
				cabinNo = cabinOptionsJson.getString("CabinNumber");
			}
			
//			categoryName = categoryJson.optString("CategoryName");
		}
		String abc = String.format("%s%c%s%c%s%c%s%c%s", suppID,KEYSEPARATOR,voyageID,KEYSEPARATOR,itineraryID,KEYSEPARATOR,pricedCategoryCode,KEYSEPARATOR,cabinNo);
		return String.format("%s%c%s%c%s%c%s%c%s", suppID,KEYSEPARATOR,voyageID,KEYSEPARATOR,itineraryID,KEYSEPARATOR,pricedCategoryCode,KEYSEPARATOR,cabinNo);
	}
	
	public static void getSupplierResponseJSON(Element otaCategoryAvailWrapperElem,JSONArray cruiseRepriceDetailsJsonArr,String otaName) {
		
		JSONObject repriceJson = new JSONObject();
		
		Element otaCruiseCategoryAvailRs = XMLUtils.getFirstElementAtXPath(otaCategoryAvailWrapperElem, otaName);
		String suppID = XMLUtils.getValueAtXPath(otaCategoryAvailWrapperElem, "./cru1:SupplierID");
		repriceJson.put("supplierRef", suppID);
		
		String errorMsg = XMLUtils.getValueAtXPath(otaCategoryAvailWrapperElem, "./cru1:ErrorList/com:Error/com:ErrorMsg");
		if(errorMsg!=null)
		repriceJson.put("ErrorMsg", errorMsg);
		
		Element[] reservationIDElems =	XMLUtils.getElementsAtXPath(otaCruiseCategoryAvailRs, "./ota:ReservationID");
		if(reservationIDElems!=null)
		{
			JSONArray reservationIDJsonArr = getReservationJson(reservationIDElems);
			repriceJson.put("ReservationID", reservationIDJsonArr);
		}
		
		Element sailingInfoElem = XMLUtils.getFirstElementAtXPath(otaCruiseCategoryAvailRs, "./ota:SailingInfo");
		if(sailingInfoElem!=null)
		{
			JSONObject sailingOptionJson = getsailingOptionJSON(sailingInfoElem,suppID);
			JSONArray sailingArr = new JSONArray();
			sailingArr.put(sailingOptionJson);
			repriceJson.put("SailingOption", sailingArr);
		}
		
		Element bookingPaymentElem = XMLUtils.getFirstElementAtXPath(otaCruiseCategoryAvailRs, "./ota:BookingPayment");
		System.out.println("OTA XML \n");
		System.out.println(XMLTransformer.toString(otaCruiseCategoryAvailRs));
		if(bookingPaymentElem!=null)
		{
			JSONObject bookingPaymentJson = getBookingPaymentJson(bookingPaymentElem);
			repriceJson.put("BookingPayment", bookingPaymentJson);
		}
		
		if(sailingInfoElem!=null && bookingPaymentElem!=null)
		addPriceInfos(repriceJson,bookingPaymentElem);
		
		cruiseRepriceDetailsJsonArr.put(repriceJson);
	}
	
	private static JSONArray getReservationJson(Element[] reservationIDElems)
 	{
		JSONArray reservationJsonArr = new JSONArray();
		for(Element reservationIDElem : reservationIDElems)
		{
			JSONObject reservationIDJson = new JSONObject();
			
			reservationIDJson.put("StatusCode", XMLUtils.getValueAtXPath(reservationIDElem, "./@StatusCode"));
			reservationIDJson.put("BookedDate", XMLUtils.getValueAtXPath(reservationIDElem, "./@BookedDate"));
			reservationIDJson.put("Type", XMLUtils.getValueAtXPath(reservationIDElem, "./@Type"));
			reservationIDJson.put("ID", XMLUtils.getValueAtXPath(reservationIDElem, "./@ID"));
			reservationIDJson.put("ID_Context", XMLUtils.getValueAtXPath(reservationIDElem, "./@ID_Context"));
			reservationIDJson.put("CompanyName", XMLUtils.getValueAtXPath(reservationIDElem, "./ota:CompanyName"));
			
			reservationJsonArr.put(reservationIDJson);
		}
		
		return reservationJsonArr;
 	}
	
	private static void addPriceInfos(JSONObject repriceJson, Element bookingPaymentElem)
	 {
		JSONObject categoryJson = repriceJson.getJSONArray("SailingOption").getJSONObject(0).getJSONArray("Category").getJSONObject(0);
		
		JSONArray priceInfosArr = new JSONArray();
		Element[] guestPriceElems = XMLUtils.getElementsAtXPath(bookingPaymentElem, "./ota:GuestPrices/ota:GuestPrice");
		
		for(Element guestPriceElem : guestPriceElems)
		{
			JSONObject priceInfoJson = new JSONObject();
			Element[] priceInfosElems =	XMLUtils.getElementsAtXPath(guestPriceElem, "./ota:PriceInfos/ota:PriceInfo");
			
			priceInfosArr.put(CruiseSearchProcessor.getPassengerPrices(priceInfosElems,false,0.0));
			
			JSONArray passengerPriceJsonArr = categoryJson.optJSONArray("PassengerPrices");
			if(passengerPriceJsonArr==null)
			{
				categoryJson.put("PassengerPrices", CruiseSearchProcessor.getPassengerPrices(priceInfosElems,false,0.0));
			}
			else
			{
				
				JSONArray passPriceJsonArr = CruiseSearchProcessor.getPassengerPrices(priceInfosElems,false,0.0);
				for(int i=0;i<passPriceJsonArr.length();i++)
				{
					passengerPriceJsonArr.put(passPriceJsonArr.getJSONObject(i));
				}
//				passengerPriceJsonArr.put(CruiseSearchProcessor.getPassengerPrices(priceInfosElems,false,0.0));
			}
			
		/*	priceInfoJson.put("Amount",XMLUtils.getValueAtXPath(guestPriceElem, "./ota:PriceInfo[PriceTypeCode='8']/@Amount"));
			priceInfoJson.put("PassengerType",XMLUtils.getValueAtXPath(guestPriceElem, "ADT"));*/
			
//			priceInfosArr.put(priceInfoJson);
		}
		
	 }
	
	private static JSONObject getBookingPaymentJson(Element bookingPaymentElem)
	 {
		JSONObject bookingPaymentJson = new JSONObject();
		
		Element bookingPricesElem =	XMLUtils.getFirstElementAtXPath(bookingPaymentElem, "./ota:BookingPrices");
		if(bookingPricesElem!=null)
		{
			JSONArray bookingPriceArr = getBookingPriceJsonArr(bookingPricesElem);
			bookingPaymentJson.put("BookingPrice", bookingPriceArr);
		}
		
		Element paymentScheduleElem = XMLUtils.getFirstElementAtXPath(bookingPaymentElem, "./ota:PaymentSchedule");
		if(paymentScheduleElem!=null)
		{
			JSONArray paymentJsonArr = getPaymentJsonArr(paymentScheduleElem);
			JSONObject paymentJson = new JSONObject();
			
			paymentJson.put("Payment", paymentJsonArr);
			bookingPaymentJson.put("PaymentSchedule", paymentJson);
		}
		
		Element guestPricesElems = XMLUtils.getFirstElementAtXPath(bookingPaymentElem, "./ota:GuestPrices");
		if(guestPricesElems!=null)
		{
			JSONArray guestPriceJsonArr = getGuestPriceJsonArr(guestPricesElems);
			bookingPaymentJson.put("GuestPrice", guestPriceJsonArr);
			
		}
		
		return bookingPaymentJson;
	 }
	
	private static JSONArray getGuestPriceJsonArr(Element paymentScheduleElem)
	{
		JSONArray guestPriceJsonArr = new JSONArray();
		Element[] guestPriceElems =	XMLUtils.getElementsAtXPath(paymentScheduleElem, "");
		
		for(Element guestPriceElem : guestPriceElems)
		{
			JSONObject guestPriceJson = new JSONObject();
			
			guestPriceJson.put("GuestRefNumber", XMLUtils.getValueAtXPath(guestPriceElem, "./@GuestRefNumber"));
			
			Element priceInfosElem = XMLUtils.getFirstElementAtXPath(guestPriceElem, "./ota:PriceInfos");
			if(priceInfosElem!=null)
			{
				JSONArray priceInfosJsonArr = getPriceInfosJsonArr(guestPriceElem);
				guestPriceJson.put("PriceInfo", priceInfosJsonArr);
			}
			
			guestPriceJsonArr.put(guestPriceJson);
		}
		return guestPriceJsonArr;
	}
	private static JSONArray getPriceInfosJsonArr(Element paymentScheduleElem)
	{
		JSONArray priceInfosJsonArr = new JSONArray();
		Element[] priceInfoElems =	XMLUtils.getElementsAtXPath(paymentScheduleElem, "./ota:PriceInfo");
		
		for(Element priceInfoElem : priceInfoElems)
		{
			JSONObject priceInfoJson = new JSONObject();
			
			priceInfoJson.put("PriceTypeCode",XMLUtils.getValueAtXPath(priceInfoElem, "./@PriceTypeCode"));
			priceInfoJson.put("Amount",XMLUtils.getValueAtXPath(priceInfoElem, "./@Amount"));
			priceInfoJson.put("CodeDetail",XMLUtils.getValueAtXPath(priceInfoElem, "./@CodeDetail"));
			priceInfoJson.put("RestrictedIndicator",XMLUtils.getValueAtXPath(priceInfoElem, "./@RestrictedIndicator"));
			
			priceInfosJsonArr.put(priceInfoJson);
		}
		
		return priceInfosJsonArr;
	}
	
	private static JSONArray getPaymentJsonArr(Element paymentScheduleElem)
	{
		JSONArray paymentJsonArr = new JSONArray();
		Element[] paymentElems = XMLUtils.getElementsAtXPath(paymentScheduleElem, "./ota:Payment");
		
		for(Element paymentElem : paymentElems)
		{
			JSONObject paymentJson = new JSONObject();
			
			paymentJson.put("PaymentNumber", XMLUtils.getValueAtXPath(paymentElem, "./@PaymentNumber"));
			paymentJson.put("DueDate", XMLUtils.getValueAtXPath(paymentElem, "./@DueDate"));
			paymentJson.put("CurrencyCode", XMLUtils.getValueAtXPath(paymentElem, "./@CurrencyCode"));
			paymentJson.put("Amount", XMLUtils.getValueAtXPath(paymentElem, "./@Amount"));
			
			paymentJsonArr.put(paymentJson);
		}
		
		return paymentJsonArr;
	}
	
	private static JSONArray getBookingPriceJsonArr(Element bookingPricesElem)
	{
		JSONArray bookingPriceArr = new JSONArray();
		
		Element[] bookingPriceElems = XMLUtils.getElementsAtXPath(bookingPricesElem, "./ota:BookingPrice");
		
		for(Element bookingPriceElem : bookingPriceElems)
		{
			JSONObject bookingPriceJson = new JSONObject();
			
			bookingPriceJson.put("PriceTypeCode", XMLUtils.getValueAtXPath(bookingPriceElem, "./@PriceTypeCode"));
			bookingPriceJson.put("Amount", XMLUtils.getValueAtXPath(bookingPriceElem, "./@Amount"));
			bookingPriceJson.put("CodeDetail", XMLUtils.getValueAtXPath(bookingPriceElem, "./@CodeDetail"));
			
			bookingPriceArr.put(bookingPriceJson);
		}
		
		return bookingPriceArr;
		
	}
	
	 private static JSONObject getsailingOptionJSON(Element sailingInfoElem,String suppID)
	 {
//		 XMLUtils.getValueAtXPath((Element)sailingInfoElem.getParentNode().getParentNode(), "");
		 
		 Random random = new Random();
		 JSONObject sailingOptionJSON = new JSONObject();
		 
		 sailingOptionJSON.put("supplierRef", suppID);
		 
		 Element selectedSailingElem = XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:SelectedSailing");
		 if(selectedSailingElem!=null)
		 sailingOptionJSON.put("SelectedSailing", getSelectedSailingJSON(selectedSailingElem));
		 
		 Element currencyElem =	XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:Currency");
		 if(currencyElem!=null)
		 sailingOptionJSON.put("CurrencyCode", XMLUtils.getValueAtXPath(currencyElem, "./@CurrencyCode"));
		 
		 sailingOptionJSON.put("ItineraryID", "");
		 
		 Element selectedCategoryElem = XMLUtils.getFirstElementAtXPath(sailingInfoElem, "./ota:SelectedCategory");
		 
		 if(selectedCategoryElem!=null)
		 sailingOptionJSON.put("Category", getCategoryJsonArr(selectedCategoryElem));
		 
		 sailingOptionJSON.put("cruiseNumber",String.valueOf(random.nextInt(900) + 100 ));
		 
		 return sailingOptionJSON;
	}
	
	private static JSONArray getCategoryJsonArr(Element selectedCategoryElem)
    {
		JSONArray categoryJsonArr = new JSONArray(); // it will always have one category since we are inside reprice and the category is already chosen in the previous operation.
		
//		for(Element selectedCategoryElem : selectedCategoryElems)
		{
			JSONObject categoryJson = new JSONObject();
			
			categoryJson.put("PricedCategoryCode", XMLUtils.getValueAtXPath(selectedCategoryElem, "./@PricedCategoryCode"));
			categoryJson.put("FareCode", XMLUtils.getValueAtXPath(selectedCategoryElem, "./@FareCode"));
			categoryJson.put("WaitlistIndicator", XMLUtils.getValueAtXPath(selectedCategoryElem, "./@WaitlistIndicator"));
			categoryJson.put("CategoryName", XMLUtils.getValueAtXPath(selectedCategoryElem, "./@CategoryName"));
			
			Element[] selectedCabinElems = XMLUtils.getElementsAtXPath(selectedCategoryElem, "./ota:SelectedCabin");
			if(selectedCabinElems!=null)
			{
				categoryJson.put("CabinOptions", getCabinOptionsJsonArr(selectedCabinElems));
			}
			
			categoryJsonArr.put(categoryJson);
		}
		
		return categoryJsonArr;
    }
	 
	private static JSONArray getCabinOptionsJsonArr(Element[] selectedCabinElems)
    {
		JSONArray cabinOptionsJsonArr = new JSONArray();
		
		for(Element selectedCabinElem : selectedCabinElems)
		{
			JSONObject cabinOptionJson = new JSONObject();
			
			cabinOptionJson.put("Status", XMLUtils.getValueAtXPath(selectedCabinElem, "./@Status"));
			cabinOptionJson.put("CabinCategoryStatusCode", XMLUtils.getValueAtXPath(selectedCabinElem, "./@CabinCategoryStatusCode"));
			cabinOptionJson.put("CabinCategoryCode", XMLUtils.getValueAtXPath(selectedCabinElem, "./@CabinCategoryCode"));
			cabinOptionJson.put("CabinRanking", XMLUtils.getValueAtXPath(selectedCabinElem, "./@CabinRanking"));
			cabinOptionJson.put("CabinNumber", XMLUtils.getValueAtXPath(selectedCabinElem, "./@CabinNumber"));
			cabinOptionJson.put("MaxOccupancy", XMLUtils.getValueAtXPath(selectedCabinElem, "./@MaxOccupancy"));
			cabinOptionJson.put("DeckNumber", XMLUtils.getValueAtXPath(selectedCabinElem, "./@DeckNumber"));
			cabinOptionJson.put("DeckName", XMLUtils.getValueAtXPath(selectedCabinElem, "./@DeckName"));
			cabinOptionJson.put("DimensionInfo", XMLUtils.getValueAtXPath(selectedCabinElem, "./ota:MeasurementInfo/@DimensionInfo"));
			cabinOptionJson.put("Remark", XMLUtils.getValueAtXPath(selectedCabinElem, "./ota:Remark"));
			cabinOptionJson.put("CabinAttributeCode", XMLUtils.getValueAtXPath(selectedCabinElem, "./ota:CabinAttributes/ota:CabinAttribute/@CabinAttributeCode"));
			
			cabinOptionsJsonArr.put(cabinOptionJson);
			
		}
		
		return cabinOptionsJsonArr;
    }
	
    private static JSONObject getSelectedSailingJSON(Element selectedSailingElem)
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
    	
    	Element arrivalPortElem = XMLUtils.getFirstElementAtXPath(selectedSailingElem, "./ota:ArrivalPort");
    	if(arrivalPortElem!=null)
    	selectedSailingJSON.put("DeparturePort", getArrivalPortJSON(arrivalPortElem));
    	
    	return selectedSailingJSON;
    }
 
	private static JSONObject getCruiseLineJSON(Element cruiseLineElem)
    {
    	JSONObject cruiseLineJSON = new JSONObject();
    	
		cruiseLineJSON.put("VendorCode", XMLUtils.getValueAtXPath(cruiseLineElem, "./@VendorCode"));
		cruiseLineJSON.put("ShipCode", XMLUtils.getValueAtXPath(cruiseLineElem, "./@ShipCode"));
		cruiseLineJSON.put("ShipName", XMLUtils.getValueAtXPath(cruiseLineElem, "./@ShipName"));
		cruiseLineJSON.put("VendorName", XMLUtils.getValueAtXPath(cruiseLineElem, "./@VendorName"));
		cruiseLineJSON.put("VendorCodeContext", XMLUtils.getValueAtXPath(cruiseLineElem, "./@VendorCodeContext"));
    	
    	return cruiseLineJSON;
    }
	
	private static JSONObject getRegionJSON(Element regionElem)
    {
    	JSONObject regionJSON = new JSONObject();
    	
		regionJSON.put("RegionName", XMLUtils.getValueAtXPath(regionElem, "./@RegionName"));
		regionJSON.put("RegionCode", XMLUtils.getValueAtXPath(regionElem, "./@RegionCode"));
		regionJSON.put("SubRegionName", XMLUtils.getValueAtXPath(regionElem, "./@SubRegionName"));
    	
    	return regionJSON;
    }
	    
    private static JSONObject getDeparturePortJSON(Element departurePortElem)
    {
    	JSONObject departurePortJSON = new JSONObject();
    	
		departurePortJSON.put("EmbarkationTime", XMLUtils.getValueAtXPath(departurePortElem, "./@EmbarkationTime"));
		departurePortJSON.put("LocationCode", XMLUtils.getValueAtXPath(departurePortElem, "./@LocationCode"));
		departurePortJSON.put("CodeContext", XMLUtils.getValueAtXPath(departurePortElem, "./ota:DeparturePort"));
    	
    	return departurePortJSON;
    }
    private static JSONObject getArrivalPortJSON(Element arrivalPortElem)
    {
    	JSONObject arrivalPortJSON = new JSONObject();
    	
		arrivalPortJSON.put("DebarkationDateTime", XMLUtils.getValueAtXPath(arrivalPortElem, "./@DebarkationDateTime"));
		arrivalPortJSON.put("LocationCode", XMLUtils.getValueAtXPath(arrivalPortElem, "./@LocationCode"));
		arrivalPortJSON.put("CodeContext", XMLUtils.getValueAtXPath(arrivalPortElem, "/ota:ArrivalPort"));
    	
    	return arrivalPortJSON;
    }
}
