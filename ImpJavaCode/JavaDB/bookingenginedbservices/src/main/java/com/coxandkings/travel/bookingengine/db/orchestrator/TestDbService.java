package com.coxandkings.travel.bookingengine.db.orchestrator;

import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;

public interface TestDbService {
	
	
	//TODO: Check for the return types of these requests later
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException;
	public String processAmClResponse(JSONObject reqJson) throws BookingEngineDBException;
	public String processBookResponse(JSONObject resJson) throws BookingEngineDBException;

}
