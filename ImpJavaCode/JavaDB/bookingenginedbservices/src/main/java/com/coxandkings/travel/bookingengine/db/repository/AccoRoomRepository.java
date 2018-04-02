package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;

public interface AccoRoomRepository extends JpaRepository<AccoRoomDetails, Serializable> {

	public AccoRoomDetails saveOrder(AccoRoomDetails orderObj,String prevRoomDetails);
	
	
}
