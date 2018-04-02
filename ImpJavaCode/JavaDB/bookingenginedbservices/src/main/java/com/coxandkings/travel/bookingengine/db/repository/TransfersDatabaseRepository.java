package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;

public interface TransfersDatabaseRepository extends JpaRepository<TransfersOrders, Serializable> {
	
    public TransfersOrders saveOrder(TransfersOrders currentOrder, String prevOrder);
	
	public List<TransfersOrders> findByBooking(Booking booking);

	public List<TransfersOrders> findBysuppID(String suppID);

	/*public void save(CarOrders order);*/
	

}
