package com.coxandkings.travel.bookingengine.resource.gateway;

public class Photo {
    //variables
    private String  fileName="";

    private String fileCategory="";

    private String fileURL="";
    //setter getter
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileCategory() {
        return fileCategory;
    }

    public void setFileCategory(String fileCategory) {
        this.fileCategory = fileCategory;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }
}
