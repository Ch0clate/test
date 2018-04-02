package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusAmCl;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BusAmclRepository;
import com.coxandkings.travel.bookingengine.db.repository.BusDatabaseRepository;

import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Bus")
@Transactional(readOnly=false)
public class BusDatabaseServiceImpl implements TestDbService,DataBaseService,Constants{
	
	@Autowired
	@Qualifier("Bus")
	private BusDatabaseRepository busRepository;
	
//	@Autowired
//	@Qualifier("BusPassenger")
//	private BusPassengerRepository buspaxRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository AmClRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	@Override
	public boolean isResponsibleFor(String product) {
	
		return "Bus".equalsIgnoreCase(product);
	}

	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {

		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
			booking = populateBookingData(bookRequestJson);
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
//		JSONArray serviceArr = bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("Service");

		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_BUS_SERVICE))
		{
			BusOrders order = populateBusData((JSONObject) orderJson, bookRequestHeader,booking);
			saveOrder(order,"");
			
//			System.out.println("before update id - ->"+order.getId());
//			updatePaxDetails((JSONObject) orderJson,booking,order); //TODO: modified...
//			saveOrder(order,"");
//			System.out.println("after update id - ->"+order.getId());
		}
		
//		saveBookingOrder(booking);
		myLogger.info(String.format("bus Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));

		return "success";
	}

//	private BusOrders updatePaxDetails(JSONObject orderJson, Booking booking,BusOrders order) {
//
//		order.setBooking(booking);
//		
//		Set<BusPassengerDetails> busPassDetails =order.getPassengerDetails();
//		
//		Iterator iterator = busPassDetails.iterator(); 
//		
//		while(iterator.hasNext())
//		{
//			BusPassengerDetails passDtls = (BusPassengerDetails) iterator.next();
//			passDtls.setBus_order_id(order.getId());
//			
//		}
//		order.setPaxDetails(busPassDetails);
//		
//		return order;
//	}

	private BusOrders saveOrder(BusOrders order, String prevOrder) {
		
		BusOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, BusOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return busRepository.saveOrder(orderObj,prevOrder);
		
	}
	
	private AmCl saveAmcl(AmCl cancelOrder, String prevOrder)
	{
		AmCl orderObj = null;
		try
		{
			orderObj = CopyUtils.copy(cancelOrder, AmCl.class);
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Bus Amend Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			
		}
		return AmClRepository.saveOrder(orderObj, prevOrder);
		
	}

	private BusOrders populateBusData(JSONObject serviceorderJson, JSONObject bookRequestHeader,Booking booking) throws BookingEngineDBException {
		
		BusOrders order=new BusOrders();
		order.setBooking(booking);
		
		order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus("OnRequest");
//		order.setClientIATANumber(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		order.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSupplierID(serviceorderJson.getString(JSON_PROP_SUPPREF));
		
		order.setSupplierTotalPrice(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPrice(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
        order.setSupplierPriceCurrencyCode(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getString("currency"));
        order.setTotalPriceCurrencyCode(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).optString(JSON_PROP_CURRENCYCODE));
        order.setBusDetails(readBusDetails(serviceorderJson));
        
        Set<PassengerDetails> setPassDetails = new HashSet<PassengerDetails>();
		setPassDetails = readPassengerDetails(order,serviceorderJson);
		
		JSONArray paxIds = new JSONArray();
		
		
		
		JSONArray paxDetailsArr = new JSONArray();
		paxDetailsArr = serviceorderJson.getJSONArray(JSON_PROP_PAXDETAILS);
		int i=0;
		for(PassengerDetails pax:setPassDetails ) {
			
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_PAXID, pax.getPassanger_id());
			paxJson.put(JSON_PROP_BUS_SEATNO, paxDetailsArr.getJSONObject(i).getString(JSON_PROP_BUS_SEATNO));
			paxJson.put("seatTypesList", paxDetailsArr.getJSONObject(i).getString("seatTypesList"));
			paxJson.put("seatTypeIds", paxDetailsArr.getJSONObject(i).getString("seatTypeIds"));
			i++;
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
		
		
		
		Set<SupplierCommercial> suppComms =  new HashSet<SupplierCommercial>();
		suppComms = readSuppCommercials(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS),order);
		
		 Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
        clientComms = readClientCommercials(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS),order);
        
        
		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);
		
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		return order;
	}

	private String readBusDetails(JSONObject serviceorderJson) {

		JSONObject BusDetails = new JSONObject();
		 
		BusDetails.put("routeScheduleId", serviceorderJson.get("routeScheduleId"));
		BusDetails.put(JSON_PROP_BUS_SOURCE, serviceorderJson.get(JSON_PROP_BUS_SOURCE));
		BusDetails.put(JSON_PROP_BUS_DESTINATION, serviceorderJson.get(JSON_PROP_BUS_DESTINATION));
		BusDetails.put(JSON_PROP_BUS_SERVICEID, serviceorderJson.get(JSON_PROP_BUS_SERVICEID));
		BusDetails.put(JSON_PROP_BUS_LAYOUTID, serviceorderJson.get(JSON_PROP_BUS_LAYOUTID));
		BusDetails.put(JSON_PROP_BUS_OPERATORID, serviceorderJson.get(JSON_PROP_BUS_OPERATORID));
		BusDetails.put("boardingPointID", serviceorderJson.get("boardingPointID"));
		BusDetails.put("droppingPointID", serviceorderJson.get("droppingPointID"));

		return BusDetails.toString();
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, BusOrders order) {
		
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

		clientCommercials.setProduct(JSON_PROP_PRODUCTBUS);
		clientCommercials.setOrder(order);
		clientCommercialsSet.add(clientCommercials);
		}
		}
		return clientCommercialsSet;
	}

