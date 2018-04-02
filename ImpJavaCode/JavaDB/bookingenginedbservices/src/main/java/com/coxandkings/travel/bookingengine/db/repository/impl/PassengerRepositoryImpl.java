package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;

@Repository
@Qualifier("Passenger")
public class PassengerRepositoryImpl extends SimpleJpaRepository<PassengerDetails, Serializable> implements PassengerRepository{
	
	public  PassengerRepositoryImpl(EntityManager em) {
        super(PassengerDetails.class, em);
        this.em = em;
    }

	private EntityManager em;
	 
	@Override
	public PassengerDetails saveOrder(PassengerDetails orderObj,String prevOrder) {
		return this.save(orderObj);
	}
	
}
