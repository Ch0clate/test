package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.CommercialsConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;

public class HolidaysSupplierCommercials implements HolidayConstants {

	private static final Logger logger = LogManager.getLogger(HolidaysSupplierCommercials.class);
	private static SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T00:00:00'");
	private static String brandname = null;
	private static double totalAirAmountAfterTax = 0;
	private static double totalAirAmountBeforeTax = 0;

	private static Map<String, ArrayList<String>> roomMap = new HashMap<String, ArrayList<String>>();
	private static Map<String, Double> airTaxMap = new HashMap<String, Double>();

	public static JSONObject getSupplierCommercials(JSONObject req, JSONObject res, String operationName) {
		Map<String, JSONObject> bussRuleIntakeBySupp = new HashMap<String, JSONObject>();
		CommercialsConfig commConfig = HolidaysConfig.getCommercialsConfig();
		CommercialTypeConfig commTypeConfig = commConfig
				.getCommercialTypeConfig(CommercialsConfig.COMMERCIAL_SUPPLIER_TRANSACTIONAL);
		JSONObject breSuppHolidayReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));

		JSONObject reqHeader = req.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBody = req.getJSONObject(JSON_PROP_REQBODY);

		JSONObject resHeader = res.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBody = res.getJSONObject(JSON_PROP_RESBODY);

		JSONObject breHdrJson = new JSONObject();
		breHdrJson.put(JSON_PROP_SESSIONID, resHeader.getString(JSON_PROP_SESSIONID));
		breHdrJson.put(JSON_PROP_SESSIONID, resHeader.getString(JSON_PROP_TRANSACTID));
		breHdrJson.put(JSON_PROP_USERID, resHeader.getString(JSON_PROP_USERID));

		String opName = convertOperationName(operationName);
		breHdrJson.put("operationName", opName);

		JSONObject rootJson = breSuppHolidayReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object")
				.getJSONObject("cnk.holidays_commercialscalculationengine.suppliertransactionalrules.Root");
		rootJson.put("header", breHdrJson);

		JSONArray dynamicPackageArr = resBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE);

		JSONArray briJsonArr = new JSONArray();
		JSONArray packageDetailsArr = null;
		for (int i = 0; i < dynamicPackageArr.length(); i++) {

			if (!dynamicPackageArr.getJSONObject(i).has("error")) {
				JSONObject dynamicPkgJson = dynamicPackageArr.getJSONObject(i);
				String suppID = dynamicPkgJson.getString(JSON_PROP_SUPPLIERID);
				JSONObject briJson = null;
				if (bussRuleIntakeBySupp.containsKey(suppID)) {
					briJson = bussRuleIntakeBySupp.get(suppID);
				} else {
					briJson = createBusinessRuleIntakeForSupplier(reqHeader, reqBody, resHeader, resBody,
							dynamicPkgJson, operationName);
					bussRuleIntakeBySupp.put(suppID, briJson);
				}

				packageDetailsArr = briJson.getJSONArray("packageDetails");
				packageDetailsArr.put(getBRMSpackageDetailsJSON(dynamicPkgJson, operationName, reqBody));
			}
		}
		Iterator<Map.Entry<String, JSONObject>> briEntryIter = bussRuleIntakeBySupp.entrySet().iterator();
		while (briEntryIter.hasNext()) {
			Map.Entry<String, JSONObject> briEntry = briEntryIter.next();
			briJsonArr.put(briEntry.getValue());
		}

		rootJson.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);

		JSONObject breSuppHolidayResJson = null;
		try {
			breSuppHolidayResJson = HTTPServiceConsumer.consumeJSONService("BRMS/SuppComm",
					commTypeConfig.getServiceURL(), commConfig.getHttpHeaders(), breSuppHolidayReqJson);
		} catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}

		System.out.println("Supplier Commercials Request:" + breSuppHolidayReqJson);
		logger.info(String.format("Supplier Commercial Request = %s", breSuppHolidayReqJson.toString()));
		return breSuppHolidayResJson;
	}

	private static String convertOperationName(String operationName) {

		if (operationName.equalsIgnoreCase("addservice")) {
			operationName = "AddService";
		} else {
			if (operationName.equalsIgnoreCase("getDetails")) {
				operationName = "Search";
			} else {
				operationName = operationName.substring(0, 1).toUpperCase() + operationName.substring(1).toLowerCase();
			}

		}

		return operationName;
	}

	private static JSONObject createBusinessRuleIntakeForSupplier(JSONObject reqHeader, JSONObject reqBody,
			JSONObject resHeader, JSONObject resBody, JSONObject dynamicPkgJson, String operationName) {

		JSONObject briJson = new JSONObject();

		JSONObject advancedDefinition = new JSONObject();

		// TODO :advancedDefinition object is hardcoded value below.Where will we get it
		// from?
		advancedDefinition.put(JSON_PROP_CLIENTNATIONALITY, "Indian");
		advancedDefinition.put("connectivitySupplierType", "LCC");
		advancedDefinition.put("connectivitySupplier", "CSN1");
		// TODO: credentialsName set as SupplierID value
		String suppID = dynamicPkgJson.getString(JSON_PROP_SUPPLIERID);
		advancedDefinition.put("credentialsName", suppID);

		JSONObject commonElemsJson = new JSONObject();
		commonElemsJson.put("supplier", suppID);
		// TODO: Supplier market is hard-coded below. Where will this come from? This
		// should be ideally come from supplier credentials.
		commonElemsJson.put("supplierMarket", "India");
		commonElemsJson.put("productCategory", "Holidays");
		// TODO: Contract validity and productCategorySubType is hard-coded below. Where
		// will this come from?
		commonElemsJson.put("contractValidity", "2016-11-11T00:00:00");
		commonElemsJson.put("productCategorySubType", "FIT");
		JSONObject clientCtx = reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT);
		commonElemsJson.put("clientType", clientCtx.getString("clientType"));
		// TODO: Properties for clientGroup, clientName are not yet set. Are these
		// required for B2C? What will be BRMS behavior if these properties are not
		// sent.

		// for slabDetails
		// TODO :slabType values need to confirm from where it will come. Now hard-coded
		// to greater than 200
		JSONArray slabDetailsJsonArray = new JSONArray();
		JSONObject slabDetailsJson = new JSONObject();

		slabDetailsJson.put("slabType", "NumberOfBookings");
		slabDetailsJson.put("slabTypeValue", 350);
		slabDetailsJsonArray.put(slabDetailsJson);

		briJson.put("slabDetails", slabDetailsJsonArray);
		briJson.put("advancedDefinition", advancedDefinition);
		briJson.put("commonElements", commonElemsJson);

		JSONArray packageDetailsArr = new JSONArray();

		// JSONObject packageDetailJson = dynamicPkgJson;
		// packageDetailsArr.put(getBRMSpackageDetailsJSON(packageDetailJson,
		// operationName, reqBody));

		briJson.put("packageDetails", packageDetailsArr);

		return briJson;
	}

	private static JSONObject getBRMSpackageDetailsJSON(JSONObject dynamicPkgJson, String operationName,
			JSONObject reqBody) {

		JSONArray applicableOnProductArr = new JSONArray();
		String key = null;
		String productName = "Amazing India";
		String travelDate = "";

		if (dynamicPkgJson.has(JSON_PROP_GLOBALINFO)) {
			JSONObject dynamicpackageId = dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO)
					.getJSONObject(JSON_PROP_DYNAMICPKGID);
			// TODO: ProductName is set to "Amazing India".Below code was for setting
			// tourName as productName from SI response

			/*
			 * if (dynamicpackageId.has(JSON_PROP_TPA_EXTENSIONS)) { productName =
			 * dynamicpackageId.getJSONObject(JSON_PROP_TPA_EXTENSIONS).getJSONObject(
			 * JSON_PROP_PKGS_TPA)
			 * .getJSONObject(JSON_PROP_TOURDETAILS).get(JSON_PROP_TOURNAME).toString(); }
			 */

			brandname = dynamicpackageId.get(JSON_PROP_COMPANYNAME).toString();

			if (dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO).has(JSON_PROP_TIMESPAN)) {
				travelDate = dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO).getJSONObject(JSON_PROP_TIMESPAN)
						.getString(JSON_PROP_TRAVELSTARTDATE);
			}
		}
		JSONObject packageDetail = new JSONObject();

		packageDetail.put(JSON_PROP_PRODUCTNAME, productName);
		// TODO
		// :productFlavorName,flavorType,productType,salesDate,tourType
		// HARDCODED set as "".Need to find from where values will come
		// below four values not in updated request
		packageDetail.put("productFlavorName", "");
		packageDetail.put("flavorType", "");
		packageDetail.put("productType", "");
		packageDetail.put("brandName", brandname);

		// Reprice Started---

		if (operationName.equalsIgnoreCase("reprice")) {

			String brandNameRes = dynamicPkgJson.getString("brandName");
			String tourCodeRes = dynamicPkgJson.getString("tourCode");
			String subTourCodeRes = dynamicPkgJson.getString("subTourCode");

			JSONArray flightDetailsArr = new JSONArray();
			JSONObject applicableOnProductFlightJson = new JSONObject();

			// get Airline component from request
			JSONArray dynamicPackageArr = reqBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE);

			String dynamicPkgAction = null;

			for (int i = 0; i < dynamicPackageArr.length(); i++) {
				JSONObject dynamicPkgJsonReq = dynamicPackageArr.getJSONObject(i);

				String brandNameReq = dynamicPkgJsonReq.getString("brandName");
				String tourCodeReq = dynamicPkgJsonReq.getString("tourCode");
				String subTourCodeReq = dynamicPkgJsonReq.getString("subTourCode");

				if (brandNameRes.equalsIgnoreCase(brandNameReq) && tourCodeRes.equalsIgnoreCase(tourCodeReq)
						&& subTourCodeRes.equalsIgnoreCase(subTourCodeReq)) {
					JSONArray airComponentArr = dynamicPkgJsonReq.getJSONObject(JSON_PROP_COMPONENTS)
							.getJSONArray(JSON_PROP_AIR_COMPONENT);
					for (int j = 0; j < airComponentArr.length(); j++) {
						JSONObject airComponentJson = airComponentArr.getJSONObject(j);

						JSONObject airItinJson = airComponentJson.getJSONObject(JSON_PROP_AIRITINERARY);
						JSONArray originDestinationOptionsArr = airItinJson.getJSONArray(JSON_PROP_ORIGINDESTOPTIONS);
						for (int k = 0; k < originDestinationOptionsArr.length(); k++) {
							JSONObject originDestinationOptionJson = originDestinationOptionsArr.getJSONObject(k);

							JSONArray flightSegmentArr = originDestinationOptionJson
									.getJSONArray(JSON_PROP_FLIGHTSEGMENT);
							for (int l = 0; l < flightSegmentArr.length(); l++) {
								JSONObject flightSegmentJson = flightSegmentArr.getJSONObject(l);
								JSONObject operatingAirlineJSON = flightSegmentJson.getJSONObject("operatingAirline");
								String flightDetails = operatingAirlineJSON.getString("airlineCode")
										+ operatingAirlineJSON.getString("flightNumber") + " "
										+ flightSegmentJson.getString("originLocation") + "/"
										+ flightSegmentJson.getString("destinationLocation");
								flightDetailsArr.put(flightDetails);
							}
						}
					}
					if(dynamicPkgJsonReq.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_HOTEL_COMPONENT)) {
					JSONArray hotelCompRqArr = dynamicPkgJsonReq.getJSONObject(JSON_PROP_COMPONENTS)
							.optJSONArray(JSON_PROP_HOTEL_COMPONENT);
					if (hotelCompRqArr!= null && hotelCompRqArr.length() > 0) {
						roomMap= new HashMap<String, ArrayList<String>>();
						for (int k = 0; k < hotelCompRqArr.length(); k++) {

							JSONArray roomStayArr = hotelCompRqArr.getJSONObject(k).getJSONObject("roomStays")
									.getJSONArray("roomStay");
							dynamicPkgAction = hotelCompRqArr.getJSONObject(k).getString(JSON_PROP_DYNAMICPKGACTION);

							for (int l = 0; l < roomStayArr.length(); l++) {
								JSONObject roomStayJson = roomStayArr.getJSONObject(l);
								String roomType = roomStayJson.optString(JSON_PROP_ROOMTYPE);
								String roomCategory = roomStayJson.optString("roomCategory");

								ArrayList<String> list = new ArrayList<String>();
								list.add(roomType);
								list.add(roomCategory);
								roomMap.put(dynamicPkgAction, list);

							}
						}

					}
				}
					if (dynamicPkgJsonReq.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_CRUISE_COMPONENT)) {
						JSONArray cruiseCompRqArr = dynamicPkgJsonReq.getJSONObject(JSON_PROP_COMPONENTS)
								.optJSONArray(JSON_PROP_CRUISE_COMPONENT);
						if (cruiseCompRqArr != null && cruiseCompRqArr.length() > 0) {
							roomMap= new HashMap<String, ArrayList<String>>();
							for (int k = 0; k < cruiseCompRqArr.length(); k++) {
								dynamicPkgAction = cruiseCompRqArr.getJSONObject(k)
										.getString(JSON_PROP_DYNAMICPKGACTION);
								JSONArray categoryOptionsArr = cruiseCompRqArr.getJSONObject(k).getJSONArray("categoryOptions");
								for (int l = 0; l < categoryOptionsArr.length(); l++) {

									JSONArray categoryOptionArr = categoryOptionsArr.getJSONObject(l).getJSONArray("categoryOption");

									for (int m = 0; m < categoryOptionArr.length(); m++) {
										JSONObject categoryOptionJson = categoryOptionArr.getJSONObject(m);
										String roomType = categoryOptionJson.optString(JSON_PROP_TYPE);
										String roomCategory = categoryOptionJson.optString("ratePlanCategory");

										ArrayList<String> list = new ArrayList<String>();
										list.add(roomType);
										list.add(roomCategory);
										roomMap.put(dynamicPkgAction, list);

									}
								}
							}
						}
					}

				}
			}

			//
			// creating priceArray of unique elements to create Wem response
			Map<String, JSONObject> priceMap = new ConcurrentHashMap<String, JSONObject>();

			JSONArray componentArr = dynamicPkgJson.getJSONArray(JSON_PROP_COMPONENTS);
			for (int c = 0; c < componentArr.length(); c++) {
				JSONObject componentJson = componentArr.getJSONObject(c);
				String paxType = componentJson.getString("paxType");

				if (paxType.equalsIgnoreCase("ADT")) {
					
					JSONArray adtPriceArray = componentJson.getJSONArray(JSON_PROP_PRICE);
					//JSONArray newPriceAdtArr = HolidaysUtil.createNewPriceArr(componentJson, priceMap);

					createApplicableOnForPax(applicableOnProductArr, adtPriceArray, flightDetailsArr,
							applicableOnProductFlightJson, paxType);
				} else if (paxType.equalsIgnoreCase("CHD")) {
					
					JSONArray chdPriceArray = componentJson.getJSONArray(JSON_PROP_PRICE);
					//JSONArray newPriceChdArr = HolidaysUtil.createNewPriceArr(componentJson, priceMap);

					createApplicableOnForPax(applicableOnProductArr, chdPriceArray, flightDetailsArr,
							applicableOnProductFlightJson, paxType);

				} else if (paxType.equalsIgnoreCase("INF")) {
					
					JSONArray infPriceArray = componentJson.getJSONArray(JSON_PROP_PRICE);
					//JSONArray newPriceInfArr = HolidaysUtil.createNewPriceArr(componentJson, priceMap);

					createApplicableOnForPax(applicableOnProductArr, infPriceArray, flightDetailsArr,
							applicableOnProductFlightJson, paxType);
				}
			}

		}

		// For AddService --start
		if (operationName.equalsIgnoreCase("addservice")) {

			JSONArray airItineraryPricingInfoArray = dynamicPkgJson.getJSONObject(JSON_PROP_COMPONENTS)
					.getJSONObject(JSON_PROP_AIR_COMPONENT).getJSONArray(JSON_PROP_AIRITINERARYPRICINGINFO);

			for (int i = 0; i < airItineraryPricingInfoArray.length(); i++) {
				JSONObject airItineraryPricingInfoJson = airItineraryPricingInfoArray.getJSONObject(i);

				JSONObject applicableOnProductJson = getAirItineraryPricingInfo(airItineraryPricingInfoJson);

				applicableOnProductArr.put(applicableOnProductJson);

			}

		}
		// For AddService --end
		packageDetail.put("applicableOnProducts", applicableOnProductArr);
		if (!travelDate.equals("") || !travelDate.isEmpty()) {

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date date = formatter.parse(travelDate);
				packageDetail.put("travelDate", mDateFormat.format(date));

			} catch (ParseException e) {

				e.printStackTrace();
			}
		} else {
			packageDetail.put("travelDate", "");
		}
		packageDetail.put("salesDate", "");
		packageDetail.put("tourType", "");
		packageDetail.put("bookingType", "Online");

		packageDetail.put("roomDetails", getBRMSroomDetailsJSON(dynamicPkgJson, operationName));

		return packageDetail;
	}

	private static void createApplicableOnForPax(JSONArray applicableOnProductArr, JSONArray newPriceArr,
			JSONArray flightDetailsArr, JSONObject applicableOnProductFlightJson, String paxType) {
		String key;
		String roomTypeStr = "", roomCategoryStr = "";
		for (int i = 0; i < newPriceArr.length(); i++) {
			JSONObject priceJson = newPriceArr.getJSONObject(i);

			String rateDescriptionText = priceJson.getJSONObject("rateDescription").getString(JSON_PROP_TEXT);
			if (!(rateDescriptionText.contains("Room"))) {
				if (rateDescriptionText.contains("Night")) {
					String applicableOnStr = rateDescriptionText.substring(0, rateDescriptionText.indexOf("Night") + 5)
							.replaceAll("[^a-zA-Z]", "");
					for (Map.Entry<String, ArrayList<String>> entry : roomMap.entrySet()) {
						key = entry.getKey();
						if (key.contains(applicableOnStr)) {
							String roomType = entry.getValue().get(0);
							String roomCategory = entry.getValue().get(1);

							JSONObject applicableOnProductJson = createApplicableOnProduct(applicableOnStr, priceJson,
									paxType, roomType, roomCategory);
							applicableOnProductArr.put(applicableOnProductJson);

						}
					}
				}

				else if (rateDescriptionText.contains("Trip Protection") || rateDescriptionText.contains("Insurance")) {
					String applicableOnStr = DYNAMICPKGACTION_INSURANCE;
					JSONObject applicableOnProductJson = createApplicableOnProduct(applicableOnStr, priceJson, paxType,
							roomTypeStr, roomCategoryStr);
					applicableOnProductArr.put(applicableOnProductJson);

				}

				// Code for Extras/ Upgrades
				else if (rateDescriptionText.equalsIgnoreCase("Extras") || rateDescriptionText.contains("Upgrade")) {
					String applicableOnStr = DYNAMICPKGACTION_EXTRAS;
					JSONObject applicableOnProductJson = createApplicableOnProduct(applicableOnStr, priceJson, paxType,
							roomTypeStr, roomCategoryStr);
					applicableOnProductArr.put(applicableOnProductJson);

				}

				// For Arrival and Departure Transfers
				else if (rateDescriptionText.contains("Transfer")) {

					String applicableOnStr = DYNAMICPKGACTION_TRANSFER;
					JSONObject applicableOnProductJson = createApplicableOnProduct(applicableOnStr, priceJson, paxType,
							roomTypeStr, roomCategoryStr);
					applicableOnProductArr.put(applicableOnProductJson);
				}

				else if (rateDescriptionText.contains("Surcharge")) {
					String applicableOnStr = "Surcharge";
					JSONObject applicableOnProductJson = createApplicableOnProduct(applicableOnStr, priceJson, paxType,
							roomTypeStr, roomCategoryStr);
					applicableOnProductArr.put(applicableOnProductJson);

				}
				// for adding all airline fares
				for (int j = 0; j < flightDetailsArr.length(); j++) {

					if (rateDescriptionText.contains(flightDetailsArr.getString(j))) {

						String amountAfterTax = priceJson.getJSONObject("total").getString(JSON_PROP_AMOUNTAFTERTAX);
						String amountBeforeTax = priceJson.getJSONObject("total").getString(JSON_PROP_AMOUNTBEFORETAX);
						totalAirAmountAfterTax = totalAirAmountAfterTax + Double.parseDouble(amountAfterTax);
						totalAirAmountBeforeTax = totalAirAmountBeforeTax + Double.parseDouble(amountBeforeTax);

						JSONArray taxJsonArr = getTotalTaxes(priceJson, airTaxMap);

						if (j != flightDetailsArr.length() - 1) {
							continue;
						}

						applicableOnProductFlightJson.put(JSON_PROP_PRODUCTNAME, "PackageDepartureAndArrivalFlights");
						applicableOnProductFlightJson.put(JSON_PROP_TOTALFARE, String.valueOf(totalAirAmountAfterTax));
						JSONObject fareBreakUpJson = new JSONObject();
						fareBreakUpJson.put(JSON_PROP_BASEFARE, String.valueOf(totalAirAmountBeforeTax));

						fareBreakUpJson.put(JSON_PROP_TAXDETAILS, taxJsonArr);
						applicableOnProductFlightJson.put(JSON_PROP_FAREBREAKUP, fareBreakUpJson);
						applicableOnProductArr.put(applicableOnProductFlightJson);

					}

				}

			}
		}
	}

	private static JSONObject getAirItineraryPricingInfo(JSONObject airItineraryPricingInfoJson) {
		JSONObject applicableOnProductJson = new JSONObject();

		String baseFareAmount = airItineraryPricingInfoJson.getJSONObject(JSON_PROP_ITINTOTALFARE)
				.getJSONObject(JSON_PROP_BASEFARE).getString(JSON_PROP_AMOUNT);
		String totalFareAmount = airItineraryPricingInfoJson.getJSONObject(JSON_PROP_ITINTOTALFARE)
				.getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_AMOUNT);

		applicableOnProductJson.put(JSON_PROP_PRODUCTNAME, DYNAMICPKGACTION_AIR_DEPARR);
		applicableOnProductJson.put(JSON_PROP_TOTALFARE, totalFareAmount);

		JSONObject fareBreakUpJson = new JSONObject();
		fareBreakUpJson.put(JSON_PROP_BASEFARE, baseFareAmount);

		fareBreakUpJson.put(JSON_PROP_TAXDETAILS, getBRMSAddServiceTaxDetails(airItineraryPricingInfoJson));
		applicableOnProductJson.put(JSON_PROP_FAREBREAKUP, fareBreakUpJson);

		return applicableOnProductJson;
	}

	private static JSONArray getBRMSAddServiceTaxDetails(JSONObject airItineraryPricingInfoJson) {
		JSONArray taxJsonArr = new JSONArray();
		JSONArray taxArr = airItineraryPricingInfoJson.getJSONObject(JSON_PROP_ITINTOTALFARE)
				.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);

		for (int k = 0; k < taxArr.length(); k++) {
			JSONObject taxJson = taxArr.getJSONObject(k);
			String taxName = taxJson.getString(JSON_PROP_TAXNAME);
			String taxValue = taxJson.getString(JSON_PROP_AMOUNT);

			taxJson.put(JSON_PROP_TAXNAME, taxName);
			taxJson.put(JSON_PROP_TAXVALUE, taxValue);

			taxJsonArr.put(taxJson);

		}

		return taxJsonArr;
	}

	public static JSONArray getTotalTaxes(JSONObject priceJson, Map<String, Double> taxMap) {
		JSONArray taxArr = priceJson.getJSONObject("total").getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
		JSONArray taxJsonArr = new JSONArray();

		// for adding all air taxes
		for (int k = 0; k < taxArr.length(); k++) {
			JSONObject tax = taxArr.getJSONObject(k);
			String taxName = tax.getString(JSON_PROP_TAXDESCRIPTION);
			if (taxName.equals("TaxesAndPortCharges")) {
				String taxesAndPortChargesValue = tax.getString(JSON_PROP_AMOUNT);

				if (taxMap.containsKey("TaxesAndPortCharges")) {
					double totalTaxesAndPortCharges = taxMap.get("TaxesAndPortCharges")
							+ Double.parseDouble(taxesAndPortChargesValue);
					taxMap.put("TaxesAndPortCharges", totalTaxesAndPortCharges);
				} else {
					double totalTaxesAndPortCharges = Double.parseDouble(taxesAndPortChargesValue);
					taxMap.put("TaxesAndPortCharges", totalTaxesAndPortCharges);
				}
			}

			if (taxName.equals("Surcharge")) {
				String surchargeValue = tax.getString(JSON_PROP_AMOUNT);
				if (taxMap.containsKey("Surcharge")) {
					double totalSurcharge = taxMap.get("Surcharge") + Double.parseDouble(surchargeValue);
					taxMap.put("Surcharge", totalSurcharge);
				} else {
					double totalSurcharge = Double.parseDouble(surchargeValue);
					taxMap.put("Surcharge", totalSurcharge);
				}
			}

		}

		for (Entry<String, Double> entry : taxMap.entrySet()) {
			JSONObject taxJson = new JSONObject();
			taxJson.put(JSON_PROP_TAXVALUE, String.valueOf(entry.getValue()));
			taxJson.put(JSON_PROP_TAXNAME, String.valueOf(entry.getKey()));
			taxJsonArr.put(taxJson);

		}
		return taxJsonArr;
	}

	private static JSONObject createApplicableOnProduct(String applicableOnStr, JSONObject priceJson, String paxType,
			String roomTypeStr, String roomCategoryStr) {
		JSONObject applicableOnProductJson = new JSONObject();
		
		String rateDescriptionName = priceJson.getJSONObject("rateDescription").getString("name");
		String rateDescriptionType = priceJson.getJSONObject("rateDescription").getString("text");
		
		applicableOnProductJson.put(JSON_PROP_PRODUCTNAME, applicableOnStr);
		applicableOnProductJson.put("componentName", rateDescriptionName);
		applicableOnProductJson.put("componentType", rateDescriptionType);
		
		BigDecimal amountAfterTax = priceJson.getJSONObject("total").getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
		applicableOnProductJson.put(JSON_PROP_TOTALFARE, amountAfterTax);

		applicableOnProductJson.put("passengerType", paxType);
		applicableOnProductJson.put("roomType", roomTypeStr);
		applicableOnProductJson.put("roomCategory", roomCategoryStr);

		BigDecimal amountBeforeTax = priceJson.getJSONObject("total").getBigDecimal(JSON_PROP_AMOUNTBEFORETAX);
		JSONObject fareBreakUpJson = new JSONObject();
		fareBreakUpJson.put(JSON_PROP_BASEFARE, amountBeforeTax);

		fareBreakUpJson.put(JSON_PROP_TAXDETAILS, getBRMStaxDetailsJSON(priceJson));
		applicableOnProductJson.put(JSON_PROP_FAREBREAKUP, fareBreakUpJson);
		return applicableOnProductJson;
	}

	private static JSONArray getBRMSroomDetailsJSON(JSONObject packageDetailJson, String operationName) {

		String tourEndCity = "";
		JSONArray roomDetailsArr = new JSONArray();

		if (packageDetailJson.has(JSON_PROP_GLOBALINFO)) {
			JSONObject dynamicpackageId = packageDetailJson.getJSONObject(JSON_PROP_GLOBALINFO)
					.getJSONObject(JSON_PROP_DYNAMICPKGID);

			if (dynamicpackageId.has(JSON_PROP_TPA_EXTENSIONS)) {
				tourEndCity = dynamicpackageId.getJSONObject(JSON_PROP_TPA_EXTENSIONS).getJSONObject(JSON_PROP_PKGS_TPA)
						.getJSONObject(JSON_PROP_TOURDETAILS).get(JSON_PROP_TOURENDCITY).toString();
			}
		}

		// add service passenger details
		if (operationName.equalsIgnoreCase("addservice")) {
			JSONObject roomDetails = new JSONObject();

			getCommonRoomDetails("", roomDetails);
			roomDetails.put("roomCategory", "");
			roomDetails.put("roomType", "");

			JSONArray passengerDetailsArr = new JSONArray();

			JSONObject passengerDetailsJson = new JSONObject();
			passengerDetailsJson.put("totalFare", "");
			passengerDetailsJson.put("passengerType", "");

			JSONObject fareBreakUpJson = new JSONObject();
			fareBreakUpJson.put("baseFare", "");

			JSONArray taxDetailsArray = new JSONArray();

			JSONObject taxDetailsJson = new JSONObject();

			taxDetailsJson.put("taxValue", "");
			taxDetailsJson.put("taxName", "");

			taxDetailsArray.put(taxDetailsJson);

			fareBreakUpJson.put("taxDetails", taxDetailsArray);
			passengerDetailsJson.put("fareBreakUp", fareBreakUpJson);
			passengerDetailsArr.put(passengerDetailsJson);
			roomDetails.put("passengerDetails", passengerDetailsArr);
			roomDetailsArr.put(roomDetails);

		}
		if (operationName.equalsIgnoreCase("search") || operationName.equalsIgnoreCase("getDetails")) {

			if (packageDetailJson.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_HOTEL_COMPONENT)) {
				
				JSONObject hotelCompJson = packageDetailJson.getJSONObject(JSON_PROP_COMPONENTS)
								.getJSONObject(JSON_PROP_HOTEL_COMPONENT);

				if(hotelCompJson!=null && hotelCompJson.length()>0) {
					String dynamicPkgAction = hotelCompJson.getString(JSON_PROP_DYNAMICPKGACTION);
					if (dynamicPkgAction.equalsIgnoreCase("PackageAccomodation")) {
						JSONArray roomStayArr = hotelCompJson.getJSONArray(JSON_PROP_ROOMSTAY);

						for (int j = 0; j < roomStayArr.length(); j++) {
							JSONObject roomDetails = new JSONObject();
							JSONObject roomStayJson = roomStayArr.getJSONObject(j);
							JSONArray roomrateArr = roomStayJson.getJSONArray(JSON_PROP_ROOMRATE);
							String roomType = roomStayJson.get(JSON_PROP_ROOMTYPE).toString();
							roomDetails.put(JSON_PROP_ROOMTYPE, roomType);
							// TODO :supplierRateType
							getCommonRoomDetails(tourEndCity, roomDetails);
							// getting Type="PREMIUM" in case of Monograms
							JSONObject roomRateJson = null;
							for (int k = 0; k < roomrateArr.length(); k++) {
								roomRateJson = roomrateArr.getJSONObject(k);
								String roomCategory = roomRateJson.optString("ratePlanCategory");
								roomDetails.put("roomCategory", roomCategory);

							}
							roomDetails.put("passengerDetails", getBRMSpassengerDetailsJSON(roomStayJson));

							roomDetailsArr.put(roomDetails);

						}
					}
					
				}}
			
		
			if (packageDetailJson.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_CRUISE_COMPONENT)) {
				JSONObject cruiseCompJson = packageDetailJson.getJSONObject(JSON_PROP_COMPONENTS)
						.getJSONObject(JSON_PROP_CRUISE_COMPONENT);

				if(cruiseCompJson!=null && cruiseCompJson.length()>0) {
					String dynamicPkgAction = cruiseCompJson.getString(JSON_PROP_DYNAMICPKGACTION);
					if (dynamicPkgAction.equalsIgnoreCase("PackageCruise")) {
						JSONArray categoryOptionsArr = cruiseCompJson.getJSONArray(JSON_PROP_CATEGORYOPTIONS);
						for (int k = 0; k < categoryOptionsArr.length(); k++) {
							JSONObject categoryOptionsJson = categoryOptionsArr.getJSONObject(k);
							JSONArray categoryOptionArr = categoryOptionsJson.getJSONArray(JSON_PROP_CATEGORYOPTION);

							for (int j = 0; j < categoryOptionArr.length(); j++) {
								JSONObject categoryOptionJson = categoryOptionArr.getJSONObject(j);
								JSONObject roomDetails = new JSONObject();
								String type = categoryOptionJson.getString(JSON_PROP_TYPE);
								roomDetails.put(JSON_PROP_ROOMTYPE, type);
								getCommonRoomDetails(tourEndCity, roomDetails);
								roomDetails.put("roomCategory", "");
								roomDetails.put("passengerDetails", getCruisePassengerDetailsJSON(categoryOptionJson));
								roomDetailsArr.put(roomDetails);

							}
						}

					}
					
			}}
			
			
		} else if (operationName.equalsIgnoreCase("reprice")) {
			JSONArray componentsArr = packageDetailJson.getJSONArray(JSON_PROP_COMPONENTS);
			String key, roomCategoryStr = "";
			for (int c = 0; c < componentsArr.length(); c++) {
				JSONObject componentJson = componentsArr.getJSONObject(c);
				String paxType = componentJson.getString("paxType");
				JSONArray priceArray = componentJson.getJSONArray(JSON_PROP_PRICE);
				for (int i = 0; i < priceArray.length(); i++) {
					JSONObject priceJson = priceArray.getJSONObject(i);
					String rateDescriptionText = priceJson.getJSONObject("rateDescription").getString(JSON_PROP_TEXT);

					if (rateDescriptionText.contains("Room")) {

						JSONObject roomDetails = new JSONObject();
						String roomType = rateDescriptionText.substring(0, rateDescriptionText.lastIndexOf(" "));
						roomDetails.put(JSON_PROP_ROOMTYPE, roomType);
						getCommonRoomDetails(tourEndCity, roomDetails);
						for (Map.Entry<String, ArrayList<String>> entry : roomMap.entrySet()) {
							key = entry.getKey();
							if (key.equals("PackageAccomodation")) {
								roomCategoryStr = entry.getValue().get(1);

							}
						}
						roomDetails.put("roomCategory", roomCategoryStr);
						roomDetails.put("passengerDetails", getRepricePassengerDetailsJSON(priceJson, paxType));
						roomDetailsArr.put(roomDetails);

					}
				}
			}
		}

		return roomDetailsArr;

	}

	public static void getCommonRoomDetails(String tourEndCity, JSONObject roomDetails) {
		roomDetails.put("supplierRateType", "");
		roomDetails.put("supplierRateCode", "");
		roomDetails.put("toContinent", "Asia");
		roomDetails.put("toCountry", "India");
		roomDetails.put("toCity", tourEndCity);
		roomDetails.put("toState", "");
	}

	private static JSONArray getRepricePassengerDetailsJSON(JSONObject priceJson, String paxType) {
		JSONArray passengerDetailsArr = new JSONArray();
		JSONObject paxDetail = new JSONObject();
		BigDecimal totalFare = priceJson.getJSONObject(JSON_PROP_TOTAL).getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
		paxDetail.put(JSON_PROP_TOTALFARE, totalFare);
		paxDetail.put(JSON_PROP_PASSENGERTYPE, paxType);

		JSONObject fareBreakUp = new JSONObject();
		BigDecimal baseFare = priceJson.getJSONObject(JSON_PROP_TOTAL).getBigDecimal(JSON_PROP_AMOUNTBEFORETAX);
		fareBreakUp.put(JSON_PROP_BASEFARE, baseFare);
		fareBreakUp.put(JSON_PROP_TAXDETAILS, getBRMStaxDetailsJSON(priceJson));
		paxDetail.put(JSON_PROP_FAREBREAKUP, fareBreakUp);
		passengerDetailsArr.put(paxDetail);

		return passengerDetailsArr;

	}

	private static JSONArray getCruisePassengerDetailsJSON(JSONObject categoryOptionJson) {
		JSONArray passengerDetailsArr = new JSONArray();
		JSONObject paxDetail = new JSONObject();

		JSONArray totalArr = categoryOptionJson.getJSONArray(JSON_PROP_TOTAL);

		for (int i = 0; i < totalArr.length(); i++) {
			JSONObject totalJson = totalArr.getJSONObject(i);
			String totalFare = totalJson.getString(JSON_PROP_AMOUNTAFTERTAX);
			paxDetail.put(JSON_PROP_TOTALFARE, totalFare);
			String type = totalJson.get(JSON_PROP_TYPE).toString();
			if (type.equals("34")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "ADT");
			}
			if (type.equals("35")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "CHD");
			}
			if (type.equals("36")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "INF");
			}
			if (type.equals("NA")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "");
			}


			JSONObject fareBreakUp = new JSONObject();
			String baseFare = totalJson.getString(JSON_PROP_AMOUNTBEFORETAX);
			fareBreakUp.put(JSON_PROP_BASEFARE, baseFare);
			fareBreakUp.put(JSON_PROP_TAXDETAILS, getBRMSCruisetaxDetailsJSON(totalJson));
			paxDetail.put(JSON_PROP_FAREBREAKUP, fareBreakUp);
			passengerDetailsArr.put(paxDetail);

		}

		return passengerDetailsArr;
	}

	private static JSONArray getBRMSCruisetaxDetailsJSON(JSONObject totalJson) {
		JSONArray taxArr = totalJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
		JSONArray taxJsonArr = new JSONArray();

		for (int i = 0; i < taxArr.length(); i++) {
			JSONObject taxJson = new JSONObject();
			JSONObject tax = taxArr.getJSONObject(i);
			String taxName = tax.getString(JSON_PROP_TAXDESCRIPTION);
			taxJson.put(JSON_PROP_TAXNAME, taxName);
			String taxValue = tax.getString(JSON_PROP_AMOUNT);
			taxJson.put(JSON_PROP_TAXVALUE, taxValue);

			taxJsonArr.put(taxJson);
		}
		return taxJsonArr;
	}

	private static JSONArray getBRMSpassengerDetailsJSON(JSONObject roomStayJson) {
		JSONArray passengerDetailsArr = new JSONArray();
		JSONArray roomrateArr = roomStayJson.getJSONArray(JSON_PROP_ROOMRATE);

		for (int i = 0; i < roomrateArr.length(); i++) {
			JSONObject paxDetail = new JSONObject();
			JSONObject roomRateJson = roomrateArr.getJSONObject(i);
			// TODO :when Type=34 set as "ADT" and Type=35 set as "CHD" in case of
			// Contiki/Trafalgar/Monograms response

			String type = roomRateJson.getJSONObject(JSON_PROP_TOTAL).get(JSON_PROP_TYPE).toString();
			if (type.equals("34")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "ADT");
			}
			if (type.equals("35")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "CHD");
			}
			if (type.equals("36")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "INF");
			}
			if (type.equals("NA")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "");
			}

			String totalFare = roomRateJson.getJSONObject(JSON_PROP_TOTAL).getString(JSON_PROP_AMOUNTAFTERTAX);
			paxDetail.put(JSON_PROP_TOTALFARE, totalFare);

			JSONObject fareBreakUp = new JSONObject();
			String baseFare = roomRateJson.getJSONObject(JSON_PROP_TOTAL).getString(JSON_PROP_AMOUNTBEFORETAX);
			fareBreakUp.put(JSON_PROP_BASEFARE, baseFare);
			fareBreakUp.put(JSON_PROP_TAXDETAILS, getBRMStaxDetailsJSON(roomRateJson));
			paxDetail.put(JSON_PROP_FAREBREAKUP, fareBreakUp);
			passengerDetailsArr.put(paxDetail);

		}

		return passengerDetailsArr;
	}

	private static JSONArray getBRMStaxDetailsJSON(JSONObject paxDetailJson) {
		JSONArray taxArr = paxDetailJson.getJSONObject("total").getJSONObject(JSON_PROP_TAXES).getJSONArray("tax");
		JSONArray taxJsonArr = new JSONArray();

		for (int i = 0; i < taxArr.length(); i++) {
			JSONObject taxJson = new JSONObject();
			JSONObject tax = taxArr.getJSONObject(i);
			String taxName = tax.getString(JSON_PROP_TAXDESCRIPTION);
			taxJson.put(JSON_PROP_TAXNAME, taxName);
			String taxValue = tax.getString(JSON_PROP_AMOUNT);
			taxJson.put(JSON_PROP_TAXVALUE, taxValue);

			taxJsonArr.put(taxJson);
		}
		return taxJsonArr;
	}

}
