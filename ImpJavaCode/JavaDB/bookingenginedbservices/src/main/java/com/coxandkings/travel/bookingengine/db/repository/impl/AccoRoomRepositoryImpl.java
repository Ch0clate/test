package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.repository.AccoRoomRepository;

@Repository
@Qualifier("AccoRoom")
public class AccoRoomRepositoryImpl extends SimpleJpaRepository<AccoRoomDetails, Serializable> implements AccoRoomRepository{
	
	public  AccoRoomRepositoryImpl(EntityManager em) {
        super(AccoRoomDetails.class, em);
        this.em = em;
    }

	private EntityManager em;
	

	@Override
	public AccoRoomDetails saveOrder(AccoRoomDetails orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
}
