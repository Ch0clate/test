package com.coxandkings.travel.bookingengine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest
@TransactionConfiguration(defaultRollback=true,transactionManager="transactionManager")
public class BookingEngineApplicationTests {

	@Test
	public void contextLoads() {
	}

}
