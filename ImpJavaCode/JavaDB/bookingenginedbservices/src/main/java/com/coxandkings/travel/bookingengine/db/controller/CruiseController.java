package com.coxandkings.travel.bookingengine.db.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coxandkings.travel.bookingengine.db.orchestrator.AirBookingServiceImpl;

@RestController
@RequestMapping("/cruiseService")
public class CruiseController {

	/*@Autowired
	private AirBookingServiceImpl airService;*/
	
	/*@GetMapping(value="/getCruiseOrders/",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getAirOrdersBySuppID(@RequestParam String suppID){
		String res =  airService.getBysuppID(suppID);
		return new ResponseEntity<String>(res, HttpStatus.OK);	
		
	}*/
	
}
