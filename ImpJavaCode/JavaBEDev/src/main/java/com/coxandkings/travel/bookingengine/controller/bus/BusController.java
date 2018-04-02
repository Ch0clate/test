package com.coxandkings.travel.bookingengine.controller.bus;

import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.coxandkings.travel.bookingengine.orchestrator.bus.BusBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.bus.BusCancelTicketProcessor;

import com.coxandkings.travel.bookingengine.orchestrator.bus.BusRetrieveBookingProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.bus.BusSearchProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.bus.BusSeatBlockingProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.bus.SeatMapProcessor;

@RestController
@RequestMapping("/BusService")
public class BusController {

	@Autowired
//	private BusSearchProcessor busbookprocessor;
	public static final String PRODUCT = "BUS";
	
	
	@GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pingService(InputStream req) {
		return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}", HttpStatus.OK);
	}

	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> lowfareSearch(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = BusSearchProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/SeatMap", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getLayout(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = SeatMapProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	
	
	@PostMapping(value = "/SeatBlocking", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> blockTicket(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = BusSeatBlockingProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/CreateBooking", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> bookTicket(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = BusBookProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/RetriveBooking", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> GetTicket(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = BusRetrieveBookingProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	
	
	@PostMapping(value = "/CancelBooking", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> CancelTicket(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = BusCancelTicketProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	
	 
}
