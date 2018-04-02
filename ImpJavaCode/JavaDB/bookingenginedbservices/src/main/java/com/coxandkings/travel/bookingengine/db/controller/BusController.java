package com.coxandkings.travel.bookingengine.db.controller;

import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coxandkings.travel.bookingengine.db.orchestrator.BusBookingServiceImpl;

@RestController
@RequestMapping("/BusService")
public class BusController {

	@Autowired
	private BusBookingServiceImpl busService;
	
//	@PutMapping(value="/update/{updateType}",produces=MediaType.APPLICATION_JSON_VALUE,consumes=MediaType.APPLICATION_JSON_VALUE)
//	public  ResponseEntity<String> updateDetails(InputStream req,  @PathVariable("updateType") String updateType){
//		
//		JSONTokener jsonTok = new JSONTokener(req);
//        JSONObject reqJson = new JSONObject(jsonTok);
//		String res =  busService.updateOrder(reqJson,updateType);
//		return new ResponseEntity<String>(res, HttpStatus.OK);	
//	}
}
