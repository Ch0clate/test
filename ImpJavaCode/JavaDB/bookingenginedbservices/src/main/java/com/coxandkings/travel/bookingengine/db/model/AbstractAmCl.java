package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractAmCl implements Serializable {
	

	

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	private String id;
	
	@Column
	private String entityName;
	@Column
	private String entityID;
	@Column
	private String orderID;
	public String getOrderID() {
		return orderID;
	}
	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}
	@Column
	private String requestType;
	@Column
	private String companyCharges;
	@Column
	private String supplierCharges;
	@Column
	private String description;
	@Column
	private String supplierChargesCurrencyCode;
	@Column
	private String companyChargesCurrencyCode;
	@Column
	private String status;
	
	@Column
	private String lastModifiedBy;
	

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	//TODO: we need to have breakups json stored for prices.
	

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	public String getEntityID() {
		return entityID;
	}
	public void setEntityID(String entityID) {
		this.entityID = entityID;
	}
	public String getRequestType() {
		return requestType;
	}
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}
	public String getCompanyCharges() {
		return companyCharges;
	}
	public void setCompanyCharges(String companyCharges) {
		this.companyCharges = companyCharges;
	}
	public String getSupplierCharges() {
		return supplierCharges;
	}
	public void setSupplierCharges(String supplierCharges) {
		this.supplierCharges = supplierCharges;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getSupplierChargesCurrencyCode() {
		return supplierChargesCurrencyCode;
	}
	public void setSupplierChargesCurrencyCode(String supplierChargesCurrencyCode) {
		this.supplierChargesCurrencyCode = supplierChargesCurrencyCode;
	}
	public String getCompanyChargesCurrencyCode() {
		return companyChargesCurrencyCode;
	}
	public void setCompanyChargesCurrencyCode(String companyChargesCurrencyCode) {
		this.companyChargesCurrencyCode = companyChargesCurrencyCode;
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
	public ZonedDateTime getLastModifiedAt() {
		return lastModifiedAt;
	}
	public void setLastModifiedAt(ZonedDateTime lastModifiedAt) {
		this.lastModifiedAt = lastModifiedAt;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

}
