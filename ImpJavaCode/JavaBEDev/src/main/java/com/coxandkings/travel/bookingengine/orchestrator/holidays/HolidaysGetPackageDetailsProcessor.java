package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class HolidaysGetPackageDetailsProcessor implements HolidayConstants {
	private static final Logger logger = LogManager.getLogger(HolidaysSearchProcessor.class);

	public static String process(JSONObject reqJson) {

		try {
			String errorShortText = "";
			String errorType = "";
			String errorCode = "";
			String errorStatus = "";
			OperationConfig opConfig = HolidaysConfig.getOperationConfig("getDetails");

			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			 UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			 List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_HOLIDAYS, PROD_CATEG_HOLIDAYS);

			XMLUtils.setValueAtXPath(reqElem, "./pac:RequestHeader/com:SessionID", sessionID);
			XMLUtils.setValueAtXPath(reqElem, "./pac:RequestHeader/com:TransactionID", transactionID);
			XMLUtils.setValueAtXPath(reqElem, "./pac:RequestHeader/com:UserID", userID);

			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac:RequestHeader/com:SupplierCredentialsList");
			for (ProductSupplier prodSupplier : prodSuppliers) {

				Element suppCredsElem = prodSupplier.toElement(ownerDoc);

				Element sequenceElem = ownerDoc.createElementNS(NS_COM, "com:Sequence");
				sequenceElem.setTextContent("3");
				suppCredsElem.appendChild(sequenceElem);

				suppCredsListElem.appendChild(suppCredsElem);
			}
			
		

			String refPointCode = reqBodyJson.get("brandName").toString();
			String optionRefCode = reqBodyJson.get("tourCode").toString();
			String quoteID = reqBodyJson.get("subTourCode").toString();
			String destinationLocation = reqBodyJson.getString("destinationLocation").toString();

			Element refPointElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/pac:OTA_DynamicPkgAvailRQWrapper/ns:OTA_DynamicPkgAvailRQ/ns:SearchCriteria/ns:PackageOptionSearch/ns:OptionSearchCriteria/ns:Criterion/ns:RefPoint");
			Element optionRefElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/pac:OTA_DynamicPkgAvailRQWrapper/ns:OTA_DynamicPkgAvailRQ/ns:SearchCriteria/ns:PackageOptionSearch/ns:OptionSearchCriteria/ns:Criterion/ns:OptionRef");
			
			//Get from JSON
			Element supplierIDElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/pac:OTA_DynamicPkgAvailRQWrapper/pac:SupplierID");
			Element supplierSeqElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/pac:OTA_DynamicPkgAvailRQWrapper/pac:Sequence");
			
			Element suppIDElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac:RequestHeader/com:SupplierCredentialsList/com:SupplierCredentials");
			
			String supplierID = String.valueOf(XMLUtils.getValueAtXPath(suppIDElem, "./com:SupplierID"));
			
			supplierIDElem.setTextContent(supplierID);
		
			Element seqElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac:RequestHeader/com:SupplierCredentialsList/com:SupplierCredentials");
			
			String seq = String.valueOf(XMLUtils.getValueAtXPath(seqElem, "./com:Sequence"));
			
			supplierSeqElem.setTextContent(seq);
			//Get from JSOn end
			
			Attr codeAttr = ownerDoc.createAttribute(JSON_PROP_CODE);
			codeAttr.setValue(refPointCode);
			refPointElem.setAttributeNode(codeAttr);

			Attr optionAttr = ownerDoc.createAttribute(JSON_PROP_CODE);
			optionAttr.setValue(optionRefCode);
			optionRefElem.setAttributeNode(optionAttr);

			Element packageOptionElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/pac:OTA_DynamicPkgAvailRQWrapper/ns:OTA_DynamicPkgAvailRQ/ns:DynamicPackage/ns:Components/ns:PackageOptionComponent");
			Attr quoteIdAttr = ownerDoc.createAttribute("QuoteID");
			quoteIdAttr.setValue(quoteID);
			packageOptionElem.setAttributeNode(quoteIdAttr);

			System.out.println(XMLTransformer.toString(reqElem));

			String reqStr = XMLTransformer.toString(reqElem);

			logger.info("Before opening HttpURLConnection");

			Element resElem = null;
			// logger.info(String.format("SI XML Request = %s",
			// XMLTransformer.toString(reqElem)));
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					HolidaysConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			logger.info(String.format("SI XML Response = %s", XMLTransformer.toString(resElem)));
			System.out.println(XMLTransformer.toString(resElem));

			// Added code for converting SI XML response to SI JSON Response

			JSONObject resBodyJson = new JSONObject();
		     JSONArray dynamicPackageArray = new JSONArray();
		      
		      Element[] oTA_wrapperElems = XMLUtils.getElementsAtXPath(resElem, 
		        "./pac1:ResponseBody/pac:OTA_DynamicPkgAvailRSWrapper");
		   
		      
		      for (Element oTA_wrapperElem : oTA_wrapperElems) {
		     
		        String supplierIDStr = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:SupplierID"));
		        String sequenceStr = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:Sequence"));
		        
		      //Error Response from SI
		        Element errorElem[] = XMLUtils.getElementsAtXPath(oTA_wrapperElem, "./ns:OTA_DynamicPkgAvailRS/ns:Errors/ns:Error");
				if(errorElem.length != 0) {
					for(Element error : errorElem) {
						errorShortText = String.valueOf(XMLUtils.getValueAtXPath(error, "./@ShortText"));
						errorType = String.valueOf(XMLUtils.getValueAtXPath(error, "./@Type"));
						errorCode = String.valueOf(XMLUtils.getValueAtXPath(error, "./@Code"));
						errorStatus = String.valueOf(XMLUtils.getValueAtXPath(error, "./@status"));
						logger.info(String.format("Recieved Error from SI. Error Details: ErrorCode:" + errorCode + ", Type:" + errorType + ", Status:" + errorStatus + ", ShortText:"+ errorShortText));
					}
					continue;
					
				}
		        
		        Element[] dynamicPackageElem = XMLUtils.getElementsAtXPath(oTA_wrapperElem, 
		          "./ns:OTA_DynamicPkgAvailRS/ns:DynamicPackage");
		        
		        for (Element dynamicPackElem : dynamicPackageElem)
		        {
		          JSONObject dynamicPackJson = HolidaysSearchProcessor.getSupplierResponseDynamicPackageJSON(dynamicPackElem);
		          dynamicPackJson.put(JSON_PROP_SUPPLIERID, supplierIDStr);
		          dynamicPackJson.put(JSON_PROP_SEQUENCE, sequenceStr);
		          dynamicPackageArray.put(dynamicPackJson);
		         
		        }
		      }
		      

		      resBodyJson.put(JSON_PROP_DYNAMICPACKAGE, dynamicPackageArray);
		      

		      JSONObject resJson = new JSONObject();
		      resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
		      resJson.put(JSON_PROP_RESBODY, resBodyJson);
		      logger.info(String.format("SI Transformed JSON Response = %s", new Object[] { resJson.toString() }));
		      
		      System.out.println("SI Transformed JSON Response = " + resJson.toString());

			// Call BRMS Supplier and Client Commercials

			logger.info(String.format("Calling to Supplier Commercial"));

			JSONObject resSupplierCommJson = HolidaysSupplierCommercials.getSupplierCommercials(reqJson, resJson,opConfig.getOperationName());
			if (BRMS_STATUS_TYPE_FAILURE.equals(resSupplierCommJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Supplier Commercials calculation engine: %s", resSupplierCommJson.toString()));
            			return getEmptyResponse(reqHdrJson).toString();
			}
			
			logger.info(String.format("Supplier Commercial Response = %s", resSupplierCommJson.toString()));
			System.out.println("Supplier Commercial Response = " + resSupplierCommJson.toString());

			JSONObject resClientCommJson = HolidaysClientCommercials.getClientCommercials(resSupplierCommJson);
			//TODO- handle commercial errors in a better way
			if (BRMS_STATUS_TYPE_FAILURE.equals(resClientCommJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Client Commercials calculation engine: %s", resClientCommJson.toString()));
            			return getEmptyResponse(reqHdrJson).toString();
			}

			logger.info(String.format("Client Commercial Response = %s", resClientCommJson.toString()));
			System.out.println("Client Commercial Response = " + resClientCommJson.toString());

			HolidaysSearchProcessor.calculatePrices(reqJson, resJson, resSupplierCommJson, resClientCommJson);

			return resJson.toString();

		}

		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}

	}
	public static JSONObject getEmptyResponse(JSONObject reqHdrJson) {
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, new JSONObject());
        return resJson;
    }

	
}

