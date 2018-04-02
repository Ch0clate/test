package com.coxandkings.travel.bookingengine.db.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class BookingDBConfigDataNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LogManager.getLogger(BookingDBConfigDataNotFoundException.class);
	
	public BookingDBConfigDataNotFoundException() {
		
		logger.error("There is a problem in loading the booking Db configuration");
		
	}
	
	

}
