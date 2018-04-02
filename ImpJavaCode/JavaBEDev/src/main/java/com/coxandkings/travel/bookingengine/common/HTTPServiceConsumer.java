package com.coxandkings.travel.bookingengine.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;

public class HTTPServiceConsumer {
	private static final Logger logger = LogManager.getLogger(HTTPServiceConsumer.class);

	public static Element consumeXMLService(String tgtSysId, URL tgtSysURL, Map<String, String> httpHdrs, Element reqElem) throws Exception {
		HttpURLConnection svcConn = null;
		try {
			svcConn = (HttpURLConnection) tgtSysURL.openConnection();
			String reqElemStr = XMLTransformer.toString(reqElem); 
			if (logger.isInfoEnabled()) {
				logger.info(String.format("%s_RQ = %s", tgtSysId, reqElemStr));
			}

			InputStream httpResStream = consumeService(tgtSysId, svcConn, httpHdrs, reqElemStr.getBytes());
			if (httpResStream != null) {
				Element resElem = XMLTransformer.getNewDocumentBuilder().parse(httpResStream).getDocumentElement();
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s_RS = %s", tgtSysId, XMLTransformer.toString(resElem)));
				}
				return resElem;
			}
		}
		catch (Exception x) {
			logger.warn(String.format("%s_ERR XML Service <%s> Consume Error", tgtSysId, tgtSysURL), x);
		}
		finally {
			if (svcConn != null) {
				svcConn.disconnect();
			}
		}
		
		return null;
	}

	public static JSONObject consumeJSONService(String tgtSysId, URL tgtSysURL, Map<String, String> httpHdrs, JSONObject reqJson) throws Exception {
		HttpURLConnection svcConn = null;
		try {
			svcConn = (HttpURLConnection) tgtSysURL.openConnection();
			String reqJsonStr = reqJson.toString(); 
			if (logger.isInfoEnabled()) {
				logger.info(String.format("%s_RQ = %s", tgtSysId, reqJsonStr));
			}
			
			InputStream httpResStream = consumeService(tgtSysId, svcConn, httpHdrs, reqJsonStr.getBytes());
			if (httpResStream != null) {
				//return new JSONObject(new JSONTokener(httpResStream));
				JSONObject resJson = new JSONObject(new JSONTokener(httpResStream));
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s_RS = %s", tgtSysId, resJson.toString()));
				}
				return resJson;
			}
		}
		catch (Exception x) {
			logger.warn(String.format("%s_ERR JSON Service <%s> Consume Error", tgtSysId, tgtSysURL), x);
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
	
	public static InputStream consumeService(JSONObject reqJson,String tgtSysURL) {
		
		HttpURLConnection svcConn = null;
		
		 URL mServiceURL;
		try {
			mServiceURL = new URL(tgtSysURL);
			svcConn = (HttpURLConnection) mServiceURL.openConnection();
			svcConn.setDoOutput(true);
			svcConn.setRequestMethod("POST");
			
			OutputStream httpOut = svcConn.getOutputStream();
			httpOut.write(reqJson.toString().getBytes());
			httpOut.flush();
			httpOut.close();
			
			int resCode = svcConn.getResponseCode();
			logger.debug(String.format("Receiving response from %s with HTTP response status: %s", tgtSysURL, resCode));
			if (resCode == HttpURLConnection.HTTP_OK) {
				return svcConn.getInputStream();
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		return null;
		
		
	}
	
}
