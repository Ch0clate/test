package com.coxandkings.travel.bookingengine.db.kafka;


import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaBookConsumer {
	
	

    @Autowired
    BookingListenerFactory bookingListenerFactory;

    @KafkaListener(topics = "bookingEngine")
    public void onReceiving(String payload) throws IOException {
       

        bookingListenerFactory.processBooking(payload);
    }


}
