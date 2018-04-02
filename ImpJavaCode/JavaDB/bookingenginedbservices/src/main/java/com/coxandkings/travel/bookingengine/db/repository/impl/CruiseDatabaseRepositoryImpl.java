package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;
import com.coxandkings.travel.bookingengine.db.repository.CruiseDatabaseRepository;

@Repository
@Qualifier("Cruise")
public class CruiseDatabaseRepositoryImpl extends SimpleJpaRepository<CruiseOrders, Serializable> implements CruiseDatabaseRepository {

	public  CruiseDatabaseRepositoryImpl(EntityManager em) {
        super(CruiseOrders.class, em);
        this.em = em;
    }
	
	private EntityManager em;
	  
	public CruiseOrders saveOrder(CruiseOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public List<CruiseOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CruiseOrders> criteria = builder.createQuery(CruiseOrders.class);
		Root<CruiseOrders> root = criteria.from(CruiseOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
		
	}
	
	@Override
	public List<CruiseOrders> findBysuppID(String suppID) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CruiseOrders> criteria = builder.createQuery(CruiseOrders.class);
		Root<CruiseOrders> root = criteria.from(CruiseOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}
}
