package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.persistence.EntityManager;



import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;


import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;



@Qualifier("updatestat")
@Repository
public class AirGetByClass {
	

	private EntityManager em;

	public AirGetByClass(EntityManager em) {
        this.em = em;
    }
	
	


	@SuppressWarnings("unchecked")
	public List<AirOrders> findByClass(String flightclass) {
		/*String queryString = "SELECT * \r\n" + 
		 		" cast(FROM dbo.airorders \r\n" + 
		 		"  flightdetails \\@\\> {\"originDestinationOptions\"\\:[{\"flightSegment\"\\: [{\"CabinType\"\\: \"Economy\"}]}]} as text);";
         String query1String = 
        		 "SELECT * cast(FROM dbo.airorders WHERE flightdetails @> '{\"originDestinationOptions\":[{\"flightSegment\": [{\"CabinType\":\"Economy\"}]}]} as text;')";*/
	   /*
*/
		
		 
		String query4 = "SELECT * \r\n" + 
					     		"  FROM dbo.airorders \r\n" + 
					     		"  WHERE flightdetails \\@\\> '{\"originDestinationOptions\":"
					     		+ " [{\"flightSegment\": [{\"CabinType\":\""+ flightclass + "\"}]}]}';";
		 javax.persistence.Query query = em.createNativeQuery(query4,AirOrders.class);
	   /*  List<AirOrders> mAir	=  ((javax.persistence.Query) query).getResultList();*/
		

		return query.getResultList();
	}
	
	

	
}

