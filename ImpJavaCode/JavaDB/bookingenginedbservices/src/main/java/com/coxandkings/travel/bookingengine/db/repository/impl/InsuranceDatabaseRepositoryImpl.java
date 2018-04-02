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
import com.coxandkings.travel.bookingengine.db.model.InsuranceOrders;
import com.coxandkings.travel.bookingengine.db.repository.InsuranceDatabaseRepository;

@Repository
@Qualifier("Insurance")
public class InsuranceDatabaseRepositoryImpl extends SimpleJpaRepository<InsuranceOrders, Serializable> implements InsuranceDatabaseRepository{

  public InsuranceDatabaseRepositoryImpl(EntityManager em) {
    super(InsuranceOrders.class, em);
    this.em = em;
  }
  
  private EntityManager em;

  @Override
  public InsuranceOrders saveOrder(InsuranceOrders currentOrder, String prevOrder) {
    return this.save(currentOrder);
  }

  @Override
  public List<InsuranceOrders> findByBooking(Booking booking) {
    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<InsuranceOrders> criteria = builder.createQuery(InsuranceOrders.class);
    Root<InsuranceOrders> root = criteria.from(InsuranceOrders.class);
    Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
    criteria.where(p1);
    return em.createQuery( criteria ).getResultList();
  }

  @Override
  public List<InsuranceOrders> findBysuppID(String suppID) {
    
    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<InsuranceOrders> criteria = builder.createQuery(InsuranceOrders.class);
    Root<InsuranceOrders> root = criteria.from(InsuranceOrders.class);
    Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
    criteria.where(p1);
    return em.createQuery( criteria ).getResultList();  
  }
  
  
}