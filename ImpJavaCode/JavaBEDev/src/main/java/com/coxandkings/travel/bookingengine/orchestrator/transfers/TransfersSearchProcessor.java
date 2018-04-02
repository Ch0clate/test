package com.coxandkings.travel.bookingengine.orchestrator.transfers;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.transfers.TransfersConfig;
import com.coxandkings.travel.bookingengine.orchestrator.bus.SeatMapProcessor;
import com.coxandkings.travel.bookingengine.orchestrator.transfers.SupplierCommercials;
import com.coxandkings.travel.bookingengine.orchestrator.transfers.TransfersSearchProcessor;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class TransfersSearchProcessor implements TransfersConstants {

	private static final Logger logger = LogManager.getLogger(TransfersSearchProcessor.class);

	public static void getSupplierResponseGroundServiceJSON(Element resBodyElem, JSONArray groundServiceJsonArr)
			throws Exception {
		getSupplierResponseGroundServiceJSON(resBodyElem, groundServiceJsonArr, false, 0);
	}

	public static void getSupplierResponseGroundServiceJSON(Element resBodyElem, JSONArray groundServicesJsonArr,
			boolean generateBookRefIdx, int bookRefIdx) throws Exception {
		// boolean isCombinedReturnJourney =
		// Boolean.valueOf(XMLUtils.getValueAtXPath(resBodyElem,
		// "./@CombinedReturnJourney"));
		Element[] groundServiceElems = XMLUtils.getElementsAtXPath(resBodyElem,
				"./ota:GroundServices/ota:GroundService");

		for (Element groundServiceElem : groundServiceElems) {
			JSONObject groundServiceJson = getGroundServiceJSON(groundServiceElem);
			String suppId = XMLUtils.getValueAtXPath((Element) resBodyElem.getParentNode(), "./tran1:SupplierID");
			try {
				groundServiceJson.put(JSON_PROP_SUPPREF, suppId.split("_")[1] != null ? suppId.split("_")[1] : suppId);
			} catch (Exception e) {
				groundServiceJson.put(JSON_PROP_SUPPREF, suppId);
			}
			groundServicesJsonArr.put(groundServiceJson);
		
		}
	}

	private static JSONObject getGroundServiceJSON(Element groundServiceElem) {
		JSONObject groundServiceJson = new JSONObject();

		JSONArray serviceArr = new JSONArray();
		Element[] serviceElems = XMLUtils.getElementsAtXPath(groundServiceElem, "./ota:Service");
		for (Element serviceElem : serviceElems) {
			serviceArr.put(getServiceJson(serviceElem));

		}
		groundServiceJson.put(JSON_PROP_SERVICE, serviceArr);

		Element transferInformationElems = XMLUtils.getFirstElementAtXPath(groundServiceElem,
				"./ota:TransferInformation");
		groundServiceJson.put(JSON_PROP_TRANSFERINFORMATION, getTransferInformation(transferInformationElems));

		Element restrictionsElem = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./ota:Restrictions");
		groundServiceJson.put(JSON_PROP_RESTRICTIONS, getRestrictionsJSON(restrictionsElem));

		Element totalChargeElem = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./ota:TotalCharge");
		groundServiceJson.put(JSON_PROP_TOTALCHARGE, getTotalChargeJSON(totalChargeElem));

		Element referenceElem1[] = XMLUtils.getElementsAtXPath(groundServiceElem, "./ota:Reference");
		groundServiceJson.put(JSON_PROP_REFERENCE1, getReference1JSON(referenceElem1));

		Element timelinesElem = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./ota:Timelines");
		groundServiceJson.put(JSON_PROP_TIMELINES, getTimelinesJSON(timelinesElem));

		Element feesElem = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./ota:Fees");
		groundServiceJson.put(JSON_PROP_FEES, getFeesJSON(feesElem));
		
		/*Element rideEstimate = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./ota:RideEstimate");
		JSONObject cateJson = new JSONObject();
		cateJson.put("category", XMLUtils.getValueAtXPath(rideEstimate, "./@Category"));*/
		
		/*if(groundServiceJson.getString("supplierRef").equalsIgnoreCase("ola")) {
			
			Element TPAExtention = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./ota:Reference/ota:TPA_Extensions");
			groundServiceJson.put(JSON_PROP_TPAEXTENTION, getTPA_Extention(TPAExtention));
			
		}*/
		
		
		return groundServiceJson;
	}

	/*private static JSONObject getTPA_Extention(Element tPAExtention) {
		JSONObject tpaExtention = new JSONObject();
		Element farebreakupsElem = XMLUtils.getFirstElementAtXPath(tPAExtention, "./ota:FareBreakup");
		JSONObject farebreakupsJson = new JSONObject();
		JSONArray farebreakupArr = new JSONArray();
		Element[] farebreakupElem = XMLUtils.getElementsAtXPath(farebreakupsElem, "./ota:fare_breakup");
		
		for(Element fareElem : farebreakupElem) {
		JSONObject fareJson = new JSONObject();
		fareJson.put("waiting_cost_per_minute", XMLUtils.getValueAtXPath(fareElem,"./@waiting_cost_per_minute"));
		fareJson.put("cost_per_distance", XMLUtils.getValueAtXPath(fareElem,"./@cost_per_distance"));
		fareJson.put("minimum_time", XMLUtils.getValueAtXPath(fareElem,"./@minimum_time"));
		fareJson.put("base_fare", XMLUtils.getValueAtXPath(fareElem,"./@base_fare"));
		fareJson.put("rates_higher_than_usual", XMLUtils.getValueAtXPath(fareElem,"./@rates_higher_than_usual"));
		fareJson.put("minimum_fare", XMLUtils.getValueAtXPath(fareElem,"./@minimum_fare"));
		fareJson.put("fare", XMLUtils.getValueAtXPath(fareElem,"./@fare"));
		fareJson.put("type", XMLUtils.getValueAtXPath(fareElem,"./@type"));
		fareJson.put("minimum_distance", XMLUtils.getValueAtXPath(fareElem,"./@minimum_distance"));
		fareJson.put("distance_unit", XMLUtils.getValueAtXPath(fareElem,"./@distance_unit"));
		fareJson.put("currency_code", XMLUtils.getValueAtXPath(fareElem,"./@currency_codeb"));
		
		farebreakupArr.put(fareJson);
		
		
		
		}
		farebreakupsJson.put("fareBreakup", farebreakupArr);
		return tpaExtention;
	}*/

	private static JSONObject getFeesJSON(Element feesElem) {
		JSONObject feesJson = new JSONObject();
		feesJson.put("description", XMLUtils.getValueAtXPath(feesElem, "./@Description"));
		return feesJson;
	}

	private static JSONObject getTransferInformation(Element transferInformationElem) {
		JSONObject transferInfoJson = new JSONObject();

		JSONArray imageListArr = new JSONArray();
		Element[] imageListElems = XMLUtils.getElementsAtXPath(transferInformationElem, "./ota:ImageList");
		for (Element imageListElem : imageListElems) {
			imageListArr.put(getImageListJson(imageListElem));
		}
		transferInfoJson.put("imageList", imageListArr);

		JSONArray descriptionArr = new JSONArray();
		Element[] descriptionElems = XMLUtils.getElementsAtXPath(transferInformationElem, "./ota:Description");
		for (Element descriptionElem : descriptionElems) {
			descriptionArr.put(getDescriptionJson(descriptionElem));
		}
		transferInfoJson.put("description", descriptionArr);

		JSONArray guidelinesArr = new JSONArray();
		Element[] guidelinesElems = XMLUtils.getElementsAtXPath(transferInformationElem, "./ota:Guidelines");
		for (Element guidelinesElem : guidelinesElems) {
			guidelinesArr.put(getGuidelinesJson(guidelinesElem));
		}
		transferInfoJson.put("guidelines", guidelinesArr);

		return transferInfoJson;
	}

	private static JSONObject getGuidelinesJson(Element guidelinesElem) {
		JSONObject guidelinesJson = new JSONObject();
		guidelinesJson.put("id", XMLUtils.getValueAtXPath(guidelinesElem, "./@id"));
		guidelinesJson.put("description", XMLUtils.getValueAtXPath(guidelinesElem, "./@description"));
		guidelinesJson.put("detailedDescription", XMLUtils.getValueAtXPath(guidelinesElem, "./@detailedDescription"));
		return guidelinesJson;
	}

	private static JSONObject getDescriptionJson(Element descriptionElem) {
		JSONObject descriptionJson = new JSONObject();
		descriptionJson.put("type", XMLUtils.getValueAtXPath(descriptionElem, "./@type"));
		descriptionJson.put("languagecode", XMLUtils.getValueAtXPath(descriptionElem, "./@languagecode"));
		descriptionJson.put("text", XMLUtils.getValueAtXPath(descriptionElem, "./@text"));
		return descriptionJson;
	}

	private static JSONObject getImageListJson(Element imageListElem) {
		JSONObject imageListJson = new JSONObject();
		imageListJson.put("type", XMLUtils.getValueAtXPath(imageListElem, "./@type"));
		imageListJson.put("url", XMLUtils.getValueAtXPath(imageListElem, "./@URL"));
		return imageListJson;
	}

	private static JSONObject getServiceJson(Element serviceElem) {
		JSONObject serviceJson = new JSONObject();
		Element locationElem = XMLUtils.getFirstElementAtXPath(serviceElem, "./ota:Location");
		serviceJson.put("serviceType", XMLUtils.getValueAtXPath(locationElem, "./@ServiceType"));

		Element pickupElem = XMLUtils.getFirstElementAtXPath(locationElem, "./ota:Pickup");
		serviceJson.put("pickLocationCode", XMLUtils.getValueAtXPath(pickupElem, "./@LocationCode"));

		Element dropoffElem = XMLUtils.getFirstElementAtXPath(locationElem, "./ota:Dropoff");
		serviceJson.put("dropLocationCode", XMLUtils.getValueAtXPath(dropoffElem, "./@LocationCode"));

		Element vehicleTypeElem = XMLUtils.getFirstElementAtXPath(serviceElem, "./ota:VehicleType");
		serviceJson.put("description", XMLUtils.getValueAtXPath(vehicleTypeElem, "./@Description"));
		serviceJson.put("maximumPassengers", XMLUtils.getValueAtXPath(vehicleTypeElem, "./@MaximumPassengers"));
		serviceJson.put("uniqueID", XMLUtils.getValueAtXPath(vehicleTypeElem, "./@UniqueID"));

		Element transferInformationElem = XMLUtils.getFirstElementAtXPath(serviceElem, "./ota:TransferInformation");
		Element descriptionElem = XMLUtils.getFirstElementAtXPath(transferInformationElem, "./ota:Description");
		serviceJson.put("text", XMLUtils.getValueAtXPath(descriptionElem, "./@text"));
		serviceJson.put("type", XMLUtils.getValueAtXPath(descriptionElem, "./@type"));

		return serviceJson;
	}

	private static JSONObject getTimelinesJSON(Element timelinesElem) {
		JSONObject timelinesJson = new JSONObject();
		timelinesJson.put("time", XMLUtils.getValueAtXPath(timelinesElem, "./@Time"));
		timelinesJson.put("type", XMLUtils.getValueAtXPath(timelinesElem, "./@type"));
		return timelinesJson;
	}

	private static JSONArray getReference1JSON(Element[] referenceElem) {
		JSONArray reference1Arr = new JSONArray();

		for (Element refer : referenceElem) {
			JSONObject reference1Json = new JSONObject();
			reference1Json.put("id", XMLUtils.getValueAtXPath(refer, "./@ID"));
			reference1Json.put("id_Context", XMLUtils.getValueAtXPath(refer, "./@ID_Context"));
			reference1Json.put("type", XMLUtils.getValueAtXPath(refer, "./@Type"));
			Element tpa_ExtensionsElem = XMLUtils.getFirstElementAtXPath(refer, "./ota:TPA_Extensions");
			if (tpa_ExtensionsElem != null) {
				String travelTime = XMLUtils.getValueAtXPath(tpa_ExtensionsElem,
						"./tran1:RideEstimate/tran1:TravelTime");
				reference1Json.put("travelTime", travelTime);

				String timeFormat = XMLUtils.getValueAtXPath(tpa_ExtensionsElem,
						"./tran1:RideEstimate/tran1:TimeFormat");
				reference1Json.put("timeFormat", timeFormat);

				// TODO:ADD According to OLA Supplier

			}
			reference1Arr.put(reference1Json);

		}
		return reference1Arr;
	}

	private static JSONObject getTotalChargeJSON(Element totalChargeElem) {
		JSONObject totalChargeJson = new JSONObject();
		totalChargeJson.put("currencyCode", XMLUtils.getValueAtXPath(totalChargeElem, "./@CurrencyCode"));
		totalChargeJson.put("amount",
				Utils.convertToInt(XMLUtils.getValueAtXPath(totalChargeElem, "./@EstimatedTotalAmount"), 0));
		/*totalChargeJson.put("rateTotalAmount",
				Utils.convertToInt(XMLUtils.getValueAtXPath(totalChargeElem, "./@RateTotalAmount"), 0));*/
		return totalChargeJson;
	}

	private static JSONObject getRestrictionsJSON(Element restrictionsElem) {
		JSONObject restrictionsJson = new JSONObject();
		restrictionsJson.put("cancellationPenaltyInd",
				XMLUtils.getValueAtXPath(restrictionsElem, "./@CancellationPenaltyInd"));
		return restrictionsJson;
	}

	public static String process(JSONObject reqJson) throws Exception {
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null)
				? reqJson.optJSONObject(JSON_PROP_REQHEADER)
				: new JSONObject();
		JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null)
				? reqJson.optJSONObject(JSON_PROP_REQBODY)
				: new JSONObject();

		try {
			OperationConfig opConfig = TransfersConfig.getOperationConfig("search");
			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();

			TrackingContext.setTrackingContext(reqJson);
			UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);

			List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_TRANSPORT,
					PROD_CATEG_SUBTYPE_TRANSFER);
			createHeader(reqHdrJson, reqElem);

			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./tran1:RequestHeader/com:SupplierCredentialsList");
			for (ProductSupplier prodSupplier : prodSuppliers) {
				suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc));
			}
