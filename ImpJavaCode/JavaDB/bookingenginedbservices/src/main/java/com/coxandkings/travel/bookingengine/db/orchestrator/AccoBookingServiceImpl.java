
package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoRoomRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

import org.apache.logging.log4j.Logger;

@Service
@Transactional(readOnly = false)
public class AccoBookingServiceImpl implements Constants,ErrorConstants{
	
	@Qualifier("Acco")
	@Autowired
	private AccoDatabaseRepository accoRepository;

	@Qualifier("AccoRoom")
	@Autowired
	private AccoRoomRepository roomRepository;

	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository amClRepository;

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject(); 

	public JSONArray process(Booking booking, String flag) {
		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray accoOrdersJson = getAccoOrdersJson(accoOrders,flag);
		myLogger.info(String.format("Acco Bookings retrieved for bookID %s = %s", booking.getBookID(),accoOrdersJson.toString()));
		return accoOrdersJson;
	}

	public String getBysuppID(String suppID) {

		List<AccoOrders> orders = accoRepository.findBysuppID(suppID);
		// TODO: check if they need bookID inside each acco order
		if(orders.size()==0)
		{
			response.put("ErrorCode", "BE_ERR_ACCO_006");
			response.put("ErrorMsg", BE_ERR_ACCO_006);
			myLogger.warn(String.format("Acco Orders not present  for suppID %s", suppID));
			return response.toString();
		}
		else
		{
		JSONArray ordersArray = getAccoOrdersJson(orders,"false");
		myLogger.info(String.format("Acco Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
		return ordersArray.toString();
		}
	}

	private JSONArray getAccoOrdersJson(List<AccoOrders> orders, String flag) {

		JSONArray accoArray = new JSONArray();

		for (AccoOrders order : orders) {
			JSONObject accoJson = new JSONObject();
			accoJson = getAccoOrderJson(order,flag);
			accoArray.put(accoJson);

		}
		return accoArray;
	}

	public JSONObject getAccoOrderJson(AccoOrders order, String flag) {

		JSONObject accoJson = new JSONObject();

		// TODO: to check from where will we get these details from WEM
		accoJson.put(JSON_PROP_CREDENTIALSNAME, "");
		accoJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		// TODO: to put ROE field in order tables

		// TODO: we only get one reference from supp as of now so putting that in
		accoJson.put(JSON_PROP_ACCO_REFNUMBER, order.getSupp_booking_reference());

		// TODO: we need to check how will SI send us the details for Enabler Supplier
		// and source supplier
		accoJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		accoJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

		// TODO: confirm from where these are going to come :(
		accoJson.put(JSON_PROP_SUPPLIERRATETYPE, "");
		// TODO: check for logic in case of inventory
		accoJson.put(JSON_PROP_INVENTORY, "N");
		
		//TODO: added these fields on the suggestions of operations
		accoJson.put("QCStatus",order.getQCStatus());
		accoJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		accoJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		
		//TODO: check if we can move out these dates from order table
		accoJson.put(JSON_PROP_CANCELDATE, "");
		accoJson.put(JSON_PROP_AMENDDATE, "");

		// TODO: to check what value to sent when it has not reach the cancel/amend
		// stage
		accoJson.put(JSON_PROP_CANCELDATE, "");
		accoJson.put(JSON_PROP_AMENDDATE, "");
		String createdAt = order.getCreatedAt().toString().substring(0, order.getCreatedAt().toString().indexOf('['));

		accoJson.put(JSON_PROP_CREATEDAT, createdAt);
		accoJson.put(JSON_PROP_PRODUCTCATEGORY, "Accomodation");
		accoJson.put(JSON_PROP_PRODUCTSUBCATEGORY, "Hotel");
		accoJson.put(JSON_PROP_ORDERID, order.getId());
		accoJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		accoJson.put(JSON_PROP_SUPPLIERBOOKINGREF, order.getSupp_booking_reference());
		accoJson.put(JSON_PROP_STATUS, order.getStatus());
		accoJson.put(JSON_PROP_LASTUPDATEDBY, order.getLastModifiedBy());

		JSONObject orderDetails = new JSONObject();
		orderDetails.put(JSON_PROP_ACCO_HOTELDETAILS, getHotelJson(order,flag));
		if(flag=="false")
		{
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getOrderSuppPriceInfoJson(order));
		}
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getOrderTotalPriceInfoJson(order));

		accoJson.put(JSON_PROP_ORDERDETAILS, orderDetails);

		return accoJson;

	}

