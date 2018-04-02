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
import org.json.JSONObject;



@Entity
@Table(name="PAYMENTINFO")
public class PaymentInfo implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column()
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	private String payment_info_id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="booking_id")
    private Booking booking; 

	@Column
	private String paymentMethod;
	
	@Column
	private String paymentAmount;
	
	@Column
	private String paymentType;
	
	@Column
	private String amountCurrency;
	
	@Column
	private String cardType;
	
	@Column
	private String cardNumber;
	
	@Column
	private String cardExpiry;
	
	@Column
	private String encryptionKey;
	
	@Column
	private String token;
	
	@Column
	private String accountType;

	
	
	public Booking getBooking() {
		return booking;
	}

	public void setBooking(Booking booking) {
		this.booking = booking;
	}

	public String getPayment_info_id() {
		return payment_info_id;
	}

	public void setPayment_info_id(String payment_info_id) {
		this.payment_info_id = payment_info_id;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(String paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public String getAmountCurrency() {
		return amountCurrency;
	}

	public void setAmountCurrency(String amountCurrency) {
		this.amountCurrency = amountCurrency;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getCardExpiry() {
		return cardExpiry;
	}

	public void setCardExpiry(String cardExpiry) {
		this.cardExpiry = cardExpiry;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	@Override
	public String toString() {
   
		JSONObject paymentInfo = new JSONObject();
		paymentInfo.put("payment_info_id", payment_info_id);
		paymentInfo.put("bookID", booking.getBookID());
		paymentInfo.put("paymentMethod", paymentMethod);
		paymentInfo.put("paymentAmount", paymentAmount);
		paymentInfo.put("paymentType", paymentType);
		paymentInfo.put("amountCurrency", amountCurrency);
		paymentInfo.put("cardType", cardType);
		paymentInfo.put("cardNumber", cardNumber);
		paymentInfo.put("cardExpiry", cardExpiry);
		paymentInfo.put("encryptionKey", encryptionKey);
		paymentInfo.put("token", token);
		paymentInfo.put("accountType", accountType);
		
		return paymentInfo.toString();
	
	}
    
	
	
}
