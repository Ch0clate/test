package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;

import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.impl.AirGetByClass;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class AirBookingServiceImpl implements Constants,ErrorConstants{
	
	@Qualifier("Air")
	@Autowired
	private AirDatabaseRepository airRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("updatestat")
	private AirGetByClass getByClass;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository accoAmClRepository;
	
    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject(); 

	
	public JSONArray process(Booking booking, String flag) {
		List<AirOrders> airOrders = airRepository.findByBooking(booking);
		JSONArray airOrdersJson = getAirOrdersJson(airOrders,flag);
		return airOrdersJson;
	}
	
	public String getBysuppID(String suppID) {
		
		List<AirOrders> orders = airRepository.findBysuppID(suppID);
		//TODO: check if they need bookID inside each air order
		if(orders.size()==0)
		{
			response.put("ErrorCode", "BE_ERR_AIR_006");
			response.put("ErrorMsg", BE_ERR_AIR_006);
			myLogger.warn(String.format("Air Orders not present  for suppID %s", suppID));
			return response.toString();
		}
		else
		{
		JSONArray ordersArray =getAirOrdersJson(orders,"false");
		myLogger.info(String.format("Air Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
		return ordersArray.toString();
	}
	}
	
	
	public JSONArray getAirOrdersJson(List<AirOrders> orders, String flag) {
		
		JSONArray airArray = new JSONArray();
		JSONObject airJson = new JSONObject();
		for (AirOrders order : orders) {
			
			airJson = getAirOrderJson(order,flag);
			airArray.put(airJson);
		}
		return airArray;	
	}
	
	public JSONObject getAirOrderJson(AirOrders order, String flag) {

		JSONObject airJson = new JSONObject();
		
				//TODO: to check from where will we get these details from WEM/BE
				airJson.put(JSON_PROP_CREDENTIALSNAME, "");
				
				//TODO: added these fields on the suggestions of operations
				airJson.put("QCStatus",order.getQCStatus());
				airJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
				airJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
				airJson.put("suppTimeLimitExpiryDate", order.getSuppTimeLimitExpiryDate());
				
				
				//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
				airJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
				airJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

				
				//TODO: to check what value to sent when it has not reach the cancel/amend stage
				airJson.put(JSON_PROP_CANCELDATE, "");
				airJson.put(JSON_PROP_AMENDDATE, "");
				airJson.put(JSON_PROP_INVENTORY, "N");
		
		//TODO: check for the category and subcategory constants.
		airJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		airJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_AIR_PRODUCTSUBCATEGORY);
		airJson.put(JSON_PROP_ORDERID, order.getId());
		airJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		airJson.put(JSON_PROP_STATUS, order.getStatus());
		airJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());

		JSONObject orderDetails = new JSONObject();
		//TODO: we only get one reference from supp as of now so not putting in anything
		orderDetails.put(JSON_PROP_AIR_GDSPNR, "");
		orderDetails.put(JSON_PROP_AIR_AIRLINEPNR, "");
		orderDetails.put(JSON_PROP_AIR_TICKETNUMBER, "");
		
		//TODO: These is set at credential name and supplier level
		orderDetails.put(JSON_PROP_TICKETINGPCC, "");
		
		orderDetails.put(JSON_PROP_AIR_TRIPTYPE,order.getTripType());
		orderDetails.put(JSON_PROP_AIR_TRIPINDICATOR,order.getTripIndicator());
		if(flag=="false")
		{	
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS,getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
	    }
		orderDetails.put(JSON_PROP_AIR_FLIGHTDETAILS, new JSONObject(order.getFlightDetails()));
		orderDetails.put(JSON_PROP_PAXINFO, getPaxInfoJson(order));
		
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalPriceInfoJson(order,flag));
	
		
		airJson.put(JSON_PROP_ORDERDETAILS, orderDetails);

		return airJson;
	}
	
	private JSONArray getSuppComms(AirOrders order) {
		
		JSONArray suppCommArray = new JSONArray();
		
		for(SupplierCommercial suppComm: order.getSuppcommercial()) {
			JSONObject suppCommJson = new JSONObject();
			suppCommJson.put(JSON_PROP_COMMERCIALNAME, suppComm.getCommercialName());
			suppCommJson.put(JSON_PROP_COMMERCIALTYPE, suppComm.getCommercialType());
			
			suppCommJson.put(JSON_PROP_COMMAMOUNT, suppComm.getCommercialAmount());
			suppCommJson.put(JSON_PROP_COMMERCIALCURRENCY, suppComm.getCommercialCurrency());
			suppCommJson.put(JSON_PROP_RECIEPTNUMBER, suppComm.getRecieptNumber());
			suppCommJson.put(JSON_PROP_INVOICENUMBER, suppComm.getInVoiceNumber());
			
			suppCommArray.put(suppCommJson);
		}		
		return suppCommArray;
	}
	
	private JSONArray getClientComms(AirOrders order) {
		
		JSONArray clientCommArray = new JSONArray();
		
		for(ClientCommercial clientComm: order.getClientCommercial()) {
			JSONObject clientCommJson = new JSONObject();
			
			clientCommJson.put(JSON_PROP_COMMERCIALNAME, clientComm.getCommercialName());
			clientCommJson.put(JSON_PROP_COMMERCIALTYPE, clientComm.getCommercialType());
		
			clientCommJson.put(JSON_PROP_COMMAMOUNT, clientComm.getCommercialAmount());
			clientCommJson.put(JSON_PROP_COMMERCIALCURRENCY, clientComm.getCommercialCurrency());
			clientCommJson.put(JSON_PROP_CLIENTID, clientComm.getClientID());
			clientCommJson.put(JSON_PROP_PARENTCLIENTID, clientComm.getParentClientID());
			clientCommJson.put(JSON_PROP_COMMERCIALENTITYID, clientComm.getCommercialEntityID());
			clientCommJson.put(JSON_PROP_COMMERCIALENTITYTYPE, clientComm.getCommercialEntityType());
			clientCommJson.put(JSON_PROP_COMPANYFLAG, clientComm.isCompanyFlag());
			
		
			clientCommJson.put(JSON_PROP_RECIEPTNUMBER, clientComm.getRecieptNumber());
			clientCommJson.put(JSON_PROP_INVOICENUMBER, clientComm.getInVoiceNumber());
			
			clientCommArray.put(clientCommJson);
		}		
		return clientCommArray;
	}
	
	private  JSONArray getPaxInfoJson(AirOrders order) {
		
		JSONArray paxJsonArray = new JSONArray();
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_AIR_PASSENGERID,guest.getPassanger_id());
			paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
			paxJson.put(JSON_PROP_TITLE,guest.getTitle());
			paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());
			paxJson.put(JSON_PROP_STATUS, guest.getStatus());
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
			if(Pax_ADT.equals(guest.getPaxType()))
			paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
			//TODO: Confirm if we are going to move it to flight details section?
			if(guest.getSpecialRequests()!=null)
			paxJson.put(JSON_PROP_SPECIALREQUESTS, new JSONObject(guest.getSpecialRequests()));
			paxJson.put(JSON_PROP_ANCILLARYSERVICES, new JSONObject(guest.getAncillaryServices()));
			
			paxJsonArray.put(paxJson);
		}
		
		return paxJsonArray;
	}
	
	private  JSONObject getSuppPriceInfoJson(AirOrders order) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getSuppPaxTypeFares()));
		//TODO: to confirm if we will get tax details in air book req?
		/*suppPriceJson.put("taxAmount", order.getSupplierTaxAmount());
		suppPriceJson.put("taxBreakup", new JSONArray(order.getSupplierTaxBreakup()));*/

		return suppPriceJson;
	}
	
	private  JSONObject getTotalPriceInfoJson(AirOrders order, String flag) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, order.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		if(flag=="false")
		{
		totalPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getTotalPaxTypeFares()));
		}
		else
		{
			JSONArray tempPaxTypeFares =  new JSONArray(order.getTotalPaxTypeFares());
			for(int i=0;i<tempPaxTypeFares.length();i++)
			{
				JSONObject currPaxTypeFares = tempPaxTypeFares.getJSONObject(i);
		      currPaxTypeFares.remove("clientEntityCommercials");
		   }
			totalPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, tempPaxTypeFares);
		}
		totalPriceJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalPriceBaseFare()));
		totalPriceJson.put(JSON_PROP_RECEIVABLES, new JSONObject(order.getTotalPriceReceivables()));
		totalPriceJson.put(JSON_PROP_FEES, new JSONObject(order.getTotalPriceFees()));
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		
		//TODO: to confirm if we need to add any other details here.
		

		return totalPriceJson;
	}
	
	private AirOrders saveOrder(AirOrders order, String prevOrder) throws BookingEngineDBException {
		AirOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, AirOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save air order object");
		}
		return airRepository.saveOrder(orderObj, prevOrder);
	}
	

	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air passenger object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Air passenger object");
		}
		return passengerRepository.saveOrder(orderObj,prevPaxDetails);
	}

	public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {
		
		 switch(updateType)
	        {
	            case JSON_PROP_AIR_FLIGHTDETAILS:
	                return updateFlightDetails(reqJson);
	            case JSON_PROP_STATUS:
	                 return updateStatus(reqJson);	     
	            case JSON_PROP_SPECIALREQUESTS:
	                return updateSpecialRequest(reqJson);
	            case JSON_PROP_DOB:
	                 return updateBirthDate(reqJson);	     
	            case JSON_PROP_TICKETINGPCC:
	                 return updateTicketingPCC(reqJson); 
	          /*  case JSON_PROP_SUPPCANCELLATIONCHARGES:
	                return updateSuppCancellationCharges(reqJson);
	            case JSON_PROP_COMPCANCELLATIONCHARGES:
	                 return updateCompanyCancellationCharges(reqJson);	     
	            case JSON_PROP_SUPPAMENDCHARGES:
	                 return updateSuppAmendCharges(reqJson);    
	            case JSON_PROP_COMPANYAMENDCHARGES:
	                 return updateCompanyAmendCharges(reqJson); */     
	                    
	            default:
	                return "no match for update type";
	        }	
	}

	//TODO: needs to check how we are going to change the commercial prices
	
	
