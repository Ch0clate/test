package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


import com.coxandkings.travel.bookingengine.db.model.BusAmCl;

public interface BusAmclRepository extends JpaRepository<BusAmCl, Serializable>{
	
	public BusAmCl saveOrder(BusAmCl currentOrder, String prevOrder);
	
	public List<BusAmCl> findforResponseUpdate(String bookID, String cancelId,String requestType,String cancelType );
}
