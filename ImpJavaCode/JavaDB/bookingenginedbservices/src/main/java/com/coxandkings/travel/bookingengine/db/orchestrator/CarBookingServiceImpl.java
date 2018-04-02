package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.CarDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class CarBookingServiceImpl implements Constants,ErrorConstants{
	
	@Qualifier("Car")
	@Autowired
	private CarDatabaseRepository carRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	JSONObject response = new JSONObject(); 
	
    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	public JSONArray process(Booking booking, String flag) {
		List<CarOrders> carOrders = carRepository.findByBooking(booking);
		JSONArray carOrdersJson = getCarOrdersJson(carOrders, flag);
		return carOrdersJson;
	}
	
	public String getBysuppID(String suppID) {
		
		List<CarOrders> orders = carRepository.findBysuppID(suppID);
		
		if(orders.size()==0){
			response.put("ErrorCode", "BE_ERR_CAR_006");
			response.put("ErrorMsg", BE_ERR_CAR_006);
			myLogger.warn(String.format("Car Orders not present for suppID %s", suppID));
			return response.toString();
		}
		else{
			JSONArray ordersArray = getCarOrdersJson(orders, "false");
			myLogger.info(String.format("Car Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
			return ordersArray.toString();
		}
	}
	

	public String updateOrder(JSONObject reqJson, String updateType) {

		switch(updateType)
        {
		
        }
		return null;
	}
	
	public JSONArray getCarOrdersJson(List<CarOrders> orders, String flag) {
		
		JSONArray carArray = new JSONArray();
		JSONObject carJson = new JSONObject();
		for (CarOrders order : orders) {
			
			carJson = getCarOrderJson(order, flag);
			carArray.put(carJson);
		}
		return carArray;	
	}
	
	public JSONObject getCarOrderJson(CarOrders order, String flag) {

		JSONObject carJson = new JSONObject();
		carJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		carJson.put(JSON_PROP_CANCELDATE, "");
		carJson.put(JSON_PROP_AMENDDATE, "");
		carJson.put(JSON_PROP_INVENTORY, "N");
		
		carJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		carJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_CAR_PRODUCTSUBCATEGORY);
		carJson.put(JSON_PROP_ORDERID, order.getId());
		carJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		carJson.put(JSON_PROP_STATUS, order.getStatus());
		carJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		
		JSONObject orderDetails = new JSONObject();
		
		//TODO: These is set at credential name and supplier level
		orderDetails.put(JSON_PROP_CAR_TRIPTYPE, order.getTripType());
		orderDetails.put(JSON_PROP_SUPPBOOKREF, order.getSuppBookRef());
		JSONObject carDetails = new JSONObject(order.getRentalDetails());
		carDetails.put(JSON_PROP_CAR_VEHICLEINFO, new JSONObject(order.getCarDetails()));
		
		orderDetails.put(JSON_PROP_CAR_CARDETAILS, carDetails);
		
		if(flag.equals("false")) {
			orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
			orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
			orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO,  new JSONObject(order.getSuppFares()));
		}
		
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalFareJson(order));
		orderDetails.put(JSON_PROP_PAXDETAILS, getPassengerInfo(order));
		
		carJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return carJson;
	}
	
	private JSONObject getTotalFareJson(CarOrders order) {
		
		JSONObject totalFareJson = new JSONObject();
		
		totalFareJson.put(JSON_PROP_AMOUNT, new BigDecimal(order.getTotalPrice()));
		totalFareJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		
		totalFareJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalBaseFare()));
		totalFareJson.put(JSON_PROP_FEES, new JSONObject(order.getTotalPriceFees()));
		totalFareJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		totalFareJson.put(JSON_PROP_RECEIVABLES, new JSONObject(order.getTotalPriceReceivables()));
		totalFareJson.put(JSON_PROP_CAR_SPLEQUIPS, new JSONObject(order.getExtraEquipments()));
		totalFareJson.put(JSON_PROP_CAR_PRICEDCOVERAGES, new JSONObject(order.getPricedCoverages()));

		return totalFareJson;
	}

	private JSONArray getSuppComms(CarOrders order) {
		
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
	
	private JSONArray getClientComms(CarOrders order) {
		
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
	
	private JSONArray getPassengerInfo(CarOrders order) {
		
		JSONArray paxJsonArray = new JSONArray();
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails customer  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_CAR_PASSENGERID, customer.getPassanger_id());
			paxJson.put(JSON_PROP_CAR_ISLEADPAX, customer.getIsLeadPax());
			paxJson.put(JSON_PROP_TITLE,customer.getTitle());
			paxJson.put(JSON_PROP_FIRSTNAME, customer.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, customer.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, customer.getLastName());
			paxJson.put(JSON_PROP_GENDER, customer.getGender());
			paxJson.put(JSON_PROP_BIRTHDATE, customer.getBirthDate());
			paxJson.put(JSON_PROP_STATUS, customer.getStatus());
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(customer.getContactDetails()));
			paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(customer.getAddressDetails()));
			
			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;
		
	}
	
	private CarOrders saveOrder(CarOrders order, String prevOrder) throws BookingEngineDBException {
		CarOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, CarOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Car order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save car order object");
		}
		return carRepository.saveOrder(orderObj, prevOrder);
	}

	
}
