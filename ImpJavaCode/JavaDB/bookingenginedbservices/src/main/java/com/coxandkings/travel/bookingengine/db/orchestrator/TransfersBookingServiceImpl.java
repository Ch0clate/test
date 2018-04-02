package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
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
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.TransfersDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class TransfersBookingServiceImpl implements Constants{
	
	@Qualifier("Transfers")
	@Autowired
	private TransfersDatabaseRepository transfersRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	public JSONArray process(Booking booking) {
		List<TransfersOrders> transferOrders = transfersRepository.findByBooking(booking);
		JSONArray transfersOrdersJson = getTransferOrdersJson(transferOrders);
		return transfersOrdersJson;
	}
	
	public String getBysuppID(String suppID) {
		
		List<TransfersOrders> orders = transfersRepository.findBysuppID(suppID);
		//TODO: check if they need bookID inside each car order
		JSONArray ordersArray = getTransferOrdersJson(orders);
		return ordersArray.toString();
	}
	

	public String updateOrder(JSONObject reqJson, String updateType) {

		switch(updateType)
        {
		
        }
		return null;
	}
	
	public JSONArray getTransferOrdersJson(List<TransfersOrders> orders) {
		
		JSONArray transfersArray = new JSONArray();
		JSONObject transfersJson = new JSONObject();
		for (TransfersOrders order : orders) {
			
			transfersJson = getTransferOrdersJson(order);
			transfersArray.put(transfersJson);
		}
		return transfersArray;	
	}
	
	public JSONObject getTransferOrdersJson(TransfersOrders order) {

		JSONObject TransfersJson = new JSONObject();
		TransfersJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		TransfersJson.put(JSON_PROP_CANCELDATE, "");
		TransfersJson.put(JSON_PROP_AMENDDATE, "");
		TransfersJson.put(JSON_PROP_INVENTORY, "N");
		
		TransfersJson.put(JSON_PROP_PRODUCTCATEGORY, "Transportation");
		TransfersJson.put(JSON_PROP_PRODUCTSUBCATEGORY, "Transfers");
		TransfersJson.put(JSON_PROP_ORDERID, order.getId());
		TransfersJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		TransfersJson.put(JSON_PROP_STATUS, order.getStatus());
		TransfersJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		
		JSONObject orderDetails = new JSONObject();
		
		//TODO: These is set at credential name and supplier level
		orderDetails.put(JSON_PROP_TRANSFERS_TRIPTYPE, order.getTripType());
		orderDetails.put(JSON_PROP_SUPPBOOKREF, order.getSuppBookRef());
		orderDetails.put(JSON_PROP_TRANSFERS_TRANSFERSDETAILS, new JSONObject(order.getTransfersDetails()));
		orderDetails.put(JSON_PROP_TRANSFERS_TRIPINDICATOR,order.getTripIndicator());
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO,  new JSONObject(order.getSuppFares()));
		orderDetails.put(JSON_PROP_PAXDETAILS, getPassengerInfo(order));
		
		TransfersJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return TransfersJson;
	}
	



	private JSONArray getSuppComms(TransfersOrders order) {
		
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
	
	private JSONArray getClientComms(TransfersOrders order) {
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
	
	private JSONArray getPassengerInfo(TransfersOrders order) {
		
		JSONArray paxJsonArray = new JSONArray();

		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();
			
			
			paxJson.put(JSON_PROP_TRANS_PASSENGERID, guest.getPassanger_id());
			paxJson.put(JSON_PROP_TRANS_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_TRANSFERS_RPH,guest.getRph());
			paxJson.put(JSON_PROP_PAX_TYPE,guest.getPaxType());
			paxJson.put(JSON_PROP_TRANSFERS_AGE,guest.getAge());
			paxJson.put(JSON_PROP_TRANSFERS_QUANTITY,guest.getQuantity());
			paxJson.put(JSON_PROP_TRANSFERS_PHONENUMBER,guest.getContactDetails());
			paxJson.put(JSON_PROP_TRANSFERS_EMAIL,guest.getEmail());
			paxJson.put(JSON_PROP_TRANSFERS_PERSONNAME,new JSONObject(guest.getPersonName()));
			
			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;
		
	}
	
	private TransfersOrders saveOrder(TransfersOrders order, String prevOrder) {
		TransfersOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, TransfersOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return transfersRepository.saveOrder(orderObj, prevOrder);
	}

	
}
