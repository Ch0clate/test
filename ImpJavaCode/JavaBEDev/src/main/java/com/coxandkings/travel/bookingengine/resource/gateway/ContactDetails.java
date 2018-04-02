package com.coxandkings.travel.bookingengine.resource.gateway;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContactDetails
{
    //variables
    private CorporateAddress corporateAddress;
    private Addresses[] addresses;
    private EmergencyContacts[] emergencyContacts;

    //setter getter
    public CorporateAddress getCorporateAddress() {
        return corporateAddress;
    }

    public void setCorporateAddress(CorporateAddress corporateAddress) {
        this.corporateAddress = corporateAddress;
    }

    public EmergencyContacts[] getEmergencyContacts() {
        return emergencyContacts;
    }

    public void setEmergencyContacts(EmergencyContacts[] emergencyContacts) {
        this.emergencyContacts = emergencyContacts;
    }

    public Addresses[] getAddresses() {
        return addresses;
    }

    public void setAddresses(Addresses[] addresses) {
        this.addresses = addresses;
    }
}