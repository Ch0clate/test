/*package com.coxandkings.travel.bookingengine.db.aspect;

import java.io.Serializable;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import com.coxandkings.travel.bookingengine.db.utils.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceAspect {

	//private static final Logger LOGGER = LoggerUtil.getLoggerInstance(ServiceAspect.class);

	@Before("execution(* com.coxandkings.travel.bookingengine.db.repository.impl.*.saveOrder(..))")
	public void logBefore(JoinPoint joinPoint) throws Exception {

		System.out.println("inside before  method execution");

		// Getting a logger instance
		Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());

		// Prints the JoinPoint details
		myLogger.info("Requested class : {} ; Method : {} " + joinPoint.getTarget().getClass().getName()
				+ joinPoint.getSignature().getName());
		
		Object[] signatureArgs = joinPoint.getArgs();
		
		JSONObject json =new JSONObject(signatureArgs[0].toString());
		JSONObject afterupdate =  (JSONObject) json.remove("data_value");
		
		json.put("afterUpdate", afterupdate.toString());
		
		if(!signatureArgs[1].equals("")){
			
			json.put("beforeUpdate", new JSONObject(signatureArgs[1].toString()).remove("data_value").toString());
			
		}
		
		Map<String, String> mHttpHeaders = new HashMap<String, String>();
		mHttpHeaders.put("Content-Type", "application/json");

		// TODO: to be done dynamically
		json.put("entityName", "AccoOrdrs");
		json.put("operationID", "update");
		
		System.out.println("request json: " + json.toString());

		JSONObject res = HTTPServiceConsumer.consumeJSONService("DBUPDATE", new URL("http://10.24.2.95:9200/booking-dbupdate/auditLogs/"),
				mHttpHeaders, json);

	}

	// Pointcut to execute on all the methods of classes in a package
	@Pointcut("within(com.coxandkings.travel.bookingengine.db.*.*)")
	public void allMethodsPointcut() {
	}

	// Generic AspectJ Loggers which will Log advice Before and After execution of
	// the functions
	@Before("allMethodsPointcut()")
	public void allServiceMethodsBeforeAdvice(JoinPoint joinPoint) {

		// Getting a logger instance
		Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());

		// Prints the JoinPoint details
		myLogger.info("Requested class : {} ; Method : {} " + joinPoint.getTarget().getClass().getName()
				+ joinPoint.getSignature().getName());
		Object[] signatureArgs = joinPoint.getArgs();
		for (Object signatureArg : signatureArgs) {
			myLogger.info("Arguments: {} " + signatureArg.toString());
		}
	}

	@Around("allMethodsPointcut()")
	public Object stats(ProceedingJoinPoint pjp) throws Throwable {
		// Getting a logger instance
		Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
		long start = System.currentTimeMillis();
		Object output = pjp.proceed();
		String className = pjp.getSignature().getDeclaringTypeName();
		String methodName = pjp.getSignature().getName();

		long elapsedTime = System.currentTimeMillis() - start;
		if (!pjp.getSignature().getName().equals("initBinder")) {
			myLogger.info(methodName + " method in class " + className + " execution time : " + elapsedTime
					+ " milliseconds.");
		}

		// I have taken more than 10ms :( There is something fishy !!
		if (elapsedTime > 10) {
			myLogger.warn("Method execution longer than 10 ms for " + className + "." + methodName + "."
					+ "There is Something fishy !!");
		}
		return output;
	}

}

class SerializedString implements Serializable {
	String s;

	public SerializedString(String s) {
		this.s = s;

	}

}
*/
