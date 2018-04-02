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

public class RailRepriceProcessor implements RailConstants {
	private static final Logger logger = LogManager.getLogger(RailRepriceProcessor.class);
	static final String OPERATION_NAME = "reprice";

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
			// *********WEM JSON TO SI XML FOR REQUEST HEADER STARTS HERE***********//

			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:SessionID", sessionId);
			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:TransactionID", transactionId);
			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:UserID", userId);

			// *******WEM JSON TO SI XML FOR REQUEST HEADER ENDS HERE**********//

			// *******WEM JSON TO SI XML FOR REQUEST BODY STARTS HERE******//

			// String railShopElemXPath =
			// "./raili:RequestBody/rail:OTA_RailShopRQWrapper/ota:OTA_RailShopRQ";
			Element railShopElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./raili:RequestBody/rail:OTA_RailShopRQWrapper/ota:OTA_RailShopRQ");
			Element preference = XMLUtils.getFirstElementAtXPath(reqElem,
					"./raili:RequestBody/rail:OTA_RailShopRQWrapper/ota:OTA_RailShopRQ/ota:Preferences");
			Element passengerType = XMLUtils.getFirstElementAtXPath(reqElem,
					"./raili:RequestBody/rail:OTA_RailShopRQWrapper/ota:OTA_RailShopRQ/ota:PassengerType");
			XMLUtils.removeNode(passengerType);

			JSONArray paxArr = reqBodyJson.getJSONArray("passengerDetails");
			int numOfPax = paxArr.length();

			Element passenger;
			for (int i = 0; i < numOfPax; i++) {
				JSONObject paxJson = paxArr.getJSONObject(i);
				passenger = (Element) passengerType.cloneNode(true);
				setPassengerType(ownerDoc, passenger, paxJson, reqBodyJson, i + 1);
				railShopElem.insertBefore(passenger, preference);

			}

			XMLUtils.setValueAtXPath(railShopElem, "./ota:OriginDestination/ota:DepartureDateTime",
					reqBodyJson.getString(JSON_PROP_TRAVELDATE));
			XMLUtils.setValueAtXPath(railShopElem, "./ota:OriginDestination/ota:OriginLocation",
					reqBodyJson.getString(JSON_PROP_ORIGINLOC));
			XMLUtils.setValueAtXPath(railShopElem, "./ota:OriginDestination/ota:DestinationLocation",
					reqBodyJson.getString(JSON_PROP_DESTLOC));
			XMLUtils.setValueAtXPath(reqElem, "./raili:RequestBody/rail:OTA_RailShopRQWrapper/rail:SupplierID",
					reqBodyJson.getString(JSON_PROP_SUPPREF));
			// TODO hard coded sequence value. ask if wrapper can repeat in case of IRCTC
			XMLUtils.setValueAtXPath(reqElem, "./raili:RequestBody/rail:OTA_RailShopRQWrapper/rail:Sequence", "1");

			Element amenities, railAmenity, accoCategory, accommodation, compartment, journeyDet, boardingStn, railTpa;
			amenities = XMLUtils.getFirstElementAtXPath(railShopElem, "./ota:Preferences/ota:RailAmenities");
			JSONArray amenitiesJson = reqBodyJson.getJSONArray("railAmenities");
			int amenitiesLen = amenitiesJson.length();
			JSONObject pref;
			railTpa=XMLUtils.getFirstElementAtXPath(railShopElem, "./ota:TPA_Extensions/rail:Rail_TPA");
			for (int i = 0; i < amenitiesLen; i++) {
				pref = amenitiesJson.getJSONObject(i);
				railAmenity = setRailAmenity(ownerDoc, pref);
				amenities.appendChild(railAmenity);
			}

			// preferred coach is optional. 
			if (reqBodyJson.has("preferredCoach") && reqBodyJson.getString("preferredCoach") != null) {
				accoCategory = ownerDoc.createElementNS(NS_OTA, "ota:AccommodationCategory");
				accommodation = ownerDoc.createElementNS(NS_OTA, "ota:Accommodation");
				compartment = ownerDoc.createElementNS(NS_OTA, "ota:Compartment");
				compartment.setTextContent(reqBodyJson.getString("preferredCoach"));
				preference.appendChild(accoCategory);
				accoCategory.appendChild(accommodation);
				accommodation.appendChild(compartment);
			}
			
			// boarding station is optional. 
			if (reqBodyJson.has("boardingStation") && reqBodyJson.getString("boardingStation") != null) {
				journeyDet = ownerDoc.createElementNS(NS_RAIL, "ota:JourneyDetails");
				boardingStn = ownerDoc.createElementNS(NS_RAIL, "ota:BoardingStation");
				journeyDet.appendChild(boardingStn);
				railTpa.appendChild(journeyDet);
			}

			XMLUtils.setValueAtXPath(railShopElem,
					"./ota:TPA_Extensions/rail:Rail_TPA/rail:ReservationDetails/rail:ReservationClass",
					reqBodyJson.getString(JSON_PROP_RESERVATIONCLASS));
			XMLUtils.setValueAtXPath(railShopElem,
					"./ota:TPA_Extensions/rail:Rail_TPA/rail:ReservationDetails/rail:ReservationType",
					reqBodyJson.getString(JSON_PROP_RESERVATIONTYPE));
			XMLUtils.setValueAtXPath(railShopElem, "./ota:TPA_Extensions/rail:Rail_TPA/rail:ReferenceID",
					reqBodyJson.getString("referenceID"));
			XMLUtils.setValueAtXPath(railShopElem, "./ota:RailSearchCriteria/ota:Train/ota:TrainNumber",
					reqBodyJson.getString(JSON_PROP_TRAINNUM));

