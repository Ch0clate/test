package com.coxandkings.travel.bookingengine.db;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.kafka.KafkaConfig;


@SpringBootApplication
@ComponentScan
public class BookingEngineApplicationDB extends SpringBootServletInitializer {
	
	/**
	 * Acts as a PreProcessor for the SpringBoot bookingengine Application
	 * Adds a Key onto the redis cache - Try fetching it on the server command prompt.
	 * Add anything in this function to make it work like a Preprocessor for the application.
	 * @throws Exception
	 */
	
	private static final Logger logger = LogManager.getLogger(BookingEngineApplicationDB.class);
	
	@PostConstruct
	public void init() throws Exception {
		try {
			logger.info("loading the Kafka config");
			KafkaConfig.loadConfig();
			
		}catch (Exception e ){
			e.printStackTrace();
		}

		
	}


	public static void main(String[] args) {
		try {
			ConfigurableApplicationContext context= SpringApplication.run(BookingEngineApplicationDB.class, args);
			
			/*KafkaConfig.loadConfig();
			 logger.info("DBpopulation consumer to start");
		     context.getBean(KafkaBookConsumer.class).runConsumer();*/
		    
		}catch (Exception e ){
			//Something is really Fishy !
			e.printStackTrace();
		}
	}
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(BookingEngineApplicationDB.class);
	}

}
