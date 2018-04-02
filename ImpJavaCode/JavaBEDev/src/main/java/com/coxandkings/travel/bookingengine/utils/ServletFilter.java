package com.coxandkings.travel.bookingengine.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import org.w3c.dom.Element;

@Component
public class ServletFilter extends AbstractRequestLoggingFilter {
	
	private static final Logger logger = LogManager.getLogger(ServletFilter.class);
	private static final int PRETTYPRINT_INDENT_FCTR = 2;
	private boolean prettyPrintJson = false;
	
	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;
	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;
	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;
	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;
	
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		
	}

	protected void beforeResponse(HttpServletResponse response, String message) {
	}
	
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		//LoggerUtil.logMessage((String.format("REQUEST DATA : %s", getMessagePayload(request))),Level.INFO);
		logger.info((String.format("BE JSON Request : %s", getMessagePayload(request))));
	}

	protected void afterResponse(HttpServletResponse response, String message) {
		//LoggerUtil.logMessage((String.format("RESPONSE DATA : %s", getMessagePayload(response))),Level.INFO);
		logger.info((String.format("BE JSON Response : %s", getMessagePayload(response))));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isFirstRequest = !isAsyncDispatch(request);
		HttpServletRequest requestToUse = request;
		HttpServletResponse responseToUse = response;

		if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper) && !(response instanceof ContentCachingResponseWrapper)) {
			requestToUse = new ContentCachingRequestWrapper(request);
			responseToUse = new ContentCachingResponseWrapper(response);
		}
		ServletContext.setServletContext(requestToUse, responseToUse);
		//TODO:add tracking ctx here
		
		try {
			filterChain.doFilter(requestToUse, responseToUse);
		}
		finally {
			if (!isAsyncStarted(requestToUse)) {
				afterRequest(requestToUse, getAfterMessage(requestToUse));
				afterResponse(responseToUse, getAfterMessage(responseToUse));
			}
			((ContentCachingResponseWrapper)responseToUse).copyBodyToResponse();
		}
	}

	private String getBeforeMessage(HttpServletRequest request) {
		return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	private String getBeforeMessage(HttpServletResponse response) {
		return createMessage(response, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}
	
	private String getAfterMessage(HttpServletRequest request) {
		return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	private String getAfterMessage(HttpServletResponse response) {
		return createMessage(response, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	protected String createMessage(HttpServletResponse response, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);

		if (isIncludeHeaders()) {
			msg.append(";headers=").append(new ServletServerHttpResponse(response).getHeaders());
		}

		if (isIncludePayload()) {
			String payload = getMessagePayload(response);
			if (payload != null) {
				msg.append(";payload=").append(payload);
			}
		}

		msg.append(suffix);
		return msg.toString();
	}

	protected String getMessagePayload(HttpServletResponse response) {
		ContentCachingResponseWrapper wrapper =
				getNativeResponse(response, ContentCachingResponseWrapper.class);
		return getPayload(wrapper);
	}

	protected String getMessagePayload(HttpServletRequest request) {
		ContentCachingRequestWrapper wrapper =
				WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
		return getPayload(wrapper);
	}

	private String getPayload(Object wrapper) {
		if (wrapper != null) {
			byte[] buf = wrapper instanceof ContentCachingRequestWrapper?((ContentCachingRequestWrapper) wrapper).getContentAsByteArray():((ContentCachingResponseWrapper) wrapper).getContentAsByteArray();
			String charEncdng = wrapper instanceof ContentCachingRequestWrapper?((ContentCachingRequestWrapper) wrapper).getCharacterEncoding():((ContentCachingResponseWrapper) wrapper).getCharacterEncoding();
				if (buf.length > 0) {
				int length = buf.length;
				try {
					String payload = new String(buf, 0, length, charEncdng);
					return prettyPrintJson?new JSONObject(payload).toString(PRETTYPRINT_INDENT_FCTR):toEscapedString(payload);
				}
				catch (UnsupportedEncodingException ex) {
					return "[unknown]";
				}
			}
		}
		return "";
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getNativeResponse(ServletResponse response,Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(response)) {
				return (T) response;
			}
			else if (response instanceof ServletResponseWrapper) {
				return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		return null;
	}
	
	public void setPrettyPrintJson(boolean prettyprintFlag) {
		prettyPrintJson = prettyprintFlag;
	}
	
	private String toEscapedString(String message){
		return message.replaceAll("[\\\t|\\\r|\\\n]", "");
	}

}


