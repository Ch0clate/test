package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarAmCl;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.CarDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.CarAmClRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Car")
@Transactional(readOnly = false)
public class CarDatabaseServiceImpl implements TestDbService,Constants, ErrorConstants, DataBaseService {

	@Autowired
	@Qualifier("Car")
	private CarDatabaseRepository carRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("CarAmCl")
	private CarAmClRepository carAmClRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());

	JSONObject response = new JSONObject();

	public boolean isResponsibleFor(String product) {
		return "car".equalsIgnoreCase(product);
	}

	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException {

		Booking booking = bookingRepository
				.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_CAR_BOOKID));

		if (booking == null)
			booking = populateBookingData(bookRequestJson);

		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY)
				.getJSONArray(JSON_PROP_CAR_CARRENTALARR)) {

			CarOrders order = populateCarData((JSONObject) orderJson, bookRequestHeader, booking);
			saveOrder(order, "");
		}

		return "success";
	}

	public CarOrders populateCarData(JSONObject bookReq, JSONObject bookRequestHeader, Booking booking)
			throws BookingEngineDBException {

		CarOrders order = new CarOrders();

		order.setBooking(booking);
		// TODO: change the client ID to userID once you get in header
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus("OnRequest");
		order.setClientCurrency(
				bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSupplierID(bookReq.getString(JSON_PROP_SUPPREF));

		order.setTripType(bookReq.optString(JSON_PROP_CAR_TRIPTYPE));
		
		JSONObject suppTotalFare = bookReq.getJSONObject(JSON_PROP_CAR_SUPPPRICEINFO).getJSONObject(JSON_PROP_CAR_TOTALFARE);
//		suppTotalFare.remove(JSON_PROP_CAR_PRICEDCOVERAGES);
//		suppTotalFare.remove(JSON_PROP_CAR_SPLEQUIPS);
		
		order.setSuppFares(suppTotalFare.toString());
		order.setSupplierTotalPrice(suppTotalFare.getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(suppTotalFare.getString(JSON_PROP_CAR_CURRENCYCODE));


		JSONObject totalFare = bookReq.getJSONObject(JSON_PROP_CAR_TOTALPRICEINFO).getJSONObject(JSON_PROP_CAR_TOTALFARE);
		JSONObject extraEquips = totalFare.optJSONObject(JSON_PROP_CAR_SPLEQUIPS);
		order.setExtraEquipments(extraEquips == null ? new JSONObject().toString() : extraEquips.toString());
		
		JSONObject pricedCovrgs = totalFare.optJSONObject(JSON_PROP_CAR_PRICEDCOVERAGES);
		order.setPricedCoverages(pricedCovrgs == null ? new JSONObject().toString() : pricedCovrgs.toString());
		
		totalFare.remove(JSON_PROP_CAR_PRICEDCOVERAGES);
		totalFare.remove(JSON_PROP_CAR_SPLEQUIPS);
		
		//TODO : To Check if to insert TotalFare, breakups are getting populated 
//		order.setTotalFares(totalFare.toString());
		order.setTotalPrice(totalFare.getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(totalFare.getString(JSON_PROP_CAR_CURRENCYCODE));
		order.setTotalPriceReceivables(totalFare.getJSONObject(JSON_PROP_RECEIVABLES).toString());
		order.setTotalBaseFare(totalFare.getJSONObject(JSON_PROP_BASEFARE).toString());
		
		JSONObject fees = totalFare.optJSONObject(JSON_PROP_FEES);
		order.setTotalPriceFees(fees == null ? new JSONObject().toString() : fees.toString());

		JSONObject taxes = totalFare.optJSONObject(JSON_PROP_TAXES);
		order.setTotalPriceTaxes(taxes == null ? new JSONObject().toString() : taxes.toString());

		JSONObject vehicleDetails = bookReq.getJSONObject(JSON_PROP_CAR_VEHICLEINFO);
		JSONObject rentalDetails = new JSONObject(bookReq.toString());
		rentalDetails.remove(JSON_PROP_PAXDETAILS);
		rentalDetails.remove(JSON_PROP_CAR_TOTALPRICEINFO);
		rentalDetails.remove(JSON_PROP_CAR_SUPPPRICEINFO);
		rentalDetails.remove(JSON_PROP_CAR_VEHICLEINFO);
		
		order.setRentalDetails(rentalDetails.toString());
		order.setCarDetails(vehicleDetails.toString());
		
		Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
		setPaxDetails = readPassengerDetails(bookReq.getJSONArray(JSON_PROP_PAXDETAILS), order);
		JSONArray paxIds = new JSONArray();
		for (PassengerDetails pax : setPaxDetails) {
			JSONObject paxJson = new JSONObject();
			paxJson.put("paxId", pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
		Set<SupplierCommercial> suppComms = new HashSet<SupplierCommercial>();
		suppComms = readSuppCommercials(
				bookReq.getJSONObject(JSON_PROP_CAR_SUPPPRICEINFO).getJSONArray(JSON_PROP_CAR_SUPPLIERCOMMS), order);

		Set<ClientCommercial> clientComms = new HashSet<ClientCommercial>();
		clientComms = readClientCommercials(bookReq.getJSONObject(JSON_PROP_CAR_TOTALPRICEINFO)
				.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS), order);
		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);

		return order;

	}

	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, CarOrders order) {

		Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;

		for (int i = 0; i < suppCommsJsonArray.length(); i++) {
			JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

			suppCommercials = new SupplierCommercial();
			suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
			suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
			suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
			suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));

			suppCommercials.setProduct(JSON_PROP_PRODUCTCAR);
			suppCommercials.setOrder(order);
			suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, CarOrders order) {

		Set<ClientCommercial> clientCommercialsSet = new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;

		for (int i = 0; i < clientCommsJsonArray.length(); i++) {

			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);

			String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);
			String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);
			String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);

			boolean companyFlag = (i == 0) ? true : false;

			JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_CLIENTCOMMERCIALSTOTAL);

			for (int j = 0; j < clientComms.length(); j++) {

				JSONObject clientComm = clientComms.getJSONObject(j);

				clientCommercials = new ClientCommercial();
				clientCommercials.setCommercialName(clientComm.getString(JSON_PROP_COMMERCIALNAME));
				clientCommercials.setCommercialType(clientComm.getString(JSON_PROP_COMMERCIALTYPE));
				clientCommercials.setCommercialAmount(clientComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
				clientCommercials.setCommercialCurrency(clientComm.getString(JSON_PROP_COMMERCIALCURRENCY));
				clientCommercials.setClientID(clientID);
				clientCommercials.setParentClientID(parentClientID);
				clientCommercials.setCommercialEntityType(commercialEntityType);
				clientCommercials.setCommercialEntityID(commercialEntityID);
				clientCommercials.setCompanyFlag(companyFlag);

				clientCommercials.setProduct(JSON_PROP_PRODUCTCAR);
				clientCommercials.setOrder(order);
				clientCommercialsSet.add(clientCommercials);
			}
		}
		return clientCommercialsSet;
	}

	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, CarOrders carOrder)
			throws BookingEngineDBException {

		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for (int i = 0; i < paxJsonArray.length(); i++) {

			JSONObject currentPaxDetails = paxJsonArray.getJSONObject(i);
			paxDetails = new PassengerDetails();
			// TODO : Set isLead Traveler.
			paxDetails.setIsLeadPax(currentPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			paxDetails.setTitle(currentPaxDetails.getString(JSON_PROP_TITLE));
			paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
			paxDetails.setStatus("OnRequest");
			paxDetails.setMiddleName(currentPaxDetails.optString(JSON_PROP_MIDDLENAME));
			paxDetails.setLastName(currentPaxDetails.optString(JSON_PROP_SURNAME));
			paxDetails.setBirthDate(currentPaxDetails.optString(JSON_PROP_CAR_DOB));
			paxDetails.setGender(currentPaxDetails.getString(JSON_PROP_GENDER));
			paxDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			paxDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

			// TODO:change it to userID later
			paxDetails.setLastModifiedBy("");
			paxDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			savePaxDetails(paxDetails, "");
			paxDetailsSet.add(paxDetails);

		}
		return paxDetailsSet;
	}

	private Booking populateBookingData(JSONObject bookRequestJson) {

		Booking order = new Booking();
		order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_CAR_BOOKID));
		order.setStatus("OnRequest");

		order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER)
				.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT)
				.getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT)
				.getString(JSON_PROP_CLIENTTYPE));
		order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
		order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));

		order.setIsHolidayBooking("NO");
		order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER)
				.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientLanguage"));
		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT)
				.getString("clientMarket"));

		// TODO: Later check what other details we need to populate for booking table.
		// Also confirm whther BE will get those additional details from Redis.
		order.setPaymentInfo(readPaymentInfo(
				bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO), order));

		return order;
	}

	private Set<PaymentInfo> readPaymentInfo(JSONArray PaymentInfo, Booking booking) {

		Set<PaymentInfo> paymentInfoSet = new HashSet<PaymentInfo>();

		for (int i = 0; i < PaymentInfo.length(); i++) {
			PaymentInfo paymentInfo = new PaymentInfo();
			JSONObject currentPaymentInfo = PaymentInfo.getJSONObject(i);
			paymentInfo.setPaymentMethod(currentPaymentInfo.getString(JSON_PROP_PAYMENTMETHOD));
			paymentInfo.setPaymentAmount(currentPaymentInfo.getString(JSON_PROP_PAYMENTAMOUNT));
			paymentInfo.setPaymentType(currentPaymentInfo.getString(JSON_PROP_PAYMENTTYPE));
			paymentInfo.setAmountCurrency(currentPaymentInfo.getString(JSON_PROP_AMOUNTCURRENCY));
			paymentInfo.setCardType(currentPaymentInfo.getString(JSON_PROP_CARDTYPE));
			paymentInfo.setCardNumber(currentPaymentInfo.getString(JSON_PROP_CARDNO));
			paymentInfo.setCardExpiry(currentPaymentInfo.getString(JSON_PROP_CARDEXPIRY));
			paymentInfo.setEncryptionKey(currentPaymentInfo.getString(JSON_PROP_ENCRYPTIONKEY));
			paymentInfo.setToken(currentPaymentInfo.getString(JSON_PROP_TOKEN));
			paymentInfo.setAccountType(currentPaymentInfo.getString(JSON_PROP_ACCOUNTTYPE));
			paymentInfo.setBooking(booking);
			paymentInfoSet.add(paymentInfo);

		}
		return paymentInfoSet;
	}

	public String processBookResponse(JSONObject bookResponseJson) {

		Booking booking = bookingRepository
				.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		if (booking == null) {
			myLogger.warn(String.format(
					"CAR Booking Response could not be populated since no bookings found for req with bookID %s",
					bookResponseJson.getJSONObject("responseBody").getString("bookID")));
			response.put("ErrorCode", "BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		List<CarOrders> orders = carRepository.findByBooking(booking);
		if (orders.size() == 0) {
			myLogger.warn(String.format(
					"Car Booking Response could not be populated since no car orders found for req with bookID %s",
					bookResponseJson.getJSONObject("responseBody").getString("bookID")));
			response.put("ErrorCode", "BE_ERR_CAR_005");
			response.put("ErrorMsg", BE_ERR_CAR_005);
			return response.toString();
		}
		int count = 0;
		JSONArray reservationArr = bookResponseJson.getJSONObject(JSON_PROP_RESBODY)
				.getJSONArray(JSON_PROP_CAR_RESERVATION);
		for (CarOrders order : orders) {
			order.setStatus("confirmed");
			// TODO : May Need to Change Later
			String bookId = "";
			JSONArray suppBookRef = reservationArr.getJSONObject(count).getJSONArray(JSON_PROP_SUPPBOOKREF);
			for (Object bookRef : suppBookRef) {
				if (((JSONObject) bookRef).getString("type").equals("14")) {
					bookId = ((JSONObject) bookRef).getString("id");
					break;
				}
			}
			order.setSuppBookRef(bookId);
			order.setBookingDateTime(new Date().toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			count++;
			carRepository.save(order);
		}
		myLogger.info(String.format("Car Booking Response populated successfully for req with bookID %s = %s",
				bookResponseJson.getJSONObject("responseBody").getString("bookID"), bookResponseJson.toString()));
		return "SUCCESS";

	}

	// This is to process cancel/amend request for Car
	public String processAmClRequest(JSONObject modifyReq) throws BookingEngineDBException {

		for (Object orderJson : modifyReq.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_CAR_CARRENTALARR)) {
			
			JSONObject reqJson = (JSONObject) orderJson;
			String type = reqJson.getString("type");
			// TODO: Check if order level status needs to be updated for each request

			CarAmCl amClEntry = new CarAmCl();
			amClEntry.setEntityID(reqJson.getString("entityId"));
			amClEntry.setEntityName(reqJson.getString("entityName"));
			amClEntry.setRequestType(reqJson.getString("requestType"));
			amClEntry.setSupplierCharges("0");
			amClEntry.setDescription(type);
			amClEntry.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			amClEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			amClEntry.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			amClEntry.setStatus("OnRequest");
			saveCarAmCl(amClEntry, "");

			switch (type) {
			
				case JSON_PROP_CAR_TYPE_ADDANCILLARY:
					return addAncillary(modifyReq, reqJson);
				case JSON_PROP_CAR_TYPE_REMOVEANCILLARY:
					return removeAncillary(modifyReq, reqJson);
				case JSON_PROP_CAR_TYPE_UPDATEPAXINFO:
					return updatePaxDetails(modifyReq, reqJson, type);
				case JSON_PROP_CAR_TYPE_CHAGERENTALINFO:
					return changeRentalInfo(modifyReq, reqJson, type);
				case JSON_PROP_CAR_TYPE_UPGRADECAR:
					return upgradeCar(modifyReq, reqJson, type);
				case JSON_PROP_CAR_TYPE_FULLCANCEL:
					return fullCancel(modifyReq, reqJson);
				default:
					return "no match for cancel/amend type";
			}
		}
		return "SUCCESS";
	}

	private String upgradeCar(JSONObject modifyReq, JSONObject reqJson, String type) throws BookingEngineDBException {
		
		CarOrders order = carRepository.findOne(reqJson.getString("entityId"));
		String prevOrder = order.toString();
		order.setStatus("Amended");
		order.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		JSONObject vehicleInfo = reqJson.getJSONObject(JSON_PROP_CAR_VEHICLEINFO);
		vehicleInfo.put(JSON_PROP_STATUS, "Amended");
		order.setCarDetails(vehicleInfo.toString());
		
		saveOrder(order, prevOrder);
		return "SUCCESS";
		
		
	}

	private String changeRentalInfo(JSONObject modifyReq, JSONObject reqJson, String type) throws BookingEngineDBException {
		
		CarOrders order = carRepository.findOne(reqJson.getString("entityId"));
		String prevOrder = order.toString();
		order.setStatus("Amended");
		order.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		JSONObject rentalInfo = new JSONObject(order.getRentalDetails());
		rentalInfo.put(JSON_PROP_CAR_PICKUPDATE, reqJson.getString(JSON_PROP_CAR_PICKUPDATE));
		rentalInfo.put(JSON_PROP_CAR_PICKUPLOCCODE,  reqJson.getString(JSON_PROP_CAR_PICKUPLOCCODE));
		rentalInfo.put(JSON_PROP_CAR_RETURNDATE,  reqJson.getString(JSON_PROP_CAR_RETURNDATE));
		rentalInfo.put(JSON_PROP_CAR_RETURNLOCCODE,  reqJson.getString(JSON_PROP_CAR_RETURNLOCCODE));
		rentalInfo.put("city", reqJson.getString("city"));
		rentalInfo.put(JSON_PROP_STATUS, "Amended");
		
		order.setRentalDetails(rentalInfo.toString());
		
		saveOrder(order, prevOrder);
		return "SUCCESS";
		
	}

	public String processAmClResponse(JSONObject resJson) {

		for (Object orderJson : resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_CAR_CARRENTALARR)) {
			
			JSONObject subRes = (JSONObject) orderJson;
			List<CarAmCl> amendEntries = carAmClRepository.findforResponseUpdate(
					subRes.getString("entityName"),
					subRes.getString("entityId"),
					subRes.getString("type"),
					subRes.getString("requestType"));
	
			if (amendEntries.size() == 0) {
				// TODO: handle this before it goes in prod
				System.out.println("no amend entry found. Request might not have been populated");
			}
	
			else if (amendEntries.size() > 1) {
				// TODO: handle this before it goes in prod
				System.out.println("multiple amend entries found. Dont know which one to update");
			}
	
			else {
				CarAmCl amendEntry = amendEntries.get(0);
				String prevOrder = amendEntry.toString();
				amendEntry.setCompanyCharges(subRes.optString("companyCharges"));
				amendEntry.setSupplierCharges(subRes.optString("supplierCharges"));
				amendEntry.setSupplierChargesCurrencyCode(subRes.optString("supplierChargesCurrencyCode"));
				amendEntry.setCompanyChargesCurrencyCode(subRes.optString("companyChargesCurrencyCode"));
				amendEntry.setStatus("Confirmed");
				amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
				amendEntry.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
				
				
				JSONObject totalFare = subRes.getJSONObject(JSON_PROP_CAR_TOTALPRICEINFO).getJSONObject(JSON_PROP_CAR_TOTALFARE);
				
				amendEntry.setTotalBaseFare(totalFare.getJSONObject(JSON_PROP_BASEFARE).toString());
				amendEntry.setTotalPrice(totalFare.getBigDecimal(JSON_PROP_AMOUNT).toString());
				amendEntry.setTotalPriceCurrencyCode(totalFare.getString(JSON_PROP_CURRENCYCODE).toString());
				amendEntry.setTotalPriceFees(totalFare.getJSONObject(JSON_PROP_FEES).toString());
				amendEntry.setTotalPriceTaxes(totalFare.getJSONObject(JSON_PROP_TAXES).toString());

				amendEntry.setPricedCoverages(totalFare.getJSONObject(JSON_PROP_CAR_PRICEDCOVERAGES).toString());
				amendEntry.setExtraEquipments(totalFare.getJSONObject(JSON_PROP_CAR_SPLEQUIPS).toString());
				
				
				// TODO: also set the currency codes and breakups before saving
				saveCarAmCl(amendEntry, prevOrder);
			}
		}
		return "SUCCESS";

	}

	private String fullCancel(JSONObject modifyReq, JSONObject reqJson) throws BookingEngineDBException{

		CarOrders order = carRepository.findOne(reqJson.getString("entityId"));
		String prevOrder = order.toString();
		order.setStatus("Cancelled");
		order.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

		saveOrder(order, prevOrder);
		return "SUCCESS";
	}

	private String addAncillary(JSONObject modifyReq, JSONObject reqJson) throws BookingEngineDBException{

		CarOrders order = carRepository.findOne(reqJson.getString("entityId"));
		String prevOrder = order.toString();
		order.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus("Amended");
		
		JSONArray reqEquipsArr = reqJson.optJSONArray(JSON_PROP_CAR_SPLEQUIPS);
		JSONArray pricedCovrgsArr = reqJson.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGES);
		//Adding SpecialEquipments
		if(reqEquipsArr!=null) {
			JSONObject extraEquip = new JSONObject(order.getExtraEquipments());
			JSONArray dBEquipsArr = extraEquip.optJSONArray(JSON_PROP_CAR_SPLEQUIP)!=null?
					extraEquip.getJSONArray(JSON_PROP_CAR_SPLEQUIP): new JSONArray();
		
			for(int i=0;i<reqEquipsArr.length();i++) {
				JSONObject reqEquipJson = reqEquipsArr.getJSONObject(i);
				String equipType = reqEquipJson.getString("equipType");
				boolean isPresent = false;
				for(int j=0;j<dBEquipsArr.length();j++) {
					JSONObject dBEquipJson = dBEquipsArr.getJSONObject(j);
					//If already present increase quantity
					if(dBEquipJson.getString("equipType").equalsIgnoreCase(equipType)) {
						Integer quantity = dBEquipJson.getInt(JSON_PROP_QTY);
						dBEquipJson.put(JSON_PROP_QTY, reqEquipJson.optInt(JSON_PROP_QTY, 1)+quantity);
						dBEquipJson.put(JSON_PROP_STATUS, "Amended");
						isPresent = true;
					}
				}
				if(isPresent==false) {
					reqEquipJson.put(JSON_PROP_STATUS, "Added");
					dBEquipsArr.put(reqEquipJson);
				}
			}
			order.setExtraEquipments(extraEquip.toString());
		}
		//Adding PricedCoverages
		if(pricedCovrgsArr!=null) {
			JSONObject pricedCovrgs = new JSONObject(order.getExtraEquipments());
			JSONArray dBpricedCovrgArr = pricedCovrgs.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE)!=null?
					pricedCovrgs.getJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE): new JSONArray();
		
			for(int i=0;i<pricedCovrgsArr.length();i++) {
				JSONObject pricedCovrgsJson = pricedCovrgsArr.getJSONObject(i);
				String coverageType = pricedCovrgsJson.getString("coverageType");
				boolean isPresent = false;
				for(int j=0;j<dBpricedCovrgArr.length();j++) {
					JSONObject dBpricedCovrgJson = dBpricedCovrgArr.getJSONObject(j);
					if(dBpricedCovrgJson.getString("coverageType").equalsIgnoreCase(coverageType)) {
						isPresent = true;
					}
				}
				//If not already Present then Coverage is added
				if(isPresent==false) {
					pricedCovrgsJson.put(JSON_PROP_STATUS, "Added");
					dBpricedCovrgArr.put(pricedCovrgsJson);
				}
			}
			order.setPricedCoverages(pricedCovrgs.toString());
		}

		saveOrder(order, prevOrder);
		return "SUCCESS";
	}

	private String removeAncillary(JSONObject modifyReq, JSONObject reqJson) throws BookingEngineDBException{

		CarOrders order = carRepository.findOne(reqJson.getString("entityId"));
		String prevOrder = order.toString();
		order.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus("Amended");
		
		JSONArray reqEquipsArr = reqJson.optJSONArray(JSON_PROP_CAR_SPLEQUIPS);
		JSONArray pricedCovrgsArr = reqJson.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGES);
		//Removing SpecialEquipments
		if(reqEquipsArr!=null) {
			JSONObject extraEquip = new JSONObject(order.getExtraEquipments());
			JSONArray dBEquipsArr = extraEquip.optJSONArray(JSON_PROP_CAR_SPLEQUIP)!=null?
					extraEquip.getJSONArray(JSON_PROP_CAR_SPLEQUIP): new JSONArray();
		
			for(int i=0;i<reqEquipsArr.length();i++) {
				JSONObject reqEquipJson = reqEquipsArr.getJSONObject(i);
				String equipType = reqEquipJson.getString("equipType");
				Boolean isPresent = false;
				for(int j=0;j<dBEquipsArr.length();j++) {
					JSONObject dBEquipJson = dBEquipsArr.getJSONObject(j);
					//If already present increase quantity
					if(dBEquipJson.getString("equipType").equalsIgnoreCase(equipType)) {
						Integer quantity = dBEquipJson.getInt(JSON_PROP_QTY);
						if(reqEquipJson.optInt(JSON_PROP_QTY)==0)
							dBEquipJson.put(JSON_PROP_STATUS, "Cancelled");
						else {
							Integer newQuantity = quantity-reqEquipJson.getInt(JSON_PROP_QTY);
							dBEquipJson.put(JSON_PROP_QTY, newQuantity<0 ? 0 : newQuantity);
							dBEquipJson.put(JSON_PROP_STATUS, "Amended");
						}
						isPresent = true;
					}
				}
				if(isPresent==false) {
					myLogger.info(String.format("Equipment %s cannot be removed as it was not added", equipType));
				}
			}
			order.setExtraEquipments(extraEquip.toString());
		}
		//Removing PricedCoverages
		if(pricedCovrgsArr!=null) {
			JSONObject pricedCovrgs = new JSONObject(order.getExtraEquipments());
			JSONArray dBpricedCovrgArr = pricedCovrgs.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE)!=null?
					pricedCovrgs.getJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE): new JSONArray();
		
			for(int i=0;i<pricedCovrgsArr.length();i++) {
				JSONObject pricedCovrgsJson = pricedCovrgsArr.getJSONObject(i);
				String coverageType = pricedCovrgsJson.getString("coverageType");
				boolean isPresent = false;
				for(int j=0;j<dBpricedCovrgArr.length();j++) {
					JSONObject dBpricedCovrgJson = dBpricedCovrgArr.getJSONObject(j);
					if(dBpricedCovrgJson.getString("coverageType").equalsIgnoreCase(coverageType)) {
						pricedCovrgsJson.put("status", "Cancelled");
						isPresent = true;
					}
				}
				//If not already Present then Coverage is added
				if(isPresent==false) {
					myLogger.info(String.format("Coverage %s cannot be removed as it is not added", coverageType));
				}
			}
			order.setPricedCoverages(pricedCovrgs.toString());
		}

		saveOrder(order, prevOrder);
		return "SUCCESS";
	}

	// TODO: Check for what statuses we need to have in pax table, Also check if we
	// need to update room table'ss status as well here.
	private String updatePaxDetails(JSONObject modifyReq, JSONObject reqJson, String type) throws BookingEngineDBException {
		
		String prevOrder;
		JSONArray paxArr = reqJson.getJSONArray(JSON_PROP_PAXINFO);
		for (int i = 0; i < paxArr.length(); i++) {

			JSONObject currentPaxDetails = paxArr.getJSONObject(i);
			PassengerDetails paxDetails;
			if (type.equals(JSON_PROP_CAR_TYPE_ADDDRIVER)) {
				paxDetails = new PassengerDetails();
				paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
				paxDetails.setMiddleName(currentPaxDetails.getString(JSON_PROP_MIDDLENAME));
				paxDetails.setLastName(currentPaxDetails.getString(JSON_PROP_SURNAME));
				prevOrder = "";
				paxDetails.setStatus("Added");
				paxDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			} else if (type.equals(JSON_PROP_CAR_TYPE_UPDATEPAXINFO)) {
				paxDetails = passengerRepository.findOne(currentPaxDetails.getString("paxId"));
				prevOrder = paxDetails.toString();
				//Suppliers does not allow to update 
				//First Name and Last Name
				paxDetails.setStatus("Updated");
			} else {
				paxDetails = passengerRepository.findOne(currentPaxDetails.getString("paxId"));
				paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
				paxDetails.setMiddleName(currentPaxDetails.getString(JSON_PROP_MIDDLENAME));
				paxDetails.setLastName(currentPaxDetails.getString(JSON_PROP_SURNAME));
				prevOrder = paxDetails.toString();
				paxDetails.setStatus("Cancelled");
			}

			paxDetails.setTitle(currentPaxDetails.getString(JSON_PROP_TITLE));
			
			paxDetails.setBirthDate(currentPaxDetails.getString(JSON_PROP_DOB));
			paxDetails.setIsLeadPax(currentPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			paxDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			paxDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
			
			paxDetails.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));

			paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			savePaxDetails(paxDetails, prevOrder);
		}
		return "SUCCESS";
	}

	public Booking saveBookingOrder(Booking order, String prevOrder) throws BookingEngineDBException {
		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);
		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Car Booking object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Car Booking object");
		}
		return bookingRepository.saveOrder(orderObj, prevOrder);
	}

	public CarAmCl saveCarAmCl(CarAmCl currentOrder, String prevOrder) {
		CarAmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, CarAmCl.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return carAmClRepository.saveOrder(orderObj, prevOrder);
	}

	private CarOrders saveOrder(CarOrders order, String prevOrder) throws BookingEngineDBException {
		CarOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, CarOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Car order object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save car order object");
		}
		return carRepository.saveOrder(orderObj, prevOrder);
	}

	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails)
			throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Car passenger object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Car passenger object");
		}
		return passengerRepository.saveOrder(orderObj, prevPaxDetails);
	}

}
