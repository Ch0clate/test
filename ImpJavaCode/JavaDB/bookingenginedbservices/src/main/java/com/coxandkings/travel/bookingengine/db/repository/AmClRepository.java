package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.AmCl;

public interface AmClRepository extends JpaRepository<AmCl, Serializable> {

	public AmCl saveOrder(AmCl currentOrder, String prevOrder);
	public List<AmCl> findByEntity(String entityName, String entityID,String requestType );
	
	public List<AmCl> findforResponseUpdate(String entityName, String entityID,String requestType,String type );
	
}
