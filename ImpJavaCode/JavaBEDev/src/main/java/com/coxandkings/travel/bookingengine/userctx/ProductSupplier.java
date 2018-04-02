package com.coxandkings.travel.bookingengine.userctx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;
import com.mongodb.DBCollection;

public class ProductSupplier implements Constants {
	private String mSuppId, mCredsName;
	private Map<String, Credential> mSuppCreds = new LinkedHashMap<String, Credential>();
	private List<OperationURL> mOpURLs = new ArrayList<OperationURL>();
	private SupplierSettlementTerms mSuppSettTerms;
	private boolean mIsGDS;
	
	ProductSupplier(JSONObject prodSuppJson) throws Exception {
		mSuppId = prodSuppJson.getString(JSON_PROP_SUPPID);
		mCredsName = prodSuppJson.getString(JSON_PROP_CREDSNAME);
		mIsGDS = prodSuppJson.getBoolean(JSON_PROP_ISGDSSUPP);

		JSONArray credsJson = prodSuppJson.getJSONArray(JSON_PROP_CREDS);
		for (int i=0; i < credsJson.length(); i++) {
			Credential cred = new Credential(credsJson.getJSONObject(i)); 
			mSuppCreds.put(cred.getKey(), cred);
		}

		JSONArray opUrlsJson = prodSuppJson.getJSONArray(JSON_PROP_OPURLS);
		for (int i=0; i < opUrlsJson.length(); i++) {
			mOpURLs.add(new OperationURL(opUrlsJson.getJSONObject(i)));
		}
	}

	@SuppressWarnings("unchecked")
	ProductSupplier(org.bson.Document prodSuppDoc) throws Exception {
		mCredsName = prodSuppDoc.getString(DBCollection.ID_FIELD_NAME);
		org.bson.Document suppDoc = (org.bson.Document) prodSuppDoc.get(MDM_PROP_SUPP);
		mSuppId = (suppDoc != null) ? suppDoc.getString(JSON_PROP_SUPPID) : "";
		if (mSuppId.isEmpty() == false) {
			org.bson.Document suppMasterDoc = MDMUtils.getSupplier(mSuppId);
			if (suppMasterDoc != null) {
				List<String> enablerCategories = (List<String>) MDMUtils.getValueObjectAtPathFromDocument(suppMasterDoc, MDM_PROP_SUPP.concat(".").concat(MDM_PROP_ENABLERCATEG));
				mIsGDS = (enablerCategories != null && enablerCategories.contains(MDM_VAL_SUPPGDS));
			}
		}

		org.bson.Document credDtlsDoc = (org.bson.Document) prodSuppDoc.get(MDM_PROP_CREDDEATILS);
		List<org.bson.Document> credsDocs = (List<org.bson.Document>) credDtlsDoc.get(MDM_PROP_CREDS);
		for (org.bson.Document credsDoc : credsDocs) {
			Credential cred = new Credential(credsDoc); 
			mSuppCreds.put(cred.getKey(), cred);
		}

		List<org.bson.Document> epUrlsDocs = (List<org.bson.Document>) credDtlsDoc.get(MDM_PROP_ENDPOINTURLS);
		for (org.bson.Document epUrlsDoc : epUrlsDocs) {
			mOpURLs.add(new OperationURL(epUrlsDoc));
		}
	}
	
	public String getSupplierID() {
		return mSuppId;
	}
	
	public Credential getCredentialForKey(String key) {
		return mSuppCreds.get(key);
	}
	
	public Collection<Credential> getCredentials() {
		return mSuppCreds.values();
	}

	public String getCredentialsName() {
		return mCredsName;
	}

	public List<OperationURL> getOperationURLs() {
		return mOpURLs;
	}
	
	public Element toElement(Document ownerDoc) {
		Element suppCredsElem = ownerDoc.createElementNS(NS_COM, "com:SupplierCredentials");
		
		Element suppIDElem = ownerDoc.createElementNS(NS_COM, "com:SupplierID");
		suppIDElem.setTextContent(mSuppId);
		suppCredsElem.appendChild(suppIDElem);
		
		Element credsElem = ownerDoc.createElementNS(NS_COM, "com:Credentials");
		credsElem.setAttribute("name", mCredsName);
		Collection<Credential> creds = mSuppCreds.values();
		for (Credential cred : creds) {
			credsElem.appendChild(cred.toElement(ownerDoc));
		}
		
		Element opUrlsElem = ownerDoc.createElementNS(NS_COM, "com:OperationURLs");
		for (OperationURL opUrl : mOpURLs) {
			opUrlsElem.appendChild(opUrl.toElement(ownerDoc));
		}
		credsElem.appendChild(opUrlsElem);
		suppCredsElem.appendChild(credsElem);
		
		return suppCredsElem;
	}
	
	public Element toElement(Document ownerDoc, int sequence) {
		Element suppCredsElem = toElement(ownerDoc);
		Element seqElem = ownerDoc.createElementNS(NS_COM, "com:Sequence");
		seqElem.setTextContent(String.valueOf(sequence));
		suppCredsElem.insertBefore(seqElem, XMLUtils.getFirstNodeAtXPath(suppCredsElem, "./com:Credentials"));
		return suppCredsElem;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put(JSON_PROP_SUPPID, mSuppId);
		json.put(JSON_PROP_CREDSNAME, mCredsName);
		json.put(JSON_PROP_ISGDSSUPP, mIsGDS);

		// Create Credentials JSON structure
		JSONArray credJsonArr = new JSONArray();
		Collection<Credential> creds = mSuppCreds.values();
		for (Credential cred : creds) {
			credJsonArr.put(cred.toJSON());
		}
		json.put(JSON_PROP_CREDS, credJsonArr);
		
		// Create OperationURLs JSON structure
		JSONArray opUrlJsonArr = new JSONArray();
		for (OperationURL opUrl : mOpURLs) {
			opUrlJsonArr.put(opUrl.toJSON());
		}
		json.put(JSON_PROP_OPURLS, opUrlJsonArr);
		
		return json;
	}
	
	public Element getPaymentDetailsElement(String prodCateg, String prodCategSubType, org.w3c.dom.Document ownerDoc) {
		if (mSuppSettTerms == null) {
			mSuppSettTerms = new SupplierSettlementTerms(this, prodCateg, prodCategSubType);
		}
		
		return mSuppSettTerms.toPaymentDetailsElement(ownerDoc);
	}
}
