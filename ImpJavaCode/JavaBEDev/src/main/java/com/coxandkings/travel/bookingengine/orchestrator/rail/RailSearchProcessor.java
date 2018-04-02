package com.coxandkings.travel.bookingengine.orchestrator.rail;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.rail.RailConfig;
import com.coxandkings.travel.bookingengine.orchestrator.rail.enums.WeekDays;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class RailSearchProcessor implements RailConstants {

	private static final Logger logger = LogManager.getLogger(RailSearchProcessor.class);
	static final String OPERATION_NAME = "search";

	public static String process(JSONObject reqJson) {
		try {

			// opConfig: contains all details of search operation-SI URL, Req XML Shell
			OperationConfig opConfig = RailConfig.getOperationConfig(OPERATION_NAME);
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			TrackingContext.setTrackingContext(reqJson);
			JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);

			String sessionId = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionId = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userId = reqHdrJson.getString(JSON_PROP_USERID);

			UserContext usrContxt = UserContext.getUserContextForSession(reqHdrJson);
			List<ProductSupplier> productSuppliers = usrContxt.getSuppliersForProduct(PROD_CATEG_TRANSPORT,
					PROD_CAT_SUBTYPE);

			// **********WEM JSON TO SI XML FOR REQUEST HEADER STARTS HERE**********//

			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:SessionID", sessionId);
			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:TransactionID", transactionId);
			XMLUtils.setValueAtXPath(reqElem, "./rail:RequestHeader/com:UserID", userId);

			Element suppCredsList = XMLUtils.getFirstElementAtXPath(reqElem,
					"./rail:RequestHeader/com:SupplierCredentialsList");

			if (productSuppliers == null) {
				throw new Exception("Product supplier not found for user/client");
			}

			for (ProductSupplier prodSupplier : productSuppliers) {
				suppCredsList.appendChild(prodSupplier.toElement(ownerDoc));
			}

			// ************WEM JSON TO SI XML FOR REQUEST HEADER ENDS HERE************//

			// *******WEM JSON TO SI XML FOR REQUEST BODY STARTS HERE*********//

			XMLUtils.setValueAtXPath(reqElem,
					"./raili:RequestBody/ota:OTA_RailShopRQ/ota:OriginDestination/ota:DepartureDateTime",
					reqBodyJson.getString(JSON_PROP_TRAVELDATE));
			XMLUtils.setValueAtXPath(reqElem,
					"./raili:RequestBody/ota:OTA_RailShopRQ/ota:OriginDestination/ota:OriginLocation",
					reqBodyJson.getString(JSON_PROP_ORIGINLOC));
			XMLUtils.setValueAtXPath(reqElem,
					"./raili:RequestBody/ota:OTA_RailShopRQ/ota:OriginDestination/ota:DestinationLocation",
					reqBodyJson.getString(JSON_PROP_DESTLOC));

			// ********WEM JSON TO SI XML FOR REQUEST BODY ENDS HERE***********//

			// ******CONSUME SI SEARCH SERVICE***********************//

			logger.info("Before opening HttpURLConnection to SI");
			Element resElem = null;
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					RailConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			logger.info("HttpURLConnection to SI closed");
			JSONObject resJson;
			resJson = getSupplierResponseJSON(reqJson, resElem);
			logger.info("SI res: " + resJson.toString());
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));

			logger.info("After creating response from SI");

			// ***********SI SEARCH RESPONSE CONVERTED TO JSON***********//

			Map<String, JSONObject> brmsToSIClassMap = new LinkedHashMap<String, JSONObject>();
			JSONObject resSupplierComJson = SupplierCommercials.getSupplierCommercials(reqJson, resJson,
					brmsToSIClassMap);

			JSONObject resClientComJson = ClientCommercials.getClientCommercials(resSupplierComJson);
			// calculatePrices(reqJson, resJson, resClientComJson, brmsToSIClassMap);
			// System.out.println("Client comm res: " + resClientComJson.toString());
			logger.info("Client comm res: " + resClientComJson.toString());
			return resJson.toString();

		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}

	public static JSONObject getSupplierResponseJSON(JSONObject reqJson, Element resElem) {
		JSONObject resJson = new JSONObject();
		Element resBodyElem = XMLUtils.getFirstElementAtXPath(resElem,
				"./raili:ResponseBody/rail:OTA_RailShopRSWrapper");
		JSONObject resBodyJson = new JSONObject();
		JSONArray originDestOptions = new JSONArray();

		resBodyJson.put(JSON_PROP_ORIGINDESTOPTS, originDestOptions);
		Element[] originDestElems = XMLUtils.getElementsAtXPath(resBodyElem,
				"./ota:OTA_RailShopRS/ota:OriginDestinationInformation/ota:OriginDestinationOption");
		for (Element orgDestElem : originDestElems) {
			JSONObject originDestJson = getOriginDestJSON(orgDestElem, resBodyElem);
			if (originDestJson != null)
				originDestOptions.put(originDestJson);
		}
		resJson.put(JSON_PROP_RESHEADER, reqJson.get(JSON_PROP_REQHEADER));
		resJson.put(JSON_PROP_RESBODY, resBodyJson);

		return resJson;
	}

	public static JSONObject getOriginDestJSON(Element originDestElem, Element resBodyElem) {
		JSONObject originDestJson = null;
		JSONArray availClassArr = null;
		Element[] availClassDetails = XMLUtils.getElementsAtXPath(originDestElem,
				"./ota:JourneySegment/ota:AvailabilityDetail");

		if (availClassDetails.length != 0) {
			originDestJson = new JSONObject();
			originDestJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(resBodyElem, "./rail:SupplierID"));
			originDestJson.put(JSON_PROP_ORIGINLOC, XMLUtils.getValueAtXPath(originDestElem, "./ota:OriginLocation"));
			originDestJson.put(JSON_PROP_DESTLOC,
					XMLUtils.getValueAtXPath(originDestElem, "./ota:DestinationLocation"));

			availClassArr = new JSONArray();
			for (Element availClass : availClassDetails) {
				availClassArr.put(getAvailClassJSON(availClass));
			}
			Element trainSegmentElem = XMLUtils.getFirstElementAtXPath(originDestElem,
					"./ota:JourneySegment/ota:TrainSegment");
			JSONObject trainDetailsJson = getTrainDetailsJSON(trainSegmentElem);

			originDestJson.put(JSON_PROP_TRAINDETAILS, trainDetailsJson);
			originDestJson.put(JSON_PROP_CLASSAVAIL, availClassArr);

		}

		return originDestJson;
	}

	public static JSONObject getAvailClassJSON(Element availClassElem) {
		JSONObject availClassJson = new JSONObject();
		JSONObject paxDetails, berthOpts;
		NamedNodeMap paxAttr, berthAttr;
		Node attr;
		Element passengerDetail, applicableBerth;
		availClassJson.put(JSON_PROP_RESERVATIONCLASS, XMLUtils.getValueAtXPath(availClassElem, "./@ReservationClass"));
		availClassJson.put(JSON_PROP_RESERVATIONTYPE, XMLUtils.getValueAtXPath(availClassElem, "./@ReservationType"));
		availClassJson.put(JSON_PROP_PRICING,
				getPricingInfoJSON(XMLUtils.getFirstElementAtXPath(availClassElem, "./ota:Pricing")));
		Element[] availDateDetails = XMLUtils.getElementsAtXPath(availClassElem,
				"./ota:Pricing/ota:TPA_Extensions/rail:Rail_TPA/rail:AvailabilityDetails");
		JSONArray availDateArr = new JSONArray();

		for (Element availDate : availDateDetails) {
			availDateArr.put(getAvailDateJSON(availDate));
		}
		availClassJson.put(JSON_PROP_AVAILABILITYINFO, availDateArr);
		passengerDetail = XMLUtils.getFirstElementAtXPath(availClassElem,
				"./ota:Pricing/ota:TPA_Extensions/rail:Rail_TPA/rail:PassengerDetails");
		paxAttr = passengerDetail.getAttributes();
		paxDetails = new JSONObject();
		for (int i = 0; i < paxAttr.getLength(); i++) {
			attr = paxAttr.item(i);
			paxDetails.put(attr.getNodeName(), Boolean.valueOf(attr.getNodeValue()));
		}

		applicableBerth = XMLUtils.getFirstElementAtXPath(availClassElem,
				"./ota:Pricing/ota:TPA_Extensions/rail:Rail_TPA/rail:ApplicableBerth");
		berthAttr = applicableBerth.getAttributes();
		berthOpts = new JSONObject();
		for (int i = 0; i < paxAttr.getLength(); i++) {
			attr = berthAttr.item(i);
			berthOpts.put(attr.getNodeName(), Boolean.valueOf(attr.getNodeValue()));
		}

		availClassJson.put(JSON_PROP_PAXPREF, paxDetails);
		availClassJson.put(JSON_PROP_APPBERTH, berthOpts);
		return availClassJson;
	}

	public static JSONObject getAvailDateJSON(Element availDateElem) {
		JSONObject availDateDetail = new JSONObject();
		availDateDetail.put(JSON_PROP_AVAILDATE, XMLUtils.getValueAtXPath(availDateElem, "./rail:AvailablityDate"));
		availDateDetail.put(JSON_PROP_AVAILSTATUS, XMLUtils.getValueAtXPath(availDateElem, "./rail:AvailablityStatus"));
		availDateDetail.put(JSON_PROP_AVAILTYPE, XMLUtils.getValueAtXPath(availDateElem, "./rail:AvailablityType"));

		return availDateDetail;
	}

	public static JSONObject getPricingInfoJSON(Element priceDetailElem) {
		JSONObject priceDetails, totalFare, priceBreakup;
		totalFare = new JSONObject();
		priceDetails = new JSONObject();
		totalFare.put(JSON_PROP_AMOUNT,
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(priceDetailElem, "./ota:Price/@Amount"), 0));
		totalFare.put(JSON_PROP_CURRENCYCODE, XMLUtils.getValueAtXPath(priceDetailElem, "./ota:Price/@CurrencyCode"));
		priceDetails.put(JSON_PROP_TOTAL, totalFare);
		priceBreakup = getFareBreakup(XMLUtils.getFirstElementAtXPath(priceDetailElem, "./ota:PriceBreakdown"));
		priceDetails.put(JSON_PROP_FAREBREAKUP, priceBreakup);

		return priceDetails;
	}

	public static JSONObject getFareBreakup(Element breakupElem) {
		JSONObject fareBreakup, baseFare;
		JSONArray taxArr, ancillaryArr, feeArr;
		baseFare = new JSONObject();
		fareBreakup = new JSONObject();

		taxArr = new JSONArray();
		ancillaryArr = new JSONArray();
		feeArr = new JSONArray();
		fareBreakup.put(JSON_PROP_BASE, baseFare);
		baseFare.put(JSON_PROP_AMOUNT,
				Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(breakupElem, "./ota:BasicFare/@Amount"), 0));
		baseFare.put(JSON_PROP_CURRENCYCODE, XMLUtils.getValueAtXPath(breakupElem, "./ota:BasicFare/@CurrencyCode"));
		Element[] ancillaryCharges = XMLUtils.getElementsAtXPath(breakupElem, "./ota:AncillaryCharge");
		Element[] fees = XMLUtils.getElementsAtXPath(breakupElem, "./ota:Fee");
		Element[] taxes = XMLUtils.getElementsAtXPath(breakupElem, "./ota:Tax");
		for (Element ancillary : ancillaryCharges) {
			JSONObject ancObj = getTaxDetailsJson(ancillary);
			ancillaryArr.put(ancObj);
		}

		for (Element fee : fees) {
			JSONObject feeObj = getTaxDetailsJson(fee);
			feeArr.put(feeObj);
		}

		for (Element tax : taxes) {
			JSONObject taxObj = getTaxDetailsJson(tax);
			taxArr.put(taxObj);
		}
		fareBreakup.put(JSON_PROP_ANCILLARY, ancillaryArr);
		fareBreakup.put(JSON_PROP_FEES, feeArr);
		fareBreakup.put(JSON_PROP_TAXES, taxArr);
		return fareBreakup;
	}

	public static JSONObject getTaxDetailsJson(Element taxElem) {
		JSONObject taxObj = new JSONObject();
		taxObj.put(JSON_PROP_CODECONTEXT, XMLUtils.getValueAtXPath(taxElem, "./@CodeContext"));
		taxObj.put(JSON_PROP_AMOUNT, Utils.convertToBigDecimal(XMLUtils.getValueAtXPath(taxElem, "./@Amount"), 0));
		taxObj.put(JSON_PROP_CURRENCYCODE, XMLUtils.getValueAtXPath(taxElem, "./@CurrencyCode"));

		return taxObj;
	}

	public static JSONObject getTrainDetailsJSON(Element trainSegmentElem) {
		JSONObject trainDetailsJson, operationTime;
		trainDetailsJson = new JSONObject();
		operationTime = new JSONObject(); // operation schedule on weekdays

		trainDetailsJson.put(JSON_PROP_DEPARTTIME, XMLUtils.getValueAtXPath(trainSegmentElem, "./@DepartureDateTime"));
		trainDetailsJson.put(JSON_PROP_ARRIVALTIME, XMLUtils.getValueAtXPath(trainSegmentElem, "./@ArrivalDateTime"));
		trainDetailsJson.put(JSON_PROP_JOURNEYDURATION,
				XMLUtils.getValueAtXPath(trainSegmentElem, "./@JourneyDuration"));
		trainDetailsJson.put(JSON_PROP_OPERATIONSCHEDULE, operationTime);
		String operationTimeXPath = "./ota:DepartureStation/ota:OperationSchedules/ota:OperationSchedule/ota:OperationTimes/ota:OperationTime/@";
		operationTime.put(WeekDays.MON.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.MON.toString()))));
		operationTime.put(WeekDays.TUE.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.TUE.toString()))));
		operationTime.put(WeekDays.WED.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.WED.toString()))));
		operationTime.put(WeekDays.THU.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.THU.toString()))));
		operationTime.put(WeekDays.FRI.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.FRI.toString()))));
		operationTime.put(WeekDays.SAT.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.SAT.toString()))));
		operationTime.put(WeekDays.SUN.toString(), Boolean.valueOf(
				XMLUtils.getValueAtXPath(trainSegmentElem, operationTimeXPath.concat(WeekDays.SUN.toString()))));

		trainDetailsJson.put(JSON_PROP_TRAINNUM,
				XMLUtils.getValueAtXPath(trainSegmentElem, "./ota:TrainInfo/ota:Train/ota:TrainNumber"));
		trainDetailsJson.put(JSON_PROP_TRAINNAME,
				XMLUtils.getValueAtXPath(trainSegmentElem, "./ota:Remarks/ota:Remark[@Tag='TrainName']/@ShortText"));
		trainDetailsJson.put(JSON_PROP_TRAINTYPE,
				XMLUtils.getValueAtXPath(trainSegmentElem, "./ota:Remarks/ota:Remark[@Tag='TrainType']/@Code"));

		return trainDetailsJson;
	}

	public static void calculatePrices(JSONObject reqJson, JSONObject resJson, JSONObject clientCommResJson,
			Map<String, JSONObject> brmsToSIClassMap) {

		JSONArray briArr = clientCommResJson.getJSONObject("result").getJSONObject("execution-results")
				.getJSONArray("results").getJSONObject(0).getJSONObject("value")
				.getJSONObject("cnk.rail_commercialscalculationengine.clienttransactionalrules.Root")
				.getJSONArray(JSON_PROP_BUSSRULEINTAKE);
		JSONArray clientCommTrainDetArr, clientCommPaxArr, clientEntityCommArr, clientCommRetentionArr;
		JSONObject clientEntityCommJson, briJson, suppPricing, suppTotalFareJson, suppBaseFareJson, suppFareBrkup,
				retentionObj;
		JSONObject newTotalPrice, newBasePrice;

		int briLen = briArr.length();
		for (int i = 0; i < briLen; i++) {
			briJson = (JSONObject) briArr.get(i);
			clientCommTrainDetArr = briJson.getJSONArray(JSON_PROP_TRAINDETAILS);
			int trainDetLen = clientCommTrainDetArr.length();
			for (int j = 0; j < trainDetLen; j++) {
				JSONObject clientCommTrainJson = clientCommTrainDetArr.getJSONObject(j);
				clientCommPaxArr = clientCommTrainJson.getJSONArray("passengerDetails");
				int paxDetLen = clientCommPaxArr.length();
				for (int k = 0; k < paxDetLen; k++) {
					JSONObject clientCommPaxJson = clientCommPaxArr.getJSONObject(k);
					JSONObject suppClassInfo = brmsToSIClassMap
							.get(String.format("%s|%s", clientCommTrainJson.get(JSON_PROP_TRAINNUM), k));
					suppPricing = suppClassInfo.getJSONObject(JSON_PROP_PRICING);
					suppTotalFareJson = suppPricing.getJSONObject(JSON_PROP_TOTAL);
					suppFareBrkup = suppPricing.getJSONObject(JSON_PROP_FAREBREAKUP);
					suppBaseFareJson = suppFareBrkup.getJSONObject(JSON_PROP_BASE);
					/*
					 * ancillary = suppPricing.getJSONArray(JSON_PROP_ANCILLARY); fees =
					 * suppPricing.getJSONArray(JSON_PROP_ANCILLARY); taxes =
					 * suppPricing.getJSONArray(JSON_PROP_ANCILLARY);
					 */clientEntityCommArr = clientCommPaxJson.optJSONArray(JSON_PROP_ENTITYCOMMS);

					if (clientEntityCommArr == null) {
						logger.warn("Client commercials calculations not found");
						continue;
					} else {
						for (int l = (clientEntityCommArr.length() - 1); l >= 0; l--) {
							clientEntityCommJson = clientEntityCommArr.getJSONObject(l);
							clientCommRetentionArr = clientEntityCommJson.optJSONArray("retentionCommercialDetails");
							newTotalPrice = new JSONObject();
							newBasePrice = new JSONObject();

							/*
							 * clientMarkupJson =
							 * clientEntityCommJson.optJSONObject("markUpCommercialDetails"); if
							 * (clientMarkupJson == null) { continue; }
							 */

							if (clientCommRetentionArr != null) {
								for (int m = 0; m < clientCommRetentionArr.length(); m++) {
									retentionObj = clientCommRetentionArr.getJSONObject(m);
									if (retentionObj.getString("commercialName").equals("Standard")) {
										BigDecimal newTotal = retentionObj.getBigDecimal("commercialAmount")
												.add(suppTotalFareJson.getBigDecimal(JSON_PROP_AMOUNT));
										newTotalPrice.put(JSON_PROP_AMOUNT, newTotal);
										newTotalPrice.put(JSON_PROP_CURRENCYCODE, reqJson
												.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_CLIENTCCY));
										suppTotalFareJson.put(JSON_PROP_TOTAL, newTotalPrice);
										BigDecimal newBase = retentionObj.getBigDecimal("commercialAmount")
												.add(suppBaseFareJson.getBigDecimal(JSON_PROP_AMOUNT));
										newBasePrice.put(JSON_PROP_AMOUNT, newBase);
										newBasePrice.put(JSON_PROP_CURRENCYCODE, reqJson
												.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_CURRENCYCODE));
										suppBaseFareJson.put(JSON_PROP_BASE, newBasePrice);

										suppPricing.put(JSON_PROP_TOTAL, newTotalPrice);
										suppPricing.put(JSON_PROP_BASE, newBasePrice);
									}
								}
							}

							/*
							 * clientCommTotalPrice = clientMarkupJson.getBigDecimal(JSON_PROP_TOTAL);
							 * newTotalPrice.put(JSON_PROP_AMOUNT, clientCommTotalPrice);
							 * newTotalPrice.put(JSON_PROP_CURRENCYCODE,
							 * reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_CLIENTCCY));
							 * suppTotalFareJson.put(JSON_PROP_TOTAL, newTotalPrice); clientFareBreakup =
							 * clientMarkupJson.getJSONObject("fareBreakUp");
							 * 
							 * clientCommBasePrice = clientFareBreakup.getBigDecimal(JSON_PROP_BASE);
							 * newBasePrice.put(JSON_PROP_AMOUNT, clientCommBasePrice);
							 * newBasePrice.put(JSON_PROP_CURRENCYCODE,
							 * reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_CURRENCYCODE))
							 * ; suppBaseFareJson.put(JSON_PROP_BASE, newBasePrice);
							 * 
							 * suppPricing.put(JSON_PROP_TOTAL, newTotalPrice);
							 * suppPricing.put(JSON_PROP_BASE, newBasePrice);
							 */
							/*
							 * clientCommTaxArr = clientFareBreakup.getJSONArray("taxDetails"); JSONObject
							 * tax = new JSONObject(); BigDecimal newTax = new BigDecimal(0); int
							 * ancLen=ancillary.length(), taxLen=taxes.length(), feeLen=fees.length(); for
							 * (int x = 0; x < ancLen; x++) { tax = (JSONObject) clientCommTaxArr.get(x);
							 * newTax = tax.getBigDecimal("taxValue");
							 * //ancillary.getJSONObject(x).put("cod", value) }
							 */

							break;

						}
					}
				}

			}
		}
	}

}