/*
			 int sequence = 1;
	            Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem, "./air:RequestHeader/com:SupplierCredentialsList");
	            for (ProductSupplier prodSupplier : prodSuppliers) {
	                suppCredsListElem.appendChild(prodSupplier.toElement(ownerDoc, sequence++));
	            }*/
			
			//Element passengersElem = null;
			Element posElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./tran:RequestBody/ns:OTA_GroundAvailRQ/ns:POS");
			Element ground = (Element) posElem.getParentNode();
			Element sourceElem = XMLUtils.getFirstElementAtXPath(posElem, "./ns:Source");
			// TODO: hardcode for ISOCurrency!get it from where?
			sourceElem.setAttribute("ISOCurrency", "");

			// TODO: for loop
			JSONArray serviceArr = reqBodyJson.getJSONArray("service");
			int serviceArrLen = serviceArr.length();
			int i;
			for (i = 0; i < serviceArrLen; i++) {
				/*
				 * Element serviceElem = XMLUtils.getFirstElementAtXPath(reqElem,
				 * "./tran:RequestBody/ns:OTA_GroundAvailRQ/ns:Service");
				 */
				Element serviceElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Service");
				JSONObject serviceObj = serviceArr.getJSONObject(i);
				serviceElem.setAttribute("ServiceType", serviceObj.getString("serviceType"));
				Element pickupElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Pickup");
				JSONObject pickupObj = serviceObj.getJSONObject("pickup");
				pickupElem.setAttribute("DateTime", pickupObj.getString("dateTime"));

				if (!pickupObj.getString("locationType").equals("") && pickupObj.getString("locationType") != null) {
					pickupElem.setAttribute("LocationType", pickupObj.getString("locationType"));
				}
				if (!pickupObj.getString("locationCode").equals("") && pickupObj.getString("locationCode") != null) {
					pickupElem.setAttribute("LocationCode", pickupObj.getString("locationCode"));
				}

				if (!pickupObj.getJSONObject("address").equals("") && pickupObj.getJSONObject("address") != null) {
					JSONObject addrObj = pickupObj.getJSONObject("address");
					Element addressElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Address");
					Element cityNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:CityName");
					cityNameElem.setTextContent(addrObj.getString("cityName"));
					addressElem.appendChild(cityNameElem);

					if (!addrObj.getString("countryCode").equals("") && addrObj.getString("countryCode") != null) {
						Element countryCodeElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:CountryCode");
						countryCodeElem.setTextContent(addrObj.getString("countryCode"));
						addressElem.appendChild(countryCodeElem);
					}
					if (!addrObj.getString("locationName").equals("") && addrObj.getString("locationName") != null) {
						Element locationNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:LocationName");
						locationNameElem.setTextContent(addrObj.getString("locationName"));
						addressElem.appendChild(locationNameElem);
					}
					pickupElem.appendChild(addressElem);

				}
				serviceElem.appendChild(pickupElem);

				Element dropoffElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Dropoff");
				JSONObject dropoffObj = serviceObj.getJSONObject("dropoff");
				dropoffElem.setAttribute("DateTime", dropoffObj.getString("dateTime"));

				if (!dropoffObj.getString("locationType").equals("") && dropoffObj.getString("locationType") != null) {
					dropoffElem.setAttribute("LocationType", dropoffObj.getString("locationType"));
				}
				if (!dropoffObj.getString("locationCode").equals("") && dropoffObj.getString("locationCode") != null) {
					dropoffElem.setAttribute("LocationCode", dropoffObj.getString("locationCode"));
				}

				if (!dropoffObj.getJSONObject("address").equals("") && dropoffObj.getJSONObject("address") != null) {
					JSONObject addrObj = dropoffObj.getJSONObject("address");
					Element addressElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Address");
					Element cityNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:CityName");
					cityNameElem.setTextContent(addrObj.getString("cityName"));
					addressElem.appendChild(cityNameElem);
					if (!addrObj.getString("countryCode").equals("") && addrObj.getString("countryCode") != null) {
						Element countryCodeElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:CountryCode");
						countryCodeElem.setTextContent(addrObj.getString("countryCode"));
						addressElem.appendChild(countryCodeElem);
					}
					if (!addrObj.getString("locationName").equals("") && addrObj.getString("locationName") != null) {
						Element locationNameElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:LocationName");
						locationNameElem.setTextContent(addrObj.getString("locationName"));
						addressElem.appendChild(locationNameElem);
					}
					dropoffElem.appendChild(addressElem);

				}
				serviceElem.appendChild(dropoffElem);
				//ground.insertBefore(serviceElem,passengersElem);
				ground.appendChild(serviceElem);
				
			}

			JSONArray paxInfoArr = reqBodyJson.getJSONArray("paxInfo");
			int paxInfoArrLen = paxInfoArr.length();
			int k;
			for (k = 0; k < paxInfoArrLen; k++) {
				JSONObject paxInfoObj = paxInfoArr.getJSONObject(k);
			/*	Element passengersElem = XMLUtils.getFirstElementAtXPath(reqElem,
						"./tran:RequestBody/ns:OTA_GroundAvailRQ/ns:Passengers");*/
				Element passengersElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Passengers");
				passengersElem.setAttribute("Quantity", paxInfoObj.getString("quantity"));
				if (!paxInfoObj.getString("age").equals("") && paxInfoObj.getString("age") != null) {

					passengersElem.setAttribute("Age", paxInfoObj.getString("age"));
					ground.appendChild(passengersElem);
				}

			}

			if (null != reqBodyJson.optJSONArray("passengerPrefs")) {
				JSONArray passengerPrefsArr = reqBodyJson.getJSONArray("passengerPrefs");

				int passengerPrefsArrLen = passengerPrefsArr.length();

				int l;
				for (l = 0; l < passengerPrefsArrLen; l++) {
					JSONObject passengerPrefsObj = passengerPrefsArr.getJSONObject(l);
					if (null != passengerPrefsArr) {
						/*Element passengerPrefsElem = XMLUtils.getFirstElementAtXPath(reqElem,
								"./tran:RequestBody/ns:OTA_GroundAvailRQ/ns:PassengerPrefs");*/
						Element passengerPrefsElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:PassengerPrefs");
						passengerPrefsElem.setAttribute("MaximumBaggage",
								passengerPrefsObj.getString("maximumBaggage"));

						Element languageElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Language");
						languageElem.setAttribute("Language", passengerPrefsObj.getString("language"));

						passengerPrefsElem.appendChild(languageElem);
						ground.appendChild(passengerPrefsElem);
					}

				}
			}
			
			if(reqBodyJson.optString("tripIndicator").equalsIgnoreCase("share")) {
				Element vehiclePrefsElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:VehiclePrefs");
				
				Element typeElem = ownerDoc.createElementNS(Constants.NS_OTA, "ns:Type");
				typeElem.setTextContent(reqBodyJson.optString("vehicleType").toLowerCase());
				vehiclePrefsElem.appendChild(typeElem);
				ground.appendChild(vehiclePrefsElem);
			}

			/*if (null != reqBodyJson.optJSONArray("vehiclePrefs")) {
				JSONArray vehiclePrefsArr = reqBodyJson.getJSONArray("vehiclePrefs");

				int vehiclePrefsArrLen = vehiclePrefsArr.length();

				int n;
				for (n = 0; n < vehiclePrefsArrLen; n++) {
					JSONObject vehiclerPrefsObj = vehiclePrefsArr.getJSONObject(n);
					if (null != vehiclePrefsArr) {
						Element vehiclePrefsElem = XMLUtils.getFirstElementAtXPath(reqElem,
								"./tran:RequestBody/ns:OTA_GroundAvailRQ/ns:VehiclePrefs");
						if (!vehiclerPrefsObj.getString("maximumPassengers").equals("")
								&& vehiclerPrefsObj.getString("maximumPassengers") != null) {
							vehiclePrefsElem.setAttribute("MaximumPassengers",
									vehiclerPrefsObj.getString("maximumPassengers"));
						}
						if (!vehiclerPrefsObj.getString("code").equals("")
								&& vehiclerPrefsObj.getString("code") != null) {
							vehiclePrefsElem.setAttribute("Code", vehiclerPrefsObj.getString("code"));
						}
						if (!vehiclerPrefsObj.getString("uniqueID").equals("")
								&& vehiclerPrefsObj.getString("uniqueID") != null) {
							vehiclePrefsElem.setAttribute("UniqueID", vehiclerPrefsObj.getString("uniqueID"));
						}
						ground.appendChild(vehiclePrefsElem);
					}

				}

			}*/

			Element resElem = null;
			// logger.trace(String.format("SI XML Request = %s",
			// XMLTransformer.toString(reqElem)));
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					TransfersConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}
			JSONObject resBodyJson = new JSONObject();
			JSONArray groundServiceJsonArr = new JSONArray();
			Element[] wrapperElems = XMLUtils.getElementsAtXPath(resElem,
					"./tran:ResponseBody/tran1:OTA_GroundAvailRSWrapper");
			for (Element wrapperElem : wrapperElems) { 
				Element resBodyElem = XMLUtils.getFirstElementAtXPath(wrapperElem, "./ota:OTA_GroundAvailRS");
				getSupplierResponseGroundServiceJSON(resBodyElem, groundServiceJsonArr);
			}
			resBodyJson.put(JSON_PROP_GROUNDSERVICES, groundServiceJsonArr);

			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));

			// Call BRMS Supplier and Client Commercials
			JSONObject resSupplierJson = SupplierCommercials.getSupplierCommercialsV2(reqJson, resJson);
			JSONObject resClientJson = ClientCommercials.getClientCommercialsV2(resSupplierJson);
			//String tripInd = reqBodyJson.getString("tripIndicator");
				calculatePrices(reqJson, resJson,resSupplierJson,resClientJson, true , usrCtx);
			pushSuppFaresToRedisAndRemove(resJson);

			return resJson.toString();
		} catch (Exception x) {
			x.printStackTrace();
			return "{\"status\": \"ERROR\"}";
		}
	}



	public static void pushSuppFaresToRedisAndRemove(JSONObject resJson) {
		JSONObject resHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);

		JSONArray groundServiceJsonArr = resBodyJson.optJSONArray(JSON_PROP_GROUNDSERVICES);

		Map<String, String> reprcSuppFaresMap = new HashMap<String, String>();
		for (int i = 0; i < groundServiceJsonArr.length(); i++) {
			JSONObject groundServiceJson = groundServiceJsonArr.getJSONObject(i);
			/*JSONObject suppPriceInfoJson = new JSONObject();*/
			JSONObject suppPriceInfoJson =	groundServiceJson.optJSONObject(JSON_PROP_SUPPPRICEINFO);
			groundServiceJson.remove(JSON_PROP_SUPPPRICEINFO);
			
			JSONObject totalPricingInfo =groundServiceJson.optJSONObject(JSON_PROP_CLIENTCOMMTOTAL);
			groundServiceJson.remove(JSON_PROP_CLIENTCOMMTOTAL);
			
			
					//supplier commercial
					/*groundServiceJson.optJSONArray(JSON_PROP_SUPPPRICEINFO);
			groundServiceJson.remove(JSON_PROP_SUPPPRICEINFO);
					//client Commercial
					groundServiceJson.optJSONArray(JSON_PROP_CLIENTCOMMTOTAL);
			groundServiceJson.remove(JSON_PROP_CLIENTCOMMTOTAL);
			
			groundServiceJson.remove(JSON_PROP_BOOKREFIDX);*/

			if (suppPriceInfoJson == null) {
				// TODO: This should never happen. Log a warning message here.
				continue;
			}
			
			
			/*//getting client commercial
			JSONArray clientCommercialItinInfoJsonArr = groundServiceJson.optJSONArray(JSON_PROP_CLIENTCOMMITININFO);
			groundServiceJson.remove(JSON_PROP_CLIENTCOMMITININFO);
			
			JSONArray clientCommercialItinTotalJsonArr = groundServiceJson.optJSONArray(JSON_PROP_CLIENTCOMMTOTAL);
			groundServiceJson.remove(JSON_PROP_CLIENTCOMMTOTAL);
			
			if ( suppPriceInfoJson == null) {
				// TODO: This should never happen. Log a warning message here.
				continue;
			}*/
		/*	JSONArray clientCommercialItinTotalJsonArr = groundServiceJson.optJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMS);
			groundServiceJson.remove(JSON_PROP_CLIENTENTITYTOTALCOMMS);
			
			JSONObject suppPriceBookInfoJson = new JSONObject();
			suppPriceBookInfoJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceInfoJson);
			//suppPriceBookInfoJson.put(JSON_PROP_BOOKREFS, bookRefJson);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTCOMMITININFO, clientCommercialItinInfoJsonArr);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientCommercialItinTotalJsonArr);*/
			
		/*	JSONObject JSON_PROP_SUPPPRICEINFO = new JSONObject();
			JSON_PROP_SUPPPRICEINFO.put(JSON_PROP_SUPPPRICEINFO, JSON_PROP_SUPPPRICEINFO);*/
			/*suppPriceBookInfoJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceInfoJson);
			//suppPriceBookInfoJson.put(JSON_PROP_BOOKREFS, bookRefJson);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTCOMMITININFO, clientCommercialItinInfoJsonArr);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTENTITYTOTALCOMMS, clientCommercialItinTotalJsonArr);*/
			JSONObject suppPriceBookInfoJson = new JSONObject();
			suppPriceBookInfoJson.put(JSON_PROP_SUPPPRICEINFO, suppPriceInfoJson);
			suppPriceBookInfoJson.put(JSON_PROP_CLIENTCOMMTOTAL, totalPricingInfo);
			reprcSuppFaresMap.put(getRedisKeyForGroundService(groundServiceJson), suppPriceBookInfoJson.toString());
		}
		
		
		String redisKey = resHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT_TRANSFERS);
		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		redisConn.hmset(redisKey, reprcSuppFaresMap);
		redisConn.pexpire(redisKey, (long) (TransfersConfig.getRedisTTLMinutes() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);
	}

	public static String getRedisKeyForGroundService(JSONObject groundServiceJson) {
		List<String> keys = new ArrayList<>();

		keys.add(groundServiceJson.optString(JSON_PROP_SUPPREF));
		JSONArray serviceJsonArr = groundServiceJson.getJSONArray(JSON_PROP_SERVICE);
		for (int i = 0; i < serviceJsonArr.length(); i++) {
			JSONObject serviceJson = serviceJsonArr.getJSONObject(i);
			keys.add(serviceJson.optString("maximumPassengers"));
			keys.add(serviceJson.optString("description"));
			keys.add(serviceJson.optString("uniqueID"));

		}

		String key = keys.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining("|"));
		return key;
	}

	public static void createHeader(JSONObject reqHdrJson, Element reqElem) {
		Element sessionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./tran1:RequestHeader/com:SessionID");
		sessionElem.setTextContent(reqHdrJson.getString(JSON_PROP_SESSIONID));

		Element transactionElem = XMLUtils.getFirstElementAtXPath(reqElem, "./tran1:RequestHeader/com:TransactionID");
		transactionElem.setTextContent(reqHdrJson.getString(JSON_PROP_TRANSACTID));

		Element userElem = XMLUtils.getFirstElementAtXPath(reqElem, "./tran1:RequestHeader/com:UserID");
		userElem.setTextContent(reqHdrJson.getString(JSON_PROP_USERID));
	}

	public static void calculatePrices(JSONObject reqJson, JSONObject resJson , JSONObject suppCommResJson, JSONObject clientCommResJson, boolean retainSuppFares, UserContext usrCtx) {
		
		   Map<String, String> suppCommToTypeMap = getSupplierCommercialsAndTheirType(suppCommResJson);
	        Map<String, String> clntCommToTypeMap = getClientCommercialsAndTheirType(clientCommResJson);
		JSONArray resGroundServicesArr = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("groundServices");

		JSONArray ccommSuppBRIJsonArr = clientCommResJson.getJSONObject("result").getJSONObject("execution-results")
				.getJSONArray("results").getJSONObject(0).getJSONObject("value")
				.getJSONObject("cnk.transfers_commercialscalculationengine.clienttransactionalrules.Root")
				.getJSONArray("businessRuleIntake");
		Map<String, JSONArray> ccommSuppBRIJsonMap = new HashMap<String, JSONArray>();
		for (int i = 0; i < ccommSuppBRIJsonArr.length(); i++) {
			JSONObject ccommSuppBRIJson = ccommSuppBRIJsonArr.getJSONObject(i);
			String suppID = ccommSuppBRIJson.getJSONObject("commonElements").getString("supplier");
			JSONArray ccommGroundServJsonArr = ccommSuppBRIJson.getJSONArray("transfersDetails");
			ccommSuppBRIJsonMap.put(suppID, ccommGroundServJsonArr);
		}
		Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();
		for (int i = 0; i < resGroundServicesArr.length(); i++) {
			JSONObject resGroundServicesJson = resGroundServicesArr.getJSONObject(i);
			//Adding clientCommercialInfo
			JSONObject suppCommercialItinInfoJson = new JSONObject();
			JSONObject clientCommercialItinInfoJson= new JSONObject();
			JSONObject totalAmountSupp = resGroundServicesJson.getJSONObject("totalCharge");
			JSONObject totalChargeJson = new JSONObject();
			totalChargeJson.put(JSON_PROP_AMOUNT, totalAmountSupp.get(JSON_PROP_AMOUNT));
			totalChargeJson.put(JSON_PROP_CURRENCYCODE, totalAmountSupp.get(JSON_PROP_CURRENCYCODE));
			//clientCommercialItinInfoJson.put(JSON_PROP_SUPPTOTALFARE, totalChargeJson);
			//suppCommercialItinInfoArr.put(clientCommercialItinInfoJson);
			// JSONObject ccommJrnyDtlsJson = ccommJrnyDtlsJsonArr.getJSONObject(i);
			String suppID = resGroundServicesJson.getString(JSON_PROP_SUPPREF).substring(0, 1).toUpperCase()
					+ resGroundServicesJson.getString(JSON_PROP_SUPPREF).substring(1).toLowerCase();
			// input.substring(0, 1).toUpperCase() + input.substring(1);
			JSONArray ccommGroundServJsonArr = ccommSuppBRIJsonMap.get(suppID);
			
			if (ccommGroundServJsonArr == null) {
				// TODO: This should never happen. Log a information message here.
				logger.info(String.format(
						"BRMS/ClientComm: \"businessRuleIntake\" JSON element for supplier %s not found.", suppID));
				continue;
			}

			int idx = 0;
			if (suppIndexMap.containsKey(suppID)) {
				idx = suppIndexMap.get(suppID) + 1;
			}
			suppIndexMap.put(suppID, idx);
			JSONObject ccommGroundServJson = ccommGroundServJsonArr.getJSONObject(idx);

			BigDecimal totalFareAmt = new BigDecimal(0);

			// Search this paxType in client commercials
			
			JSONObject ccommTransPsgrDtlJson = ccommGroundServJson.getJSONArray("passengerDetails").getJSONObject(0);

			if (ccommTransPsgrDtlJson == null) {
				// TODO: Log a crying message here. Ideally this part of the code will never be
				// reached.
				continue;
			}
			
			
			
			// From the passenger type client commercial JSON, retrieve calculated client
			// commercial commercials
			JSONArray clientEntityCommJsonArr = ccommTransPsgrDtlJson.optJSONArray("entityCommercials");
			if (clientEntityCommJsonArr == null) {
				// TODO: Refine this warning message. Maybe log some context information also.
				logger.warn("Client commercials calculations not found");
				continue;
			}
			
			
			JSONArray clientCommercials= new JSONArray();
			JSONObject totalCharge = resGroundServicesJson.getJSONObject(JSON_PROP_TOTALCHARGE);
			JSONObject suppTotalCharge = new JSONObject(totalCharge.toString());
			for (int k = (clientEntityCommJsonArr.length() - 1); k >= 0; k--) {
				JSONObject clientCommercial= new JSONObject();
				JSONArray clientEntityCommercialsJsonArr=new JSONArray();
				JSONObject clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(k);
				JSONObject markupCalcJson = clientEntityCommJson.optJSONObject("markUpCommercialDetails");
				if (markupCalcJson == null) {
					continue;
				}

				//JSONObject fareBreakupJson = markupCalcJson.getJSONObject("fareBreakUp");
			//	BigDecimal baseFare = fareBreakupJson.getBigDecimal("baseFare");
				BigDecimal totalFare = markupCalcJson.getBigDecimal("totalFare");
				//totalfare withcomm
			
			//	JSONArray ccommTaxDetailsJsonArr = fareBreakupJson.getJSONArray("taxDetails");
				// JSONArray taxDetailsJsonArr =
				// resGroundServicesJson.getJSONArray("TaxDetails");

				/*JSONArray taxJsonArr = new JSONArray();
				BigDecimal taxesTotal = new BigDecimal(0);
				// if(taxDetailsJsonArr.length()==0) {
				for (int l = 0; l < ccommTaxDetailsJsonArr.length(); l++) {
					JSONObject ccommTaxDetailJson = ccommTaxDetailsJsonArr.optJSONObject(l);
					
					 * if (ccommTaxDetailJson != null) taxesTotal =
					 * taxesTotal.add(ccommTaxDetailJson.getBigDecimal("taxValue"));
					 
				}*/
				//resGroundServicesJson.put("TaxDetails", ccommTaxDetailsJsonArr);
				//totalCharge.put("rateTotalAmount", baseFare);
				totalCharge.put(JSON_PROP_AMOUNT, totalFare);
				JSONObject totalFarecommJson = new JSONObject();
				totalFarecommJson.put(JSON_PROP_AMOUNT, totalFare);
				totalFarecommJson.put(JSON_PROP_CURRENCYCODE, totalCharge.get(JSON_PROP_CURRENCYCODE));
				//totalCharge.put(JSON_PROP_CURRENCYCODE, );
				
				// TODO: Should this be made conditional? Only in case when (retainSuppFares == true)
				//supplier commercial
				
				JSONArray suppCommJsonArr = new JSONArray();
				JSONArray ccommSuppCommJsonArr = ccommTransPsgrDtlJson.optJSONArray(JSON_PROP_COMMDETAILS);
				String suppCcyCode = suppTotalCharge.optString(JSON_PROP_CURRENCYCODE);
				if (ccommSuppCommJsonArr == null) {
					logger.warn(String.format("No supplier commercials found for supplier %s", suppID));
					return;
				}
				for (int x=0; x < ccommSuppCommJsonArr.length(); x++) {
					JSONObject ccommSuppCommJson = ccommSuppCommJsonArr.getJSONObject(x);
					JSONObject suppCommJson = new JSONObject();
					suppCommJson.put(JSON_PROP_COMMNAME, ccommSuppCommJson.getString(JSON_PROP_COMMNAME));
					suppCommJson.put(JSON_PROP_COMMTYPE, suppCommToTypeMap.get(ccommSuppCommJson.getString(JSON_PROP_COMMNAME)));
					suppCommJson.put(JSON_PROP_COMMAMOUNT, ccommSuppCommJson.getBigDecimal(JSON_PROP_COMMAMOUNT));
					suppCommJson.put(JSON_PROP_COMMCCY, ccommSuppCommJson.optString(JSON_PROP_COMMCCY, suppCcyCode));
					suppCommJsonArr.put(suppCommJson);
				}
			
				suppCommercialItinInfoJson.put(JSON_PROP_SUPPCOMMTOTALS, suppCommJsonArr);
				//suppCommercialItinInfoArr.put(suppCommercialItinInfoJson);
				//resGroundServicesJson.put(JSON_PROP_SUPPCOMM, suppCommJsonArr);
				suppCommercialItinInfoJson.put(JSON_PROP_SUPPTOTALFARE, totalChargeJson);
				//client commercial
				JSONObject clientEntityDetailsJson=new JSONObject();
		    	JSONObject userCtxJson=usrCtx.toJSON();
		    	JSONArray clientEntityDetailsArr = userCtxJson.getJSONArray(JSON_PROP_CLIENTCOMMENTITYDTLS);
				for(int y=0;y<clientEntityDetailsArr.length();y++) {
					/*if(clientEntityDetailsArr.getJSONObject(y).opt(JSON_PROP_COMMENTITYID)!=null)
					{
					if(clientEntityDetailsArr.getJSONObject(y).opt(JSON_PROP_COMMENTITYID).toString().equalsIgnoreCase(clientEntityCommJson.get("entityName").toString()))
					{
						clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
					}
					}*/
				//TODO:Add a check later
					
					clientEntityDetailsJson=clientEntityDetailsArr.getJSONObject(y);
					
					
				}
				String commercialCurrency="";
				//markup commercialcalc clientCommercial
				clientCommercial.put(JSON_PROP_COMMTYPE, clntCommToTypeMap.get(markupCalcJson.get(JSON_PROP_COMMNAME).toString()));
				clientCommercial.put(JSON_PROP_COMMAMOUNT, markupCalcJson.get(JSON_PROP_COMMAMOUNT).toString());
				clientCommercial.put(JSON_PROP_COMMNAME,markupCalcJson.get(JSON_PROP_COMMNAME));
				
				if((markupCalcJson.get(JSON_PROP_COMMCALCPCT).toString())==null)
				{
					clientCommercial.put(JSON_PROP_COMMCCY,markupCalcJson.get(JSON_PROP_COMMCCY).toString());
				}
				else {
					clientCommercial.put(JSON_PROP_COMMCCY,commercialCurrency);
				}
				clientCommercial.put(JSON_PROP_COMMCCY,markupCalcJson.optString(JSON_PROP_COMMCCY, commercialCurrency));
				clientEntityCommercialsJsonArr.put(clientCommercial);
			
				clientEntityDetailsJson.put(JSON_PROP_CLIENTCOMM, clientEntityCommercialsJsonArr);
				//clientEntityDetailsJson.put(JSON_PROP_TOTALFARE, totalFarecommJson);
				clientCommercials.put(clientEntityDetailsJson);
				
				clientCommercialItinInfoJson.put(JSON_PROP_CLIENTENTITYCOMMS, clientCommercials);
				clientCommercialItinInfoJson.put(JSON_PROP_TOTALFARE, totalFarecommJson);
				
				
				break;
			}
			if (retainSuppFares) {
				/*JSONObject suppPricingInfoJson = new JSONObject();

				suppPricingInfoJson.put(JSON_PROP_CURRENCYCODE, suppTotalCharge.optString(JSON_PROP_CURRENCYCODE));
				suppPricingInfoJson.put(JSON_PROP_AMOUNT, suppTotalCharge.getBigDecimal("Amount"));
			*/
			//	suppPricingInfoJson.put(JSON_PROP_RATEAMOUNT, suppTotalCharge.getBigDecimal("rateTotalAmount"));
				/*
				 * suppPricingInfoJson.put(JSON_PROP_AMOUNT,
				 * suppTotalCharge.optBigDecimal("estimatedTotalAmount",null));
				 */
				
				//resGroundServicesJson.put(JSON_PROP_SUPPCOMMTOTAL,suppCommJsonArr);
				resGroundServicesJson.put(JSON_PROP_CLIENTCOMMTOTAL, clientCommercialItinInfoJson);
				resGroundServicesJson.put(JSON_PROP_SUPPPRICEINFO, suppCommercialItinInfoJson);

			}
		}
		logger.trace(String.format("supplierResponse after supplierItinFare = %s", resJson.toString()));
	}
	
    private static Map<String, String> getSupplierCommercialsAndTheirType(JSONObject suppCommResJson) {
        JSONArray commHeadJsonArr = null;
        JSONObject commHeadJson = null;
        Map<String, String> commToTypeMap = new HashMap<String, String>();
        JSONArray scommBRIJsonArr = getSupplierCommercialsBusinessRuleIntakeJSONArray(suppCommResJson);
        for (int i=0; i < scommBRIJsonArr.length(); i++) {
        	if ((commHeadJsonArr = scommBRIJsonArr.getJSONObject(i).optJSONArray(JSON_PROP_COMMHEAD)) == null) {
        		logger.warn("No commercial heads found in supplier commercials");
        		continue;
        	}
        	
        	for (int j=0; j < commHeadJsonArr.length(); j++) {
        		commHeadJson = commHeadJsonArr.getJSONObject(j);
        		commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME), commHeadJson.getString(JSON_PROP_COMMTYPE));
        	}
        }
        
        return commToTypeMap;
    }
    private static JSONArray getSupplierCommercialsBusinessRuleIntakeJSONArray(JSONObject commResJson) {
    	return commResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.transfers_commercialscalculationengine.suppliertransactionalrules.Root").getJSONArray("businessRuleIntake");
    }
    // Retrieve commercials head array from client commercials and find type (Receivable, Payable) for commercials 
    private static Map<String, String> getClientCommercialsAndTheirType(JSONObject clientCommResJson) {
        JSONArray commHeadJsonArr = null;
        JSONObject commHeadJson = null;
        
        JSONArray entityDtlsJsonArr = null;
        Map<String, String> commToTypeMap = new HashMap<String, String>();
        JSONArray ccommBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(clientCommResJson);
        for (int i=0; i < ccommBRIJsonArr.length(); i++) {
        	if ((entityDtlsJsonArr = ccommBRIJsonArr.getJSONObject(i).optJSONArray(JSON_PROP_ENTITYDETAILS)) == null) {
        		continue;
        	}
        	for (int j=0; j < entityDtlsJsonArr.length(); j++) {
            	if ((commHeadJsonArr = entityDtlsJsonArr.getJSONObject(j).optJSONArray(JSON_PROP_COMMHEAD)) == null) {
            		logger.warn("No commercial heads found in client commercials");
            		continue;
            	}
            	
            	for (int k=0; k < commHeadJsonArr.length(); k++) {
            		commHeadJson = commHeadJsonArr.getJSONObject(k);
            		commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME), commHeadJson.getString(JSON_PROP_COMMTYPE));
            	}
        	}
        }
        
        return commToTypeMap;
    }
    private static JSONArray getClientCommercialsBusinessRuleIntakeJSONArray(JSONObject commResJson) {
    	return commResJson.getJSONObject("result").getJSONObject("execution-results").getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject("cnk.transfers_commercialscalculationengine.clienttransactionalrules.Root").getJSONArray("businessRuleIntake");
    }
}
