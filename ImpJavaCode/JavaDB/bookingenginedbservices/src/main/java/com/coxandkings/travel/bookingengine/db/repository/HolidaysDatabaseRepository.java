package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.HolidaysOrders;



public interface HolidaysDatabaseRepository extends JpaRepository<HolidaysOrders, Serializable>{

  public HolidaysOrders saveOrder(HolidaysOrders currentOrder, String prevOrder);
  
  public List<HolidaysOrders> findByBooking(Booking booking);

  public List<HolidaysOrders> findBysuppID(String suppID);
  
	
}
