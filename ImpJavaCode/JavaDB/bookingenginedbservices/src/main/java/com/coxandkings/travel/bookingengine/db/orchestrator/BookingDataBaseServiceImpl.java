package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ProductOrder;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;

@Service
@Qualifier("Booking")
@Transactional(readOnly = false)
public class BookingDataBaseServiceImpl implements DataBaseService,Constants {
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	
	@Override
	public boolean isResponsibleFor(String product) {
		return "BOOKING".equalsIgnoreCase(product);
	}

	@Override
	public String processBookRequest(JSONObject reqJson) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String processBookResponse(JSONObject bookResponseJson) {
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject("responseBody").getString("bookID"));
		String prevBooking = booking.toString();
		
		Set<ProductOrder> productOrders = booking.getProductOrders();
		int size = 0;
		for(ProductOrder prodOrder:productOrders ) {
			String s = prodOrder.getStatus();
			if(prodOrder.getStatus().equalsIgnoreCase("Confirmed"))
				size++;		
		}
		
		if(size==productOrders.size()) {
		booking.setStatus("confirmed");
		
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		saveBookingOrder(booking,prevBooking);
		
		return "SUCCESS";
		}
		
		return "FAILED";
	}

	public Booking saveBookingOrder(Booking order, String prevBooking) {
		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj,prevBooking);
	}

}
