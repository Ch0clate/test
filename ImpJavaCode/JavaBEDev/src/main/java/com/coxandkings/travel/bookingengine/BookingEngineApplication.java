package com.coxandkings.travel.bookingengine;

import javax.annotation.PostConstruct;

//import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import com.coxandkings.travel.bookingengine.config.KafkaConfig;
import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.config.OperationsShellConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.config.activities.ActivitiesConfig;
import com.coxandkings.travel.bookingengine.config.air.AirConfig;
import com.coxandkings.travel.bookingengine.config.bus.BusConfig;
import com.coxandkings.travel.bookingengine.config.car.CarConfig;
import com.coxandkings.travel.bookingengine.config.cruise.CruiseConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.config.rail.RailConfig;
import com.coxandkings.travel.bookingengine.config.transfers.TransfersConfig;
import com.coxandkings.travel.bookingengine.utils.RedisAirlineData;
//import com.coxandkings.travel.bookingengine.utils.RedisAirportData;
import com.coxandkings.travel.bookingengine.utils.RedisAirportDataV2;
import com.coxandkings.travel.bookingengine.utils.RedisCityData;
import com.coxandkings.travel.bookingengine.utils.RedisCityDataV2;
import com.coxandkings.travel.bookingengine.utils.RedisRoeDataV2;
import com.coxandkings.travel.bookingengine.utils.TrackingContextPatternConverter;

@SpringBootApplication
@ComponentScan
//@EnableAutoConfiguration(exclude = { MongoAutoConfiguration.class })
public class BookingEngineApplication extends SpringBootServletInitializer {

	/**
	 * Acts as a PreProcessor for the SpringBoot bookingengine Application Adds a
	 * Key onto the redis cache - Try fetching it on the server command prompt. Add
	 * anything in this function to make it work like a Preprocessor for the
	 * application.
	 * 
	 * @throws Exception
	 */
	@PostConstruct
	public void init() throws Exception {
		//PluginManager.addPackage(TrackingContextPatternConverter.class.getPackage().getName());
		// Loading the configurations
		try {
			AirConfig.loadConfig();
			AccoConfig.loadConfig();
			HolidaysConfig.loadConfig();
			ActivitiesConfig.loadConfig();
			CarConfig.loadConfig();
			BusConfig.loadConfig();
			TransfersConfig.loadConfig();
			RailConfig.loadConfig();
			MDMConfig.loadConfig();
			RedisConfig.loadConfig();
			RedisAirlineData.loadConfig();
			RedisAirportDataV2.loadConfig();
			RedisCityData.load();
			RedisCityDataV2.loadConfig();
			RedisRoeDataV2.loadConfig();
			KafkaConfig.loadConfig();
			CruiseConfig.loadConfig();
			OperationsShellConfig.loadConfig();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			SpringApplication.run(BookingEngineApplication.class, args);
		} catch (Exception e) {
			// Something is really Fishy !
			e.printStackTrace();
		}
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(BookingEngineApplication.class);
	}
}
