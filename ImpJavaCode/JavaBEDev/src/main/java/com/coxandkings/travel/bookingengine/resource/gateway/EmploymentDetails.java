package com.coxandkings.travel.bookingengine.resource.gateway;

public class EmploymentDetails
{
    //variables
    private Boolean approvalRequired=false;
    private String employeeId="";
    private String grade="";
    private String designation="";
    private String department="";
    private String projectCode="";
    private String location="";
    private String costCenterCode="";
    private String reportingManager="";
    private String employeeStatus="";
    private DateOfJoining dateOfJoining;
    private String profileType="";
    private String officeEmailId="";
    //setter getter
    public Boolean getApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(Boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCostCenterCode() {
        return costCenterCode;
    }

    public void setCostCenterCode(String costCenterCode) {
        this.costCenterCode = costCenterCode;
    }

    public String getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(String reportingManager) {
        this.reportingManager = reportingManager;
    }

    public String getEmployeeStatus() {
        return employeeStatus;
    }

    public void setEmployeeStatus(String employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    public DateOfJoining getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(DateOfJoining dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getProfileType() {
        return profileType;
    }

    public void setProfileType(String profileType) {
        this.profileType = profileType;
    }

    public String getOfficeEmailId() {
        return officeEmailId;
    }

    public void setOfficeEmailId(String officeEmailId) {
        this.officeEmailId = officeEmailId;
    }
}

