package com.coxandkings.travel.bookingengine.resource.gateway;

public class Addresses {
    //variables
    private String addressType ="";
    private String otherAddressType="";
    private String addressLine1="";
    private String addressLine2="";
    private String country="";
    private String state="";
    private String area="";
    private String city="";
    private String location="";
    private Long postalCode=0L;
    private Long residentialTel=0L;
    private Long OfficeExtn=0L;
    private String fax="";
    //setter getter
    public String getAddressType() {
        return addressType;
    }
    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }
    public String getOtherAddressType() {
        return otherAddressType;
    }
    public void setOtherAddressType(String otherAddressType) {
        this.otherAddressType = otherAddressType;
    }
    public String getAddressLine1() {
        return addressLine1;
    }
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }
    public String getAddressLine2() {
        return addressLine2;
    }
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getArea() {
        return area;
    }
    public void setArea(String area) {
        this.area = area;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public Long getPostalCode() {
        return postalCode;
    }
    public void setPostalCode(Long postalCode) {
        this.postalCode = postalCode;
    }
    public Long getResidentialTel() {
        return residentialTel;
    }
    public void setResidentialTel(Long residentialTel) {
        this.residentialTel = residentialTel;
    }
    public Long getOfficeExtn() {
        return OfficeExtn;
    }
    public void setOfficeExtn(Long officeExtn) {
        OfficeExtn = officeExtn;
    }
    public String getFax() {
        return fax;
    }
    public void setFax(String fax) {
        this.fax = fax;
    }
}