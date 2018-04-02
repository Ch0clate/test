package com.coxandkings.travel.bookingengine.orchestrator.cruise;

import com.coxandkings.travel.bookingengine.utils.Constants;

public interface CruiseConstants extends Constants {

	public static final String PRODUCT_CRUISE = "CRUISE";
	public static final String PRODUCT_CRUISE_SUPPLTRANSRQ = "SUPPLTRANSRQ";
	public static final String PRODUCT_CRUISE_SUPPLTRANSRS = "SUPPLTRANSRS";
	
	public static final String PRODUCT_CRUISE_SUPPLCRUISE = "http://www.coxandkings.com/integ/suppl/cruise";
	public static final String PRODUCT_CRUISE_TOTALFARE = "totalFare";
	public static final char KEYSEPARATOR = '|';
	
	public static final String JSON_PROP_RECEIVABLE = "receivable";
	public static final String JSON_PROP_RECEIVABLES = "receivables";
	
	public static final String JSON_PROP_CRUISETOTALFARE = "cruiseTotalFare";
	public static final String JSON_PROP_BASEFARE = "baseFare";
}
