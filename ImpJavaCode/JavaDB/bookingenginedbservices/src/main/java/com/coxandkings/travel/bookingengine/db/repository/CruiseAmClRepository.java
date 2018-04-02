package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.CruiseAmCl;

public interface CruiseAmClRepository extends JpaRepository<CruiseAmCl, Serializable> {

	public CruiseAmCl saveOrder(CruiseAmCl currentOrder, String prevOrder);
	public List<CruiseAmCl> findByEntity(String entityName, String entityID, String requestType);
	public List<CruiseAmCl> findforResponseUpdate(String entityName, String entityID, String requestType, String type);
}
