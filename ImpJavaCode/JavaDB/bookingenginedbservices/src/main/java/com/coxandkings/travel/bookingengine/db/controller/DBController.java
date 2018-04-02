package com.coxandkings.travel.bookingengine.db.controller;

import java.io.InputStream;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaBootstrapConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coxandkings.travel.bookingengine.db.kafka.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.db.orchestrator.AccoDataBaseServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.DataBaseService;
import com.coxandkings.travel.bookingengine.db.orchestrator.TestDbService;



@RestController
@RequestMapping("/DBService")
public class DBController {
	
	@Autowired
	private List<DataBaseService> services;
	
	@Autowired
	private BookingServiceImpl bookingService;
	
	@Autowired
	@Qualifier("Kafka")
	private KafkaBookProducer kafkaService;
	
	//TODO: this is just for testing purpose
	@Autowired
	@Qualifier("Acco")
	private TestDbService accoService;
	
	@Autowired
	@Qualifier("Air")
	private TestDbService airService;
	
	@Autowired
	@Qualifier("Car")
	private TestDbService carService;
	
	
	private DataBaseService serviceForProduct(String product) {
        for(DataBaseService service : services) {
             if(service.isResponsibleFor(product)) {
                  return service;
             }
        }

        throw new UnsupportedOperationException("unsupported ProductType");
   }
	
	@PostMapping(value = "/dbUpdate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> bookInsert(InputStream req) throws JSONException, Exception {
		
		
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res,productRes;
		if(reqJson.has("requestBody")) { 
		 res = serviceForProduct(reqJson.getJSONObject("requestBody").getString("product")).processBookRequest(reqJson);
		 return new ResponseEntity<String>(res, HttpStatus.CREATED);
		}
		else {
		res = "FAILED";
		productRes = serviceForProduct(reqJson.getJSONObject("responseBody").getString("product")).processBookResponse(reqJson);
		if(productRes.equalsIgnoreCase("SUCCESS"))
		res =  serviceForProduct("BOOKING").processBookResponse(reqJson);
		//TODO: Confirm where to put this kafka logic
		kafkaService.runProducer(1,new JSONObject( bookingService.getByBookID(reqJson.getJSONObject("responseBody").getString("bookID"),"false")));
		
		return new ResponseEntity<String>(res, HttpStatus.OK);
		
		}
	}
	
	@PostMapping(value = "/amendDBUpdate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateCancelAmend(InputStream req) throws JSONException, Exception {
		
		
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res ;
		if(reqJson.has("requestBody")) 
		 res = accoService.processAmClRequest(reqJson);
		else
		 res = accoService.processAmClResponse(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PostMapping(value = "/amendDBUpdateAir", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateCancelAmendAir(InputStream req) throws JSONException, Exception {
		
		
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res ;
		if(reqJson.has("requestBody")) 
		 res = airService.processAmClRequest(reqJson);
		else
		 res = airService.processAmClResponse(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PostMapping(value = "/amendDBUpdateCar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateCancelAmendCar(InputStream req) throws JSONException, Exception {
		
		
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		String res ;
		if(reqJson.has("requestBody")) 
		 res = carService.processAmClRequest(reqJson);
		else
		 res = carService.processAmClResponse(reqJson);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	
}
