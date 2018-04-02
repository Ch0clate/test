package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.BusDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class BusBookingServiceImpl implements Constants {

	@Qualifier("Bus")
	@Autowired
	private BusDatabaseRepository busRepository;
	
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository AmClRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	public JSONArray process(Booking booking, String flag) {
		List<BusOrders> busOrders = busRepository.findByBooking(booking);
		JSONArray busOrdersJson = getBusOrdersJson(busOrders,flag);
		return busOrdersJson;
	}

	public JSONArray getBusOrdersJson(List<BusOrders> orders, String flag) {
		
		JSONArray busArray = new JSONArray();
		JSONObject busJson = new JSONObject();
		for (BusOrders order : orders) {
			
			busJson = getBusOrderJson(order,flag);
			busArray.put(busJson);
		}
		return busArray;
	
	}

	public JSONArray getCancellationsByBooking(Booking booking) {

		List<BusOrders> busOrders = busRepository.findByBooking(booking);
		JSONArray busOrdersJson = getBusOrdersCancellations(busOrders, "cancel");
		return busOrdersJson;

	}
	public JSONObject getBusOrderJson(BusOrders order, String flag) {

		JSONObject busJson = new JSONObject();
		
		//TODO: to check from where will we get these details from WEM
		busJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		
		//TODO: added these fields on the suggestions of operations
		busJson.put("QCStatus",order.getQCStatus());
		busJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		busJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		busJson.put("suppTimeLimitExpiryDate", order.getSuppTimeLimitExpiryDate());
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		busJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		busJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

		
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		busJson.put(JSON_PROP_CANCELDATE, "");
		busJson.put(JSON_PROP_AMENDDATE, "");
		busJson.put(JSON_PROP_INVENTORY, "N");


		busJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		busJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_BUS_PRODUCTSUBCATEGORY);
		busJson.put(JSON_PROP_ORDERID, order.getId());
		busJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		busJson.put(JSON_PROP_STATUS, order.getStatus());
		busJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		
		JSONObject orderDetails = new JSONObject();
//		orderDetails.put(JSON_PROP_SUPPCOMM, getSuppComms(order));
//		orderDetails.put(JSON_PROP_CLIENTCOMM,getClientComms(order));
		if(flag=="false")
		{	
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS,getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
	    }
		orderDetails.put(JSON_PROP_BUS_BUSDETAILS, new JSONObject(order.getBusDetails()));
		orderDetails.put(JSON_PROP_PAXINFO, getPassInfoJson(order));
		orderDetails.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
		orderDetails.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(order));
		orderDetails.put("ticketNo", order.getTicketNo());
		orderDetails.put("PNRNo", order.getBusPNR());
		busJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return busJson;
	}

//	private BusOrders saveOrder(BusOrders order) {
//		BusOrders orderObj = null;
//		try {
//			orderObj = CopyUtils.copy(order, BusOrders.class);
//
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//
//			e.printStackTrace();
//		}
//		return busRepository.saveOrder(orderObj,"");
//	}
	
