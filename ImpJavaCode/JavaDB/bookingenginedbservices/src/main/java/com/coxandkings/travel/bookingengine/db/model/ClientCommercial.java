package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;

@Entity
@Table(name ="CLIENTCOMMERCIAL")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class ClientCommercial implements Serializable {
 
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column()
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	private String client_commercial_id;
	
	@ManyToOne(fetch = FetchType.LAZY, targetEntity= ProductOrder.class)
	@JoinColumn(name="product_reference_id")
	private ProductOrder order;

	@Column
	private String commercialName;
	@Column
	private String commercialType;
	@Column
	private String commercialCurrency;
	@Column
	private String commercialAmount;
	@Column
	private String clientID;
	@Column
	private String parentClientID;
	@Column
	private String commercialEntityType;
	@Column
	private String commercialEntityID;
	@Column
	private boolean companyFlag;
	
	@Column
	private String product;
	@Column
	private String inVoiceNumber;
	@Column
	private String recieptNumber;
	
	
	public String getInVoiceNumber() {
		return inVoiceNumber;
	}
	public void setInVoiceNumber(String inVoiceNumber) {
		this.inVoiceNumber = inVoiceNumber;
	}
	public String getRecieptNumber() {
		return recieptNumber;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	
	public String getCommercialName() {
		return commercialName;
	}
	public void setCommercialName(String commercialName) {
		this.commercialName = commercialName;
	}
	public String getCommercialType() {
		return commercialType;
	}
	public void setCommercialType(String commercialType) {
		this.commercialType = commercialType;
	}
	
	public String getCommercialCurrency() {
		return commercialCurrency;
	}
	public void setCommercialCurrency(String commercialCurrency) {
		this.commercialCurrency = commercialCurrency;
	}
	public void setRecieptNumber(String recieptNumber) {
		this.recieptNumber = recieptNumber;
	}
	public ProductOrder getOrder() {
		return order;
	}
	public void setOrder(ProductOrder order) {
		this.order = order;
	}
	public String getClient_commercial_id() {
		return client_commercial_id;
	}
	public void setClient_commercial_id(String client_commercial_id) {
		this.client_commercial_id = client_commercial_id;
	}
	public String getCommercialAmount() {
		return commercialAmount;
	}
	public void setCommercialAmount(String commercialAmount) {
		this.commercialAmount = commercialAmount;
	}
	public String getClientID() {
		return clientID;
	}
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getCommercialEntityType() {
		return commercialEntityType;
	}
	public void setCommercialEntityType(String commercialEntityType) {
		this.commercialEntityType = commercialEntityType;
	}
	public String getCommercialEntityID() {
		return commercialEntityID;
	}
	public void setCommercialEntityID(String commercialEntityID) {
		this.commercialEntityID = commercialEntityID;
	}

	public String getParentClientID() {
		return parentClientID;
	}
	public void setParentClientID(String parentClientID) {
		this.parentClientID = parentClientID;
	}
	public boolean isCompanyFlag() {
		return companyFlag;
	}
	public void setCompanyFlag(boolean companyFlag) {
		this.companyFlag = companyFlag;
	}
	
	
	@Override
	public String toString() {
		JSONObject clientCommJson = new JSONObject();
		clientCommJson.put("client_commercial_id", client_commercial_id);
		clientCommJson.put("bookID",order.getBooking().getBookID());
		clientCommJson.put("orderID",order.getId());
		clientCommJson.put("commercialName",commercialName);
		clientCommJson.put("commercialType",commercialType);
		clientCommJson.put("product",product);
		clientCommJson.put("inVoiceNumber",inVoiceNumber);
		clientCommJson.put("recieptNumber",recieptNumber);
		
		
		return clientCommJson.toString();
		
	}
	
}
