package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="OFFER")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Offer implements Serializable
{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue
	private UUID offer_id;
	@OneToOne(cascade = CascadeType.ALL)
	private AccoOrders order_id;
	@Column
	private String offer_name;
	@Column
	private String offer_effective_from_date;
	@Column
	private String discount_percentage;
	@Column
	private String discount_amount;
	@Column
	private String remark;
	
	
	

	public String getOffer_name() {
		return offer_name;
	}
	public void setOffer_name(String offer_name) {
		this.offer_name = offer_name;
	}
	public String getOffer_effective_from_date() {
		return offer_effective_from_date;
	}
	public void setOffer_effective_from_date(String offer_effective_from_date) {
		this.offer_effective_from_date = offer_effective_from_date;
	}
	public String getDiscount_percentage() {
		return discount_percentage;
	}
	public void setDiscount_percentage(String discount_percentage) {
		this.discount_percentage = discount_percentage;
	}
	public String getDiscount_amount() {
		return discount_amount;
	}
	public void setDiscount_amount(String discount_amount) {
		this.discount_amount = discount_amount;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	@Override
	public String toString() {
		return "Offer [offer_name=" + offer_name + ", offer_effective_from_date=" + offer_effective_from_date
				+ ", discount_percentage=" + discount_percentage + ", discount_amount=" + discount_amount + ", remark="
				+ remark + "]";
	}

	public AccoOrders getOrder_id() {
		return order_id;
	}
	public void setOrder_id(AccoOrders order_id) {
		this.order_id = order_id;
	}
	public UUID getOffer_id() {
		return offer_id;
	}
	public void setOffer_id(UUID offer_id) {
		this.offer_id = offer_id;
	}
	
	
	

}
