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

import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;

@Qualifier("AccoAmCl")
@Repository
public class AmClRepositoryImpl extends SimpleJpaRepository<AmCl, Serializable> implements AmClRepository {

	
	public AmClRepositoryImpl(EntityManager em) {
        super(AmCl.class, em);
        this.em = em;
    }
	
	
	private EntityManager em;
 
	public AmCl saveOrder(AmCl currentOrder, String prevOrder) {
		return this.save(currentOrder);
		
	}

	@Override
	public List<AmCl> findByEntity(String entityName, String orderId, String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AmCl> criteria = builder.createQuery(AmCl.class);
		Root<AmCl> root = criteria.from(AmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("orderID"), orderId)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
		
	
	}

	@Override
	public List<AmCl> findforResponseUpdate(String entityName, String entityID,String type , String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AmCl> criteria = builder.createQuery(AmCl.class);
		Root<AmCl> root = criteria.from(AmCl.class);
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
