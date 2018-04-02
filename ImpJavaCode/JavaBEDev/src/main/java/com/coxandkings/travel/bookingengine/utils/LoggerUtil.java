package com.coxandkings.travel.bookingengine.utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {

    private LoggerUtil() {
    }

	/**
	 * This method is no longer valid.
	 * Use <a href="https://logging.apache.org/log4j/2.0/log4j-api/apidocs/org/apache/logging/log4j/LogManager.html#getLogger(java.lang.Class)">LogManager.getLogger(Class<?> clazz)</a> instead.
	**/
    @Deprecated
    public static Logger getLoggerInstance(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

}
