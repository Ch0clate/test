package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.json.JSONObject;

@Entity
@Table(name="BOOKING")
public class Booking implements Serializable {     					
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="BOOKID")
	private String bookID;
	
	@OneToMany(mappedBy="booking", cascade=CascadeType.ALL)
	private Set<PaymentInfo> paymentInfo;
	
	@OneToMany(mappedBy="booking", cascade=CascadeType.ALL)
	private Set<ProductOrder> productOrders;

	@Column
	private String userID;
	@Column
	private String sessionID;
	@Column
	private String transactionID;
	@Column
	private String clientID;
	@Column
	private String branchID;
	@Column
	private String staffID;
	@Column
	private String travelAgentID;
	@Column
	private String clientType;
	@Column
	private String clientCurrency;
	@Column
	private String clientLanguage;
	@Column
	private String clientMarket;
	@Column
	private String clientNationality;
	@Column
	private String company;
	@Column
	private String clientIATANumber;
	@Column
	private String status;
	@Column
	private String isHolidayBooking;
	@Column
	private String enquiryID;
	@Column
	private String quoteID;
	@Column
	private String rateOfExchange;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	
	/*@Column
	private String userID;*/

	
	//Fields added after ops team's suggestions

	
	
	public String getRateOfExchange() {
		return rateOfExchange;
	}
	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}
	public Set<ProductOrder> getProductOrders() {
		return productOrders;
	}
	public void setProductOrders(Set<ProductOrder> productOrders) {
		this.productOrders = productOrders;
	}
	public Set<PaymentInfo> getPaymentInfo() {
		return paymentInfo;
	}
	public void setPaymentInfo(Set<PaymentInfo> paymentInfo) {
		this.paymentInfo = paymentInfo;
	}
	public String getBookID() {
		return bookID;
	}
	public void setBookID(String bookID) {
		this.bookID = bookID;
	}
	public String getSessionID() {
		return sessionID;
	}
	
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getTransactionID() {
		return transactionID;
	}
	public void setTransactionID(String transactionID) {
		this.transactionID = transactionID;
	}
	public String getClientID() {
		return clientID;
	}
	
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getBranchID() {
		return branchID;
	}
	public void setBranchID(String branchID) {
		this.branchID = branchID;
	}
	public String getStaffID() {
		return staffID;
	}
	public void setStaffID(String staffID) {
		this.staffID = staffID;
	}
	public String getTravelAgentID() {
		return travelAgentID;
	}
	public void setTravelAgentID(String travelAgentID) {
		this.travelAgentID = travelAgentID;
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
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getIsHolidayBooking() {
		return isHolidayBooking;
	}
	public void setIsHolidayBooking(String isHolidayBooking) {
		this.isHolidayBooking = isHolidayBooking;
	}
	
	public String getLastModifiedBy() {
		return lastModifiedBy;
	}
	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	public String getEnquiryID() {
		return enquiryID;
	}
	public void setEnquiryID(String enquiryID) {
		this.enquiryID = enquiryID;
	}
	public String getQuoteID() {
		return quoteID;
	}
	public void setQuoteID(String quoteID) {
		this.quoteID = quoteID;
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
	
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getClientLanguage() {
		return clientLanguage;
	}
	public void setClientLanguage(String clientLanguage) {
		this.clientLanguage = clientLanguage;
	}
	public String getClientMarket() {
		return clientMarket;
	}
	public void setClientMarket(String clientMarket) {
		this.clientMarket = clientMarket;
	}
	public String getClientNationality() {
		return clientNationality;
	}
	public void setClientNationality(String clientNationality) {
		this.clientNationality = clientNationality;
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	@Override
	public String toString() {
		
	    JSONObject bookingJson = new JSONObject();
		
	    bookingJson.put("bookID", getBookID());
	    bookingJson.put("lastModifiedAt", lastModifiedAt);
		
		JSONObject testjson = new JSONObject();
		testjson.put("bookID", bookID);
		testjson.put("sessionID",sessionID);
		testjson.put("transactionID",transactionID);
		testjson.put("clientID",clientID);
		testjson.put("branchID",branchID);
		testjson.put("staffID",staffID);
		testjson.put("travelAgentID",travelAgentID);
		testjson.put("clientType",clientType);
		testjson.put("clientCurrency",clientCurrency);
		testjson.put("clientIATANumber",clientIATANumber);
		testjson.put("status",status);
		testjson.put("isHolidayBooking",isHolidayBooking);
		testjson.put("enquiryID",enquiryID);
		testjson.put("quoteID",quoteID);
		testjson.put("rateOfExchange",rateOfExchange);
		testjson.put("createdAt",createdAt);
		testjson.put("lastModifiedAt",lastModifiedAt);
		testjson.put("userID", userID);
		testjson.put("clientLanguage", clientLanguage);
		testjson.put("clientMarket", clientMarket);
		testjson.put("clientNationality",clientNationality);
		testjson.put("company", company);
		
		
		bookingJson.put("data_value", testjson);
		
		return bookingJson.toString();
		
	}
	
}

