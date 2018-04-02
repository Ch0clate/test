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
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Air")
@Transactional(readOnly=false)
public class AirDataBaseServiceImpl implements DataBaseService,Constants,ErrorConstants,TestDbService {

	@Autowired
	@Qualifier("Air")
	private AirDatabaseRepository airRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository accoAmClRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject();
	
	public boolean isResponsibleFor(String product) {
        return "air".equalsIgnoreCase(product);
    }

	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException {
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		booking = populateBookingData(bookRequestJson);
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		JSONArray paxDetailsJson = bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXDETAILS);
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_AIR_PRICEDITINERARY)) {

			AirOrders order = populateAirData((JSONObject) orderJson, paxDetailsJson, bookRequestHeader,booking, bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_AIR_TRIPINDICATOR), bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_AIR_TRIPTYPE));
			System.out.println(order);
			saveOrder(order,"");

		}
		myLogger.info(String.format("Air Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
	}

	public AirOrders populateAirData(JSONObject pricedItineraryJson,JSONArray paxDetailsJson, JSONObject bookRequestHeader, Booking booking, String tripIndicator, String tripType ) throws BookingEngineDBException {
		try {
		AirOrders order=new AirOrders();
		
		order.setBooking(booking);
		//TODO: change the client ID to userID once you get in header
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus("OnRequest");
		
		
		order.setSupplierID(pricedItineraryJson.getString(JSON_PROP_SUPPREF));
		
		//TODO: trip indicator and tripType will need to be moved inside priced itinerary?
		order.setTripIndicator(tripIndicator);
		order.setTripType(tripType);
		
		//TODO: add fees and other components of prices as well here
		order.setSuppPaxTypeFares(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONArray(JSON_PROP_AIR_PAXTYPEFARES).toString());
		order.setSupplierPrice(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		
		order.setTotalPaxTypeFares(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONArray(JSON_PROP_AIR_PAXTYPEFARES).toString());
		order.setTotalPrice(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		
		//TODO: Confirm if we need any other price components here for air 
		
		order.setTotalPriceBaseFare(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_BASEFARE).toString());
		order.setTotalPriceReceivables(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
		order.setTotalPriceFees(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_FEES).toString());
		order.setTotalPriceTaxes(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
		
		//TODO: Do we need to set adult count and child count as well?
		Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
		setPaxDetails = readPassengerDetails(paxDetailsJson, order);
		
		JSONArray paxIds = new JSONArray();
		for(PassengerDetails pax:setPaxDetails ) {
			JSONObject paxJson = new JSONObject();
			paxJson.put("paxId", pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
		order.setFlightDetails(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARY).toString());
		Set<SupplierCommercial> suppComms =  new HashSet<SupplierCommercial>();
        suppComms = readSuppCommercials(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS),order);
        
        Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
        clientComms = readClientCommercials(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS),order);
		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);
		
		return order;
	}catch(Exception e)
		{
		myLogger.fatal("Failed to populate Air Data "+ e);
		throw new BookingEngineDBException("Failed to populate Air Data");
	}
}
	
	

	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, AirOrders airOrder) throws BookingEngineDBException {
		 
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for(int i=0;i<paxJsonArray.length();i++)	{
		JSONObject currenntPaxDetails = paxJsonArray.getJSONObject(i);
		paxDetails =new PassengerDetails();
		
	
		paxDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE) );
		paxDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME) );
		paxDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
		paxDetails.setStatus("OnRequest");
		paxDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME) );
		paxDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
		paxDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
		paxDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE) );
		paxDetails.setGender(currenntPaxDetails.getString(JSON_PROP_GENDER));
		paxDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
		paxDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
                if(Pax_ADT.equals(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE)))		
                paxDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
                
        if(currenntPaxDetails.has(JSON_PROP_SPECIALREQUESTS))
		paxDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
		paxDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());
		
		//TODO:change it to userID later 
		paxDetails.setLastModifiedBy("");
		paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
	    savePaxDetails(paxDetails,"");
		//TODO: later check if we are going to get any paxKey in BE
		//paxDetails[i].setPaxkey(currenntPaxDetails.getString("paxKey") );
		
		paxDetailsSet.add(paxDetails);
		
		}
		return paxDetailsSet;
	}
	
	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, AirOrders order) {
		 
		
		Set<SupplierCommercial> suppCommercialsSet =new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		
		for(int i=0;i<suppCommsJsonArray.length();i++)	{
		JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);
		
		suppCommercials =new SupplierCommercial();
		suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
		suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
		suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		
	
		suppCommercials.setProduct(JSON_PROP_PRODUCTAIR);
		suppCommercials.setOrder(order);
		suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, AirOrders order) {
	 
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
		
		//TODO: to check for the holiday booking logic?
		order.setIsHolidayBooking("NO");
		order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTLANGUAGE));
		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET));
		
		//TODO: Later check what other details we need to populate for booking table. Also confirm whther BE will get those additional details from Redis.
	
		order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));
		
		
		
		return order;
	}catch(Exception e)
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
	

	public String processBookResponse(JSONObject bookResponseJson) {

		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		if(booking==null)
		{
			myLogger.warn(String.format("AIR Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject("responseBody").getString("bookID")));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		else
		{
		List<AirOrders> orders = airRepository.findByBooking(booking);
		
		for(AirOrders order:orders) {
			order.setStatus("confirmed");
			order.setAirlinePNR(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_SUPPBOOKREF));
			order.setBookingDateTime(new Date().toString());
			airRepository.save(order);
			
		}
		myLogger.info(String.format("Air Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject("responseBody").getString("bookID"),bookResponseJson.toString()));
		return "SUCCESS";
		}
	}
	
	public String processAmClResponse(JSONObject reqJson) throws BookingEngineDBException {
		
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
		saveAirAmCl(amendEntry, prevOrder);
		}
		return "SUCCESS";
		
	}
	
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
		saveAirAmCl(amendEntry, "");
		
		 switch(type)
	        {
	           
	        	case JSON_PROP_AIR_CANNCELTYPE_CANCELPAX:
	        		return updatePaxDetails(reqJson,type);
	           /* case JSON_PROP_AIR_CANNCELTYPE_CANCELJOU:
	            	return updateFlightDetails(reqJson,type);*/
	            case JSON_PROP_AIR_CANNCELTYPE_CANCELSSR:
	            	return cancelSSR(reqJson);	     
	        	case JSON_PROP_AIR_CANNCELTYPE_FULLCANCEL:
	                return fullCancel(reqJson);
	        	case JSON_PROP_AIR_CANNCELTYPE_CANCELODO:
		            	return cancelODO(reqJson);	  
	            default:
	                return "no match for cancel/amend type";
	        }	
		
	}
	

private String cancelODO(JSONObject reqJson) throws BookingEngineDBException {
	  JSONObject currFlightSeg,origDestObj1,operatingAirlineObj;
	 JSONArray origDestArray = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("cancelRequests").getJSONObject(0).getJSONArray("supplierBookReferences").getJSONObject(0).getJSONArray("originDestinationOptions");
	 String entityID = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("cancelRequests").getJSONObject(0).getJSONArray("supplierBookReferences").getJSONObject(0).getString("orderID");
	 AirOrders order = airRepository.findOne(entityID);
	 JSONObject flightDetails = new JSONObject(order.getFlightDetails());
	 JSONArray originDestinationOptionsArray = flightDetails.getJSONArray("originDestinationOptions");//dATABASE
		
	 String prevOrder = order.toString();
	 for(int i=0;i<origDestArray.length();i++)
		{
			JSONObject origDestObj = origDestArray.getJSONObject(i);//CancelReq
			JSONArray flightSegArr = origDestObj.getJSONArray("flightSegment");
			for(int j=0;j<flightSegArr.length();j++)
			{
				JSONObject flightSegObj = flightSegArr.getJSONObject(j);
				JSONObject operatingAirlineObjCan = flightSegObj.getJSONObject("operatingAirline");//Req
				for(int k=0;k<originDestinationOptionsArray.length();k++)//DB
				{
                origDestObj1 = originDestinationOptionsArray.getJSONObject(k);//FirstObject Second Obj
				    JSONArray flightSegArr2 = 	origDestObj1.getJSONArray("flightSegment");
				    for(int l=0;l<flightSegArr2.length();l++)
				    {
				    	     currFlightSeg = flightSegArr2.getJSONObject(l);
				      		 operatingAirlineObj = currFlightSeg.getJSONObject("operatingAirline");
				      		  if((operatingAirlineObjCan.getString("airlineCode").equalsIgnoreCase(operatingAirlineObj.getString("airlineCode")))  && (operatingAirlineObjCan.getString("flightNumber").equalsIgnoreCase(operatingAirlineObj.getString("flightNumber"))))
				      		  {
				      			currFlightSeg.put("status", "Cancel");
				      		  }
				      }
				    
				}
				 
			}
			order.setFlightDetails(flightDetails.toString());
			AirOrders order1 = saveOrder(order, prevOrder);
			System.out.println(order1);
		}
	 
		return "SUCCESS";
	}
	private String fullCancel(JSONObject reqJson) throws BookingEngineDBException {
		
		AirOrders order = airRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		String prevOrder = order.toString();
		order.setStatus("Cancelled");
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		saveOrder(order, prevOrder);
		return "SUCCESS";
	}
	
	private String cancelSSR(JSONObject reqJson) throws BookingEngineDBException {
		
		
			JSONArray paxDetails = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("cancelRequests").getJSONObject(0).getJSONArray("supplierBookReferences").getJSONObject(0).getJSONArray("paxDetails");
			for(int j=0;j<paxDetails.length();j++)
			{
				JSONObject currPaxObj = paxDetails.getJSONObject(j);
				
		PassengerDetails paxDetailsObj = passengerRepository.findOne(currPaxObj.getString("passengerID"));
		String prevOrder = paxDetailsObj.toString();
		JSONArray SSrCan = currPaxObj.getJSONArray("specialRequestInfo");
		for(int m=0;m<SSrCan.length();m++)
		{
		JSONObject currSSRcAN = SSrCan.getJSONObject(m);
		String delSsr = currSSRcAN.getString("ssrCode");
		JSONObject ssr = new JSONObject(paxDetailsObj.getSpecialRequests());
		JSONArray ssrArray = ssr.getJSONArray("specialRequestInfo");
		for (int k = 0; k < ssrArray.length(); k++) 
		{
			JSONObject currentssr = ssrArray.getJSONObject(k);	
			   if(currentssr.getString("ssrCode").equalsIgnoreCase(delSsr))
			   {
				   currentssr.put("status", "Cancelled");
			   }
			
		}
		paxDetailsObj.setSpecialRequests(ssr.toString());
		}
		//paxDetails.setStatus("Cancelled");
		paxDetailsObj.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		paxDetailsObj.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		savePaxDetails(paxDetailsObj, prevOrder);
		}
		
			return "SUCCESS";
		
	}
	
	
	private String updatePaxDetails(JSONObject reqJson, String type) throws BookingEngineDBException {
		AirOrders airOrder = airRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		String prevOrder = null;
		
		JSONArray guestRoomJsonArray = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXINFO);
		for (int i = 0; i < guestRoomJsonArray.length(); i++) {
			
		JSONObject currenntPaxDetails = guestRoomJsonArray.getJSONObject(i);

		PassengerDetails guestDetails = null;	

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
		else  {
			String paxid = currenntPaxDetails.getString("paxids");
			guestDetails = passengerRepository.findOne(paxid);
			prevOrder = guestDetails.toString();
			guestDetails.setStatus("Cancelled");

			guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			savePaxDetails(guestDetails,prevOrder);
		}
		}
		
		return "SUCCESS";
	}
	
	
	
	
	public AmCl saveAirAmCl(AmCl amendEntry, String prevOrder) throws BookingEngineDBException {
		AmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(amendEntry, AmCl.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air Amend Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Air Amend Cancel order object");
		}
		return accoAmClRepository.saveOrder(orderObj, prevOrder);
	}
	
	public  Booking saveBookingOrder(Booking order, String prevOrder) throws BookingEngineDBException {
		Booking orderObj=null;
	try {
		orderObj = CopyUtils.copy(order, Booking.class);
		
	} catch (InvocationTargetException | IllegalAccessException e) {
		 myLogger.fatal("Error while saving Air Booking object : " + e);
		 //myLogger.error("Error while saving order object: " + e);
		throw new BookingEngineDBException("Failed to save Air Booking object");
	}
    return bookingRepository.saveOrder(orderObj,prevOrder);
	}
	
	public AirOrders saveOrder(AirOrders order, String prevOrder) throws BookingEngineDBException {
		AirOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, AirOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save air order object");
		}
		return airRepository.saveOrder(orderObj,prevOrder);
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
