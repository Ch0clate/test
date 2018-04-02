package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;




@Entity
@Table(name = "AIRORDERS",indexes= {@Index(name = "IDX_MYIDX2", columnList = "flightDetails")})
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class AirOrders extends ProductOrder  implements Serializable {

	/**
	 * 
	 */
	public AirOrders() {
		super();
		operationType="update";
	}
	
	private static final long serialVersionUID = 1L;


	@Column
	private String tripType;
	@Column
	private String tripIndicator;
	@Column
	private String bookingDateTime;
	@Column
	private String amendDate;
	@Column
	private String cancelDate;

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	@Column
	private String supplierID;
	
	//Prices
	
	@Column
	private String supplierPrice;
	@Column
	private String totalPrice;
	@Column
	private String totalPriceCurrencyCode;
	@Column
	private String supplierPriceCurrencyCode;
	@Column
	@Type(type = "StringJsonObject")
	private String SuppPaxTypeFares;
	
	@Column
	@Type(type = "StringJsonObject")
	private String totalPaxTypeFares;
	
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceBaseFare;
	
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceReceivables;
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceFees;
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceTaxes;
	
	@Column
	@Type(type = "StringJsonObject")
	private String paxDetails;
	
	@Column
	private String rateOfExchange;
	

	@Column
	@Type(type = "StringJsonObject")
	private String flightDetails;
	
	//TODO: these are new fields  added as per ops requirements: confirm from where we are going to get these fields.

	@Column
	private String credentialsName;
	@Column
	private String supplierRateType;
	@Column
	private String inventory;
	@Column
	private String enamblerSupplierName;
	@Column
	private String sourceSupplierName;
	@Column
	private String airlinePNR;
	@Column
	private String GDSPNR;
	@Column
	private String  ticketingPNR;
	@Column
	private String ticketingPCC;
	//Only for GDS suppliers.
	@Column
	private String suppTimeLimitExpiryDate;


	
	
	
	@Transient
	private String operationType;
	
	

	//TODO : need to check if tax breakup comes at total level for AIR
	/*@Column
	private String totalTaxAmount;
	@Column
	private String totalTaxBreakup;
	@Column
	private String supplierTaxAmount;
	@Column
	private String supplierTaxBreakup;*/


	
	
	
	public String getOperationType() {
		return operationType;
	}


	public String getSuppTimeLimitExpiryDate() {
		return suppTimeLimitExpiryDate;
	}


	public void setSuppTimeLimitExpiryDate(String suppTimeLimitExpiryDate) {
		this.suppTimeLimitExpiryDate = suppTimeLimitExpiryDate;
	}


	public String getTotalPriceBaseFare() {
		return totalPriceBaseFare;
	}

	public void setTotalPriceBaseFare(String totalPriceBaseFare) {
		this.totalPriceBaseFare = totalPriceBaseFare;
	}

	public String getTotalPriceReceivables() {
		return totalPriceReceivables;
	}

	public void setTotalPriceReceivables(String totalPriceReceivables) {
		this.totalPriceReceivables = totalPriceReceivables;
	}

	public String getTotalPriceFees() {
		return totalPriceFees;
	}

	public void setTotalPriceFees(String totalPriceFees) {
		this.totalPriceFees = totalPriceFees;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	
	public String getCredentialsName() {
		return credentialsName;
	}

	public void setCredentialsName(String credentialsName) {
		this.credentialsName = credentialsName;
	}

	public String getSupplierRateType() {
		return supplierRateType;
	}

	public void setSupplierRateType(String supplierRateType) {
		this.supplierRateType = supplierRateType;
	}

	public String getInventory() {
		return inventory;
	}

	public void setInventory(String inventory) {
		this.inventory = inventory;
	}

	public String getRateOfExchange() {
		return rateOfExchange;
	}

	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}

	public String getTripType() {
		return tripType;
	}

	public void setTripType(String tripType) {
		this.tripType = tripType;
	}

	public String getTripIndicator() {
		return tripIndicator;
	}

	public void setTripIndicator(String tripIndicator) {
		this.tripIndicator = tripIndicator;
	}

	public String getBookingDateTime() {
		return bookingDateTime;
	}

	public void setBookingDateTime(String bookingDateTime) {
		this.bookingDateTime = bookingDateTime;
	}

	public String getAmendDate() {
		return amendDate;
	}

	public void setAmendDate(String amendDate) {
		this.amendDate = amendDate;
	}

	public String getCancelDate() {
		return cancelDate;
	}

	public void setCancelDate(String cancelDate) {
		this.cancelDate = cancelDate;
	}

	
	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public ZonedDateTime getLastModifiedAt() {
		return lastModifiedAt;
	}

	public void setLastModifiedAt(ZonedDateTime lastModifiedAt) {
		this.lastModifiedAt = lastModifiedAt;
	}
	
	

	public String getLastModifiedBy() {
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	public String getSupplierPrice() {
		return supplierPrice;
	}

	public void setSupplierPrice(String supplierTotalPrice) {
		this.supplierPrice = supplierTotalPrice;
	}

	public String getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(String totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getTotalPriceCurrencyCode() {
		return totalPriceCurrencyCode;
	}

	public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
		this.totalPriceCurrencyCode = totalPriceCurrencyCode;
	}

	public String getSupplierPriceCurrencyCode() {
		return supplierPriceCurrencyCode;
	}

	public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
		this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
	}

	public String getSuppPaxTypeFares() {
		return SuppPaxTypeFares;
	}

	public void setSuppPaxTypeFares(String suppPaxTypeFares) {
		SuppPaxTypeFares = suppPaxTypeFares;
	}

	public String getTotalPaxTypeFares() {
		return totalPaxTypeFares;
	}

	public void setTotalPaxTypeFares(String totalPaxTypeFares) {
		this.totalPaxTypeFares = totalPaxTypeFares;
	}

	public String getFlightDetails() {
		return flightDetails;
	}

	public void setFlightDetails(String flightDetails) {
		this.flightDetails = flightDetails;
	}

	public String getSupplierID() {
		return supplierID;
	}

	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}

	public String getEnamblerSupplierName() {
		return enamblerSupplierName;
	}

	public void setEnamblerSupplierName(String enamblerSupplierName) {
		this.enamblerSupplierName = enamblerSupplierName;
	}

	public String getSourceSupplierName() {
		return sourceSupplierName;
	}

	public void setSourceSupplierName(String sourceSupplierName) {
		this.sourceSupplierName = sourceSupplierName;
	}

	public String getAirlinePNR() {
		return airlinePNR;
	}

	public void setAirlinePNR(String airlinePNR) {
		this.airlinePNR = airlinePNR;
	}

	public String getGDSPNR() {
		return GDSPNR;
	}

	public void setGDSPNR(String gDSPNR) {
		GDSPNR = gDSPNR;
	}

	public String getTicketingPNR() {
		return ticketingPNR;
	}

	public void setTicketingPNR(String ticketingPNR) {
		this.ticketingPNR = ticketingPNR;
	}

	public String getTicketingPCC() {
		return ticketingPCC;
	}

	public void setTicketingPCC(String ticketingPCC) {
		this.ticketingPCC = ticketingPCC;
	}
	public String getTotalPriceTaxes() {
		return totalPriceTaxes;
	}

	public void setTotalPriceTaxes(String totalPriceTaxes) {
		this.totalPriceTaxes = totalPriceTaxes;
	}

	public String getPaxDetails() {
		return paxDetails;
	}


	public void setPaxDetails(String paxDetails) {
		this.paxDetails = paxDetails;
	}


	@Override
	public String toString() {
		
		JSONObject airJson = new JSONObject();
		
		airJson.put("bookID", booking.getBookID());
		airJson.put("lastModifiedAt", lastModifiedAt);
		
		    JSONObject testJson = new JSONObject();
		
	       testJson.put("bookID", booking.getBookID());
			testJson.put("id", id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			testJson.put("rateOfExchange", rateOfExchange);
			
			testJson.put("tripType",tripType);
			testJson.put("tripIndicator",tripIndicator);
			testJson.put("bookingDateTime",bookingDateTime);
			testJson.put("supplierTotalPrice",supplierPrice);
			testJson.put("totalPrice",totalPrice);
			testJson.put("totalPriceCurrencyCode",totalPriceCurrencyCode);
			testJson.put("supplierPriceCurrencyCode",supplierPriceCurrencyCode);
			testJson.put("enamblerSupplierName",enamblerSupplierName);
			testJson.put("sourceSupplierName",sourceSupplierName);
			testJson.put("airlinePNR",airlinePNR);
			testJson.put("GDSPNR",GDSPNR);
			testJson.put("ticketingPNR",ticketingPNR);
			testJson.put("SuppPaxTypeFares", new JSONArray(SuppPaxTypeFares));
			testJson.put("totalPaxTypeFares", new JSONArray(totalPaxTypeFares) );
			testJson.put("flightDetails", new JSONObject(flightDetails) );

			testJson.put("amendDate", amendDate);
			testJson.put("cancelDate", cancelDate);
			testJson.put("lastModifiedBy", lastModifiedBy);
			testJson.put("supplierID", supplierID);
			testJson.put("ticketingPCC", ticketingPCC);
			testJson.put("credentialsName", credentialsName);
			
			testJson.put("supplierRateType", supplierRateType);
			testJson.put("inventory", inventory);
		   
		
		if(operationType.equalsIgnoreCase("insert"))	{
			    testJson.put("supplierComms", this.getSuppcommercial());
			    testJson.put("clientComms", this.getClientCommercial());
				JSONObject bookingJson = new JSONObject(booking.toString());
				bookingJson.put("paymentInfo", new JSONArray(booking.getPaymentInfo().toString()));
				testJson.put("bookingInfo", bookingJson);
		}
		
	
		airJson.put("data_value", testJson);
		return airJson.toString();

	
	}	
	
}
