package com.coxandkings.travel.bookingengine.orchestrator.rail;

import java.text.SimpleDateFormat;

import com.coxandkings.travel.bookingengine.utils.Constants;

public interface RailConstants extends Constants{
	
	public static final String PRODUCT_RAIL = "RAIL";
	String[] weekDays= {"Mon","Tue","Weds","Thur","Fri","Sat","Sun"};
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00");
	
	public static final String PROD_CAT_SUBTYPE="Rail";
	public static final String JSON_PROP_ORIGINDESTOPTS="originDestinationOptions";
	public static final String JSON_PROP_ORIGINLOC="originLocation";
	public static final String JSON_PROP_DESTLOC="destinationLocation";
	public static final String JSON_PROP_TRAVELDATE="travelDate";
	public static final String JSON_PROP_CLASSAVAIL="classAvailInfo";
	public static final String JSON_PROP_TRAINDETAILS="trainDetails";
	public static final String JSON_PROP_RESERVATIONCLASS="reservationClass";
	public static final String JSON_PROP_RESERVATIONTYPE="reservationType";
	public static final String JSON_PROP_PRICING="pricing";
	public static final String JSON_PROP_AVAILABILITYINFO="availabilityDetail";
	public static final String JSON_PROP_AVAILDATE="availablityDate";
	public static final String JSON_PROP_AVAILSTATUS="availablityStatus";
	public static final String JSON_PROP_AVAILTYPE="availablityType";
	public static final String JSON_PROP_AMOUNT="amount";
	public static final String JSON_PROP_CURRENCYCODE="currencyCode";
	public static final String JSON_PROP_TOTAL="totalFare";
	public static final String JSON_PROP_FAREBREAKUP="fareBreakup";
	public static final String JSON_PROP_BASE="baseFare";
	public static final String JSON_PROP_DEPARTTIME="departureTime";
	public static final String JSON_PROP_ARRIVALTIME="arrivalTime";
	public static final String JSON_PROP_JOURNEYDURATION="journeyDuration";
	public static final String JSON_PROP_OPERATIONSCHEDULE="operationSchedule";
	public static final String JSON_PROP_TRAININFO="trainInfo";
	public static final String JSON_PROP_TRAINNUM="trainNumber";
	public static final String JSON_PROP_TRAINNAME="trainName";
	public static final String JSON_PROP_TRAINTYPE="trainType";
	public static final String JSON_PROP_CODECONTEXT="codeContext";
	public static final String JSON_PROP_ANCILLARY="ancillaryCharges";
	public static final String JSON_PROP_FEES="fees";
	public static final String JSON_PROP_TAXES="taxes";
	public static final String JSON_PROP_PAXPREF="passengerPreferences";
	public static final String JSON_PROP_APPBERTH="applicableBerth";
	public static final String JSON_PROP_BOARDINGSTN="boardingStation";
	public static final String JSON_PROP_STATIONCODE="stationCode";
	public static final String JSON_PROP_STATIONNAME="stationName";
	public static final String JSON_PROP_JOURNEYDIST="distance";
	public static final String JSON_PROP_HALTTIME="haltTime";
	public static final String JSON_PROP_DAYCOUNT="dayCount";
	public static final String JSON_PROP_ROUTENUM="route";
	public static final String JSON_PROP_JOURNEYDET="journeyDetails";
	public static final String JSON_PROP_ITINERARY="itinerary";
	
	public static final String JSON_PROP_OPERATIONNAME="operationName";
	public static final String JSON_PROP_BRMSHEADER="header";
	public static final String JSON_PROP_COMMONELEMS="commonElements";
	public static final String JSON_PROP_ADVANCEDEF="advancedDefinition";
	public static final String NS_RAIL="http://www.coxandkings.com/integ/suppl/rail";
	
	
}
