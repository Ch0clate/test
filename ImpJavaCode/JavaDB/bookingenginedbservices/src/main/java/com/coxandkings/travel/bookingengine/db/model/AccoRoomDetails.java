package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;





@Entity
@Table(name ="ACCOROOMDETAILS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class AccoRoomDetails  implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@Column()
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	protected String id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="acco_order_id")
    private AccoOrders accoOrders;	
	
	@Column
	private String checkInDate;
	@Column
	private String checkOutDate;
	@Column
	private String roomTypeCode;
	@Column
	private String roomCategoryName;
	@Column
	private String roomRef;
	@Column
	private String roomTypeName;
	@Column
	private String roomCategoryID;
	@Column
	private String hotelCode;
	@Column
	private String hotelName;
	@Column
	private String ratePlanName;
	@Column
	private String ratePlanCode;
	@Column
	private String ratePlanRef;
	@Column
	private String bookingRef;
	@Column
	private String supplierPrice;
	
	@Column
	@Type(type = "StringJsonObject")
	private String supplierTaxBreakup;
	@Column
	private String supplierPriceCurrencyCode;
	@Column
	private String totalPrice;
	@Column
	@Type(type = "StringJsonObject")
	private String totalTaxBreakup;
	@Column
	private String totalPriceCurrencyCode;
	@Column
	private String countryCode;
	@Column
	private String cityCode;
	@Column
	private String supplierName;
	@Column
	private String status;
	@Column
	private String mealName;
	@Column
	private String  mealCode;

	@Column
	@Type(type = "StringJsonObject")
	private String suppCommercials;
	@Column
	@Type(type = "StringJsonObject")
	private String clientCommercials;
	
	@Column
	@Type(type = "StringJsonObject")
	private String paxDetails;
	
	//Extra fields for paackages
	@Column
    private String accomodationType;//Hotel or Cruise
	@Column
    private String supplierTaxAmount;
	@Column
    private String totalTaxAmount;
	//roomInfo
    @Column
    private String availabilityStatus;
    //hotelInfo
    @Column
    @Type(type = "StringJsonObject")
    private String hotelInfo;
    //roomTypeInfo
    @Column
    private String roomType;
    @Column
    private String roomCategory;
    @Column
    private String roomName;
    @Column
    private String cabinNumber;
    @Column
    private String InvBlockCode;
    //address
    @Column
    @Type(type = "StringJsonObject")
    private String address;
    //timeSpan
    @Column
    private String start;
    @Column(name="\"end\"")
    private String end;
    @Column
    private String duration;
	

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	
	
	public String getMealName() {
		return mealName;
	}
	public void setMealName(String mealName) {
		this.mealName = mealName;
	}
	public String getMealCode() {
		return mealCode;
	}
	public void setMealCode(String mealID) {
		this.mealCode = mealID;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCheckInDate() {
		return checkInDate;
	}
	public void setCheckInDate(String checkInDate) {
		this.checkInDate = checkInDate;
	}
	public String getCheckOutDate() {
		return checkOutDate;
	}
	public void setCheckOutDate(String checkOutDate) {
		this.checkOutDate = checkOutDate;
	}
	public String getRatePlanName() {
		return ratePlanName;
	}
	public void setRatePlanName(String ratePlanName) {
		this.ratePlanName = ratePlanName;
	}
	public String getRatePlanCode() {
		return ratePlanCode;
	}
	public void setRatePlanCode(String ratePlanCode) {
		this.ratePlanCode = ratePlanCode;
	}
	public String getRatePlanRef() {
		return ratePlanRef;
	}
	public void setRatePlanRef(String ratePlanRef) {
		this.ratePlanRef = ratePlanRef;
	}
	public String getBookingRef() {
		return bookingRef;
	}
	public void setBookingRef(String bookingRef) {
		this.bookingRef = bookingRef;
	}
	public String getHotelName() {
		return hotelName;
	}
	public void setHotelName(String hotelName) {
		this.hotelName = hotelName;
	}
	

	public String getHotelCode() {
		return hotelCode;
	}
	public void setHotelCode(String hotelCode) {
		this.hotelCode = hotelCode;
	}
	public String getRoomTypeCode() {
		return roomTypeCode;
	}
	public void setRoomTypeCode(String roomTypeCode) {
		this.roomTypeCode = roomTypeCode;
	}
	public String getRoomCategoryName() {
		return roomCategoryName;
	}
	public void setRoomCategoryName(String roomCategoryName) {
		this.roomCategoryName = roomCategoryName;
	}
	public String getRoomRef() {
		return roomRef;
	}
	public void setRoomRef(String roomId) {
		this.roomRef = roomId;
	}
	public String getRoomTypeName() {
		return roomTypeName;
	}
	public void setRoomTypeName(String roomTypeName) {
		this.roomTypeName = roomTypeName;
	}
	public String getRoomCategoryID() {
		return roomCategoryID;
	}
	public void setRoomCategoryID(String roomCategoryID) {
		this.roomCategoryID = roomCategoryID;
	}
	public String getSupplierPrice() {
		return supplierPrice;
	}
	public void setSupplierPrice(String supplierPrice) {
		this.supplierPrice = supplierPrice;
	}
	public String getSupplierTaxBreakup() {
		return supplierTaxBreakup;
	}
	public void setSupplierTaxBreakup(String supplierTaxBreakup) {
		this.supplierTaxBreakup = supplierTaxBreakup;
	}
	public String getSupplierPriceCurrencyCode() {
		return supplierPriceCurrencyCode;
	}
	public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
		this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
	}
	
	public String getTotalPrice() {
		return totalPrice;
	}
	public void setTotalPrice(String totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getTotalTaxBreakup() {
		return totalTaxBreakup;
	}
	public void setTotalTaxBreakup(String totalTaxBreakup) {
		this.totalTaxBreakup = totalTaxBreakup;
	}
	public String getTotalPriceCurrencyCode() {
		return totalPriceCurrencyCode;
	}
	public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
		this.totalPriceCurrencyCode = totalPriceCurrencyCode;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getCityCode() {
		return cityCode;
	}
	public void setCityCode(String cityCode) {
		this.cityCode = cityCode;
	}
	public String getSupplierName() {
		return supplierName;
	}
	public void setSupplierName(String supplierName) {
		this.supplierName = supplierName;
	}
	public AccoOrders getAccoOrders() {
		return accoOrders;
	}
	public void setAccoOrders(AccoOrders accoOrders) {
		this.accoOrders = accoOrders;
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
	
	public String getSuppCommercials() {
		return suppCommercials;
	}
	public void setSuppCommercials(String suppCommercials) {
		this.suppCommercials = suppCommercials;
	}
	public String getClientCommercials() {
		return clientCommercials;
	}
	public void setClientCommercials(String clientCommercials) {
		this.clientCommercials = clientCommercials;
	}
	
	public String getPaxDetails() {
		return paxDetails;
	}
	public void setPaxDetails(String paxDetails) {
		this.paxDetails = paxDetails;
	}
	@Override
	public String toString() {
		
		JSONObject accoJson = new JSONObject();
		
		//accoJson.put("bookID", booking.getBookID());
		accoJson.put("lastModifiedAt", lastModifiedAt);
		JSONObject testJson = new JSONObject();
		
		//testJson.put("bookID", booking.getBookID());
		testJson.put("accoOrderID",accoOrders.getId() );
		testJson.put("id", id);
		testJson.put("createdAt", createdAt);
		testJson.put("lastModifiedAt", lastModifiedAt);
		testJson.put("checkInDate", checkInDate);
		testJson.put("checkOutDate", checkOutDate);
		testJson.put("roomTypeCode", roomTypeCode);
		testJson.put("roomCategoryName", roomCategoryName);
		testJson.put("roomRef", roomRef);
		testJson.put("roomTypeName", roomTypeName);
		testJson.put("roomCategoryID", roomCategoryID);
		testJson.put("hotelCode", hotelCode);
		testJson.put("hotelName", hotelName);
		testJson.put("ratePlanName", ratePlanName);
		
		testJson.put("ratePlanCode", ratePlanCode);
		testJson.put("ratePlanRef", ratePlanRef);
		testJson.put("bookingRef", bookingRef);
		testJson.put("supplierPrice", supplierPrice);
		testJson.put("supplierTaxBreakup", supplierTaxBreakup);
		testJson.put("supplierPriceCurrencyCode", supplierPriceCurrencyCode);
		
		testJson.put("totalPrice", totalPrice);
		testJson.put("totalTaxBreakup",  new JSONArray(totalTaxBreakup));
		testJson.put("totalPriceCurrencyCode", totalPriceCurrencyCode);
		testJson.put("countryCode", countryCode);
		testJson.put("cityCode", cityCode);
		testJson.put("supplierName", supplierName);
		testJson.put("status", status);
		testJson.put("mealName", mealName);
		testJson.put("mealCode", mealCode);

		testJson.put("createdAt", createdAt);
		testJson.put("lastModifiedAt", lastModifiedAt);
		testJson.put("lastModifiedBy", lastModifiedBy);
		testJson.put("supplierTaxBreakup", new JSONArray(supplierTaxBreakup));
		testJson.put("supplierPriceCurrencyCode", supplierPriceCurrencyCode);
		
		accoJson.put("data_value", testJson);
		return accoJson.toString();
		
	}
  public String getAccomodationType() {
    return accomodationType;
  }
  public void setAccomodationType(String accomodationType) {
    this.accomodationType = accomodationType;
  }
  public String getSupplierTaxAmount() {
    return supplierTaxAmount;
  }
  public void setSupplierTaxAmount(String supplierTaxAmount) {
    this.supplierTaxAmount = supplierTaxAmount;
  }
  public String getTotalTaxAmount() {
    return totalTaxAmount;
  }
  public void setTotalTaxAmount(String totalTaxAmount) {
    this.totalTaxAmount = totalTaxAmount;
  }
  public String getAvailabilityStatus() {
    return availabilityStatus;
  }
  public void setAvailabilityStatus(String availabilityStatus) {
    this.availabilityStatus = availabilityStatus;
  }
  public String getHotelInfo() {
    return hotelInfo;
  }
  public void setHotelInfo(String hotelInfo) {
    this.hotelInfo = hotelInfo;
  }
  public String getRoomType() {
    return roomType;
  }
  public void setRoomType(String roomType) {
    this.roomType = roomType;
  }
  public String getRoomCategory() {
    return roomCategory;
  }
  public void setRoomCategory(String roomCategory) {
    this.roomCategory = roomCategory;
  }
  public String getRoomName() {
    return roomName;
  }
  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }
  public String getCabinNumber() {
    return cabinNumber;
  }
  public void setCabinNumber(String cabinNumber) {
    this.cabinNumber = cabinNumber;
  }
  public String getInvBlockCode() {
    return InvBlockCode;
  }
  public void setInvBlockCode(String invBlockCode) {
    InvBlockCode = invBlockCode;
  }
  public String getStart() {
    return start;
  }
  public void setStart(String start) {
    this.start = start;
  }
  public String getEnd() {
    return end;
  }
  public void setEnd(String end) {
    this.end = end;
  }
  public String getDuration() {
    return duration;
  }
  public void setDuration(String duration) {
    this.duration = duration;
  }
  public static long getSerialversionuid() {
    return serialVersionUID;
  }
  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }
	
	
	
}
