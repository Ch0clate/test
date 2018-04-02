package com.coxandkings.travel.bookingengine.controller.holidays;

import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.coxandkings.travel.bookingengine.orchestrator.holidays.HolidaysAddServiceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.holidays.HolidaysBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.holidays.HolidaysGetPackageDetailsProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.holidays.HolidaysRepriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.holidays.HolidaysRetrieveProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.holidays.HolidaysSearchProcessor;

@RestController
@RequestMapping("/HolidaysService")
public class HolidaysController {


	@GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pingService(InputStream req) {
		return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}", HttpStatus.OK);
	}

	@PostMapping(value = "/searchByTour", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getTour(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = HolidaysSearchProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/getPackageDetails", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getPackageDetails(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = HolidaysGetPackageDetailsProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	@PostMapping(value = "/addservice", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAddService(InputStream req) throws Exception {
        System.out.println("Entered the controller");
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = HolidaysAddServiceProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/reprice", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getReprice(InputStream req) throws Exception {
        System.out.println("Entered the controller");
	    JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = HolidaysRepriceProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/book", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBook(InputStream req) throws Exception {
        System.out.println("Entered the controller");
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = HolidaysBookProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/retrieve", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getRetrieveBooking(InputStream req) throws Exception {
        System.out.println("Entered the controller");
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = HolidaysRetrieveProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
}