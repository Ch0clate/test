package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;

@Entity
@Table(name=  "BUSAMCL")

public class BusAmCl extends AbstractAmCl implements Serializable {

	private static final long serialVersionUID = 1L;
	
//	@Id
//	@GeneratedValue(generator="system-uuid")
//	@GenericGenerator(name="system-uuid",strategy = "uuid")
//	private String id;
//	
//	
////	@OneToOne(fetch = FetchType.LAZY)
////	@JoinColumn(name="bus_order_id",insertable = false, updatable = false)
////	private BusOrders busOrders;
//	
////	@OneToMany(fetch = FetchType.LAZY,mappedBy = "BusAmCl")
////	@JoinColumn(name="paxId",insertable = false, updatable = false)
////	@JoinTable(name="BusPassengerDetails" )
////	private BusPassengerDetails paxdetails;
//	
////	@OneToMany(cascade=CascadeType.ALL, targetEntity=BusPassengerDetails.class)
////	@JoinColumn(name="paxId")
////	private Set<BusPassengerDetails> coachGroups = new HashSet<>();
//	
//	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
//	private ZonedDateTime createdAt;
//	
//	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
//	private ZonedDateTime lastModifiedAt;
//	
//	@Column
//	private String lastModifiedBy;
//	
//	@Column
//	private String entityName;
//	@Column
//	private String bus_order_id;
//	@Column
//	private String paxId;
//	@Column
//	private String requestType;
//	@Column
//	private String status;
//	
//	
//	@Column
//	private String cancelType;
//	@Column
//	private String bookId;
//	@Column
//	private String refundAmount;
//	@Column
//	private String refundAmountCurrency;
//	
//	public String getRefundAmount() {
//		return refundAmount;
//	}
//	public void setRefundAmount(String refundAmount) {
//		this.refundAmount = refundAmount;
//	}
//	public String getRefundAmountCurrency() {
//		return refundAmountCurrency;
//	}
//	public void setRefundAmountCurrency(String refundAmountCurrency) {
//		this.refundAmountCurrency = refundAmountCurrency;
//	}
//	public String getCancelType() {
//		return cancelType;
//	}
//	public void setCancelType(String cancelType) {
//		this.cancelType = cancelType;
//	}
//	public String getBookId() {
//		return bookId;
//	}
//	public void setBookId(String bookId) {
//		this.bookId = bookId;
//	}
//	public String getId() {
//		return id;
//	}
//	public void setId(String id) {
//		this.id = id;
//	}
//	public String getLastModifiedBy() {
//		return lastModifiedBy;
//	}
//	public void setLastModifiedBy(String lastModifiedBy) {
//		this.lastModifiedBy = lastModifiedBy;
//	}
//	public ZonedDateTime getCreatedAt() {
//		return createdAt;
//	}
//	public void setCreatedAt(ZonedDateTime createdAt) {
//		this.createdAt = createdAt;
//	}
//	public ZonedDateTime getLastModifiedAt() {
//		return lastModifiedAt;
//	}
//	public void setLastModifiedAt(ZonedDateTime lastModifiedAt) {
//		this.lastModifiedAt = lastModifiedAt;
//	}
//	public String getEntityName() {
//		return entityName;
//	}
//	public void setEntityName(String entityName) {
//		this.entityName = entityName;
//	}
////	public String getOrderId() {
////		return bus_order_id;
////	}
////	public void setOrderId(String orderId) {
////		this.bus_order_id = orderId;
////	}
//	public String getPaxId() {
//		return paxId;
//	}
//	public String getBus_order_id() {
//		return bus_order_id;
//	}
//	public void setBus_order_id(String bus_order_id) {
//		this.bus_order_id = bus_order_id;
//	}
//	public void setPaxId(String paxId) {
//		this.paxId = paxId;
//	}
//	public String getRequestType() {
//		return requestType;
//	}
//	public void setRequestType(String requestType) {
//		this.requestType = requestType;
//	}
//	public String getStatus() {
//		return status;
//	}
//	public void setStatus(String status) {
//		this.status = status;
//	}
//	
////	public BusOrders getBusOrders() {
////		return busOrders;
////	}
////	public void setBusOrders(BusOrders busOrders) {
////		this.busOrders = busOrders;
////	}
////	public BusPassengerDetails getPaxdetails() {
////		return paxdetails;
////	}
////	public void setPaxdetails(BusPassengerDetails paxdetails) {
////		this.paxdetails = paxdetails;
////	}
	
	
	
	
	
}
