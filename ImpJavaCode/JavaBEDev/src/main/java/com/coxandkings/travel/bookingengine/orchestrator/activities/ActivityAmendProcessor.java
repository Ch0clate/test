package com.coxandkings.travel.bookingengine.orchestrator.activities;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.activities.ActivitiesConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class ActivityAmendProcessor implements ActivityService{
	
	private static final Logger logger = LogManager.getLogger(ActivityAmendProcessor.class);

	public static String process(JSONObject reqJson) {
		try {

			OperationConfig opConfig = ActivitiesConfig.getOperationConfig("amend");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			Element blankWrapperElem = XMLUtils.getFirstElementAtXPath(reqElem, "./sig:RequestBody/sig1:OTA_TourActivityModifyRQWrapper");
			XMLUtils.removeNode(blankWrapperElem);


			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(ActivityService.JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(ActivityService.JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(ActivityService.JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(ActivityService.JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(ActivityService.JSON_PROP_USERID);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			
			XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_SESSIONID, sessionID);
			XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_TRANSACTIONID, transactionID);
			XMLUtils.setValueAtXPath(reqElem, XPATH_REQUESTHEADER_USERID, userID);

			JSONArray actReqArr = reqBodyJson.getJSONArray(JSON_PROP_ACTIVITYINFO);
			Element wrapperElement;
			JSONObject activityInfo;

			for (int i = 0; i < actReqArr.length(); i++) {

			activityInfo = actReqArr.getJSONObject(i);
			wrapperElement = (Element) blankWrapperElem.cloneNode(true);
			String suppID = activityInfo.getString(SUPPLIER_ID);
			Element bookingInfoElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ns:OTA_TourActivityModifyRQ/ns:BookingInfo");
			ProductSupplier prodSupplier = usrCtx.getSupplierForProduct(ActivityService.PRODUCT_CATEGORY,ActivityService.PRODUCT_SUBCATEGORY, suppID);
			if (prodSupplier == null) {
				throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
			}
			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, XPATH_REQUESTHEADER_SUPPLIERCREDENTIALSLIST);
			suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc,i));
			Element suppIDElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./sig1:SupplierID");
			suppIDElem.setTextContent(suppID);
			Element sequenceElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./sig1:Sequence");
			sequenceElem.setTextContent(String.valueOf(i));
			XMLUtils.setValueAtXPath(bookingInfoElem, "./ns:BasicInfo/@SupplierProductCode", activityInfo.getString(JSON_PROP_SUPPLIERPRODUCTCODE));
			JSONObject confirmation = getConfirmation(activityInfo.getString("bookRefId"));
			
			XMLUtils.setValueAtXPath(bookingInfoElem, "./ns:Confirmation/@Type", confirmation.getString(JSON_PROP_TYPE));
			XMLUtils.setValueAtXPath(bookingInfoElem, "./ns:Confirmation/@ID", confirmation.getString(JSON_PROP_ID));
			
			JSONArray participantInfoJsonArr = activityInfo.getJSONArray(JSON_PROP_PARTICIPANT_INFO);
			Element participantInfoElemBlank = XMLUtils.getFirstElementAtXPath(wrapperElement,"./ns:OTA_TourActivityModifyRQ/ns:BookingInfo/ns:ParticipantInfo");
			XMLUtils.removeNode(participantInfoElemBlank);
			Element scheduleElem=XMLUtils.getFirstElementAtXPath(wrapperElement, "./ns:OTA_TourActivityModifyRQ/ns:BookingInfo/ns:Schedule");
			 for(int j=0;j<participantInfoJsonArr.length();j++) {
			JSONObject participantInfoJson=participantInfoJsonArr.getJSONObject(j);
			Element participantInfoElem = (Element) participantInfoElemBlank.cloneNode(true);			
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/@ParticipantCategoryID", participantInfoJson.getString(JSON_PROP_PARTICIPANT_CATEGORY_ID));
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/ns:QualifierInfo", participantInfoJson.getString(JSON_PROP_QUALIFIERINFO));			
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:NamePrefix", participantInfoJson.getString(JSON_PROP_NAME_PREFIX));			
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:GivenName", participantInfoJson.getString(JSON_PROP_GIVEN_NAME));		
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:MiddleName", participantInfoJson.getString(JSON_PROP_MIDDLE_NAME));		
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:Surname", participantInfoJson.getString(JSON_PROP_SURNAME));
			XMLUtils.setValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:NameTitle", participantInfoJson.getString(JSON_PROP_NAME_TITLE));			 
			bookingInfoElem.insertBefore(participantInfoElem,scheduleElem);			
			 }
			 
   		    XMLUtils.setValueAtXPath(scheduleElem, "./@Start", activityInfo.getString(JSON_PROP_START));
			XMLUtils.insertChildNode(reqElem, "./sig:RequestBody", wrapperElement, false);
			
			}
			
			logger.info("Before opening HttpURLConnection");
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), ActivitiesConfig.getHttpHeaders(), reqElem);
			
			if (resElem == null) {
				throw new Exception(ERROR_MSG_NULL_RESPONSE_SI);
			}

			System.out.println("Amend supplier response : "+ XMLTransformer.toString(resElem));
			Element[] resBodyElem = XMLUtils.getElementsAtXPath(resElem, "./sig:ResponseBody/sig1:OTA_TourActivityBookRSWrapper");
			JSONObject resBodyJson = getSupplierResponseJSON(resBodyElem);

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));
     		Map<String,JSONObject> briActTourActMap= new HashMap<String,JSONObject>();
			JSONObject resSupplierComJson =  SupplierCommercials.getSupplierCommercials(reqJson,reqElem, resJson,briActTourActMap,usrCtx);
			JSONObject resClientComJson = ClientCommercials.getClientCommercials(resSupplierComJson);
			ActivitySearchProcessor.calculatePrices(reqJson,resJson, resSupplierComJson, resClientComJson,briActTourActMap,usrCtx,true);
    		
			// TODO:remove supplier price from resJSON
			//pushSuppFaresToRedisAndRemove(resJson);
			
			return resJson.toString();

		} 
		catch (Exception x) {
			x.printStackTrace();
			return STATUS_ERROR;
		}


	}
	
	
	private static JSONObject getConfirmation(String confirmationString) {

		JSONArray confirmationArray = new JSONArray();

		String[] split1Array = confirmationString.split("\\|");

		for (String split1String : split1Array) {
			JSONObject confirmationObject = new JSONObject();
			String[] split2Array = split1String.split(",");
			for (int split2Count = 0; split2Count < split2Array.length; split2Count++) {

				switch (split2Count) {
				case 0:
					confirmationObject.put("ID", split2Array[0]);
					break;
				case 1:
					confirmationObject.put("type", split2Array[1]);
					break;
				case 2:
					confirmationObject.put("instance", split2Array[2]);
					break;
				}
			}
			confirmationArray.put(confirmationObject);
		}

		for (int confirmationCount = 0; confirmationCount < confirmationArray.length(); confirmationCount++) {
			if (confirmationArray.getJSONObject(confirmationCount).getString("type").equalsIgnoreCase("14")) {
				return confirmationArray.getJSONObject(confirmationCount);
			}
		}
		return null;

	}


	private static JSONObject getSupplierResponseJSON(Element[] resBodyElems) {

		JSONObject resJson = new JSONObject();
		Element[] tourActivityElems;
		JSONArray activityInfoJsonArr = new JSONArray();
		resJson.put(JSON_PROP_ACTIVITYINFO, activityInfoJsonArr);
		for (Element resBodyElem : resBodyElems) {
			JSONObject activityInfoJson = new JSONObject();
			JSONArray tourActivityJsonArr = new JSONArray();
			activityInfoJson.put(JSON_PROP_TOURACTIVITYINFO, tourActivityJsonArr);
			String supplierID = XMLUtils.getValueAtXPath(resBodyElem, "./sig1:SupplierID");
			String sequence=XMLUtils.getValueAtXPath(resBodyElem, "./sig1:Sequence");
			tourActivityElems = XMLUtils.getElementsAtXPath(resBodyElem,
					"./ns:OTA_TourActivityBookRS/ns:ReservationDetails");
			for (Element tourActivityElem : tourActivityElems) {
				JSONObject tourActivityJson = getTourActivityJSON(tourActivityElem, supplierID);
				tourActivityJsonArr.put(tourActivityJson);
			}
			activityInfoJsonArr.put(Integer.parseInt(sequence),activityInfoJson);
		}

		return resJson;

	}
	
	
	private static JSONObject getTourActivityJSON(Element tourActivityElem, String supplierID) {
		JSONObject tourActivityJson = new JSONObject();
		tourActivityJson.put(SUPPLIER_ID, supplierID);
		tourActivityJson.put(JSON_PROP_BASICINFO, getBasicInfoJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:BasicInfo")));
		tourActivityJson.put(JSON_PROP_PRICING, getPricingJson(XMLUtils.getFirstElementAtXPath(tourActivityElem, "./ns:Pricing"),supplierID));
		tourActivityJson.put(JSON_PROP_START, XMLUtils.getValueAtXPath(tourActivityElem,"./ns:Schedule/@Start"));
		tourActivityJson.put(JSON_PROP_PARTICIPANT_INFO, getParticipantInfoJson(XMLUtils.getElementsAtXPath(tourActivityElem, "./ns:ParticipantInfo")));
	return tourActivityJson;
	}
	
	private static JSONObject getBasicInfoJson(Element basicInfoElem) {
		JSONObject basicInfoJson = new JSONObject();
		basicInfoJson.put(JSON_PROP_SUPPLIERPRODUCTCODE, XMLUtils.getValueAtXPath(basicInfoElem, "./@SupplierProductCode"));
		basicInfoJson.put(JSON_PROP_NAME, XMLUtils.getValueAtXPath(basicInfoElem, "./@Name"));
		return basicInfoJson;

	}
	
	private static JSONArray getPricingJson(Element pricingElem, String supplierID) {
		JSONArray pricingJsonArr = new JSONArray();
		if (XMLUtils.getValueAtXPath(pricingElem, "./@PerPaxPriceInd").equalsIgnoreCase("true"))
			pricingJsonArr = ActivityPricing.paxPricing.getPricingJson(pricingJsonArr, pricingElem);
		else
			pricingJsonArr = ActivityPricing.summaryPricing.getPricingJson(pricingJsonArr, pricingElem);
		return pricingJsonArr;
	}
	
	private static JSONArray getParticipantInfoJson(Element[] participantInfoElems) {
		JSONArray participantInfoJsonArr=new JSONArray();
		for (Element participantInfoElem : participantInfoElems) {
			JSONObject participantInfoJson = new JSONObject();			
			participantInfoJson.put(JSON_PROP_PARTICIPANT_CATEGORY_ID, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/@ParticipantCategoryID"));
			participantInfoJson.put(JSON_PROP_QUALIFIERINFO, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/ns:QualifierInfo"));
			participantInfoJson.put(JSON_PROP_NAME_PREFIX, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:NamePrefix"));
			participantInfoJson.put(JSON_PROP_GIVEN_NAME, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:GivenName"));
			participantInfoJson.put(JSON_PROP_MIDDLE_NAME, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:MiddleName"));
			participantInfoJson.put(JSON_PROP_SURNAME, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:Surname"));
			participantInfoJson.put(JSON_PROP_NAME_TITLE, XMLUtils.getValueAtXPath(participantInfoElem, "./ns:Category/ns:Contact/ns:PersonName/ns:NameTitle"));
			participantInfoJson.put(JSON_PROP_STATUS, XMLUtils.getValueAtXPath(participantInfoElem,"./ns:Category/ns:TPA_Extensions/sig1:Activity_TPA/sig1:Status"));
			participantInfoJsonArr.put(participantInfoJson);
		}
		return participantInfoJsonArr;
	}

}
