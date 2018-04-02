package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.CarAmCl;


public interface CarAmClRepository extends JpaRepository<CarAmCl, Serializable> {

	public CarAmCl saveOrder(CarAmCl currentOrder, String prevOrder);
	public List<CarAmCl> findByEntity(String entityName, String entityID,String requestType );
	
	public List<CarAmCl> findforResponseUpdate(String entityName, String entityID,String requestType,String type );
}
