package com.coxandkings.travel.bookingengine.db.controller;

import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.coxandkings.travel.bookingengine.db.orchestrator.TransfersBookingServiceImpl;

@RestController
@RequestMapping("/TransfersService")
public class TransfersController {
	
	@Autowired
	private TransfersBookingServiceImpl transfersService;
	
	@PutMapping(value="/update/{updateType}",produces=MediaType.APPLICATION_JSON_VALUE,consumes=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> updateDetails(InputStream req,  @PathVariable("updateType") String updateType){
		
		JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
		String res =  transfersService.updateOrder(reqJson,updateType);
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	@GetMapping(value="/getTransOrders/",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getTransfersOrdersBySuppID(@RequestParam String suppID){
		String res =  transfersService.getBysuppID(suppID);
		return new ResponseEntity<String>(res, HttpStatus.OK);	
		
	}
	
	
}
