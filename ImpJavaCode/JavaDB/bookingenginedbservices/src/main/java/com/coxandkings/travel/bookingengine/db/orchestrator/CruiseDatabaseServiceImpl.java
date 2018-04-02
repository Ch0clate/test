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

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.CruiseAmCl;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.CruiseAmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.CruiseDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Cruise")
@Transactional(readOnly=false)
public class CruiseDatabaseServiceImpl implements DataBaseService,Constants,ErrorConstants,TestDbService {

	@Autowired
	@Qualifier("Cruise")
	private CruiseDatabaseRepository cruiseRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("CruiseAmCl")
	private CruiseAmClRepository amClRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Override
	public boolean isResponsibleFor(String product) {
		return "cruise".equalsIgnoreCase(product);
	}

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException{
		// TODO Auto-generated method stub
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
			
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("cruiseDetails")) {
			
			JSONObject orderJsonObj = new JSONObject(orderJson.toString());
			
			if(booking==null)
			booking = populateBookingData(bookRequestJson,orderJsonObj);
			JSONArray paxDetailsJson = orderJsonObj.getJSONArray("Guests");
			
			CruiseOrders order = populateCruiseData((JSONObject) orderJson, paxDetailsJson, bookRequestHeader,booking);
			saveOrder(order,"");
		}
		myLogger.info(String.format("Air Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
	}
	
	public CruiseOrders saveOrder(CruiseOrders order, String prevOrder) throws BookingEngineDBException {
		CruiseOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, CruiseOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Cruise order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save cruise order object");
		}
		return cruiseRepository.saveOrder(orderObj,prevOrder);
	}
	
	public CruiseOrders populateCruiseData(JSONObject cruiseDetailsJson,JSONArray paxDetailsJson, JSONObject bookRequestHeader, Booking booking) throws BookingEngineDBException {
		
		try {
			CruiseOrders order = new CruiseOrders();
			
			order.setBooking(booking);
			
			order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
			order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC));
			order.setStatus("OnRequest");
			
			order.setSupplierID(cruiseDetailsJson.getString(JSON_PROP_SUPPREF));
			
