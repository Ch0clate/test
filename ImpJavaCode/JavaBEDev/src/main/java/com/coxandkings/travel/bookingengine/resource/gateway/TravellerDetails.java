package com.coxandkings.travel.bookingengine.resource.gateway;

public class TravellerDetails {
    //variables
    private Employee employee;
    private EmploymentDetails employmentDetails;
    private Boolean selfBookingToolUser=false;
    private String userId="";
    private Boolean personalTravelAllowed;
    private String diet="";
    private String dietRemarks="";
    private Boolean differentiallyEnabled=false;
    private String spRemark="";
    private String[] loyaltyPrograms;
    private String[] passportDetails;
    private String[] visaDocuments;
    private String preferences="";
    private String[] interests;
    private String identity="";
    private Friends[] friends;
    //setter getter
    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public EmploymentDetails getEmploymentDetails() {
        return employmentDetails;
    }

    public void setEmploymentDetails(EmploymentDetails employmentDetails) {
        this.employmentDetails = employmentDetails;
    }

    public Boolean getSelfBookingToolUser() {
        return selfBookingToolUser;
    }

    public void setSelfBookingToolUser(Boolean selfBookingToolUser) {
        this.selfBookingToolUser = selfBookingToolUser;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getPersonalTravelAllowed() {
        return personalTravelAllowed;
    }

    public void setPersonalTravelAllowed(Boolean personalTravelAllowed) {
        this.personalTravelAllowed = personalTravelAllowed;
    }

    public String getDiet() {
        return diet;
    }

    public void setDiet(String diet) {
        this.diet = diet;
    }

    public String getDietRemarks() {
        return dietRemarks;
    }

    public void setDietRemarks(String dietRemarks) {
        this.dietRemarks = dietRemarks;
    }

    public Boolean getDifferentiallyEnabled() {
        return differentiallyEnabled;
    }

    public void setDifferentiallyEnabled(Boolean differentiallyEnabled) {
        this.differentiallyEnabled = differentiallyEnabled;
    }

    public String getSpRemark() {
        return spRemark;
    }

    public void setSpRemark(String spRemark) {
        this.spRemark = spRemark;
    }

    public String[] getVisaDocuments() {
        return visaDocuments;
    }

    public void setVisaDocuments(String[] visaDocuments) {
        this.visaDocuments = visaDocuments;
    }

    public String[] getLoyaltyPrograms() {
        return loyaltyPrograms;
    }

    public void setLoyaltyPrograms(String[] loyaltyPrograms) {
        this.loyaltyPrograms = loyaltyPrograms;
    }

    public String[] getPassportDetails() {
        return passportDetails;
    }

    public void setPassportDetails(String[] passportDetails) {
        this.passportDetails = passportDetails;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public String[] getInterests() {
        return interests;
    }

    public void setInterests(String[] interests) {
        this.interests = interests;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public Friends[] getFriends() {
        return friends;
    }

    public void setFriends(Friends[] friends) {
        this.friends = friends;
    }
}

