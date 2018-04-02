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
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;


@Repository
@Qualifier("Air")
public class AirDatabaseRepositoryImpl extends SimpleJpaRepository<AirOrders, Serializable> implements AirDatabaseRepository {

	public  AirDatabaseRepositoryImpl(EntityManager em) {
        super(AirOrders.class, em);
        this.em = em;
    }

	private EntityManager em;
	  
	public AirOrders saveOrder(AirOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public List<AirOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AirOrders> criteria = builder.createQuery(AirOrders.class);
		Root<AirOrders> root = criteria.from(AirOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
		
	}
	
	@Override
	public List<AirOrders> findBysuppID(String suppID) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AirOrders> criteria = builder.createQuery(AirOrders.class);
		Root<AirOrders> root = criteria.from(AirOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}
	
}