			order.setSuppPaxTypeFares(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONArray("suppPaxTypeFare").toString());
			order.setSupplierPrice(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONObject("suppTotalInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
			order.setSupplierPriceCurrencyCode(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONObject("suppTotalInfo").getString(JSON_PROP_CURRENCYCODE));
			
			order.setTotalPaxTypeFares(cruiseDetailsJson.getJSONObject("PricingInfo").getJSONArray("PaxTypeFare").toString());
			order.setTotalPrice(cruiseDetailsJson.getJSONObject("PricingInfo").getJSONObject("TotalInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
			order.setTotalPriceCurrencyCode(cruiseDetailsJson.getJSONObject("PricingInfo").getJSONObject("TotalInfo").optString(JSON_PROP_CURRENCYCODE));
			
			order.setTotalPriceBaseFare(cruiseDetailsJson.getJSONObject("PricingInfo").getJSONObject("TotalInfo").optJSONObject(JSON_PROP_BASEFARE).toString());
			order.setTotalPriceReceivables(cruiseDetailsJson.getJSONObject("PricingInfo").getJSONObject("clientEntityTotalCommercials").toString());
			
			Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
			setPaxDetails = readPassengerDetails(paxDetailsJson,order);
			
			JSONArray paxIds = new JSONArray();
			for(PassengerDetails pax:setPaxDetails ) {
				JSONObject paxJson = new JSONObject();
				paxJson.put("paxId", pax.getPassanger_id());
				paxIds.put(paxJson);
			}
			order.setCruiseDetails(cruiseDetailsJson.optJSONObject("sailingInfo").toString());
			
			Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
//	        clientComms = readClientCommercials(pricedItineraryJson.getJSONObject("PricingInfo").getJSONObject("TotalInfo").getJSONArray(JSON_PROP_RECEIVABLES),order);
			order.setPaxDetails(paxIds.toString());
			order.setClientCommercial(clientComms);
			
			order.setVoyageID(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedSailing").getString("voyageId"));
			order.setItineraryID(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").optJSONObject("selectedCabin").getString("itineraryId"));
			order.setSailingID(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").getString("sailingID"));
			
			order.setCabinNo(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").optJSONObject("selectedCabin").getString("CabinNumber"));
			order.setPricedCategoryCode(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").getString("pricedCategoryCode"));
			order.setFareCode(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").getString("fareCode"));
			order.setStartDate(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedSailing").getString("start"));
			
			order.setShipCode(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedSailing").optJSONObject("cruiseLine").optString("shipCode"));
			
			return order;
			
		} catch (Exception e) {
			// TODO: handle exception
			myLogger.fatal("Failed to populate Cruise Data "+ e);
			throw new BookingEngineDBException("Failed to populate Cruise Data");
		}
		
	}
	
	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, CruiseOrders order) {
		 
		Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;
		
		for(int i=0;i<clientCommsJsonArray.length();i++)	
		{
			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
			
			String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;		
			String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;		
			String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
			
			boolean companyFlag = (i==0)?true:false;
			
		
			JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_CLIENTCOMMERCIALSTOTAL);
			
			for(int j=0;j<clientComms.length();j++) 
			{
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
	
	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, CruiseOrders cruiseOrder) throws BookingEngineDBException {
		
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for(int i=0;i<paxJsonArray.length();i++)	{
		JSONObject currenntPaxDetails = paxJsonArray.getJSONObject(i);
		
		paxDetails =new PassengerDetails();
		paxDetails.setTitle(currenntPaxDetails.getJSONObject("guestName").getString("surName") );
		paxDetails.setFirstName(currenntPaxDetails.getJSONObject("guestName").getString("givenName") );
//		paxDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
		paxDetails.setIsLeadPax(false);
		paxDetails.setStatus("OnRequest");
		paxDetails.setMiddleName(currenntPaxDetails.getJSONObject("guestName").getString("middleName") );
		paxDetails.setLastName(currenntPaxDetails.getJSONObject("guestName").getString("surName") );
		paxDetails.setBirthDate(currenntPaxDetails.getString("personBirthDate"));
		paxDetails.setPaxType("ADT");
		paxDetails.setGender(currenntPaxDetails.getString(JSON_PROP_GENDER));
		paxDetails.setContactDetails(currenntPaxDetails.getJSONObject("Telephone").toString());
		paxDetails.setAddressDetails(currenntPaxDetails.getJSONObject("Address").toString());
        paxDetails.setDocumentDetails(currenntPaxDetails.getJSONObject("TravelDocument").toString());
                
        if(currenntPaxDetails.has("SelectedDining"))
		paxDetails.setSpecialRequests(currenntPaxDetails.getJSONObject("SelectedDining").toString());
		
		//TODO:change it to userID later 
		paxDetails.setLastModifiedBy("");
		paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		//TODO: later check if we are going to get any paxKey in BE
		//paxDetails[i].setPaxkey(currenntPaxDetails.getString("paxKey") );
		savePaxDetails(paxDetails,"");
		paxDetailsSet.add(paxDetails);
		
		}
		return paxDetailsSet;
		
	}
	
	private Booking populateBookingData(JSONObject bookRequestJson,JSONObject orderJson) throws BookingEngineDBException {
		try 
		{
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
			
			//TODO: to check for the holiday booking logic?
			order.setIsHolidayBooking("NO");
			order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTLANGUAGE));
			order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET));
			
			//TODO: Later check what other details we need to populate for booking table. Also confirm whther BE will get those additional details from Redis.
			order.setPaymentInfo(readPaymentInfo(orderJson.getJSONArray(JSON_PROP_CRUISE_PAYMENTOPTIONS),order));
			
			return order;
		}
		catch(Exception e)
		{
			myLogger.fatal("Failed to populate Booking Data "+ e);
			throw new BookingEngineDBException("Failed to populate Booking Data");
		}
	}
	
	private Set<PaymentInfo> readPaymentInfo(JSONArray PaymentInfo, Booking booking) {

		Set<PaymentInfo> paymentInfoSet = new HashSet<PaymentInfo>();

		for (int i = 0; i < PaymentInfo.length(); i++) {
			PaymentInfo paymentInfo = new PaymentInfo();
			JSONObject currentPaymentInfo = PaymentInfo.getJSONObject(i);
			paymentInfo.setPaymentAmount(currentPaymentInfo.getString(JSON_PROP_PAYMENTAMOUNT));
			paymentInfo.setPaymentType(currentPaymentInfo.getString(JSON_PROP_PAYMENTTYPE));
			paymentInfo.setBooking(booking);
			paymentInfoSet.add(paymentInfo);
		}
		return paymentInfoSet;
	}
	JSONObject response=new JSONObject();
	@Override
	public String processBookResponse(JSONObject bookResponseJson) {
		// TODO Auto-generated method stub
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null){
			myLogger.warn(String.format("CRUISE Booking Response could not be populated since no bookings found for req with bookID %s","" ));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		else{
			List<CruiseOrders> orders = cruiseRepository.findByBooking(booking);
			
			for(CruiseOrders order:orders) {
				order.setStatus("confirmed");
				order.setReservationID(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookResp").getJSONObject(0).getJSONArray("ReservationID").getJSONObject(0).getString("ID"));
				order.setBookingDateTime(new Date().toString());
				order.setBookingCompanyName(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookResp").getJSONObject(0).getJSONArray("ReservationID").getJSONObject(0).getString("CompanyName"));
				cruiseRepository.save(order);
			}
			myLogger.info(String.format("CRUISE Booking Response populated successfully for req with bookID %s = %s", "",bookResponseJson.toString()));
			return "SUCCESS";
		}
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving passenger object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save passenger object");
		}
		return passengerRepository.saveOrder(orderObj,prevPaxDetails);
	}

	@Override
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		
		JSONArray cancelRqArr =	reqJson.getJSONObject("requestBody").getJSONArray("cancelRequests");
		
		for(int i=0;i<cancelRqArr.length();i++)
		{
			JSONObject cancelRqJson = cancelRqArr.getJSONObject(i);
			
			String type = cancelRqJson.getString("type");
			CruiseAmCl cancelEntry = new CruiseAmCl();
			
			cancelEntry.setEntityID(cancelRqJson.getString("entityId"));
			cancelEntry.setEntityName(cancelRqJson.getString("entityName"));
			cancelEntry.setRequestType(cancelRqJson.getString("requestType"));
			cancelEntry.setSupplierCharges("0");
			cancelEntry.setDescription(type);
			cancelEntry.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			cancelEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			cancelEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			cancelEntry.setStatus("OnRequest");
			
			cancelEntry.setSupplierID(cancelRqJson.getString("supplierRef"));
			cancelEntry.setUniqueIDs(cancelRqJson.getJSONArray("UniqueID").toString());
			cancelEntry.setPaxVerificationInfo(cancelRqJson.getJSONObject("Verification").toString());
			cancelEntry.setCnclOvrrides(cancelRqJson.getJSONArray("CancellationOverrides").toString());
			
			saveAmCl(cancelEntry, "");
			switch(type)
	        {
	        	case JSON_PROP_CRUISE_CANCELTYPE_FULLCANCEL:
	                return fullCancel(reqJson,cancelRqJson);
	            default:
	                return "no match for cancel/amend type";
	        }
		}
		return "No Cancel Requests inside Request Body";
	}

	private String fullCancel(JSONObject reqJson,JSONObject cancelRqJson) throws BookingEngineDBException {
		
		CruiseOrders order = cruiseRepository.findOne(cancelRqJson.getString("entityId"));
		String prevOrder = order.toString();
		order.setStatus("Cancelled");
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		saveOrder(order, prevOrder);
		return "SUCCESS";
	}
	
	@Override
	public String processAmClResponse(JSONObject reqJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		
		List<CruiseAmCl> amendEntries  = amClRepository.findforResponseUpdate(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),reqJson.getJSONObject(JSON_PROP_RESBODY).getString("entityId"), reqJson.getJSONObject(JSON_PROP_RESBODY).getString("type"), reqJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
		
		if(amendEntries.size()==0) {
			//TODO: handle this before it goes in prod
			System.out.println("no amend entry found. Request might not have been populated");
		}
		else if(amendEntries.size()>1) {
			//TODO: handle this before it goes in prod
			System.out.println("multiple amend entries found. Dont know which one to update");
		}
		else {
			CruiseAmCl amendEntry = amendEntries.get(0);	
			String prevOrder = amendEntry.toString();
			amendEntry.setCompanyCharges(reqJson.getJSONObject(JSON_PROP_RESBODY).optString("companyCharges"));
			amendEntry.setSupplierCharges(reqJson.getJSONObject(JSON_PROP_RESBODY).optString("supplierCharges"));
			amendEntry.setSupplierChargesCurrencyCode(reqJson.getJSONObject(JSON_PROP_RESBODY).optString("supplierChargesCurrencyCode"));
			amendEntry.setCompanyChargesCurrencyCode(reqJson.getJSONObject(JSON_PROP_RESBODY).optString("companyChargesCurrencyCode"));
			amendEntry.setStatus("Confirmed");
			amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
			amendEntry.setCancelRules(reqJson.getJSONObject("responseBody").getJSONObject("CancelInfo").getJSONArray("CancelRules").toString());
			amendEntry.setCancelInfoIDs(reqJson.getJSONObject("responseBody").getJSONObject("CancelInfo").getJSONArray("UniqueID").toString());
		
			//TODO: also set the currency codes and breakups before saving
			saveAmCl(amendEntry, prevOrder);
		}
		return "SUCCESS";
	}
	
	public CruiseAmCl saveAmCl(CruiseAmCl currentOrder, String prevOrder) throws BookingEngineDBException {
		CruiseAmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, CruiseAmCl.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Cruise Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Cruise Cancel order object");
		}
		return amClRepository.saveOrder(orderObj, prevOrder);
	}
	
}
