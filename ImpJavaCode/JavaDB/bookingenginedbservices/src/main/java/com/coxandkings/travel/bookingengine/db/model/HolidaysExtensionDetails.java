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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;



@Entity
@Table(name ="HOLIDAYSEXTENSIONDETAILS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class HolidaysExtensionDetails implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    @Id
    @Column()
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid",strategy = "uuid")
    protected String id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="holidays_order_id", nullable=true)
    private HolidaysOrders holidaysOrders;
	
	@Column
    @Type(type = "StringJsonObject")
    private String paxDetails;
	
	@Column
    private String configType;
	
	@Column
	private String extensionType;//pre-night or post-night
	
	@Column
    private String status;
	
	//suppPriceInfo
	@Column
	private String supplierPrice;
	@Column
	private String supplierTaxAmount;
	@Column
	@Type(type = "StringJsonObject")
	private String supplierTaxBreakup;
	@Column
	private String supplierPriceCurrencyCode;
	
	//totalPriceInfo
	@Column
	private String totalPrice;
	@Column
	private String totalTaxAmount;
	@Column
	@Type(type = "StringJsonObject")
	private String totalTaxBreakup;
	@Column
	private String totalPriceCurrencyCode;
	
	//Adding Supplier and Client commercials
	@Column
    @Type(type = "StringJsonObject")
    private String suppCommercials;
    @Column
    @Type(type = "StringJsonObject")
    private String clientCommercials;
	
	//roomInfo
	@Column
	private String availabilityStatus;
	
	//hotelInfo
	@Column
	private String hotelCode;
	@Column
	private String hotelName;
	@Column
	private String hotelRef;
	@Column
	private String HotelSegmentCategoryCode;
	
	//address
	@Column
	@Type(type = "StringJsonObject")
	private String address;
	
	//roomTypeInfo
	@Column
	private String roomType;
	@Column
	private String roomCategory;
	@Column
	private String roomName;
	@Column
	private String InvBlockCode;
	
	//ratePlanInfo
	@Column
	private String ratePlanName;
	@Column
	private String ratePlanCode;
	@Column
	private String bookingRef;
	//timeSpan
	@Column
	private String start;
	@Column(name="\"end\"")
	private String end;
	@Column
	private String duration;
	
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	@Column
	private String lastModifiedBy;
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public HolidaysOrders getHolidaysOrders() {
    return holidaysOrders;
  }
  public void setHolidaysOrders(HolidaysOrders holidaysOrders) {
    this.holidaysOrders = holidaysOrders;
  }
  public String getConfigType() {
    return configType;
  }
  public void setConfigType(String configType) {
    this.configType = configType;
  }
  public String getExtensionType() {
    return extensionType;
  }
  public void setExtensionType(String extensionType) {
    extensionType = extensionType;
  }
  public String getSupplierPrice() {
    return supplierPrice;
  }
  public void setSupplierPrice(String supplierPrice) {
    this.supplierPrice = supplierPrice;
  }
  public String getSupplierTaxAmount() {
    return supplierTaxAmount;
  }
  public void setSupplierTaxAmount(String supplierTaxAmount) {
    this.supplierTaxAmount = supplierTaxAmount;
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
  public String getTotalTaxAmount() {
    return totalTaxAmount;
  }
  public void setTotalTaxAmount(String totalTaxAmount) {
    this.totalTaxAmount = totalTaxAmount;
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
  public String getAvailabilityStatus() {
    return availabilityStatus;
  }
  public void setAvailabilityStatus(String availabilityStatus) {
    this.availabilityStatus = availabilityStatus;
  }
  public String getHotelCode() {
    return hotelCode;
  }
  public void setHotelCode(String hotelCode) {
    this.hotelCode = hotelCode;
  }
  public String getHotelName() {
    return hotelName;
  }
  public void setHotelName(String hotelName) {
    this.hotelName = hotelName;
  }
  public String getHotelRef() {
    return hotelRef;
  }
  public void setHotelRef(String hotelRef) {
    this.hotelRef = hotelRef;
  }
  public String getHotelSegmentCategoryCode() {
    return HotelSegmentCategoryCode;
  }
  public void setHotelSegmentCategoryCode(String hotelSegmentCategoryCode) {
    HotelSegmentCategoryCode = hotelSegmentCategoryCode;
  }
  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
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
  public String getInvBlockCode() {
    return InvBlockCode;
  }
  public void setInvBlockCode(String invBlockCode) {
    InvBlockCode = invBlockCode;
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
  public String getBookingRef() {
    return bookingRef;
  }
  public void setBookingRef(String bookingRef) {
    this.bookingRef = bookingRef;
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
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public String getPaxDetails() {
    return paxDetails;
  }
  public void setPaxDetails(String paxDetails) {
    this.paxDetails = paxDetails;
  }
 
  
	
 
}
