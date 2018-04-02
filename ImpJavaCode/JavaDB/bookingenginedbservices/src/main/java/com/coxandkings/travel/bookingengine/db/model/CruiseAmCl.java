package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name=  "CRUISECL")
public class CruiseAmCl extends AbstractAmCl implements  Serializable {

	private static final long serialVersionUID = 1L;
	//Request
	@Column
	private String paxVerificationInfo;
	
	@Column
	private String currCode;
	
	@Column
	private String cnclOvrrides;
	
	@Column
	private String uniqueIDs;
	
	@Column
	private String supplierID;
	
	//Response
	@Column
	private String cancelRules;
	
	@Column
	private String cancelInfoIDs;
	
	public String getCancelRules() {
		return cancelRules;
	}
	public void setCancelRules(String cancelRules) {
		this.cancelRules = cancelRules;
	}
	public String getCancelInfoIDs() {
		return cancelInfoIDs;
	}
	public void setCancelInfoIDs(String cancelInfoIDs) {
		this.cancelInfoIDs = cancelInfoIDs;
	}
	public String getPaxVerificationInfo() {
		return paxVerificationInfo;
	}
	public void setPaxVerificationInfo(String paxVerificationInfo) {
		this.paxVerificationInfo = paxVerificationInfo;
	}
	public String getCurrCode() {
		return currCode;
	}
	public void setCurrCode(String currCode) {
		this.currCode = currCode;
	}
	public String getCnclOvrrides() {
		return cnclOvrrides;
	}
	public void setCnclOvrrides(String cnclOvrrides) {
		this.cnclOvrrides = cnclOvrrides;
	}
	public String getUniqueIDs() {
		return uniqueIDs;
	}
	public void setUniqueIDs(String uniqueIDs) {
		this.uniqueIDs = uniqueIDs;
	}
	public String getSupplierID() {
		return supplierID;
	}
	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}
}
