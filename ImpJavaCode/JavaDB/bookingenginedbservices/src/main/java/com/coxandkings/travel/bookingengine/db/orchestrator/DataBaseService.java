package com.coxandkings.travel.bookingengine.db.orchestrator;

import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;

public interface DataBaseService {
	
	public boolean isResponsibleFor(String product);
	
	//TODO: Check for the return types of these requests later
	public String processBookRequest(JSONObject reqJson) throws BookingEngineDBException;
	public String processBookResponse(JSONObject resJson) throws BookingEngineDBException;

}
