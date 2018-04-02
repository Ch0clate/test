package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;

public interface CruiseDatabaseRepository extends JpaRepository<CruiseOrders, Serializable> {

	public CruiseOrders saveOrder(CruiseOrders orderObj, String prevOrder);

	public List<CruiseOrders> findByBooking(Booking booking);
	
	public List<CruiseOrders> findBysuppID(String suppID);
	
}
