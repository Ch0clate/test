package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;


@Service
@Transactional(readOnly = false)
public class BookingServiceImpl implements Constants,ErrorConstants{
	
	@Autowired
	private AccoBookingServiceImpl accoService;
	@Autowired
	private AirBookingServiceImpl airService;
	@Autowired
	private ActivitiesBookingServiceImpl activitiesService;
	@Autowired
	private BusBookingServiceImpl busService;
	@Autowired
	private CarBookingServiceImpl carService;
	@Autowired
	private TransfersBookingServiceImpl transfersService;
	@Autowired
	private CruiseBookingServiceImpl cruiseService;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookRepository;
        Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	JSONObject response=new JSONObject(); 
	
	
	
	
	public String getCancellationsByBookID(String bookID) {
		
		Booking booking = bookRepository.findOne(bookID);
		JSONObject resJson = new JSONObject();
		resJson.put(JSON_PROP_BOOKID, bookID);
		resJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
		JSONArray productsArray = new JSONArray();
		JSONArray accoArray = accoService.getCancellationsByBooking(booking);
		JSONArray airArray = airService.getCancellationsByBooking(booking);
		JSONArray busArray = busService.getCancellationsByBooking(booking);
		for(int i=0;i<accoArray.length();i++) {
			productsArray.put(accoArray.get(i));	
		}
		
		for(int i=0;i<airArray.length();i++)
		{
			productsArray.put(airArray.get(i));
		}
		for(int i=0;i<busArray.length();i++)
		{
			productsArray.put(busArray.get(i));
		}
		resJson.put("products", productsArray);
		return resJson.toString();
	}
	
	public String getAmendmentsByBookID(String bookID) {
		
		Booking booking = bookRepository.findOne(bookID);
		JSONObject resJson = new JSONObject();
		resJson.put(JSON_PROP_BOOKID, bookID);
		resJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
		JSONArray productsArray = new JSONArray();
		JSONArray accoArray = accoService.getAmendmentsByBooking(booking);
		JSONArray airArray = airService.getAmendmentsByBooking(booking);
		
		for(int i=0;i<accoArray.length();i++) {
			productsArray.put(accoArray.get(i));	
		}
		
		for(int i=0;i<airArray.length();i++) {
			productsArray.put(airArray.get(i));
		}
		
		resJson.put("products", productsArray);
		return resJson.toString();
	}
	
	public String getByBookID(String bookID, String flag) {

		Booking booking = bookRepository.findOne(bookID);
		if (booking == null) {
			response.put("ErrorCode", "BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			myLogger.warn(String.format("No booking details found for bookid  %s ",bookID));
			return (response.toString());
		}
		else
		{
		JSONObject resJson = new JSONObject();
		resJson.put(JSON_PROP_BOOKID, bookID);
		resJson.put("status", booking.getStatus());
		resJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
		resJson.put(JSON_PROP_CLIENTID, booking.getClientID());
		resJson.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
		resJson.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
		resJson.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());
		resJson.put(JSON_PROP_ISHOLIDAYBOOKING, booking.getIsHolidayBooking());
		resJson.put(JSON_PROP_SESSIONID, booking.getSessionID());
		resJson.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
		resJson.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));
		
		resJson.put("userID", booking.getUserID());
		resJson.put("clientLanguage", booking.getClientLanguage());
		resJson.put("clientMarket", booking.getClientMarket());
		resJson.put("clientNationality", booking.getClientNationality());
		resJson.put("company", booking.getCompany());
		
		
		

		JSONArray productsArray = new JSONArray();
		JSONArray accoArray = accoService.process(booking,flag);
		JSONArray airArray = airService.process(booking,flag);
		JSONArray activitiesArray = activitiesService.process(booking);
		JSONArray busArray  =busService.process(booking,flag);
		JSONArray carArray  = carService.process(booking, flag);
		JSONArray transfersArray  = transfersService.process(booking);
		JSONArray cruiseArray  = cruiseService.process(booking);
		
