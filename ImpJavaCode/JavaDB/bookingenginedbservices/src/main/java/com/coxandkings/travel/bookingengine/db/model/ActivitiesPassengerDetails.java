package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;

/**
 * @author anuradha.kumari
 *
 */
@Entity
@Table(name = "ACTIVITYPASSENGERDETAILS")
@TypeDefs({ @TypeDef(name = "StringJsonObject", typeClass = StringJsonUserType.class) })
public class ActivitiesPassengerDetails implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column
	@GeneratedValue
	private UUID passanger_id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activities_order_id")
	private ActivitiesOrders activitiesOrders;

	@Column
	private String namePrefix;
	
	@Column
	private String givenName;
	
	@Column
	private String middleName;
	
	@Column
	private String surname;
	
	@Column
	private String nameTitle;
	
	@Column
	private String birthDate;
	
	
	@Column
	private String passengerType;
	
	@Column
	@Type(type = "StringJsonObject")
	private String contactDetails;

	public UUID getPassanger_id() {
		return passanger_id;
	}

	public void setPassanger_id(UUID passanger_id) {
		this.passanger_id = passanger_id;
	}

	public ActivitiesOrders getActivitiesOrders() {
		return activitiesOrders;
	}

	public void setActivitiesOrders(ActivitiesOrders activitiesOrders) {
		this.activitiesOrders = activitiesOrders;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getNameTitle() {
		return nameTitle;
	}

	public void setNameTitle(String nameTitle) {
		this.nameTitle = nameTitle;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public String getPassengerType() {
		return passengerType;
	}

	public void setPassengerType(String passengerType) {
		this.passengerType = passengerType;
	}

	public String getContactDetails() {
		return contactDetails;
	}

	public void setContactDetails(String contactDetails) {
		this.contactDetails = contactDetails;
	}
	
	
	
}
