package com.coxandkings.travel.bookingengine.resource.gateway;

public class FamilyDetails {
    //variables
    private String relationship="";
    private String title="";
    private String firstName="";
    private String middleName="";
    private String lastName="";
    private String gender="";
    private DateOfBirth dateOfBirth;
    private String nationality="";
    private long mobileNumber=0L;
    private String personalEmailId="";
    private String diet="";
    private String dietRemarks="";
    private Boolean differentiallyEnabled=false;
    private String spRemark="";
    private Photo photo;
    private String[]loyaltyPrograms;
    private String[] passportDetails;
    private String[] visaDocuments;
    private String preferences="";
    //setter getter
    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public DateOfBirth getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(DateOfBirth dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public long getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(long mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPersonalEmailId() {
        return personalEmailId;
    }

    public void setPersonalEmailId(String personalEmailId) {
        this.personalEmailId = personalEmailId;
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

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
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

    public String[] getVisaDocuments() {
        return visaDocuments;
    }

    public void setVisaDocuments(String[] visaDocuments) {
        this.visaDocuments = visaDocuments;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }
}
