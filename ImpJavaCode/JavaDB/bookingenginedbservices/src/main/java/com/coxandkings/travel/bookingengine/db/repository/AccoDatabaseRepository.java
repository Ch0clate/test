package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;

public interface AccoDatabaseRepository extends JpaRepository<AccoOrders, Serializable> {

	public AccoOrders saveOrder(AccoOrders currentOrder, String prevOrder);
	
	public List<AccoOrders> findByBooking(Booking booking);

	public List<AccoOrders> findBysuppID(String suppID);
	
}
