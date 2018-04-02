package com.coxandkings.travel.bookingengine.orchestrator.cruise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.cruise.CruiseConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class CruiseCancelProcessor implements CruiseConstants {

	
private static final Logger logger = LogManager.getLogger(CruiseSearchProcessor.class);
	
	public static String process(JSONObject reqJson) throws Exception
	{
		KafkaBookProducer cancelProducer = new KafkaBookProducer();
		
		OperationConfig opConfig = CruiseConfig.getOperationConfig("cancel");
		//Element reqElem = (Element) mXMLPriceShellElem.cloneNode(true);
		Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
		//logger.info(String.format("Read Reprice Verify XML request template: %s\n", XMLTransformer.toEscapedString(reqElem)));
		Document ownerDoc = reqElem.getOwnerDocument();
		
		JSONObject kafkaCancelJson = new JSONObject();
		kafkaCancelJson = reqJson;
		
		Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./cru:RequestBody");
		Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./cru1:OTA_CancelRQWrapper");
		reqBodyElem.removeChild(wrapperElem);

		TrackingContext.setTrackingContext(reqJson);
		JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);

		UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
		
		CruiseSearchProcessor.createHeader(reqElem, sessionID, transactionID, userID);
		
		Element suppWrapperElem = null;
		int seqItr =0;
		JSONArray cruisePriceDetailsArr = reqBodyJson.getJSONArray("cancelRequests");
		
		for(int i=0;i<cruisePriceDetailsArr.length();i++)
		{
			JSONObject supplierBody = cruisePriceDetailsArr.getJSONObject(i);
			
			String suppID =	supplierBody.getString("supplierRef");
			
			suppWrapperElem = (Element) wrapperElem.cloneNode(true);
			reqBodyElem.appendChild(suppWrapperElem);
			
			//Request Header starts
			ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_TRANSPORT, "Cruise", suppID);
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
			
			Element otaCategoryAvail = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ota:OTA_CancelRQ");
			CruisePriceProcessor.createPOS(ownerDoc, otaCategoryAvail);
			
			Element reqVerificationElem =	XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:Verification");
			
			JSONArray reqUniqueIdJsonArr = supplierBody.getJSONArray("UniqueID");
			for(int j=0;j<reqUniqueIdJsonArr.length();j++)
			{
				JSONObject uniqueIdJson = reqUniqueIdJsonArr.getJSONObject(j);
				
				Element itineraryIDElem = ownerDoc.createElementNS(PRODUCT_CRUISE_SUPPLCRUISE, "ItineraryID");
				itineraryIDElem.setTextContent(uniqueIdJson.getString("ItineraryID"));
				
				Element tpa_ExtensionsElem = ownerDoc.createElementNS(NS_OTA, "TPA_Extensions");
				tpa_ExtensionsElem.appendChild(itineraryIDElem);
				
				Element uniqueIdElem =	ownerDoc.createElementNS(NS_OTA, "UniqueID");
				uniqueIdElem.setAttribute("ID", uniqueIdJson.getString("ID"));
				uniqueIdElem.setAttribute("Type", uniqueIdJson.getString("Type"));
				uniqueIdElem.appendChild(tpa_ExtensionsElem);
				
				otaCategoryAvail.insertBefore(uniqueIdElem, reqVerificationElem);
			}
			
			JSONObject reqPersonJson = supplierBody.getJSONObject("Verification").getJSONObject("PersonName");
			Element personNameElem = XMLUtils.getFirstElementAtXPath(reqVerificationElem, "./ota:PersonName");
			
			Element firstNameElem =	XMLUtils.getFirstElementAtXPath(personNameElem, "./ota:GivenName");
			firstNameElem.setTextContent(reqPersonJson.getString("GivenName"));
			
			Element middleNameElem = XMLUtils.getFirstElementAtXPath(personNameElem, "./ota:MiddleName");
			middleNameElem.setTextContent(reqPersonJson.getString("MiddleName"));
			
			Element surNameElem = XMLUtils.getFirstElementAtXPath(personNameElem, "./ota:Surname");
			surNameElem.setTextContent(reqPersonJson.getString("Surname"));
			
			JSONArray reqCancellationOverridesArr = supplierBody.getJSONArray("CancellationOverrides");
			Element cancellationOverridesElem =	XMLUtils.getFirstElementAtXPath(otaCategoryAvail, "./ota:CancellationOverrides");
			
			for(int j=0;j<reqCancellationOverridesArr.length();j++)
			{
				JSONObject reqCancellationOverridesJson = reqCancellationOverridesArr.getJSONObject(j);
				
				Element cancellationOverrideElem =	ownerDoc.createElementNS(NS_OTA, "CancellationOverride");
				cancellationOverrideElem.setAttribute("CancelByDate", reqCancellationOverridesJson.getString("CancelByDate"));
				
				cancellationOverridesElem.appendChild(cancellationOverrideElem);
			}
		}
		
		System.out.println(XMLTransformer.toString(reqElem));
		
		Element resElem = null;
        resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), CruiseConfig.getHttpHeaders(), reqElem);
        if (resElem == null) {
        	throw new Exception("Null response received from SI");
        }
        
        System.out.println(XMLTransformer.toString(resElem));
        
        //Cancel DB Population
        System.out.println(kafkaCancelJson.toString());
        cancelProducer.runProducer(1, kafkaCancelJson);
        
        /*DocumentBuilderFactory docBldrFactory = DocumentBuilderFactory.newInstance();
        docBldrFactory.setNamespaceAware(true);
		DocumentBuilder docBldr = docBldrFactory.newDocumentBuilder();
        
		Document doc = docBldr.parse(new File("D:\\BookingEngine\\StandardizedMSCCancelResponse.xml"));
		resElem = doc.getDocumentElement();*/
        
        JSONObject resBodyJson = new JSONObject();
        JSONArray cruiseRepriceDetailsJsonArr = new JSONArray();
        Element[] otaCategoryAvailWrapperElems = XMLUtils.getElementsAtXPath(resElem, "./cru:ResponseBody/cru1:OTA_CancelRSWrapper");
        for(Element otaCategoryAvailWrapperElem : otaCategoryAvailWrapperElems)
        {
//        	Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_CruiseSailAvailRS");
        	getSupplierResponseJSON(otaCategoryAvailWrapperElem,cruiseRepriceDetailsJsonArr);
        }
        
        System.out.println(cruiseRepriceDetailsJsonArr.toString());
        
        resBodyJson.put("cancel", cruiseRepriceDetailsJsonArr);
        
        kafkaCancelJson = new JSONObject();
        
        
        JSONObject kafkaRsBdyJson = cruiseRepriceDetailsJsonArr.getJSONObject(0);
        
        kafkaRsBdyJson.put("product", "CRUISE");
        kafkaRsBdyJson.put("entityName", "");
        kafkaRsBdyJson.put("entityId", reqJson.getJSONObject("requestBody").getJSONArray("cancelRequests").getJSONObject(0).getString("entityId"));
        kafkaRsBdyJson.put("requestType", reqJson.getJSONObject("requestBody").getJSONArray("cancelRequests").getJSONObject(0).getString("requestType"));
        kafkaRsBdyJson.put("type", reqJson.getJSONObject("requestBody").getJSONArray("cancelRequests").getJSONObject(0).getString("type"));
        cancelProducer.runProducer(1, kafkaRsBdyJson);
        
        kafkaCancelJson.put("responseHeader", reqJson.getJSONObject("requestHeader"));
        kafkaCancelJson.put("responseBody", kafkaRsBdyJson);
        
        System.out.println(kafkaCancelJson);
        JSONObject resJson = new JSONObject();
