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
import com.coxandkings.travel.bookingengine.db.model.BusAmCl;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;

import com.coxandkings.travel.bookingengine.db.repository.BusDatabaseRepository;

@Qualifier("Bus")
@Repository
public class BusDatabaseRepositoryImpl extends SimpleJpaRepository<BusOrders, Serializable> implements BusDatabaseRepository{

	public  BusDatabaseRepositoryImpl(EntityManager em) {
        super(BusOrders.class, em);
        this.em = em;
    }
	private EntityManager em;
	
	
	public BusOrders saveOrder(BusOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public List<BusOrders> findByBooking(Booking booking) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<BusOrders> criteria = builder.createQuery(BusOrders.class);
		Root<BusOrders> root = criteria.from(BusOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
	
	}
	
	
	// TODO:temporary .. have to ask ????
	@Override
	public List<BusAmCl> findByEntityId(String entityId) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<BusAmCl> criteria = builder.createQuery(BusAmCl.class);
		Root<BusAmCl> root = criteria.from(BusAmCl.class);
		Predicate p1 = builder.and(builder.equal(root.get("entityId"), entityId));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
		
		
	}
	
	
	
}
