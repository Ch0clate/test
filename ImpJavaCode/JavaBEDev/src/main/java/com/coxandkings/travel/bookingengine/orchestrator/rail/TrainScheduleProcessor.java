package com.coxandkings.travel.bookingengine.orchestrator.rail;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.rail.RailConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class TrainScheduleProcessor implements RailConstants {

	private static final Logger logger = LogManager.getLogger(TrainScheduleProcessor.class);
	static final String OPERATION_NAME = "trainSchedule";


	public static String process(JSONObject reqJson) {
		try {
			OperationConfig opConfig = RailConfig.getOperationConfig(OPERATION_NAME);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionId = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionId = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userId = reqHdrJson.getString(JSON_PROP_USERID);
			
			
			UserContext usrContxt = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> productSuppliers = usrContxt.getSuppliersForProduct(PROD_CATEG_TRANSPORT,
					PROD_CAT_SUBTYPE);

			Element suppCredsList = XMLUtils.getFirstElementAtXPath(reqElem,
					"./rail:RequestHeader/com:SupplierCredentialsList");
			if (productSuppliers == null) {
				throw new Exception("Product supplier not found for user/client");
			}

			for (ProductSupplier prodSupplier : productSuppliers) {
				suppCredsList.appendChild(prodSupplier.toElement(ownerDoc));
			}
		
			// *************WEM JSON TO SI XML FOR REQUEST HEADER STARTS HERE*************//

			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:SessionID", sessionId);
			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:TransactionID", transactionId);
			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:UserID", userId);

			// **************WEM JSON TO SI XML FOR REQUEST HEADER ENDS HERE*************//

			// ***********WEM JSON TO SI XML FOR REQUEST BODY STARTS HERE**************//
			Element railScheduleRQ = XMLUtils.getFirstElementAtXPath(reqElem,
					"./raili:RequestBody/rail:OTA_RailScheduleRQWrapper/ota:OTA_RailScheduleRQ/ota:RailScheduleQuery");
			Element searchCriteria = XMLUtils.getFirstElementAtXPath(reqElem,
					"./raili:RequestBody/rail:OTA_RailScheduleRQWrapper/ota:OTA_RailScheduleRQ/ota:RailScheduleQuery/ota:RailSearchCriteria");

			XMLUtils.setValueAtXPath(reqElem, "./raili:RequestBody/rail:OTA_RailScheduleRQWrapper/rail:SupplierID",
					reqBodyJson.getString(JSON_PROP_SUPPREF));
			// TODO hard coded sequence value. ask if wrapper can repeat in case of IRCTC
			XMLUtils.setValueAtXPath(reqElem, "./raili:RequestBody/rail:OTA_RailScheduleRQWrapper/rail:Sequence", "1");

			if (reqBodyJson.has(JSON_PROP_TRAVELDATE) || reqBodyJson.has(JSON_PROP_ORIGINLOC)) {
				Element orgDestInfo = ownerDoc.createElementNS(NS_OTA, "ota:OriginDestinationInformation");
				if (reqBodyJson.has(JSON_PROP_ORIGINLOC) && !reqBodyJson.getString(JSON_PROP_ORIGINLOC).isEmpty()) {
					Element orgLoc = ownerDoc.createElementNS(NS_OTA, "ota:OriginLocation");
					orgLoc.setTextContent(reqBodyJson.getString(JSON_PROP_ORIGINLOC));
					orgDestInfo.appendChild(orgLoc);
				}
				if (reqBodyJson.has(JSON_PROP_TRAVELDATE) && !reqBodyJson.getString(JSON_PROP_TRAVELDATE).isEmpty()) {
					Element depDate = ownerDoc.createElementNS(NS_OTA, "ota:DepartureDateTime");
					depDate.setTextContent(reqBodyJson.getString(JSON_PROP_TRAVELDATE));
					orgDestInfo.appendChild(depDate);
				}
				railScheduleRQ.insertBefore(orgDestInfo, searchCriteria);
			}

			XMLUtils.setValueAtXPath(searchCriteria, "./ota:Train/ota:TrainNumber",
					reqBodyJson.getString(JSON_PROP_TRAINNUM));

			logger.info("Before opening HttpURLConnection to SI");
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					RailConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			logger.info("HttpURLConnection to SI closed");
			JSONObject resJson = getSupplierResponseJSON(reqJson, resElem);

			return resJson.toString();

		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}

	}

	public static JSONObject getSupplierResponseJSON(JSONObject reqJson, Element resElem) {
		JSONObject resJson = new JSONObject();
		JSONObject resBodyJson = new JSONObject();
		JSONObject orgDestJson, operationTime;
		JSONArray originDestOptionsJson = new JSONArray();
		operationTime = new JSONObject();
		Element orgDestOptsElem = XMLUtils.getFirstElementAtXPath(resElem,
				"./raili:ResponseBody/rail:OTA_RailScheduleRSWrapper/ota:OTA_RailScheduleRS/ota:OriginDestinationOptions");
		resBodyJson.put(JSON_PROP_ORIGINLOC, XMLUtils.getValueAtXPath(orgDestOptsElem, "./ota:OriginLocation"));
		resBodyJson.put(JSON_PROP_DESTLOC, XMLUtils.getValueAtXPath(orgDestOptsElem, "./ota:DestinationLocation"));

		Element[] orgDestArr = XMLUtils.getElementsAtXPath(orgDestOptsElem, "./ota:OriginDestinationOption");
		// get the first ODO and populate operation schedule json
		Element orgDestFirst = XMLUtils.getFirstElementAtXPath(orgDestOptsElem, "./ota:OriginDestinationOption");
		int count = 0;
		for (Element elem : orgDestArr) {
			count++;
			orgDestJson = getOriginDestOptionJSON(elem, count);
			originDestOptionsJson.put(orgDestJson);
		}

		resBodyJson.put(JSON_PROP_OPERATIONSCHEDULE, operationTime);
		String operationTimeXPath = "./ota:TrainSegment/ota:DepartureStation/ota:OperationSchedules/ota:OperationSchedule/ota:OperationTimes/ota:OperationTime/@";
		operationTime.put(weekDays[0],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[0]))));
		operationTime.put(weekDays[1],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[1]))));
		operationTime.put(weekDays[2],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[2]))));
		operationTime.put(weekDays[3],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[3]))));
		operationTime.put(weekDays[4],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[4]))));
		operationTime.put(weekDays[5],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[5]))));
		operationTime.put(weekDays[6],
				Boolean.valueOf(XMLUtils.getValueAtXPath(orgDestFirst, operationTimeXPath.concat(weekDays[6]))));

		resBodyJson.put(JSON_PROP_ORIGINDESTOPTS, originDestOptionsJson);
		resJson.put(JSON_PROP_RESHEADER, reqJson.get(JSON_PROP_REQHEADER));
		resJson.put(JSON_PROP_RESBODY, resBodyJson);

		return resJson;
	}

	public static JSONObject getOriginDestOptionJSON(Element orgDestElem, int count) {
		JSONObject orgDestOpt = new JSONObject();
		orgDestOpt.put(JSON_PROP_DEPARTTIME,
				XMLUtils.getValueAtXPath(orgDestElem, "./ota:TrainSegment/@DepartureDateTime"));
		orgDestOpt.put(JSON_PROP_ARRIVALTIME,
				XMLUtils.getValueAtXPath(orgDestElem, "./ota:TrainSegment/@ArrivalDateTime"));

		Element depStn = XMLUtils.getFirstElementAtXPath(orgDestElem, "./ota:TrainSegment/ota:DepartureStation");
		Element arrStn = XMLUtils.getFirstElementAtXPath(orgDestElem, "./ota:TrainSegment/ota:ArrivalStation");
		if (count == 1) {
			getStation(orgDestOpt, depStn);
		} else {
			getStation(orgDestOpt, arrStn);
		}

		return orgDestOpt;
	}

	public static void getStation(JSONObject orgDestOpt, Element elem) {
		JSONObject journeyDetails = new JSONObject();
		orgDestOpt.put(JSON_PROP_STATIONCODE, XMLUtils.getValueAtXPath(elem, "./ota:Details/@LocationCode"));
		orgDestOpt.put(JSON_PROP_STATIONNAME, XMLUtils.getValueAtXPath(elem, "./ota:Details/@CodeContext"));

		Element journeyDet = XMLUtils.getFirstElementAtXPath(elem,
				"./ota:OperationSchedules/ota:OperationSchedule/ota:TPA_Extensions/rail:Rail_TPA/rail:JourneyDetails");
		journeyDetails.put(JSON_PROP_JOURNEYDIST,
				Integer.valueOf(XMLUtils.getValueAtXPath(journeyDet, "./rail:JourneyDistance")));
		journeyDetails.put(JSON_PROP_HALTTIME, XMLUtils.getValueAtXPath(journeyDet, "./rail:HaltTime"));
		journeyDetails.put(JSON_PROP_DAYCOUNT,
				Integer.valueOf(XMLUtils.getValueAtXPath(journeyDet, "./rail:DayCount")));
		journeyDetails.put(JSON_PROP_ROUTENUM,
				Integer.valueOf(XMLUtils.getValueAtXPath(journeyDet, "./rail:RouteNumber")));
		orgDestOpt.put(JSON_PROP_JOURNEYDET, journeyDetails);
	}

}
