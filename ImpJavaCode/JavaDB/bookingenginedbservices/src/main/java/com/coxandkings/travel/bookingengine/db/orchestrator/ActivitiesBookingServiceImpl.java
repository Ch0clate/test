package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesPassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.ActivitiesDatabaseRepository;

@Service
public class ActivitiesBookingServiceImpl {

	@Autowired
	@Qualifier("Activity")
	private ActivitiesDatabaseRepository activitiesDatabaseRepository;
	
	public JSONArray process(Booking booking) {
		List<ActivitiesOrders> activitiesOrders = activitiesDatabaseRepository.findByBooking(booking);
		JSONArray activitiesOrdersJson = getActivitiesOrdersJson(activitiesOrders);
		
		return activitiesOrdersJson;
	}

	private JSONArray getActivitiesOrdersJson(List<ActivitiesOrders> activitiesOrders) {
		JSONArray activitiesArray = new JSONArray();
		JSONObject activitiesJson = null;
		for (ActivitiesOrders order : activitiesOrders) {
			
			activitiesJson = getActivitiesOrderJsonObject(order);
			activitiesArray.put(activitiesJson);

		}
		return activitiesArray;
	}

	private JSONObject getActivitiesOrderJsonObject(ActivitiesOrders order) {
		JSONObject activitiesJson = new JSONObject();
		activitiesJson.put("productCategory", Constants.JSON_PROP_ACTIVITIES_CATEGORY);
		
		// TODO : Need to chk on it
		activitiesJson.put("productSubCategory",  Constants.JSON_PROP_ACTIVITIES_SUBCATEGORY);
		activitiesJson.put("lastUpdatedBy", order.getLastUpdatedBy());
		activitiesJson.put("cancelDate", order.getCancelDate());
		activitiesJson.put("supplierID", order.getSupplierID());
		activitiesJson.put("orderID", order.getId());
		// TODO : Need to check 
		activitiesJson.put("supplierRateType", "");
		activitiesJson.put("amendDate", order.getAmendDate());
		
		// TODO : Not Known as of now
		activitiesJson.put("reconfirmationDate", "");
		// TODO : Need to check 
		activitiesJson.put("inventory", "");
		activitiesJson.put("enamblerSupplierName", order.getSupplierID());
		// TODO : Need to check 
		activitiesJson.put("suppReconfirmationDate", "");
		activitiesJson.put("createdAt",  order.getCreatedAt());
		activitiesJson.put("sourceSupplierName", order.getSupplierID());
		// TODO : Need to check 
		activitiesJson.put("credentialsName", "");
		activitiesJson.put("status", order.getStatus());
		
//		JSONObject orderDetails = new JSONObject();
//		orderDetails.put("orderDetails", getActivitiesDetails(order));
		activitiesJson.put("orderDetails", getActivitiesDetails(order));
		
		return activitiesJson;
	}

