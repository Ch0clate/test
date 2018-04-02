package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.Booking;

public interface BookingDatabaseRepository extends JpaRepository<Booking, Serializable> {

	public Booking saveOrder(Booking orderObj, String prevBooking);
	
	public List<Booking> findByUserID(String userID);

	public List<Booking> findByStatus(String status);

}
