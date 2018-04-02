package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;

@Entity
@Table(name = "TRANSFERSORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class TransfersOrders extends ProductOrder  implements Serializable {

	/**
	 * 
	 */
	public TransfersOrders() {
		super();
		operationType="update";
	}
	
	
	@OneToMany(mappedBy="transfersOrders", cascade=CascadeType.ALL)
	private Set<TransfersPassengerDetails> passengerDetails;

	private static final long serialVersionUID = 1L;

	
	//In case of Indian Suppliers
	@Column
	private String tripType;
	
	@Column
	private String tripIndicator;
	
	@Column
	private String bookingDateTime;
	@Column
	private String status;
	@Column
	private String amendDate;
	@Column
	private String cancelDate;

	@Column
	private String suppBookRef;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	@Column
	private String supplierID;
	@Column
	private String clientID;
	@Column
	private String clientType;
	@Column
	private String clientCurrency;
	@Column
	private String clientIATANumber;
	@Column
	private String supplierTotalPrice;
	@Column
	private String totalPrice;
	@Column
	private String totalPriceCurrencyCode;
	@Column
	private String supplierPriceCurrencyCode;
	@Column
	private String rateOfExchange;
	
	@Column
	@Type(type = "StringJsonObject")
	private String paxDetails;
	
	@Column
	@Type(type = "StringJsonObject")
	private String suppFares;
	
	@Column
	@Type(type = "StringJsonObject")
	private String totalFares;
	
	@Column
    @Type(type = "StringJsonObject")
    private String suppCommercials;
	
    @Column
    @Type(type = "StringJsonObject")
    private String clientCommercials;


	//TODO: these are new fields  added as per ops requirements: confirm from where we are going to get these fields.

	@Column
	private String credentialsName;
	@Column
	private String supplierRateType;

	@Transient
	private String operationType;

	@Column
	@Type(type = "StringJsonObject")
	private String transfersDetails;
	
	//Field for packages
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="holidays_order_id", nullable=true)
    private HolidaysOrders holidaysOrders;
    @Column
    private String configType;
    @Column
    private String transferType;
    //transfersInfo
    @Column
    private String availabilityStatus;
    //Location
    @Column
    private String pickUpLocation;
    @Column
    private String airportName;
    
    //transferDetails
    @Column
    private String transferName;
    @Column
    private String transferDescription;
    @Column
    private String departureCity;
    @Column
    private String arrivalCity;
    @Column
    private String departureDate;
    @Column
    private String arrivalDate;
    
    //timeSpan
    @Column
    private String start;
    @Column(name="\"end\"")
    private String end;
    @Column
    private String duration;

	//TODO : need to check if tax breakup comes at total level for AIR
	/*@Column
	private String totalTaxAmount;
	@Column
	private String totalTaxBreakup;
	@Column
	private String supplierTaxAmount;
	@Column
	private String supplierTaxBreakup;*/


	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public String getCredentialsName() {
		return credentialsName;
	}

	public void setCredentialsName(String credentialsName) {
		this.credentialsName = credentialsName;
	}

	public String getSupplierRateType() {
		return supplierRateType;
	}

	public void setSupplierRateType(String supplierRateType) {
		this.supplierRateType = supplierRateType;
	}


	public String getRateOfExchange() {
		return rateOfExchange;
	}

	public void setRateOfExchange(String rateOfExchange) {
		this.rateOfExchange = rateOfExchange;
	}

	public String getSuppFares() {
		return suppFares;
	}

	public void setSuppFares(String suppFares) {
		this.suppFares = suppFares;
	}

	public String getTotalFares() {
		return totalFares;
	}

	public void setTotalFares(String totalFares) {
		this.totalFares = totalFares;
	}
	
	public String getSuppBookRef() {
		return suppBookRef;
	}

	public void setSuppBookRef(String suppBookRef) {
		this.suppBookRef = suppBookRef;
	}


	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTripType() {
		return tripType;
	}
	
	public void setTripType(String tripType) {
		this.tripType = tripType;
	}
	
	public String getTripIndicator() {
		return tripIndicator;
	}
	
	public void setTripIndicator(String tripIndicator) {
		this.tripIndicator = tripIndicator;
	}

	

	public String getBookingDateTime() {
		return bookingDateTime;
	}

	public void setBookingDateTime(String bookingDateTime) {
		this.bookingDateTime = bookingDateTime;
	}

	public String getAmendDate() {
		return amendDate;
	}

	public void setAmendDate(String amendDate) {
		this.amendDate = amendDate;
	}

	public String getCancelDate() {
		return cancelDate;
	}

	public void setCancelDate(String cancelDate) {
		this.cancelDate = cancelDate;
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

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getClientCurrency() {
		return clientCurrency;
	}

	public void setClientCurrency(String clientCurrency) {
		this.clientCurrency = clientCurrency;
	}

	public String getClientIATANumber() {
		return clientIATANumber;
	}

	public void setClientIATANumber(String clientIATANumber) {
		this.clientIATANumber = clientIATANumber;
	}

	public String getSupplierTotalPrice() {
		return supplierTotalPrice;
	}

	public void setSupplierTotalPrice(String supplierTotalPrice) {
		this.supplierTotalPrice = supplierTotalPrice;
	}

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

	public String getSupplierPriceCurrencyCode() {
		return supplierPriceCurrencyCode;
	}

	public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
		this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
	}

	public String getTransfersDetails() {
		return transfersDetails;
	}

	public void setTransfersDetails(String transfersDetails) {
		this.transfersDetails = transfersDetails;
	}

	public String getSupplierID() {
		return supplierID;
	}

	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}

	public Set<TransfersPassengerDetails> getPassengerDetails() {
		return passengerDetails;
	}

	public void setPassengerDetails(Set<TransfersPassengerDetails> passengerDetails) {
		this.passengerDetails = passengerDetails;
	}
	public String getPaxDetails() {
		return paxDetails;
	}


	public void setPaxDetails(String paxDetails) {
		this.paxDetails = paxDetails;
	}

	@Override
	public String toString() {
		
		JSONObject transfersJson = new JSONObject();
		
		transfersJson.put("bookID", booking.getBookID());
		transfersJson.put("lastModifiedAt", lastModifiedAt);
		
		    JSONObject testJson = new JSONObject();
		
	       testJson.put("bookID", booking.getBookID());
			testJson.put("id", id);
			testJson.put("createdAt", createdAt);
			testJson.put("lastModifiedAt", lastModifiedAt);
			
			testJson.put("tripType",tripType);
			testJson.put("tripIndicator",tripIndicator);
			testJson.put("bookingDateTime",bookingDateTime);
			testJson.put("supplierTotalPrice",supplierTotalPrice);
			testJson.put("supplierPriceCurrencyCode",supplierPriceCurrencyCode);
			testJson.put("totalPrice",totalPrice);
			testJson.put("totalPriceCurrencyCode",totalPriceCurrencyCode);
		
			testJson.put("SuppFares", new JSONObject(suppFares));
			testJson.put("totalFares", new JSONObject(totalFares) );
			testJson.put("transfersDetails", new JSONObject(transfersDetails) );
		
			
			testJson.put("status", status);
			testJson.put("amendDate", amendDate);
			testJson.put("cancelDate", cancelDate);
			testJson.put("lastModifiedBy", lastModifiedBy);
			testJson.put("supplierID", supplierID);
			testJson.put("clientID", clientID);
			testJson.put("clientType", clientType);
			testJson.put("clientCurrency", clientCurrency);
			
			testJson.put("clientIATANumber", clientIATANumber);
			testJson.put("credentialsName", credentialsName);
			
			testJson.put("supplierRateType", supplierRateType);
		   
		
		if(operationType.equalsIgnoreCase("insert"))	{
			    testJson.put("supplierComms", this.getSuppcommercial());
			    testJson.put("clientComms", this.getClientCommercial());
				JSONObject bookingJson = new JSONObject(booking.toString());
				bookingJson.put("paymentInfo", new JSONArray(booking.getPaymentInfo().toString()));
				testJson.put("bookingInfo", bookingJson);
		}
		
	
		transfersJson.put("data_value", testJson);
		return transfersJson.toString();

	
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

  public String getTransferType() {
    return transferType;
  }

  public void setTransferType(String transferType) {
    this.transferType = transferType;
  }

  public String getAvailabilityStatus() {
    return availabilityStatus;
  }

  public void setAvailabilityStatus(String availabilityStatus) {
    this.availabilityStatus = availabilityStatus;
  }

  public String getPickUpLocation() {
    return pickUpLocation;
  }

  public void setPickUpLocation(String pickUpLocation) {
    this.pickUpLocation = pickUpLocation;
  }

  public String getAirportName() {
    return airportName;
  }

  public void setAirportName(String airportName) {
    this.airportName = airportName;
  }

  public String getTransferName() {
    return transferName;
  }

  public void setTransferName(String transferName) {
    this.transferName = transferName;
  }

  public String getTransferDescription() {
    return transferDescription;
  }

  public void setTransferDescription(String transferDescription) {
    this.transferDescription = transferDescription;
  }

  public String getDepartureCity() {
    return departureCity;
  }

  public void setDepartureCity(String departureCity) {
    this.departureCity = departureCity;
  }

  public String getArrivalCity() {
    return arrivalCity;
  }

  public void setArrivalCity(String arrivalCity) {
    this.arrivalCity = arrivalCity;
  }

  public String getDepartureDate() {
    return departureDate;
  }

  public void setDepartureDate(String departureDate) {
    this.departureDate = departureDate;
  }

  public String getArrivalDate() {
    return arrivalDate;
  }

  public void setArrivalDate(String arrivalDate) {
    this.arrivalDate = arrivalDate;
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

 
}
