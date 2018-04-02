package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

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
@Table(name = "TRANSFERSPASSENGERDETAILS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class TransfersPassengerDetails  implements Serializable{
	
	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

		@Id
		@Column()
		@GeneratedValue(generator="system-uuid")
		@GenericGenerator(name="system-uuid",strategy = "uuid")
		private String passanger_id;
	
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="transfers_order_id")
		private TransfersOrders transfersOrders; 
		
		@Column
		private String rph;
		
		@Column
		private String paxType;
		
		@Column
		private String age;
		
		@Column
		private String quantity;
		
		@Column
		private String phoneNumber;
		
		@Column
		private String email;
		
		@Column
		private Boolean isLeadPax;
		
		@Column
		private String status;
		
		@Column
		@Type(type = "StringJsonObject")
		private String personName;
		/*@Column
		private String title;
		@Column
		private String firstName;
		@Column
		private String middleName;
		@Column
		private String surname;
		@Column
		private String gender;
		@Column
		private String birthDate;
		@Column
		@Type(type = "StringJsonObject")
		private String contactDetails;
		
		@Column
		@Type(type = "StringJsonObject")
		private String addressDetails;*/
		
		@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
		private ZonedDateTime createdAt;
		
		@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
		private ZonedDateTime lastModifiedAt;
		
		@Column
		private String lastModifiedBy;

		public String getPassanger_id() {
			return passanger_id;
		}

		public void setPassanger_id(String passanger_id) {
			this.passanger_id = passanger_id;
		}

		public TransfersOrders getTransfersOrders() {
			return transfersOrders;
		}

		public void setTransfersOrders(TransfersOrders transfersOrders) {
			this.transfersOrders = transfersOrders;
		}

		public String getRph() {
			return rph;
		}

		public void setRph(String rph) {
			this.rph = rph;
		}

		public String getPaxType() {
			return paxType;
		}

		public void setPaxType(String paxType) {
			this.paxType = paxType;
		}

		public String getAge() {
			return age;
		}

		public void setAge(String age) {
			this.age = age;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public void setPhoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public Boolean getIsLeadPax() {
			return isLeadPax;
		}

		public void setIsLeadPax(Boolean isLeadPax) {
			this.isLeadPax = isLeadPax;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getPersonName() {
			return personName;
		}

		public void setPersonName(String personName) {
			this.personName = personName;
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

		public String getLastModifiedBy() {
			return lastModifiedBy;
		}

		public void setLastModifiedBy(String lastModifiedBy) {
			this.lastModifiedBy = lastModifiedBy;
		}

		public static long getSerialversionuid() {
			return serialVersionUID;
		}

		@Override
		public String toString() {
			

			JSONObject transfersJson = new JSONObject();
			
			transfersJson.put("bookID", transfersOrders.getBooking().getBookID());
			transfersJson.put("lastModifiedAt", lastModifiedAt);
			
			JSONObject testJson = new JSONObject();
			testJson.put("bookID", transfersOrders.getBooking().getBookID());
			testJson.put("transfersOrderID", transfersOrders.getId()); 
			testJson.put("id", passanger_id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			testJson.put("rph", rph);
			testJson.put("paxType", paxType);
			testJson.put("age", age);
			testJson.put("quantity", quantity);
			testJson.put("phoneNumber", phoneNumber);
			testJson.put("email", email);
			testJson.put("personName", new JSONObject(personName));
			
			transfersJson.put("data_value", testJson);
			return transfersJson.toString();
		}

		/*
		@Override
		public String toString() {
			
			JSONObject transfersJson = new JSONObject();
			
			transfersJson.put("bookID", transfersOrders.getBooking().getBookID());
			transfersJson.put("lastModifiedAt", lastModifiedAt);
			
			JSONObject testJson = new JSONObject();

			testJson.put("bookID", transfersOrders.getBooking().getBookID());
			testJson.put("accoOrderID", transfersOrders.getId());
			testJson.put("id", passanger_id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			testJson.put("title", title);
			testJson.put("firstName", firstName);
			testJson.put("middleName", middleName);
			testJson.put("surname", surname);
			testJson.put("birthDate", birthDate);
			testJson.put("gender", gender);
			testJson.put("status", status);
			testJson.put("lastModifiedBy", lastModifiedBy);
			testJson.put("contactDetails", new JSONArray(contactDetails));
			testJson.put("addressDetails", new JSONObject(addressDetails));
			
			transfersJson.put("data_value", testJson);

			return transfersJson.toString();
		}*/


		
		

		/*public String getContactDetails() {
			return contactDetails;
		}
		public void setContactDetails(String contactDetails) {
			this.contactDetails = contactDetails;
		}
		public String getAddressDetails() {
			return addressDetails;
		}
		public void setAddressDetails(String addressDetails) {
			this.addressDetails = addressDetails;
		}
		public String getFirstName() {
			return firstName;
		}
		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
		
		public String getMiddleName() {
			return middleName;
		}
		public void setMiddleName(String middleName) {
			this.middleName = middleName;
		}
		public String getGender() {
			return gender;
		}
		public void setGender(String gender) {
			this.gender = gender;
		}

		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		
		public String getSurname() {
			return surname;
		}
		public void setSurname(String surname) {
			this.surname = surname;
		}
		
		public String getBirthDate() {
			return birthDate;
		}
		public void setBirthDate(String birthDate) {
			this.birthDate = birthDate;
		}*/
		/*public TransfersOrders getTransfersOrders() {
			return transfersOrders;
		}
		public void setTransfersOrders(TransfersOrders transfersOrders) {
			this.transfersOrders = transfersOrders;
		}
		
		public String getPassanger_id() {
			return passanger_id;
		}
		public void setPassanger_id(String passanger_id) {
			this.passanger_id = passanger_id;
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
		public String getLastModifiedBy() {
			return lastModifiedBy;
		}
		public void setLastModifiedBy(String lastModifiedBy) {
			this.lastModifiedBy = lastModifiedBy;
		}
		public Boolean getIsLeadPax() {
			return isLeadPax;
		}
		public void setIsLeadPax(Boolean isLeadPax) {
			this.isLeadPax = isLeadPax;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		*/
		/*@Override
		public String toString() {
			
			JSONObject transfersJson = new JSONObject();
			
			transfersJson.put("bookID", transfersOrders.getBooking().getBookID());
			transfersJson.put("lastModifiedAt", lastModifiedAt);
			
			JSONObject testJson = new JSONObject();

			testJson.put("bookID", transfersOrders.getBooking().getBookID());
			testJson.put("accoOrderID", transfersOrders.getId());
			testJson.put("id", passanger_id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			testJson.put("title", title);
			testJson.put("firstName", firstName);
			testJson.put("middleName", middleName);
			testJson.put("surname", surname);
			testJson.put("birthDate", birthDate);
			testJson.put("gender", gender);
			testJson.put("status", status);
			testJson.put("lastModifiedBy", lastModifiedBy);
			testJson.put("contactDetails", new JSONArray(contactDetails));
			testJson.put("addressDetails", new JSONObject(addressDetails));
			
			transfersJson.put("data_value", testJson);

			return transfersJson.toString();
		}*/
		
		
		

}
