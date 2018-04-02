package com.coxandkings.travel.bookingengine.userctx;

import org.bson.Document;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.utils.Constants;
import com.mongodb.DBCollection;

public class OrgHierarchy implements Constants {
	
	private String mCompanyId, mCompanyName, mCompanyState, mCompanyCity, mCompanyCountry, mSBU, mBU;
	private String mDivision, mSalesOffice, mSalesOfficeLoc, mSalesOfficeName;
	
	OrgHierarchy(JSONObject orgHierarchyJson) {
		mCompanyId = orgHierarchyJson.optString(JSON_PROP_COMPANYID, "");
		mCompanyName = orgHierarchyJson.optString(JSON_PROP_COMPANYNAME, "");
		mCompanyCity = orgHierarchyJson.optString(JSON_PROP_COMPANYCITY, "");
		mCompanyState = orgHierarchyJson.optString(JSON_PROP_COMPANYSTATE, "");
		mCompanyCountry = orgHierarchyJson.optString(JSON_PROP_COMPANYCOUNTRY, "");
		mSBU = orgHierarchyJson.optString(JSON_PROP_SBU, "");
		mBU = orgHierarchyJson.optString(JSON_PROP_BU, "");
		mDivision = orgHierarchyJson.optString(JSON_PROP_DIVISION, "");
		mSalesOffice = orgHierarchyJson.optString(JSON_PROP_SALESOFFICE, "");
		mSalesOfficeLoc = orgHierarchyJson.optString(JSON_PROP_SALESOFFICELOC);
		mSalesOfficeName = orgHierarchyJson.optString(JSON_PROP_SALESOFFICENAME, "");
	}
	
	// The org.bson.Document received here would either be from clientB2Bs (clientProfile.orgHierarchy) or clientTypes (orgHierarchy)
	OrgHierarchy(Document orgHierarchyDoc) {
		Document orgEntityDoc = null;
		if (orgHierarchyDoc != null) {
			mCompanyId = (orgHierarchyDoc.containsKey(MDM_PROP_COMPANYID)) ? orgHierarchyDoc.getString(MDM_PROP_COMPANYID) : (orgHierarchyDoc.containsKey(MDM_PROP_COMPANY)) ? orgHierarchyDoc.getString(MDM_PROP_COMPANY) : "";
			orgEntityDoc = UserContext.getOrgHierarchyDocumentById(MDM_VAL_TYPECOMPANY, mCompanyId);
			if (orgEntityDoc != null) {
				mCompanyName = (String) orgEntityDoc.getOrDefault(MDM_PROP_NAME, "");
				mCompanyCity = MDMUtils.getValueAtPathFromDocument(orgEntityDoc, MDM_PROP_ADDRESS.concat(".").concat(MDM_PROP_CITY));
				mCompanyState = MDMUtils.getValueAtPathFromDocument(orgEntityDoc, MDM_PROP_ADDRESS.concat(".").concat(MDM_PROP_STATE));
				mCompanyCountry = MDMUtils.getValueAtPathFromDocument(orgEntityDoc, MDM_PROP_ADDRESS.concat(".").concat(MDM_PROP_COUNTRY));
			}
			
			mSBU = (String) orgHierarchyDoc.getOrDefault(MDM_PROP_SBU, "");
			mBU = (String) orgHierarchyDoc.getOrDefault(MDM_PROP_BU, "");
			// clientType document from MDM does not contain Sales Office information
			mSalesOfficeName = (String) orgHierarchyDoc.getOrDefault(MDM_PROP_REPORTSONAME, "");
			
			if (mSalesOfficeName != null && mSalesOfficeName.isEmpty() == false) {
				// TODO: Check if companyId also needs to be passed in the following call
				orgEntityDoc = UserContext.getOrgHierarchyDocumentByName(MDM_VAL_TYPESO, mSalesOfficeName);
				if (orgEntityDoc != null) {
					mSalesOffice = orgEntityDoc.getString(DBCollection.ID_FIELD_NAME);
					mSalesOfficeLoc = MDMUtils.getValueAtPathFromDocument(orgEntityDoc, MDM_PROP_ADDRESS.concat(".").concat(MDM_PROP_CITY));
					mDivision = orgEntityDoc.getString(MDM_PROP_DIVISION);
				}
			}
		}
	}
	
	public JSONObject toJSON() {
		JSONObject orgHierarchyJson = new JSONObject();
		
		orgHierarchyJson.put(JSON_PROP_COMPANYID, mCompanyId);
		orgHierarchyJson.put(JSON_PROP_COMPANYNAME, mCompanyName);
		orgHierarchyJson.put(JSON_PROP_COMPANYCITY, mCompanyCity);
		orgHierarchyJson.put(JSON_PROP_COMPANYSTATE, mCompanyState);
		orgHierarchyJson.put(JSON_PROP_COMPANYCOUNTRY, mCompanyCountry);
		orgHierarchyJson.put(JSON_PROP_SBU, mSBU);
		orgHierarchyJson.put(JSON_PROP_BU, mBU);
		orgHierarchyJson.put(JSON_PROP_DIVISION, mDivision);
		orgHierarchyJson.put(JSON_PROP_SALESOFFICE, mSalesOffice);
		orgHierarchyJson.put(JSON_PROP_SALESOFFICELOC, mSalesOfficeLoc);
		orgHierarchyJson.put(JSON_PROP_SALESOFFICENAME, mSalesOfficeName);
		
		return orgHierarchyJson;
	}
	
	public String getCompanyId() {
		return mCompanyId;
	}

	public String getCompanyCity() {
		return mCompanyCity;
	}

	public String getCompanyCountry() {
		return mCompanyCountry;
	}

	public String getCompanyName() {
		return mCompanyName;
	}

	public String getCompanyState() {
		return mCompanyState;
	}

	public String getSBU() {
		return mSBU;
	}

	public String getBU() {
		return mBU;
	}

	public String getDivision() {
		return mDivision;
	}

	public String getSalesOffice() {
		return mSalesOffice;
	}

	public String getSalesOfficeLoc() {
		return mSalesOfficeLoc;
	}

	public String getSalesOfficeName() {
		return mSalesOfficeName;
	}
	
}
