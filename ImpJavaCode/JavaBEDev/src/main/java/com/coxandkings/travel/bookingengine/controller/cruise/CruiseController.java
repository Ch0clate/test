package com.coxandkings.travel.bookingengine.controller.cruise;

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

import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruiseBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruiseCabinAvailProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruiseCancelProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruisePriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruiseRePriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.cruise.CruiseSearchProcessor;

@RestController
@RequestMapping("/CruiseService")
public class CruiseController {
	
//	@Autowired
//	private CruiseBookProcessor cruiseBookProcessor;
	public static final String PRODUCT = "CRUISE";
	
	@GetMapping(value="/ping", produces= MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pingService(InputStream req)
	{
		return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}",HttpStatus.OK);
	}
	
	@PostMapping(value="/search",produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> cruiseAvailability(InputStream req) throws Exception
	{
		JSONTokener jsonTokener = new JSONTokener(req);
		JSONObject jsonObject = new JSONObject(jsonTokener);
		String res = CruiseSearchProcessor.process(jsonObject);
		return new ResponseEntity<String>(res,HttpStatus.OK);
	}
	
	@PostMapping(value = "/price", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> categoryAvail(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = CruisePriceProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/cabinAvail", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> cabinAvail(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		System.out.println("Hellooo");
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = CruiseCabinAvailProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/rePricing", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> rePrice(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		System.out.println("Hellooo");
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = CruiseRePriceProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/book", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> book(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		System.out.println("Hellooo");
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = CruiseBookProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> cancel(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		System.out.println("Hellooo");
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = CruiseCancelProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
}
