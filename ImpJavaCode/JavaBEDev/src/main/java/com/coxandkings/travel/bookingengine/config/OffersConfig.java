package com.coxandkings.travel.bookingengine.config;

import org.bson.Document;

public class OffersConfig extends RuleEngineConfig {
	
	private static final String PROP_OFFER_TYPE_CONFIG = "offerTypesConfig";
	public enum Type { 
		
		COMPANY_SEARCH_TIME("companySearchTime"),
		COMPANY_REDEEM_TIME("companyRedeemTime"), 
		SUPPLIER_SEARCH_TIME("supplierSearchTime"), 
		SUPPLIER_REDEEM_TIME("supplierRedeemType");
		
		private String mTypeString;
		private Type(String typeString) {
			mTypeString = typeString;
		}
		
		public String toString() {
			return mTypeString;
		}
	};
	
	public OffersConfig(Document offersConfigDoc) {
		super(offersConfigDoc);
	}

	@Override
	String getInvocationTypeConfigName() {
		return PROP_OFFER_TYPE_CONFIG;
	}
	
	public CommercialTypeConfig getOfferTypeConfig(Type offerType) {
		return super.getInvocationTypeConfig(offerType.toString());
	}
}
