package com.coxandkings.travel.bookingengine.controller.acco;


import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoBookProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoModifyProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoPriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoRepriceProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoSearchAsyncProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.acco.AccoSearchProcessor;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
@RestController
@RequestMapping("/AccoService")
public class AccoController {
	
	public static final String PRODUCT = "ACCO";
	
	//@Autowired
	//AccoBookProcessor  bookservice;
	
	@GetMapping(value = "/ping",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> pingService(InputStream req) {
        return new ResponseEntity<String>("{\"operation\": \"ping\", \"status\": \"SUCCESS\"}", HttpStatus.OK);
    }
	
	@PostMapping(value = "/search",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAvailabilityAndPrice(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = AccoSearchProcessor.processV2(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	@PostMapping(value = "/asearch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> lowfareAsyncSearch(InputStream req) throws Exception {
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		
		// TODO: Validate request here. Validate for what?
		//String res = AccoSearchProcessor.process(reqJson);
		//AirConfig.getThreadPool().execute(new AccoSearchAsyncProcessor(reqJson));
		AirConfig.execute(new AccoSearchAsyncProcessor(reqJson));
		//return new ResponseEntity<String>(res, HttpStatus.OK);
		return new ResponseEntity<String>(HttpStatus.ACCEPTED);
	}
	
	@PostMapping(value = "/price",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
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
	
	
	@PostMapping(value = "/book",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> book(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = AccoBookProcessor.processV2(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
	/*@PostMapping(value = "/cancel",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancel(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = AccoCancelProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }*/

	
	@PostMapping(value = "/modify",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> modify(InputStream req) throws Exception{
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = AccoModifyProcessor.process(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }
	
}
