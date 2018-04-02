package com.coxandkings.travel.bookingengine.orchestrator.rail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.rail.RailConfig;

public class ClientCommercials implements RailConstants {

	private static final Logger logger = LogManager.getLogger(RailSearchProcessor.class);

	public static JSONObject getClientCommercials(JSONObject suppCommRes) {
		CommercialsConfig commConfig = RailConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig
				.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_CLIENT_TRANSACTIONAL);
		JSONObject breClientReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));
		JSONObject breClientReqRoot = breClientReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object")
				.getJSONObject("cnk.rail_commercialscalculationengine.clienttransactionalrules.Root");
		JSONObject suppCommResRoot = suppCommRes.getJSONObject("result").getJSONObject("execution-results")
				.getJSONArray("results").getJSONObject(0).getJSONObject("value")
				.getJSONObject("cnk.rail_commercialscalculationengine.suppliertransactionalrules.Root");
		JSONObject suppCommResHdr = suppCommResRoot.getJSONObject(JSON_PROP_BRMSHEADER);
		suppCommResHdr.remove("status");

		JSONArray suppCommResBRI = suppCommResRoot.getJSONArray(JSON_PROP_BUSSRULEINTAKE);
		int suppCommResBRILength = suppCommResBRI.length();
		JSONArray briJsonArr = new JSONArray();
		JSONArray entityDetailsJsonArr = new JSONArray();
		JSONObject entityDetailsJson = new JSONObject();
		JSONObject currSuppCommResBRI;
		for (int i = 0; i < suppCommResBRILength; i++) {
			// TODO: Entity details have been hard-coded. These should be
			// retrieved from client context and then added here.
			currSuppCommResBRI = (JSONObject) suppCommResBRI.get(i);
			currSuppCommResBRI.remove("commercialHead");
			currSuppCommResBRI.remove("selectedRow");
			currSuppCommResBRI.remove("commercialStatus");
			currSuppCommResBRI.remove("ruleFlowName");
			entityDetailsJson.put("entityType", "ClientType");
			entityDetailsJson.put("entityName", "CnKB2BIndEng");
			entityDetailsJson.put("entityMarket", "India");
			entityDetailsJsonArr.put(entityDetailsJson);
			currSuppCommResBRI.put("entityDetails", entityDetailsJsonArr);
			briJsonArr.put(currSuppCommResBRI);
		}

		breClientReqRoot.put(JSON_PROP_BRMSHEADER, suppCommResHdr);
		breClientReqRoot.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);
		//System.out.println("Client comm  req: "+breClientReqJson.toString());
		JSONObject breClientResJson = null;
		try {
			logger.info("Before opening HttpURLConnection to BRMS for Client Commercials");
			breClientResJson = HTTPServiceConsumer.consumeJSONService("BRMS", commTypeConfig.getServiceURL(),
					commConfig.getHttpHeaders(), breClientReqJson);
			logger.info("HttpURLConnection to BRMS closed");
		} catch (Exception x) {
			logger.warn("An exception occurred when calling client commercials", x);
		}

		return breClientResJson;
	}
}