		for(int i=0;i<accoArray.length();i++) {
			productsArray.put(accoArray.get(i));	
		}
		for(int i=0;i<airArray.length();i++) {
			productsArray.put(airArray.get(i));	
		}
		for(int i=0;i<activitiesArray.length();i++) {
			productsArray.put(activitiesArray.get(i));	
		}
		for(int i=0;i<busArray.length();i++) {
			productsArray.put(busArray.get(i));	
		}
		for(int i=0;i<carArray.length();i++) {
			productsArray.put(carArray.get(i));	
		}
		for(int i=0;i<transfersArray.length();i++) {
			productsArray.put(transfersArray.get(i));	
		}
		for(int i=0;i<cruiseArray.length();i++) {
			productsArray.put(cruiseArray.get(i));	
		}
		resJson.put("products", productsArray);
        
		myLogger.info(String.format("Bookings retrieved for bookID %s = %s", bookID, resJson.toString()));
		return resJson.toString();
		}

	}

	
	
	private JSONArray getPaymentInfo(Booking booking) {
		JSONArray paymentArray =new JSONArray();
		JSONObject paymentJson = new JSONObject();
		for(PaymentInfo payment:booking.getPaymentInfo()) {
			paymentJson.put(JSON_PROP_PAYMENTID, payment.getPayment_info_id());
			paymentJson.put(JSON_PROP_PAYMENTMETHOD, payment.getPaymentMethod());
			paymentJson.put(JSON_PROP_PAYMENTAMOUNT, payment.getPaymentAmount());
			paymentJson.put(JSON_PROP_PAYMENTTYPE, payment.getPaymentType());
			paymentJson.put(JSON_PROP_AMOUNTCURRENCY, payment.getAmountCurrency());
			paymentJson.put(JSON_PROP_CARDTYPE, payment.getCardType());
			paymentJson.put(JSON_PROP_CARDNO, payment.getCardNumber());
			paymentJson.put(JSON_PROP_CARDEXPIRY, payment.getCardExpiry());
			paymentJson.put(JSON_PROP_ENCRYPTIONKEY, payment.getEncryptionKey());
			paymentJson.put(JSON_PROP_TOKEN, payment.getToken());
			paymentJson.put(JSON_PROP_ACCOUNTTYPE, payment.getAccountType());
			
			paymentArray.put(paymentJson);
		}
		return paymentArray;
	}

	public String getByUserID(String userID) {


		List<Booking> bookings = bookRepository.findByUserID(userID);
		if (bookings.size() == 0) {
			response.put("ErrorCode", "BE_ERR_002");
			response.put("ErrorMsg", BE_ERR_002);
			myLogger.warn(String.format("No booking details found for userId  %s ",userID));
			return response.toString();
		}
		else
		{
		JSONArray bookingArray = new JSONArray();
		
		for(Booking booking:bookings) {
		JSONObject bookingJson = new JSONObject();
		bookingJson.put(JSON_PROP_BOOKID, booking.getBookID());
		bookingJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
		bookingJson.put(JSON_PROP_CLIENTID, booking.getClientID());
		bookingJson.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
		bookingJson.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
		bookingJson.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());
		bookingJson.put(JSON_PROP_ISHOLIDAYBOOKING, booking.getIsHolidayBooking());
		bookingJson.put(JSON_PROP_SESSIONID, booking.getSessionID());
		bookingJson.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
		bookingJson.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));

		JSONArray productsArray = new JSONArray();
		JSONArray accoArray = accoService.process(booking,"false");
		JSONArray airArray = airService.process(booking,"false");
		JSONArray activitiesArray = activitiesService.process(booking);
		
		for(int i=0;i<accoArray.length();i++) {
			productsArray.put(accoArray.get(i));	
		}
		for(int i=0;i<airArray.length();i++) {
			productsArray.put(airArray.get(i));	
		}
		for(int i=0;i<activitiesArray.length();i++) {
			productsArray.put(activitiesArray.get(i));	
		}
		bookingJson.put("products", productsArray);
		bookingArray.put(bookingJson);
		}
		myLogger.info(String.format("Bookings retrieved for userID %s = %s", userID, bookingArray.toString()));
		return bookingArray.toString();
		}
	}
	
	public String getByStatus(String status) {


		List<Booking> bookings = bookRepository.findByStatus(status);
		if(bookings.size()==0)
		{
			response.put("ErrorCode", "BE_ERR_003");
			response.put("ErrorMsg", BE_ERR_003);
			myLogger.warn(String.format("No booking details found with status  %s ",status));
			return response.toString();
		}
		else
		{
		JSONArray bookingArray = new JSONArray();
		
		for(Booking booking:bookings) {
		JSONObject bookingJson = new JSONObject();
		bookingJson.put(JSON_PROP_BOOKID, booking.getBookID());
		bookingJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
		bookingJson.put(JSON_PROP_CLIENTID, booking.getClientID());
		bookingJson.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
		bookingJson.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
		bookingJson.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());
		bookingJson.put(JSON_PROP_ISHOLIDAYBOOKING, booking.getIsHolidayBooking());
		bookingJson.put(JSON_PROP_SESSIONID, booking.getSessionID());
		bookingJson.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
		bookingJson.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));

		JSONArray productsArray = new JSONArray();
		JSONArray accoArray = accoService.process(booking,"false");
		JSONArray airArray = airService.process(booking,"false");
		JSONArray activitiesArray = activitiesService.process(booking);
		
		for(int i=0;i<accoArray.length();i++) {
			productsArray.put(accoArray.get(i));	
		}
		for(int i=0;i<airArray.length();i++) {
			productsArray.put(airArray.get(i));	
		}
		for(int i=0;i<activitiesArray.length();i++) {
			productsArray.put(activitiesArray.get(i));	
		}
		bookingJson.put("products", productsArray);
		bookingArray.put(bookingJson);
		}
		myLogger.info(String.format("Bookings retrieved with status  %s = %s",status , bookingArray.toString()));
		return bookingArray.toString();
		}
	
	}
	
	
	
	public String updateOrder(JSONObject reqJson, String updateType) {
		
		 switch(updateType)
	        {
	            case JSON_PROP_STATUS:
	                 return updateStatus(reqJson);		     
	           /* case JSON_PROP_EXPIRYDATE:
	                 return updateExpiryDate(reqJson); */   
	                    
	            default:
	            	response.put("ErrorCode", "BE_ERR_000");
	            	response.put("ErrorMsg", BE_ERR_000);
	            	 myLogger.warn(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
	                return "no match for update type";
	        }	
	}
	
	public String updateStatus(JSONObject reqJson) {
		String bookID = reqJson.getString(JSON_PROP_BOOKID);
		Booking booking = bookRepository.findOne(bookID);
		if(booking==null)
		{
			response.put("ErrorCode", "BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			myLogger.warn(String.format("Failed to update status since booking not found for bookid %s", bookID));
			return response.toString();
		}
		else
		{
		String prevBooking = booking.toString();
		booking.setStatus(reqJson.getString(JSON_PROP_STATUS));
		Booking updatedBookingObj = saveBookingOrder(booking,prevBooking);
		myLogger.info(String.format("Status updated for booking with bookid %s = %s",bookID,updatedBookingObj));
		return "booking status updated Successfully";
		}
	}
	
	/*public String updateExpiryDate(JSONObject reqJson) {
		Booking booking = bookRepository.findOne(reqJson.getString(JSON_PROP_BOOKID));
		String prevBooking = booking.toString();
		booking.setExpiryDate(reqJson.getString(JSON_PROP_EXPIRYDATE));
		saveBookingOrder(booking,prevBooking);
		return "booking expiry date time limit updated Successfully";
	}*/
	
	
	public Booking saveBookingOrder(Booking order, String prevBooking) {
		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return bookRepository.saveOrder(orderObj,prevBooking);
	}

	
	
}
