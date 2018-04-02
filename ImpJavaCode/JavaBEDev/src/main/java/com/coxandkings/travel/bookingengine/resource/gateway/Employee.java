package com.coxandkings.travel.bookingengine.resource.gateway;

public class Employee
{   //variables
    private String title="";
    private String firstName="";
    private String middleName="";
    private String lastName="";
    private String travellerName="";
    private String gender="";
    private DateOfBirth dateOfBirth;
    private String nationality="";
    private long mobileNumber=0L;
    private String personalEmailId="";
    private String maritalStatus="";
    private Anniversary anniversary;
    private String nationalIdType="";
    private String nationalIdNumber="";
    private UploadTravellerPhoto uploadTravellerPhoto;
    //setter getter
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

    public String getTravellerName() {
        return travellerName;
    }

    public void setTravellerName(String travellerName) {
        this.travellerName = travellerName;
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

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public Anniversary getAnniversary() {
        return anniversary;
    }

    public void setAnniversary(Anniversary anniversary) {
        this.anniversary = anniversary;
    }

    public String getNationalIdType() {
        return nationalIdType;
    }

    public void setNationalIdType(String nationalIdType) {
        this.nationalIdType = nationalIdType;
    }

    public String getNationalIdNumber() {
        return nationalIdNumber;
    }

    public void setNationalIdNumber(String nationalIdNumber) {
        this.nationalIdNumber = nationalIdNumber;
    }

    public UploadTravellerPhoto getUploadTravellerPhoto() {
        return uploadTravellerPhoto;
    }

    public void setUploadTravellerPhoto(UploadTravellerPhoto uploadTravellerPhoto) {
        this.uploadTravellerPhoto = uploadTravellerPhoto;
    }
}