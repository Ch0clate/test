package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesPassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.ActivitiesDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;

@Service
@Qualifier("Activity")
@Transactional(readOnly = false)
public class ActivitiesDataBaseServiceImpl implements DataBaseService {

	@Autowired
	@Qualifier("Activity")
	private ActivitiesDatabaseRepository activitiesDatabaseRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Override
	public boolean isResponsibleFor(String product) {
		return Constants.JSON_PROP_ACTIVITIES_CATEGORY.equals(product);
	}

	@Override
	public String processBookRequest(JSONObject bookRequestJson) {
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject("requestHeader");
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject("requestBody").getString("bookID"));
		
		if(booking==null)
			booking = populateBookingData(bookRequestJson);
		
		for (Object orderJson : bookRequestJson.getJSONObject("requestBody").getJSONArray("reservations")) {

			ActivitiesOrders order = populateActivitiesData((JSONObject) orderJson, bookRequestHeader,
					booking);
			System.out.println(order);
			saveActivitiesOrder(order);
		}
	
		return "SUCCESS";
	}

	private Booking populateBookingData(JSONObject bookRequestJson) {
		Booking order = new Booking();
		order.setBookID(bookRequestJson.getJSONObject("requestBody").getString("bookID"));
		order.setStatus("OnRequest");
		
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		order.setClientIATANumber(bookRequestJson.getJSONObject("requestHeader").getJSONObject("clientContext")
				.getString("clientIATANumber"));
		order.setClientCurrency(bookRequestJson.getJSONObject("requestHeader").getJSONObject("clientContext")
				.getString("clientCurrency"));
		order.setClientID(
				bookRequestJson.getJSONObject("requestHeader").getJSONObject("clientContext").getString("clientID"));
		order.setClientType(
				bookRequestJson.getJSONObject("requestHeader").getJSONObject("clientContext").getString("clientType"));
		order.setSessionID(bookRequestJson.getJSONObject("requestHeader").getString("sessionID"));
		order.setTransactionID(bookRequestJson.getJSONObject("requestHeader").getString("transactionID"));
		order.setIsHolidayBooking("NO");
		order.setPaymentInfo(
				readPaymentInfo(bookRequestJson.getJSONObject("requestBody").getJSONArray("paymentInfo"), order));

		return order;
	}

	private Set<PaymentInfo> readPaymentInfo(JSONArray PaymentInfo, Booking booking) {
		Set<PaymentInfo> paymentInfoSet = new HashSet<PaymentInfo>();

		for (int i = 0; i < PaymentInfo.length(); i++) {
			PaymentInfo paymentInfo = new PaymentInfo();
			JSONObject currentPaymentInfo = PaymentInfo.getJSONObject(i);
			paymentInfo.setPaymentMethod(currentPaymentInfo.getString("paymentMethod"));
			paymentInfo.setPaymentAmount(currentPaymentInfo.getString("paymentAmount"));
			paymentInfo.setPaymentType(currentPaymentInfo.getString("paymentType"));
			paymentInfo.setAmountCurrency(currentPaymentInfo.getString("amountCurrency"));
			paymentInfo.setCardType(currentPaymentInfo.getString("cardType"));
			paymentInfo.setCardNumber(currentPaymentInfo.getString("cardNumber"));
			paymentInfo.setCardExpiry(currentPaymentInfo.getString("cardExpiry"));
			paymentInfo.setEncryptionKey(currentPaymentInfo.getString("encryptionKey"));
			paymentInfo.setToken(currentPaymentInfo.getString("token"));
			paymentInfo.setAccountType(currentPaymentInfo.getString("accountType"));
			paymentInfo.setBooking(booking);
			paymentInfoSet.add(paymentInfo);

		}
		return paymentInfoSet;
	}

	private ActivitiesOrders saveActivitiesOrder(ActivitiesOrders order) {

		ActivitiesOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, ActivitiesOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return activitiesDatabaseRepository.saveOrder(orderObj);
	
	}

	private ActivitiesOrders populateActivitiesData(JSONObject reservations, JSONObject bookRequestHeader, Booking booking) {
		ActivitiesOrders order = new ActivitiesOrders();
		
		order.setBooking(booking);
		order.setLastUpdatedBy(bookRequestHeader.getJSONObject("clientContext").getString("clientID"));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus("OnRequest");
		order.setClientIATANumber(bookRequestHeader.getJSONObject("clientContext").getString("clientIATANumber"));
		order.setClientCurrency(bookRequestHeader.getJSONObject("clientContext").getString("clientCurrency"));
		order.setClientID(bookRequestHeader.getJSONObject("clientContext").getString("clientID"));
		order.setClientType(bookRequestHeader.getJSONObject("clientContext").getString("clientType"));
		order.setSupplierID(reservations.getString("supplierID"));
		
		
		readOrderDetails(order, reservations);
		
		ActivitiesPassengerDetails[] passengerDetails = readPassengerDetails(reservations, order);
		
		Set<ActivitiesPassengerDetails> setPassengerDetails = new HashSet<ActivitiesPassengerDetails>(Arrays.asList(passengerDetails));
		order.setPassengerDetails(setPassengerDetails);
		
		
		
		return order;
	}

	/**
	 * @param order
	 * @param reservation
	 */
	private void readOrderDetails(ActivitiesOrders order, JSONObject reservation) {
		order.setSupplierProductCode(reservation.getJSONObject("basicInfo").getString("supplierProductCode"));
		order.setSupplierBrandCode(reservation.getJSONObject("basicInfo").getString("supplierBrandCode"));
		order.setName(reservation.getJSONObject("basicInfo").getString("name"));
		order.setSupplier_Details(reservation.getJSONObject("basicInfo").getJSONObject("supplier_Details").toString());
		order.setTourLanguage(reservation.getJSONObject("basicInfo").getJSONArray("tourLanguage").toString());
		order.setAnswers(reservation.getJSONObject("basicInfo").getJSONArray("answers").toString());
		order.setShipping_Details(reservation.getJSONObject("basicInfo").getJSONObject("shipping_Details").toString());
		
		order.setPOS(reservation.getJSONObject("basicInfo").getJSONObject("POS").toString());
		order.setTimeSlotDetails(reservation.getJSONObject("basicInfo").getJSONArray("timeSlotDetails").toString());
		order.setStartDate(readStartDateEndDate(reservation.getJSONObject("schedule").getString("start")));
		order.setEndDate(readStartDateEndDate(reservation.getJSONObject("schedule").getString("end")));
		JSONObject pickupDropoff = readPickupDropoff(reservation);
		order.setPickupDropoff(pickupDropoff.toString());
		
		order.setCountryCode(reservation.getString("countryCode"));
		order.setCityCode(reservation.getString("cityCode"));
		
		
		order.setContactDetail(reservation.getJSONObject("contactDetail").toString());
		
		
		JSONArray paxInfo = reservation.optJSONArray("paxInfo");
		for(Object pax:paxInfo) {
			if("ADT".equals(((JSONObject) pax).getString("paxType"))){
				
				order.setAdultCount(((JSONObject) pax).getNumber("quantity")==null?new String("0"):((JSONObject) pax).getNumber("quantity").toString());
			}
			if("CHD".equals(((JSONObject) pax).getString("paxType"))){
				order.setChildCount(((JSONObject) pax).getNumber("quantity")==null?new String("0"):((JSONObject) pax).getNumber("quantity").toString());
			}
		}
		
		
		
		order.setSuppPaxTypeFares(getSuppPaxTypeFares(reservation).toString());
		order.setTotalPaxTypeFares(getTotalPaxTypeFares(reservation).toString());
		
		order.setCommercialPaxTypeFares(reservation.getJSONArray("totalPriceInfo").toString());
		
		Set<SupplierCommercial> setSuppComms = new HashSet<SupplierCommercial>();
		setSuppComms = readSuppCommercials(reservation.getJSONArray("suppPriceInfo"),setSuppComms,order);
		Set<ClientCommercial> setClientComms = new HashSet<ClientCommercial>();
		setClientComms = readClientCommercials(reservation.getJSONArray("suppPriceInfo"),setClientComms,order);
		
		order.setSuppcommercial(setSuppComms);
		order.setClientCommercial(setClientComms);
	}

	private JSONArray getTotalPaxTypeFares(JSONObject reservation) {
		JSONArray pricingDetails = reservation.getJSONArray("suppPriceInfo");
        JSONArray totalPaxtypeFares = new JSONArray();
		for(int pricingDetailsCount=0;pricingDetailsCount<pricingDetails.length();pricingDetailsCount++){
			// TODO : get clarification , that in suppPaxTypeFares only Adult and Child details will go
            // or summary details will go too. Based on that a "IF THEN ELSE" will come here with check on 
			// participantCategory
			JSONObject totalPaxtypeFare = new JSONObject();
			
			// TODO : Check if Adult is good or the value will be "ADT" for Adult and "CHD" for Child
			totalPaxtypeFare.put("paxType", pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"));

			JSONObject basefares= new JSONObject();
			BigDecimal totalPrice = pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice");
			BigDecimal markupPrice = new BigDecimal(0);
			JSONArray clientCommercials = pricingDetails.getJSONObject(pricingDetailsCount).optJSONArray("clientCommercials");
			
			// TODO : check if single entityCommercial will only present in ClientCommercial array
			// TODO : what needs to be done when, more then one entityCommercial array will present here
			JSONArray entityCommercials = null ;
			if(clientCommercials != null && clientCommercials.optJSONObject(0)!= null)
			entityCommercials = clientCommercials.optJSONObject(0).optJSONArray("entityCommercials");
			
			if(entityCommercials != null)
			for(int entityCommercialcount=0;entityCommercialcount<entityCommercials.length();entityCommercialcount++) {
				JSONObject entityCommercial = entityCommercials.getJSONObject(entityCommercialcount);
				if("MarkUp".equals(entityCommercial.getString("commercialName"))) {
					markupPrice = entityCommercial.getBigDecimal("commercialAmount");
					break;
				}
			}
			
			if(totalPrice != null)
			totalPrice = totalPrice.add(markupPrice);
			basefares.put("amount", totalPrice);
			basefares.put("currencyCode", pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));
			
			totalPaxtypeFare.put("baseFare", basefares);
			
			JSONArray clientEntityCommercials = new JSONArray();
			BigDecimal totalFarePrice = new BigDecimal(0);
//			totalFarePrice = totalFarePrice.add(totalPrice);
			
			if(clientCommercials != null)
			for(int clientCommercialscount=0;clientCommercialscount<clientCommercials.length();clientCommercialscount++) {
			JSONObject clientEntityCommercial = new JSONObject();
			
			// TODO : Check is clientID, parentClientID, CoomercialEntityType and commercialEntityID are same for all the elements 
			//e.g: Markup, ManagementFees, Discount , IssuanceFees etc. are same. Or they are different. Check calculatePrices method 
			// in activitySearchProcessor
			String clientID = clientCommercials.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials").optJSONObject(0).optString("clientID");
			String parentClientID = clientCommercials.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials").optJSONObject(0).optString("parentClientID");
			String commercialEntityType = clientCommercials.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials").optJSONObject(0).optString("commercialEntityType");
			String commercialEntityID = clientCommercials.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials").optJSONObject(0).optString("commercialEntityID");
			
			clientEntityCommercial.put("clientID", clientID);
			clientEntityCommercial.put("parentClientID", parentClientID);
			clientEntityCommercial.put("commercialEntityID", commercialEntityID);
			clientEntityCommercial.put("commercialEntityType", commercialEntityType);
			
			JSONArray entityCommercialsForClientCommercials = clientCommercials.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials");
			JSONArray clientEntityCommercialsArray = new JSONArray();
			
			for(int entityCommercialsForClientCommercialsCount=0;entityCommercialsForClientCommercialsCount<entityCommercialsForClientCommercials.length();entityCommercialsForClientCommercialsCount++) {
				
				JSONObject entityCommercialsForClientCommercial = new JSONObject();
				entityCommercialsForClientCommercial.put("commercialCurrency", entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialCurrency"));
				entityCommercialsForClientCommercial.put("commercialType", entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialType"));
				entityCommercialsForClientCommercial.put("commercialAmount", entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialAmount"));
				entityCommercialsForClientCommercial.put("commercialName", entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialName"));
				clientEntityCommercialsArray.put(entityCommercialsForClientCommercial);

				
//				if("Receivable".equals(entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialType"))) {
//					totalFarePrice = totalFarePrice.add(entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optBigDecimal("commercialAmount", new BigDecimal(0)));
//				}
			}
            
			clientEntityCommercial.put("clientCommercials", clientEntityCommercialsArray);
			clientEntityCommercials.put(clientEntityCommercial);
			
			}
			
			totalPaxtypeFare.put("clientEntityCommercials", clientEntityCommercials);
			
			JSONArray totalPriceInfo = reservation.getJSONArray("totalPriceInfo");
			for(int totalPriceInfoCount=0;totalPriceInfoCount<totalPriceInfo.length();totalPriceInfoCount++) {
				
				// TODO : "ADT" or "Adult" needs to standardize. "Child" or "CHD" needs to standardize
				if(pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory").equals(totalPriceInfo.getJSONObject(totalPriceInfoCount).getString("participantCategory"))) {
					totalFarePrice = totalPriceInfo.getJSONObject(totalPriceInfoCount).getBigDecimal("totalPrice");
					break;
				}
			}
			
			JSONObject totalFares= new JSONObject();
			totalFares.put("amount", totalFarePrice);
			totalFares.put("currencyCode", pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));
			
			totalPaxtypeFare.put("totalFare", totalFares);
			totalPaxtypeFares.put(totalPaxtypeFare);
		
		}
		return totalPaxtypeFares;
	}

	/**
	 * @param reservation
	 * @return
	 */
	private JSONArray getSuppPaxTypeFares(JSONObject reservation) {
		JSONArray pricingDetails = reservation.getJSONArray("suppPriceInfo");
        JSONArray suppPaxtypeFares = new JSONArray();
		for(int pricingDetailsCount=0;pricingDetailsCount<pricingDetails.length();pricingDetailsCount++){
			// TODO : get clarification , that in suppPaxTypeFares only Adult and Child details will go
            // or summary details will go too. Based on that a "IF THEN ELSE" will come here with check on 
			// participantCategory
			JSONObject suppPaxTypeFare = new JSONObject();
			
			// TODO : Check if Adult is good or the value will be "ADT" for Adult and "CHD" for Child
			suppPaxTypeFare.put("paxType", pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"));
			
			JSONObject basefares= new JSONObject();
			basefares.put("amount", pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice"));
			basefares.put("currencyCode", pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));
			
			suppPaxTypeFare.put("baseFare", basefares);
			
			
			// TODO : totalFares in SuppPaxTypeFare is the sum of tax and fees we are receiving. Right now we are not receiving 
			// both, Hence it is same as basetypeFares. In case tax and fees starts coming, the code below needs modification.
			JSONObject totalFares= new JSONObject();
			totalFares.put("amount", pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice"));
			totalFares.put("currencyCode", pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));
			
			suppPaxTypeFare.put("totalFare", totalFares);
			
			JSONArray supplierCommercials = pricingDetails.getJSONObject(pricingDetailsCount).optJSONArray("supplierCommercials");
			
			suppPaxTypeFare.put("supplierCommercials", supplierCommercials);
			suppPaxtypeFares.put(suppPaxTypeFare);
		}
		return suppPaxtypeFares;
	}

	/**
	 * @param reservation
	 * @return
	 */
	private JSONObject readPickupDropoff(JSONObject reservation) {

		JSONObject pickupDropoff = new JSONObject();

		String dateInString = reservation.getJSONObject("pickupDropoff").getString("dateTime");

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Instant instant = sdf.parse(dateInString).toInstant();

			// TODO: done
			ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC );
			String stringDate = zonedDateTime.toString();
			pickupDropoff.put("dateTime", zonedDateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		pickupDropoff.put("locationName", reservation.getJSONObject("pickupDropoff").get("locationName"));

		return pickupDropoff;
	}

	private ZonedDateTime readStartDateEndDate(String stringInDate) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Instant instant = sdf.parse(stringInDate).toInstant();

			// TODO: done
			ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC );
//			String stringOutDate = zonedDateTime.toString();
			return zonedDateTime;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray suppPriceInfo, Set<ClientCommercial> setClientComms,ActivitiesOrders order) {
		ClientCommercial clientCommercial = null;
		for (int commercialCount = 0; commercialCount < suppPriceInfo.length(); commercialCount++) {
			
			// TODO: The supplier and Client Commercials for Adult and Child will not go into 
			// Supplier and Client Commercial table. Adult and child will be put into order/passengers table.
			if("Summary".equals(suppPriceInfo.getJSONObject(commercialCount).getString("participantCategory"))) {
			JSONArray clientCommercials = suppPriceInfo.getJSONObject(commercialCount)
					.optJSONArray("clientCommercials");
			if (clientCommercials != null && clientCommercials.length() > 0) {
				for (int clientCommercialCount = 0; clientCommercialCount < clientCommercials
						.length(); clientCommercialCount++) {
					JSONArray entityCommercials = clientCommercials.getJSONObject(clientCommercialCount)
							.optJSONArray("entityCommercials");

					for (int entityCommercialCount = 0; entityCommercialCount < entityCommercials
							.length(); entityCommercialCount++) {
						clientCommercial = new ClientCommercial();
						
//						 boolean companyFlag = (i==0)?true:false;
						 
						JSONObject entityCommercial = entityCommercials.getJSONObject(entityCommercialCount);
						clientCommercial.setClientID(entityCommercial.getString("clientID"));
						clientCommercial.setCommercialEntityType(entityCommercial.getString("commercialEntityType"));
						clientCommercial.setParentClientID(entityCommercial.getString("parentClientID"));
						clientCommercial.setCommercialCurrency(entityCommercial.getString("commercialCurrency"));
						clientCommercial.setCommercialType(entityCommercial.getString("commercialType"));
						clientCommercial.setCommercialEntityID(entityCommercial.getString("commercialEntityID"));
						clientCommercial
								.setCommercialAmount(entityCommercial.getBigDecimal("commercialAmount").toString());
						clientCommercial.setCommercialName(entityCommercial.getString("commercialName"));
						
						clientCommercial.setProduct(Constants.JSON_PROP_ACTIVITIES_CATEGORY);
						clientCommercial.setOrder(order);
						
						setClientComms.add(clientCommercial);

					}
				}
			}

		}
		}

		return setClientComms;
	}

	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppPriceInfo, Set<SupplierCommercial> setSuppComms,ActivitiesOrders order) {
		SupplierCommercial supplierCommercial = null;
		for (int commercialCount = 0; commercialCount < suppPriceInfo.length(); commercialCount++) {
			// TODO: The supplier and Client Commercials for Adult and Child will not go into 
		    // Supplier and Client Commercial table. Adult and child will be put into order/passengers table.
			if("Summary".equals(suppPriceInfo.getJSONObject(commercialCount).getString("participantCategory"))) {
			JSONArray supplierCommercials = suppPriceInfo.getJSONObject(commercialCount)
					.optJSONArray("supplierCommercials");
			if (supplierCommercials != null && supplierCommercials.length() > 0) {

				for (int supplierCommercialCount = 0; supplierCommercialCount < supplierCommercials
						.length(); supplierCommercialCount++) {
					JSONObject supplierCommercialsJSON = supplierCommercials.getJSONObject(supplierCommercialCount);
					supplierCommercial = new SupplierCommercial();
					supplierCommercial.setCommercialType(supplierCommercialsJSON.getString("commercialType"));
					supplierCommercial
							.setCommercialAmount(supplierCommercialsJSON.getBigDecimal("commercialAmount").toString());
					supplierCommercial.setCommercialName(supplierCommercialsJSON.getString("commercialName"));
					supplierCommercial.setCommercialCurrency(supplierCommercialsJSON.getString("commercialCurrency"));
					supplierCommercial.setProduct(Constants.JSON_PROP_ACTIVITIES_CATEGORY);
					supplierCommercial.setOrder(order);

					// TODO: Commented out by Pritish check and make changes accordingly
					/*
					 * supplierCommercial.setBeforeCommercialAmount(
					 * suppPriceInfo.getJSONObject(commercialCount).getBigDecimal("totalPrice").
					 * toString()); supplierCommercial.setTotalCommercialAmount(supplierCommercials
					 * .getJSONObject(supplierCommercialCount).getBigDecimal("commercialAmount").
					 * toString());
					 */

					setSuppComms.add(supplierCommercial);

				}
			}
		}

		}

		return setSuppComms;
	}

	private ActivitiesPassengerDetails[] readPassengerDetails(JSONObject reservations, ActivitiesOrders order) {
		JSONArray passengetDetailsJsonArray = reservations.getJSONArray("participantInfo");
		ActivitiesPassengerDetails[] passengetDetails = new ActivitiesPassengerDetails[passengetDetailsJsonArray.length()];
		
		for (int i = 0; i < passengetDetailsJsonArray.length(); i++) {
			passengetDetails[i] = new ActivitiesPassengerDetails();
			
			passengetDetails[i].setBirthDate(passengetDetailsJsonArray.getJSONObject(i).getString("DOB"));
			passengetDetails[i].setContactDetails(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("contactDetails").toString());
			passengetDetails[i].setGivenName(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("personName").getString("givenName"));
			passengetDetails[i].setMiddleName(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("personName").getString("middleName"));
			passengetDetails[i].setNamePrefix(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("personName").getString("namePrefix"));
			passengetDetails[i].setNameTitle(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("personName").getString("nameTitle"));
			passengetDetails[i].setPassengerType(passengetDetailsJsonArray.getJSONObject(i).getString("qualifierInfo"));
			passengetDetails[i].setSurname(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("personName").getString("surname"));
			passengetDetails[i].setActivitiesOrders(order);
		}

		return passengetDetails;
	}

	@Override
	public String processBookResponse(JSONObject bookResponseJson) {
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject("responseBody").getString("bookID"));
		String prevOrder = booking.toString(); 
		booking.setStatus("confirmed");
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		saveBookingOrder(booking,prevOrder);
		
		List<ActivitiesOrders> orders = activitiesDatabaseRepository.findByBooking(booking);
		
		for(ActivitiesOrders order:orders) {
			order.setStatus("confirmed");
			
			// TODO : how to identify which confirmation code is for which order
			order.setSupp_booking_reference(bookResponseJson.getJSONObject("responseBody").getJSONArray("supplierBookReferences").getJSONObject(0).getString("bookRefId"));
			order.setBookingDateTime(new Date().toString());
			activitiesDatabaseRepository.save(order);
		}
		
		return "SUCCESS";
	}

	private Booking saveBookingOrder(Booking order,String prevOrder) {

		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj,prevOrder);
	
		
	}

}