	private JSONObject getHotelJson(AccoOrders order, String flag) {
		JSONObject hotelJson = new JSONObject();
		JSONArray roomArray = new JSONArray();

		int i = 0;
		for (AccoRoomDetails room : order.getRoomDetails()) {
			JSONObject roomJson = new JSONObject();
			if (i == 0) {
				i++;
				hotelJson.put(JSON_PROP_COUNTRYCODE, room.getCountryCode());
				hotelJson.put(JSON_PROP_CITYCODE, room.getCityCode());
				hotelJson.put(JSON_PROP_ACCO_HOTELCODE, room.getHotelCode());
				hotelJson.put(JSON_PROP_ACCO_HOTELNAME, room.getHotelName());
			}

			roomJson = getRoomJson(room,flag);
			roomArray.put(roomJson);
		}
		hotelJson.put(JSON_PROP_ACCO_ROOMS, roomArray);

		return hotelJson;
	}

	private JSONObject getRoomJson(AccoRoomDetails room, String flag) {
		JSONObject roomJson = new JSONObject();
		roomJson.put(JSON_PROP_ACCO_ROOMID, room.getId());
		roomJson.put(JSON_PROP_ACCO_CHKIN, room.getCheckInDate());
		roomJson.put(JSON_PROP_ACCO_CHKOUT, room.getCheckOutDate());
		roomJson.put(JSON_PROP_ACCO_ROOMTYPEINFO, getRoomTypeInfoJson(room));
		roomJson.put(JSON_PROP_ACCO_RATEPLANINFO, getRatePlanInfoJson(room));
	
		roomJson.put(JSON_PROP_ACCO_MEALINFO, getmealInfojson(room));
		roomJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(room));
		roomJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(room));
		if(flag=="false")
		{
		roomJson.put(JSON_PROP_SUPPCOMM, new JSONArray(room.getSuppCommercials()));
		roomJson.put(JSON_PROP_CLIENTCOMM, new JSONArray(room.getClientCommercials()));
		roomJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(room));
		}
		return roomJson;
	}

	private JSONObject getRoomTypeInfoJson(AccoRoomDetails room) {

		JSONObject roomTypeJson = new JSONObject();
		roomTypeJson.put(JSON_PROP_ACCO_ROOMTYPECODE, room.getRoomTypeCode());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMTYPENAME, room.getRoomTypeName());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMCATEGID, room.getRoomCategoryID());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMCATEGNAME, room.getRoomCategoryName());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMREF, room.getRoomRef());

		return roomTypeJson;
	}

	private JSONObject getRatePlanInfoJson(AccoRoomDetails room) {

		JSONObject ratePlanJson = new JSONObject();

		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANCODE, room.getRatePlanCode());
		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANNAME, room.getRatePlanName());
		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANREF, room.getRatePlanRef());
		ratePlanJson.put(JSON_PROP_ACCO_BOOKINGREF, room.getRatePlanRef());

		return ratePlanJson;
	}

	private JSONObject getSuppPriceInfoJson(AccoRoomDetails room) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, room.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, room.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_TAXES, new JSONObject(room.getSupplierTaxBreakup()));

		return suppPriceJson;
	}

	private JSONObject getmealInfojson(AccoRoomDetails room) {

		JSONObject mealInfoJson = new JSONObject();

		mealInfoJson.put(JSON_PROP_ACCO_MEALID, room.getMealCode());
		mealInfoJson.put(JSON_PROP_ACCO_MEALNAME, room.getMealName());

		return mealInfoJson;
	}

	private JSONObject getTotalPriceInfoJson(AccoRoomDetails room) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, room.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, room.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(room.getTotalTaxBreakup()));

		return totalPriceJson;
	}

	private JSONArray getPaxInfoJson(AccoRoomDetails room) {

		JSONArray paxJsonArray = new JSONArray();

		for (Object paxId : new JSONArray(room.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();

			paxJson.put(JSON_PROP_PAXID, guest.getPassanger_id());
			paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
			paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_TITLE, guest.getTitle());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());

			// TODO: For acco contact details is a JSONObject not array?
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
			if (Pax_ADT.equals(guest.getPaxType()))
				paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
			// TODO: to confirm whhether we are going to get special requests in Acco
			// paxJson.put("specialRequests", new JSONObject(guest.getSpecialRequests()));
			if (guest.getAncillaryServices() != null)
				paxJson.put(JSON_PROP_ANCILLARYSERVICES, new JSONObject(guest.getAncillaryServices()));

			paxJsonArray.put(paxJson);
		}

		return paxJsonArray;
	}

	private JSONArray getSuppComms(AccoOrders order) {

		JSONArray suppCommArray = new JSONArray();

		for (SupplierCommercial suppComm : order.getSuppcommercial()) {
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

	private JSONArray getClientComms(AccoOrders order) {

		JSONArray clientCommArray = new JSONArray();

		for (ClientCommercial clientComm : order.getClientCommercial()) {
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

	private JSONObject getOrderSuppPriceInfoJson(AccoOrders order) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getSuppPriceTaxes()));

		// TODO: to confirm if we will get other charges and fees details in ACco like
		// we get in Air.

		return suppPriceJson;
	}

	private JSONObject getOrderTotalPriceInfoJson(AccoOrders order) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, order.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));

		// TODO: to confirm if we will get other charges and fees details in ACco like
		// we get in Air.

		return totalPriceJson;
	}

	public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {
		
		 switch(updateType)
	        {
	           case JSON_PROP_PAXDETAILS:
	                return updatePaxDetails(reqJson);
	        	case JSON_PROP_ACCO_STAYDATES:
	        		return updateStayDates(reqJson);
	            case JSON_PROP_ACCO_CLIENTRECONFIRMSTATUS:
	                return updateClientReconfirmStatus(reqJson);
	            case "suppReconfirmStatus":
	                 return updateSuppReconfirmStatus(reqJson);	     
	            case JSON_PROP_TICKETINGPCC:
	                 return updateTicketingPCC(reqJson); 
	        	case JSON_PROP_STATUS:
                return updateStatus(reqJson);
	        	/*case JSON_PROP_SUPPCANCELLATIONCHARGES:
	                return updateSuppCancellationCharges(reqJson);
	            case JSON_PROP_COMPCANCELLATIONCHARGES:
	                 return updateCompanyCancellationCharges(reqJson);	     
	            case JSON_PROP_SUPPAMENDCHARGES:
	                 return updateSuppAmendCharges(reqJson);    
	            case JSON_PROP_COMPANYAMENDCHARGES:
	                 return updateCompanyAmendCharges(reqJson);     */  
	            default:
	            	response.put("ErrorCode", "BE_ERR_ACCO_000");
	            	response.put("ErrorMsg", BE_ERR_000);
	            	myLogger.info(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
	                return (response.toString());
	        }	
	}

	private String updatePaxDetails(JSONObject reqJson) throws BookingEngineDBException {
		String paxId = reqJson.getString(JSON_PROP_PAXID);
		PassengerDetails paxDetails = passengerRepository.findOne(paxId);
		if (paxDetails == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_001");
			response.put("ErrorMsg", BE_ERR_ACCO_001);
			myLogger.warn(String.format("Acco Pax details  not found for  paxid  %s ",paxId));
			return (response.toString());
		} else {
			String prevPaxDetails = paxDetails.toString();

		paxDetails.setTitle(reqJson.getString(JSON_PROP_TITLE));
		paxDetails.setFirstName(reqJson.getString(JSON_PROP_FIRSTNAME));
		paxDetails.setMiddleName(reqJson.getString(JSON_PROP_MIDDLENAME));
		paxDetails.setLastName(reqJson.getString(JSON_PROP_LASTNAME));
		paxDetails.setBirthDate(reqJson.getString(JSON_PROP_BIRTHDATE));
		paxDetails.setIsLeadPax(reqJson.getBoolean(JSON_PROP_ISLEADPAX));
		paxDetails.setPaxType(reqJson.getString(JSON_PROP_PAX_TYPE));

		paxDetails.setContactDetails(reqJson.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
		paxDetails.setAddressDetails(reqJson.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
		if (paxDetails.getPaxType().equals(Pax_ADT))
			paxDetails.setDocumentDetails(reqJson.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
		paxDetails.setAncillaryServices(reqJson.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());

		paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails, prevPaxDetails);
			myLogger.info(String.format("Acco Pax details updated successfully for  paxid  %s = %s", paxId, updatedPaxDetails.toString()));
			return "Acco pax details updated Successfully";
		}
	}

	private String updateStayDates(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String.format("Stay dates failed to update since Acco Room details  not found for  roomid  %s",roomId));
			return (response.toString());
		} else {
			String prevRoomDetails = room.toString();
			room.setCheckInDate(reqJson.getString("checkInDate"));
			room.setCheckOutDate(reqJson.getString("checkOutDate"));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedRoomDetails = saveRoomDetails(room, prevRoomDetails);
			myLogger.info(String.format("Acco Room check in/out details updated successfully for  roomid  %s = %s", roomId, updatedRoomDetails.toString()));
			return "Acco Room check in/out details updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_003");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("ClientReconfirmdate failed to update since Order details  not found for  orderid  %s ",orderID));
			return(response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_ACCO_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedclientReconfirmDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco client reconfirmation date updated Successfully for  orderId  %s = %s",orderID, updatedclientReconfirmDetails.toString()));
			return "Acco order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_003");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("SuppReconfirmDate failed to update since Order details  not found for  orderid  %s ",orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedSuppReconfirmDateDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco supplier reconfirmation date updated Successfully for  orderId  %s = %s",orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Acco order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateTicketingPCC(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_003");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("TicketingPCC failed to update since Order details  not found for  orderid  %s ",orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setTicketingPCC(reqJson.getString(JSON_PROP_TICKETINGPCC));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedTicketingPCCDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco ticketingPCC updated Successfully for  orderId  %s = %s", orderID, updatedTicketingPCCDetails.toString()));
			return "Acco order ticketing PCC updated Successfully";
		}
	}

	private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_003");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format("Status failed to update since Order details  not found for  orderid  %s ",orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			order.setStatus(reqJson.getString(JSON_PROP_STATUS));
			AccoOrders updatedStatusDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco Order status updated Successfully for  orderId  %s = %s", orderID, updatedStatusDetails.toString()));
			return "Acco Order status updated Successfully";
		}
	}

	/*private String updateCompanyAmendCharges(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String.format("CompanyAmendCharges  failed to update since Acco Room details  not found for  roomid  %s ",roomId));
			return (response.toString());
		} else {
			String prevOrder = room.toString();
			room.setCompanyAmendCharges(reqJson.getString(JSON_PROP_COMPANYAMENDCHARGES));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedCompanyAmendCharges = saveRoomDetails(room, prevOrder);
			myLogger.info(String.format("Acco Room company amend  charges updated Successfully for  roomID  %s = %s",roomId, updatedCompanyAmendCharges.toString()));
			return "Acco Room company amend  charges updated Successfully";
		}
	}

	private String updateSuppAmendCharges(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String.format("SuppAmendCharges  failed to update since Acco Room details  not found for  roomid  %s ",roomId));
			return (response.toString());
		} else {
			String prevOrder = room.toString();
			room.setSuppAmendCharges(reqJson.getString(JSON_PROP_SUPPAMENDCHARGES));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedSuppAmendCharges = saveRoomDetails(room, prevOrder);
			myLogger.info(String.format("Acco Room supp amend  charges updated Successfully for  roomID  %s = %s", roomId, updatedSuppAmendCharges.toString()));
			return "Acco Room supp amend charges updated Successfully";
		}
	}

	private String updateCompanyCancellationCharges(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String.format("CompanyCancellationCharges  failed to update since Acco Room details  not found for  roomid  %s ",roomId));
			return (response.toString());
		} else {
			String prevOrder = room.toString();
			room.setCompanyCancellationCharges(reqJson.getString(JSON_PROP_COMPCANCELLATIONCHARGES));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedCompCanCharges = saveRoomDetails(room, prevOrder);
			myLogger.info(String.format("Acco Room company cancellation charges updated Successfully for  roomID  %s = %s",roomId, updatedCompCanCharges.toString()));
			return "Acco Room company cancellation charges updated Successfully";
		}
	}

	private String updateSuppCancellationCharges(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String.format("SuppCancellationCharges  failed to update since Acco Room details  not found for  roomid  %s ",roomId));
			return (response.toString());
		} else {
			String prevOrder = room.toString();
			room.setSuppCancellationCharges(reqJson.getString(JSON_PROP_SUPPCANCELLATIONCHARGES));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedSuppCancCharges = saveRoomDetails(room, prevOrder);
			myLogger.info(String.format("Acco Room supplier cancellation charges updated Successfully for  roomID  %s = %s",roomId, updatedSuppCancCharges.toString()));
			return "Acco Room supplier cancellation charges updated Successfully";
		}
	}*/

	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco Passenger order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return passengerRepository.saveOrder(orderObj, prevOrder);
	}

	private AccoRoomDetails saveRoomDetails(AccoRoomDetails room, String prevRoomDetails) throws BookingEngineDBException {
		AccoRoomDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(room, AccoRoomDetails.class);

		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco Room  object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save acco room object");
		}
		return roomRepository.saveOrder(orderObj, prevRoomDetails);
	}

	public AccoOrders saveAccoOrder(AccoOrders currentOrder, String prevOrder) throws BookingEngineDBException {
		AccoOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AccoOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return accoRepository.saveOrder(orderObj,prevOrder);
	}

	public JSONArray getCancellationsByBooking(Booking booking) {

		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray accoOrdersJson = getAccoOrdersCancellation(accoOrders, "cancel");
		return accoOrdersJson;

	}
	
	public JSONArray getAmendmentsByBooking(Booking booking) {

		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray accoOrdersJson = getAccoOrdersAmendments(accoOrders, "amend");
		return accoOrdersJson;

	}
	

	private JSONArray getAccoOrdersCancellation(List<AccoOrders> accoOrders, String type) {

		JSONArray response = new JSONArray();
		for (AccoOrders order : accoOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			
			List<AmCl> cancelAccoOrders = amClRepository.findByEntity("order", orderId, type);
			JSONObject cancelOrderJson = new JSONObject();
			if (cancelAccoOrders.size()>0) {
			orderJson.put(JSON_PROP_ORDERCANCELLATIONS, cancelOrderJson);
			//There will be never two cancellation entries for same order. so loop will only run once
				for(AmCl cancelAccoOrder:cancelAccoOrders) {
				
				cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelAccoOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelAccoOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
						cancelAccoOrder.getSupplierChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
						cancelAccoOrder.getCompanyChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelAccoOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAccoOrder.getCreatedAt().toString().substring(0, cancelAccoOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAccoOrder.getLastModifiedAt().toString().substring(0, cancelAccoOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAccoOrder.getLastModifiedBy());
				}
	        
		    orderJson.put(JSON_PROP_ORDERID, orderId);
		
			}
			/*Set<AccoRoomDetails> rooms = accoRepository.findOne(orderId).getRoomDetails();
			
			JSONArray roomJsonArray = new JSONArray();
			
			for (AccoRoomDetails room : rooms) {

				String roomID = room.getId();*/
			JSONArray roomJsonArray = new JSONArray();
				List<AmCl> cancelRoomOrders = amClRepository.findByEntity("room", orderId, type);
				if(cancelRoomOrders.size()>0) {
				JSONObject cancelRoomJson;
				   orderJson.put(JSON_PROP_ORDERID, orderId);
				//There will be never two cancellation entries for same order. so loop will only run once
				for (AmCl cancelRoomOrder : cancelRoomOrders) {
					cancelRoomJson = new JSONObject();
					cancelRoomJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelRoomOrder.getSupplierCharges());
					cancelRoomJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelRoomOrder.getCompanyCharges());
					cancelRoomJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
							cancelRoomOrder.getSupplierChargesCurrencyCode());
					cancelRoomJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
							cancelRoomOrder.getCompanyChargesCurrencyCode());
					cancelRoomJson.put(JSON_PROP_CANCELTYPE, cancelRoomOrder.getDescription());
					cancelRoomJson.put(JSON_PROP_CREATEDAT, cancelRoomOrder.getCreatedAt().toString().substring(0, cancelRoomOrder.getCreatedAt().toString().indexOf('[')));
					cancelRoomJson.put(JSON_PROP_LASTMODIFIEDAT, cancelRoomOrder.getLastModifiedAt().toString().substring(0, cancelRoomOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelRoomJson.put(JSON_PROP_LASTMODIFIEDBY, cancelRoomOrder.getLastModifiedBy());
					
					if(cancelRoomJson!=null && (cancelRoomJson.has(JSON_PROP_SUPPLIERCANCELCHARGES))) {
						JSONObject roomJson = new JSONObject();
						roomJson.put(JSON_PROP_ACCO_ROOMID, cancelRoomOrder.getEntityID());
						roomJson.put("roomCancellation", cancelRoomJson);
						roomJsonArray.put(roomJson);
					}
					
				}
				
				orderJson.put("rooms", roomJsonArray);
			}
             
				response.put(orderJson);
			}
			
		
			return response;
		}
		

	
	

	private JSONArray getAccoOrdersAmendments(List<AccoOrders> accoOrders,String type ) {
		JSONArray response = new JSONArray();
		for (AccoOrders order : accoOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			orderJson.put(JSON_PROP_ORDERID, orderId);
			List<AmCl> cancelAccoOrders = amClRepository.findByEntity("order", orderId, type);
			JSONArray orderCancelArray = new JSONArray();

			for (AmCl cancelAccoOrder : cancelAccoOrders) {
				JSONObject cancelOrderJson = new JSONObject();
				cancelOrderJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelAccoOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelAccoOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
						cancelAccoOrder.getSupplierChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
						cancelAccoOrder.getCompanyChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_AMENDTYPE, cancelAccoOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAccoOrder.getCreatedAt().toString().substring(0, cancelAccoOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAccoOrder.getLastModifiedAt().toString().substring(0, cancelAccoOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAccoOrder.getLastModifiedBy());
				orderCancelArray.put(cancelOrderJson);
			}
            if(orderCancelArray.length()>0)
			orderJson.put(JSON_PROP_ORDERAMENDS, orderCancelArray);

			Set<AccoRoomDetails> rooms = accoRepository.findOne(orderId).getRoomDetails();
			JSONArray roomJsonArray = new JSONArray();
			
		

				
				List<AmCl> cancelRoomOrders = amClRepository.findByEntity("room", orderId, type);

				for (AmCl cancelRoomOrder : cancelRoomOrders) {
					JSONObject cancelRoomJson = new JSONObject();
					cancelRoomJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelRoomOrder.getSupplierCharges());
					cancelRoomJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelRoomOrder.getCompanyCharges());
					cancelRoomJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
							cancelRoomOrder.getSupplierChargesCurrencyCode());
					cancelRoomJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
							cancelRoomOrder.getCompanyChargesCurrencyCode());
					cancelRoomJson.put(JSON_PROP_AMENDTYPE, cancelRoomOrder.getDescription());
					cancelRoomJson.put(JSON_PROP_CREATEDAT, cancelRoomOrder.getCreatedAt().toString().substring(0, cancelRoomOrder.getCreatedAt().toString().indexOf('[')));
					cancelRoomJson.put(JSON_PROP_LASTMODIFIEDAT, cancelRoomOrder.getLastModifiedAt().toString().substring(0, cancelRoomOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelRoomJson.put(JSON_PROP_LASTMODIFIEDBY, cancelRoomOrder.getLastModifiedBy());
					
					if (cancelRoomJson != null &&  (cancelRoomJson.has(JSON_PROP_SUPPLIERAMENDCHARGES))) {
						JSONObject roomJson = new JSONObject();
						roomJson.put(JSON_PROP_ACCO_ROOMID, cancelRoomOrder.getEntityID());
						roomJson.put(JSON_PROP_ACCO_ROOMAMENDMENTS, cancelRoomJson);
						roomJsonArray.put(roomJson);
					}
				
				}
				
			
				orderJson.put("rooms", roomJsonArray);
		
			response.put(orderJson);

		}

		return response;

	}

}