//        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, resBodyJson);
        
		return resJson.toString();
	}
	
	public static void getSupplierResponseJSON(Element otaCategoryAvailWrapperElem,JSONArray cruiseRepriceDetailsJsonArr) {
		
		JSONObject cancelRsJson = new JSONObject();
		
		String suppID =	XMLUtils.getValueAtXPath(otaCategoryAvailWrapperElem, "./cru1:SupplierID");
		cancelRsJson.put("supplierRef", suppID);
		
		Element otaCancelRsElem = XMLUtils.getFirstElementAtXPath(otaCategoryAvailWrapperElem, "./ota:OTA_CancelRS");
		
		Element[] uniqueIDElems = XMLUtils.getElementsAtXPath(otaCancelRsElem, "./ota:UniqueID");
		if(uniqueIDElems!=null)
		{
			JSONArray uniqueIDJson = getUniqueIDJsonArr(uniqueIDElems);
			cancelRsJson.put("UniqueID", uniqueIDJson);
		}
		
		Element cancelInfoRsElem =	XMLUtils.getFirstElementAtXPath(otaCancelRsElem, "./ota:CancelInfoRS");
		if(cancelInfoRsElem!=null)
		{
			JSONObject cancelInfoRsJson = getcancelInfoRsJson(cancelInfoRsElem);
			cancelRsJson.put("CancelInfo", cancelInfoRsJson);
		}
		
		Element errorsRsElem = XMLUtils.getFirstElementAtXPath(otaCancelRsElem, "./ota:Errors");
		if(errorsRsElem!=null)
		{
			JSONArray errorsJsonArr = getErrorJsonArr(errorsRsElem);
			cancelRsJson.put("ErrorMsg", errorsJsonArr);
		}
		
		cruiseRepriceDetailsJsonArr.put(cancelRsJson);
	}
	
	private static JSONArray getErrorJsonArr(Element errorsRsElem)
 	{
		JSONArray errorsJsonArr = new JSONArray();
		Element[] errorElems =	XMLUtils.getElementsAtXPath(errorsRsElem, "./ota:Error");
		
		for(Element errorElem : errorElems)
		{
			JSONObject errorJson = new JSONObject();
			errorJson.put("Language", XMLUtils.getValueAtXPath(errorElem, "./@Language"));
			errorJson.put("Type", XMLUtils.getValueAtXPath(errorElem, "./@Type"));
			errorJson.put("ShortText", XMLUtils.getValueAtXPath(errorElem, "./@ShortText"));
			errorJson.put("Code", XMLUtils.getValueAtXPath(errorElem, "./@Code"));
			errorJson.put("DocURL", XMLUtils.getValueAtXPath(errorElem, "./@DocURL"));
			errorJson.put("Status", XMLUtils.getValueAtXPath(errorElem, "./@Status"));
			errorJson.put("Tag", XMLUtils.getValueAtXPath(errorElem, "./@Tag"));
			errorJson.put("RecordID", XMLUtils.getValueAtXPath(errorElem, "./@RecordID"));
			errorJson.put("NodeList", XMLUtils.getValueAtXPath(errorElem, "./@NodeList"));
			
			errorsJsonArr.put(errorJson);
		}
		return errorsJsonArr;
 	}
	
	private static JSONObject getcancelInfoRsJson(Element cancelInfoRsElem)
 	{
		
		JSONObject cancelInfoRsJson = new JSONObject();
		
		Element[] cancelRuleElems =	XMLUtils.getElementsAtXPath(cancelInfoRsElem, "./ota:CancelRules/ota:CancelRule");
		if(cancelRuleElems!=null)
		{
			JSONArray cancelRuleJsonArr = getCancelRulesJsonArr(cancelRuleElems);
			cancelInfoRsJson.put("CancelRules", cancelRuleJsonArr);
		}
		
		Element[] cancelUniqueIDElems =	XMLUtils.getElementsAtXPath(cancelInfoRsElem, "./ota:UniqueID");
		if(cancelUniqueIDElems!=null)
		{
			JSONArray cancelUniqueIDJsonArr = getCancelUniqueIDJsonArr(cancelUniqueIDElems);
			cancelInfoRsJson.put("UniqueID", cancelUniqueIDJsonArr);
		}
		
		return cancelInfoRsJson;
 	}
	
	private static JSONArray getCancelUniqueIDJsonArr(Element[] cancelUniqueIDElems)
 	{
		JSONArray cancelUniqueIDJsonArr = new JSONArray();
		
		for(Element cancelUniqueIDElem : cancelUniqueIDElems)
		{
			JSONObject cancelUniqueIDJson = new JSONObject();
			
			cancelUniqueIDJson.put("Instance", XMLUtils.getValueAtXPath(cancelUniqueIDElem, "./@Instance"));
			cancelUniqueIDJson.put("ID", XMLUtils.getValueAtXPath(cancelUniqueIDElem, "./@ID"));
			
			cancelUniqueIDJsonArr.put(cancelUniqueIDJson);
		}
		return cancelUniqueIDJsonArr;
 	}
	
	private static JSONArray getCancelRulesJsonArr(Element[] cancelRuleElems)
 	{
		JSONArray cancelRuleJsonArr = new JSONArray();
		
		for(Element cancelRuleElem : cancelRuleElems)
		{
			JSONObject cancelRuleJson = new JSONObject();
			
			cancelRuleJson.put("Type", XMLUtils.getValueAtXPath(cancelRuleElem, "./@Type"));
			cancelRuleJson.put("Amount", XMLUtils.getValueAtXPath(cancelRuleElem, "./@Amount"));
			cancelRuleJson.put("CurrencyCode", XMLUtils.getValueAtXPath(cancelRuleElem, "./@CurrencyCode"));
			
			cancelRuleJsonArr.put(cancelRuleJson);
		}
		return cancelRuleJsonArr;
 	}
	
	private static JSONArray getUniqueIDJsonArr(Element[] uniqueIDElems)
 	{
		JSONArray uniqueIDJsonArr = new JSONArray();
		for(Element uniqueIDElem : uniqueIDElems)
		{
			JSONObject uniqueIDJson = new JSONObject();
			
			uniqueIDJson.put("ID", XMLUtils.getValueAtXPath(uniqueIDElem, "./@ID"));
			uniqueIDJson.put("Type", XMLUtils.getValueAtXPath(uniqueIDElem, "./@Type"));
			uniqueIDJson.put("Instance", XMLUtils.getValueAtXPath(uniqueIDElem, "./@Instance"));
			
			uniqueIDJsonArr.put(uniqueIDJson);
		}
		return uniqueIDJsonArr;
 	}
}
