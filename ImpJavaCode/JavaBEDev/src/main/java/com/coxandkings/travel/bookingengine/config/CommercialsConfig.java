package com.coxandkings.travel.bookingengine.config;

import org.bson.Document;

public class CommercialsConfig extends RuleEngineConfig {
	
	private static final String PROP_COMMERCIAL_TYPE_CONFIG = "commercialTypesConfig";
	public static final String COMMERCIAL_SUPPLIER_TRANSACTIONAL = "supplierTransactional";
	public static final String COMMERCIAL_CLIENT_TRANSACTIONAL = "clientTransactional";
	
	public CommercialsConfig(Document commConfigDoc) {
		super(commConfigDoc);
	}

	public CommercialTypeConfig getCommercialTypeConfig(String commType) {
		return super.getInvocationTypeConfig(commType);
	}

	@Override
	String getInvocationTypeConfigName() {
		return PROP_COMMERCIAL_TYPE_CONFIG;
	}
	
}
