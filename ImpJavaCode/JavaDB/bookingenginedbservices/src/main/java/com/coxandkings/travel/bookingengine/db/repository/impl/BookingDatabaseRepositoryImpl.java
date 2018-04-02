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
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;

@Qualifier("Booking")
@Repository
public class BookingDatabaseRepositoryImpl extends SimpleJpaRepository<Booking, Serializable> implements BookingDatabaseRepository  {

	public BookingDatabaseRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }

	private EntityManager em;
	  
	public Booking saveOrder(Booking orderObj, String prevOrder) {
		return this.save(orderObj);
	
	}
	
	@Override
	public List<Booking> findByUserID(String userID) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Booking> criteria = builder.createQuery(Booking.class);
		Root<Booking> root = criteria.from(Booking.class);
		Predicate p1 = builder.and(builder.equal(root.get("userID"), userID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}
	
	@Override
	public List<Booking> findByStatus(String status) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Booking> criteria = builder.createQuery(Booking.class);
		Root<Booking> root = criteria.from(Booking.class);
		Predicate p1 = builder.and(builder.equal(root.get("status"), status));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}
	
	
	
}
