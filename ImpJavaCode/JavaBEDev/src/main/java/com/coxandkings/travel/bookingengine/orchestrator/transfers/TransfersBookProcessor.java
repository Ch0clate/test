package com.coxandkings.travel.bookingengine.orchestrator.transfers;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.transfers.TransfersConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.orchestrator.car.CarBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.car.CarPriceProcessor;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class TransfersBookProcessor implements TransfersConstants {
	@Autowired
	private static final Logger logger = LogManager.getLogger(TransfersBookProcessor.class);
	private static final DateFormat mDtFmt = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	public static void getSupplierResponseGroundWrapperJSON(Element resWrapperElem, JSONArray groundWrapperJsonArr)
			throws Exception {
		getSupplierResponseGroundBookWrapperJSON(resWrapperElem, groundWrapperJsonArr, false, 0);
	}

	public static void getSupplierResponseGroundBookWrapperJSON(Element resWrapperElem, JSONArray groundWrapperJsonArr,
			boolean generateBookRefIdx, int bookRefIdx) throws Exception {
		// boolean isCombinedReturnJourney =
		// Boolean.valueOf(XMLUtils.getValueAtXPath(resBodyElem,
		// "./@CombinedReturnJourney"));
		String suppId = XMLUtils.getValueAtXPath(resWrapperElem, "./tran1:SupplierID");
		String sequence = XMLUtils.getValueAtXPath(resWrapperElem, "./tran1:Sequence");
		JSONObject bookwrapperJson = new JSONObject();
		try {
			bookwrapperJson.put(JSON_PROP_SUPPREF, suppId.split("_")[1] != null ? suppId.split("_")[1] : suppId);
		} catch (Exception e) {
			bookwrapperJson.put(JSON_PROP_SUPPREF, suppId);
		} finally {
			bookwrapperJson.put("sequence", sequence);
		}
		// groundServiceJson.put("isReturnJourneyCombined", isCombinedReturnJourney);
		/*
		 * if (generateBookRefIdx) { groundServiceJson.put(JSON_PROP_BOOKREFIDX,
		 * bookRefIdx); }
		 */
		JSONArray reservationIdsArr = new JSONArray();
		Element reservationElem = XMLUtils.getFirstElementAtXPath(resWrapperElem,
				"./ns:OTA_GroundBookRS/ns:Reservation");
		JSONObject reservationjson = new JSONObject();
		reservationjson.put("status", XMLUtils.getValueAtXPath(reservationElem, "./@Status"));

		Element confirmationid[] = XMLUtils.getElementsAtXPath(reservationElem, "./ns:Confirmation");
		for (Element confid : confirmationid) {
			JSONObject reservationIdJson = new JSONObject();
			// ID="TS" ID_Context="s_practica_sigla" Type="16"
			reservationIdJson.put("id", XMLUtils.getValueAtXPath(confid, "./@ID"));
			reservationIdJson.put("id_Context", XMLUtils.getValueAtXPath(confid, "./@ID_Context"));
			reservationIdJson.put("type", XMLUtils.getValueAtXPath(confid, "./@Type"));

			reservationIdsArr.put(reservationIdJson);
		}
		bookwrapperJson.put("status", reservationjson);
		bookwrapperJson.put("reservationIds", reservationIdsArr);
		groundWrapperJsonArr.put(bookwrapperJson);
	}

	public static String process(JSONObject reqJson) throws Exception {
		
		try {
			KafkaBookProducer bookProducer = new KafkaBookProducer();
			OperationConfig opConfig = TransfersConfig.getOperationConfig("book");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			Element reqBodyElem = XMLUtils.getFirstElementAtXPath(reqElem, "./tran:RequestBody");
			Element wrapperElem = XMLUtils.getFirstElementAtXPath(reqBodyElem, "./tran1:OTA_GroundBookRQWrapper");
			reqBodyElem.removeChild(wrapperElem);
			// System.out.println(XMLTransformer.toString(reqBodyElem));
			TrackingContext.setTrackingContext(reqJson);
			JSONObject kafkaMsgJson = reqJson;
			JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null)
					? reqJson.optJSONObject(JSON_PROP_REQHEADER)
					: new JSONObject();
			JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null)
					? reqJson.optJSONObject(JSON_PROP_REQBODY)
					: new JSONObject();

			/*
			 * JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			 * JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
			 */

			/*
			 * String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID); String
			 * transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID); String userID =
			 * reqHdrJson.getString(JSON_PROP_USERID);
			 * 
			 * UserContext usrCtx = UserContext.getUserContextForSession(sessionID);
			 * List<ProductSupplier> prodSuppliers =
			 * usrCtx.getSuppliersForProduct(TransfersConstants.PRODUCT_TRANSFERS);
			 * 
			 * 
			 * 
			 * XMLUtils.setValueAtXPath(reqElem, "./tran1:RequestHeader/com:SessionID",
			 * sessionID); XMLUtils.setValueAtXPath(reqElem,
			 * "./tran1:RequestHeader/com:TransactionID", transactionID);
			 * XMLUtils.setValueAtXPath(reqElem, "./tran1:RequestHeader/com:UserID",
			 * userID);
			 */

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);

			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_TRANSPORT,
					PROD_CATEG_SUBTYPE_TRANSFER);

			TransfersSearchProcessor.createHeader(reqHdrJson, reqElem);

			// TODO : Redis Connection

			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./tran1:RequestHeader/com:SupplierCredentialsList");
			for (ProductSupplier prodSupplier : prodSuppliers) {
				suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
			}

			
			  Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			  String redisKey =
			  String.format("%s%c%s", reqHdrJson.optString(JSON_PROP_SESSIONID),
			  KEYSEPARATOR, PRODUCT_TRANSFERS); 
			  System.out.println(redisKey);
			  Map<String, String> searchSuppFaresMap = redisConn.hgetAll(redisKey); 
			  if (searchSuppFaresMap == null) {   
				  throw new Exception(String.format("Search context not found,for %s", redisKey)); }
			 
			JSONArray transJSONArr = reqBodyJson.getJSONArray(JSON_PROP_TRANSFERSINFO);
			for (int t = 0; t < transJSONArr.length(); t++) {
				JSONObject bookReq = transJSONArr.getJSONObject(t);
				String suppID = bookReq.getString(JSON_PROP_SUPPREF);
				Element suppWrapperElem = null;
				
				JSONObject totalPricingInfo =  new JSONObject();
						/*bookReq.getJSONObject(JSON_PROP_TOTALFARE);*/
				String vehicleKey = getRedisKeyForGroundService(bookReq);
				//Appending Commercials and Fares in KafkaBookReq For Database Population
				JSONObject suppPriceBookInfoJson = new JSONObject(searchSuppFaresMap.get(vehicleKey));
				JSONObject suppPricingInfo = suppPriceBookInfoJson.getJSONObject(JSON_PROP_SUPPPRICEINFO);
				bookReq.put(JSON_PROP_SUPPPRICEINFO, suppPricingInfo);
				JSONObject clientCommercialItinTotalInfoObj = suppPriceBookInfoJson.getJSONObject(JSON_PROP_CLIENTCOMMTOTAL);
				bookReq.put(JSON_PROP_CLIENTCOMMTOTAL, clientCommercialItinTotalInfoObj);
				
				suppWrapperElem = (Element) wrapperElem.cloneNode(true);
				reqBodyElem.appendChild(suppWrapperElem);
				/*
				 * ProductSupplier prodSupplier =
				 * usrCtx.getSupplierForProduct(TransfersConstants.PRODUCT_TRANSFERS, suppID);
				 */
				if (prodSuppliers == null) {
					throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
				}
		
				
				Element suppIDElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./tran1:SupplierID");
				suppIDElem.setTextContent(suppID);

				Element sequenceElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./tran1:Sequence");
				sequenceElem.setTextContent("1");

				Element posElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ns:OTA_GroundBookRQ/ns:POS");
				/* Element ground = (Element)posElem.getParentNode(); */
				Element sourceElem = XMLUtils.getFirstElementAtXPath(posElem, "./ns:Source");
				// TODO: hardcode for ISOCurrency!get it from where?
				sourceElem.setAttribute("ISOCurrency", bookReq.getString("currencyCode"));

				Element groundReservationElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem,
						"./ns:OTA_GroundBookRQ/ns:GroundReservation");

				// JSONObject groundReservationJson =
				// reqBodyJson.getJSONObject("groundReservation");

				if (null != bookReq.optJSONArray("references")) {
					JSONArray referencesArr = bookReq.getJSONArray("references");
					int referencesArrLen = referencesArr.length();
					// Element referenceElem = ownerDoc.createElementNS(Constants.NS_OTA,
					// "ns:Reference");
					Element groundAvailElem = XMLUtils.getFirstElementAtXPath(suppWrapperElem, "./ns:OTA_GroundBookRQ");
					// groundAvailElem.removeChild(referenceElem);
					getJSONReference(ownerDoc, groundReservationElem, referencesArr, referencesArrLen, groundAvailElem);

				}

				JSONArray passengerArr = bookReq.getJSONArray("paxDetails"); /* getJSONArray("paxDetails"); */
				int passengerArrLen = passengerArr.length();
				getJSONPaxDetails(ownerDoc, groundReservationElem, passengerArr, passengerArrLen);

				// TODO: for loop
				JSONArray serviceArr = bookReq.getJSONArray("service");
				int serviceArrLen = serviceArr.length();

				getJSONService(ownerDoc, groundReservationElem, serviceArr, serviceArrLen);

			}
			bookProducer.runProducer(1, kafkaMsgJson);
			
			// System.out.println(XMLTransformer.toString(reqElem));
			Element resElem = null;
			// logger.trace(String.format("SI XML Request = %s",
			// XMLTransformer.toString(reqElem)));
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					TransfersConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			JSONObject resBodyJson = new JSONObject();
			JSONArray groundWrapperJsonArray = new JSONArray();
			Element[] resWrapperElems = XMLUtils.getElementsAtXPath(resElem,
					"./tran:ResponseBody/tran1:OTA_GroundBookRSWrapper");
			for (Element resWrapperElem : resWrapperElems) {
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(resWrapperElem, "./ns:OTA_GroundBookRS");
				getSupplierResponseGroundWrapperJSON(resWrapperElem, groundWrapperJsonArray);
			}
			resBodyJson.put(JSON_PROP_GROUNDWRAPPER, groundWrapperJsonArray);

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));

	        kafkaMsgJson = new JSONObject(resJson.toString());
	        kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("product", reqBodyJson.getString("product"));
			//kafkaMsgJson.put("messageType", "CONFIRM");
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_BOOKID, reqBodyJson.getString(JSON_PROP_BOOKID));
			kafkaMsgJson.getJSONObject(JSON_PROP_RESHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).put("clientIATANumber", usrCtx.getClientIATANUmber());
			bookProducer.runProducer(1, kafkaMsgJson);
			return resJson.toString();
		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	private static String getRedisKeyForGroundService(JSONObject bookReq) {
		List<String> keys = new ArrayList<>();
		String suppId = bookReq.optString(JSON_PROP_SUPPREF);
		try {
		keys.add(suppId.split("_")[1] != null ? suppId.split("_")[1] : suppId);
		}
		catch (Exception e) {
			keys.add(suppId);
		}
		//suppId.split("_")[1] != null ? suppId.split("_")[1] : suppId
		JSONArray serviceArr = bookReq.getJSONArray(JSON_PROP_SERVICES);
		for(int x = 0;x<serviceArr.length();x++) {
		JSONObject serviceJSON = serviceArr.getJSONObject(x);
		JSONArray vehicleTypeArr = serviceJSON.getJSONArray(JSON_PROP_VEHICLETYPE);
		for(int k = 0 ;k < vehicleTypeArr.length() ; k++) {
			JSONObject vehicleTypeJSON = vehicleTypeArr.getJSONObject(k);
			keys.add(vehicleTypeJSON.optString("maximumPassengers"));
			keys.add(vehicleTypeJSON.optString("description"));
			keys.add(vehicleTypeJSON.optString("uniqueID"));
			
		}
		}
		String key = keys.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining("|"));
		return key;
	}

	private static void getJSONService(Document ownerDoc, Element groundReservationElem, JSONArray serviceArr,
			int serviceArrLen) {
		for (int j = 0; j < serviceArrLen; j++) {

			Element serviceElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Service");
			Element locationElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Location");
			serviceElem.appendChild(locationElem);
			JSONObject pickupJson = serviceArr.getJSONObject(j).getJSONObject("pickup");
			Element pickupElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Pickup");
			pickupElem.setAttribute("LocationCode", pickupJson.getString("locationCode"));
			pickupElem.setAttribute("LocationType", pickupJson.getString("locationType"));
			pickupElem.setAttribute("DateTime", pickupJson.getString("dateTime"));
			locationElem.appendChild(pickupElem);
			JSONObject dropoffJson = serviceArr.getJSONObject(j).getJSONObject("dropoff");
			Element dropoffElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Dropoff");
			dropoffElem.setAttribute("LocationCode", dropoffJson.getString("locationCode"));
			dropoffElem.setAttribute("LocationType", dropoffJson.getString("locationType"));
			dropoffElem.setAttribute("DateTime", dropoffJson.getString("dateTime"));
			locationElem.appendChild(dropoffElem);
			groundReservationElem.appendChild(serviceElem);

		}
	}

	private static void getJSONReference(Document ownerDoc, Element groundReservationElem, JSONArray referencesArr,
			int referencesArrLen, Element groundAvailElem) {
		for (int m = 0; m < referencesArrLen; m++) {

			Element reference = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Reference");
			// Element reference = (Element) referenceElem.cloneNode(true);
			reference.setAttribute("ID", referencesArr.getJSONObject(m).getString("id"));
			reference.setAttribute("ID_Context", referencesArr.getJSONObject(m).getString("id_Context"));//
			reference.setAttribute("Type", referencesArr.getJSONObject(m).getString("type"));

			groundAvailElem.insertBefore(reference, groundReservationElem);
		}
		
	}

	private static void getJSONPaxDetails(Document ownerDoc, Element groundReservationElem, JSONArray passengerArr,
			int passengerArrLen) {
		for (int p = 0; p < passengerArrLen; p++) {

			// Passenger
			Element passengerElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Passenger");
			groundReservationElem.appendChild(passengerElem);
			Element primaryElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Primary");
			passengerElem.appendChild(primaryElem);
			JSONObject primaryJson = passengerArr.getJSONObject(p).getJSONObject("primary");
			Element personNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:PersonName");
			primaryElem.appendChild(personNameElem);
			Element givenNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:GivenName");
			givenNameElem.setTextContent(primaryJson.getJSONObject("personName").getString("givenName"));
			personNameElem.appendChild(givenNameElem);
			Element surNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Surname");
			surNameElem.setTextContent(primaryJson.getJSONObject("personName").getString("surname"));
			personNameElem.appendChild(surNameElem);
			Element telephoneElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Telephone");
			telephoneElem.setAttribute("PhoneNumber", primaryJson.getString("phoneNumber"));
			primaryElem.appendChild(telephoneElem);
			Element emailElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Email");
			emailElem.setTextContent(primaryJson.getString("email"));
			primaryElem.appendChild(emailElem);

			JSONObject additionalJson = passengerArr.getJSONObject(p).getJSONObject("additional");
			// Additional
			Element additionalElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Additional");
			additionalElem.setAttribute("PaxType", additionalJson.getString("paxType"));
			additionalElem.setAttribute("Age", additionalJson.getString("age"));
			personNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:PersonName");
			additionalElem.appendChild(personNameElem);
			givenNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:GivenName");
			givenNameElem.setTextContent(additionalJson.getJSONObject("personName").getString("givenName"));
			personNameElem.appendChild(givenNameElem);
			surNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Surname");
			surNameElem.setTextContent(additionalJson.getJSONObject("personName").getString("surname"));
			personNameElem.appendChild(surNameElem);
			telephoneElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Telephone");
			telephoneElem.setAttribute("PhoneNumber", additionalJson.getString("phoneNumber"));
			additionalElem.appendChild(telephoneElem);
			emailElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Email");
			emailElem.setTextContent(additionalJson.getString("email"));
			additionalElem.appendChild(emailElem);
			passengerElem.appendChild(additionalElem);

		}
		
	}
	private static Element createRentalPaymentPrefElement(Document ownerDoc, JSONObject vehicleAvailJson, JSONObject suppTotalFare) {
		 
//		 String temp;
		 JSONObject rentalPaymentPrefJson = vehicleAvailJson.getJSONObject("rentalPaymentPref");
        Element rentalPaymentPrefElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:RentalPaymentPref");
        rentalPaymentPrefElem.setAttribute("PaymentType", rentalPaymentPrefJson.optString("paymentType"));
        rentalPaymentPrefElem.setAttribute("Type", rentalPaymentPrefJson.optString("type"));
        
       /* Element paymentCardElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:PaymentCard"); 
        if(!(temp = rentalPaymentPrefJson.optString("PaymentCardCode")).isEmpty())
       	 paymentCardElem.setAttribute("CardCode", temp);
        if(!(temp = rentalPaymentPrefJson.optString("PaymentCardExpiryDate")).isEmpty())
       	 paymentCardElem.setAttribute("ExpireDate", temp);
        
        Element cardType = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CardType");
        cardType.setTextContent(rentalPaymentPrefJson.optString("CardType"));
        paymentCardElem.appendChild(cardType);
        Element cardHolderName = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CardHolderName");
        cardHolderName.setTextContent(rentalPaymentPrefJson.optString("CardHolderName"));
        paymentCardElem.appendChild(cardHolderName);
        Element cardNumber = ownerDoc.createElementNS(Constants.NS_OTA, "ota:CardNumber");
        cardNumber.setTextContent(rentalPaymentPrefJson.optString("CardNumber"));
        paymentCardElem.appendChild(cardNumber);*/
        
        // Payment Amount taken from Redis which was saved in price operation 
        Element paymentAmountElem = ownerDoc.createElementNS(Constants.NS_OTA, "ota:PaymentAmount");
		 paymentAmountElem.setAttribute("Amount",  BigDecimaltoString(rentalPaymentPrefJson,JSON_PROP_AMOUNT));
        paymentAmountElem.setAttribute("CurrencyCode", rentalPaymentPrefJson.getString(JSON_PROP_CURRENCYCODE));
        
       /* Element paymentAmountElem =ownerDoc.createElementNS(Constants.NS_OTA, "ota:PaymentAmount");
        if(!(temp = rentalPaymentPrefJson.optString("Amount")).isEmpty())
        paymentAmountElem.setAttribute("Amount", temp);
        if(!(temp = rentalPaymentPrefJson.optString("CurrencyCode")).isEmpty())
        paymentAmountElem.setAttribute("CurrencyCode", temp);*/
        
//      rentalPaymentPrefElem.appendChild(paymentCardElem);
        rentalPaymentPrefElem.appendChild(paymentAmountElem);
		return rentalPaymentPrefElem;
	}
	
	public static String BigDecimaltoString(JSONObject json, String prop) {
		
		if(json==null)
			return "";
		try {
			if(json.getBigDecimal(prop).compareTo(new BigDecimal(0)) == 0)
				return "";
			else
				return json.getBigDecimal(prop).toString();
		}
		catch(JSONException e) {
			return "";
		}
	}
	
	public static String NumbertoString(JSONObject json, String prop) {
		
		if(json==null)
			return "";
		try {
			if(json.getNumber(prop).equals(0))
				return "";
			else
				return json.getNumber(prop).toString();
		}
		catch(JSONException e) {
			return "";
		}
	}
}
