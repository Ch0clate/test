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
@Table(name = "BUSORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class BusOrders extends ProductOrder  implements Serializable{


	private static final long serialVersionUID = 1L;


//	@OneToMany(mappedBy="busOrders", cascade=CascadeType.ALL)
//	private Set<BusPassengerDetails> passengerDetails;
	
	
	
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
	public String getSuppTimeLimitExpiryDate() {
		return suppTimeLimitExpiryDate;
	}
	public void setSuppTimeLimitExpiryDate(String suppTimeLimitExpiryDate) {
		this.suppTimeLimitExpiryDate = suppTimeLimitExpiryDate;
	}
	@Column
	private String supplierPriceCurrencyCode;
	
	@Column
	private String rateOfExchange;
	@Column
	@Type(type = "StringJsonObject")
	private String BusDetails;
	@Column
	private String busPNR;
	@Column
	private String ticketNo;
	@Column
	private String bookingDate;
	@Column
	@Type(type = "StringJsonObject")
	private String paxDetails;
	
	@Column
	private String suppTimeLimitExpiryDate;
	
	public String getPaxDetails() {
		return paxDetails;
	}
	public void setPaxDetails(String paxDetails) {
		this.paxDetails = paxDetails;
	}
	public String getBookingDate() {
		return bookingDate;
	}
	public void setBookingDate(String bookingDate) {
		this.bookingDate = bookingDate;
	}
	public String getBusPNR() {
		return busPNR;
	}
	public void setBusPNR(String busPNR) {
		this.busPNR = busPNR;
	}
	public String getTicketNo() {
		return ticketNo;
	}
	public void setTicketNo(String ticketNo) {
		this.ticketNo = ticketNo;
	}
	public String getBusDetails() {
		return BusDetails;
	}
	public void setBusDetails(String busDetails) {
		BusDetails = busDetails;
	}
//	public Set<BusPassengerDetails> getPassengerDetails() {
//		return passengerDetails;
//	}
//	public void setPassengerDetails(Set<BusPassengerDetails> passengerDetails) {
//		this.passengerDetails = passengerDetails;
//	}
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
	public String getRateOfExchange() {
		return rateOfExchange;
	}
	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}

}
