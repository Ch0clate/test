package com.coxandkings.travel.bookingengine.resource.search;

import com.coxandkings.travel.bookingengine.resource.BaseResource;

public class SearchRequestResource extends BaseResource{

	private String originLocation;
	private String destinationLocation;
	private String departureDate;
	private String arrivalDate;
	private String originLocation1;
	private String destinationLocation1;
	private String departureDate1;
	private String departureDate2;
	private String originLocation2;
	private String destinationLocation2;
	private String adult;
	private String children;
	private String infant;
	private String cabinClass;
	private String tripType;
	
	public String getOriginLocation() {
		return originLocation;
	}
	public void setOriginLocation(String originLocation) {
		this.originLocation = originLocation;
	}
	public String getDestinationLocation() {
		return destinationLocation;
	}
	public void setDestinationLocation(String destinationLocation) {
		this.destinationLocation = destinationLocation;
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
	public String getOriginLocation1() {
		return originLocation1;
	}
	public void setOriginLocation1(String originLocation1) {
		this.originLocation1 = originLocation1;
	}
	public String getDestinationLocation1() {
		return destinationLocation1;
	}
	public void setDestinationLocation1(String destinationLocation1) {
		this.destinationLocation1 = destinationLocation1;
	}
	public String getDepartureDate1() {
		return departureDate1;
	}
	public void setDepartureDate1(String departureDate1) {
		this.departureDate1 = departureDate1;
	}
	public String getDepartureDate2() {
		return departureDate2;
	}
	public void setDepartureDate2(String departureDate2) {
		this.departureDate2 = departureDate2;
	}
	public String getOriginLocation2() {
		return originLocation2;
	}
	public void setOriginLocation2(String originLocation2) {
		this.originLocation2 = originLocation2;
	}
	public String getDestinationLocation2() {
		return destinationLocation2;
	}
	public void setDestinationLocation2(String destinationLocation2) {
		this.destinationLocation2 = destinationLocation2;
	}
	public String getAdult() {
		return adult;
	}
	public void setAdult(String adult) {
		this.adult = adult;
	}
	public String getChildren() {
		return children;
	}
	public void setChildren(String children) {
		this.children = children;
	}
	public String getInfant() {
		return infant;
	}
	public void setInfant(String infant) {
		this.infant = infant;
	}
	public String getCabinClass() {
		return cabinClass;
	}
	public void setCabinClass(String cabinClass) {
		this.cabinClass = cabinClass;
	}
	public String getTripType() {
		return tripType;
	}
	public void setTripType(String tripType) {
		this.tripType = tripType;
	}
	
	
}
