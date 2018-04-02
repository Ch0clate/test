package com.coxandkings.travel.bookingengine.userctx;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.utils.Constants;

public class SupplierSettlementTerms implements Constants {
	private Logger logger = LogManager.getLogger(SupplierSettlementTerms.class);
	
	private SettlementType mSettType;
	private Document mConfigDoc;
	private ProductSupplier mProdSupp;
	
	@SuppressWarnings("unchecked")
	SupplierSettlementTerms(ProductSupplier prodSupp, String prodCateg, String prodCategSubType) {
		mSettType = SettlementType.unknown;
		mConfigDoc = null;
		mProdSupp = prodSupp;
		
		String suppID = mProdSupp.getSupplierID();
		String credsName = mProdSupp.getCredentialsName();
		
		org.bson.Document suppCommDoc = MDMUtils.getSupplierCommercials(prodCateg, prodCategSubType, suppID);
		if (suppCommDoc == null) {
			logger.trace(String.format("Supplier commercials definition for productCategory=%s, productCategorySubType=%s and SupplierId=%s not found", prodCateg, prodCategSubType, suppID));
			return;
		}
		
		boolean isCommissionable = Boolean.valueOf(MDMUtils.getValueAtPathFromDocument(suppCommDoc, MDM_PROP_STANDARDCOMM.concat(".").concat(MDM_PROP_ISCOMMISSIONABLE)));
		//boolean isCommissionable = (Boolean) MDMUtils.getValueObjectAtPathFromDocument(suppCommDoc, MDM_PROP_STANDARDCOMM.concat(".").concat(MDM_PROP_ISCOMMISSIONABLE));
		org.bson.Document suppSettleDoc = MDMUtils.getSupplierSettlementTerms(prodCateg, prodCategSubType, suppID);
		if (suppSettleDoc == null) {
			logger.trace(String.format("Supplier settlement definition for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
			return;
		}
		
		org.bson.Document stdCommSettlementDoc = MDMUtils.getStandardCommercialSettlementTerms(suppSettleDoc, isCommissionable); 
		if (stdCommSettlementDoc == null) {
			logger.trace(String.format("Standard commercial settlement terms for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
			return;
		}
		
		String typeOfSettlement = stdCommSettlementDoc.getString(MDM_PROP_TYPEOFSETTLE);
		if (typeOfSettlement == null) {
			logger.trace(String.format("Type of standard commercial settlement for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
			return;
		}
		if (MDM_VAL_CREDIT.equals(typeOfSettlement) == false && MDM_VAL_NOCREDIT.equals(typeOfSettlement) == false) {
			logger.trace(String.format("Type %s of standard commercial settlement is not %s or %s for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s", typeOfSettlement, MDM_VAL_CREDIT, MDM_VAL_NOCREDIT, prodCateg, prodCategSubType, suppID, credsName));
			return;
		}
		
		if (MDM_VAL_CREDIT.equals(typeOfSettlement)) {
			List<org.bson.Document> credSettDocs = (List<org.bson.Document>) stdCommSettlementDoc.get(MDM_PROP_CREDITSETTLEMENT);
			if (credSettDocs == null || credSettDocs.size() == 0) {
				logger.trace(String.format("Configuration for credit settlement of Standard commercial for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
				return;
			}
			
			if (credSettDocs.size() > 1) {
				logger.trace(String.format("Multiple configurations for credit settlement of Standard commercial for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s found. Only first configuration will be considered.", prodCateg, prodCategSubType, suppID, credsName));
			}

			org.bson.Document defCredDoc = (org.bson.Document) credSettDocs.get(0).get(MDM_PROP_DEFCREDITTYPE);
			if (defCredDoc == null) {
				logger.trace(String.format("Credit type definition of Standard commercial for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
				return;
			}
			
			String creditType = defCredDoc.getString(MDM_PROP_CREDITTYPE);
			String modeOfSecurity = defCredDoc.getString(MDM_PROP_MODEOFSECURITY);
			mSettType = SettlementType.forStrings(typeOfSettlement, creditType, modeOfSecurity);
			mConfigDoc = defCredDoc;
			if (isCredentialsNameConfigured(mConfigDoc, credsName) == false) {
				mConfigDoc = null;
				logger.trace(String.format("Credential %s is not configured in credit type definition of Standard commercial for productCategory=%s, productCategorySubType=%s and SupplierId=%s", credsName, prodCateg, prodCategSubType, suppID));
				return;
			}
		}
		else {
			List<Document> noCredSettDocs = (List<Document>) stdCommSettlementDoc.get(MDM_PROP_CREDITSETTLEMENT);
			if (noCredSettDocs == null || noCredSettDocs.size() == 0) {
				logger.trace(String.format("Configuration for no-credit settlement of Standard commercial for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
				return;
			}
			
			if (noCredSettDocs.size() > 1) {
				logger.trace(String.format("Multiple configurations for no-credit settlement of Standard commercial for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s found. Only first configuration will be considered.", prodCateg, prodCategSubType, suppID, credsName));
			}

			org.bson.Document defNoCredDoc = (org.bson.Document) noCredSettDocs.get(0);
			if (defNoCredDoc == null) {
				logger.trace(String.format("No-credit type definition of Standard commercial for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s not found", prodCateg, prodCategSubType, suppID, credsName));
				return;
			}
			
			String creditType = defNoCredDoc.getString(MDM_PROP_CREDITTYPE);
			if (MDM_VAL_DEPOSIT.equals(creditType) == false && MDM_VAL_PREPAYMENT.equals(creditType) == false) {
				logger.trace(String.format("Credit type %s in no-credit type definition of Standard commercial is not %s or %s for productCategory=%s, productCategorySubType=%s, SupplierId=%s and CredentialsName=%s", creditType, MDM_VAL_DEPOSIT, MDM_VAL_PREPAYMENT, prodCateg, prodCategSubType, suppID, credsName));
				return;
			}
			
			mSettType = SettlementType.forStrings(typeOfSettlement, creditType, null);
			String configDocName = (MDM_VAL_DEPOSIT.equals(creditType)) ? MDM_PROP_DEPOSITSETTLEMENT : MDM_PROP_PREPAYSETTLEMENT;
			mConfigDoc = (Document) defNoCredDoc.get(configDocName);
			if (isCredentialsNameConfigured(mConfigDoc, credsName) == false) {
				mConfigDoc = null;
				logger.trace(String.format("Credential %s is not configured in no-credit type definition %s of Standard commercial for productCategory=%s, productCategorySubType=%s and SupplierId=%s", credsName, configDocName, prodCateg, prodCategSubType, suppID));
				return;
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private boolean isCredentialsNameConfigured(Document configDoc, String credsName) {
		if (configDoc == null || credsName == null || credsName.trim().isEmpty()) {
			return false;
		}
		
		List<String> credNames = (List<String>) configDoc.get(MDM_PROP_CREDNAMES);
		if (credNames == null) {
			return false;
		}
		
		for (String credName : credNames) {
			if (credsName.equals(credName)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	@SuppressWarnings("incomplete-switch")
	public Element toPaymentDetailsElement(org.w3c.dom.Document ownerDoc) {
		Element paymentDetail = ownerDoc.createElementNS(NS_OTA, "ota:PaymentDetail");
		if (mConfigDoc != null) {
			switch (mSettType) {
				// TODO: Need to find out how each type of settlement type translates to PaymentDetail element in OTA
				case creditUnsecured : {
					paymentDetail.setAttribute(XML_ATTR_PAYTYPE, OTA_PAYTYPE_BUSSACCT);
					
					Credential agNameCred = mProdSupp.getCredentialForKey(CREDENTIAL_PROP_AGENTNAME);
					if (agNameCred != null) {
						paymentDetail.setAttribute(XML_ATTR_COSTCENTERID, agNameCred.getValue());
					}
					
					Credential agCodeCred = mProdSupp.getCredentialForKey(CREDENTIAL_PROP_AGENTCODE);
					if (agNameCred != null) {
						paymentDetail.setAttribute(XML_ATTR_GUARANTEEID, agCodeCred.getValue());
					}
					
					break;
				}
			}
		}
		
		return paymentDetail;
	}
}
