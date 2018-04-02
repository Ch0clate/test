package com.coxandkings.travel.bookingengine.orchestrator.acco;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.enums.AccoSubType;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo.CommercialsEntityType;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

@Service
public class AccoBookProcessor  implements AccoConstants {

	private static final Logger logger = LogManager.getLogger(AccoBookProcessor.class);
	static final String OPERATION_NAME = "book";

	public static void setSupplierRequestElem(JSONObject reqJson,Element reqElem) throws Exception {

		Document ownerDoc = reqElem.getOwnerDocument();
		Element blankWrapperElem = XMLUtils.getFirstElementAtXPath(reqElem, "./accoi:RequestBody/acco:OTA_HotelResRQWrapper");
		XMLUtils.removeNode(blankWrapperElem);
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
		JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
		Map<String, String> reprcSuppFaresMap = null;

		String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
		String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
		String userID = reqHdrJson.getString(JSON_PROP_USERID);

		try(Jedis redisConn = RedisConfig.getRedisConnectionFromPool()){
			String redisKey = String.format("%s%c%s", sessionID,KEYSEPARATOR,PRODUCT_ACCO);
			reprcSuppFaresMap = redisConn.hgetAll(redisKey);
			if (reprcSuppFaresMap == null || reprcSuppFaresMap.isEmpty()) {
				throw new Exception(String.format("Reprice context not found for %s", redisKey));
			}
		}

		AccoSearchProcessor.createSuppReqHdrElem(XMLUtils.getFirstElementAtXPath(reqElem, "./acco:RequestHeader"),sessionID,transactionID,userID);
		UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
		String suppID;
		ProductSupplier prodSupplier;
		JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		JSONObject roomObjectJson;
		Element wrapperElement,suppCredsListElem= XMLUtils.getFirstElementAtXPath(reqElem, "./acco:RequestHeader/com:SupplierCredentialsList");
		AccoSubType prodCategSubtype;
		for (int j=0; j < multiReqArr.length(); j++) {

			roomObjectJson =   multiReqArr.getJSONObject(j);
			suppID = roomObjectJson.getString(JSON_PROP_SUPPREF);
			prodCategSubtype = AccoSubType.forString(roomObjectJson.optString(JSON_PROP_ACCOSUBTYPEARR));
			prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_ACCO,prodCategSubtype!=null?prodCategSubtype.toString():DEF_PRODSUBTYPE, suppID);

			if (prodSupplier == null) {
				throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
			}

			suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc,j));
			wrapperElement= (Element) blankWrapperElem.cloneNode(true);

			XMLUtils.setValueAtXPath(wrapperElement, "./acco:SupplierID",suppID);
			XMLUtils.setValueAtXPath(wrapperElement, "./acco:Sequence", String.valueOf(j));
			setSuppReqOTAElem(ownerDoc, XMLUtils.getFirstElementAtXPath(wrapperElement,"./ota:OTA_HotelResRQ"), roomObjectJson, reprcSuppFaresMap);

			XMLUtils.insertChildNode(reqElem, "./accoi:RequestBody", wrapperElement, false);
		}
	}

	public static void setSuppReqOTAElem(Document ownerDoc,Element reqOTAElem,JSONObject roomObjectJson, Map<String, String> reprcSuppFaresMap) {

		Element hotelResElem = XMLUtils.getFirstElementAtXPath(reqOTAElem, "./ota:HotelReservations/ota:HotelReservation");
		Element roomStaysElem = (Element) XMLUtils.getFirstElementAtXPath(hotelResElem, "./ota:RoomStays");
		Element resGuestsElem =  XMLUtils.getFirstElementAtXPath(hotelResElem, "./ota:ResGuests");
		Element resGlobalInfoElem = XMLUtils.getFirstElementAtXPath(hotelResElem, "./ota:ResGlobalInfo");

		String cityCode = roomObjectJson.getString(JSON_PROP_CITYCODE);
		String countryCode = roomObjectJson.getString(JSON_PROP_COUNTRYCODE);
		String chkIn = roomObjectJson.getString(JSON_PROP_CHKIN);
		String chkOut = roomObjectJson.getString(JSON_PROP_CHKOUT);
		String redisReqKey = AccoRepriceProcessor.getRedisKeyForReq(roomObjectJson);
		String suppCcyCode="",roomKey="";
		JSONArray paxArr;
		JSONObject roomInfoJson,totalPriceInfoJson,price_commInfojson;
		Element roomStayElem,guestCountsElem,bookingTotalElem,bookingTaxElem,totalElem;
		int rph=0;
		BigDecimal bookingTotalPrice = new BigDecimal(0);
		BigDecimal bookingTaxPrice = new BigDecimal(0);
		BigDecimal roomTotalPrice,roomTaxPrice;

		for(Object roomConfig:roomObjectJson.getJSONArray(JSON_PROP_REQUESTEDROOMARR)) {
			roomStayElem = ownerDoc.createElementNS(NS_OTA, "ota:RoomStay");
			guestCountsElem =  ownerDoc.createElementNS(NS_OTA, "ota:GuestCounts");

			paxArr = ((JSONObject) roomConfig).getJSONArray(JSON_PROP_PAXINFOARR);
			roomInfoJson = ((JSONObject) roomConfig).getJSONObject(JSON_PROP_ROOMINFO);
			roomKey = redisReqKey.concat(AccoRepriceProcessor.getRedisKeyForRoomStay(((JSONObject) roomConfig).getJSONObject(JSON_PROP_ROOMINFO)));
			price_commInfojson = new JSONObject(reprcSuppFaresMap.get(roomKey));
			totalPriceInfoJson = price_commInfojson.getJSONObject(JSON_PROP_SUPPROOMPRICE);
			((JSONObject) roomConfig).put(JSON_PROP_SUPPROOMPRICE,totalPriceInfoJson);
			((JSONObject) roomConfig).put(JSON_PROP_ROOMPRICE,price_commInfojson.getJSONObject(JSON_PROP_ROOMPRICE));
			((JSONObject) roomConfig).put(JSON_PROP_SUPPCOMM,price_commInfojson.optJSONArray(JSON_PROP_SUPPCOMM));
			((JSONObject) roomConfig).put(JSON_PROP_CLIENTCOMMENTITYDTLS,price_commInfojson.optJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS));

			roomStayElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:RoomTypes")).appendChild(getRoomTypeElem(ownerDoc, roomInfoJson));
			roomStayElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:RatePlans")).appendChild(getRateElem(ownerDoc, roomInfoJson));
			for(int k=0;k<paxArr.length();k++) {
				addGuestDetails(ownerDoc, (JSONObject) paxArr.get(k), resGuestsElem, guestCountsElem, rph++);
			}
			roomStayElem.appendChild(guestCountsElem);
			roomStayElem.appendChild(getTimeSpanElem(ownerDoc, chkIn, chkOut));
			//roomStayElem.appendChild(getTotalElem(ownerDoc, totalPriceInfoJson));
			totalElem = ownerDoc.createElementNS(NS_OTA, "ota:Total");

			totalElem.setAttribute("CurrencyCode", totalPriceInfoJson.getString(JSON_PROP_CCYCODE));
			roomTotalPrice = totalPriceInfoJson.getBigDecimal(JSON_PROP_AMOUNT);
			totalElem.setAttribute("AmountAfterTax", String.valueOf(roomTotalPrice));
			bookingTotalPrice = bookingTotalPrice.add(roomTotalPrice);
			JSONObject taxesJson = totalPriceInfoJson.optJSONObject(JSON_PROP_TOTALTAX);
			if(taxesJson!=null && taxesJson.has(JSON_PROP_AMOUNT)) {
				Element taxesElem = (Element) totalElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Taxes"));
				taxesElem.setAttribute("CurrencyCode", taxesJson.getString(JSON_PROP_CCYCODE));
				roomTaxPrice = totalPriceInfoJson.getBigDecimal(JSON_PROP_AMOUNT);
				taxesElem.setAttribute("Amount", String.valueOf(roomTaxPrice));
				bookingTaxPrice = bookingTaxPrice.add(roomTaxPrice);
			}


			roomStayElem.appendChild(getHotelElem(ownerDoc, roomInfoJson, cityCode, countryCode));
			for(Object reference:roomInfoJson.getJSONArray(JSON_PROP_REFERENCESARR)) {
				roomStayElem.appendChild(getReferenceElem(ownerDoc, (JSONObject) reference));
			}
			roomStayElem.setAttribute("RPH", String.valueOf(roomInfoJson.getInt(JSON_PROP_ROOMINDEX)));
			roomStayElem.setAttribute("RoomStayStatus", (roomInfoJson.getString(JSON_PROP_AVAILSTATUS)));

			roomStaysElem.appendChild(roomStayElem);
		}

		bookingTotalElem = XMLUtils.getFirstElementAtXPath(resGlobalInfoElem, "./ota:Total");
		bookingTaxElem = XMLUtils.getFirstElementAtXPath(bookingTotalElem, "./ota:Taxes");
		bookingTotalElem.setAttribute("AmountAfterTax", String.valueOf(bookingTotalPrice));
		bookingTotalElem.setAttribute("CurrencyCode", suppCcyCode);
		bookingTaxElem.setAttribute("Amount",  String.valueOf(bookingTaxPrice));
		bookingTaxElem.setAttribute("CurrencyCode", suppCcyCode);
		//TODO:payment info is hard coded in shell. It will be cnk payment details.Where will it come from?
	}

	public static Element getRoomTypeElem(Document ownerDoc, JSONObject roomInfoJson) {

		Element roomTypeElem = ownerDoc.createElementNS(NS_OTA, "ota:RoomType");
		Element tpaElem = ownerDoc.createElementNS(NS_OTA,"ota:TPA_Extensions");
		JSONObject roomTypeInfo = roomInfoJson.getJSONObject(JSON_PROP_ROOMTYPEINFO);

		roomTypeElem.setAttribute("RoomTypeCode",roomTypeInfo.getString(JSON_PROP_ROOMTYPECODE));
		roomTypeElem.setAttribute("RoomType",roomTypeInfo.getString(JSON_PROP_ROOMTYPENAME));
		roomTypeElem.setAttribute("RoomID",roomTypeInfo.getString(JSON_PROP_ROOMREF));
		tpaElem.appendChild(ownerDoc.createElementNS(NS_ACCO,"acco:RoomCategoryID")).setTextContent(roomTypeInfo.getString(JSON_PROP_ROOMCATEGCODE));
		tpaElem.appendChild(ownerDoc.createElementNS(NS_ACCO,"acco:RoomCategoryName")).setTextContent(roomTypeInfo.getString(JSON_PROP_ROOMCATEGNAME));
		roomTypeElem.setAttribute("RoomID",roomTypeInfo.getString(JSON_PROP_ROOMREF));
		roomTypeElem.appendChild(tpaElem);

		return roomTypeElem;
	}

	public static Element getRateElem(Document ownerDoc, JSONObject roomInfoJson) {

		Element ratePlanElem = ownerDoc.createElementNS(NS_OTA, "ota:RatePlan");
		JSONObject ratePlanInfo = roomInfoJson.getJSONObject(JSON_PROP_RATEPLANINFO);
		Element tpaElem = ownerDoc.createElementNS(NS_OTA,"ota:TPA_Extensions");

		ratePlanElem.setAttribute("RatePlanCode",ratePlanInfo.getString(JSON_PROP_RATEPLANCODE));
		ratePlanElem.setAttribute("RatePlanName",ratePlanInfo.getString(JSON_PROP_RATEPLANNAME));
		ratePlanElem.setAttribute("BookingCode",ratePlanInfo.getString(JSON_PROP_RATEBOOKINGREF));
		ratePlanElem.setAttribute("RatePlanID",ratePlanInfo.getString(JSON_PROP_RATEPLANREF));
		Element mealsElem = (Element) tpaElem.appendChild(ownerDoc.createElementNS(NS_ACCO,"acco:Meals"));
		Element mealElem = (Element) mealsElem.appendChild(ownerDoc.createElementNS(NS_ACCO,"acco:Meal"));
		mealElem.setAttribute("MealId", roomInfoJson.getJSONObject(JSON_PROP_MEALINFO).getString(JSON_PROP_MEALCODE));
		ratePlanElem.appendChild(tpaElem);

		return ratePlanElem;
	}

	public static Element getHotelElem(Document ownerDoc, JSONObject roomInfoJson,String cityCode,String countryCode) {

		Element hotelElem = ownerDoc.createElementNS(NS_OTA, "ota:BasicPropertyInfo");
		JSONObject hotelInfo = roomInfoJson.getJSONObject(JSON_PROP_HOTELINFO);

		hotelElem.setAttribute("HotelCode",hotelInfo.getString(JSON_PROP_HOTELCODE));
		hotelElem.setAttribute("HotelCodeContext",hotelInfo.getString(JSON_PROP_HOTELREF));
		hotelElem.setAttribute("HotelCityCode",cityCode);
		((Element) hotelElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Address"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CountryName")))
		.setAttribute("Code",countryCode);

		return hotelElem;
	}

	public static Element getReferenceElem(Document ownerDoc, JSONObject referenceJson) {

		Element referenceElem = ownerDoc.createElementNS(NS_OTA, "ota:Reference");

		referenceElem.setAttribute("ID",referenceJson.getString(JSON_PROP_REFVALUE));
		referenceElem.setAttribute("ID_Context", referenceJson.getString(JSON_PROP_REFNAME));
		referenceElem.setAttribute("Type",referenceJson.getString(JSON_PROP_REFCODE));

		return referenceElem;
	}

	public static Element getTimeSpanElem(Document ownerDoc, String chkIn,String chkOut) {

		Element timeSpanElem = ownerDoc.createElementNS(NS_OTA, "ota:TimeSpan");

		timeSpanElem.setAttribute("Start",chkIn);
		timeSpanElem.setAttribute("End",chkOut);

		return timeSpanElem;
	}

	public static Element getTotalElem(Document ownerDoc,JSONObject priceJson) {

		Element totalElem = ownerDoc.createElementNS(NS_OTA, "ota:Total");

		totalElem.setAttribute("CurrencyCode", priceJson.getString(JSON_PROP_CCYCODE));
		totalElem.setAttribute("AmountAfterTax", String.valueOf(priceJson.getBigDecimal(JSON_PROP_AMOUNT)));
		//bookingTotalPrice = bookingTotalPrice.add(roomTotalPrice);
		JSONObject taxesJson = priceJson.optJSONObject(JSON_PROP_TOTALTAX);
		if(taxesJson!=null && taxesJson.has(JSON_PROP_AMOUNT)) {
			Element taxesElem = (Element) totalElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Taxes"));
			taxesElem.setAttribute("CurrencyCode", taxesJson.getString(JSON_PROP_CCYCODE));
			taxesElem.setAttribute("Amount", String.valueOf(taxesJson.getBigDecimal(JSON_PROP_AMOUNT)));
			//bookingTaxPrice = bookingTaxPrice.add(roomTaxPrice);
		}

		return totalElem;

	}

	public static Element getResGuestElement(Document ownerDoc,JSONObject paxInfo,String rph) {
		
		JSONObject contactDetails = (JSONObject) paxInfo.getJSONArray(JSON_PROP_CONTACTDTLS).getJSONObject(0).getJSONObject(JSON_PROP_CONTACTINFO);
		JSONObject addressDetails = (JSONObject) paxInfo.get(JSON_PROP_ADDRDTLS);

		Element resGuest = ownerDoc.createElementNS(NS_OTA, "ota:ResGuest");

		resGuest.setAttribute("ResGuestRPH",rph!=null?rph:"");
		resGuest.setAttribute("AgeQualifyingCode",Pax_CHD.equals(paxInfo.get(JSON_PROP_PAXTYPE))?Pax_CHD_ID:Pax_ADT_ID);
		resGuest.setAttribute("PrimaryIndicator",paxInfo.optBoolean(JSON_PROP_LEADPAX_IND,false)?"true":"false");
		resGuest.setAttribute("Age", Integer.toString(calculateAge(paxInfo.getString(JSON_PROP_DATEOFBIRTH))));

		Element customerElem = (Element) resGuest.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Profiles"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:ProfileInfo"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Profile"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Customer"));
		customerElem.setAttribute("BirthDate",paxInfo.getString(JSON_PROP_DATEOFBIRTH));

		Element personNameElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:PersonName"));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:NamePrefix")).setTextContent(paxInfo.getString(JSON_PROP_TITLE));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:GivenName")).setTextContent(paxInfo.getString(JSON_PROP_FIRSTNAME));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:MiddleName")).setTextContent(paxInfo.getString(JSON_PROP_MIDDLENAME));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Surname")).setTextContent(paxInfo.getString(JSON_PROP_SURNAME));
		//TODO:citizenCountryName and code to be added.Is this same as address countryCodes and names?
		Element contactElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Telephone"));
		contactElem.setAttribute("CountryAccessCode",contactDetails.getString(JSON_PROP_COUNTRYCODE));
		contactElem.setAttribute("PhoneNumber",contactDetails.getString(JSON_PROP_MOBILENBR));

		customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Email")).setTextContent(contactDetails.getString(JSON_PROP_EMAIL));

		Element addressElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Address"));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:AddressLine")).setTextContent(paxInfo.getJSONObject(JSON_PROP_ADDRDTLS).getString(JSON_PROP_ADDRLINE2));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:AddressLine")).setTextContent(paxInfo.getJSONObject(JSON_PROP_ADDRDTLS).getString(JSON_PROP_ADDRLINE1));
        addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CityName")).setTextContent(addressDetails.getString(JSON_PROP_CITY));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:PostalCode")).setTextContent(addressDetails.getString(JSON_PROP_ZIP));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CountryName")).setTextContent(addressDetails.getString(JSON_PROP_COUNTRY));
		//TODO:state and country code need to be added.Will wem provide codes?
		//Element citizenCountryNameElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CitizenCountryName"));
		//citizenCountryNameElem.setAttribute("Code",reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTNATIONALITY));
		return resGuest;

	}

	public static void addGuestDetails(Document ownerDoc, JSONObject paxInfoJson, Element resGuestsElem,Element guestCountsElem, int rph) {

		String birthdate = paxInfoJson.getString(JSON_PROP_DATEOFBIRTH);
		Element guestElem = AccoSearchProcessor.getGuestCountElem(ownerDoc, 1, AccoBookProcessor.calculateAge(birthdate), paxInfoJson.getString(JSON_PROP_PAXTYPE));
		guestElem.setAttribute("ResGuestRPH",String.valueOf(rph));
		guestCountsElem.appendChild(guestElem);
		resGuestsElem.appendChild(getResGuestElement(ownerDoc, paxInfoJson, String.valueOf(rph)));
	}

	/*public Element getResGuestElement(Document ownerDoc,JSONObject paxInfo,String rph,JSONObject reqHdrJson) {
		JSONObject personalDetails = (JSONObject) paxInfo.get(JSON_PROP_PERSONALDETAILS);
		JSONObject contactDetails = (JSONObject) paxInfo.get(JSON_PROP_CONTACTDETAILS);
		JSONObject addressDetails = (JSONObject) paxInfo.get(JSON_PROP_ADDRESS);

		Element resGuest = ownerDoc.createElementNS(NS_OTA, "ota:ResGuest");

		resGuest.setAttribute("ResGuestRPH",rph!=null?rph:"");
		resGuest.setAttribute("AgeQualifyingCode",Pax_CHD.equals(paxInfo.get("paxType"))?Pax_CHD_ID:Pax_ADT_ID);
		resGuest.setAttribute("PrimaryIndicator",paxInfo.optBoolean("isLeadPax",false)?"true":"false");
		resGuest.setAttribute("Age", Integer.toString(calculateAge(personalDetails.getString(JSON_PROP_BIRTHDATE))));

		Element customerElem = (Element) resGuest.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Profiles"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:ProfileInfo"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Profile"))
				.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Customer"));
		customerElem.setAttribute("BirthDate",personalDetails.getString(JSON_PROP_BIRTHDATE));

		Element personNameElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:PersonName"));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:NamePrefix")).setTextContent(personalDetails.getString("title"));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:GivenName")).setTextContent(personalDetails.getString("firstName"));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:MiddleName")).setTextContent(personalDetails.getString("middleName"));
		personNameElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Surname")).setTextContent(personalDetails.getString("lastName"));
		//TODO:citizenCountryName and code to be added.Is this same as address countryCodes and names?
		Element contactElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Telephone"));
		contactElem.setAttribute("CountryAccessCode",contactDetails.getString(JSON_PROP_CTRYACESCODE));
		contactElem.setAttribute("PhoneNumber",contactDetails.getString(JSON_PROP_CONTCTNUMBR));

		customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Email")).setTextContent(contactDetails.getString(JSON_PROP_EMAIL));

		Element addressElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:Address"));

		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:AddressLine")).setTextContent(paxInfo.getJSONObject(JSON_PROP_ADDRDTLS).getString(JSON_PROP_ADDRLINE2));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:AddressLine")).setTextContent(paxInfo.getJSONObject(JSON_PROP_ADDRDTLS).getString(JSON_PROP_ADDRLINE1));
        addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CityName")).setTextContent(addressDetails.getString(JSON_PROP_CITY));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:PostalCode")).setTextContent(addressDetails.getString(JSON_PROP_ZIP));
		addressElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CountryName")).setTextContent(addressDetails.getString(JSON_PROP_COUNTRY));
		//TODO:state and country code need to be added.Will wem provide codes?
		Element citizenCountryNameElem = (Element) customerElem.appendChild(ownerDoc.createElementNS(NS_OTA, "ota:CitizenCountryName"));
		citizenCountryNameElem.setAttribute("Code",reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTNATIONALITY));
		return resGuest;

	}*/

	@Deprecated
	public static String process(JSONObject reqJson) {
		try{


			OperationConfig opConfig = AccoConfig.getOperationConfig(OPERATION_NAME);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			Element blankWrapperElem = XMLUtils.getFirstElementAtXPath(reqElem, "./accoi:RequestBody/acco:OTA_HotelResRQWrapper");
			XMLUtils.removeNode(blankWrapperElem);

			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			String suppID;
			ProductSupplier prodSupplier;

			XMLUtils.setValueAtXPath(reqElem, "./acco:RequestHeader/com:SessionID",sessionID);
			XMLUtils.setValueAtXPath(reqElem, "./acco:RequestHeader/com:TransactionID",transactionID);
			XMLUtils.setValueAtXPath(reqElem, "./acco:RequestHeader/com:UserID",userID);

			JSONArray multiReqArr = reqBodyJson.getJSONArray(JSON_PROP_ACCOMODATIONARR);
			//required to be added for kafka req json
			reqBodyJson.put(JSON_PROP_TYPE, "request");
			reqBodyJson.put(JSON_PROP_PROD, PRODUCT_ACCO);
			AccoSubType prodCategSubtype;

			for (int j=0; j < multiReqArr.length(); j++) {

				JSONObject roomObjectJson =   multiReqArr.getJSONObject(j);
				suppID = roomObjectJson.getString(JSON_PROP_SUPPREF);
				prodCategSubtype = AccoSubType.forString(roomObjectJson.optString(JSON_PROP_ACCOSUBTYPEARR));
				prodSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_ACCO,prodCategSubtype!=null?prodCategSubtype.toString():DEF_PRODSUBTYPE, suppID);
				if (prodSupplier == null) {
					throw new Exception(String.format("Product supplier %s not found for user/client", suppID));
				}
				XMLUtils.insertChildNode(reqElem, "./acco:RequestHeader/com:SupplierCredentialsList", prodSupplier.toElement(ownerDoc,j), false);


				Element wrapperElement= (Element) blankWrapperElem.cloneNode(true);
				Document wrapperOwner  =	wrapperElement.getOwnerDocument();

				XMLUtils.setValueAtXPath(wrapperElement, "./acco:SupplierID",suppID);
				XMLUtils.setValueAtXPath(wrapperElement, "./acco:Sequence", String.valueOf(j));

				Element hotelResElem = XMLUtils.getFirstElementAtXPath(wrapperElement, "./ota:OTA_HotelResRQ/ota:HotelReservations/ota:HotelReservation");

				Element roomStaysElem = XMLUtils.getFirstElementAtXPath(hotelResElem, "./ota:RoomStays");
				Element resGuestsElem =  XMLUtils.getFirstElementAtXPath(hotelResElem, "./ota:ResGuests");
				Element resGlobalInfoElem = XMLUtils.getFirstElementAtXPath(hotelResElem, "./ota:ResGlobalInfo");

				Element roomStayElem,roomTypeElem,ratePlanElem,guestCountsElem,guestElem,hotelElem,referenceElem;
				Element tpaElem,totalElem,taxesElem,timeSpanElem,mealsElem,mealElem;
				JSONArray paxArr;
				JSONObject paxInfo,roomInfo,roomTypeInfo,ratePlanInfo,hotelInfo,totalPriceInfo,taxes;
				BigDecimal bookingTotalPrice = new BigDecimal(0);
				BigDecimal bookingTaxPrice = new BigDecimal(0);
				BigDecimal roomTotalPrice,roomTaxPrice;
				String suppCcyCode="",roomKey="";

				String chkIn=roomObjectJson.getString(JSON_PROP_CHKIN);
				String chkOut=roomObjectJson.getString(JSON_PROP_CHKOUT);

				Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
				String redisKey = String.format("%s%c%s", sessionID,KEYSEPARATOR,PRODUCT_ACCO);
				Map<String, String> reprcSuppFaresMap = redisConn.hgetAll(redisKey);
				if (reprcSuppFaresMap == null) {
					throw new Exception(String.format("Reprice context not found,for %s", redisKey));
				}
				String redisReqKey = AccoRepriceProcessor.getRedisKeyForReq(roomObjectJson);
				int rph=0;
				for(Object roomConfig:roomObjectJson.getJSONArray(JSON_PROP_REQUESTEDROOMARR)) {

					roomStayElem =  wrapperOwner.createElementNS(NS_OTA, "ota:RoomStay");

					paxArr = ((JSONObject) roomConfig).getJSONArray("paxInfo");
					roomInfo = ((JSONObject) roomConfig).getJSONObject("roomInfo");
					//TODO:might not be needed in future as it will come from cache
					//totalPriceInfo = ((JSONObject) roomConfig).getJSONObject("totalPriceInfo");
					roomKey = redisReqKey.concat(AccoRepriceProcessor.getRedisKeyForRoomStay(((JSONObject) roomConfig).getJSONObject(JSON_PROP_ROOMINFO)));

					JSONObject suppPriceBookInfo = new JSONObject(reprcSuppFaresMap.get(roomKey));
					((JSONObject) roomConfig).put(JSON_PROP_SUPPROOMPRICE,suppPriceBookInfo.getJSONObject(JSON_PROP_SUPPROOMPRICE));
					((JSONObject) roomConfig).put(JSON_PROP_ROOMPRICE,suppPriceBookInfo.getJSONObject(JSON_PROP_ROOMPRICE));
					((JSONObject) roomConfig).put(JSON_PROP_SUPPCOMM,suppPriceBookInfo.getJSONArray(JSON_PROP_SUPPCOMM));
					((JSONObject) roomConfig).put(JSON_PROP_CLIENTCOMM,suppPriceBookInfo.getJSONArray(JSON_PROP_CLIENTCOMM));


					totalPriceInfo = (new JSONObject(reprcSuppFaresMap.get(roomKey))).getJSONObject("supplierTotalPriceInfo");

					//TODO:check for optional values hereafter
					roomTypeElem = wrapperOwner.createElementNS(NS_OTA, "ota:RoomType");
					roomTypeInfo = roomInfo.getJSONObject("roomTypeInfo");
					roomTypeElem.setAttribute("RoomTypeCode",roomTypeInfo.getString(JSON_PROP_ROOMTYPECODE));
					roomTypeElem.setAttribute("RoomType",roomTypeInfo.getString(JSON_PROP_ROOMTYPENAME));
					roomTypeElem.setAttribute("RoomID",roomTypeInfo.getString(JSON_PROP_ROOMREF));
					tpaElem = wrapperOwner.createElementNS(NS_OTA,"ota:TPA_Extensions");
					tpaElem.appendChild(wrapperOwner.createElementNS(NS_ACCO,"acco:RoomCategoryID")).setTextContent(roomTypeInfo.getString(JSON_PROP_ROOMCATEGCODE));
					tpaElem.appendChild(wrapperOwner.createElementNS(NS_ACCO,"acco:RoomCategoryName")).setTextContent(roomTypeInfo.getString(JSON_PROP_ROOMCATEGNAME));
					roomTypeElem.setAttribute("RoomID",roomTypeInfo.getString(JSON_PROP_ROOMREF));
					roomTypeElem.appendChild(tpaElem);

					ratePlanElem = wrapperOwner.createElementNS(NS_OTA, "ota:RatePlan");
					ratePlanInfo = roomInfo.getJSONObject(JSON_PROP_RATEPLANINFO);
					tpaElem = wrapperOwner.createElementNS(NS_OTA,"ota:TPA_Extensions");
					ratePlanElem.setAttribute("RatePlanCode",ratePlanInfo.getString(JSON_PROP_RATEPLANCODE));
					ratePlanElem.setAttribute("RatePlanName",ratePlanInfo.getString(JSON_PROP_RATEPLANNAME));
					ratePlanElem.setAttribute("BookingCode",ratePlanInfo.getString(JSON_PROP_RATEBOOKINGREF));
					ratePlanElem.setAttribute("RatePlanID",ratePlanInfo.getString(JSON_PROP_RATEPLANREF));
					mealsElem = (Element) tpaElem.appendChild(wrapperOwner.createElementNS(NS_ACCO,"acco:Meals"));
					mealElem = (Element) mealsElem.appendChild(wrapperOwner.createElementNS(NS_ACCO,"acco:Meal"));
					mealElem.setAttribute("MealId", roomInfo.getJSONObject(JSON_PROP_MEALINFO).getString(JSON_PROP_MEALCODE));
					ratePlanElem.appendChild(tpaElem);

					timeSpanElem = wrapperOwner.createElementNS(NS_OTA, "ota:TimeSpan");
					timeSpanElem.setAttribute("Start",chkIn);
					timeSpanElem.setAttribute("End",chkOut);

					guestCountsElem = wrapperOwner.createElementNS(NS_OTA, "ota:GuestCounts");
					for(int k=0;k<paxArr.length();k++) {
						//TODO:calc age from birth date if possible
						paxInfo = (JSONObject) paxArr.get(k);
						String birthdate = paxInfo.getString(JSON_PROP_DATEOFBIRTH);
						guestElem = AccoSearchProcessor.getGuestCountElem(wrapperOwner, 1, calculateAge(birthdate), paxInfo.getString(JSON_PROP_PAXTYPE));
						guestElem.setAttribute("ResGuestRPH",String.valueOf(rph));
						guestCountsElem.appendChild(guestElem);
						//resGuestsElem.appendChild(getResGuestElement(wrapperOwner, paxInfo, String.valueOf(rph++),reqHdrJson));
						resGuestsElem.appendChild(getResGuestElement(wrapperOwner, paxInfo, String.valueOf(rph++)));
					}

					//TODO:This should be calculated from prices which are kept in cache during reprice response
					totalElem = wrapperOwner.createElementNS(NS_OTA, "ota:Total");
					suppCcyCode = totalPriceInfo.getString(JSON_PROP_CCYCODE);
					totalElem.setAttribute("CurrencyCode", suppCcyCode);
					roomTotalPrice = totalPriceInfo.getBigDecimal(JSON_PROP_AMOUNT);
					totalElem.setAttribute("AmountAfterTax", String.valueOf(roomTotalPrice));
					bookingTotalPrice = bookingTotalPrice.add(roomTotalPrice);
					taxes = totalPriceInfo.optJSONObject(JSON_PROP_TOTALTAX);
					if(taxes!=null && taxes.has(JSON_PROP_AMOUNT)) {
						taxesElem = (Element) totalElem.appendChild(wrapperOwner.createElementNS(NS_OTA, "ota:Taxes"));
						taxesElem.setAttribute("CurrencyCode", taxes.getString(JSON_PROP_CCYCODE));
						roomTaxPrice = taxes.getBigDecimal(JSON_PROP_AMOUNT);
						taxesElem.setAttribute("Amount", String.valueOf(roomTaxPrice));
						bookingTaxPrice = bookingTaxPrice.add(roomTaxPrice);
					}

					hotelElem = wrapperOwner.createElementNS(NS_OTA, "ota:BasicPropertyInfo");
					hotelInfo = roomInfo.getJSONObject(JSON_PROP_HOTELINFO);
					hotelElem.setAttribute("HotelCode",hotelInfo.getString(JSON_PROP_HOTELCODE));
					hotelElem.setAttribute("HotelCodeContext",hotelInfo.getString(JSON_PROP_HOTELREF));
					hotelElem.setAttribute("HotelCityCode",roomObjectJson.getString(JSON_PROP_CITYCODE));
					((Element) hotelElem.appendChild(wrapperOwner.createElementNS(NS_OTA, "ota:Address"))
							.appendChild(wrapperOwner.createElementNS(NS_OTA, "ota:CountryName")))
					.setAttribute("Code",roomObjectJson.getString(JSON_PROP_COUNTRYCODE));

					roomStayElem.appendChild(wrapperOwner.createElementNS(NS_OTA, "ota:RoomTypes")).appendChild(roomTypeElem);
					roomStayElem.appendChild(wrapperOwner.createElementNS(NS_OTA, "ota:RatePlans")).appendChild(ratePlanElem);
					roomStayElem.appendChild(guestCountsElem);
					roomStayElem.appendChild(timeSpanElem);
					roomStayElem.appendChild(totalElem);
					roomStayElem.appendChild(hotelElem);
					for(Object reference:roomInfo.getJSONArray(JSON_PROP_REFERENCESARR)) {
						referenceElem = wrapperOwner.createElementNS(NS_OTA, "Reference");
						referenceElem.setAttribute("ID",((JSONObject) reference).getString(JSON_PROP_REFVALUE));
						referenceElem.setAttribute("ID_Context",((JSONObject) reference).getString(JSON_PROP_REFNAME));
						referenceElem.setAttribute("Type",((JSONObject) reference).getString(JSON_PROP_REFCODE));
						roomStayElem.appendChild(referenceElem);
					}

					roomStayElem.setAttribute("RPH", String.valueOf(roomInfo.getInt(JSON_PROP_ROOMINDEX)));
					roomStayElem.setAttribute("RoomStayStatus", (roomInfo.getString(JSON_PROP_AVAILSTATUS)));
					roomStaysElem.appendChild(roomStayElem);
				}

				totalElem = XMLUtils.getFirstElementAtXPath(resGlobalInfoElem, "./ota:Total");
				taxesElem = XMLUtils.getFirstElementAtXPath(totalElem, "./ota:Taxes");
				totalElem.setAttribute("AmountAfterTax", String.valueOf(bookingTotalPrice));
				totalElem.setAttribute("CurrencyCode", suppCcyCode);
				taxesElem.setAttribute("Amount",  String.valueOf(bookingTaxPrice));
				taxesElem.setAttribute("CurrencyCode", suppCcyCode);
				//TODO:payment info is hard coded in shell. It will be cnk payment details.Where will it come from?

				hotelResElem.appendChild(roomStaysElem);
				hotelResElem.appendChild(resGuestsElem);
				hotelResElem.appendChild(resGlobalInfoElem);
				//SI req made
				XMLUtils.insertChildNode(reqElem, "./accoi:RequestBody", wrapperElement, false);
			}


			String bookingId = reqBodyJson.getString("bookID");
			JSONObject kafkaMsgJson = reqJson;
			kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put("bookID",bookingId);
			addSupplierClientCommTotalComm(kafkaMsgJson);
			//System.out.println("Kafka request msg"+kafkaMsgJson);
			//System.out.println("bookreq"+XMLTransformer.toString(reqElem));
			//KafkaBookProducer bookProducer = new KafkaBookProducer();
			//bookProducer.runProducer(1, kafkaMsgJson);


			//return XMLTransformer.toString(reqElem);

			Element resElem = null;
			logger.trace(String.format("SI XML Request = %s", XMLTransformer.toString(reqElem)));
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), AccoConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			logger.trace(String.format("SI XML Response = %s", XMLTransformer.toString(resElem)));
			//SI res made

			//TODOthis is temp.make it proper
			System.out.println("book res"+XMLTransformer.toString(resElem));

			JSONObject resJson = getSupplierResponseJSON(reqJson, resElem);
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));

			//kafka response
			JSONObject resBodyJson = new JSONObject();
			resBodyJson.put(JSON_PROP_BOOKID, bookingId);
			JSONArray suppBooksJsonArr = new JSONArray();
			JSONArray accInfoArr=resJson.getJSONArray("accomodationInfo");
			for(Object accInfoObj:accInfoArr) {
				JSONObject suppBookJson = new JSONObject();
				suppBookJson.put(JSON_PROP_SUPPREF,((JSONObject) accInfoObj).getString(JSON_PROP_SUPPREF));
				JSONArray suppBookRefArr=((JSONObject) accInfoObj).getJSONArray("supplierBookReferences");
				for(int i=0;i<suppBookRefArr.length();i++) {
					if("14".equals(suppBookRefArr.getJSONObject(i).getString(JSON_PROP_REFCODE))) {
						suppBookJson.put(JSON_PROP_REFVALUE,suppBookRefArr.getJSONObject(i).getString(JSON_PROP_REFVALUE));
						break;
					}
				}

				suppBooksJsonArr.put(suppBookJson);
			}

			resBodyJson.put(JSON_PROP_SUPPBOOKREFERENCES, suppBooksJsonArr);
			kafkaMsgJson = new JSONObject();
			kafkaMsgJson.put(JSON_PROP_RESBODY, resBodyJson);
			kafkaMsgJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("product", PRODUCT_ACCO);
			System.out.println("kafka response"+kafkaMsgJson);
			//bookProducer.runProducer(1, kafkaMsgJson);

			return resJson.toString();
		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	public static String processV2(JSONObject reqJson) {
		try{
			OperationConfig opConfig = AccoConfig.getOperationConfig(OPERATION_NAME);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			TrackingContext.setTrackingContext(reqJson);
		
			setSupplierRequestElem(reqJson, reqElem);
			
			kafkaRequestJson(reqJson);

			Element resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), AccoConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			JSONObject resJson = getSupplierResponseJSON(reqJson, resElem);
			
			kafkaResponseJson(reqJson,resJson);
			
			return resJson.toString();
		}
		catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	private static void kafkaResponseJson(JSONObject reqJson, JSONObject resJson) throws Exception {
		//kafka response
		/*JSONObject resBodyJson = new JSONObject();
		resBodyJson.put(JSON_PROP_BOOKID, reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		JSONArray suppBooksJsonArr = new JSONArray();
		JSONArray accInfoArr=resJson.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		for(Object accInfoObj:accInfoArr) {
			JSONObject suppBookJson = new JSONObject();
			suppBookJson.put(JSON_PROP_SUPPREF,((JSONObject) accInfoObj).getString(JSON_PROP_SUPPREF));
			JSONArray suppBookRefArr=((JSONObject) accInfoObj).getJSONArray(JSON_PROP_SUPPBOOKREFERENCES);
			for(int i=0;i<suppBookRefArr.length();i++) {
				if("14".equals(suppBookRefArr.getJSONObject(i).getString(JSON_PROP_REFCODE))) {
					suppBookJson.put(JSON_PROP_REFVALUE,suppBookRefArr.getJSONObject(i).getString(JSON_PROP_REFVALUE));
					break;
				}
			}

			suppBooksJsonArr.put(suppBookJson);
		}
		resBodyJson.put(JSON_PROP_SUPPBOOKREFERENCES, suppBooksJsonArr);
		JSONObject kafkaMsgJson = new JSONObject();
		kafkaMsgJson.put(JSON_PROP_RESBODY, resBodyJson);
		kafkaMsgJson.put(JSON_PROP_RESHEADER, reqJson.getJSONObject(JSON_PROP_REQHEADER));
		kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_PROD, PRODUCT_ACCO);
		System.out.println("kafka response"+kafkaMsgJson);*/
		
		//JSONObject kafkaMsgJson = new JSONObject();
		//kafkaMsgJson.put(JSON_PROP_RESHEADER, resJson.getJSONObject(JSON_PROP_RESHEADER));
		resJson.getJSONObject(JSON_PROP_RESBODY).put(JSON_PROP_BOOKID, reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		//System.out.println("kafka response"+resJson);
		KafkaBookProducer bookProducer = new KafkaBookProducer();
		bookProducer.runProducer(1, resJson);
    }

	private static void kafkaRequestJson(JSONObject reqJson) throws Exception {
		JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
        reqBodyJson.put(JSON_PROP_TYPE, "request");
		reqBodyJson.put(JSON_PROP_PROD, PRODUCT_ACCO);
		addSupplierClientCommTotalComm(reqJson);
		System.out.println("Kafka request msg"+reqJson);
		KafkaBookProducer bookProducer = new KafkaBookProducer();
		bookProducer.runProducer(1, reqJson);
		
	}
    
	private static void addSupplierClientCommTotalComm(JSONObject kafkamsgjson) {
		Map<String, JSONObject> suppCommTotalsMap = new HashMap<String, JSONObject>();
		Map<String, JSONObject> supptaxBrkUpTotalsMap = new HashMap<String, JSONObject>();
		Map<String, JSONObject> roomPrctaxBrkUpTotalsMap = new HashMap<String, JSONObject>();
		Map<String, JSONObject> clientCommTotalsMap = new HashMap<String, JSONObject>();
		JSONArray suppCommTotalsJsonArr = new JSONArray();
		JSONArray clientEntityTotalCommArr=new JSONArray();
        JSONObject reqBody = kafkamsgjson.getJSONObject(JSON_PROP_REQBODY);
		JSONArray multiReqArr = reqBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		for(int i=0;i<multiReqArr.length();i++) {//accoinfo arr
			JSONObject roomObjectJson =   multiReqArr.getJSONObject(i);	
			BigDecimal totalroomSuppInfoAmt= new BigDecimal(0);
			BigDecimal totalroomPriceAmt= new BigDecimal(0);
			BigDecimal totalroomTaxAmt= new BigDecimal(0);
			BigDecimal totalroomSuppTaxAmt= new BigDecimal(0);
			JSONObject totalRoomSuppPriceInfo=new JSONObject();
			JSONObject totalpriceInfo=new JSONObject();
			
			for(Object roomConfig:roomObjectJson.getJSONArray(JSON_PROP_REQUESTEDROOMARR)) {//roomConfig length

				//Calculate total roomSuppPriceInfo
				JSONObject roomSuppInfo=((JSONObject) roomConfig).getJSONObject(JSON_PROP_SUPPROOMPRICE);
				BigDecimal[] suppRoomTotals=getTotalTaxesV2(roomSuppInfo,totalRoomSuppPriceInfo,supptaxBrkUpTotalsMap,totalroomSuppInfoAmt,totalroomSuppTaxAmt);
				totalroomSuppInfoAmt=suppRoomTotals[0];
				totalroomSuppTaxAmt=suppRoomTotals[1];

                                //Calculate total roomPriceInfo
				JSONObject roompriceInfo=((JSONObject) roomConfig).getJSONObject(JSON_PROP_ROOMPRICE);
				BigDecimal[] roomTotals=getTotalTaxesV2(roompriceInfo,totalpriceInfo,roomPrctaxBrkUpTotalsMap,totalroomPriceAmt,totalroomTaxAmt);
				totalroomPriceAmt=roomTotals[0];
				totalroomTaxAmt=roomTotals[1];

				//ADD SUPPLIER COMMERCIALS
				JSONArray suppCommJsonArr=((JSONObject) roomConfig).optJSONArray(JSON_PROP_SUPPCOMM);

				// If no supplier commercials have been defined in BRMS, the JSONArray for suppCommJsonArr will be null.
				// In this case, log a message and proceed with other calculations.
				if (suppCommJsonArr == null) {
					logger.warn("No supplier commercials found");
				}
				else {
					for (int j=0; j < suppCommJsonArr.length(); j++) {
						JSONObject suppCommJson = suppCommJsonArr.getJSONObject(j);
						String suppCommName = suppCommJson.getString(JSON_PROP_COMMNAME);
						JSONObject suppCommTotalsJson = null;
						if (suppCommTotalsMap.containsKey(suppCommName)) {
							suppCommTotalsJson = suppCommTotalsMap.get(suppCommName);
							suppCommTotalsJson.put(JSON_PROP_COMMAMOUNT, suppCommTotalsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).add(suppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT)));
						}
						else {
							suppCommTotalsJson = new JSONObject();
							suppCommTotalsJson.put(JSON_PROP_COMMNAME, suppCommName);
							suppCommTotalsJson.put(JSON_PROP_COMMCCY, suppCommJson.optString(JSON_PROP_COMMCCY));
							suppCommTotalsJson.put(JSON_PROP_COMMTYPE, suppCommJson.getString(JSON_PROP_COMMTYPE));
							suppCommTotalsJson.put(JSON_PROP_COMMAMOUNT, suppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
							suppCommTotalsMap.put(suppCommName, suppCommTotalsJson);
						}
					}

				}
				
				/*Iterator<Entry<String, JSONObject>> suppCommTotalsIter = suppCommTotalsMap.entrySet().iterator();
				while (suppCommTotalsIter.hasNext()) {
					suppCommTotalsJsonArr.put(suppCommTotalsIter.next().getValue());
				}*/
				
	            
				//ADDING CLIENTCOMMTOTAL
				JSONArray clientCommJsonArr=((JSONObject) roomConfig).optJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS);
				if (clientCommJsonArr == null) {
					logger.warn("No supplier commercials found");
				}
				else {
					//for each entity in clientcomm add a new object in clientEntityTotalCommercials Array
					for (int j=0; j < clientCommJsonArr.length(); j++) {
						JSONObject clientCommJson = clientCommJsonArr.getJSONObject(j);
						String entityName = clientCommJson.getString(JSON_PROP_ENTITYNAME);
						JSONArray clntCommArr = clientCommJson.getJSONArray(JSON_PROP_CLIENTCOMM);
			            JSONObject clientCommTotalsJson = null;
						

						//Add the commercial type inside the markUpCommercialDetails obj into clientCommTotalsMap and calculate total of each entity
						for(int k=0;k<clntCommArr.length();k++) {
						JSONObject clntCommJsonObj= clntCommArr.getJSONObject(k);
						String clientCommName = clntCommArr.getJSONObject(k).getString(JSON_PROP_COMMNAME);

						if (clientCommTotalsMap.containsKey(clientCommName.concat(entityName))) {
							clientCommTotalsJson = clientCommTotalsMap.get(clientCommName.concat(entityName));
							clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, clientCommTotalsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).add(clntCommJsonObj.getBigDecimal(JSON_PROP_COMMAMOUNT)));
						}
						else {
							clientCommTotalsJson = new JSONObject();
							clientCommTotalsJson.put(JSON_PROP_COMMNAME, clientCommName);
							clientCommTotalsJson.put(JSON_PROP_COMMTYPE, clntCommJsonObj.getString(JSON_PROP_COMMTYPE));
							clientCommTotalsJson.put(JSON_PROP_COMMCCY, clntCommJsonObj.optString(JSON_PROP_COMMCCY));
							clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, clntCommJsonObj.getBigDecimal(JSON_PROP_COMMAMOUNT));
							clientCommTotalsMap.put(clientCommName.concat(entityName), clientCommTotalsJson);
						}

						//
						
						}
						JSONArray clientCommTotalsJsonArr = new JSONArray();
						Iterator<Entry<String, JSONObject>> clientCommTotalsIter = clientCommTotalsMap.entrySet().iterator();
						while (clientCommTotalsIter.hasNext()) {
							clientCommTotalsJsonArr.put(clientCommTotalsIter.next().getValue());
						}
						if(!clientCommTotalsMap.containsKey(entityName)) {
							JSONObject clientEntityTotalJson=new JSONObject();
							clientEntityTotalJson.put(JSON_PROP_CLIENTID, clientCommJson.getString(JSON_PROP_CLIENTID));
							clientEntityTotalJson.put(JSON_PROP_PARENTCLIENTID, clientCommJson.getString(JSON_PROP_PARENTCLIENTID));
							clientEntityTotalJson.put(JSON_PROP_COMMENTITYTYPE, clientCommJson.getString(JSON_PROP_COMMENTITYTYPE));
							clientEntityTotalJson.put(JSON_PROP_COMMENTITYID,clientCommJson.getString(JSON_PROP_COMMENTITYID));
							clientCommTotalsMap.put(entityName,clientEntityTotalJson);
							clientEntityTotalJson.put(JSON_PROP_CLIENTCOMMTOTAL, clientCommTotalsJsonArr);
							clientEntityTotalCommArr.put(clientEntityTotalJson);
					}
						

				}
				}
				//final total calculation at order level
				/*roomObjectJson.put(JSON_PROP_SUPPCOMMTOTALS, suppCommTotalsJsonArr);
				roomObjectJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientEntityTotalCommArr);
				roomObjectJson.put(JSON_PROP_SUPPBOOKPRICE, totalRoomSuppPriceInfo);
				roomObjectJson.put(JSON_PROP_BOOKPRICE, totalpriceInfo);*/

				
			}
			Iterator<Entry<String, JSONObject>> suppCommTotalsIter = suppCommTotalsMap.entrySet().iterator();
			while (suppCommTotalsIter.hasNext()) {
				suppCommTotalsJsonArr.put(suppCommTotalsIter.next().getValue());
			}
			
			roomObjectJson.put(JSON_PROP_SUPPCOMMTOTALS, suppCommTotalsJsonArr);
			roomObjectJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientEntityTotalCommArr);
			roomObjectJson.put(JSON_PROP_SUPPBOOKPRICE, totalRoomSuppPriceInfo);
			roomObjectJson.put(JSON_PROP_BOOKPRICE, totalpriceInfo);
		}
		
		
	}
	
	/*private static void addSupplierClientCommTotalComm(JSONObject kafkamsgjson) {
		Map<String, JSONObject> suppCommTotalsMap = new HashMap<String, JSONObject>();
		Map<String, JSONObject> supptaxBrkUpTotalsMap = new HashMap<String, JSONObject>();
		Map<String, JSONObject> roomPrctaxBrkUpTotalsMap = new HashMap<String, JSONObject>();


		JSONObject reqBody = kafkamsgjson.getJSONObject(JSON_PROP_REQBODY);
		JSONArray multiReqArr = reqBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		for(int i=0;i<multiReqArr.length();i++) {
			JSONObject roomObjectJson =   multiReqArr.getJSONObject(i);	
			Map<String, JSONObject> clientCommTotalsMap = new HashMap<String, JSONObject>();
			BigDecimal totalroomSuppInfoAmt= new BigDecimal(0);
			BigDecimal totalroomPriceAmt= new BigDecimal(0);
			BigDecimal totalroomTaxAmt= new BigDecimal(0);
			BigDecimal totalroomSuppTaxAmt= new BigDecimal(0);
			JSONObject totalRoomSuppPriceInfo=new JSONObject();
			JSONObject totalpriceInfo=new JSONObject();
			for(Object roomConfig:roomObjectJson.getJSONArray(JSON_PROP_REQUESTEDROOMARR)) {//roomConfig length

				//Calculate total roomSuppPriceInfo
				JSONObject roomSuppInfo=((JSONObject) roomConfig).getJSONObject(JSON_PROP_SUPPROOMPRICE);
				BigDecimal[] suppRoomTotals=getTotalTaxesV2(roomSuppInfo,totalRoomSuppPriceInfo,supptaxBrkUpTotalsMap,totalroomSuppInfoAmt,totalroomSuppTaxAmt);
				totalroomSuppInfoAmt=suppRoomTotals[0];
				totalroomSuppTaxAmt=suppRoomTotals[1];

				JSONObject totalRoomSuppPriceInfo=new JSONObject();
				totalroomSuppInfoAmt=totalroomSuppInfoAmt.add(roomSuppInfo.getBigDecimal(JSON_PROP_AMOUNT));
				totalRoomSuppPriceInfo.put("amount", totalroomSuppInfoAmt) ;
				totalRoomSuppPriceInfo.put("currencyCode", roomSuppInfo.getString("currencyCode"));
				JSONObject totaltaxes=new JSONObject();
				JSONObject taxes=roomSuppInfo.getJSONObject("taxes");
				totalroomSuppTaxAmt=totalroomSuppTaxAmt.add(taxes.optBigDecimal("amount",new BigDecimal(0)));
				totaltaxes.put(JSON_PROP_AMOUNT,totalroomSuppTaxAmt );
				totaltaxes.put(JSON_PROP_CCYCODE, taxes.getString("currencyCode"));
				JSONArray taxBreakUpArr = taxes.getJSONArray("taxBreakup");
				for(int t=0;t<taxBreakUpArr.length();t++) {
					JSONObject taxBrkUpJson = taxBreakUpArr.getJSONObject(t);
					String taxCode= taxBrkUpJson.getString("taxCode");
					JSONObject taxBrkUpTotalsJson = null;
					if (supptaxBrkUpTotalsMap.containsKey(taxCode)) {
						taxBrkUpTotalsJson = supptaxBrkUpTotalsMap.get(taxCode);
						taxBrkUpTotalsJson.put(JSON_PROP_AMOUNT, taxBrkUpTotalsJson.getBigDecimal(JSON_PROP_AMOUNT).add(taxBrkUpJson.getBigDecimal(JSON_PROP_AMOUNT)));
					}
					else {
						taxBrkUpTotalsJson = new JSONObject();
						taxBrkUpTotalsJson.put(JSON_PROP_AMOUNT,taxBrkUpJson.getBigDecimal(JSON_PROP_AMOUNT));
						taxBrkUpTotalsJson.put("taxCode", taxBrkUpJson.getString("taxCode"));
						taxBrkUpTotalsJson.put(JSON_PROP_CCYCODE,taxBrkUpJson.getString(JSON_PROP_CCYCODE));
						supptaxBrkUpTotalsMap.put(taxCode, taxBrkUpTotalsJson);
					}

				}
				JSONArray TotaltaxBrkUpArr = new JSONArray();
				Iterator<Entry<String, JSONObject>> supptaxIter = supptaxBrkUpTotalsMap.entrySet().iterator();
				while (supptaxIter.hasNext()) {
					TotaltaxBrkUpArr.put(supptaxIter.next().getValue());
				}
				totaltaxes.put("totalTaxBreakUp", TotaltaxBrkUpArr);
				totalRoomSuppPriceInfo.put("totalTaxes", totaltaxes);


				//Calculate total roomPriceInfo
				JSONObject roompriceInfo=((JSONObject) roomConfig).getJSONObject(JSON_PROP_ROOMPRICE);
				BigDecimal[] roomTotals=getTotalTaxesV2(roompriceInfo,totalpriceInfo,roomPrctaxBrkUpTotalsMap,totalroomPriceAmt,totalroomTaxAmt);
				totalroomPriceAmt=roomTotals[0];
				totalroomTaxAmt=roomTotals[1];

				//JSONObject totalpriceInfo=new JSONObject();
				totalroomPriceAmt=totalroomPriceAmt.add(roompriceInfo.getBigDecimal(JSON_PROP_AMOUNT));
				totalpriceInfo.put("amount", totalroomPriceAmt) ;
				totalpriceInfo.put("currencyCode", roompriceInfo.getString("currencyCode"));
				JSONObject roomPrctotaltaxes=new JSONObject();
				JSONObject roomPrctaxes=roompriceInfo.getJSONObject("taxes");
				totalroomTaxAmt=totalroomTaxAmt.add(roomPrctaxes.getBigDecimal("amount"));
				roomPrctotaltaxes.put(JSON_PROP_AMOUNT,totalroomTaxAmt );
				roomPrctotaltaxes.put(JSON_PROP_CCYCODE, roomPrctaxes.getString("currencyCode"));
				JSONArray roomPrctaxBreakUpArr = roomPrctaxes.getJSONArray("taxBreakup");
				for(int t=0;t<roomPrctaxBreakUpArr.length();t++) {
					JSONObject taxBrkUpJson = roomPrctaxBreakUpArr.getJSONObject(t);
					String taxCode= taxBrkUpJson.getString("taxCode");
					JSONObject taxBrkUpTotalsJson = null;
					if (roomPrctaxBrkUpTotalsMap.containsKey(taxCode)) {
						taxBrkUpTotalsJson = roomPrctaxBrkUpTotalsMap.get(taxCode);
						taxBrkUpTotalsJson.put(JSON_PROP_AMOUNT, taxBrkUpTotalsJson.getBigDecimal(JSON_PROP_AMOUNT).add(taxBrkUpJson.getBigDecimal(JSON_PROP_AMOUNT)));
					}
					else {
						taxBrkUpTotalsJson = new JSONObject();
						taxBrkUpTotalsJson.put(JSON_PROP_AMOUNT,taxBrkUpJson.getBigDecimal(JSON_PROP_AMOUNT));
						taxBrkUpTotalsJson.put("taxCode", taxBrkUpJson.getString("taxCode"));
						taxBrkUpTotalsJson.put(JSON_PROP_CCYCODE,taxBrkUpJson.getString(JSON_PROP_CCYCODE));
						roomPrctaxBrkUpTotalsMap.put(taxCode, taxBrkUpTotalsJson);
					}

				}
				JSONArray roomPrcTotaltaxBrkUpArr = new JSONArray();
				Iterator<Entry<String, JSONObject>> roomPrctaxIter = supptaxBrkUpTotalsMap.entrySet().iterator();
				while (roomPrctaxIter.hasNext()) {
					roomPrcTotaltaxBrkUpArr.put(roomPrctaxIter.next().getValue());
				}
				roomPrctotaltaxes.put("totalTaxBreakUp", roomPrcTotaltaxBrkUpArr);
				totalpriceInfo.put("totalTaxes", roomPrctotaltaxes);
				 

				//ADD SUPPLIER COMMERCIALS
				JSONArray suppCommJsonArr=((JSONObject) roomConfig).optJSONArray(JSON_PROP_SUPPCOMM);

				// If no supplier commercials have been defined in BRMS, the JSONArray for suppCommJsonArr will be null.
				// In this case, log a message and proceed with other calculations.
				if (suppCommJsonArr == null) {
					logger.warn("No supplier commercials found");
				}
				else {
					for (int j=0; j < suppCommJsonArr.length(); j++) {
						JSONObject suppCommJson = suppCommJsonArr.getJSONObject(j);
						String suppCommName = suppCommJson.getString(JSON_PROP_COMMNAME);
						JSONObject suppCommTotalsJson = null;
						if (suppCommTotalsMap.containsKey(suppCommName)) {
							suppCommTotalsJson = suppCommTotalsMap.get(suppCommName);
							suppCommTotalsJson.put(JSON_PROP_COMMAMOUNT, suppCommTotalsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).add(suppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT)));
						}
						else {
							suppCommTotalsJson = new JSONObject();
							suppCommTotalsJson.put(JSON_PROP_COMMNAME, suppCommName);
							suppCommTotalsJson.put(JSON_PROP_COMMCCY, suppCommJson.optString(JSON_PROP_COMMCCY));
							suppCommTotalsJson.put(JSON_PROP_COMMTYPE, suppCommJson.getString(JSON_PROP_COMMTYPE));
							suppCommTotalsJson.put(JSON_PROP_COMMAMOUNT, suppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
							suppCommTotalsMap.put(suppCommName, suppCommTotalsJson);
						}
					}

				}
				JSONArray suppCommTotalsJsonArr = new JSONArray();
				Iterator<Entry<String, JSONObject>> suppCommTotalsIter = suppCommTotalsMap.entrySet().iterator();
				while (suppCommTotalsIter.hasNext()) {
					suppCommTotalsJsonArr.put(suppCommTotalsIter.next().getValue());
				}
				JSONObject reqHeader = kafkamsgjson.getJSONObject(JSON_PROP_REQHEADER);
				UserContext usrCtx = UserContext.getUserContextForSession(reqHeader);
				List<ClientInfo> clientCommHierarchyList = usrCtx.getClientHierarchy();

				//ADDING CLIENTCOMMTOTAL
				JSONArray clientEntityTotalCommArr=new JSONArray();
				JSONArray clientCommJsonArr=((JSONObject) roomConfig).optJSONArray(JSON_PROP_CLIENTCOMM);
				if (clientCommJsonArr == null) {
					logger.warn("No supplier commercials found");
				}
				else {
					//for each entity in clientcomm add a new object in clientEntityTotalCommercials Array
					for (int j=0; j < clientCommJsonArr.length(); j++) {
						JSONObject clientCommJson = clientCommJsonArr.getJSONObject(j);
						JSONArray additionalCommsJsonArr = clientCommJson.optJSONArray(JSON_PROP_ADDCOMMDETAILS);
						String clientID="";
						String ParentClienttId="";
						String CommercialEntityId="";
						CommercialsEntityType commEntityType=null;
						JSONObject clientEntityTotalJson=new JSONObject();
						if (clientCommHierarchyList != null && clientCommHierarchyList.size() > 0) {
							ClientInfo clInfo = clientCommHierarchyList.get(clientCommHierarchyList.size() - 1);
							if (clInfo.getCommercialsEntityId()==clientCommJson.get(JSON_PROP_ENTITYNAME)) {
								clientID = clInfo.getClientId();
								ParentClienttId=clInfo.getParentClienttId();
								CommercialEntityId=clInfo.getCommercialsEntityId();
								commEntityType=clInfo.getCommercialsEntityType();
							}
						}
						clientEntityTotalJson.put(JSON_PROP_CLIENTID, clientID);
						clientEntityTotalJson.put(JSON_PROP_PARENTCLIENTID, ParentClienttId);
						clientEntityTotalJson.put(JSON_PROP_COMMENTITYTYPE, commEntityType);
						clientEntityTotalJson.put(JSON_PROP_COMMENTITYID,CommercialEntityId);

						//Add the commercial types inside the additionalCommdetails array into clientCommTotalsMap and calculate total of each entity
						JSONObject clientCommTotalsJson = null;
						if((additionalCommsJsonArr!=null)&& (!(additionalCommsJsonArr.length()<0))){
							for(int p=0;p<additionalCommsJsonArr.length();p++) {
								JSONObject additionalCommJson=additionalCommsJsonArr.getJSONObject(p);
								String addcommName=additionalCommJson.getString(JSON_PROP_COMMNAME);

								if (clientCommTotalsMap.containsKey(addcommName.concat(clientCommJson.getString(JSON_PROP_ENTITYNAME)))) {
									clientCommTotalsJson = clientCommTotalsMap.get(addcommName.concat(clientCommJson.getString(JSON_PROP_ENTITYNAME)));
									clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, additionalCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT).add(additionalCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT)));
								}
								else {
									clientCommTotalsJson = new JSONObject();
									clientCommTotalsJson.put(JSON_PROP_COMMTYPE, additionalCommJson.getString(JSON_PROP_COMMNAME));
									clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, additionalCommJson.get(JSON_PROP_COMMAMOUNT).toString());
									clientCommTotalsJson.put(JSON_PROP_COMMNAME,additionalCommJson.get(JSON_PROP_COMMNAME));
									clientCommTotalsJson.put(JSON_PROP_COMMCCY,additionalCommJson.get(JSON_PROP_COMMCCY).toString());
									clientCommTotalsMap.put(addcommName.concat(clientCommJson.getString(JSON_PROP_ENTITYNAME)), clientCommTotalsJson);
								}
							}
						}

						//Add the commercial type inside the markUpCommercialDetails obj into clientCommTotalsMap and calculate total of each entity
						JSONObject markupJson = clientCommJson.getJSONObject(JSON_PROP_MARKUPCOMDTLS);
						String clientCommName = markupJson.getString(JSON_PROP_COMMNAME);

						if (clientCommTotalsMap.containsKey(clientCommName.concat(clientCommJson.getString(JSON_PROP_ENTITYNAME)))) {
							clientCommTotalsJson = clientCommTotalsMap.get(clientCommName.concat(clientCommJson.getString(JSON_PROP_ENTITYNAME)));
							clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, clientCommTotalsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).add(markupJson.getBigDecimal(JSON_PROP_COMMAMOUNT)));
						}
						else {
							clientCommTotalsJson = new JSONObject();
							clientCommTotalsJson.put(JSON_PROP_COMMNAME, clientCommName);
							clientCommTotalsJson.put(JSON_PROP_COMMTYPE, clientCommJson.getString(JSON_PROP_COMMTYPE));
							clientCommTotalsJson.put(JSON_PROP_COMMCCY, markupJson.optString(JSON_PROP_COMMCCY));
							clientCommTotalsJson.put(JSON_PROP_COMMAMOUNT, markupJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
							clientCommTotalsMap.put(clientCommName.concat(clientCommJson.getString("entityName")), clientCommTotalsJson);
						}

						//
						JSONArray clientCommTotalsJsonArr = new JSONArray();
						Iterator<Entry<String, JSONObject>> clientCommTotalsIter = clientCommTotalsMap.entrySet().iterator();
						while (clientCommTotalsIter.hasNext()) {
							clientCommTotalsJsonArr.put(clientCommTotalsIter.next().getValue());
						}
						clientEntityTotalJson.put(JSON_PROP_CLIENTCOMMTOTAL, clientCommTotalsJsonArr);
						clientEntityTotalCommArr.put(clientEntityTotalJson);
					}

				}

				//final total calculation at order level
				roomObjectJson.put(JSON_PROP_SUPPCOMMTOTALS, suppCommTotalsJsonArr);
				roomObjectJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientEntityTotalCommArr);
				roomObjectJson.put(JSON_PROP_SUPPBOOKPRCINFO, totalRoomSuppPriceInfo);
				roomObjectJson.put(JSON_PROP_BOOKPRCINFO, totalpriceInfo);

			}
		}
	}*/

	private static  BigDecimal[] getTotalTaxesV2(JSONObject roomSuppInfo, JSONObject totalPriceInfo, Map<String, JSONObject> taxBrkUpTotalsMap, BigDecimal totalInfoAmt, BigDecimal totalTaxAmt) {
		totalInfoAmt=totalInfoAmt.add(roomSuppInfo.getBigDecimal(JSON_PROP_AMOUNT));
		totalPriceInfo.put(JSON_PROP_AMOUNT, totalInfoAmt) ;
		totalPriceInfo.put(JSON_PROP_CCYCODE, roomSuppInfo.getString(JSON_PROP_CCYCODE));
		JSONObject totaltaxes=new JSONObject();
		JSONObject taxes=roomSuppInfo.getJSONObject(JSON_PROP_TOTALTAX);
		totalTaxAmt=totalTaxAmt.add(taxes.optBigDecimal(JSON_PROP_AMOUNT,new BigDecimal(0)));
		totaltaxes.put(JSON_PROP_AMOUNT,totalTaxAmt );
		totaltaxes.put(JSON_PROP_CCYCODE, taxes.optString(JSON_PROP_CCYCODE));
		JSONArray taxBreakUpArr = taxes.optJSONArray(JSON_PROP_TAXBRKPARR);
		if(taxBreakUpArr!=null) {
		for(int t=0;t<taxBreakUpArr.length();t++) {
			JSONObject taxBrkUpJson = taxBreakUpArr.getJSONObject(t);
			String taxCode= taxBrkUpJson.getString(JSON_PROP_TAXCODE);
			JSONObject taxBrkUpTotalsJson = null;
			if (taxBrkUpTotalsMap.containsKey(taxCode)) {
				taxBrkUpTotalsJson = taxBrkUpTotalsMap.get(taxCode);
				taxBrkUpTotalsJson.put(JSON_PROP_AMOUNT, taxBrkUpTotalsJson.getBigDecimal(JSON_PROP_AMOUNT).add(taxBrkUpJson.getBigDecimal(JSON_PROP_AMOUNT)));
			}
			else {
				taxBrkUpTotalsJson = new JSONObject();
				taxBrkUpTotalsJson.put(JSON_PROP_AMOUNT,taxBrkUpJson.getBigDecimal(JSON_PROP_AMOUNT));
				taxBrkUpTotalsJson.put(JSON_PROP_TAXCODE, taxBrkUpJson.getString(JSON_PROP_TAXCODE));
				taxBrkUpTotalsJson.put(JSON_PROP_CCYCODE,taxBrkUpJson.getString(JSON_PROP_CCYCODE));
				taxBrkUpTotalsMap.put(taxCode, taxBrkUpTotalsJson);
			}

		}
		
		JSONArray TotaltaxBrkUpArr = new JSONArray();
		Iterator<Entry<String, JSONObject>> taxIter = taxBrkUpTotalsMap.entrySet().iterator();
		while (taxIter.hasNext()) {
			TotaltaxBrkUpArr.put(taxIter.next().getValue());
		}
		totaltaxes.put(JSON_PROP_TAXBRKPARR, TotaltaxBrkUpArr);
		}
		totalPriceInfo.put(JSON_PROP_TOTALTAX, totaltaxes);

		return new BigDecimal[] {totalInfoAmt, totalTaxAmt};
	}

	/*private JSONObject getSupplierResponseJSON(JSONObject reqJson, Element resElem) {
		JSONArray accomodationInfoArr = new JSONArray();
		JSONObject accomodationInfoObj,roomStayJson,uniqueIdObj;
		JSONArray roomStayJsonArr=new JSONArray();
		JSONArray uniqueIdArray=new JSONArray();

		for(Element wrapperElem:XMLUtils.getElementsAtXPath(resElem, "./accoi:ResponseBody/acco:OTA_HotelResRSWrapper")) {
			accomodationInfoObj=new JSONObject();
			accomodationInfoObj.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(wrapperElem, "./acco:SupplierID"));
			for(Element uniqueId : XMLUtils.getElementsAtXPath(wrapperElem, "./ota:OTA_HotelResRS/ota:HotelReservations/ota:HotelReservation/ota:UniqueID"))
			{
				uniqueIdObj=new JSONObject();
				uniqueIdObj.put(JSON_PROP_BOOKREFID, uniqueId.getAttribute("ID"));
				uniqueIdObj.put(JSON_PROP_BOOKREFTYPE, uniqueId.getAttribute("Type"));
				uniqueIdArray.put(uniqueIdObj);
			}
			accomodationInfoObj.put(JSON_PROP_UNIQUEID, uniqueIdArray);
			for (Element roomStayElem : XMLUtils.getElementsAtXPath(wrapperElem, "./ota:OTA_HotelResRS/ota:HotelReservations/ota:HotelReservation/ota:RoomStays/ota:RoomStay")) {
				roomStayJson = AccoSearchProcessor.getRoomStayJSON(roomStayElem);
				roomStayJson.remove(JSON_PROP_ROOMPRICE);
				roomStayJson.remove(JSON_PROP_NIGHTLYPRICEARR);
				roomStayJson.remove(JSON_PROP_OCCUPANCYARR);
				roomStayJson.getJSONObject(JSON_PROP_ROOMINFO).remove(JSON_PROP_AVAILSTATUS);
				roomStayJsonArr.put(roomStayJson);


			}
			accomodationInfoObj.put(JSON_PROP_ROOMSTAYARR,roomStayJsonArr);
			accomodationInfoArr.put(accomodationInfoObj);
		}
		JSONObject resJson = new JSONObject();
		JSONObject resBodyJson = new JSONObject();
		resBodyJson.put(JSON_PROP_ACCOMODATIONARR, accomodationInfoArr);
		resJson.put(JSON_PROP_RESHEADER, reqJson.getJSONObject(JSON_PROP_REQHEADER));
		resJson.put(JSON_PROP_RESBODY, resBodyJson);
		return resJson;
	}*/

	private static JSONObject getSupplierResponseJSON(JSONObject reqJson, Element resElem) {

		JSONObject resBodyJson = new JSONObject();
		JSONArray multiResArr = new JSONArray();
		resBodyJson.put(JSON_PROP_ACCOMODATIONARR, multiResArr);
		int sequence = 0;String sequence_str="";
		JSONObject accoInfoJson,bookIdJson,roomJson;
		JSONArray bookRefJsonArr,roomRefJsonArr;

		for(Element wrapperElem:XMLUtils.getElementsAtXPath(resElem, "./accoi:ResponseBody/acco:OTA_HotelResRSWrapper")) {
			accoInfoJson = new JSONObject();

			accoInfoJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(wrapperElem, "./acco:SupplierID"));

			bookRefJsonArr = new JSONArray();
			for(Element uniqueId : XMLUtils.getElementsAtXPath(wrapperElem, "./ota:OTA_HotelResRS/ota:HotelReservations/ota:HotelReservation/ota:UniqueID")) {
				bookIdJson = AccoSearchProcessor.getReferencesInfoJson(uniqueId);
				bookIdJson.remove(JSON_PROP_REFNAME);
				bookRefJsonArr.put(bookIdJson);
			}
			accoInfoJson.put(JSON_PROP_SUPPBOOKREFERENCES, bookRefJsonArr);

			roomRefJsonArr = new JSONArray();
			for (Element roomStayElem : XMLUtils.getElementsAtXPath(wrapperElem, "./ota:OTA_HotelResRS/ota:HotelReservations/ota:HotelReservation/ota:RoomStays/ota:RoomStay")) {
				roomJson = new JSONObject();
				String roomIdx_str=XMLUtils.getValueAtXPath(roomStayElem, "./@RPH");
				int roomIdx = roomIdx_str.isEmpty()?-1:Integer.valueOf(roomIdx_str);
				roomJson.put(JSON_PROP_ROOMINDEX,roomIdx);
				roomJson.put(JSON_PROP_SUPPROOMINDEX, XMLUtils.getValueAtXPath(roomStayElem, "./@IndexNumber"));
				roomRefJsonArr.put(roomJson);
			}
			accoInfoJson.put(JSON_PROP_SUPPROOMREFERENCES, roomRefJsonArr);

			sequence_str = XMLUtils.getValueAtXPath(wrapperElem, "./acco:Sequence");
			sequence = sequence_str.isEmpty()?sequence:Integer.parseInt(sequence_str);
			multiResArr.put(sequence++, accoInfoJson);
		}
		
		JSONObject resJson = new JSONObject();
		resJson.put(JSON_PROP_RESHEADER, reqJson.getJSONObject(JSON_PROP_REQHEADER));
		resJson.put(JSON_PROP_RESBODY, resBodyJson);
		
		return resJson;
	}

	static int calculateAge(String birthdate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		int age = 1;
		if(birthdate==null || birthdate.isEmpty())
			return age;
		if (birthdate.matches("([0-9]{4})-([0-9]{2})-([0-9]{2})")) {
			LocalDate dob = LocalDate.parse(birthdate);
			LocalDate curDate = LocalDate.now();
			age = Period.between(dob, curDate).getYears();
		} else if (birthdate.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
			LocalDate dob = LocalDate.parse(birthdate, formatter);
			LocalDate curDate = LocalDate.now();
			age = Period.between(dob, curDate).getYears();
		}
		return age;
	}

}
