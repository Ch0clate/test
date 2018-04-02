package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.InsuranceOrders;

public interface InsuranceDatabaseRepository extends JpaRepository<InsuranceOrders, Serializable>{

  public InsuranceOrders saveOrder(InsuranceOrders currentOrder, String prevOrder);
  
  public List<InsuranceOrders> findByBooking(Booking booking);

  public List<InsuranceOrders> findBysuppID(String suppID);
  
    
}