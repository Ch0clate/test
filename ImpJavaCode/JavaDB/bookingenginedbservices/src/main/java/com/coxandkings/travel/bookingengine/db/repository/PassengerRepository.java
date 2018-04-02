package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;

public interface PassengerRepository extends JpaRepository<PassengerDetails, Serializable> {

	public PassengerDetails saveOrder(PassengerDetails orderObj, String prevOrder);
	
	
}
