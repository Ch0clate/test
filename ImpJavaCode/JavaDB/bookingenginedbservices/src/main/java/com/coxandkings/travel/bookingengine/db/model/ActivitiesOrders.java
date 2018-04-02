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
@Table(name = "ACTIVITIESORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class ActivitiesOrders extends ProductOrder  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@OneToMany(mappedBy="activitiesOrders", cascade=CascadeType.ALL)
	private Set<ActivitiesPassengerDetails> passengerDetails;
	
	
	@Column
	private String rateOfExchange;
	
	@Column
	private String bookingDateTime;
	
	// Combination of confirmation ID, Type and Instance
	@Column
	private String supp_booking_reference;
	
	
	
	@Column
	private String amendDate;
	
	@Column
	private String cancelDate;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastUpdatedBy;
	
	@Column
	private String supplierID;
	
	@Column
	private String supplierProductCode;
	
	@Column
	private String supplierBrandCode;
	
	@Column 
	private String name; //Activity Name
	
	
	@Column
	@Type(type = "StringJsonObject")
	private String supplier_Details;
	
	
	@Column
	@Type(type = "StringJsonObject")
	private String tourLanguage;
	
	@Column
	@Type(type = "StringJsonObject")
	private String answers;
	
	@Column
	@Type(type = "StringJsonObject")
	private String shipping_Details;
	
	@Column
	@Type(type = "StringJsonObject")
	private String POS;
	
	@Column
	@Type(type = "StringJsonObject")
	private String timeSlotDetails;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime startDate;

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime endDate;

	@Column
	@Type(type = "StringJsonObject")
	private String pickupDropoff;
	
	@Column
	private String countryCode;
	
	
	@Column
	private String cityCode;
	
	
	//Will contain Adult/Child or Activity pricing from Supplier
	@Column
	@Type(type = "StringJsonObject")
	private String SuppPaxTypeFares;
	
	//Will contain Adult/Child or Activity pricing after receiving commercials
	@Column
	@Type(type = "StringJsonObject")
	private String CommercialPaxTypeFares;
	
	
	// TODO : TotalPriceBaseFares is also needed. TotalPriceBaseFare is the supplier Price added with commercial price 
	//without the addition of taxes and fees. Currently we don't receive tax and fee, so no base fare is added separately 
	// in ActivitiesOrders
	@Column
	@Type(type = "StringJsonObject")
	private String totalPaxTypeFares;
	
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceReceivables;
	
	@Column
	@Type(type = "StringJsonObject")
	private String contactDetail;
	
	@Column
	private String adultCount;
	
	@Column
	private String childCount;
	
	
	@Column
	private String clientID;
	@Column
	private String clientType;
	@Column
	private String clientCurrency;
	@Column
	private String clientIATANumber;
	
	
	
	public String getTotalPaxTypeFares() {
		return totalPaxTypeFares;
	}
	public void setTotalPaxTypeFares(String totalPaxTypeFares) {
		this.totalPaxTypeFares = totalPaxTypeFares;
	}
	public String getTotalPriceReceivables() {
		return totalPriceReceivables;
	}
	public void setTotalPriceReceivables(String totalPriceReceivables) {
		this.totalPriceReceivables = totalPriceReceivables;
	}
	public Set<ActivitiesPassengerDetails> getPassengerDetails() {
		return passengerDetails;
	}
	public void setPassengerDetails(Set<ActivitiesPassengerDetails> passengerDetails) {
		this.passengerDetails = passengerDetails;
	}
	public String getRateOfExchange() {
		return rateOfExchange;
	}
	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}
	public String getBookingDateTime() {
		return bookingDateTime;
	}
	public void setBookingDateTime(String bookingDateTime) {
		this.bookingDateTime = bookingDateTime;
	}
	public String getSupp_booking_reference() {
		return supp_booking_reference;
	}
	public void setSupp_booking_reference(String supp_booking_reference) {
		this.supp_booking_reference = supp_booking_reference;
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
	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}
	public void setLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}
	public String getSupplierID() {
		return supplierID;
	}
	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}
	public String getSupplierProductCode() {
		return supplierProductCode;
	}
	public void setSupplierProductCode(String supplierProductCode) {
		this.supplierProductCode = supplierProductCode;
	}
	public String getSupplierBrandCode() {
		return supplierBrandCode;
	}
	public void setSupplierBrandCode(String supplierBrandCode) {
		this.supplierBrandCode = supplierBrandCode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSupplier_Details() {
		return supplier_Details;
	}
	public void setSupplier_Details(String supplier_Details) {
		this.supplier_Details = supplier_Details;
	}
	public String getTourLanguage() {
		return tourLanguage;
	}
	public void setTourLanguage(String tourLanguage) {
		this.tourLanguage = tourLanguage;
	}
	public String getAnswers() {
		return answers;
	}
	public void setAnswers(String answers) {
		this.answers = answers;
	}
	public String getShipping_Details() {
		return shipping_Details;
	}
	public void setShipping_Details(String shipping_Details) {
		this.shipping_Details = shipping_Details;
	}
	public String getPOS() {
		return POS;
	}
	public void setPOS(String pOS) {
		POS = pOS;
	}
	public String getTimeSlotDetails() {
		return timeSlotDetails;
	}
	public void setTimeSlotDetails(String timeSlotDetails) {
		this.timeSlotDetails = timeSlotDetails;
	}
	public ZonedDateTime getStartDate() {
		return startDate;
	}
	public void setStartDate(ZonedDateTime startDate) {
		this.startDate = startDate;
	}
	public ZonedDateTime getEndDate() {
		return endDate;
	}
	public void setEndDate(ZonedDateTime endDate) {
		this.endDate = endDate;
	}
	public String getPickupDropoff() {
		return pickupDropoff;
	}
	public void setPickupDropoff(String pickupDropoff) {
		this.pickupDropoff = pickupDropoff;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getCityCode() {
		return cityCode;
	}
	public void setCityCode(String cityCode) {
		this.cityCode = cityCode;
	}
	public String getSuppPaxTypeFares() {
		return SuppPaxTypeFares;
	}
	public void setSuppPaxTypeFares(String suppPaxTypeFares) {
		SuppPaxTypeFares = suppPaxTypeFares;
	}
	public String getCommercialPaxTypeFares() {
		return CommercialPaxTypeFares;
	}
	public void setCommercialPaxTypeFares(String commercialPaxTypeFares) {
		CommercialPaxTypeFares = commercialPaxTypeFares;
	}
	public String getContactDetail() {
		return contactDetail;
	}
	public void setContactDetail(String contactDetail) {
		this.contactDetail = contactDetail;
	}
	public String getAdultCount() {
		return adultCount;
	}
	public void setAdultCount(String adultCount) {
		this.adultCount = adultCount;
	}
	public String getChildCount() {
		return childCount;
	}
	public void setChildCount(String childCount) {
		this.childCount = childCount;
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
	
	
}