			//System.out.println("Reprice xml req: " + XMLTransformer.toString(reqElem));
			logger.info("Before opening HttpURLConnection to SI");
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					RailConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			logger.info("HttpURLConnection to SI closed");

			JSONObject resJson = RailSearchProcessor.getSupplierResponseJSON(reqJson, resElem);
			//remove the extra fields not returned in reprice response of SI
			for(Object orgDestOpt: resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_ORIGINDESTOPTS)) {
				JSONObject orgDestOptJson=(JSONObject) orgDestOpt;
				JSONObject train=orgDestOptJson.getJSONObject(JSON_PROP_TRAINDETAILS);
				train.remove(JSON_PROP_OPERATIONSCHEDULE);
				train.remove(JSON_PROP_DEPARTTIME);
				train.remove(JSON_PROP_ARRIVALTIME);
				train.remove(JSON_PROP_JOURNEYDURATION);
				train.remove(JSON_PROP_TRAINTYPE);
			}
			return resJson.toString();

		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}

	}

	public static Element setRailAmenity(Document ownerDoc, JSONObject prefJson) {
		Element railAmenity = ownerDoc.createElementNS(NS_OTA, "ota:RailAmenity");
		railAmenity.setAttribute("CodeContext", prefJson.getString("codeContext"));
		if (prefJson.has("code")) {
			railAmenity.setAttribute("Code", prefJson.getString("code"));
		}
		return railAmenity;
	}

	public static void setPassengerType(Document ownerDoc, Element passengerElem, JSONObject passengerJson,
			JSONObject reqBody, int rph) {

		Element paxTypeElem, passengerDetail, identification, givenName, surname, tpaExt, tpa, paxDetails, foodChoice,
				profileRef, uniqueId, address, country, phone;
		
		passengerElem.setAttribute("Gender", passengerJson.getString("gender"));
		passengerElem.setAttribute("RPH", Integer.toString(rph));

		paxTypeElem = ownerDoc.createElementNS(NS_OTA, "ota:PassengerQualifyingInfo");
		String passengerType = passengerJson.getString("passengerType");
		paxTypeElem.setAttribute("Code", passengerType);

		passengerElem.appendChild(paxTypeElem);

		passengerDetail = ownerDoc.createElementNS(NS_OTA, "ota:PassengerDetail");
		passengerElem.appendChild(passengerDetail);
		identification = ownerDoc.createElementNS(NS_OTA, "ota:Identification");
		passengerDetail.appendChild(identification);
		givenName = ownerDoc.createElementNS(NS_OTA, "ota:GivenName");
		givenName.setTextContent(passengerJson.getString("firstName"));
		surname = ownerDoc.createElementNS(NS_OTA, "ota:Surname");
		surname.setTextContent(passengerJson.getString("lastName"));
		identification.appendChild(givenName);
		identification.appendChild(surname);
		if (rph == 1 && passengerType.equals("Adult")) {
			phone = ownerDoc.createElementNS(NS_OTA, "ota:Telephone");
			phone.setAttribute("PhoneNumber", reqBody.getString("phoneNumber"));
			passengerDetail.appendChild(phone);
		}
		tpaExt = ownerDoc.createElementNS(NS_OTA, "ota:TPA_Extensions");
		passengerDetail.appendChild(tpaExt);
		identification.appendChild(tpaExt);
		tpa = ownerDoc.createElementNS(NS_RAIL, "rail:Rail_TPA");
		tpaExt.appendChild(tpa);
		paxDetails = ownerDoc.createElementNS(NS_RAIL, "rail:PassengerDetails");
		paxDetails.setAttribute("Age", String.valueOf(passengerJson.getInt("age")));

		tpa.appendChild(paxDetails);
		if (passengerJson.has("berthChoice") && passengerJson.getString("berthChoice") != null) {
			paxDetails.setAttribute("BerthChoice", passengerJson.getString("berthChoice"));
		}
		if (passengerJson.has("bedrollChoice")) {
			paxDetails.setAttribute("BedrollChoice", String.valueOf(passengerJson.getBoolean("bedrollChoice")));
		}
		if (passengerJson.has("meal") && passengerJson.getString("meal") != null) {
			foodChoice = ownerDoc.createElementNS(NS_RAIL, "rail:FoodChoice");
			foodChoice.setTextContent(passengerJson.getString("meal"));
			paxDetails.appendChild(foodChoice);
		}
		JSONObject idProof;
		if (passengerType.equals("Senior") && passengerJson.getBoolean("concession") == true) {
			profileRef = ownerDoc.createElementNS(NS_OTA, "ota:ProfileRef");
			uniqueId = ownerDoc.createElementNS(NS_OTA, "ota:UniqueID");
			idProof = passengerJson.getJSONObject("identityProof");
			uniqueId.setAttribute("Type", idProof.getString("docType"));
			uniqueId.setAttribute("ID", idProof.getString("number"));
			passengerDetail.appendChild(profileRef);
			profileRef.appendChild(uniqueId);
			paxDetails.setAttribute("Concession", String.valueOf(passengerJson.getBoolean("concession")));
		}
		if (passengerType.equals("Child") && passengerJson.has("childBerthNeeded")) {
			paxDetails.setAttribute("ChildBerthNeeded", String.valueOf(passengerJson.has("childBerthNeeded")));
		}
		address = ownerDoc.createElementNS(NS_OTA, "ota:Address");
		country = ownerDoc.createElementNS(NS_OTA, "ota:CountryName");
		country.setAttribute("Code", passengerJson.getString("nationality"));
		passengerDetail.appendChild(address);
		address.appendChild(country);

	}

}
