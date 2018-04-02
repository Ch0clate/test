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

import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.repository.TransfersDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.model.Booking;

@Qualifier("Transfers")
@Repository
public class TransfersDatabaseRepositoryImpl extends SimpleJpaRepository<TransfersOrders, Serializable> implements TransfersDatabaseRepository {

	public TransfersDatabaseRepositoryImpl(EntityManager em) {
    super(TransfersOrders.class, em);
    this.em = em;
}

	private EntityManager em;

	public TransfersOrders saveOrder(TransfersOrders currentOrder, String prevOrder) {
		return this.save(currentOrder);

	}

	@Override
	public List<TransfersOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TransfersOrders> criteria = builder.createQuery(TransfersOrders.class);
		Root<TransfersOrders> root = criteria.from(TransfersOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		return em.createQuery(criteria).getResultList();

	}

	@Override
	public List<TransfersOrders> findBysuppID(String suppID) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TransfersOrders> criteria = builder.createQuery(TransfersOrders.class);
		Root<TransfersOrders> root = criteria.from(TransfersOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery(criteria).getResultList();
	}
}
