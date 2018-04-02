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
import com.coxandkings.travel.bookingengine.db.model.HolidaysOrders;
import com.coxandkings.travel.bookingengine.db.repository.HolidaysDatabaseRepository;



@Repository
@Qualifier("Holidays")
public class HolidaysDatabaseRepositoryImpl extends SimpleJpaRepository<HolidaysOrders, Serializable> implements HolidaysDatabaseRepository{

  public HolidaysDatabaseRepositoryImpl(EntityManager em) {
    super(HolidaysOrders.class, em);
    this.em = em;
  }
  
  private EntityManager em;

  @Override
  public HolidaysOrders saveOrder(HolidaysOrders currentOrder, String prevOrder) {
    return this.save(currentOrder);
  }

  @Override
  public List<HolidaysOrders> findByBooking(Booking booking) {
    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<HolidaysOrders> criteria = builder.createQuery(HolidaysOrders.class);
    Root<HolidaysOrders> root = criteria.from(HolidaysOrders.class);
    Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
    criteria.where(p1);
    return em.createQuery( criteria ).getResultList();
  }

  @Override
  public List<HolidaysOrders> findBysuppID(String suppID) {
    
    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<HolidaysOrders> criteria = builder.createQuery(HolidaysOrders.class);
    Root<HolidaysOrders> root = criteria.from(HolidaysOrders.class);
    Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
    criteria.where(p1);
    return em.createQuery( criteria ).getResultList();  
  }
  
  
}
