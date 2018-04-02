package com.coxandkings.travel.bookingengine.config;

import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import com.coxandkings.travel.bookingengine.utils.ServletFilter;
import com.coxandkings.travel.bookingengine.utils.TrackingContextPatternConverter;

@Configuration
public class ServletFilterConfig {

	static {
		PluginManager.addPackage(TrackingContextPatternConverter.class.getPackage().getName());
	}
	@Autowired
	ServletFilter filter;

	@Bean
	public ServletFilter logFilter() {	
		filter.setIncludePayload(true);
		filter.setIncludeHeaders(false);
		//filter.setPrettyPrintJson(true);
		return filter;
	}
}