private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, BusOrders order) {
		 
		
		Set<SupplierCommercial> suppCommercialsSet =new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		
		for(int i=0;i<suppCommsJsonArray.length();i++)	{
		JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);
		
		suppCommercials =new SupplierCommercial();
		suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
		suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
		suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		
	
		suppCommercials.setProduct(JSON_PROP_PRODUCTBUS);
		suppCommercials.setOrder(order);
		suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<PassengerDetails> readPassengerDetails(BusOrders order,JSONObject serviceorderJson) throws BookingEngineDBException {

//		Set<PassengerDetails> passDetailsSet = new HashSet<PassengerDetails>();
//		BusPassengerDetails passDetails;
//		JSONArray passDetailsJsonArr = serviceorderJson.getJSONArray("Passangers");
//		for(int i=0;i<passDetailsJsonArr.length();i++)
//		{
//			JSONObject currentPassDetails = passDetailsJsonArr.getJSONObject(i);
//			passDetails =new BusPassengerDetails();
//			passDetails.setTitle(currentPassDetails.getString("Title"));
//			passDetails.setName(currentPassDetails.getString("Name"));
//			passDetails.setIdType(currentPassDetails.getString("IdType"));
//			passDetails.setIdNumber(currentPassDetails.getNumber("IdNumber").toString());
//			passDetails.setAge(currentPassDetails.getNumber("Age").toString());
//			passDetails.setGender(currentPassDetails.getString("Gender"));
//			passDetails.setSeatNo(currentPassDetails.getString("SeatNo"));
//			passDetails.setSeatTypesList(currentPassDetails.getString("seatTypesList"));
//			passDetails.setSeatTypeIds(currentPassDetails.getString("seatTypeIds"));
//			passDetails.setPhone(serviceorderJson.get("Phone").toString());
//			passDetails.setMobile(serviceorderJson.get("Mobile").toString());
//			
//			passDetails.setBus_order_id(order.getId());// is it right??
//			
//			passDetails.setLastModifiedBy("");
//			passDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
//			passDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
//			
//			passDetails.setBusOrders(order);
//			passDetailsSet.add(passDetails);
		
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		
		JSONArray passDetailsJsonArr = serviceorderJson.getJSONArray(JSON_PROP_PAXDETAILS);
		for(int i=0;i<passDetailsJsonArr.length();i++)
		{
			JSONObject currenntPaxDetails = passDetailsJsonArr.getJSONObject(i);
			PassengerDetails paxDetails =new PassengerDetails();
			
			paxDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			paxDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME) );
			
			paxDetails.setGender(currenntPaxDetails.getString(JSON_PROP_GENDER));

			
			
			paxDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			paxDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
			paxDetails.setStatus("OnRequest");
			
			
			paxDetails.setLastModifiedBy("");
			paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			
			
			savePaxDetails(paxDetails,"");
			paxDetailsSet.add(paxDetails);
			
		}
		return paxDetailsSet;
	}

	private PassengerDetails savePaxDetails(PassengerDetails paxDetails, String prevPaxDetails) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(paxDetails, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving passenger object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save passenger object");
		}
		return passengerRepository.saveOrder(orderObj,prevPaxDetails);
		
	}

	private Booking populateBookingData(JSONObject bookRequestJson) {
		
//		Booking order =new Booking();
//		order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
//		order.setStatus("OnRequest");
//		order.setClientIATANumber(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
//		order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
//		order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
//		order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
//		order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
//		order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));
//		order.setIsHolidayBooking("NO");
//		
////		TODO:
//		order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));
//		
//		return order;
		
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
//		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientLanguage"));
//		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString("clientMarket"));
		
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

	@Override
	public String processBookResponse(JSONObject bookResponseJson) {
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		String prevOrder=booking.toString();
		
		booking.setStatus("confirmed");
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		saveBookingOrder(booking,prevOrder);
		
		List<BusOrders> orders = busRepository.findByBooking(booking);
		int count =0;

		for(BusOrders order:orders) {
			order.setStatus("confirmed");
			order.setBusPNR(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookTicket").getJSONObject(count).getString(JSON_PROP_BUS_PNRNO));
			order.setTicketNo(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookTicket").getJSONObject(count).getString(JSON_PROP_BUS_TICKETNO));
			count++;
			order.setBookingDate(new Date().toString());
			busRepository.save(order);
		}
		
		return "SUCCESS";
	}
	
//	public String processAmClRequest(JSONObject reqJson) 
//	{
//		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
//		BusAmCl cancelOrder = new BusAmCl();
//		
//		cancelOrder.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("requestType"));
//		cancelOrder.setCancelType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("cancelType"));
//		cancelOrder.setBookId(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
//		
//		cancelOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
//		cancelOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
//		cancelOrder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
//		cancelOrder.setStatus("OnRequest");
//		
//		Booking booking = bookingRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
//
////		List<BusOrders> orders = busRepository.findByBooking(booking);
//		
//		int count =0;
//		JSONObject reqBodyJson = reqJson.getJSONObject("requestBody");
////		JSONArray serviceArr = reqBodyJson.getJSONArray("service");
////		for(int i=0;i<serviceArr.length();i++)
////		{
//			JSONObject serviceJson = reqBodyJson.getJSONObject("service");
//			JSONArray cancelSeatsArr = serviceJson.getJSONArray("seatsToCancel");
//			String orderId;
////			for(BusOrders order:orders) {
////				order.setStatus("confirmed");
////				order.setBusPNR(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookTicket").getJSONObject(count).getString("PNRNo"));
////				order.setTicketNo(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookTicket").getJSONObject(count).getString("TicketNo"));
////				count++;
////				order.setBookingDate(new Date().toString());
////				busRepository.save(order);
//				
//				List<BusOrders> cancelOrders = busRepository.findByTktNo(serviceJson.getString("ticketNo"));
//				for(BusOrders order:cancelOrders)
//				{
//					String prevOrder = order.toString();
////					cancelEntry.setBusOrders(order);
//					
//					if("full".equalsIgnoreCase(reqBodyJson.getString("cancelType")))
//					{
//						order.setStatus("cancelled");
//						 // setting orderid in cancel table
//						
////						setPassengersStatusforfullCancel(order.getId());
//						Set<BusPassengerDetails> setPassDetails = new HashSet<BusPassengerDetails>();
//						setPassDetails = setPassengersStatusforfullCancel(cancelOrder,order,order.getId());
//						order.setPaxDetails(setPassDetails);
////						saveOrder(order,"");
//				
//						saveOrder(order, prevOrder);
//						
//					}	
//					else if("partial".equalsIgnoreCase(reqBodyJson.getString("cancelType")))
//					{
//						for(int j=0;j<cancelSeatsArr.length();j++)
//						{
//							JSONObject cancelJson = cancelSeatsArr.getJSONObject(j);
////							setPassengersStatusforPartialCancel(cancelEntry,order.getId(),cancelJson.getString("seatNo"));
//							Set<BusPassengerDetails> setPassDetails = new HashSet<BusPassengerDetails>();
//							
//							setPassDetails = setPassengersStatusforPartialCancel(cancelOrder,order,order.getId(),cancelJson.getString("seatNo"));
//							order.setPaxDetails(setPassDetails);
//								saveOrder(order, prevOrder);
//						}
//						
//					}
//					
//				}
//				
//				saveBusAmclOrder(cancelOrder, "");
//				
//				System.out.println("cancel id" + cancelOrder.getId());
////			}
////		}
//		
//
//		
//		
//		
//		return "SUCCESS";
//		
//	}
//
//	private Set<BusPassengerDetails> setPassengersStatusforPartialCancel(BusAmCl cancelOrder,BusOrders order,String id,String seatNo) {
//		
//		Set<BusPassengerDetails> passDetailsSet = new HashSet<BusPassengerDetails>();
//		List<BusPassengerDetails> paxList = buspaxRepository.findBySeatNo(id,seatNo);
//		
//		for(BusPassengerDetails pax:paxList)
//		{
//			pax.setStatus("cancelled");
//			pax.setBusOrders(order);
//			passDetailsSet.add(pax);
//			cancelOrder.setBus_order_id(id);
//			cancelOrder.setPaxId(pax.getPassanger_id());
////			cancelEntry.setPaxdetails(pax);
//		}
//		return passDetailsSet;
//	}
//
//	private Set<BusPassengerDetails> setPassengersStatusforfullCancel(BusAmCl cancelOrder,BusOrders order,String id) {
//		
//		Set<BusPassengerDetails> passDetailsSet = new HashSet<BusPassengerDetails>();
//		
//		
//		List<BusPassengerDetails> paxList = buspaxRepository.findByBusOrderId(id);
//		
//		for(BusPassengerDetails pax:paxList)
//		{
//			
//			pax.setStatus("cancelled");
//			pax.setBusOrders(order);
//			passDetailsSet.add(pax);
//			cancelOrder.setBus_order_id(id);
//			cancelOrder.setPaxId(pax.getPassanger_id());
//		}
//		
//		return passDetailsSet;
//		}

	private Booking saveBookingOrder(Booking order, String prevOrder) {
		
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

//	@Override
//	public String processAmClResponse(JSONObject resJson) {
//		
//		List<BusAmCl> cancelEntries  = busAmClRepository.findforResponseUpdate(resJson.getJSONObject(JSON_PROP_RESBODY).getString("bookID"),resJson.getJSONObject(JSON_PROP_RESBODY).getString("cancelId"), resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"), resJson.getJSONObject(JSON_PROP_RESBODY).getString("cancelType"));
//		if(cancelEntries.size()==0) {
//			//TODO: handle this before it goes in prod
//			System.out.println("no amend entry found. Request might not have been populated");
//		}
//		
//		else if(cancelEntries.size()>1) {
//			//TODO: handle this before it goes in prod
//			System.out.println("multiple amend entries found. Dont know which one to update");
//		}
//		else
//		{
//			BusAmCl cancelOrder = cancelEntries.get(0);	
//			String prevOrder = cancelOrder.toString();//TODO:doubt???
//			
//			cancelOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
//			cancelOrder.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
//			cancelOrder.setRefundAmount(resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getBigDecimal("refundAmount").toString());
//			cancelOrder.setRefundAmountCurrency(resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getString("refundAmountCurrency").toString());
//			cancelOrder.setStatus("cancelled");
//			saveBusAmclOrder(cancelOrder, prevOrder);
//		}
//		return "SUCCESS";
//	}

	@Override
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {

		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
		
		AmCl cancelOrder= new AmCl();
		cancelOrder.setEntityID(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		cancelOrder.setOrderID(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("orderId"));
		cancelOrder.setEntityName(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityName"));
		cancelOrder.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("requestType"));
		cancelOrder.setSupplierCharges("0");
		cancelOrder.setDescription(type);
		cancelOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		cancelOrder.setStatus("cancelled");
		cancelOrder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		cancelOrder.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		saveAmcl(cancelOrder, "");
		
		 switch(type)
	        {
	           
	        	case JSON_PROP_AIR_CANNCELTYPE_CANCELPAX:
	        		return updatePaxDetails(reqJson,type);
	           
	            
	        	case JSON_PROP_AIR_CANNCELTYPE_FULLCANCEL:
	                return fullCancel(reqJson);
	        		  
	            default:
	                return "no match for cancel/amend type";
	        }	
		
	
	}



	private String fullCancel(JSONObject reqJson) 
	{
		BusOrders order = busRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("orderId"));
		System.out.println(order.toString());
		String prevOrder = order.toString();
		order.setStatus("Cancelled");
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		saveOrder(order, prevOrder);
		return "SUCCESS";
	}

private String updatePaxDetails(JSONObject reqJson, String type) throws BookingEngineDBException {


	JSONArray serviceArr = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_BUS_SERVICE);
	for(int i=0;i<serviceArr.length();i++)
	{
		JSONArray cancelArr = serviceArr.getJSONObject(i).getJSONArray("seatsToCancel");
		for(int j=0;j<cancelArr.length();j++)
		{
			PassengerDetails paxOrder = passengerRepository.findOne(cancelArr.getJSONObject(j).get(JSON_PROP_PAXID).toString());
			paxOrder.setStatus("cancelled");
			savePaxDetails(paxOrder,"");
		}
	}

	
	return "SUCCESS";
}

@Override
public String processAmClResponse(JSONObject resJson) throws BookingEngineDBException {

	List<AmCl> amendEntries  = AmClRepository.findforResponseUpdate(resJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),resJson.getJSONObject(JSON_PROP_RESBODY).getString("entityId"), resJson.getJSONObject(JSON_PROP_RESBODY).getString("type"), resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
	if(amendEntries.size()==0) {
		//TODO: handle this before it goes in prod
		System.out.println("no amend entry found. Request might not have been populated");
	}
	
	else if(amendEntries.size()>1) {
		//TODO: handle this before it goes in prod
		System.out.println("multiple amend entries found. Dont know which one to update");
	}
	
	else 
	{
		AmCl cancelOrder = amendEntries.get(0);	
		JSONObject serviceJson = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject(JSON_PROP_BUS_SERVICE);
		cancelOrder.setSupplierCharges(serviceJson.getBigDecimal("refundAmount").toString());
		cancelOrder.setSupplierChargesCurrencyCode(serviceJson.getString("currency"));
	
		cancelOrder.setStatus("Cancelled");
		cancelOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		cancelOrder.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString(JSON_PROP_USERID));
		
		//TODO: also set the currency codes and breakups before saving
		saveAmcl(cancelOrder, "");
	}
	return "SUCCESS";
}

}
