package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name=  "CARAMCL")
public class CarAmCl extends AbstractAmCl implements Serializable {
	

	private static final long serialVersionUID = 1L;
	
	@Column
	private String totalPrice;
	@Column
	private String totalPriceCurrencyCode;
	
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
	public String getTotalBaseFare() {
		return totalBaseFare;
	}
	public void setTotalBaseFare(String totalBaseFare) {
		this.totalBaseFare = totalBaseFare;
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
	
}

