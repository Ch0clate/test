package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;



@Entity
@Table(name=  "ACCOORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class AccoOrders extends ProductOrder implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AccoOrders() {
		super();
		operationType="update";
	}
	
	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="holidays_order_id", nullable=true)
    private HolidaysOrders holidaysOrders; 
	
	@OneToMany(mappedBy="accoOrders", cascade=CascadeType.ALL)
	private Set<AccoRoomDetails> roomDetails;
	

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String rateOfExchange;
	@Column
	private String supp_booking_reference;

	@Column
	private String amendDate;
	@Column
	private String cancelDate;
	@Column
	private String lastModifiedBy;
	@Column
	private String supplierID;
	
	
	//TODO: these are the new fields added after ops team's ssuggestion
	@Column
	private String ticketingPCC;
	@Column
	private String credentialsName;
	@Column
	private String accoRefNumber;
	@Column
	private String supplierRateType;
	@Column
	private String inventory;
	
	@Column
	private String supplierPrice;
	@Column
	private String supplierPriceCurrencyCode;
	@Column
	private String totalPrice;
	@Column
	private String totalPriceCurrencyCode;	
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceTaxes;
	@Column
	@Type(type = "StringJsonObject")
	private String suppPriceTaxes;
	
	@Transient
	private String operationType;
	
	
	
	public String getSupplierPrice() {
		return supplierPrice;
	}
	public void setSupplierPrice(String supplierPrice) {
		this.supplierPrice = supplierPrice;
	}
	public String getSupplierPriceCurrencyCode() {
		return supplierPriceCurrencyCode;
	}
	public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
		this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
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
	public String getOperationType() {
		return operationType;
	}
	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}
	
	public String getRateOfExchange() {
		return rateOfExchange;
	}
	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}
	
	public String getCredentialsName() {
		return credentialsName;
	}
	public void setCredentialsName(String credentialsName) {
		this.credentialsName = credentialsName;
	}

	
	public String getTotalPriceTaxes() {
		return totalPriceTaxes;
	}
	public void setTotalPriceTaxes(String totalPriceTaxes) {
		this.totalPriceTaxes = totalPriceTaxes;
	}
	public String getSuppPriceTaxes() {
		return suppPriceTaxes;
	}
	public void setSuppPriceTaxes(String suppPriceTaxes) {
		this.suppPriceTaxes = suppPriceTaxes;
	}
	public String getAccoRefNumber() {
		return accoRefNumber;
	}
	public void setAccoRefNumber(String accoRefNumber) {
		this.accoRefNumber = accoRefNumber;
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
	
	public String getSupplierID() {
		return supplierID;
	}
	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}
	public Set<AccoRoomDetails> getRoomDetails() {
		return roomDetails;
	}
	public void setRoomDetails(Set<AccoRoomDetails> roomDetails) {
		this.roomDetails = roomDetails;
	}
	
	public ZonedDateTime getLastModifiedAt() {
		return lastModifiedAt;
	}
	public void setLastModifiedAt(ZonedDateTime lastModifiedAt) {
		this.lastModifiedAt = lastModifiedAt;
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
	public String getLastModifiedBy() {
		return lastModifiedBy;
	}
	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	
	
	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	public String getSupp_booking_reference() {
		return supp_booking_reference;
	}
	public void setSupp_booking_reference(String supp_booking_reference) {
		this.supp_booking_reference = supp_booking_reference;
	}
	public String getTicketingPCC() {
		return ticketingPCC;
	}
	public void setTicketingPCC(String ticketingPCC) {
		this.ticketingPCC = ticketingPCC;
	}
  public static long getSerialversionuid() {
    return serialVersionUID;
  }
  public HolidaysOrders getHolidaysOrders() {
    return holidaysOrders;
  }
  public void setHolidaysOrders(HolidaysOrders holidaysOrders) {
    this.holidaysOrders = holidaysOrders;
  }
	
  
	
}
