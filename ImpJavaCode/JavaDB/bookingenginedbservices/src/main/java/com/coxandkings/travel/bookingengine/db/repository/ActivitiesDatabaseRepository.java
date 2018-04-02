package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;

public interface ActivitiesDatabaseRepository extends JpaRepository<ActivitiesOrders, Serializable> {

	public ActivitiesOrders saveOrder(ActivitiesOrders orderObj);
	
	public List<ActivitiesOrders> findByBooking(Booking booking);
}
