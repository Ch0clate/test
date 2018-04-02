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
@Table(name ="SUPPLIERCOMMERCIAL" )
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class SupplierCommercial implements Serializable {
  
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column()
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	private String supp_commercial_id;
	
	
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
	public void setRecieptNumber(String recieptNumber) {
		this.recieptNumber = recieptNumber;
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
	public String getCommercialAmount() {
		return commercialAmount;
	}
	public void setCommercialAmount(String commercialAmount) {
		this.commercialAmount = commercialAmount;
	}
	public ProductOrder getOrder() {
		return order;
	}
	public void setOrder(ProductOrder order) {
		this.order = order;
	}
	public String getSupp_commercial_id() {
		return supp_commercial_id;
	}
	public void setSupp_commercial_id(String supp_commercial_id) {
		this.supp_commercial_id = supp_commercial_id;
	}
	@Override
	public String toString() {
	
		JSONObject suppCommJson = new JSONObject();
		suppCommJson.put("supp_commercial_id", supp_commercial_id);
		suppCommJson.put("bookID",order.getBooking().getBookID());
		suppCommJson.put("orderID",order.getId());
		suppCommJson.put("commercialName",commercialName);
		suppCommJson.put("commercialType",commercialType);
		suppCommJson.put("product",product);
		suppCommJson.put("inVoiceNumber",inVoiceNumber);
		suppCommJson.put("recieptNumber",recieptNumber);
		
		
		return suppCommJson.toString();
	
	
}
}
