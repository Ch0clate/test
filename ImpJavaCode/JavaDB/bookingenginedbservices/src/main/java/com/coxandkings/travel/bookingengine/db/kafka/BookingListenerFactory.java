package com.coxandkings.travel.bookingengine.db.kafka;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.DataBaseService;

@Service
public class BookingListenerFactory {

	@Autowired
	private List<DataBaseService> services;
	@Autowired
	@Qualifier("Kafka")
	private KafkaBookProducer kafkaService;

	@Autowired
	private BookingServiceImpl bookingService;

	private static final Logger logger = LogManager.getLogger(BookingListenerFactory.class);

	private DataBaseService serviceForProduct(String product) {
		for (DataBaseService service : services) {
			if (service.isResponsibleFor(product)) {
				return service;
			}
		}

		throw new UnsupportedOperationException("unsupported ProductType");
	}

	// TODO: check for the return types of these methods
	public void processBooking(String payload) throws JSONException, BookingEngineDBException {

		JSONObject reqjson = new JSONObject(payload);

		String res = null, productRes;
		if (reqjson.has("requestBody"))
			serviceForProduct(reqjson.getJSONObject("requestBody").getString("product")).processBookRequest(reqjson);

		else {
			productRes = serviceForProduct(reqjson.getJSONObject("responseBody").getString("product"))
					.processBookResponse(reqjson);

			if (productRes.equalsIgnoreCase("SUCCESS"))
				res = serviceForProduct("BOOKING").processBookResponse(reqjson);

			if (res.equalsIgnoreCase("SUCCESS")) {
				try {
					kafkaService.runProducer(1, new JSONObject(
							bookingService.getByBookID(reqjson.getJSONObject("responseBody").getString("bookID"),"false")));

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}
}
