package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoRoomRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;



@Service
@Qualifier("Acco")
@Transactional(readOnly = false)
public class AccoDataBaseServiceImpl implements DataBaseService,Constants,ErrorConstants,TestDbService {

	@Autowired
	@Qualifier("Acco")
	private AccoDatabaseRepository accoRepository;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository accoAmClRepository;
	
	@Qualifier("AccoRoom")
	@Autowired
	private AccoRoomRepository roomRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject(); 
	
	public boolean isResponsibleFor(String product) {
		return "ACCO".equalsIgnoreCase(product);
	}

	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {

		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		booking = populateBookingData(bookRequestJson);
		
		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ACCO_ACCOMODATIONINFO)) {

			AccoOrders order = populateAccoData((JSONObject) orderJson, bookRequestHeader,
					booking);
			saveAccoOrder(order,"");
			
		}
		myLogger.info(String.format("Acco Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";

	}

	public AccoOrders populateAccoData(JSONObject accoInfo, JSONObject bookRequestHeader, Booking booking) throws BookingEngineDBException {

		try {
		AccoOrders order = new AccoOrders();
		
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setBooking(booking);
		order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setStatus("OnRequest");
		
		order.setSupplierID(accoInfo.getString(JSON_PROP_SUPPREF));
		order.setOperationType("insert");
		
		order.setSupplierPrice(accoInfo.getJSONObject("supplierBookingPriceInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(accoInfo.getJSONObject("supplierBookingPriceInfo").getString(JSON_PROP_CURRENCYCODE));
		order.setTotalPrice(accoInfo.getJSONObject("bookingPriceInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(accoInfo.getJSONObject("bookingPriceInfo").getString(JSON_PROP_CURRENCYCODE));

		order.setSuppPriceTaxes(accoInfo.getJSONObject("supplierBookingPriceInfo").getJSONObject(JSON_PROP_TAXES).toString());
		order.setTotalPriceTaxes(accoInfo.getJSONObject("bookingPriceInfo").getJSONObject(JSON_PROP_TAXES).toString());
		
		
		//TODO: check if we need to put taxes as well here
		

		Set<AccoRoomDetails> setRoomDetails = new HashSet<AccoRoomDetails>();
		setRoomDetails = readRoomDetails(accoInfo, order, booking);
		Set<SupplierCommercial> setSuppComms = new HashSet<SupplierCommercial>();
		setSuppComms = readSuppCommercials(accoInfo, order);
		
		
		Set<ClientCommercial> setClientComms = new HashSet<ClientCommercial>();
		setClientComms = readClientCommercials(accoInfo.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS), order);
		
		order.setClientCommercial(setClientComms);
		order.setSuppcommercial(setSuppComms);
		
		order.setRoomDetails(setRoomDetails);

		return order;
		}
		catch(Exception e)
		{
			
			myLogger.fatal("Failed to populate Acco Data "+ e);
			throw new BookingEngineDBException("Failed to populate Acco Data");
		}
	}

	private Set<AccoRoomDetails> readRoomDetails(JSONObject requestBody, AccoOrders accoOrder, Booking booking) throws BookingEngineDBException {

		JSONArray roomConfigJsonArray = requestBody.getJSONArray(JSON_PROP_ACCO_ROOMCONFIG);
		Set<AccoRoomDetails> roomDetailsSet = new HashSet<AccoRoomDetails>();
		AccoRoomDetails roomDetails;

		for (int i = 0; i < roomConfigJsonArray.length(); i++) {
			roomDetails = new AccoRoomDetails();
                        JSONObject currentRoomDetails = roomConfigJsonArray.getJSONObject(i);
			
                        roomDetails.setCheckInDate(requestBody.getString(JSON_PROP_ACCO_CHKIN));
			roomDetails.setCheckOutDate(requestBody.getString(JSON_PROP_ACCO_CHKOUT));
			roomDetails.setCityCode(requestBody.getString(JSON_PROP_CITYCODE));
			roomDetails.setCountryCode(requestBody.getString(JSON_PROP_COUNTRYCODE));
			roomDetails.setSupplierName(requestBody.getString(JSON_PROP_SUPPREF));
			roomDetails.setStatus("OnRequest");
			roomDetails.setMealCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_MEALINFO).getString(JSON_PROP_ACCO_MEALCODE));
			roomDetails.setMealName(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_MEALINFO).getString(JSON_PROP_ACCO_MEALNAME));
			roomDetails.setRoomCategoryID(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMCATEGORYCODE));
			roomDetails.setRoomCategoryName(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMCATEGNAME));
			roomDetails.setRoomRef(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO).getString(JSON_PROP_ACCO_ROOMREF));
			roomDetails.setRoomTypeCode(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMTYPECODE));
			roomDetails.setRoomTypeName(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMTYPENAME));
			roomDetails.setHotelCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_HOTELINFO).getString(JSON_PROP_ACCO_HOTELCODE));
			roomDetails.setHotelName(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_HOTELINFO).getString(JSON_PROP_ACCO_HOTELNAME));
			roomDetails.setRatePlanName(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO)
					.getString(JSON_PROP_ACCO_RATEPLANCODE));
			roomDetails.setRatePlanCode(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO)
					.getString(JSON_PROP_ACCO_RATEPLANCODE));
			roomDetails.setRatePlanRef(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO)
					.getString(JSON_PROP_ACCO_RATEPLANREF));
			roomDetails.setBookingRef(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
			
			Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
			setGuestDetails = readGuestDetails(currentRoomDetails, roomDetails);
			
			JSONArray paxIds = new JSONArray();
			for(PassengerDetails pax:setGuestDetails ) {
				JSONObject paxJson = new JSONObject();
				paxJson.put("paxId", pax.getPassanger_id());
				paxIds.put(paxJson);
			}

			roomDetails.setPaxDetails(paxIds.toString());
			
			roomDetails.setTotalPrice(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
			roomDetails.setTotalPriceCurrencyCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			roomDetails.setTotalTaxBreakup(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());
			roomDetails.setSupplierPrice(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMSUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
			roomDetails.setSupplierPriceCurrencyCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMSUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			
			roomDetails.setSupplierTaxBreakup(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMSUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES)
					.toString());
			
			roomDetails.setSuppCommercials(currentRoomDetails.getJSONArray(JSON_PROP_SUPPCOMM).toString());
			roomDetails.setClientCommercials(currentRoomDetails.getJSONArray(JSON_PROP_CLIENTCOMM).toString());

			
			roomDetails.setAccoOrders(accoOrder);
			
			roomDetailsSet.add(roomDetails);
		}
		return roomDetailsSet;
	}

	private Set<PassengerDetails> readGuestDetails(JSONObject roomConfigJson, AccoRoomDetails roomDetails) throws BookingEngineDBException {

		JSONArray guestRoomJsonArray = roomConfigJson.getJSONArray(JSON_PROP_PAXINFO);

		Set<PassengerDetails> guestDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails guestDetails;
		for (int i = 0; i < guestRoomJsonArray.length(); i++) {
			JSONObject currenntPaxDetails = guestRoomJsonArray.getJSONObject(i);
			guestDetails = new PassengerDetails();
			
			//TODO: Put a logic to create the primary key for pax
			
			guestDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			guestDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME));
			guestDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME));
			guestDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
			guestDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
			guestDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			guestDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE));
			guestDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			guestDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

	/*		if (currenntPaxDetails.getString(JSON_PROP_PAX_TYPE).equals(Pax_ADT))
				guestDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ACCO_DOCUMENTDETAILS).toString());
			        guestDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());*/
                        //TODO: to be checked whether we are going to get the special requests for ACCO
			//guestDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
                        // TODO:change it to userID later
			guestDetails.setLastModifiedBy("");
			guestDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			savePaxDetails(guestDetails,"");

			guestDetailsSet.add(guestDetails);

		}
		return guestDetailsSet;
	}

	private Set<SupplierCommercial> readSuppCommercials(JSONObject accoInfoJson, AccoOrders order) {

		JSONArray suppCommsJsonArray = accoInfoJson.getJSONArray(JSON_PROP_SUPPCOMMTOTALS);
		Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		for (int i = 0; i < suppCommsJsonArray.length(); i++) {
			JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

			suppCommercials = new SupplierCommercial();
			suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
			suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
			suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
			suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
			
			suppCommercials.setProduct(JSON_PROP_PRODUCTACCO);
			suppCommercials.setOrder(order);
			suppCommercialsSet.add(suppCommercials);

		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, AccoOrders order) {
		 
		Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;
		
		for(int i=0;i<clientCommsJsonArray.length();i++)	{
			
			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
			
			 String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			 String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;		
			 String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;		
			 String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
			
			 boolean companyFlag = (i==0)?true:false;
			
		
		JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_CLIENTCOMMERCIALSTOTAL);
		
		for(int j=0;j<clientComms.length();j++) {
		
		JSONObject clientComm = clientComms.getJSONObject(j);
		
		clientCommercials =new ClientCommercial();
		clientCommercials.setCommercialName(clientComm.getString(JSON_PROP_COMMERCIALNAME));
		clientCommercials.setCommercialType(clientComm.getString(JSON_PROP_COMMERCIALTYPE));
		clientCommercials.setCommercialAmount(clientComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		clientCommercials.setCommercialCurrency(clientComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		clientCommercials.setClientID(clientID);
		clientCommercials.setParentClientID(parentClientID);
		clientCommercials.setCommercialEntityType(commercialEntityType);
		clientCommercials.setCommercialEntityID(commercialEntityID);
		clientCommercials.setCompanyFlag(companyFlag);

		clientCommercials.setProduct(JSON_PROP_PRODUCTAIR);
		clientCommercials.setOrder(order);
		clientCommercialsSet.add(clientCommercials);
		}
		}
		return clientCommercialsSet;
		}

	private Booking populateBookingData(JSONObject bookRequestJson) throws BookingEngineDBException {
		try {
		Booking order =new Booking();
		order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		order.setStatus("OnRequest");
		
		order.setLastModifiedBy(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
		order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));
		order.setIsHolidayBooking("NO");
		order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTLANGUAGE));
		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET));
		
		//TODO: Later check what other details we need to populate for booking table. Also confirm whther BE will get those additional details from Redis.
	
		order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));
		
		
		
		return order;
	}
		catch(Exception e)
		{
			myLogger.fatal("Failed to populate Booking Data "+ e);
			throw new BookingEngineDBException("Failed to populate Booking Data");
		}
	}
	
	//TODO: WEM needs to confirm what all info they are going to pass to BE
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

	public String processBookResponse(JSONObject bookResponseJson) throws BookingEngineDBException {

		//TODO: We need to put logic to update status for booking based on the statuses of individual products.
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		if(booking==null)
		{
			myLogger.warn(String.format("Acco Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject("responseBody").getString("bookID")));
			response.put("ErrorCode","BE_ERR_ACCO_004");
			response.put("ErrorMsg", BE_ERR_ACCO_004);
			return response.toString();
		}
		else
		{
		List<AccoOrders> orders = accoRepository.findByBooking(booking);
		if(orders.size()==0)
		{
			myLogger.warn(String.format("Acco Booking Response could not be populated since no acco orders found for req with bookID %s", bookResponseJson.getJSONObject("responseBody").getString("bookID")));
			response.put("ErrorCode", "BE_ERR_ACCO_005");
			response.put("ErrorMsg", BE_ERR_ACCO_005);
			return response.toString();
		}
		else
		{
		int count =0;
		for(AccoOrders order:orders) {
			String prevOrder = order.toString();
			order.setStatus("confirmed");
			order.setSupp_booking_reference(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES).getJSONObject(count).getString(JSON_PROP_BOOKREFID));
			count++;
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			
			for(AccoRoomDetails room: order.getRoomDetails()) {
				
				room.setStatus("Confirmed");
			}
		     saveAccoOrder(order, prevOrder);
		}
		myLogger.info(String.format("Acco Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject("responseBody").getString("bookID"),bookResponseJson.toString()));
		return "SUCCESS";
		}
		}
	}
	
	//This is to process cancel/amend request for Acco
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {

		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
		//TODO: Check if order level status needs to be updated for each request
		
		AmCl amendEntry = new AmCl();
		amendEntry.setEntityID(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		amendEntry.setOrderID(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("orderId"));
		amendEntry.setEntityName(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityName"));
		amendEntry.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("requestType"));
		amendEntry.setSupplierCharges("0");
		amendEntry.setDescription(type);
		amendEntry.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		amendEntry.setStatus("OnRequest");
		saveAccoAmCl(amendEntry, "");
		
		 switch(type)
	        {
	           case JSON_PROP_ACCO_CANNCELTYPE_ADDPAX:
	                return updatePaxDetails(reqJson,type);
	        	case JSON_PROP_ACCO_CANNCELTYPE_CANCELPAX:
	        		return updatePaxDetails(reqJson,type);
	            case JSON_PROP_ACCO_CANNCELTYPE_UPDATEPAX:
	            	return updatePaxDetails(reqJson,type);
	            case JSON_PROP_ACCO_CANNCELTYPE_UPDATEROOM:
	                 return updateRoom(reqJson,type);	     
	            /*case JSON_PROP_ACCO_CANNCELTYPE_UPDATESTAYDATES:
	                 return updateStayDates(reqJson); */
	        	case  JSON_PROP_ACCO_CANNCELTYPE_CANCELROOM :
               return updateRoom(reqJson,type);
	        	case JSON_PROP_ACCO_CANNCELTYPE_FULLCANCEL:
	                return fullCancel(reqJson);
	                
	            default:
	                return "no match for cancel/amend type";
	        }	
		
	}
	
	//TODO: for add pax how will we get entity ID
	public String processAmClResponse(JSONObject reqJson) {
		
		List<AmCl> amendEntries  = accoAmClRepository.findforResponseUpdate(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),reqJson.getJSONObject(JSON_PROP_RESBODY).getString("entityId"), reqJson.getJSONObject(JSON_PROP_RESBODY).getString("type"), reqJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
		
		if(amendEntries.size()==0) {
			//TODO: handle this before it goes in prod
			System.out.println("no amend entry found. Request might not have been populated");
		}
		
		else if(amendEntries.size()>1) {
			//TODO: handle this before it goes in prod
			System.out.println("multiple amend entries found. Dont know which one to update");
		}
		
		else {
		AmCl amendEntry = amendEntries.get(0);	
		String prevOrder = amendEntry.toString();
		amendEntry.setCompanyCharges(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("companyCharges"));
		amendEntry.setSupplierCharges(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("supplierCharges"));
		amendEntry.setSupplierChargesCurrencyCode(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("supplierChargesCurrencyCode"));
		amendEntry.setCompanyChargesCurrencyCode(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("companyChargesCurrencyCode"));
		amendEntry.setStatus("Confirmed");
		amendEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
		
		//TODO: also set the currency codes and breakups before saving
		saveAccoAmCl(amendEntry, prevOrder);
		}
		return "SUCCESS";
		
	}
	
	private String fullCancel(JSONObject reqJson) {
		
		AccoOrders order = accoRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		String prevOrder = order.toString();
		order.setStatus("Cancelled");
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		saveAccoOrder(order, prevOrder);
		return "SUCCESS";
	}

	//TODO: check what status we need to have in room table
	private String updateRoom(JSONObject reqJson, String type) throws BookingEngineDBException {
		AccoRoomDetails room = roomRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));	
		String prevOrder = room.toString();

		if(type.equals(JSON_PROP_ACCO_CANNCELTYPE_UPDATEROOM)) {
		room.setRoomTypeCode(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("roomTypeCode"));
		room.setRatePlanCode(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("ratePlanCode"));
		room.setStatus("Amended");
		room.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		}
		else {
		
			room.setStatus("Cancelled");
			room.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		}	
		
			saveRoomDetails(room, prevOrder);
		
		
		return "SUCCESS";
	}

	//TODO: Check for what statuses we need to have in pax table, Also check if we need to update room table'ss status as well here.
	private String updatePaxDetails(JSONObject reqJson, String type) throws BookingEngineDBException {
		AccoRoomDetails room = roomRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		String prevOrder;
		
		JSONArray guestRoomJsonArray = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXINFO);
		for (int i = 0; i < guestRoomJsonArray.length(); i++) {
			
		JSONObject currenntPaxDetails = guestRoomJsonArray.getJSONObject(i);

		PassengerDetails guestDetails;	

		if(type.equals(JSON_PROP_ACCO_CANNCELTYPE_ADDPAX)) {
			guestDetails = new PassengerDetails();
			prevOrder = "";
			guestDetails.setStatus("Added");
			guestDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			guestDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			guestDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME));
			guestDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME));
			guestDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
			guestDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
			guestDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			guestDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE));
			guestDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			guestDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

	/*		if (currenntPaxDetails.getString(JSON_PROP_PAX_TYPE).equals(Pax_ADT))
				guestDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ACCO_DOCUMENTDETAILS).toString());
			        guestDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());*/
	                    //TODO: to be checked whether we are going to get the special requests for ACCO
			//guestDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
	                    // TODO:change it to userID later
			guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			savePaxDetails(guestDetails,prevOrder);
		}
		else if(type.equals(JSON_PROP_ACCO_CANNCELTYPE_UPDATEPAX)) {
			guestDetails = passengerRepository.findOne(currenntPaxDetails.getString("paxid"));
			prevOrder = guestDetails.toString();
			guestDetails.setStatus("Updated");
			guestDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			guestDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME));
			guestDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME));
			guestDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
			guestDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
			guestDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			guestDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE));
			guestDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			guestDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

	/*		if (currenntPaxDetails.getString(JSON_PROP_PAX_TYPE).equals(Pax_ADT))
				guestDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ACCO_DOCUMENTDETAILS).toString());
			        guestDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());*/
	                    //TODO: to be checked whether we are going to get the special requests for ACCO
			//guestDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
	                    // TODO:change it to userID later
			guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			savePaxDetails(guestDetails,prevOrder);
		}
		else {
			guestDetails = passengerRepository.findOne(currenntPaxDetails.getString("paxid"));
			prevOrder = guestDetails.toString();
			guestDetails.setStatus("Cancelled");
		}
		
		
		}	
		return "SUCCESS";
	}
	
	
	public AccoOrders saveAccoOrder(AccoOrders currentOrder, String prevOrder) {
		AccoOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AccoOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return accoRepository.saveOrder(orderObj, prevOrder);
	}
	
	public AmCl saveAccoAmCl(AmCl currentOrder, String prevOrder) {
		AmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AmCl.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return accoAmClRepository.saveOrder(orderObj, prevOrder);
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException 
	{
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco Passenger order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return passengerRepository.saveOrder(orderObj,prevOrder);
	}
	
	private AccoRoomDetails saveRoomDetails(AccoRoomDetails room, String prevOrder) throws BookingEngineDBException 
	{
		AccoRoomDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(room, AccoRoomDetails.class);

		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco Room  object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save acco room object");
		}
		return roomRepository.saveOrder(orderObj,prevOrder);
	}
	
	
	
	
}
