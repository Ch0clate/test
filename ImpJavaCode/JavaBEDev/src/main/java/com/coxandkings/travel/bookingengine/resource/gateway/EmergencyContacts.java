package com.coxandkings.travel.bookingengine.resource.gateway;

public class EmergencyContacts {
    //variables
    private String name="";
    private String relationship="";
    private String address="";
    private Long telInBusinessHr=0L;
    private Long telAfterBusinessHr=0L ;
    private Long mobileNumber=0L;
    private String email="";
    //setter getter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getTelInBusinessHr() {
        return telInBusinessHr;
    }

    public void setTelInBusinessHr(Long telInBusinessHr) {
        this.telInBusinessHr = telInBusinessHr;
    }

    public Long getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(Long mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getTelAfterBusinessHr() {
        return telAfterBusinessHr;
    }

    public void setTelAfterBusinessHr(Long telAfterBusinessHr) {
        this.telAfterBusinessHr = telAfterBusinessHr;
    }
}
