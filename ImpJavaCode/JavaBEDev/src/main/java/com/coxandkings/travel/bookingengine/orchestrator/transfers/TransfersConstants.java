package com.coxandkings.travel.bookingengine.orchestrator.transfers;

import com.coxandkings.travel.bookingengine.utils.Constants;

public interface TransfersConstants  extends Constants {
	
	public static final String JSON_PROP_LOCATIONTYPE = "LocationType";
	public static final String JSON_PROP_LOCATIONCODE = "LocationCode";
	public static final String JSON_PROP_DATETIME = "DateTime";
	public static final String JSON_PROP_LONGITUDE = "Longitude";
	public static final String JSON_PROP_LATITUDE = "Latitude";
	
	public static final String JSON_PROP_GROUNDSERVICES = "groundServices";
	public static final String JSON_PROP_GROUNDWRAPPER = "groundBookRSWrapper";
	public static final String JSON_PROP_SERVICE = "service";
	public static final String JSON_PROP_SERVICES = "service";
	public static final String JSON_PROP_RESTRICTIONS = "restrictions";
	public static final String JSON_PROP_TOTALCHARGE = "totalCharge";
	public static final String JSON_PROP_TAXDETAILS = "TaxDetails";
	public static final String JSON_PROP_REFERENCE = "reference";
	public static final String JSON_PROP_REFERENCE1 = "reference";
	public static final String JSON_PROP_TIMELINES = "timelines";
	public static final String PRODUCT_TRANSFERS = "TRANSFERS";
	public static final String JSON_PROP_TRIPTYPE = "serviceType";
	public static final String JSON_PROP_TRANSFERSDETAILS = "transfersDetails";
	public static final String JSON_PROP_SERVICECHARGES = "ServiceCharges";
	public static final String JSON_PROP_RESERVATION = "Reservation";
	public static final String JSON_PROP_BOOKRS = "GroundBookRS";
	public static final String JSON_PROP_TRANSFERINFORMATION = "transferInformation";
	public static final String PRODUCT_NAME_BRMS = "Transfers";
	public static final String JSON_PROP_CURRENCYCODE = "currencyCode";
	public static final String JSON_PROP_ESTIMATEDAMOUNT = "estimatedTotalAmount";
	public static final String JSON_PROP_RATEAMOUNT = "rateTotalAmount";
	public static final String JSON_PROP_AMOUNT = "amount";
	public static final String JSON_PROP_SUPPPRICEINFO = "supplierPricingInfo";
	public static final String JSON_PROP_BOOKREFIDX ="bookRefIdx";
	public static final String JSON_PROP_PICKUPDATE =  "pickUpdateTime";
	public static final String JSON_PROP_PICKUPLOCCODE =  "pickUpLocationCode";
	public static final String JSON_PROP_DROPOFFDATE =  "dropOffdateTime";
	public static final String JSON_PROP_DROPOFFLOCCODE =  "dropOffLocationCode";
	//public static final String JSON_PROP_SERVICECHARGE = "ServiceCharges";
	public static final String JSON_PROP_DESCRIPTION = "description";
	public static final String JSON_PROP_ID = "id";
	public static final String JSON_PROP_SERVICETYPE = "serviceType";
	public static final String JSON_PROP_UNIQUEID = "uniqueID";
	public static final String JSON_PROP_FEES = "fees";
	public static final String JSON_PROP_TPAEXTENTION = "tpa_Extensions";
	
	public static final String PROD_CATEG_SUBTYPE_TRANSFER = "Transfer";
	public static final String JSON_PROP_TRANSFERSINFO = "TransfersInfo";
	public static final String JSON_PROP_CLIENTCOMMITININFO = "clientCommercialItinInfo";
	public static final String JSON_PROP_CLIENTCOMMTOTAL ="totalPricingInfo";
	public static final String JSON_PROP_TOTALFARE ="totalFare";
	public static final String JSON_PROP_SUPPTOTALFARE ="suppTotalFare";
	public static final String JSON_PROP_SUPPCOMMTOTAL = "supplierCommercialsTotals";
	public static final char KEYSEPARATOR = '|';
	public static final String JSON_PROP_VEHICLETYPE = "vehicleType";
}
