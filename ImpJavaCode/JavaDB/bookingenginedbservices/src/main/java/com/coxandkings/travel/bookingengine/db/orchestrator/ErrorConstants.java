package com.coxandkings.travel.bookingengine.db.orchestrator;

public interface ErrorConstants {

	public static final String BE_ERR_ACCO_001 = "No such Acco PaxID found";
	public static final String BE_ERR_ACCO_002 = "No such roomID found";
	public static final String BE_ERR_004 = "No such orderID found";
	public static final String BE_ERR_ACCO_004 = "No such bookID found";
	public static final String BE_ERR_ACCO_005 = "No Acco orders for bookID found";
	public static final String BE_ERR_ACCO_006 = "No Acco orders found for requested supplier";
	
	public static final String BE_ERR_AIR_006 = "No Air orders found for requested supplier";
	public static final String BE_ERR_AIR_001 = "No such Air PaxID found";
	
	public static final String BE_ERR_000  = "no match for update type";
	public static final String BE_ERR_001 = "No such bookID found";
	public static final String BE_ERR_002 = "No such userID found";
	public static final String BE_ERR_003 = "No booking details found with the given status";
	
	//Packages Error Constants
	public static final String BE_ERR_HOLIDAYS_001 = "No such Holidays PaxID found";
    public static final String BE_ERR_HOLIDAYS_002 = "No such roomID found";
    public static final String BE_ERR_HOLIDAYS_004 = "No such bookID found";
    public static final String BE_ERR_HOLIDAYS_005 = "No Holiday orders for bookID found";
    public static final String BE_ERR_HOLIDAYS_006 = "No Holiday orders found for requested supplier";

    //Car Error Constants
    
    public static final String BE_ERR_CAR_001 = "No such Car PaxID found";
    public static final String BE_ERR_CAR_004 = "No such bookID found";
    public static final String BE_ERR_CAR_005 = "No Car orders for bookID found";
    public static final String BE_ERR_CAR_006 = "No Car orders found for requested supplier";
}
