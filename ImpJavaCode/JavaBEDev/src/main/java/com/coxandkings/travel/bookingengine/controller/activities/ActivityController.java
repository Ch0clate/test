package com.coxandkings.travel.bookingengine.controller.activities;

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

import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivityAmendProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivityBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivityCancelProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivityPriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivityRepriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivityRetrieveProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.activities.ActivitySearchProcessor;

@RestController
@RequestMapping("/ActivityService")
public class ActivityController {
	
	@Autowired
	private ActivityBookProcessor activityBookProcessor;
	
	@GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pingService(InputStream req) {
		return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}", HttpStatus.OK);
	}

	
	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> lowfareSearch(InputStream   req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = ActivitySearchProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/price",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDetails(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = ActivityPriceProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	

	@PostMapping(value = "/reprice", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> rePrice(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = ActivityRepriceProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/book", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> book(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = activityBookProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> cancel(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = ActivityCancelProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/retrieve", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> retrieve(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = ActivityRetrieveProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/amend", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> amend(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = ActivityAmendProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	

}
