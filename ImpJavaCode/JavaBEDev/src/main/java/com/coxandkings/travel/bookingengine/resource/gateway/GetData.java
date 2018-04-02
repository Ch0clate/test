package com.coxandkings.travel.bookingengine.resource.gateway;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;

public class GetData {

    //variables
    private String tierId ="";
    private String entityId="";
    private String entityType="";
    private Long __v;
    private Boolean deleted=false;
    private String lastUpdated="";
    private String createdAt="";
    private String createdBy="";
    private String updatedBy="";
    private FamilyDetails[] familyDetails;
    private ContactDetails contactDetails;
    private TravellerDetails travellerDetails;
    private ClientDetails clientDetails;


    //setter getter

    public ClientDetails getClientDetails() {
        return clientDetails;
    }

    public void setClientDetails(ClientDetails clientDetails) {
        this.clientDetails = clientDetails;
    }

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public TravellerDetails getTravellerDetails() {
        return travellerDetails;
    }

    public void setTravellerDetails(TravellerDetails travellerDetails) {
        this.travellerDetails = travellerDetails;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public FamilyDetails[] getFamilyDetails() {
        return familyDetails;
    }

    public void setFamilyDetails(FamilyDetails[] familyDetails) {
        this.familyDetails = familyDetails;
    }

    public Long get__v() {
        return __v;
    }

    public void set__v(Long __v) {
        this.__v = __v;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

