package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;

@Entity
@Table(name = "CRUISEORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class CruiseOrders extends ProductOrder implements Serializable 
{

	public CruiseOrders()
	{
		super();
	}
	private static final long serialVersionUID = 1L;
	
	@Column
	@Type(type = "StringJsonObject")
	private String paxDetails;
	
	@Column
	private String bookingDateTime;
	@Column
	private String status;
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
	private String rateOfExchange;
	
	@Column
	@Type(type = "StringJsonObject")
	private String cruiseDetails;
	
	@Column
	private String cabinNo;
	
	@Column
	private String pricedCategoryCode;
	
	@Column
	private String voyageID;
	
	@Column
	private String shipCode;
	
	@Column
	private String sailingID;
	
	@Column
	private String itineraryID;
	
	@Column
	private String startDate;
	
	@Column
	private String fareCode;
	
	@Column
	private String reservationID;
	
	@Column
	private String bookingCompanyName;

	public String getReservationID() {
		return reservationID;
	}

	public void setReservationID(String reservationID) {
		this.reservationID = reservationID;
	}

	public String getBookingCompanyName() {
		return bookingCompanyName;
	}

	public void setBookingCompanyName(String bookingCompanyName) {
		this.bookingCompanyName = bookingCompanyName;
	}

	public String getPaxDetails() {
		return paxDetails;
	}

	public void setPaxDetails(String paxDetails) {
		this.paxDetails = paxDetails;
	}

	public String getBookingDateTime() {
		return bookingDateTime;
	}

	public void setBookingDateTime(String bookingDateTime) {
		this.bookingDateTime = bookingDateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getSupplierID() {
		return supplierID;
	}

	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}

	public String getSupplierPrice() {
		return supplierPrice;
	}

	public void setSupplierPrice(String supplierPrice) {
		this.supplierPrice = supplierPrice;
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

	public String getTotalPriceTaxes() {
		return totalPriceTaxes;
	}

	public void setTotalPriceTaxes(String totalPriceTaxes) {
		this.totalPriceTaxes = totalPriceTaxes;
	}

	public String getRateOfExchange() {
		return rateOfExchange;
	}

	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}

	public String getCabinNo() {
		return cabinNo;
	}

	public void setCabinNo(String cabinNo) {
		this.cabinNo = cabinNo;
	}

	public String getPricedCategoryCode() {
		return pricedCategoryCode;
	}

	public void setPricedCategoryCode(String pricedCategoryCode) {
		this.pricedCategoryCode = pricedCategoryCode;
	}

	public String getVoyageID() {
		return voyageID;
	}

	public void setVoyageID(String voyageID) {
		this.voyageID = voyageID;
	}

	public String getShipCode() {
		return shipCode;
	}

	public void setShipCode(String shipCode) {
		this.shipCode = shipCode;
	}

	public String getSailingID() {
		return sailingID;
	}

	public void setSailingID(String sailingID) {
		this.sailingID = sailingID;
	}

	public String getItineraryID() {
		return itineraryID;
	}

	public void setItineraryID(String itineraryID) {
		this.itineraryID = itineraryID;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getFareCode() {
		return fareCode;
	}

	public void setFareCode(String fareCode) {
		this.fareCode = fareCode;
	}

	public String getCruiseDetails() {
		return cruiseDetails;
	}

	public void setCruiseDetails(String cruiseDetails) {
		this.cruiseDetails = cruiseDetails;
	}

}
