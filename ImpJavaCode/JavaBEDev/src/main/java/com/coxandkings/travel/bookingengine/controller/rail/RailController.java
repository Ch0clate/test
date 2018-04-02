package com.coxandkings.travel.bookingengine.controller.rail;

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

import com.coxandkings.travel.bookingengine.orchestrator.rail.RailGetDetailsProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.rail.RailRepriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.rail.RailSearchProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.rail.TrainScheduleProcessor;

@RestController
@RequestMapping("/RailService")
public class RailController {
	
	//public static final String PRODUCT = "ACCO";
	
	/*@Autowired
	AccoBookProcessor  bookservice;
	*/

	@GetMapping(value = "/ping",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> pingService(InputStream req) {
        return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}", HttpStatus.OK);
    }
	
	@PostMapping(value = "/search",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAvailabilityAndPrice(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = RailSearchProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/reprice",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> rePrice(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = RailRepriceProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/trainSchedule",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> trainSchedule(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = TrainScheduleProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/getDetails",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDetails(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = RailGetDetailsProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
		
	/*@PostMapping(value = "/price",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDetails(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = AccoPriceProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/reprice", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> rePrice(InputStream req) {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res = AccoRepriceProcessor.process(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	*/
	
	
/*	@PostMapping(value = "/book",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> book(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = bookservice.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	*/
	
}
