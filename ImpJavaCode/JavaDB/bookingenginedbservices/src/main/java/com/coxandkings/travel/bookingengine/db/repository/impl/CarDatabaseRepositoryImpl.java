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
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.repository.CarDatabaseRepository;


@Repository
@Qualifier("Car")
public class CarDatabaseRepositoryImpl extends SimpleJpaRepository<CarOrders, Serializable> implements CarDatabaseRepository {

	public  CarDatabaseRepositoryImpl(EntityManager em) {
        super(CarOrders.class, em);
        this.em = em;
    }

	private EntityManager em;
	  
	public CarOrders saveOrder(CarOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public List<CarOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarOrders> criteria = builder.createQuery(CarOrders.class);
		Root<CarOrders> root = criteria.from(CarOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
		
	}
	
	@Override
	public List<CarOrders> findBysuppID(String suppID) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarOrders> criteria = builder.createQuery(CarOrders.class);
		Root<CarOrders> root = criteria.from(CarOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}
	
}