/*	private String updateCompanyAmendCharges(JSONObject reqJson) {
		
		AirOrders order = airRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		String prevOrder = order.toString();
		order.setCompanyAmendCharges(reqJson.getString(JSON_PROP_COMPANYAMENDCHARGES));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		saveOrder(order, prevOrder);
		return "Air order company amend  charges updated Successfully";
	}

	private String updateSuppAmendCharges(JSONObject reqJson) {
		
		AirOrders order = airRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		String prevOrder = order.toString();
		order.setSuppAmendCharges(reqJson.getString(JSON_PROP_SUPPAMENDCHARGES));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		saveOrder(order, prevOrder);
		return "Air order supp amend charges updated Successfully";		
	}

	private String updateCompanyCancellationCharges(JSONObject reqJson) {
		
		AirOrders order = airRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		String prevOrder = order.toString();
		order.setCompanyCancellationCharges(reqJson.getString(JSON_PROP_COMPCANCELLATIONCHARGES));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		saveOrder(order, prevOrder);
		return "Air order company cancellation charges updated Successfully";
	}

	private String updateSuppCancellationCharges(JSONObject reqJson) {
		
		AirOrders order = airRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		String prevOrder = order.toString();
		order.setSuppCancellationCharges(reqJson.getString(JSON_PROP_SUPPCANCELLATIONCHARGES));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		saveOrder(order, prevOrder);
		return "Air order supplier cancellation charges updated Successfully";
	}*/

	private String updateTicketingPCC(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AirOrders order = airRepository.findOne(orderID);
		if(order==null)
		{
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("TicketingPCC  failed to update since Air order details   not found for  orderid  %s ",orderID));
			return (response.toString());
		}
		else
		{
		String prevOrder = order.toString();
		order.setTicketingPCC(reqJson.getString(JSON_PROP_TICKETINGPCC));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		AirOrders updatedTicketingPCCObj = saveOrder(order, prevOrder);
		myLogger.info(String.format("Air Ticketing PCC updated Successfully for orderID  %s = %s",orderID, updatedTicketingPCCObj.toString()));
		return "Air order ticketing PCC updated Successfully";
	}
	}

	private String updateBirthDate(JSONObject reqJson) throws BookingEngineDBException {
		String paxID = reqJson.getString(JSON_PROP_PAXID);
		PassengerDetails paxDetails = passengerRepository.findOne(paxID);
		if(paxDetails==null)
		{
			response.put("ErrorCode", "BE_ERR_AIR_001");
			response.put("ErrorMsg", BE_ERR_AIR_001);
			myLogger.warn(String.format("BirthDate  failed to update since Air Passenger details   not found for paxid  %s ",paxID));
			return (response.toString());
		}
		else
		{
		String prevPaxDetails = paxDetails.toString();
		paxDetails.setBirthDate(reqJson.getString(JSON_PROP_BIRTHDATE));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails, prevPaxDetails);
		myLogger.info(String.format("BirthDate updated Successfully for Air paxID  %s = %s",paxID, updatedPaxDetails.toString()));
		return "passsenger birth date updated Successfully";
	}
	}
	
	//TODO: check if we need this method as we have moved pecial request to flight details now?
	private String updateSpecialRequest(JSONObject reqJson) throws BookingEngineDBException {
		String paxID = reqJson.getString(JSON_PROP_PAXID);
		PassengerDetails paxDetails = passengerRepository.findOne(paxID);
		if(paxDetails==null)
		{
			response.put("ErrorCode", "BE_ERR_AIR_001");
			response.put("ErrorMsg", BE_ERR_AIR_001);
			myLogger.warn(String.format("Special Request  failed to update since Air Passenger details   not found for paxid  %s ",paxID));
			return (response.toString());
		}
		else
		{
		String prevPaxDetails = paxDetails.toString();
		paxDetails.setSpecialRequests(reqJson.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails,prevPaxDetails);
		myLogger.info(String.format("BirthDate updated Successfully for Air paxID  %s = %s",paxID, updatedPaxDetails.toString()));
		return "Passenger's special request updated Successfully";
	}
	}
	private String updateFlightDetails(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AirOrders order = airRepository.findOne(orderID);
		if(order==null)
		{
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("Flight Details  failed to update since Air order details   not found for  orderid  %s ",orderID));
			return (response.toString());
		}
		String prevOrder = order.toString();
		order.setFlightDetails(reqJson.getJSONObject(JSON_PROP_AIR_FLIGHTDETAILS).toString());
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		AirOrders updatedFlightDetailsObj = saveOrder(order, prevOrder);
		myLogger.info(String.format("Flight Details  updated Successfully for Air orderID %s = %s",orderID, updatedFlightDetailsObj.toString()));
		return "Flight Details updated Successfully";
	}
	
	private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AirOrders order = airRepository.findOne(orderID);
		if(order==null)
		{
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("Status  failed to update since Air order details   not found for  orderid  %s ",orderID));
			return (response.toString());
		}
		String prevOrder = order.toString();
		order.setStatus(reqJson.getString(JSON_PROP_STATUS));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		AirOrders updatedStatusObj = saveOrder(order, prevOrder);
		myLogger.info(String.format("Status updated Successfully for Air orderID %s = %s",orderID, updatedStatusObj.toString()));
		return "Air Order status updated Successfully";
	}
	
	public String getByClass(String flightclass ) {
		List<AirOrders> orders = getByClass.findByClass(flightclass);
		//TODO: check if they need bookID inside each air order
		JSONArray ordersArray =getAirOrdersJson(orders,"false");
		return ordersArray.toString();
	}
	
	
	public JSONArray getCancellationsByBooking(Booking booking) {

		List<AirOrders> airOrders = airRepository.findByBooking(booking);
		JSONArray airOrdersJson = getAirOrdersCancellations(airOrders, "cancel");
		return airOrdersJson;

	}
	
	public JSONArray getAmendmentsByBooking(Booking booking) {

		List<AirOrders> airOrders = airRepository.findByBooking(booking);
		JSONArray airOrdersJson = getAirOrdersAmendments(airOrders, "amend");
		return airOrdersJson;

	}
	
	private JSONArray getAirOrdersAmendments(List<AirOrders> airOrders,String type ) {
		JSONArray response = new JSONArray();
		for (AirOrders order : airOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			orderJson.put(JSON_PROP_ORDERID, orderId);
			List<AmCl> cancelAirOrders = accoAmClRepository.findByEntity("order", orderId, type);
			JSONArray orderCancelArray = new JSONArray();

			for (AmCl cancelAirOrder : cancelAirOrders) {
				JSONObject cancelOrderJson = new JSONObject();
				cancelOrderJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelAirOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelAirOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
						cancelAirOrder.getSupplierChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
						cancelAirOrder.getCompanyChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_AMENDTYPE, cancelAirOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAirOrder.getCreatedAt().toString().substring(0, cancelAirOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAirOrder.getLastModifiedAt().toString().substring(0, cancelAirOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAirOrder.getLastModifiedBy());
				orderCancelArray.put(cancelOrderJson);
			}

			orderJson.put(JSON_PROP_ORDERAMENDS, orderCancelArray);

			String passengersString = airRepository.findOne(orderId).getPaxDetails();
			JSONArray passengers = new JSONArray(passengersString);
			JSONArray cancelPAxJsonArray = new JSONArray();
			JSONArray paxJsonArray = new JSONArray();
		
			for (int m=0;m<passengers.length();m++) {

				   JSONObject currPaxObj = passengers.getJSONObject(m);
					String paxID = currPaxObj.getString("paxId");
				List<AmCl> cancelPaxOrders = accoAmClRepository.findByEntity("pax", paxID, type);

				for (AmCl cancelPaxOrder : cancelPaxOrders) {
					JSONObject cancelPaxJson = new JSONObject();
					cancelPaxJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelPaxOrder.getSupplierCharges());
					cancelPaxJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelPaxOrder.getCompanyCharges());
					cancelPaxJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
							cancelPaxOrder.getSupplierChargesCurrencyCode());
					cancelPaxJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
							cancelPaxOrder.getCompanyChargesCurrencyCode());
					cancelPaxJson.put(JSON_PROP_AMENDTYPE, cancelPaxOrder.getDescription());
					cancelPaxJson.put(JSON_PROP_CREATEDAT, cancelPaxOrder.getCreatedAt().toString().substring(0, cancelPaxOrder.getCreatedAt().toString().indexOf('[')));
					cancelPaxJson.put(JSON_PROP_LASTMODIFIEDAT, cancelPaxOrder.getLastModifiedAt().toString().substring(0, cancelPaxOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelPaxJson.put(JSON_PROP_LASTMODIFIEDBY, cancelPaxOrder.getLastModifiedBy());
					cancelPAxJsonArray.put(cancelPaxJson);
				}

				if (cancelPAxJsonArray != null && cancelPAxJsonArray.length()!=0) {
					JSONObject paxJson = new JSONObject();
					paxJson.put(JSON_PROP_PAXID, paxID);
					paxJson.put(JSON_PROP_PAXAMENDMENTS, cancelPAxJsonArray);
					paxJsonArray.put(paxJson);
				}
			}

			orderJson.put("Passengers", paxJsonArray);
			response.put(orderJson);

		}
		

		return response;
}

	

	private JSONArray getAirOrdersCancellations(List<AirOrders> airOrders,String type ) {
		JSONArray response = new JSONArray();
		for (AirOrders order : airOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			
			List<AmCl> cancelAirOrders = accoAmClRepository.findByEntity("order", orderId, type);
			if(cancelAirOrders.size()>0) {
			orderJson.put(JSON_PROP_ORDERID, orderId);
			JSONArray orderCancelArray = new JSONArray();

			for (AmCl cancelAirOrder : cancelAirOrders) {
				JSONObject cancelOrderJson = new JSONObject();
				cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelAirOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelAirOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
						cancelAirOrder.getSupplierChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
						cancelAirOrder.getCompanyChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelAirOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAirOrder.getCreatedAt().toString().substring(0, cancelAirOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAirOrder.getLastModifiedAt().toString().substring(0, cancelAirOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAirOrder.getLastModifiedBy());
				orderCancelArray.put(cancelOrderJson);
			}

			orderJson.put(JSON_PROP_ORDERCANCELLATIONS, orderCancelArray);
		
			JSONArray paxJsonArray = new JSONArray();
			List<AmCl> cancelPaxOrders = accoAmClRepository.findByEntity("pax", orderId, type);
			if(cancelPaxOrders.size()>0) {
			JSONArray orderCancelPaxArray = new JSONArray();
			for (AmCl cancelPaxOrder : cancelPaxOrders) {
				JSONObject cancelPaxOrderJson = new JSONObject();
				cancelPaxOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelPaxOrder.getSupplierCharges());
				cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelPaxOrder.getCompanyCharges());
				cancelPaxOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
						cancelPaxOrder.getSupplierChargesCurrencyCode());
				cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
						cancelPaxOrder.getCompanyChargesCurrencyCode());
				cancelPaxOrderJson.put(JSON_PROP_CANCELTYPE, cancelPaxOrder.getDescription());
				cancelPaxOrderJson.put(JSON_PROP_CREATEDAT, cancelPaxOrder.getCreatedAt().toString().substring(0, cancelPaxOrder.getCreatedAt().toString().indexOf('[')));
				cancelPaxOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelPaxOrder.getLastModifiedAt().toString().substring(0, cancelPaxOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelPaxOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelPaxOrder.getLastModifiedBy());
				String entityIDs = cancelPaxOrder.getEntityID();
				String newEntityIds = entityIDs.replaceAll("\\|", "\\,");
				cancelPaxOrderJson.put("paxIDs", newEntityIds);
				orderCancelPaxArray.put(cancelPaxOrderJson);
				
			}
				
			orderJson.put(JSON_PROP_PAXCANCELLATIONS, orderCancelPaxArray);
			}
			response.put(orderJson);
			}
			
	
		}
		return response;

	
	
}
}