//	private BusPassengerDetails savePaxDetails(BusPassengerDetails pax) {
//		BusPassengerDetails orderObj = null;
//		try {
//			orderObj = CopyUtils.copy(pax, BusPassengerDetails.class);
//
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//
//			e.printStackTrace();
//		}
//		return busPassReposiory.saveOrder(orderObj);
//	}
	
	
	
	private JSONObject getTotalPriceInfoJson(BusOrders order) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put("TotalFare", new BigDecimal(order.getTotalPrice()));
		totalPriceJson.put("TotalFareCurrency", order.getTotalPriceCurrencyCode());
		return totalPriceJson;
	}

	private JSONObject getSuppPriceInfoJson(BusOrders order) {
		JSONObject suppPriceJson = new JSONObject();

//		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierTotalPrice());
		suppPriceJson.put(JSON_PROP_SUPPPRICE, new BigDecimal(order.getSupplierTotalPrice()));
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		return suppPriceJson;
	}

	private JSONArray getPassInfoJson(BusOrders order) {

//		JSONArray passJsonArray = new JSONArray();
//		JSONObject passJson =new JSONObject();
//		
//		for(BusPassengerDetails guest: order.getPassengerDetails()) {
//			passJson.put(JSON_PROP_TITLE,guest.getTitle());
//			passJson.put("Name",guest.getName());
//			passJson.put("IdNumber",guest.getIdNumber());
//			passJson.put("IdType",guest.getIdType());
//			passJson.put("Age",guest.getAge());
//			passJson.put("Gender",guest.getGender());
//			passJson.put("SeatNo",guest.getSeatNo());
//			passJson.put("seatTypesList",guest.getSeatTypesList());
//			passJson.put("seatTypeIds",guest.getSeatTypeIds());
//
//			passJsonArray.put(passJson);
//		}
//		return passJsonArray;
		
		
		JSONArray paxJsonArray = new JSONArray();
		
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_AIR_PASSENGERID,guest.getPassanger_id());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());
			paxJson.put(JSON_PROP_STATUS, guest.getStatus());
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
			
			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;

	}

	private JSONArray getClientComms(BusOrders order) {

//		JSONObject clientCommJson = new JSONObject();
		JSONArray clientCommArray = new JSONArray();
		
		for(ClientCommercial clientComm: order.getClientCommercial()) {
			
			JSONObject clientCommJson = new JSONObject();
//			clientCommJson.put(JSON_PROP_COMMERCIALNAME, clientComm.getCommercialName());
//			clientCommJson.put(JSON_PROP_COMMERCIALTYPE, clientComm.getCommercialType());
//			clientCommJson.put(JSON_PROP_COMMERCIALCURRENCY, clientComm.getCommercialCurrency());
//			clientCommJson.put(JSON_PROP_COMMERCIALTOTALAMOUNT, new BigDecimal(clientComm.getCommercialAmount()));
			
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

	private JSONArray getSuppComms(BusOrders order) {
		
//		JSONObject suppCommJson = new JSONObject();
		JSONArray suppCommArray = new JSONArray();
		for(SupplierCommercial suppComm: order.getSuppcommercial()) {
			
			JSONObject suppCommJson = new JSONObject();
			suppCommJson.put(JSON_PROP_COMMERCIALNAME, suppComm.getCommercialName());
			suppCommJson.put(JSON_PROP_COMMERCIALTYPE, suppComm.getCommercialType());
//			suppCommJson.put(JSON_PROP_BEFORECOMMERCIALAMOUNT, suppComm.getBeforeCommercialAmount());
//			suppCommJson.put("beofreCommercialAmount", suppComm.getBeforeCommercialAmount());
//			suppCommJson.put(JSON_PROP_COMMERCIALCALCULATIONPERCENTAGE, suppComm.getCommercialCalculationPercentage());
//			suppCommJson.put(JSON_PROP_COMMERCIALCALCULATIONAMOUNT, suppComm.getCommercialName());
//			suppCommJson.put(JSON_PROP_COMMERCIALFARECOMPONENT, suppComm.getCommercialFareComponent());
			suppCommJson.put(JSON_PROP_COMMERCIALCURRENCY, suppComm.getCommercialCurrency());
			suppCommJson.put(JSON_PROP_COMMERCIALTOTALAMOUNT, new BigDecimal(suppComm.getCommercialAmount()));
//			suppCommJson.put(JSON_PROP_AFTERCOMMERCIALTOTALAMOUNT, suppComm.getAfterCommercialAmount());
//			suppCommJson.put(JSON_PROP_AFTERCOMMERCIALBASEFARE, suppComm.getAfterCommercialBaseFare());
//			suppCommJson.put(JSON_PROP_AFTERCOMMERCIALTAXDETAILS, suppComm.getAfterCommercialTaxDetails());
//			suppCommJson.put(JSON_PROP_RECIEPTNUMBER, suppComm.getRecieptNumber());
//			suppCommJson.put(JSON_PROP_INVOICENUMBER, suppComm.getInVoiceNumber());
			
			suppCommArray.put(suppCommJson);
		}		
		return suppCommArray;
	}

	
	private JSONArray getBusOrdersCancellations(List<BusOrders> busOrders,String type )
	{
		JSONArray response = new JSONArray();
		for (BusOrders order : busOrders)
		{
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			List<AmCl> cancelBusOrders = AmClRepository.findByEntity("order", orderId, type);
			
			if(cancelBusOrders.size()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				JSONArray orderCancelArray = new JSONArray();

				for (AmCl cancelBusOrder : cancelBusOrders) {
					JSONObject cancelOrderJson = new JSONObject();
					cancelOrderJson.put("supplierCancelCharges", cancelBusOrder.getSupplierCharges());
					cancelOrderJson.put("companyCancelCharges", cancelBusOrder.getCompanyCharges());
					cancelOrderJson.put("supplierCancelChargesCurrencyCode",
							cancelBusOrder.getSupplierChargesCurrencyCode());
					cancelOrderJson.put("companyCancelChargesCurrencyCode",
							cancelBusOrder.getCompanyChargesCurrencyCode());
					cancelOrderJson.put("cancelType", cancelBusOrder.getDescription());
					cancelOrderJson.put("createdAt", cancelBusOrder.getCreatedAt());
					cancelOrderJson.put("lastModifiedAt", cancelBusOrder.getLastModifiedAt());
					cancelOrderJson.put("lastModifiedBy", cancelBusOrder.getLastModifiedBy());
					orderCancelArray.put(cancelOrderJson);
				}

				orderJson.put("orderCancellations", orderCancelArray);
				JSONArray paxJsonArray = new JSONArray();
				List<AmCl> cancelPaxOrders = AmClRepository.findByEntity("pax", orderId, type);
				
				if(cancelPaxOrders.size()>0) {
					JSONArray orderCancelPaxArray = new JSONArray();
					for (AmCl cancelPaxOrder : cancelPaxOrders) {
						JSONObject cancelPaxOrderJson = new JSONObject();
						cancelPaxOrderJson.put("supplierCancelCharges", cancelPaxOrder.getSupplierCharges());
						cancelPaxOrderJson.put("companyCancelCharges", cancelPaxOrder.getCompanyCharges());
						cancelPaxOrderJson.put("supplierCancelChargesCurrencyCode",
								cancelPaxOrder.getSupplierChargesCurrencyCode());
						cancelPaxOrderJson.put("companyCancelChargesCurrencyCode",
								cancelPaxOrder.getCompanyChargesCurrencyCode());
						cancelPaxOrderJson.put("cancelType", cancelPaxOrder.getDescription());
						cancelPaxOrderJson.put("createdAt", cancelPaxOrder.getCreatedAt());
						cancelPaxOrderJson.put("lastModifiedAt", cancelPaxOrder.getLastModifiedAt());
						cancelPaxOrderJson.put("lastModifiedBy", cancelPaxOrder.getLastModifiedBy());
						String entityIDs = cancelPaxOrder.getEntityID();
						String newEntityIds = entityIDs.replaceAll("\\|", "\\,");
						cancelPaxOrderJson.put("paxIDs", newEntityIds);
						orderCancelPaxArray.put(cancelPaxOrderJson);
						
					}
						
					orderJson.put("paxCancellations", orderCancelPaxArray);
					}
				response.put(orderJson);
		}
		
		
	}
		return response;

	}
	
	
}
