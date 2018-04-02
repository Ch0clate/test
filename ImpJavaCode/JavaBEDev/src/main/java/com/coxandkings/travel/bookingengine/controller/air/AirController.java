package com.coxandkings.travel.bookingengine.controller.air;

import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirAmendProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirCancelProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirPriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirRepriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirSearchAsyncProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirSearchProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.air.AirSsrProcessor;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/AirService")
public class AirController {

	//@Autowired
	//private AirBookProcessor airbookprocessor;
	//public static final String PRODUCT = "AIR";

	@GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pingService(InputStream req) {
		return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}", HttpStatus.OK);
	}

	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> lowfareSearch(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		//String res = AirSearchProcessor.process(reqJson);
		String res = AirSearchAsyncProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PostMapping(value = "/asearch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> lowfareAsyncSearch(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		
		// TODO: Validate request here. Validate for what?
		//String res = AirSearchProcessor.process(reqJson);
		//AirConfig.getThreadPool().execute(new AirSearchAsyncProcessor(reqJson));
		AirConfig.execute(new AirSearchAsyncProcessor(reqJson));
		//return new ResponseEntity<String>(res, HttpStatus.OK);
		return new ResponseEntity<String>(HttpStatus.ACCEPTED);
	}
	
	@PostMapping(value = "/price", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> priceVerify(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = AirPriceProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	@PostMapping(value = "/reprice", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> rePrice(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = AirRepriceProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PostMapping(value = "/book", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> bookVerify(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		//String res = airbookprocessor.process(reqJson);
		String res = AirBookProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PostMapping(value = "/getssr", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getSsr(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = AirSsrProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> cancel(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = AirCancelProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PostMapping(value = "/amend", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> amend(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = AirAmendProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
}
