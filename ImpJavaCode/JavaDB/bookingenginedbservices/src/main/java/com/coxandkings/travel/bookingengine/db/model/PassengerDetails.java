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
@Table(name = "PASSENGERDETAILS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class PassengerDetails implements Serializable {
	
	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		
		@Id
		@GeneratedValue(generator="system-uuid")
		@GenericGenerator(name="system-uuid",strategy = "uuid")
		private String passanger_id;
	
		@Column
		private String title;
		@Column
		private String firstName;
		@Column
		private String middleName;
		@Column
		private String lastName;
		@Column
		private String birthDate;
		@Column
		private Boolean isLeadPax;
		@Column
		private String PaxType;
		@Column
		private String status;
		@Column
		private String gender;
		@Column
		private String rph;
		@Column
		private String age;
		@Column
		private String quantity;
		@Column
		private String email;
		
		@Column
        private boolean isHolidayPassenger;
		
		@Column
		@Type(type = "StringJsonObject")
		private String personName;
		
		@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
		private ZonedDateTime createdAt;
		
		@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
		private ZonedDateTime lastModifiedAt;
		
		@Column
		private String lastModifiedBy;
		
		@Column
		@Type(type = "StringJsonObject")
		private String contactDetails;
		
		@Column
		@Type(type = "StringJsonObject")
		private String addressDetails;
		
		@Column
		@Type(type = "StringJsonObject")
		private String documentDetails;
		
		@Column
		@Type(type = "StringJsonObject")
		private String specialRequests;
		
		@Column
		@Type(type = "StringJsonObject")
		private String ancillaryServices;

	
		
		public void setIsLeadPax(Boolean isLeadPax) {
			this.isLeadPax = isLeadPax;
		}
		
		public boolean getIsLeadPax() {
			return isLeadPax;
		}
		public void setIsLeadPax(boolean isLeadPax) {
			this.isLeadPax = isLeadPax;
		}
		
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		
		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
		
		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public String getBirthDate() {
			return birthDate;
		}
		public void setBirthDate(String birthDate) {
			this.birthDate = birthDate;
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
		
		public String getPassanger_id() {
			return passanger_id;
		}

		public void setPassanger_id(String passanger_id) {
			this.passanger_id = passanger_id;
		}

		public String getPaxType() {
			return PaxType;
		}

		public void setPaxType(String paxType) {
			PaxType = paxType;
		}

		public String getContactDetails() {
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

		public String getDocumentDetails() {
			return documentDetails;
		}

		public void setDocumentDetails(String documentDetails) {
			this.documentDetails = documentDetails;
		}

		public String getSpecialRequests() {
			return specialRequests;
		}

		public void setSpecialRequests(String specialRequests) {
			this.specialRequests = specialRequests;
		}

		public String getAncillaryServices() {
			return ancillaryServices;
		}

		public void setAncillaryServices(String ancillaryServices) {
			this.ancillaryServices = ancillaryServices;
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
		
		

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
		
		public String getRph() {
			return rph;
		}
		public void setRph(String rph) {
			this.rph = rph;
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
		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
		public String getPersonName() {
			return personName;
		}

		public void setPersonName(String personName) {
			this.personName = personName;
		}
 
		@Override
		public String toString() {
			
			JSONObject accoJson = new JSONObject();
			
			//accoJson.put("bookID", roomDetails.getBooking().getBookID());
			accoJson.put("lastModifiedAt", lastModifiedAt);
			
			JSONObject testJson = new JSONObject();
			
			//testJson.put("bookID", roomDetails.getBooking().getBookID());
			testJson.put("id", passanger_id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			testJson.put("title", title);
			testJson.put("firstName", firstName);
			testJson.put("middleName", middleName);
			testJson.put("lastName", lastName);
			testJson.put("birthDate", birthDate);
			testJson.put("age", age);
			testJson.put("email", email);
			testJson.put("quantity", quantity);
			testJson.put("rph", rph);
			testJson.put("isLeadPax", isLeadPax);
			testJson.put("PaxType", PaxType);
			testJson.put("lastModifiedBy", lastModifiedBy);
			testJson.put("contactDetails", new JSONArray(contactDetails));
			testJson.put("addressDetails", new JSONObject(addressDetails));
			//testJson.put("documentDetails",new JSONObject( documentDetails));
			//TODO: Later confirm whether we are going to get special rwequests for ACCo
			//paxJson.put("specialRequests", new JSONObject(specialRequests));
			//testJson.put("ancillaryServices",new JSONObject( ancillaryServices));
			
			accoJson.put("data_value", testJson);
			return accoJson.toString();
		}

    public boolean getIsHolidayPassenger() {
      return isHolidayPassenger;
    }

    public void setIsHolidayPassenger(boolean isHolidayPassenger) {
      this.isHolidayPassenger = isHolidayPassenger;
    }

		
		
	
		
		

		
}
