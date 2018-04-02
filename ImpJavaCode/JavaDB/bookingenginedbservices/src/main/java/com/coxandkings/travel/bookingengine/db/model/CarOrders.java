package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "CARORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class CarOrders extends ProductOrder  implements Serializable {

	/**
	 * 
	 */
	public CarOrders() {
		super();
		operationType="update";
	}
	

	private static final long serialVersionUID = 1L;

	
	//In case of Indian Suppliers
	@Column
	private String tripType;
	
	@Column
	private String bookingDateTime;
	@Column
	private String amendDate;
	@Column
	private String cancelDate;

	@Column
	private String suppBookRef;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	@Column
	private String supplierID;
	@Column
	private String clientID;
	@Column
	private String clientType;
	@Column
	private String clientCurrency;
	@Column
	private String clientIATANumber;
	@Column
	private String supplierTotalPrice;
	@Column
	private String totalPrice;
	@Column
	private String totalPriceCurrencyCode;
	@Column
	private String supplierPriceCurrencyCode;
	@Column
	private String rateOfExchange;
	
	@Column
	@Type(type = "StringJsonObject")
	private String suppFares;
	
	@Column
	@Type(type = "StringJsonObject")
	private String carDetails;
	
	@Column
	@Type(type = "StringJsonObject")
	private String rentalDetails;
	
	@Column
	@Type(type = "StringJsonObject")
	private String paxDetails;
	
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceReceivables;
	@Column
	@Type(type = "StringJsonObject")
	private String totalBaseFare;
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceFees;
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceTaxes;
	
	//Ancillaries
	@Column
	@Type(type = "StringJsonObject")
	private String extraEquipments;
	@Column
	@Type(type = "StringJsonObject")
	private String pricedCoverages;
	
	//TODO: these are new fields  added as per ops requirements: confirm from where we are going to get these fields.

	@Column
	private String credentialsName;
	@Column
	private String supplierRateType;

	@Transient
	private String operationType;

	//TODO : need to check if tax breakup comes at total level for CAR
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

	public String getRateOfExchange() {
		return rateOfExchange;
	}

	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}

	public String getSuppFares() {
		return suppFares;
	}

	public void setSuppFares(String suppFares) {
		this.suppFares = suppFares;
	}
	
	public String getSuppBookRef() {
		return suppBookRef;
	}

	public void setSuppBookRef(String suppBookRef) {
		this.suppBookRef = suppBookRef;
	}

	public String getTripType() {
		return tripType;
	}

	public void setTripType(String tripType) {
		this.tripType = tripType;
	}
	
	public String getExtraEquipments() {
		return extraEquipments;
	}

	public void setExtraEquipments(String extraEquipments) {
		this.extraEquipments = extraEquipments;
	}

	public String getPricedCoverages() {
		return pricedCoverages;
	}

	public void setPricedCoverages(String pricedCoverages) {
		this.pricedCoverages = pricedCoverages;
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

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getClientCurrency() {
		return clientCurrency;
	}

	public void setClientCurrency(String clientCurrency) {
		this.clientCurrency = clientCurrency;
	}

	public String getClientIATANumber() {
		return clientIATANumber;
	}

	public void setClientIATANumber(String clientIATANumber) {
		this.clientIATANumber = clientIATANumber;
	}

	public String getSupplierTotalPrice() {
		return supplierTotalPrice;
	}

	public void setSupplierTotalPrice(String supplierTotalPrice) {
		this.supplierTotalPrice = supplierTotalPrice;
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

	public String getCarDetails() {
		return carDetails;
	}

	public void setCarDetails(String carDetails) {
		this.carDetails = carDetails;
	}
	
	public String getRentalDetails() {
		return rentalDetails;
	}

	public void setRentalDetails(String rentalDetails) {
		this.rentalDetails = rentalDetails;
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

	public String getTotalPriceTaxes() {
		return totalPriceTaxes;
	}

	public void setTotalPriceTaxes(String totalPriceTaxes) {
		this.totalPriceTaxes = totalPriceTaxes;
	}
	
	public String getTotalBaseFare() {
		return totalBaseFare;
	}

	public void setTotalBaseFare(String totalBaseFare) {
		this.totalBaseFare = totalBaseFare;
	}	
	
	public String getPaxDetails() {
		return paxDetails;
	}

	public void setPaxDetails(String paxDetails) {
		this.paxDetails = paxDetails;
	}


	public String getSupplierID() {
		return supplierID;
	}

	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}

	@Override
	public String toString() {
		
		JSONObject carJson = new JSONObject();
		
		carJson.put("bookID", booking.getBookID());
		carJson.put("lastModifiedAt", lastModifiedAt);
		
		    JSONObject testJson = new JSONObject();
		
	       testJson.put("bookID", booking.getBookID());
			testJson.put("id", id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			testJson.put("rateOfExchange", rateOfExchange);
			
			testJson.put("tripType",tripType);
			testJson.put("bookingDateTime",bookingDateTime);
			testJson.put("supplierTotalPrice",supplierTotalPrice);
			testJson.put("supplierPriceCurrencyCode",supplierPriceCurrencyCode);
			testJson.put("totalPrice",totalPrice);
			testJson.put("totalPriceCurrencyCode",totalPriceCurrencyCode);
		
			testJson.put("SuppFares", new JSONObject(suppFares));
			testJson.put("totalBaseFares", new JSONObject(totalBaseFare));
			testJson.put("carDetails", new JSONObject(carDetails));
			testJson.put("rentalDetails", new JSONObject(rentalDetails) );
			testJson.put("extraEquipments", new JSONObject(extraEquipments));
			
			testJson.put("amendDate", amendDate);
			testJson.put("cancelDate", cancelDate);
			testJson.put("lastModifiedBy", lastModifiedBy);
			testJson.put("supplierID", supplierID);
			testJson.put("clientID", clientID);
			testJson.put("clientType", clientType);
			testJson.put("clientCurrency", clientCurrency);
			
			testJson.put("clientIATANumber", clientIATANumber);
			testJson.put("credentialsName", credentialsName);
			
			testJson.put("supplierRateType", supplierRateType);
		   
		
		if(operationType.equalsIgnoreCase("insert"))	{
			    testJson.put("supplierComms", this.getSuppcommercial());
			    testJson.put("clientComms", this.getClientCommercial());
				JSONObject bookingJson = new JSONObject(booking.toString());
				bookingJson.put("paymentInfo", new JSONArray(booking.getPaymentInfo().toString()));
				testJson.put("bookingInfo", bookingJson);
		}
		
	
		carJson.put("data_value", testJson);
		return carJson.toString();

	
	}

	
}