	private JSONObject getActivitiesDetails(ActivitiesOrders order) {
		JSONObject activitiesJson = new JSONObject();
		
		activitiesJson.put("adultCount", order.getAdultCount());
		activitiesJson.put("answers", new JSONArray(order.getAnswers()));
		activitiesJson.put("bookingDateTime", order.getBookingDateTime());
		activitiesJson.put("childCount", order.getChildCount());
		activitiesJson.put("cityCode", order.getCityCode());
		
		
		// TODO :client and SupplierComercial pending now
         activitiesJson.put("orderClientCommercials", getClientComms(order)); 
		
		activitiesJson.put("clientCurrency", order.getClientCurrency());
		activitiesJson.put("clientIATANumber", order.getClientIATANumber());
		activitiesJson.put("clientID", order.getClientID());
		activitiesJson.put("clientType", order.getClientType());
		activitiesJson.put("orderTotalPriceInfo", new JSONArray(order.getCommercialPaxTypeFares()));
		
		
		// Need to convert it to Array.
		activitiesJson.put("contactDetail", new JSONObject(order.getContactDetail()));
		
		activitiesJson.put("countryCode", order.getCountryCode());
		
		activitiesJson.put("endDate", order.getEndDate());
		
		activitiesJson.put("name", order.getName());
		
		activitiesJson.put("paxInfo", getPaxInfoJson(order));
		
		activitiesJson.put("pickupDropoff", new JSONObject(order.getPickupDropoff()));
		activitiesJson.put("POS", new JSONObject(order.getPOS()));
		
		activitiesJson.put("shipping_Details", new JSONObject(order.getShipping_Details()));
		activitiesJson.put("startDate", order.getStartDate());
		activitiesJson.put("status", order.getStatus());
		activitiesJson.put("supp_booking_reference", order.getSupp_booking_reference());
		
		// TODO : client and SupplierComercial pending now 
		activitiesJson.put("orderSupplierCommercials",  getSuppComms(order));
		
		activitiesJson.put("supplier_Details", new JSONObject(order.getSupplier_Details()));
		activitiesJson.put("supplierBrandCode", order.getSupplierBrandCode());
		activitiesJson.put("supplierID", order.getSupplierID());
		activitiesJson.put("supplierProductCode", order.getSupplierProductCode());
		
		activitiesJson.put("supplierPriceInfo", new JSONArray(order.getSuppPaxTypeFares()));
		activitiesJson.put("timeSlotDetails", new JSONArray(order.getTimeSlotDetails()));
		activitiesJson.put("tourLanguage", new JSONArray(order.getTourLanguage()));
		
		return activitiesJson;
	}

	private JSONArray getClientComms(ActivitiesOrders order) {
		JSONArray clientCommArray = new JSONArray();

		for (ClientCommercial clientComm : order.getClientCommercial()) {
			JSONObject clientCommJson = new JSONObject();

			clientCommJson.put("commercialName", clientComm.getCommercialName());
			clientCommJson.put("commercialType", clientComm.getCommercialType());

			clientCommJson.put("commercialAmount", clientComm.getCommercialAmount());
			clientCommJson.put("commercialCurrency", clientComm.getCommercialCurrency());
			clientCommJson.put("clientID", clientComm.getClientID());
			clientCommJson.put("parentClientID", clientComm.getParentClientID());
			clientCommJson.put("commercialEntityID", clientComm.getCommercialEntityID());
			clientCommJson.put("commercialEntityType", clientComm.getCommercialEntityType());
			clientCommJson.put("companyFlag", clientComm.isCompanyFlag());


			clientCommArray.put(clientCommJson);
		}
		return clientCommArray;
	}

	private JSONArray getSuppComms(ActivitiesOrders order) {
		JSONArray suppCommArray = new JSONArray();

		for (SupplierCommercial suppComm : order.getSuppcommercial()) {
			JSONObject suppCommJson = new JSONObject();
			suppCommJson.put("commercialName", suppComm.getCommercialName());
			suppCommJson.put("commercialType", suppComm.getCommercialType());

			suppCommJson.put("commercialAmount", suppComm.getCommercialAmount());
			suppCommJson.put("commercialCurrency", suppComm.getCommercialCurrency());

			suppCommArray.put(suppCommJson);
		}
		return suppCommArray;
	}

	private JSONArray getPaxInfoJson(ActivitiesOrders order) {
		JSONArray paxJsonArray = new JSONArray();
		for(ActivitiesPassengerDetails guest: order.getPassengerDetails()) {
			JSONObject paxInfo = new JSONObject();
			paxInfo.put("DOB", guest.getBirthDate());
			paxInfo.put("contactDetails", new JSONObject(guest.getContactDetails()));
			paxInfo.put("qualifierInfo", guest.getPassengerType());
			
			JSONObject personName = new JSONObject();
			personName.put("namePrefix", guest.getNamePrefix());
			personName.put("surname", guest.getSurname());
			personName.put("nameTitle", guest.getNameTitle());
			personName.put("givenName", guest.getGivenName());
			personName.put("middleName", guest.getMiddleName());
			
			paxInfo.put("personName", personName);
			paxInfo.put("passengerID", guest.getPassanger_id());
			paxJsonArray.put(paxInfo);
		}
		return paxJsonArray;
	}
}
