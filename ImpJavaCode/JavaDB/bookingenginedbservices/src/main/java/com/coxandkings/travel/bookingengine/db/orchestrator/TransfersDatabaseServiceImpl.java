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
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.TransfersDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;


@Service
@Qualifier("Transfers")
@Transactional(readOnly=false)
public class TransfersDatabaseServiceImpl implements DataBaseService,Constants {

	

	@Autowired
	@Qualifier("Transfers")   
	private TransfersDatabaseRepository transfersRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	public boolean isResponsibleFor(String product) {
        return "transfers".equalsIgnoreCase(product);
    }

	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException  {
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		booking = populateBookingData(bookRequestJson);
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_TRANSFERS_TRANSINFO)) {

			TransfersOrders order = populateTransfersData((JSONObject)orderJson, bookRequestHeader, booking);
			saveOrder(order,"");
		}
	
		return "success";
	}

	public TransfersOrders populateTransfersData(JSONObject bookReq, JSONObject bookRequestHeader, Booking booking) throws BookingEngineDBException  {

		TransfersOrders order=new TransfersOrders();
		
		order.setBooking(booking);
		//TODO: change the client ID to userID once you get in header
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus("OnRequest");
	//	order.setClientIATANumber(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		order.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSupplierID(bookReq.getString(JSON_PROP_SUPPREF));
		
		order.setTripType(bookReq.getString(JSON_PROP_TRANSFERS_TRIPTYPE));
		order.setTripType(bookReq.getString(JSON_PROP_TRANSFERS_TRIPINDICATOR));
		
		order.setSuppFares(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSSUPPTOTALFARE).toString());
		order.setSupplierTotalPrice(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSSUPPTOTALFARE).getBigDecimal(JSON_PROP_TRANSFERS_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSSUPPTOTALFARE).getString(JSON_PROP_TRANSFERS_CURRENCYCODE));

		order.setTotalFares(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSTOTALFARE).toString());
		order.setTotalPrice(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSTOTALFARE).getBigDecimal(JSON_PROP_TRANSFERS_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSTOTALFARE).getString(JSON_PROP_TRANSFERS_CURRENCYCODE));
		
		JSONObject vehicleDetails = new JSONObject(bookReq.toString());
		vehicleDetails.remove(JSON_PROP_PAXDETAILS);
		vehicleDetails.remove(JSON_PROP_TRANSFERS_SUPPFARES);
		vehicleDetails.remove(JSON_PROP_TRANSFERS_TOTALFARES);
		/*vehicleDetails.remove(JSON_PROP_TRANSFERS_SUPPFARES);
		vehicleDetails.remove(JSON_PROP_TRANSFERS_TOTALFARES);*/
		
		/*JSONArray extraEquips = vehicleDetails.getJSONArray(JSON_PROP_CAR_SPLEQUIPS);*/
		
		System.out.println(bookReq.toString());
		order.setTransfersDetails(vehicleDetails.toString());
