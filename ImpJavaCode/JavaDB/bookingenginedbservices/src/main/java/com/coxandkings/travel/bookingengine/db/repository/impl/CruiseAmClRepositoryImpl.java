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

import com.coxandkings.travel.bookingengine.db.model.CruiseAmCl;
import com.coxandkings.travel.bookingengine.db.repository.CruiseAmClRepository;

@Qualifier("CruiseAmCl")
@Repository
public class CruiseAmClRepositoryImpl extends SimpleJpaRepository<CruiseAmCl, Serializable> implements CruiseAmClRepository {

	public CruiseAmClRepositoryImpl(EntityManager em) {
        super(CruiseAmCl.class, em);
        this.em = em;
    }

	private EntityManager em;
	
	@Override
	public CruiseAmCl saveOrder(CruiseAmCl currentOrder, String prevOrder) {
		// TODO Auto-generated method stub
		return this.save(currentOrder);
	}

	@Override
	public List<CruiseAmCl> findByEntity(String entityName, String entityID, String requestType) {
		// TODO Auto-generated method stub
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CruiseAmCl> criteria = builder.createQuery(CruiseAmCl.class);
		Root<CruiseAmCl> root = criteria.from(CruiseAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityID)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
	}

	@Override
	public List<CruiseAmCl> findforResponseUpdate(String entityName, String entityID, String requestType, String type) {
		// TODO Auto-generated method stub
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CruiseAmCl> criteria = builder.createQuery(CruiseAmCl.class);
		Root<CruiseAmCl> root = criteria.from(CruiseAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
//		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityID)));
//		predicate.add((builder.equal(root.get("requestType"), requestType)));
//		predicate.add((builder.equal(root.get("description"), type)));
//		predicate.add((builder.equal(root.get("supplierCharges"), "0")));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
	}
	
}
