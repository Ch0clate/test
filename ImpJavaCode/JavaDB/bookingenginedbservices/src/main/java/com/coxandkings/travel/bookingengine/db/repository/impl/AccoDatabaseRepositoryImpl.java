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

import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.AccoDatabaseRepository;

@Qualifier("Acco")
@Repository
public class AccoDatabaseRepositoryImpl extends SimpleJpaRepository<AccoOrders, Serializable> implements AccoDatabaseRepository {

	
	public AccoDatabaseRepositoryImpl(EntityManager em) {
        super(AccoOrders.class, em);
        this.em = em;
    }
	
	
	private EntityManager em;
	
	
	  
	public AccoOrders saveOrder(AccoOrders currentOrder, String prevOrder) {
		return this.save(currentOrder);
		
	}

	@Override
	public List<AccoOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AccoOrders> criteria = builder.createQuery(AccoOrders.class);
		Root<AccoOrders> root = criteria.from(AccoOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
		
	}

	@Override
	public List<AccoOrders> findBysuppID(String suppID) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AccoOrders> criteria = builder.createQuery(AccoOrders.class);
		Root<AccoOrders> root = criteria.from(AccoOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}
	
}
