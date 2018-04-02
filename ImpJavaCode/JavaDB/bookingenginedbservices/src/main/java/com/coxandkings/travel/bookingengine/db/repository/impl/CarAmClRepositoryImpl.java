package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.CarAmCl;
import com.coxandkings.travel.bookingengine.db.repository.CarAmClRepository;

@Qualifier("CarAmCl")
@Repository
public class CarAmClRepositoryImpl extends SimpleJpaRepository<CarAmCl, Serializable> implements CarAmClRepository {

	
	public CarAmClRepositoryImpl(EntityManager em) {
        super(CarAmCl.class, em);
        this.em = em;
    }
	
	
	private EntityManager em;
 
	public CarAmCl saveOrder(CarAmCl currentOrder, String prevOrder) {
		return this.save(currentOrder);
		
	}
	

	@Override
	public List<CarAmCl> findByEntity(String entityName, String entityID, String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarAmCl> criteria = builder.createQuery(CarAmCl.class);
		Root<CarAmCl> root = criteria.from(CarAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityID)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
		
	
	}

	@Override
	public List<CarAmCl> findforResponseUpdate(String entityName, String entityID,String type , String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarAmCl> criteria = builder.createQuery(CarAmCl.class);
		Root<CarAmCl> root = criteria.from(CarAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityID)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		predicate.add((builder.equal(root.get("description"), type)));
		predicate.add((builder.equal(root.get("supplierCharges"), "0")));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
	}

}
