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

import com.coxandkings.travel.bookingengine.db.orchestrator.BookingServiceImpl;


@RestController
@RequestMapping("/BookingService")
public class BookingController {
	
	@Autowired
	private BookingServiceImpl service;
	
	

	
	@GetMapping(value="/getBooking/{bookID}",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getBookingByBookID( @PathVariable("bookID") String bookID){
		String res =  service.getByBookID(bookID,"false");
		
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	
	@GetMapping(value="/getBookingID/{bookID}",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getNewBookingByBookingID( @PathVariable("bookID") String bookID){
		String res =  service.getByBookID(bookID,"true");
		
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	@GetMapping(value="/getAmendments/{bookID}",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getAmendMentsByBookID( @PathVariable("bookID") String bookID){
		String res =  service.getAmendmentsByBookID(bookID);
		
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	@GetMapping(value="/getCancellations/{bookID}",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getCancellationsByBookID( @PathVariable("bookID") String bookID )
	{
		String res =  service.getCancellationsByBookID(bookID);
		
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	@GetMapping(value="/getBookings/",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getBookingByUserID(@RequestParam String userID){
		String res =  service.getByUserID(userID);
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	@GetMapping(value="/getBookings",produces=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> getBookingByStatus( @RequestParam String status){
		String res =  service.getByStatus(status);
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	

	@PutMapping(value="/update/{updateType}",produces=MediaType.APPLICATION_JSON_VALUE,consumes=MediaType.APPLICATION_JSON_VALUE)
	public  ResponseEntity<String> updateDetails(InputStream req,  @PathVariable("updateType") String updateType){
		
		JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
		String res =  service.updateOrder(reqJson,updateType);
		return new ResponseEntity<String>(res, HttpStatus.OK);	
	}
	
	
}
