package com.coxandkings.travel.bookingengine.controller.common;


import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

//import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.RedisRoeDataV2;

@RestController
public class CommonController {
	@GetMapping(value = "/getRoe/{fromCcy}/{toCcy}/{market}")
    public BigDecimal roeService(@PathVariable("fromCcy") String fromCcy,@PathVariable("toCcy") String toCcy,@PathVariable("market") String market) {
		return RedisRoeDataV2.getRateOfExchange(fromCcy, toCcy,market);
    }
}
