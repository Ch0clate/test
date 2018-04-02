package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.CruiseDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class CruiseBookingServiceImpl implements Constants,ErrorConstants {

	@Qualifier("Cruise")
	@Autowired
	private CruiseDatabaseRepository cruiseRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
		
	JSONObject response=new JSONObject();
	
	public JSONArray process(Booking booking) {
		List<CruiseOrders> cruiseOrders = cruiseRepository.findByBooking(booking);
		JSONArray cruiseOrdersJson = getCruiseOrdersJson(cruiseOrders);
		return cruiseOrdersJson;
	}
	
	public JSONArray getCruiseOrdersJson(List<CruiseOrders> orders) {
		JSONArray cruiseArray = new JSONArray();
		JSONObject cruiseJson = new JSONObject();
		for (CruiseOrders order : orders) {
			cruiseJson = getCruiseOrderJson(order);
			cruiseArray.put(cruiseJson);
		}
		return cruiseArray;	
	}
	
	public JSONObject getCruiseOrderJson(CruiseOrders order)
	{
		JSONObject cruiseJson = new JSONObject();
		
		//TODO: to check from where will we get these details from WEM/BE
		cruiseJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		//TODO: added these fields on the suggestions of operations
		cruiseJson.put("QCStatus",order.getQCStatus());
		cruiseJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		cruiseJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		cruiseJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		cruiseJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());
		
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		cruiseJson.put(JSON_PROP_CANCELDATE, "");
		cruiseJson.put(JSON_PROP_AMENDDATE, "");
		cruiseJson.put(JSON_PROP_INVENTORY, "N");

		//TODO: check for the category and subcategory constants.
		cruiseJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		cruiseJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_AIR_PRODUCTSUBCATEGORY);
		cruiseJson.put(JSON_PROP_ORDERID, order.getId());
		cruiseJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		cruiseJson.put(JSON_PROP_STATUS, order.getStatus());
		cruiseJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		
		JSONObject orderDetails = new JSONObject();
		orderDetails.put("ReservationID", order.getReservationID());
		orderDetails.put("CompanyName", order.getBookingCompanyName());
		orderDetails.put("BookingDate", order.getBookingDateTime());
		
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS,getClientComms(order));
		orderDetails.put("cruiseDetails", new JSONObject(order.getCruiseDetails()));
		orderDetails.put(JSON_PROP_PAXINFO, getPaxInfoJson(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalPriceInfoJson(order));
		
		cruiseJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return cruiseJson;
	}
	
	private  JSONObject getTotalPriceInfoJson(CruiseOrders order) {
		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, order.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getTotalPaxTypeFares()));
		
		totalPriceJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalPriceBaseFare()));
		totalPriceJson.put(JSON_PROP_RECEIVABLES, new JSONObject(order.getTotalPriceReceivables()));
		if(order.getTotalPriceFees()!=null)
		totalPriceJson.put(JSON_PROP_FEES, new JSONObject(order.getTotalPriceFees()));
		if(order.getTotalPriceTaxes()!=null)
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		
		return totalPriceJson;
	}
	
	private  JSONObject getSuppPriceInfoJson(CruiseOrders order) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getSuppPaxTypeFares()));

		return suppPriceJson;
	}
	
	private  JSONArray getPaxInfoJson(CruiseOrders order) {
		
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
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONObject(guest.getContactDetails()));
			if(Pax_ADT.equals(guest.getPaxType()))
			paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
			//TODO: Confirm if we are going to move it to flight details section?
			if(guest.getSpecialRequests()!=null)
			paxJson.put(JSON_PROP_SPECIALREQUESTS, new JSONObject(guest.getSpecialRequests()));
//			paxJson.put(JSON_PROP_ANCILLARYSERVICES, new JSONObject(guest.getAncillaryServices()));
			
			paxJsonArray.put(paxJson);
		}
		
		return paxJsonArray;
	}
	
	private JSONArray getSuppComms(CruiseOrders order) {
		
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
	
	private JSONArray getClientComms(CruiseOrders order) {
		
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
