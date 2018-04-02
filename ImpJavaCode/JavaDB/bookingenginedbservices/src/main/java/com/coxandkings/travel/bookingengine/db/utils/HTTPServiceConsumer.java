package com.coxandkings.travel.bookingengine.db.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;


public class HTTPServiceConsumer {
	private static final Logger logger = LogManager.getLogger(HTTPServiceConsumer.class);

	

	public static JSONObject consumeJSONService(String tgtSysId, URL tgtSysURL, Map<String, String> httpHdrs, JSONObject reqJson) throws Exception {
		HttpURLConnection svcConn = null;
		try {
			svcConn = (HttpURLConnection) tgtSysURL.openConnection();
			String reqJsonStr = reqJson.toString(); 
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("%s JSON Request = %s", tgtSysId, reqJsonStr));
			}
			
			InputStream httpResStream = consumeService(tgtSysId, svcConn, httpHdrs, reqJsonStr.getBytes());
			if (httpResStream != null) {
				//return new JSONObject(new JSONTokener(httpResStream));
				JSONObject resJson = new JSONObject(new JSONTokener(httpResStream));
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("%s JSON Response = %s", tgtSysId, resJson.toString()));
				}
				return resJson;
			}
		}
		catch (Exception x) {
			System.out.println(x);
			logger.warn(String.format("%s JSON Service <%s> Consume Error", tgtSysId, tgtSysURL), x);
		}
		finally {
			if (svcConn != null) {
				svcConn.disconnect();
			}
		}
		
		return null;
	}

	private static InputStream consumeService(String tgtSysId, HttpURLConnection svcConn, Map<String, String> httpHdrs, byte[] payload) throws Exception {
		svcConn.setDoOutput(true);
		svcConn.setRequestMethod("POST");
		
		Set<Entry<String,String>> httpHeaders = httpHdrs.entrySet();
		if (httpHeaders != null && httpHeaders.size() > 0) {
			Iterator<Entry<String,String>> httpHeadersIter = httpHeaders.iterator();
			while (httpHeadersIter.hasNext()) {
				Entry<String,String> httpHeader = httpHeadersIter.next();
				svcConn.setRequestProperty(httpHeader.getKey(), httpHeader.getValue());
			}
		}
		
		logger.trace(String.format("Sending request to %s",tgtSysId));
		OutputStream httpOut = svcConn.getOutputStream();
		httpOut.write(payload);
		httpOut.flush();
		httpOut.close();

		int resCode = svcConn.getResponseCode();
		logger.debug(String.format("Receiving response from %s with HTTP response status: %s", tgtSysId, resCode));
		if (resCode == HttpURLConnection.HTTP_OK) {
			return svcConn.getInputStream();
		}
		
		return null;
	}
}