/*		order.setExtraEquipments(extraEquips.toString());*/
		//TODO: to set client comms later and also check if we need to add other fields in supplier comms
		
		Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
		setPaxDetails = readPassengerDetails(bookReq.getJSONArray(JSON_PROP_PAXDETAILS), order);
		
		Set<SupplierCommercial> suppComms =  new HashSet<SupplierCommercial>();
        suppComms = readSuppCommercials(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONArray(JSON_PROP_TRANSFERS_SUPPLIERCOMMS), order);
        
        Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
        clientComms = readClientCommercials(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONArray(JSON_PROP_TRANSFERS_CLIENTENTITYCOMMS), order);
        JSONArray paxIds = new JSONArray();
		for(PassengerDetails pax:setPaxDetails ) {
			JSONObject paxJson = new JSONObject();
			paxJson.put("paxId", pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
        order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);
		
		return order;
		
	}
	
	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, TransfersOrders order) {
		 
		
		Set<SupplierCommercial> suppCommercialsSet =new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		
		for(int i=0;i<suppCommsJsonArray.length();i++)	{
		JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);
		
		suppCommercials =new SupplierCommercial();
		suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
		suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
		//TODO : CommercialAmount put under TotalCommercialAmount, Why?
		suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		
		/*suppCommercials[i].setBeforeCommercialAmount(suppComm.getBigDecimal("commercialInitialAmount").toString());
		suppCommercials[i].setCommercialCalculationPercentage(suppComm.getBigDecimal("commercialCalculationPercentage").toString());
		suppCommercials[i].setCommercialCalculationAmount(suppComm.getBigDecimal("commercialCalculationAmount").toString());
		suppCommercials[i].setTotalCommercialAmount(suppComm.getBigDecimal("commercialAmount").toString());
		suppCommercials[i].setAfterCommercialAmount(suppComm.getBigDecimal("commercialTotalAmount").toString());
		
		
		suppCommercials[i].setCommercialFareComponent((suppComm.getString("commercialFareComponent")));
		suppCommercials[i].setAfterCommercialBaseFare(suppComm.getJSONObject("fareBreakUp").getBigDecimal("baseFare").toString());
		suppCommercials[i].setAfterCommercialTaxDetails(suppComm.getJSONObject("fareBreakUp").getJSONArray("taxDetails").toString());*/
		suppCommercials.setProduct(JSON_PROP_PRODUCTTRANSFER);
		suppCommercials.setOrder(order);
		suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, TransfersOrders order) {
		Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;
		
		for(int i=0;i<clientCommsJsonArray.length();i++)	{
			
			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
			
			 String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			 String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;		
			 String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;		
			 String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
			
			 boolean companyFlag = (i==0)? true: false;
			
		
			JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_TRANSFERS_CLIENTCOMMINFO);
			
			for(int j=0;j<clientComms.length();j++) {
			
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
		
				clientCommercials.setProduct(JSON_PROP_PRODUCTTRANSFER);
				clientCommercials.setOrder(order);
				clientCommercialsSet.add(clientCommercials);
			}
		}
		return clientCommercialsSet;
	}
	
	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, TransfersOrders transfersOrder) throws BookingEngineDBException  {
		 
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for(int i=0;i<paxJsonArray.length();i++)	{
		JSONObject currentPaxDetails = paxJsonArray.getJSONObject(i);
		paxDetails = new PassengerDetails();
		//TODO : Set isLead Traveler.
		JSONObject primaryJson = currentPaxDetails.getJSONObject(JSON_PROP_TRANSPRIMARY);
		
		
		paxDetails.setRph(primaryJson.getString(JSON_PROP_TRANSFERS_RPH) );
		paxDetails.setPaxType(primaryJson.getString(JSON_PROP_PAX_TYPE) );
		paxDetails.setAge(primaryJson.getString(JSON_PROP_TRANSFERS_AGE));
		paxDetails.setQuantity(primaryJson.optString(JSON_PROP_TRANSFERS_QUANTITY));
		paxDetails.setContactDetails(primaryJson.optString(JSON_PROP_TRANSFERS_PHONENUMBER));
		paxDetails.setEmail(primaryJson.optString(JSON_PROP_TRANSFERS_EMAIL));
		paxDetails.setPersonName(primaryJson.optJSONObject(JSON_PROP_TRANSFERS_PERSONNAME).toString());
		
		JSONObject additionalJson = currentPaxDetails.getJSONObject(JSON_PROP_TRANSFERS_ADDITIONAL);
		paxDetails.setPersonName(additionalJson.getJSONObject(JSON_PROP_TRANSFERS_PERSONNAME).toString());
		paxDetails.setRph(additionalJson.getString(JSON_PROP_TRANSFERS_RPH) );
		paxDetails.setPaxType(additionalJson.getString(JSON_PROP_PAX_TYPE) );
		paxDetails.setAge(additionalJson.getString(JSON_PROP_TRANSFERS_AGE));
		paxDetails.setQuantity(additionalJson.optString(JSON_PROP_TRANSFERS_QUANTITY));
		paxDetails.setContactDetails(primaryJson.optString(JSON_PROP_TRANSFERS_PHONENUMBER));
		paxDetails.setEmail(additionalJson.optString(JSON_PROP_TRANSFERS_EMAIL));
		/*paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
		paxDetails.setStatus("OnRequest");
		paxDetails.setMiddleName(currentPaxDetails.getString(JSON_PROP_MIDDLENAME));
		paxDetails.setSurname(currentPaxDetails.getString(JSON_PROP_SURNAME));
		paxDetails.setBirthDate(currentPaxDetails.getString(JSON_PROP_CAR_DOB));
		paxDetails.setGender(currentPaxDetails.getString(JSON_PROP_GENDER));
		paxDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
		paxDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());*/
		
		//TODO:change it to userID later 
		paxDetails.setLastModifiedBy("");
		paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		savePaxDetails(paxDetails,"");
			paxDetailsSet.add(paxDetails);
		
		}
		return paxDetailsSet;
	}
	
	private Booking populateBookingData(JSONObject bookRequestJson) {
		/*Booking order =new Booking();
		order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		order.setStatus("OnRequest");
		order.setClientIATANumber(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
		order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));
		order.setIsHolidayBooking("NO");
		order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientLanguage"));
		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientMarket"));
		order.setClientNationality(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientNationality"));		
		order.setCompany(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("company"));
		order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));

		return order;*/
		
		Booking order =new Booking();
		order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		order.setStatus("OnRequest");
		
		order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
		order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));
		
		order.setIsHolidayBooking("NO");
		order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientLanguage"));
		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientMarket"));
		
		//TODO: Later check what other details we need to populate for booking table. Also confirm whther BE will get those additional details from Redis.
		order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));

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
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		String prevOrder = booking.toString();
		booking.setStatus("confirmed");
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		saveBookingOrder(booking,prevOrder);
		
		JSONArray reservationArr = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("Reservation");
		List<TransfersOrders> orders = transfersRepository.findByBooking(booking);
		int count=0;
		for(TransfersOrders order:orders) {
			order.setStatus("confirmed");
			// TODO : May Need to Change Later
			String bookId = "";
			JSONArray suppBookRef = reservationArr.getJSONObject(count).getJSONArray(JSON_PROP_SUPPBOOKREF);
			for(Object bookRef: suppBookRef) {
				if(((JSONObject) bookRef).getString("type").equals("14")) {
					bookId = ((JSONObject) bookRef).getString("id");
					break;
				}
			}
			order.setSuppBookRef(bookId);
			order.setBookingDateTime(new Date().toString());
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			count++;
			transfersRepository.save(order);
		}
		
		return "SUCCESS";
	
	}
	
	public  Booking saveBookingOrder(Booking order, String prevOrder) {
		
		Booking orderObj=null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
		
			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj,prevOrder);
	}
	
	public TransfersOrders saveOrder(TransfersOrders order, String prevOrder) {
		TransfersOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, TransfersOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return transfersRepository.saveOrder(orderObj,prevOrder);
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

}
