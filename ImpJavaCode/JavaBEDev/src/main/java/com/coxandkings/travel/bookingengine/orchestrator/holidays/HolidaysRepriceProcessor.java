package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.Utils;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class HolidaysRepriceProcessor implements HolidayConstants {

	private static final Logger logger = LogManager.getLogger(HolidaysRepriceProcessor.class);
	private static BigDecimal totalFare = new BigDecimal("0");
	private static String paxType;
	private static BigDecimal totalPrice = new BigDecimal(0);
	
	@SuppressWarnings("unused")
	public static String process(JSONObject requestJson) {
		try {

			// HolidaysConfig.loadConfig();
			OperationConfig opConfig = HolidaysConfig.getOperationConfig("reprice");

			// clone shell si request from ProductConfig collection HOLIDAYS document
			Element requestElement = (Element) opConfig.getRequestXMLShell().cloneNode(true);

			// create Document object associated with request node, this Document object is
			// also used to create new nodes.
			Document ownerDoc = requestElement.getOwnerDocument();

			TrackingContext.setTrackingContext(requestJson);

			JSONObject requestHeader = requestJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject requestBody = requestJson.getJSONObject(JSON_PROP_REQBODY);

			// CREATE SI REQUEST HEADER
			String sessionID = requestHeader.getString(JSON_PROP_SESSIONID);
			String transactionID = requestHeader.getString(JSON_PROP_TRANSACTID);
			String userID = requestHeader.getString(JSON_PROP_USERID);

			Element requestHeaderElement = XMLUtils.getFirstElementAtXPath(requestElement, "./pac:RequestHeader");

			Element userElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:UserID");
			userElement.setTextContent(userID);

			Element sessionElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:SessionID");
			sessionElement.setTextContent(sessionID);

			Element transactionElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:TransactionID");
			transactionElement.setTextContent(transactionID);

			// get the list of all supplier in packages
			// UserContext usrCtx = UserContext.getUserContextForSession(sessionID);
			UserContext usrCtx = UserContext.getUserContextForSession(requestHeader);

			Element supplierCredentialsList = XMLUtils.getFirstElementAtXPath(requestHeaderElement,
					"./com:SupplierCredentialsList");

			// CREATE SI REQUEST BODY
			Element requestBodyElement = XMLUtils.getFirstElementAtXPath(requestElement, "./pac1:RequestBody");

			Element wrapperElement = XMLUtils.getFirstElementAtXPath(requestBodyElement,
					"./pac:OTA_DynamicPkgAvailRQWrapper");
			requestBodyElement.removeChild(wrapperElement);

			int sequence = 0;
			JSONArray dynamicPackageArr = requestBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE);
			for (int i = 0; i < dynamicPackageArr.length(); i++) {
				JSONObject dynamicPackageObj = dynamicPackageArr.getJSONObject(i);
				sequence++;

				String supplierID = dynamicPackageObj.getString("supplierID");
				Element supWrapperElement = null;
				Element otaAvailRQ = null;
				Element searchCriteria = null;
				Element dynamicPackage = null;

				/*
				 * //Making request Header for particular supplierID
				 * 
				 * //getting credentials and operation urls for particular supplier
				 * ProductSupplier productSupplier = usrCtx.getSupplierForProduct(PRODUCT,
				 * supplierID); if (productSupplier == null) { throw new
				 * Exception(String.format("Product supplier %s not found for user/client",
				 * supplierID)); }
				 * 
				 * //Setting the sequence, supplier credentials and urls for the header Element
				 * supplierCredentials = productSupplier.toElement(ownerDoc, sequence);
				 * 
				 * supplierCredentialsList.appendChild(supplierCredentials);
				 */

				// Making supplierCredentialsList for Each SupplierID

				supplierCredentialsList = getSupplierCredentialsList(ownerDoc, usrCtx, supplierID, sequence,
						supplierCredentialsList);

				// Making request body for particular supplierID
				supWrapperElement = (Element) wrapperElement.cloneNode(true);
				requestBodyElement.appendChild(supWrapperElement);

				// Setting supplier id in request body
				Element supplierIDElement = XMLUtils.getFirstElementAtXPath(supWrapperElement, "./pac:SupplierID");
				supplierIDElement.setTextContent(supplierID);

				// Setting sequence in request body
				Element sequenceElement = XMLUtils.getFirstElementAtXPath(supWrapperElement, "./pac:Sequence");
				sequenceElement.setTextContent(Integer.toString(sequence));

				// creating element search criteria
				searchCriteria = XMLUtils.getFirstElementAtXPath(supWrapperElement,
						"./ns:OTA_DynamicPkgAvailRQ/ns:SearchCriteria");

				// getting parent node OTA_DynamicPkgAvailRQ from SearchCriteria
				otaAvailRQ = (Element) searchCriteria.getParentNode();

				String tourCode = dynamicPackageObj.getString("tourCode");
				String brandName = dynamicPackageObj.getString("brandName");
				String subTourCode = dynamicPackageObj.getString("subTourCode");

				Element refPoint = XMLUtils.getFirstElementAtXPath(searchCriteria,
						"./ns:PackageOptionSearch/ns:OptionSearchCriteria/ns:Criterion/ns:RefPoint");
				Attr attributeBrandCode = ownerDoc.createAttribute("Code");
				attributeBrandCode.setValue(brandName);
				refPoint.setAttributeNode(attributeBrandCode);

				Element optionRef = XMLUtils.getFirstElementAtXPath(searchCriteria,
						"./ns:PackageOptionSearch/ns:OptionSearchCriteria/ns:Criterion/ns:OptionRef");
				Attr attributeTourCode = ownerDoc.createAttribute("Code");
				attributeTourCode.setValue(tourCode);
				optionRef.setAttributeNode(attributeTourCode);

				// creating element dynamic package
				dynamicPackage = XMLUtils.getFirstElementAtXPath(otaAvailRQ, "./ns:DynamicPackage");

				// Creating Components element
				JSONObject components = dynamicPackageObj.getJSONObject(JSON_PROP_COMPONENTS);

				if (components == null || components.length() == 0) {
					throw new Exception(String.format("Object components must be set for supplier %s", supplierID));
				}

				Element componentsElement = XMLUtils.getFirstElementAtXPath(dynamicPackage, "./ns:Components");

				// Check whether hotel component is empty or not. If not empty then add pre and
				// postnight in hotel component
				if (components.has(JSON_PROP_HOTEL_COMPONENT)) {
					JSONObject hotelComponentJson = components.getJSONObject(JSON_PROP_HOTEL_COMPONENT);
					if (hotelComponentJson != null && hotelComponentJson.length() != 0) {
						JSONArray hotelComponentsJsonArray = new JSONArray();
						hotelComponentsJsonArray.put(hotelComponentJson);
						components.remove(JSON_PROP_HOTEL_COMPONENT);
						components.put(JSON_PROP_HOTEL_COMPONENT, hotelComponentsJsonArray);

						// Creating Hotel Component
						JSONArray hotelComponents = components.getJSONArray(JSON_PROP_HOTEL_COMPONENT);
						if (hotelComponents != null && hotelComponents.length() != 0) {
							// read post and pre Night and put into hotel component
							putPreAndPostNightInHotelOrCruiseComponent(components, hotelComponents, JSON_PROP_HOTEL_COMPONENT);
							// end
							componentsElement = getHotelComponentElement(ownerDoc, hotelComponents, componentsElement);
						}
					} else {
						JSONArray hotelComponentsJsonArray = new JSONArray();
						components.put(JSON_PROP_HOTEL_COMPONENT, hotelComponentsJsonArray);
					}

				}

				// Creating Air Component
				JSONArray airComponents = components.getJSONArray(JSON_PROP_AIR_COMPONENT);

				if (airComponents != null && airComponents.length() != 0) {
					componentsElement = getAirComponentElement(ownerDoc, dynamicPackageObj, airComponents,
							componentsElement, supplierID);
				}

				// Creating PackageOptionComponent Element
				Element packageOptionComponentElement = ownerDoc.createElementNS(NS_OTA, "ns:PackageOptionComponent");

				Attr attributeQuoteID = ownerDoc.createAttribute("QuoteID");
				attributeQuoteID.setValue(subTourCode);
				packageOptionComponentElement.setAttributeNode(attributeQuoteID);

				Element packageOptionsElement = ownerDoc.createElementNS(NS_OTA, "ns:PackageOptions");

				Element packageOptionElement = ownerDoc.createElementNS(NS_OTA, "ns:PackageOption");

				Element tpaElement = ownerDoc.createElementNS(NS_OTA, "ns:TPA_Extensions");

				// Check whether cruise component is empty or not. If not empty then add pre and
				// postnight in cruise component
				// Note- either cruise or hotel will be empty
				if (components.has(JSON_PROP_CRUISE_COMPONENT)) {
					JSONObject cruiseComponentJson = components.getJSONObject(JSON_PROP_CRUISE_COMPONENT);
					if (cruiseComponentJson != null && cruiseComponentJson.length() != 0) {
						JSONArray cruiseComponentsJsonArray = new JSONArray();
						cruiseComponentsJsonArray.put(cruiseComponentJson);
						components.remove(JSON_PROP_CRUISE_COMPONENT);
						components.put(JSON_PROP_CRUISE_COMPONENT, cruiseComponentsJsonArray);

						// Creating Cruise Component
						JSONArray cruiseComponents = components.getJSONArray(JSON_PROP_CRUISE_COMPONENT);
						if (cruiseComponents != null && cruiseComponents.length() != 0) {
							// read post and pre Night and put into cruise component
							putPreAndPostNightInHotelOrCruiseComponent(components, cruiseComponents, JSON_PROP_CRUISE_COMPONENT);
							// end
							tpaElement = getCruiseComponentElement(ownerDoc, cruiseComponents, tpaElement);
						}

					} else {
						JSONArray cruiseComponentsJsonArray = new JSONArray();
						components.put(JSON_PROP_CRUISE_COMPONENT, cruiseComponentsJsonArray);
					}
				}

				// Creating Transfers Component
				JSONArray transfersComponents = components.getJSONArray(JSON_PROP_TRANSFER_COMPONENT);

				if (transfersComponents != null && transfersComponents.length() != 0) {
//					tpaElement = getTransferComponentElement(ownerDoc, transfersComponents, tpaElement);
					//passed components to read globalinfo component
					putDynamicActionForTranfers(transfersComponents,dynamicPackageObj);
					
					tpaElement = getTransferComponentElement(ownerDoc, transfersComponents, tpaElement);
				}

				// Creating Insurance Component
				JSONArray insuranceComponents = components.getJSONArray(JSON_PROP_INSURANCE_COMPONENT);

				if (insuranceComponents != null && insuranceComponents.length() != 0) {
					tpaElement = getInsuranceComponentElement(ownerDoc, insuranceComponents, tpaElement);
				}

				// Appending TPA element to package Option Element
				packageOptionElement.appendChild(tpaElement);

				// Appending package Option Element to package Options Element
				packageOptionsElement.appendChild(packageOptionElement);

				// Appending package Options Element to PackageOptionComponent Element
				packageOptionComponentElement.appendChild(packageOptionsElement);

				// Appending PackageOptionComponent Element to Components Element
				componentsElement.appendChild(packageOptionComponentElement);

				// create RestGuests xml elements
				JSONArray resGuests = dynamicPackageObj.getJSONArray(JSON_PROP_RESGUESTS);

				Element resGuestsElement = XMLUtils.getFirstElementAtXPath(dynamicPackage, "./ns:ResGuests");

				if (resGuests != null && resGuests.length() != 0) {
					for (int j = 0; j < resGuests.length(); j++) {
						JSONObject resGuest = resGuests.getJSONObject(j);

						Element resGuestElement = getResGuestElement(ownerDoc, resGuest);

						resGuestsElement.appendChild(resGuestElement);
					}

					// dynamicPackage.appendChild(resGuestsElement);
				}

				// Create GlobalInfo xml element
				JSONObject globalInfo = dynamicPackageObj.getJSONObject(JSON_PROP_GLOBALINFO);

				if (globalInfo != null && globalInfo.length() != 0) {
					Element globalInfoElement = XMLUtils.getFirstElementAtXPath(dynamicPackage, "./ns:GlobalInfo");
					globalInfoElement = getGlobalInfoElement(ownerDoc, globalInfo, globalInfoElement);

					// dynamicPackage.appendChild(globalInfoElement);
				}
			}

			System.out.println("XML Request for SI: "+XMLTransformer.toString(requestElement));

			Element responseElement = null;
			responseElement = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					HolidaysConfig.getHttpHeaders(), requestElement);
			if (responseElement == null) {
				throw new Exception("Null response received from SI");
			}
			System.out.println("responseElement SI ===>" + XMLTransformer.toString(responseElement));

			// Added code for converting SI XML response to SI JSON Response

			// JSONObject reqHdrJson = reqJson.getJSONObject("requestHeader");
			// JSONObject reqBodyJson = reqJson.getJSONObject("requestBody");
			JSONObject resBodyJson = new JSONObject();

			// ------wrapper array----- start

			JSONArray dynamicPackageArray = new JSONArray();

			Element[] oTA_wrapperElems = XMLUtils.getElementsAtXPath(responseElement,
					"./pac1:ResponseBody/pac:OTA_DynamicPkgAvailRSWrapper");
			int dynPkgSync = 0;
			for (Element oTA_wrapperElem : oTA_wrapperElems) {
				JSONArray bookReferencesArray = new JSONArray();

				String supplierIDStr = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:SupplierID"));
				String sequenceStr = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:Sequence"));

				// -----Error Handling Started-----

				Element oTA_DynamicPkgAvailRS = XMLUtils.getFirstElementAtXPath(oTA_wrapperElem,
						"./ns:OTA_DynamicPkgAvailRS");
				JSONArray errorArray = HolidaysUtil.holidaysErrorHandler(oTA_DynamicPkgAvailRS);
				JSONObject errorJson = new JSONObject();
				if (errorArray.length() > 0) {
					errorJson.put("sequence", sequenceStr);
					errorJson.put("supplierID", supplierIDStr);
					errorJson.put("error", errorArray);
				}
				// -----Error Handling ended-----

				else {
					Element[] dynamicPackageElemArray = XMLUtils.getElementsAtXPath(oTA_wrapperElem,
							"./ns:OTA_DynamicPkgAvailRS/ns:DynamicPackage");
					for (Element dynamicPackageElem : dynamicPackageElemArray) {

						String tourCode = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackageElem,
								"./ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/@ID"));
						String subTourCode = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackageElem,
								"./ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@QuoteID"));
						String brandName = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackageElem,
								"./ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/ns:CompanyName/@CompanyShortName"));

						String resUniqueKey = brandName + tourCode + subTourCode;
						JSONArray reqDynamicPkgArray = requestBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE);
						JSONObject reqCurrentDynamicPkg = new JSONObject();

						for (int i = 0; i < reqDynamicPkgArray.length(); i++) {

							JSONObject DynamicPkg = requestBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE).getJSONObject(i);
							String reqBrandName = DynamicPkg.getString("brandName");
							String reqTourCode = DynamicPkg.getString("tourCode");
							String reqSubTourCode = DynamicPkg.getString("subTourCode");

							String reqUniqueKey = reqBrandName + reqTourCode + reqSubTourCode;

							if (resUniqueKey.equals(reqUniqueKey)) {
								reqCurrentDynamicPkg = DynamicPkg;
								break;
							}
						}
						JSONObject dynamicPackJson = getSupplierResponseDynamicPackageJSON(dynamicPackageElem,
								reqCurrentDynamicPkg);

						dynamicPackJson.put(JSON_PROP_SUPPLIERID, supplierIDStr);
						dynamicPackJson.put(JSON_PROP_SEQUENCE, sequenceStr);

						dynamicPackJson.put("tourCode", tourCode);
						dynamicPackJson.put("subTourCode", subTourCode);
						dynamicPackJson.put("brandName", brandName);

						JSONObject globalInfoJson = getGlobalInfo(dynamicPackageElem);

						dynamicPackJson.put("globalInfo", globalInfoJson);

						// Adding the supplier prices (SupplierInfo object) into dynamic package
						JSONObject supplierInfo = getSupplierDataToStoreInRedis(dynamicPackJson, dynamicPackageElem,
								requestBody, reqCurrentDynamicPkg);
						dynamicPackJson.put("supplierInfo", supplierInfo);

						dynamicPackageArray.put(dynamicPackJson);

						// Creating bookReferences Object and supplierBookingFare Object
						JSONObject bookReferencesJson = getBookReference(dynamicPackageElem);
						bookReferencesArray.put(bookReferencesJson);
						dynPkgSync++;
					}
				}
				if (errorArray.length() > 0) {
					dynamicPackageArray.put(errorJson);
					//return XMLTransformer.toString(responseElement);
				}
				resBodyJson.put("dynamicPackage", dynamicPackageArray);
				resBodyJson.put("bookReferences", bookReferencesArray);

			}

			// wrapperarray ----end

			JSONObject resJson = new JSONObject();
			resJson.put("responseHeader", requestHeader);
			resJson.put("responseBody", resBodyJson);
			logger.trace(String.format("SI Transformed JSON Response = %s", resJson.toString()));
			System.out.println("SI JSON Response before RedisCache = " + resJson.toString());

			// Call BRMS Supplier and Client Commercials
			logger.info(String.format("Calling to Supplier Commercial"));

			JSONObject resSupplierCommJson = HolidaysSupplierCommercials.getSupplierCommercials(requestJson, resJson,
					opConfig.getOperationName());
			if (BRMS_STATUS_TYPE_FAILURE.equals(resSupplierCommJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Supplier Commercials calculation engine: %s", resSupplierCommJson.toString()));
            			return getEmptyResponse(requestHeader).toString();
			}

			logger.info(String.format("Supplier Commercial Response = %s", resSupplierCommJson.toString()));
			System.out.println("Supplier Commercial Response = " + resSupplierCommJson.toString());

			JSONObject resClientCommJson = HolidaysClientCommercials.getClientCommercials(resSupplierCommJson);
			if (BRMS_STATUS_TYPE_FAILURE.equals(resClientCommJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Client Commercials calculation engine: %s", resClientCommJson.toString()));
            			return getEmptyResponse(requestHeader).toString();
			}
			
			
			// For Adding Supplier and Client Commercials into Redis
			inputSupplierCommercialRedis(resSupplierCommJson, resClientCommJson, resJson);

			logger.info(String.format("Client Commercial Response = %s", resClientCommJson.toString()));
			System.out.println("Client Commercial Response = " + resClientCommJson.toString());

			calculatePrices(requestJson, resJson, resSupplierCommJson, resClientCommJson);

			// Call to Redis Cache
			pushSuppFaresToRedisAndRemove(resJson, requestBody);

			System.out.println("SI JSON Response after RedisCache = " + resJson.toString());

			// return XMLTransformer.toString(responseElement);
			return resJson.toString();

		} catch (Exception x) {
			x.printStackTrace();
			return x.getMessage();
		}
	}
	
	private static void putDynamicActionForTranfers(JSONArray transfersComponents, JSONObject dynamicPackageObj) {
		// Read dynamicPkgID from globalInfo to get tourStartCity and tourEndCity
		String tourStartCity = dynamicPackageObj.getJSONObject("globalInfo").getJSONObject("dynamicPkgID")
				.getJSONObject("tourDetails").getString("tourStartCity");
		String tourEndCity = dynamicPackageObj.getJSONObject("globalInfo").getJSONObject("dynamicPkgID")
				.getJSONObject("tourDetails").getString("tourEndCity");
		for (int i = 0; i < transfersComponents.length(); i++) {
			JSONObject transfersComponent = transfersComponents.getJSONObject(i);
			// if tourStartCity matched with pickuplocation then set "dynamicPkgAction":
			// "PackageArrivalTransfer"
			// if tourEndCity matched with pickuplocation then set "dynamicPkgAction":
			// "PackageDepartureTransfer"
			if (!transfersComponent.has(JSON_PROP_DYNAMICPKGACTION)) {
				JSONArray groundServiceJsonArray = transfersComponent.getJSONArray("groundService");
				for (int j = 0; j < groundServiceJsonArray.length(); j++) {
					String pickUpLocation = groundServiceJsonArray.getJSONObject(j).getJSONObject("location")
							.getString("pickUpLocation");
					if (tourStartCity.equalsIgnoreCase(pickUpLocation)) {
						transfersComponent.put(JSON_PROP_DYNAMICPKGACTION, "PackageArrivalTransfer");
					} else if (tourEndCity.equalsIgnoreCase(pickUpLocation)) {
						transfersComponent.put(JSON_PROP_DYNAMICPKGACTION, "PackageDepartureTransfer");
					}
				}
			}
		}
	}

	//Get postNight and preNight component and put into hotel component
	private static void putPreAndPostNightInHotelOrCruiseComponent(JSONObject components, JSONArray hotelComponents,
			String componentType) {
		JSONObject postNightJson = new JSONObject();
		JSONObject preNightJson = new JSONObject();
		if (components.has(JSON_PROP_POSTNIGHT)) {
			if (componentType.equals(JSON_PROP_CRUISE_COMPONENT)) {
				components.getJSONObject(JSON_PROP_POSTNIGHT).put(JSON_PROP_DYNAMICPKGACTION,
						DYNAMICPKGACTION_CRUISE_POST);
				postNightJson = components.getJSONObject(JSON_PROP_POSTNIGHT);
				components.remove(JSON_PROP_POSTNIGHT);
				components.getJSONArray(JSON_PROP_CRUISE_COMPONENT).put(postNightJson);
			} else if (componentType.equals(JSON_PROP_HOTEL_COMPONENT)) {
				components.getJSONObject(JSON_PROP_POSTNIGHT).put(JSON_PROP_DYNAMICPKGACTION,
						DYNAMICPKGACTION_HOTEL_POST);
				postNightJson = components.getJSONObject(JSON_PROP_POSTNIGHT);
				components.remove(JSON_PROP_POSTNIGHT);
				components.getJSONArray(JSON_PROP_HOTEL_COMPONENT).put(postNightJson);
			}
		}
		if (components.has(JSON_PROP_PRENIGHT)) {
			if (componentType.equals(JSON_PROP_CRUISE_COMPONENT)) {
				components.getJSONObject(JSON_PROP_PRENIGHT).put(JSON_PROP_DYNAMICPKGACTION,
						DYNAMICPKGACTION_CRUISE_PRE);
				preNightJson = components.getJSONObject(JSON_PROP_PRENIGHT);
				components.remove(JSON_PROP_PRENIGHT);
				components.getJSONArray(JSON_PROP_CRUISE_COMPONENT).put(preNightJson);
			} else if (componentType.equals(JSON_PROP_HOTEL_COMPONENT)) {
				components.getJSONObject(JSON_PROP_PRENIGHT).put(JSON_PROP_DYNAMICPKGACTION,
						DYNAMICPKGACTION_HOTEL_PRE);
				preNightJson = components.getJSONObject(JSON_PROP_PRENIGHT);
				components.remove(JSON_PROP_PRENIGHT);
				components.getJSONArray(JSON_PROP_HOTEL_COMPONENT).put(preNightJson);
			}
		}

	}
	public static JSONObject getEmptyResponse(JSONObject reqHdrJson) {
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, new JSONObject());
        return resJson;
    }

	/*
	 * private static void inputClientCommercialRedis(JSONObject resClientCommJson,
	 * JSONObject resJson) { // TODO Auto-generated method stub
	 * 
	 * JSONArray briArr =
	 * resClientCommJson.getJSONObject("result").getJSONObject("execution-results").
	 * getJSONArray("results").getJSONObject(0).getJSONObject("value").getJSONObject
	 * ("cnk.holidays_commercialscalculationengine.suppliertransactionalrules.Root")
	 * .getJSONArray("businessRuleIntake"); JSONObject briJson = new JSONObject();
	 * String scrSuppId = ""; Map<String, Integer> suppIndexMap = new
	 * HashMap<String, Integer>();
	 * 
	 * //Creating a Map to Fetch Proper dynamicPkg for corresponding Business Rule
	 * Intake(Supplier response and redis req Sync) for (int i = 0; i <
	 * briArr.length(); i++) { briJson = briArr.getJSONObject(i); scrSuppId =
	 * briJson.getJSONObject("commonElements").getString("supplier"); JSONArray
	 * dynamicPkgArray =
	 * resBody.getJSONObject("responseBody").getJSONArray("dynamicPackage");
	 * JSONObject currentDynamicPkg = new JSONObject(); String dynPkgSuppID = "";
	 * for(int j=0;j<dynamicPkgArray.length();j++) { currentDynamicPkg =
	 * dynamicPkgArray.getJSONObject(j); dynPkgSuppID =
	 * currentDynamicPkg.getString("supplierID");
	 * 
	 * if (!(scrSuppId.equalsIgnoreCase(dynPkgSuppID))) { continue; } int idx =
	 * (suppIndexMap.containsKey(dynPkgSuppID)) ? (suppIndexMap.get(dynPkgSuppID) +
	 * 1) : 0; suppIndexMap.put(dynPkgSuppID, idx); } }
	 * 
	 * }
	 */

	private static void inputSupplierCommercialRedis(JSONObject resSupplierCommJson, JSONObject resClientCommJson,
			JSONObject resBody) {

		JSONArray supplierBriArr = resSupplierCommJson.getJSONObject("result").getJSONObject("execution-results")
				.getJSONArray("results").getJSONObject(0).getJSONObject("value")
				.getJSONObject("cnk.holidays_commercialscalculationengine.suppliertransactionalrules.Root")
				.getJSONArray("businessRuleIntake");
		JSONArray clientBriArr = resClientCommJson.getJSONObject("result").getJSONObject("execution-results")
				.getJSONArray("results").getJSONObject(0).getJSONObject("value")
				.getJSONObject("cnk.holidays_commercialscalculationengine.clienttransactionalrules.Root")
				.getJSONArray("businessRuleIntake");
		JSONObject supplierBriJson = new JSONObject();
		JSONObject clientBriJson = new JSONObject();
		String scrSuppId = "";
		String ccrSuppId = "";
		Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();

		// Creating a Map to sync dynamicPkg array with Business Rule Intake
		// array(Supplier response and redis req Sync)
		for (int i = 0; i < supplierBriArr.length(); i++) {
			supplierBriJson = supplierBriArr.getJSONObject(i);
			clientBriJson = clientBriArr.getJSONObject(i);
			scrSuppId = supplierBriJson.getJSONObject("commonElements").getString("supplier");
			ccrSuppId = clientBriJson.getJSONObject("commonElements").getString("supplier");
			JSONArray dynamicPkgArray = resBody.getJSONObject("responseBody").getJSONArray("dynamicPackage");
			JSONObject currentDynamicPkg = new JSONObject();
			String dynPkgSuppID = "";
			for (int j = 0; j < dynamicPkgArray.length(); j++) {
				currentDynamicPkg = dynamicPkgArray.getJSONObject(j);
				dynPkgSuppID = currentDynamicPkg.getString("supplierID");

				if (!(scrSuppId.equalsIgnoreCase(dynPkgSuppID))) {
					continue;
				}
				int idx = (suppIndexMap.containsKey(dynPkgSuppID)) ? (suppIndexMap.get(dynPkgSuppID) + 1) : 0;
				suppIndexMap.put(dynPkgSuppID, idx);
			}
		}

		JSONArray dynamicPkgArr = resBody.getJSONObject("responseBody").getJSONArray("dynamicPackage");
		for (int d = 0; d < dynamicPkgArr.length(); d++) {
			JSONObject currDynamicPkg = dynamicPkgArr.getJSONObject(d);
			String dynPkgSuppID = currDynamicPkg.getString("supplierID");
			int index = suppIndexMap.get(dynPkgSuppID);
			JSONArray paxTypeFares = currDynamicPkg.getJSONObject("supplierInfo")
					.getJSONObject("perComponentPerPaxFares").getJSONArray("paxTypeFares");

			// Adding SupplierCommercialPrice and ClientCommercialPrice object for
			// Accommodation Component
			for (int b = 0; b < supplierBriArr.length(); b++) {
				supplierBriJson = supplierBriArr.getJSONObject(b);
				clientBriJson = clientBriArr.getJSONObject(b);
				scrSuppId = supplierBriJson.getJSONObject("commonElements").getString("supplier");
				if ((scrSuppId.equalsIgnoreCase(dynPkgSuppID)) && (ccrSuppId.equalsIgnoreCase(dynPkgSuppID))) {
					JSONArray supplierPackageDetails = supplierBriJson.getJSONArray("packageDetails");
					JSONArray clientPackageDetails = clientBriJson.getJSONArray("packageDetails");
					for (int c = 0; c < supplierPackageDetails.length(); c++) {
						if (c == index) {
							JSONObject supplierPkgDetail = supplierPackageDetails.getJSONObject(index);
							JSONObject clientPkgDetail = clientPackageDetails.getJSONObject(index);
							JSONArray supplierRoomDetails = supplierPkgDetail.getJSONArray("roomDetails");
							JSONArray clientRoomDetails = clientPkgDetail.getJSONArray("roomDetails");

							for (int i = 0; i < supplierRoomDetails.length(); i++) {
								JSONObject supplierRoomJSON = supplierRoomDetails.getJSONObject(i);
								JSONObject clientRoomJSON = clientRoomDetails.getJSONObject(i);
								String supplierRoomType = supplierRoomJSON.getString("roomType");
								String clientRoomType = clientRoomJSON.getString("roomType");

								JSONArray supplierPassengerDetails = supplierRoomJSON.getJSONArray("passengerDetails");
								JSONArray clientPassengerDetails = clientRoomJSON.getJSONArray("passengerDetails");

								for (int j = 0; j < supplierPassengerDetails.length(); j++) {
									JSONObject supplierPassenger = supplierPassengerDetails.getJSONObject(j);
									JSONObject clientPassenger = clientPassengerDetails.getJSONObject(j);

									String suppResPaxType = supplierPassenger.getString("passengerType");
									String clientResPaxType = clientPassenger.getString("passengerType");

									JSONArray suppCommercial = supplierPassenger.getJSONArray("commercialDetails");
									JSONArray clientCommercial = clientPassenger.getJSONArray("entityCommercials");

									for (int k = 0; k < paxTypeFares.length(); k++) {
										String redisPaxType = paxTypeFares.getJSONObject(k).getString("paxType");

										if ((redisPaxType.equalsIgnoreCase(suppResPaxType))
												&& (redisPaxType.equalsIgnoreCase(clientResPaxType))) {
											JSONObject supplierPricesAdChIn = paxTypeFares.getJSONObject(k)
													.getJSONObject("prices");
											JSONArray accoArray = supplierPricesAdChIn
													.getJSONArray("AccommodationComponent");

											for (int a = 0; a < accoArray.length(); a++) {
												JSONObject currentRoom = accoArray.getJSONObject(a);
												String redisRoomType = currentRoom.getString("roomType");
												if (redisRoomType.contains(supplierRoomType)) {
													currentRoom.put("supplierCommercialPrice", suppCommercial);
												}
												if (redisRoomType.contains(clientRoomType)) {
													currentRoom.put("clientCommercialPrice", clientCommercial);
												}

											}
										}
									}
								}
							}

							// Adding SupplierCommercialPrice and ClientCommercialPrice object for
							// applicable On Products
							JSONArray supplierApplicableOnProducts = supplierPkgDetail
									.getJSONArray("applicableOnProducts");
							JSONArray clientApplicableOnProducts = clientPkgDetail.getJSONArray("applicableOnProducts");
							JSONObject supplierCommercialsapplicableonProduct = new JSONObject();
							JSONObject clientCommercialsapplicableonProduct = new JSONObject();

							for (int e = 0; e < supplierApplicableOnProducts.length(); e++) {
								supplierCommercialsapplicableonProduct = supplierApplicableOnProducts.getJSONObject(e);
								clientCommercialsapplicableonProduct = clientApplicableOnProducts.getJSONObject(e);

								String supplierApplicableOnProductName = supplierCommercialsapplicableonProduct
										.getString("productName");
								String clientApplicableOnProductName = clientCommercialsapplicableonProduct
										.getString("productName");

								String supplierApplicableOnPaxType = supplierCommercialsapplicableonProduct
										.getString("passengerType");
								String clientApplicableOnPaxType = clientCommercialsapplicableonProduct
										.getString("passengerType");

								for (int f = 0; f < paxTypeFares.length(); f++) {
									JSONObject currentPaxTypeObject = paxTypeFares.getJSONObject(f);
									String redisPassengerType = currentPaxTypeObject.getString("paxType");
									if ((supplierApplicableOnPaxType.equalsIgnoreCase(redisPassengerType))
											&& (clientApplicableOnPaxType.equalsIgnoreCase(redisPassengerType))) {
										if ((supplierApplicableOnProductName.equals("PreNight"))
												|| (supplierApplicableOnProductName.equals("PostNight"))) {
											String supplierRoomType = supplierCommercialsapplicableonProduct
													.getString("roomType");
											String clientRoomType = clientCommercialsapplicableonProduct
													.getString("roomType");

											JSONArray ppArray = currentPaxTypeObject.getJSONObject("prices")
													.getJSONArray(supplierApplicableOnProductName);
											for (int g = 0; g < ppArray.length(); g++) {
												String redisRoomType = ppArray.getJSONObject(g).getString("roomType");

												// TODO : append an empty array if No commercial applied on current
												// ApplicableOn Product
												if (supplierRoomType.equals(redisRoomType)
														&& clientRoomType.equals(redisRoomType)) {
													currentPaxTypeObject.getJSONObject("prices")
															.getJSONArray(supplierApplicableOnProductName)
															.getJSONObject(g).put("supplierCommercialPrice",
																	supplierCommercialsapplicableonProduct
																			.optJSONArray("commercialDetails"));
													currentPaxTypeObject.getJSONObject("prices")
															.getJSONArray(supplierApplicableOnProductName)
															.getJSONObject(g).put("clientCommercialPrice",
																	clientCommercialsapplicableonProduct
																			.optJSONArray("entityCommercials"));
												}
											}
										} else {
											// TODO : append an empty array if No commercial applied on current
											// ApplicableOn Product
											paxTypeFares.getJSONObject(f).getJSONObject("prices")
													.getJSONObject(supplierApplicableOnProductName)
													.put("supplierCommercialPrice",
															supplierCommercialsapplicableonProduct
																	.optJSONArray("commercialDetails"));
											paxTypeFares.getJSONObject(f).getJSONObject("prices")
													.getJSONObject(clientApplicableOnProductName)
													.put("clientCommercialPrice", clientCommercialsapplicableonProduct
															.optJSONArray("entityCommercials"));
										}
									}
								}

							}

						}
					}
				}
			}
		}
		System.out.println("Final ResponseBody with redis Structure:" + resBody);
	}

	/*
	 * private static JSONArray getTotalSuppComm(JSONObject resSupplierCommJson) {
	 * TODO Auto-generated method stub JSONArray total = new JSONArray();
	 * 
	 * for(i=) total.put(resSupplierCommJson.getJSONObject("result").getJSONObject(
	 * "execution-results").getJSONArray("results").getJSONObject(0).getJSONObject(
	 * "value").getJSONObject(
	 * "cnk.holidays_commercialscalculationengine.suppliertransactionalrules.Root").
	 * getJSONArray("businessRuleIntake").getJSONObject(0).getJSONArray(
	 * "packageDetails").getJSONObject(0).getJSONArray("roomDetails").getJSONObject(
	 * 0).getJSONArray("passengerDetails").getJSONObject(0).getJSONArray(
	 * "commercialDetails"));
	 * total.put("commercialDetails",resSupplierCommJson.getJSONArray(
	 * "commercialDetails"));getJSONObject("value").getJSONObject(
	 * "businessRuleIntake").getJSONObject("packageDetails").getJSONObject(
	 * "roomDetails").getJSONObject("passengerDetails").getJSONArray(
	 * "commercialDetails")); System.out.println("Total"+total.toString());
	 * 
	 * return
	 * resSupplierCommJson.getJSONObject("result").getJSONObject("execution-results"
	 * ).getJSONArray("results").getJSONObject(0).getJSONObject("value").
	 * getJSONObject(
	 * "cnk.holidays_commercialscalculationengine.suppliertransactionalrules.Root").
	 * getJSONArray("businessRuleIntake").getJSONObject(0).getJSONArray(
	 * "packageDetails").getJSONObject(0).getJSONArray("roomDetails").getJSONObject(
	 * 0).getJSONArray("passengerDetails").getJSONObject(0).getJSONArray(
	 * "commercialDetails"); }
	 */

	private static void calculatePrices(JSONObject requestJson, JSONObject resJson, JSONObject resSupplierCommJson,
			JSONObject resClientCommJson) {
		JSONObject briJson, ccommPkgDtlsJson;
		JSONArray ccommPkgDtlsJsonArr, ccommRoomDtlsJsonArr,applicableOnProductsArr, supplierCommercialDetailsArr = null;

		Map<String, String> scommToTypeMap = getSupplierCommercialsAndTheirType(resSupplierCommJson);
		Map<String, String> commToTypeMap = getClientCommercialsAndTheirType(resClientCommJson);

		String clientMarket = requestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
		String clientCcyCode = requestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
		// creating priceArray of unique elements to create Wem response
		Map<String, JSONObject> priceMap = new ConcurrentHashMap<String, JSONObject>();
		
		// retrieve passenger Qty from requestJson
		JSONObject reqCurrentDynamicPkg = new JSONObject();
		JSONObject resdynamicPkgJson = new JSONObject();
		JSONArray dynamicPkgArray = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_DYNAMICPACKAGE);
		for (int a = 0; a < dynamicPkgArray.length(); a++) {
			resdynamicPkgJson = dynamicPkgArray.getJSONObject(a);
			String brandNameRes = resdynamicPkgJson.getString("brandName");
			String tourCodeRes = resdynamicPkgJson.getString("tourCode");
			String subTourCodeRes = resdynamicPkgJson.getString("subTourCode");

			String resUniqueKey = brandNameRes + tourCodeRes + subTourCodeRes;
			JSONArray reqDynamicPkgArray = requestJson.getJSONObject(JSON_PROP_REQBODY)
					.getJSONArray(JSON_PROP_DYNAMICPACKAGE);

			for (int i = 0; i < reqDynamicPkgArray.length(); i++) {

				JSONObject dynamicPkg = reqDynamicPkgArray.getJSONObject(i);
				String reqBrandName = dynamicPkg.getString("brandName");
				String reqTourCode = dynamicPkg.getString("tourCode");
				String reqSubTourCode = dynamicPkg.getString("subTourCode");

				String reqUniqueKey = reqBrandName + reqTourCode + reqSubTourCode;

				if (resUniqueKey.equals(reqUniqueKey)) {
					reqCurrentDynamicPkg = dynamicPkg;
					break;
				}

			}
		}

		// create Map of componentWise passenger Type quantity
		Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty = retrievePassengerQty(reqCurrentDynamicPkg,resdynamicPkgJson);
		// ------populate values from client commercials to generate wem response(i.e.
		// replacing the values of SI json response)

		JSONArray briArr = resClientCommJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject(JSON_PROP_VALUE)
				.getJSONObject(JSON_PROP_CLIENTTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);
		Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();
		
		BigDecimal totalTaxPrice = new BigDecimal(0);
		String suppId = "";
		for (int i = 0; i < briArr.length(); i++) {

			briJson = (JSONObject) briArr.get(i);
			ccommPkgDtlsJsonArr = briJson.getJSONArray(JSON_PROP_PACKAGEDETAILS);
			suppId = briJson.getJSONObject("commonElements").getString("supplier");

			for (int j = 0; j < dynamicPkgArray.length(); j++) {
				JSONObject dynamicPackageJson = dynamicPkgArray.getJSONObject(j);
				totalFare = new BigDecimal("0");
				String supplierIdSI = dynamicPackageJson.getString(JSON_PROP_SUPPLIERID);
				if (!(suppId.equalsIgnoreCase(supplierIdSI))) {
					continue;
				}
				int idx = (suppIndexMap.containsKey(supplierIdSI)) ? (suppIndexMap.get(supplierIdSI) + 1) : 0;
				suppIndexMap.put(supplierIdSI, idx);
				ccommPkgDtlsJson = ccommPkgDtlsJsonArr.getJSONObject(idx);

				JSONArray componentArr = dynamicPackageJson.getJSONArray(JSON_PROP_COMPONENTS);
				for (int k = 0; k < componentArr.length(); k++) {
					JSONObject componentJson = componentArr.getJSONObject(k);
					paxType = componentJson.getString("paxType");

					JSONArray priceArr = componentJson.getJSONArray(JSON_PROP_PRICE);
					priceMap = HolidaysUtil.retainSuppFaresMap(componentJson, priceMap);
					for (int l = 0; l < priceArr.length(); l++) {
						JSONObject priceJson = priceArr.getJSONObject(l);
						
						String rateDescriptionText = priceJson.getJSONObject(JSON_PROP_RATEDESC)
								.getString(JSON_PROP_TEXT);
						if(priceJson.getJSONObject(JSON_PROP_TOTAL).has(JSON_PROP_RATEDESC)) {
						
						
						// for roomDetails ---start
						ccommRoomDtlsJsonArr = ccommPkgDtlsJson.getJSONArray(JSON_PROP_ROOMDETAILS);

						if (rateDescriptionText.contains("Room")) {
							getRoomDetailsPrice(ccommRoomDtlsJsonArr,
									supplierCommercialDetailsArr, scommToTypeMap, commToTypeMap, clientMarket,clientCcyCode, priceMap, 
									componentWisePaxTypeQty, totalTaxPrice, suppId, priceJson,rateDescriptionText);

						}
						// roomDetails -- end
						// for applicable On products --start
						applicableOnProductsArr = ccommPkgDtlsJson.getJSONArray("applicableOnProducts");

						if (!(rateDescriptionText.contains("Room"))) {
							 getApplicableOnProductsPrice(applicableOnProductsArr,supplierCommercialDetailsArr,scommToTypeMap,commToTypeMap,clientMarket,
									clientCcyCode, priceMap, componentWisePaxTypeQty, totalTaxPrice, suppId, priceJson,rateDescriptionText);
						}
						
						// for applicable On products --end
					}}
					//Added SI prices from priceArray for whom commercials are not applied to totalFare 
					if (priceMap != null || !priceMap.isEmpty()) {
						
					totalFare = addSupplierFarestoTotalPrice(priceMap, componentWisePaxTypeQty);}} // Setting the Final price for the package
					JSONObject totalJson = dynamicPackageJson.getJSONObject(JSON_PROP_GLOBALINFO).getJSONObject(JSON_PROP_TOTAL);

				totalJson.put(JSON_PROP_AMOUNTAFTERTAX, totalFare);

			}

		}
	}

	private static void getApplicableOnProductsPrice(JSONArray applicableOnProductsArr,
			JSONArray supplierCommercialDetailsArr, Map<String, String> scommToTypeMap,
			Map<String, String> commToTypeMap, String clientMarket, String clientCcyCode,
			Map<String, JSONObject> priceMap, Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty,
			BigDecimal totalTaxPrice, String suppId, JSONObject priceJson, String rateDescriptionText) {

		JSONObject clientEntityCommJson, markupCalcJson, applicableOnProductsJson;
		JSONArray clientEntityCommJsonArr;

		BigDecimal amountAfterTaxSI = priceJson.getJSONObject(JSON_PROP_TOTAL).getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
		BigDecimal amountBeforeTaxSI = priceJson.getJSONObject(JSON_PROP_TOTAL).getBigDecimal(JSON_PROP_AMOUNTBEFORETAX);
		String dynamicPkgAction = priceJson.getJSONObject(JSON_PROP_TOTAL).getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_DYNAMICPKGACTION);
		String rateDescriptionName = priceJson.getJSONObject(JSON_PROP_TOTAL).getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_NAME);

		for (int a = 0; a < applicableOnProductsArr.length(); a++) {
			applicableOnProductsJson = applicableOnProductsArr.getJSONObject(a);
			String productName = applicableOnProductsJson.getString("productName");
			String passengerType = applicableOnProductsJson.getString("passengerType");

			// Checks if SI PaxType matches with CC passengerType

			if (passengerType.equalsIgnoreCase(paxType)) {
				clientEntityCommJsonArr = applicableOnProductsJson.optJSONArray(JSON_PROP_ENTITYCOMMS);
				if (clientEntityCommJsonArr == null) { // TODO: Refine this warning message.
					// Maybe log some context information also.
					logger.warn("Client commercials calculations not found");
					continue;
				} else// This is to capture the comm type field from commercial head in entity
						// details
				{
					int len = clientEntityCommJsonArr.length();
					for (int x = 0; x < len; x++) {
						JSONObject ccommClientCommJson = clientEntityCommJsonArr.getJSONObject(x);
						ccommClientCommJson.put(JSON_PROP_COMMTYPE, commToTypeMap.get(ccommClientCommJson
								.optJSONObject(JSON_PROP_MARKUPCOMDTLS).getString(JSON_PROP_COMMNAME)));
					}

				}
				supplierCommercialDetailsArr = applicableOnProductsJson.optJSONArray(JSON_PROP_COMMDETAILS);

				if (supplierCommercialDetailsArr == null) {
					logger.warn(String.format("No supplier commercials found for supplier %s", suppId));
				}

				else// This is to capture the comm type field from commercial head in
					// suppCommercialRes
				{
					int len = supplierCommercialDetailsArr.length();
					for (int x = 0; x < len; x++) {
						JSONObject scommClientCommJson = supplierCommercialDetailsArr.getJSONObject(x);
						scommClientCommJson.put(JSON_PROP_COMMTYPE,scommToTypeMap.get(scommClientCommJson.getString(JSON_PROP_COMMNAME)));
					}
					// for multiple chain of entity take the latest commercials applied
					for (int y = (clientEntityCommJsonArr.length() - 1); y >= 0; y--) {

						clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(y);
						// TODO: In case of B2B, do we need to add additional receivable commercials
						// for all client hierarchy levels?
						JSONArray additionalCommsJsonArr = clientEntityCommJson.optJSONArray(JSON_PROP_ADDCOMMDETAILS);// take the array additionalcommercialDetails
						if (additionalCommsJsonArr != null) {
							for (int x = 0; x < additionalCommsJsonArr.length(); x++) {
								JSONObject additionalCommsJson = additionalCommsJsonArr.getJSONObject(x);// take object of additionalcommercialDetails array one by one
								String additionalCommName = additionalCommsJson.optString(JSON_PROP_COMMNAME);// fetch comm Name from additionalcommercialDetails object
								if (COMM_TYPE_RECEIVABLE.equals(commToTypeMap.get(additionalCommName))) {// is the additionalCommName receivable?
									String additionalCommCcy = additionalCommsJson.getString(JSON_PROP_COMMCCY);// get comm currency from additionalcommercialDetails Object
									BigDecimal additionalCommAmt = additionalCommsJson
											.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData
													.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
									totalPrice = totalPrice.add(additionalCommAmt);

								}
							}
						}
						markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS);
						if (markupCalcJson == null) {
							continue;
						}

						if (rateDescriptionText.contains("Night")) {
							String applicableOnStr = rateDescriptionText
									.substring(0, rateDescriptionText.indexOf("Night") + 5).replaceAll("[^a-zA-Z]", "");
							BigDecimal amountAfterTaxcc = applicableOnProductsJson.getBigDecimal(JSON_PROP_TOTALFARE);
							BigDecimal amountBeforeTaxcc = applicableOnProductsJson.getJSONObject(JSON_PROP_FAREBREAKUP)
									.getBigDecimal(JSON_PROP_BASEFARE);
							int amountAfterTaxvalue = (amountAfterTaxcc).compareTo(amountAfterTaxSI);
							int amountBeforeTaxvalue = amountBeforeTaxcc.compareTo(amountBeforeTaxSI);
							if (applicableOnStr.equalsIgnoreCase(productName) && amountAfterTaxvalue == 0
									&& amountBeforeTaxvalue == 0) {
								populateFinalPrice(markupCalcJson, priceJson, totalPrice, totalTaxPrice,
										componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName);
								priceMap.remove(rateDescriptionText);
							}
						}

						else if (rateDescriptionText.contains("Transfer") && (productName.contains("Transfer"))) {
							if (rateDescriptionText.contains("Arrival") && rateDescriptionText.contains("Transfer")) {

								populateFinalPrice(markupCalcJson, priceJson, totalPrice, totalTaxPrice,
										componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName);
								priceMap.remove(rateDescriptionText);
							} else if (rateDescriptionText.contains("Departure")
									&& rateDescriptionText.contains("Transfer")) {

								populateFinalPrice(markupCalcJson, priceJson, totalPrice, totalTaxPrice,
										componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName);
								priceMap.remove(rateDescriptionText);
							}
						}

						else if ((rateDescriptionText.contains("Extra") || rateDescriptionText.contains("Upgrade"))
								&& (productName.contains("Extra"))) {

							populateFinalPrice(markupCalcJson, priceJson, totalPrice, totalTaxPrice,
									componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName);
							priceMap.remove(rateDescriptionText);
						}

						else if ((rateDescriptionText.contains("Trip Protection")
								|| rateDescriptionText.contains("Insurance")) && (productName.contains("Insurance"))) {

							populateFinalPrice(markupCalcJson, priceJson, totalPrice, totalTaxPrice,
									componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName);
							priceMap.remove(rateDescriptionText);

						} else if (rateDescriptionText.contains("Surcharge") && (productName.contains("Surcharge"))) {

							populateFinalPrice(markupCalcJson, priceJson, totalPrice, totalTaxPrice,
									componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName);
							priceMap.remove(rateDescriptionText);
						}

					}

				}
			}
		}

	}

	private  static void getRoomDetailsPrice(JSONArray ccommRoomDtlsJsonArr, JSONArray supplierCommercialDetailsArr,
			Map<String, String> scommToTypeMap, Map<String, String> commToTypeMap, String clientMarket,
			String clientCcyCode, Map<String, JSONObject> priceMap,
			Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty, BigDecimal totalTaxPrice, String suppId,
			JSONObject priceJson, String rateDescriptionText) {
		
		JSONObject ccommRoomDtlsJson,clientEntityCommJson,markupCalcJson,ccommPaxDtlsJson;
		JSONArray clientEntityCommJsonArr,ccommPaxDtlsJsonArr ;
		
		BigDecimal amountAfterTaxSI = priceJson.getJSONObject(JSON_PROP_TOTAL)
				.getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
		BigDecimal amountBeforeTaxSI = priceJson.getJSONObject(JSON_PROP_TOTAL)
				.getBigDecimal(JSON_PROP_AMOUNTBEFORETAX);
		String dynamicPkgAction = priceJson.getJSONObject(JSON_PROP_TOTAL).getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_DYNAMICPKGACTION);
		String rateDescriptionName = priceJson.getJSONObject(JSON_PROP_TOTAL).getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_NAME);
		
		for (int m = 0; m < ccommRoomDtlsJsonArr.length(); m++) {
			ccommRoomDtlsJson = ccommRoomDtlsJsonArr.getJSONObject(m);

			String roomType = ccommRoomDtlsJson.getString(JSON_PROP_ROOMTYPE);

			ccommPaxDtlsJsonArr = ccommRoomDtlsJson.getJSONArray(JSON_PROP_PASSENGERDETAILS);

			for (int n = 0; n < ccommPaxDtlsJsonArr.length(); n++) {

				ccommPaxDtlsJson = ccommPaxDtlsJsonArr.getJSONObject(n);

				String passengerType = ccommPaxDtlsJson.getString("passengerType");

				// Checks if SI PaxType matches with CC passengerType
				if (passengerType.equalsIgnoreCase(paxType)) {

					clientEntityCommJsonArr = ccommPaxDtlsJson.optJSONArray(JSON_PROP_ENTITYCOMMS);
					if (clientEntityCommJsonArr == null) {
						// TODO: Refine this warning message. Maybe log some context information
						// also.
						logger.warn("Client commercials calculations not found");
						continue;
					} else// This is to capture the comm type field from commercial head in entity
							// details
					{
						int len = clientEntityCommJsonArr.length();
						for (int x = 0; x < len; x++) {
							JSONObject ccommClientCommJson = clientEntityCommJsonArr.getJSONObject(x);
							ccommClientCommJson.put(JSON_PROP_COMMTYPE,commToTypeMap.get(ccommClientCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS).getString(JSON_PROP_COMMNAME)));
						}

					}
					supplierCommercialDetailsArr = ccommPaxDtlsJson.optJSONArray(JSON_PROP_COMMDETAILS);
					if (supplierCommercialDetailsArr == null) {
						logger.warn(String.format("No supplier commercials found for supplier %s",
								suppId));
					}
					else// This is to capture the comm type field from commercial head in suppCommercialRes
					{   int len = supplierCommercialDetailsArr.length();
						for (int x = 0; x < len; x++) {
							JSONObject scommClientCommJson = supplierCommercialDetailsArr.getJSONObject(x);
							scommClientCommJson.put(JSON_PROP_COMMTYPE, scommToTypeMap.get(scommClientCommJson.getString(JSON_PROP_COMMNAME)));
						}
						// for multiple chain of entity take the latest commercials applied
						for (int o = (clientEntityCommJsonArr.length() - 1); o >= 0; o--) {

							clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(o);
							// TODO: In case of B2B, do we need to add additional receivable
							// commercials for all client hierarchy levels?
							JSONArray additionalCommsJsonArr = clientEntityCommJson.optJSONArray(JSON_PROP_ADDCOMMDETAILS);// take the array additionalcommercialDetails
							if (additionalCommsJsonArr != null) {
								for (int x = 0; x < additionalCommsJsonArr.length(); x++) {
									JSONObject additionalCommsJson = additionalCommsJsonArr.getJSONObject(x);// take object of additionalcommercialDetails array one by one
									String additionalCommName = additionalCommsJson.optString(JSON_PROP_COMMNAME);// fetch comm Name from additionalcommercialDetails object
									if (COMM_TYPE_RECEIVABLE.equals(commToTypeMap.get(additionalCommName))) {// is the additionalCommName receivable?
										String additionalCommCcy = additionalCommsJson.getString(JSON_PROP_COMMCCY);// get comm currency from additionalcommercialDetails Object
										BigDecimal additionalCommAmt = additionalCommsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(
														additionalCommCcy, clientCcyCode,clientMarket));
										totalPrice = totalPrice.add(additionalCommAmt);

									}
								}
							}
							markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS);
							if (markupCalcJson == null) {

								continue;
							}

							BigDecimal amountAfterTaxcc = ccommPaxDtlsJson.getBigDecimal(JSON_PROP_TOTALFARE);
							BigDecimal amountBeforeTaxcc = ccommPaxDtlsJson.getJSONObject(JSON_PROP_FAREBREAKUP).getBigDecimal(JSON_PROP_BASEFARE);
							int amountAfterTaxvalue = (amountAfterTaxcc).compareTo(amountAfterTaxSI);
							int amountBeforeTaxvalue = amountBeforeTaxcc.compareTo(amountBeforeTaxSI);
							if (rateDescriptionText.contains(roomType) && amountAfterTaxvalue == 0
									&& amountBeforeTaxvalue == 0) {

								populateFinalPrice(markupCalcJson, priceJson, totalPrice,
										totalTaxPrice, componentWisePaxTypeQty,dynamicPkgAction,rateDescriptionName);
								priceMap.remove(rateDescriptionText);
							}
						}
					}

				}

			}
		}
		
		
	}

	private static BigDecimal addSupplierFarestoTotalPrice(Map<String, JSONObject> priceMap,
			Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty) {
		for (Entry<String, JSONObject> entry : priceMap.entrySet()) 
		{ JSONObject priceMapJson = entry.getValue();
		BigDecimal paxCount = new BigDecimal(0);
		if(priceMapJson.getJSONObject(JSON_PROP_TOTAL).has(JSON_PROP_RATEDESC)) {
		String dynamicPkgAction = priceMapJson.getJSONObject(JSON_PROP_TOTAL).getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_DYNAMICPKGACTION);
		String rateDescriptionName = priceMapJson.getJSONObject(JSON_PROP_TOTAL).getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_NAME);
		
		if(dynamicPkgAction.contains(DYNAMICPKGACTION_HOTEL_TOUR)) {

			String[] arr= rateDescriptionName.split("\\s+");
			
			rateDescriptionName = (dynamicPkgAction+arr[0]).toLowerCase();
			
		}else if (dynamicPkgAction.contains("Night")) {
			if(dynamicPkgAction.contains(JSON_PROP_PRENIGHT)){
				rateDescriptionName = JSON_PROP_PRENIGHT.toLowerCase();
				}else if (dynamicPkgAction.contains(JSON_PROP_POSTNIGHT)){
					rateDescriptionName = JSON_PROP_POSTNIGHT.toLowerCase();
				}
			//rateDescriptionName = (dynamicPkgAction+rateDescriptionName.substring(rateDescriptionName.indexOf("(")+1,rateDescriptionName.indexOf(")"))).toLowerCase();
			//TODO :Check how to handle when supplier doesn't send roomType
			
		}else if (dynamicPkgAction.contains("Insurance")) {
			
			rateDescriptionName = (dynamicPkgAction+rateDescriptionName).toLowerCase();
		}else if (dynamicPkgAction.contains("Transfer")) {
			rateDescriptionName = dynamicPkgAction.toLowerCase();
		}
		
		
		BigDecimal amountAfterTax = priceMapJson.getJSONObject("total").optBigDecimal(JSON_PROP_AMOUNTAFTERTAX, new BigDecimal("0"));
		paxCount = getPaxCount(componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName, paxCount);
		totalFare = totalFare.add((amountAfterTax).multiply(paxCount));
		}
}		 return totalFare;
	}

	private static void populateFinalPrice(JSONObject markupCalcJson, JSONObject priceJson, BigDecimal totalPrice,
			BigDecimal totalTaxPrice, Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty,
			String dynamicPkgAction, String rateDescriptionName) {
		String roomType = null;
		if(dynamicPkgAction.equalsIgnoreCase(DYNAMICPKGACTION_HOTEL_TOUR)||dynamicPkgAction.equalsIgnoreCase(DYNAMICPKGACTION_CRUISE_TOUR)) {

			String[] arr= rateDescriptionName.split("\\s+");
			roomType=arr[0];
			rateDescriptionName = (dynamicPkgAction+arr[0]).toLowerCase();
			
		}else if (dynamicPkgAction.contains("Night")) {
			/*if(rateDescriptionName.contains("(")) {
			rateDescriptionName = (dynamicPkgAction+rateDescriptionName.substring(rateDescriptionName.indexOf("(")+1,rateDescriptionName.indexOf(")"))).toLowerCase();
		}else {*/ 
			//TODO :Check how to handle when supplier doesnt send roomType//}
			if(dynamicPkgAction.contains(JSON_PROP_PRENIGHT)){
			rateDescriptionName = JSON_PROP_PRENIGHT.toLowerCase();
			}else if (dynamicPkgAction.contains(JSON_PROP_POSTNIGHT)){
				rateDescriptionName = JSON_PROP_POSTNIGHT.toLowerCase();
			}
			}
			else if (dynamicPkgAction.contains("Insurance")) {
			
			rateDescriptionName = (dynamicPkgAction+rateDescriptionName).toLowerCase();
		}else if (dynamicPkgAction.contains("Transfer")) {
			rateDescriptionName = dynamicPkgAction.toLowerCase();
		}
		BigDecimal totalFareCC = markupCalcJson.optBigDecimal(JSON_PROP_TOTALFARE, new BigDecimal("0"));

		BigDecimal baseFareCC = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP).optBigDecimal(JSON_PROP_BASEFARE,
				new BigDecimal("0"));
		BigDecimal paxCount = new BigDecimal(0);

		JSONArray taxArr = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP).getJSONArray(JSON_PROP_TAXDETAILS);
		JSONObject totalJson = priceJson.getJSONObject(JSON_PROP_TOTAL);
		JSONObject baseJson = priceJson.getJSONObject("base");
		totalPrice = totalPrice.add(totalFareCC);

		JSONArray taxArraySI = totalJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
		totalTaxPrice = totalPrice.subtract(baseFareCC);
		totalJson.getJSONObject(JSON_PROP_TAXES).put(JSON_PROP_AMOUNT, totalTaxPrice);

		if (taxArraySI.length() > 0 && taxArr.length() > 0) {
			for (int t = 0; t < taxArr.length(); t++) {
				JSONObject taxJson = taxArr.getJSONObject(t);
				BigDecimal taxValue = taxJson.getBigDecimal(JSON_PROP_TAXVALUE);
				String taxName = taxJson.getString(JSON_PROP_TAXNAME);

				JSONObject taxJsonSI = taxArraySI.getJSONObject(t);
				taxJsonSI.put(JSON_PROP_AMOUNT, taxValue);
				taxJsonSI.put(JSON_PROP_TAXDESCRIPTION, taxName);
				// TODO : check whether we need to replace SI currency
				// code with
				// markup commercials currencycode
				// taxJsonSI.put("currencyCode", currencyCode);

			}
		}

		totalJson.put(JSON_PROP_AMOUNTAFTERTAX, totalPrice);
		totalJson.put(JSON_PROP_AMOUNTBEFORETAX, baseFareCC);
		baseJson.put(JSON_PROP_AMOUNTBEFORETAX, baseFareCC);
		// TODO : check whether we need to replace SI currency code with
		// markup
		// commercials currencycode
		// totalJson.put("currencyCode", currencyCode);
		paxCount = getPaxCount(componentWisePaxTypeQty, dynamicPkgAction, rateDescriptionName, paxCount);
		totalFare = totalFare.add((totalPrice).multiply(paxCount));

	}

	private static BigDecimal getPaxCount(Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty,
			String dynamicPkgAction, String rateDescriptionName, BigDecimal paxCount) {
		Iterator<Map.Entry<String, Map<String, BigDecimal>>> componentPaxTypeQty = componentWisePaxTypeQty.entrySet()
				.iterator();
		while (componentPaxTypeQty.hasNext()) {

			Map.Entry<String, Map<String, BigDecimal>> compPaxEntry = componentPaxTypeQty.next();
			if (!(compPaxEntry.getKey().contains("Transfer"))){
			if (compPaxEntry.getKey().equalsIgnoreCase(rateDescriptionName)) {
				Iterator<Map.Entry<String, BigDecimal>> paxTypeQty = compPaxEntry.getValue().entrySet().iterator();
				while (paxTypeQty.hasNext()) {
					Map.Entry<String, BigDecimal> paxTypeEntry = paxTypeQty.next();
					if (paxType.equalsIgnoreCase(paxTypeEntry.getKey())) {
						paxCount = paxTypeEntry.getValue();
					}

				}
			} }else if ((compPaxEntry.getKey().contains("Transfer"))){
				if ((compPaxEntry.getKey().contains(dynamicPkgAction.toLowerCase()))) {
					Iterator<Map.Entry<String, BigDecimal>> paxTypeQty = compPaxEntry.getValue().entrySet().iterator();
					while (paxTypeQty.hasNext()) {
						Map.Entry<String, BigDecimal> paxTypeEntry = paxTypeQty.next();
						if (paxType.equalsIgnoreCase(paxTypeEntry.getKey())) {
							paxCount = paxTypeEntry.getValue();
						}

					}
				}
			}
			
			if(!(componentWisePaxTypeQty.containsKey(rateDescriptionName))) {
				if ((compPaxEntry.getKey().equalsIgnoreCase("default"))) {
					Iterator<Map.Entry<String, BigDecimal>> paxTypeQty = compPaxEntry.getValue().entrySet().iterator();
					while (paxTypeQty.hasNext()) {
						Map.Entry<String, BigDecimal> paxTypeEntry = paxTypeQty.next();
						if (paxType.equalsIgnoreCase(paxTypeEntry.getKey())) {
							paxCount = paxTypeEntry.getValue();
						}

					}
				}
		}
		}
		return paxCount;
	}

	public static Map<String, String> getClientCommercialsAndTheirType(JSONObject clientCommResJson) {
		JSONArray commHeadJsonArr = null;
		JSONArray entDetaiJsonArray = null;
		JSONObject commHeadJson = null;
		JSONObject scommBRIJson = null;
		Map<String, String> commToTypeMap = new HashMap<String, String>();
		JSONArray scommBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(clientCommResJson);

		for (int i = 0; i < scommBRIJsonArr.length(); i++) {
			scommBRIJson = scommBRIJsonArr.getJSONObject(i);
			entDetaiJsonArray = scommBRIJson.getJSONArray(JSON_PROP_ENTITYDETAILS);
			for (int j = 0; j < entDetaiJsonArray.length(); j++) {
				commHeadJsonArr = entDetaiJsonArray.getJSONObject(j).getJSONArray(JSON_PROP_COMMHEAD);
				if (commHeadJsonArr == null) {
					logger.warn("No commercial heads found in supplier commercials");
					continue;
				}

				for (int k = 0; k < commHeadJsonArr.length(); k++) {
					commHeadJson = commHeadJsonArr.getJSONObject(k);
					commToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME),
							commHeadJson.getString(JSON_PROP_COMMTYPE));
				}
			}
		}

		return commToTypeMap;

	}

	private static JSONArray getClientCommercialsBusinessRuleIntakeJSONArray(JSONObject clientCommResJson) {

		return clientCommResJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES)
				.getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject("value")
				.getJSONObject(JSON_PROP_CLIENTTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);
	}

	public static Map<String, String> getSupplierCommercialsAndTheirType(JSONObject suppCommResJson) {
		JSONObject scommBRIJson, commHeadJson;
		JSONArray commHeadJsonArr = null;
		Map<String, String> suppCommToTypeMap = new HashMap<String, String>();
		JSONArray scommBRIJsonArr = getSupplierCommercialsBusinessRuleIntakeJSONArray(suppCommResJson);
		for (int i = 0; i < scommBRIJsonArr.length(); i++) {
			scommBRIJson = scommBRIJsonArr.getJSONObject(i);
			commHeadJsonArr = scommBRIJson.optJSONArray(JSON_PROP_COMMHEAD);
			if (commHeadJsonArr == null) {
				logger.warn("No commercial heads found in supplier commercials");
				continue;
			}

			for (int j = 0; j < commHeadJsonArr.length(); j++) {
				commHeadJson = commHeadJsonArr.getJSONObject(j);
				suppCommToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME),
						commHeadJson.getString(JSON_PROP_COMMTYPE));
			}
		}

		return suppCommToTypeMap;

	}

	private static JSONArray getSupplierCommercialsBusinessRuleIntakeJSONArray(JSONObject suppCommResJson) {
		return suppCommResJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES)
				.getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject(JSON_PROP_VALUE)
				.getJSONObject(JSON_PROP_SUPPTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);

	}

	private static Map<String, Map<String, BigDecimal>> retrievePassengerQty(JSONObject reqCurrentDynamicPkg,
			JSONObject resdynamicPkgJson) {

		Map<String, String> paxTypeMap = new HashMap<String, String>();

		Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty = new HashMap<String, Map<String, BigDecimal>>();
		String productType = null;

		// to find which resGuestNumber corresponds to which paxType

		JSONArray resGuestArr = reqCurrentDynamicPkg.getJSONArray("resGuests");

		for (int j = 0; j < resGuestArr.length(); j++) {

			JSONObject resGuestJson = resGuestArr.getJSONObject(j);

			String resGuestNumber = resGuestJson.getString("resGuestRPH");
			String paxType = resGuestJson.getString("paxType");
			paxTypeMap.put(resGuestNumber, paxType);
		}

		JSONObject componentJson = reqCurrentDynamicPkg.getJSONObject(JSON_PROP_COMPONENTS);
		
		// For Hotel Component

		JSONArray hotelCompArr = componentJson.optJSONArray(JSON_PROP_HOTEL_COMPONENT);
		for (int k = 0; k < hotelCompArr.length(); k++) {

			String dynamicPkgAction = hotelCompArr.getJSONObject(k).getString(JSON_PROP_DYNAMICPKGACTION);

			JSONArray roomStayArr = hotelCompArr.getJSONObject(k).getJSONObject("roomStays").getJSONArray("roomStay");
			for (int l = 0; l < roomStayArr.length(); l++) {
				JSONObject roomStayJSon = roomStayArr.getJSONObject(l);
				if (dynamicPkgAction.equalsIgnoreCase(DYNAMICPKGACTION_HOTEL_TOUR)) {
					productType =  (dynamicPkgAction+roomStayJSon.getString("roomType")).toLowerCase();
				} else if (dynamicPkgAction.equalsIgnoreCase(DYNAMICPKGACTION_HOTEL_PRE)) {
					productType = JSON_PROP_PRENIGHT.toLowerCase();
					//TODO : some supplier doesnt send roomType so commented below code
					//productType =  (dynamicPkgAction+roomStayJSon.getString("roomType")).toLowerCase();
				} else if (dynamicPkgAction.contains(DYNAMICPKGACTION_HOTEL_POST)) {
					productType = JSON_PROP_POSTNIGHT.toLowerCase();
					//productType = (dynamicPkgAction+roomStayJSon.getString("roomType")).toLowerCase();
				}

				getcomponentWisePaxTypeQty(paxTypeMap, componentWisePaxTypeQty, productType, roomStayJSon);

			}
		}

		// for cruise component
		JSONArray cruiseCompRqArr = componentJson.optJSONArray(JSON_PROP_CRUISE_COMPONENT);
		
			if (cruiseCompRqArr.length() > 0) {
				for (int l = 0; l < cruiseCompRqArr.length(); l++) {
					String dynamicPkgActionCruise = cruiseCompRqArr.getJSONObject(l).getString(JSON_PROP_DYNAMICPKGACTION);
					JSONArray categoryOptionsArr = cruiseCompRqArr.getJSONObject(l)
							.getJSONArray(JSON_PROP_CATEGORYOPTIONS);
					for (int m = 0; m < categoryOptionsArr.length(); m++) {

						JSONArray categoryOptionArr = categoryOptionsArr.getJSONObject(m)
								.getJSONArray(JSON_PROP_CATEGORYOPTION);
						for (int n = 0; n < categoryOptionArr.length(); n++) {
							JSONObject categoryOptionJson = categoryOptionArr.getJSONObject(n);

							if (dynamicPkgActionCruise.equalsIgnoreCase(DYNAMICPKGACTION_CRUISE_TOUR)) {
								productType =  (dynamicPkgActionCruise+categoryOptionJson.getString(JSON_PROP_TYPE)).toLowerCase();
							} else if (dynamicPkgActionCruise.equalsIgnoreCase(DYNAMICPKGACTION_CRUISE_PRE)) {
								productType = JSON_PROP_PRENIGHT.toLowerCase();
							} else if (dynamicPkgActionCruise.contains(DYNAMICPKGACTION_CRUISE_POST)) {
								productType = JSON_PROP_POSTNIGHT.toLowerCase();
							}

							getcomponentWisePaxTypeQty(paxTypeMap, componentWisePaxTypeQty, productType,
									categoryOptionJson);

						}
					}
			}
		}
			
		

		// for Insurance component
		JSONArray insuranceCompArr = componentJson.getJSONArray("insuranceComponent");
		for (int n = 0; n < insuranceCompArr.length(); n++) {
			String dynamicPkgAction = insuranceCompArr.getJSONObject(n).getString("dynamicPkgAction");
			JSONObject insuranceJson = insuranceCompArr.getJSONObject(n);
			productType = (dynamicPkgAction+insuranceJson.getJSONObject("insCoverageDetail").getString("description")).toLowerCase();
			getcomponentWisePaxTypeQty(paxTypeMap, componentWisePaxTypeQty, productType, insuranceJson);
		}
		// for Transfer component

		JSONArray transferComponentArr = componentJson.getJSONArray("transferComponent");
		for (int p = 0; p < transferComponentArr.length(); p++) {

			String dynamicPkgActionTransfer = transferComponentArr.getJSONObject(p).getString("dynamicPkgAction");

			if (dynamicPkgActionTransfer.equalsIgnoreCase("PackageDepartureTransfer")) {
				productType = dynamicPkgActionTransfer.toLowerCase();
			} else if (dynamicPkgActionTransfer.equalsIgnoreCase("PackageArrivalTransfer")) {
				productType = dynamicPkgActionTransfer.toLowerCase();
			}

			JSONArray groundServiceArr = transferComponentArr.getJSONObject(p).getJSONArray("groundService");
			for (int q = 0; q < groundServiceArr.length(); q++) {
				JSONObject groundServiceJson = groundServiceArr.getJSONObject(q);
				getcomponentWisePaxTypeQty(paxTypeMap, componentWisePaxTypeQty, productType, groundServiceJson);
			}
		}
		// for other applicableOnProducts which are not in request
				JSONArray paxInfoArr = reqCurrentDynamicPkg.getJSONArray("paxInfo");
				Map<String, BigDecimal> defaultPaxTypeMap = new HashMap<String, BigDecimal>();
				for (int k = 0; k < paxInfoArr.length(); k++) {
					JSONObject paxInfoJson = paxInfoArr.getJSONObject(k);
					String paxType = paxInfoJson.getString("paxType");
					BigDecimal paxQty = new BigDecimal(paxInfoJson.getInt("quantity"));
					defaultPaxTypeMap.put(paxType, paxQty);
					productType = "default";

				}
				componentWisePaxTypeQty.put(productType, defaultPaxTypeMap);
		return componentWisePaxTypeQty;

	}

	private static void getcomponentWisePaxTypeQty(Map<String, String> paxTypeMap,
			Map<String, Map<String, BigDecimal>> componentWisePaxTypeQty, String productType,
			JSONObject compGuestJSon) {
		JSONArray guestCountArr = compGuestJSon.getJSONArray("guestCount");
		Map<String, BigDecimal> paxTypeQtyMap = new HashMap<String, BigDecimal>();
		BigDecimal paxQty = new BigDecimal(0);

		for (int m = 0; m < guestCountArr.length(); m++) {
			JSONObject guestCountJson = guestCountArr.getJSONObject(m);
			String resGuestNumber = String.valueOf(guestCountJson.get("resGuestRPH"));

			Iterator<Map.Entry<String, String>> paxTypeIter = paxTypeMap.entrySet().iterator();
			while (paxTypeIter.hasNext()) {
				Map.Entry<String, String> priceEntry = paxTypeIter.next();
				if (resGuestNumber.equalsIgnoreCase(priceEntry.getKey())) {
					if (paxTypeQtyMap.containsKey(priceEntry.getValue())) {

						paxQty = paxQty.add(paxQty);
						paxTypeQtyMap.put(priceEntry.getValue(), paxQty);
					} else {
						paxQty = new BigDecimal(1);
						paxTypeQtyMap.put(priceEntry.getValue(), paxQty);
					}

				}
			}
		}
		componentWisePaxTypeQty.put(productType, paxTypeQtyMap);
	}

	public static Element getSupplierCredentialsList(Document ownerDoc, UserContext usrCtx, String supplierID,
			int sequence, Element supplierCredentialsList) throws Exception {
		// Making request Header for particular supplierID

		// getting credentials and operation urls for particular supplier
		// ProductSupplier productSupplier = usrCtx.getSupplierForProduct(PRODUCT,
		// supplierID);
		ProductSupplier productSupplier = usrCtx.getSupplierForProduct(PROD_CATEG_HOLIDAYS, PROD_CATEG_HOLIDAYS,
				supplierID);

		if (productSupplier == null) {
			throw new Exception(String.format("Product supplier %s not found for user/client", supplierID));
		}

		// Setting the sequence, supplier credentials and urls for the header
		Element supplierCredentials = productSupplier.toElement(ownerDoc, sequence);

		supplierCredentialsList.appendChild(supplierCredentials);

		return supplierCredentialsList;
	}

	public static Element getResGuestElement(Document ownerDoc, JSONObject resGuest) {
		Element resGuestElement = ownerDoc.createElementNS(NS_OTA, "ns:ResGuest");

		Attr attributeResGuestRPH = ownerDoc.createAttribute("ResGuestRPH");
		attributeResGuestRPH.setValue(resGuest.getString("resGuestRPH"));
		resGuestElement.setAttributeNode(attributeResGuestRPH);

		Attr attributePrimaryIndicator = ownerDoc.createAttribute("PrimaryIndicator");
		attributePrimaryIndicator.setValue(Boolean.toString(resGuest.getBoolean("primaryIndicator")));
		resGuestElement.setAttributeNode(attributePrimaryIndicator);

		Attr attributeAge = ownerDoc.createAttribute("Age");
		attributeAge.setValue(resGuest.getString("age"));
		resGuestElement.setAttributeNode(attributeAge);

		Element profilesElement = ownerDoc.createElementNS(NS_OTA, "ns:Profiles");
		Element profileInfoElement = ownerDoc.createElementNS(NS_OTA, "ns:ProfileInfo");
		Element profileElement = ownerDoc.createElementNS(NS_OTA, "ns:Profile");
		Element customerElement = ownerDoc.createElementNS(NS_OTA, "ns:Customer");

		Attr attributeBirthDate = ownerDoc.createAttribute("BirthDate");
		attributeBirthDate.setValue(resGuest.getString("dob"));
		customerElement.setAttributeNode(attributeBirthDate);

		Attr attributeGender = ownerDoc.createAttribute("Gender");
		attributeGender.setValue(resGuest.getString("gender"));
		customerElement.setAttributeNode(attributeGender);

		// Setting person name element
		String personName = resGuest.getString("firstName");

		if (personName != null && personName.length() != 0) {
			Element personNameElement = ownerDoc.createElementNS(NS_OTA, "ns:PersonName");

			Element givenNameElement = ownerDoc.createElementNS(NS_OTA, "ns:GivenName");
			givenNameElement.setTextContent(resGuest.getString("firstName"));
			personNameElement.appendChild(givenNameElement);

			Element middleNameElement = ownerDoc.createElementNS(NS_OTA, "ns:MiddleName");
			middleNameElement.setTextContent(resGuest.getString("middleName"));
			personNameElement.appendChild(middleNameElement);

			Element surnameElement = ownerDoc.createElementNS(NS_OTA, "ns:Surname");
			surnameElement.setTextContent(resGuest.getString("surname"));
			personNameElement.appendChild(surnameElement);

			Element nameTitleElement = ownerDoc.createElementNS(NS_OTA, "ns:NameTitle");
			nameTitleElement.setTextContent(resGuest.getString("title"));
			personNameElement.appendChild(nameTitleElement);

			customerElement.appendChild(personNameElement);
		}

		// Setting telephone element
//		JSONArray contactInfoArray = resGuest.getJSONArray("contactInfo");
		//make contact info as jsonobject
				JSONObject contactInfo = resGuest.getJSONObject("contactInfo");
		
//		if (contactInfoArray != null && contactInfoArray.length() != 0) {
				if (contactInfo != null && contactInfo.length() != 0) {
		  
//		  for(int g=0;g<contactInfoArray.length();g++)
//		  {
//		    JSONObject contactInfo = contactInfoArray.getJSONObject(g);
		        
		    //Setting Telephone number
		    Element telephoneElement = ownerDoc.createElementNS(NS_OTA, "ns:Telephone");
        
        	Attr attributePhoneNumber = ownerDoc.createAttribute("PhoneNumber");
        	attributePhoneNumber.setValue(contactInfo.getString("phoneNumber"));
        	telephoneElement.setAttributeNode(attributePhoneNumber);
        		
        	telephoneElement.setAttribute("PhoneUseType", contactInfo.getString("contactType"));
        		
        	telephoneElement.setAttribute("AreaCityCode", contactInfo.getString("areaCityCode"));
        		
        	telephoneElement.setAttribute("CountryAccessCode", contactInfo.getString("countryCode"));
        
        	customerElement.appendChild(telephoneElement);
        
    		// Setting Email element
    		Element emailElement = ownerDoc.createElementNS(NS_OTA, "ns:Email");
    
    		emailElement.setTextContent(contactInfo.getString("email"));
    
    		customerElement.appendChild(emailElement);
//		  }
		}


		// Setting Address element
		JSONObject addressDetails = resGuest.getJSONObject("addressDetails");

		if (addressDetails != null && addressDetails.length() != 0) {
			Element addressElement = ownerDoc.createElementNS(NS_OTA, "ns:Address");

			Element addrLine1Element = ownerDoc.createElementNS(NS_OTA, "ns:AddressLine");
			addrLine1Element.setTextContent(addressDetails.getString("addrLine1"));
			addressElement.appendChild(addrLine1Element);

			Element addrLine2Element = ownerDoc.createElementNS(NS_OTA, "ns:AddressLine");
			addrLine2Element.setTextContent(addressDetails.getString("addrLine2"));
			addressElement.appendChild(addrLine2Element);

			Element cityElement = ownerDoc.createElementNS(NS_OTA, "ns:CityName");
			cityElement.setTextContent(addressDetails.getString("city"));
			addressElement.appendChild(cityElement);

			Element zipElement = ownerDoc.createElementNS(NS_OTA, "ns:PostalCode");
			zipElement.setTextContent(addressDetails.getString("zip"));
			addressElement.appendChild(zipElement);

			Element countryElement = ownerDoc.createElementNS(NS_OTA, "ns:CountryName");
			countryElement.setTextContent(addressDetails.getString("country"));
			addressElement.appendChild(countryElement);

			Element stateElement = ownerDoc.createElementNS(NS_OTA, "ns:StateProv");
			stateElement.setTextContent(addressDetails.getString("state"));
			addressElement.appendChild(stateElement);

			customerElement.appendChild(addressElement);
		}

		// Setting Document Element and appending it to customer element
		JSONArray documentInfo = resGuest.getJSONArray("documentInfo");

		if (documentInfo != null && documentInfo.length() != 0) {
			for (int i = 0; i < documentInfo.length(); i++) {
				JSONObject document = documentInfo.getJSONObject(i);

				Element documentElement = ownerDoc.createElementNS(NS_OTA, "ns:Document");

				Attr attributeDocType = ownerDoc.createAttribute("DocType");
				attributeDocType.setValue(document.getString("docType"));
				documentElement.setAttributeNode(attributeDocType);

				Attr attributeDocID = ownerDoc.createAttribute("DocID");
				attributeDocID.setValue(document.getString("docNumber"));
				documentElement.setAttributeNode(attributeDocID);

				customerElement.appendChild(documentElement);
			}
		}

		// Appending elements in backward sequencial order
		profileElement.appendChild(customerElement);
		profileInfoElement.appendChild(profileElement);
		profilesElement.appendChild(profileInfoElement);
		resGuestElement.appendChild(profilesElement);

		// Setting Document Element and appending it to customer element
		JSONObject specialRequests = resGuest.getJSONObject("specialRequests");

		if (specialRequests != null && specialRequests.length() != 0) {
			JSONArray specialRequestInfo = specialRequests.getJSONArray("specialRequestInfo");

			if (specialRequestInfo != null && specialRequestInfo.length() != 0) {
				Element specialRequestsElement = ownerDoc.createElementNS(NS_OTA, "ns:SpecialRequests");

				for (int i = 0; i < specialRequestInfo.length(); i++) {
					JSONObject specialRequest = specialRequestInfo.getJSONObject(i);

					Element specialRequestElement = ownerDoc.createElementNS(NS_OTA, "ns:SpecialRequest");

					Attr attributeCode = ownerDoc.createAttribute("RequestCode");
					attributeCode.setValue(specialRequest.getString("code"));
					specialRequestElement.setAttributeNode(attributeCode);

					Attr attributeName = ownerDoc.createAttribute("Name");
					attributeName.setValue(specialRequest.getString("name"));
					specialRequestElement.setAttributeNode(attributeName);

					specialRequestsElement.appendChild(specialRequestElement);
				}

				resGuestElement.appendChild(specialRequestsElement);
			}
		}

		return resGuestElement;
	}

	public static Element getGlobalInfoElement(Document ownerDoc, JSONObject globalInfo, Element globalInfoElement) {

		// Setting Comments Element
		JSONObject comments = globalInfo.getJSONObject("comments");

		Element commentsElement = ownerDoc.createElementNS(NS_OTA, "ns:Comments");

		if (comments != null && comments.length() != 0) {
			JSONArray comment = comments.getJSONArray(JSON_PROP_COMMENT);

			if (comment != null && comment.length() != 0) {
				for (int i = 0; i < comment.length(); i++) {
					JSONObject comm = comment.getJSONObject(i);

					Element commentElement = ownerDoc.createElementNS(NS_OTA, "ns:Comment");

					Attr attributeName = ownerDoc.createAttribute("Name");
					attributeName.setValue(comm.getString("Name"));
					commentElement.setAttributeNode(attributeName);

					Element textElement = ownerDoc.createElementNS(NS_OTA, "ns:Text");
					textElement.setTextContent(comm.getString("Text"));
					commentElement.appendChild(textElement);

					commentsElement.appendChild(commentElement);
				}

				globalInfoElement.appendChild(commentsElement);
			}
		}

		// Setting fees element
		JSONArray fees = globalInfo.getJSONArray(JSON_PROP_FEES);

		if (fees != null && fees.length() != 0) {
			Element feesElement = ownerDoc.createElementNS(NS_OTA, "ns:Fees");

			for (int j = 0; j < fees.length(); j++) {
				JSONObject fee = fees.getJSONObject(j);

				Element feeElement = ownerDoc.createElementNS(NS_OTA, "ns:Fee");

				Attr attributeCode = ownerDoc.createAttribute("Code");
				attributeCode.setValue(fee.getString("feeCode"));
				feeElement.setAttributeNode(attributeCode);

				Attr attributeQuantity = ownerDoc.createAttribute("MaxChargeUnitApplies");
				attributeQuantity.setValue(fee.getString("quantity"));
				feeElement.setAttributeNode(attributeQuantity);

				Element descriptionELement = ownerDoc.createElementNS(NS_OTA, "ns:Description");

				Attr attributeName = ownerDoc.createAttribute("Name");
				attributeName.setValue(fee.getString("feeName"));
				descriptionELement.setAttributeNode(attributeName);

				Element textElement = ownerDoc.createElementNS(NS_OTA, "ns:Text");
				textElement.setTextContent(fee.getString("text"));

				descriptionELement.appendChild(textElement);
				feeElement.appendChild(descriptionELement);
				feesElement.appendChild(feeElement);
			}

			globalInfoElement.appendChild(feesElement);
		}

		// Setting booking rules element
		JSONArray bookingRules = globalInfo.getJSONArray(JSON_PROP_BOOKINGRULE);

		Element bookingRulesElement = ownerDoc.createElementNS(NS_OTA, "ns:BookingRules");

		if (bookingRules != null && bookingRules.length() != 0) {
			for (int k = 0; k < bookingRules.length(); k++) {
				JSONObject bookingRule = bookingRules.getJSONObject(k);

				Element bookingRuleElement = ownerDoc.createElementNS(NS_OTA, "ns:BookingRule");

				Element descriptionElement = ownerDoc.createElementNS(NS_OTA, "ns:Description");

				Attr attributeName = ownerDoc.createAttribute("Name");
				attributeName.setValue(bookingRule.getString("name"));
				descriptionElement.setAttributeNode(attributeName);

				Element textElement = ownerDoc.createElementNS(NS_OTA, "ns:Text");
				textElement.setTextContent(bookingRule.getString("text"));
				descriptionElement.appendChild(textElement);

				bookingRuleElement.appendChild(descriptionElement);
				bookingRulesElement.appendChild(bookingRuleElement);
			}

			globalInfoElement.appendChild(bookingRulesElement);
		}

		// Setting Promotions element
		JSONArray promotions = globalInfo.getJSONArray(JSON_PROP_PROMOTION);

		Element promotionsElement = ownerDoc.createElementNS(NS_OTA, "ns:Promotions");

		if (promotions != null && promotions.length() != 0) {
			for (int y = 0; y < promotions.length(); y++) {
				JSONObject promotion = promotions.getJSONObject(y);

				Element promotionELement = ownerDoc.createElementNS(NS_OTA, "ns:Promotion");

				Attr attributeIsIncludedInTour = ownerDoc.createAttribute("isIncludedInTour");
				attributeIsIncludedInTour.setValue(Boolean.toString(promotion.getBoolean("isIncludedInTour")));
				promotionELement.setAttributeNode(attributeIsIncludedInTour);

				Attr attributeAmount = ownerDoc.createAttribute("Amount");
				attributeAmount.setValue(promotion.getString("amount"));
				promotionELement.setAttributeNode(attributeAmount);

				Attr attributeCurrencyCode = ownerDoc.createAttribute("CurrencyCode");
				attributeCurrencyCode.setValue(promotion.getString("currencyCode"));
				promotionELement.setAttributeNode(attributeCurrencyCode);

				Attr attributeID = ownerDoc.createAttribute("ID");
				attributeID.setValue(promotion.getString("id"));
				promotionELement.setAttributeNode(attributeID);

				Attr attributeDescription = ownerDoc.createAttribute("Description");
				attributeDescription.setValue(promotion.getString("description"));
				promotionELement.setAttributeNode(attributeDescription);

				promotionsElement.appendChild(promotionELement);
			}

			globalInfoElement.appendChild(promotionsElement);
		}

		return globalInfoElement;
	}

	public static Element getHotelComponentElement(Document ownerDoc, JSONArray hotelComponents,
			Element componentsElement) {
		for (int i = 0; i < hotelComponents.length(); i++) {
			JSONObject hotelComponent = hotelComponents.getJSONObject(i);

			Element hotelComponentElement = ownerDoc.createElementNS(NS_OTA, "ns:HotelComponent");

			if (!hotelComponent.has(JSON_PROP_DYNAMICPKGACTION)) {
				//code to add dynamicPkgAction in Hotel Component
				hotelComponent.put(JSON_PROP_DYNAMICPKGACTION, "PackageAccomodation");
			}
			
			// Setting DynamicPkgActon
			Attr attributeDynamicPkgAction = ownerDoc.createAttribute(DYNAMICPKGACTION);
			attributeDynamicPkgAction.setValue(hotelComponent.getString(JSON_PROP_DYNAMICPKGACTION));
			hotelComponentElement.setAttributeNode(attributeDynamicPkgAction);

			// Creating Room Stays ELement
			Element roomStaysElement = ownerDoc.createElementNS(NS_OTA, "ns:RoomStays");

			JSONObject roomStays = hotelComponent.getJSONObject("roomStays");
			JSONArray roomStay = roomStays.getJSONArray("roomStay");

			for (int j = 0; j < roomStay.length(); j++) {
				JSONObject roomSty = roomStay.getJSONObject(j);

				// Creating Room Stay element
				Element roomStayElement = ownerDoc.createElementNS(NS_OTA, "ns:RoomStay");

				// Setting Room Type Element
				String roomType = roomSty.getString("roomType");

				if (roomType != null && !roomType.isEmpty()) {
					Element roomTypesElement = ownerDoc.createElementNS(NS_OTA, "ns:RoomTypes");
					Element roomTypeElement = ownerDoc.createElementNS(NS_OTA, "ns:RoomType");

					Attr attributeRoomType = ownerDoc.createAttribute("RoomType");
					attributeRoomType.setValue(roomSty.getString("roomType"));
					roomTypeElement.setAttributeNode(attributeRoomType);

					roomTypesElement.appendChild(roomTypeElement);

					roomStayElement.appendChild(roomTypesElement);
				}

				// Setting Guest Count ELement
				JSONArray guestCounts = roomSty.getJSONArray("guestCount");

				if (guestCounts != null && guestCounts.length() != 0) {
					Element guestCountsElement = ownerDoc.createElementNS(NS_OTA, "ns:GuestCounts");

					for (int k = 0; k < guestCounts.length(); k++) {
						JSONObject guestCount = guestCounts.getJSONObject(k);

						Element guestCountElement = ownerDoc.createElementNS(NS_OTA, "ns:GuestCount");

						guestCountElement.setAttribute("ResGuestRPH",
								Integer.toString(guestCount.getInt("resGuestRPH")));

						/*
						 * Attr attributeResGuestRPH = ownerDoc.createAttribute("ResGuestRPH");
						 * attributeResGuestRPH.setValue(guestCount.getString("resGuestRPH"));
						 * guestCountElement.setAttributeNode(attributeResGuestRPH);
						 */

						guestCountsElement.appendChild(guestCountElement);
					}

					roomStayElement.appendChild(guestCountsElement);
				}

				// Setting TimeSpan Element
				JSONObject timeSpan = roomSty.getJSONObject("timeSpan");

				if (timeSpan != null && timeSpan.length() != 0) {
					Element timeSpanElement = ownerDoc.createElementNS(NS_OTA, "ns:TimeSpan");

					Attr attributeStart = ownerDoc.createAttribute("Start");
					attributeStart.setValue(timeSpan.getString("start"));
					timeSpanElement.setAttributeNode(attributeStart);

					Attr attributeDuration = ownerDoc.createAttribute("Duration");
						String duration = Integer.toString(timeSpan.optInt("duration"));
						if (duration != null && duration.length() != 0) {
							attributeDuration.setValue(Integer.toString(timeSpan.optInt("duration")));
						} else {
							attributeDuration.setValue("0");
						}
					timeSpanElement.setAttributeNode(attributeDuration);

					roomStayElement.appendChild(timeSpanElement);
				}

				// Setting Basic Property Info Element
				JSONArray basicPropertyInfoArr = roomSty.getJSONArray("basicPropertyInfo");
				for (int l = 0; l < basicPropertyInfoArr.length(); l++) {
					JSONObject basicPropertyInfo = basicPropertyInfoArr.getJSONObject(l);
					if (basicPropertyInfo != null && basicPropertyInfo.length() != 0) {
						Element basicPropertyInfoElement = ownerDoc.createElementNS(NS_OTA, "ns:BasicPropertyInfo");

						Attr attributeHotelCode = ownerDoc.createAttribute("HotelCode");
						attributeHotelCode.setValue(basicPropertyInfo.getString("hotelCode"));
						basicPropertyInfoElement.setAttributeNode(attributeHotelCode);

						roomStayElement.appendChild(basicPropertyInfoElement);
					}
				}

				roomStaysElement.appendChild(roomStayElement);
			}

			hotelComponentElement.appendChild(roomStaysElement);
			componentsElement.appendChild(hotelComponentElement);
		}

		return componentsElement;
	}

	public static Element getAirComponentElement(Document ownerDoc, JSONObject dynamicPackageObj,
			JSONArray airComponents, Element componentsElement, String supplierID) throws Exception {

		for (int i = 0; i < airComponents.length(); i++) {
			JSONObject airComponent = airComponents.getJSONObject(i);

			Element airComponentElement = ownerDoc.createElementNS(NS_OTA, "ns:AirComponent");

			// Setting DynamicPkgActon
			Attr attributeDynamicPkgAction = ownerDoc.createAttribute(DYNAMICPKGACTION);
			attributeDynamicPkgAction.setValue(airComponent.getString(JSON_PROP_DYNAMICPKGACTION));
			airComponentElement.setAttributeNode(attributeDynamicPkgAction);

			// Setting AirItinerary Element
			JSONObject airItinerary = airComponent.getJSONObject("airItinerary");

			Element airItineraryElement = ownerDoc.createElementNS(NS_OTA, "ns:AirItinerary");

			Attr attributeAirItineraryRPH = ownerDoc.createAttribute("AirItineraryRPH");
			attributeAirItineraryRPH.setValue(airItinerary.getString("airItineraryRPH"));
			airItineraryElement.setAttributeNode(attributeAirItineraryRPH);

			JSONArray ODOs = airItinerary.getJSONArray("originDestinationOptions");

			if (ODOs == null && ODOs.length() != 0) {
				throw new Exception(String.format(
						"Object originDestinationOptions must be set for supplier %s if flights are to be availed",
						supplierID));
			}

			// Creating ODOs Element
			Element ODOsElement = ownerDoc.createElementNS(NS_OTA, "ns:OriginDestinationOptions");

			for (int j = 0; j < ODOs.length(); j++) {
				JSONObject ODO = ODOs.getJSONObject(j);

				// Creating ODO Element
				Element ODOElement = ownerDoc.createElementNS(NS_OTA, "ns:OriginDestinationOption");

				JSONArray flightSegments = ODO.getJSONArray("flightSegment");

				for (int k = 0; k < flightSegments.length(); k++) {
					JSONObject flightSegment = flightSegments.getJSONObject(k);

					// Creating Flight Segment Element
					Element flightSegmentElement = ownerDoc.createElementNS(NS_OTA, "ns:FlightSegment");

					Attr attributeFlightNumber = ownerDoc.createAttribute("FlightNumber");
					attributeFlightNumber.setValue(flightSegment.getString("flightNumber"));
					flightSegmentElement.setAttributeNode(attributeFlightNumber);

					Attr attributeDepartureDate = ownerDoc.createAttribute("DepartureDateTime");
					attributeDepartureDate.setValue(flightSegment.getString("departureDate"));
					flightSegmentElement.setAttributeNode(attributeDepartureDate);

					Attr attributeArrivalDate = ownerDoc.createAttribute("ArrivalDateTime");
					attributeArrivalDate.setValue(flightSegment.getString("arrivalDate"));
					flightSegmentElement.setAttributeNode(attributeArrivalDate);

					Attr attributeStopQuantity = ownerDoc.createAttribute("StopQuantity");
					attributeStopQuantity.setValue(flightSegment.getString("stopQuantity"));
					flightSegmentElement.setAttributeNode(attributeStopQuantity);

					Attr attributeNumberInParty = ownerDoc.createAttribute("NumberInParty");
					attributeNumberInParty.setValue(flightSegment.getString("numberInParty"));
					flightSegmentElement.setAttributeNode(attributeNumberInParty);

					// Setting departure Airport element
					Element departureAirportElement = ownerDoc.createElementNS(NS_OTA, "ns:DepartureAirport");

					Attr attributeLocationCode = ownerDoc.createAttribute("LocationCode");
					attributeLocationCode.setValue(flightSegment.getString("originLocation"));
					departureAirportElement.setAttributeNode(attributeLocationCode);

					Attr attributeTerminal = ownerDoc.createAttribute("Terminal");
					attributeTerminal.setValue(flightSegment.getString("departureTerminal"));
					departureAirportElement.setAttributeNode(attributeTerminal);

					flightSegmentElement.appendChild(departureAirportElement);

					// Setting Arrival Airport Element
					Element arrivalAirportElement = ownerDoc.createElementNS(NS_OTA, "ns:ArrivalAirport");

					Attr attributeArrLocationCode = ownerDoc.createAttribute("LocationCode");
					attributeArrLocationCode.setValue(flightSegment.getString("destinationLocation"));
					arrivalAirportElement.setAttributeNode(attributeArrLocationCode);

					Attr attributeArrTerminal = ownerDoc.createAttribute("Terminal");
					attributeArrTerminal.setValue(flightSegment.getString("arrivalTerminal"));
					arrivalAirportElement.setAttributeNode(attributeArrTerminal);

					flightSegmentElement.appendChild(arrivalAirportElement);

					// Setting Operating Airline Element
					JSONObject operatingAirline = flightSegment.getJSONObject("operatingAirline");

					if (operatingAirline != null && operatingAirline.length() != 0) {
						Element operatingAirlinrElement = ownerDoc.createElementNS(NS_OTA, "ns:OperatingAirline");

						Attr attributeFlightNum = ownerDoc.createAttribute("FlightNumber");
						attributeFlightNum.setValue(operatingAirline.getString("flightNumber"));
						operatingAirlinrElement.setAttributeNode(attributeFlightNum);

						Attr attributeCompanyShortName = ownerDoc.createAttribute("CompanyShortName");
						attributeCompanyShortName.setValue(operatingAirline.getString("companyShortName"));
						operatingAirlinrElement.setAttributeNode(attributeCompanyShortName);

						Attr attributeCode = ownerDoc.createAttribute("Code");
						attributeCode.setValue(operatingAirline.getString("airlineCode"));
						operatingAirlinrElement.setAttributeNode(attributeCode);

						flightSegmentElement.appendChild(operatingAirlinrElement);
					}

					// Setting BookingClassAvails element
					Element bookingClassAvailsElement = ownerDoc.createElementNS(NS_OTA, "ns:BookingClassAvails");

					Attr attributeCabinType = ownerDoc.createAttribute("CabinType");
					attributeCabinType.setValue(flightSegment.getString("cabinType"));
					bookingClassAvailsElement.setAttributeNode(attributeCabinType);

					Element bookingClassAvailElement = ownerDoc.createElementNS(NS_OTA, "ns:BookingClassAvail");

					Attr attributeRBD = ownerDoc.createAttribute("ResBookDesigCode");
					attributeRBD.setValue(flightSegment.getString("resBookDesigCode"));
					bookingClassAvailElement.setAttributeNode(attributeRBD);

					bookingClassAvailsElement.appendChild(bookingClassAvailElement);

					flightSegmentElement.appendChild(bookingClassAvailsElement);

					ODOElement.appendChild(flightSegmentElement);
				}

				ODOsElement.appendChild(ODOElement);
			}

			airItineraryElement.appendChild(ODOsElement);

			airComponentElement.appendChild(airItineraryElement);

			// Creating Air Itinerary Pricing Info Element
			JSONObject airItineraryPricingInfo = airComponent.getJSONObject("airItineraryPricingInfo");

			if (airItineraryPricingInfo != null && airItineraryPricingInfo.length() != 0) {
				Element airItineraryPricingInfoElement = ownerDoc.createElementNS(NS_OTA, "ns:AirItineraryPricingInfo");

				// Creating ItinTotalFare Element
				JSONObject itinTotalFare = airItineraryPricingInfo.getJSONObject("itinTotalFare");

				Element ItinElement = ownerDoc.createElementNS(NS_OTA, "ns:ItinTotalFare");

				// Setting Base Fare Element
				JSONObject baseFare = itinTotalFare.getJSONObject("baseFare");

				if (baseFare != null && baseFare.length() != 0) {
					Element baseFareElement = ownerDoc.createElementNS(NS_OTA, "ns:BaseFare");

					/*
					 * Attr attributeBaseFAT = ownerDoc.createAttribute("FareAmountType");
					 * attributeBaseFAT.setValue(baseFare.getString("fareAmountType"));
					 * baseFareElement.setAttributeNode(attributeBaseFAT);
					 */

					Attr attributeBaseCurrency = ownerDoc.createAttribute("CurrencyCode");
					attributeBaseCurrency.setValue(baseFare.getString("currencyCode"));
					baseFareElement.setAttributeNode(attributeBaseCurrency);

					Attr attributeBaseDecmlPt = ownerDoc.createAttribute("DecimalPlaces");
					attributeBaseDecmlPt.setValue(Integer.toString(baseFare.getInt("decimalPlaces")));
					baseFareElement.setAttributeNode(attributeBaseDecmlPt);

					Attr attributeBaseAmount = ownerDoc.createAttribute("Amount");
					attributeBaseAmount.setValue(Integer.toString(baseFare.getInt("amount")));
					baseFareElement.setAttributeNode(attributeBaseAmount);

					ItinElement.appendChild(baseFareElement);
				}

				// Setting Taxes Element
				JSONObject taxes = itinTotalFare.getJSONObject("taxes");

				if (taxes != null && taxes.length() != 0) {
					Element taxesElement = ownerDoc.createElementNS(NS_OTA, "ns:Taxes");

					Attr attributeTaxesAmount = ownerDoc.createAttribute("Amount");
					attributeTaxesAmount.setValue(Integer.toString(taxes.getInt("amount")));
					taxesElement.setAttributeNode(attributeTaxesAmount);

					JSONArray tax = taxes.getJSONArray("tax");

					if (tax != null && tax.length() != 0) {
						for (int x = 0; x < tax.length(); x++) {
							JSONObject taxx = tax.getJSONObject(x);

							Element taxElement = ownerDoc.createElementNS(NS_OTA, "ns:Tax");

							Attr attributeTaxCode = ownerDoc.createAttribute("TaxCode");
							attributeTaxCode.setValue(taxx.getString("taxCode"));
							taxElement.setAttributeNode(attributeTaxCode);

							Attr attributeTaxName = ownerDoc.createAttribute("TaxName");
							attributeTaxName.setValue(taxx.getString("taxName"));
							taxElement.setAttributeNode(attributeTaxName);

							Attr attributeTaxCurrency = ownerDoc.createAttribute("CurrencyCode");
							attributeTaxCurrency.setValue(taxx.getString("currencyCode"));
							taxElement.setAttributeNode(attributeTaxCurrency);

							Attr attributeTaxDecmlPt = ownerDoc.createAttribute("DecimalPlaces");
							attributeTaxDecmlPt.setValue(Integer.toString(taxx.getInt("decimalPlaces")));
							taxElement.setAttributeNode(attributeTaxDecmlPt);

							Attr attributeTaxAmount = ownerDoc.createAttribute("Amount");
							attributeTaxAmount.setValue(Integer.toString(taxx.getInt("amount")));
							taxElement.setAttributeNode(attributeTaxAmount);

							taxesElement.appendChild(taxElement);
						}

					}

					ItinElement.appendChild(taxesElement);
				}

				// Setting Total Fare Element
				JSONObject totalFare = itinTotalFare.getJSONObject("totalFare");

				if (totalFare != null && totalFare.length() != 0) {
					Element totalFareElement = ownerDoc.createElementNS(NS_OTA, "ns:TotalFare");

					Attr attributeTotalPQ = ownerDoc.createAttribute("PassengerQuantity");
					attributeTotalPQ.setValue(Integer.toString(totalFare.getInt("passengerQuantity")));
					totalFareElement.setAttributeNode(attributeTotalPQ);

					Attr attributeTotalCurrency = ownerDoc.createAttribute("CurrencyCode");
					attributeTotalCurrency.setValue(totalFare.getString("currencyCode"));
					totalFareElement.setAttributeNode(attributeTotalCurrency);

					Attr attributeTotalAmount = ownerDoc.createAttribute("Amount");
					attributeTotalAmount.setValue(Integer.toString(totalFare.getInt("amount")));
					totalFareElement.setAttributeNode(attributeTotalAmount);

					ItinElement.appendChild(totalFareElement);
				}

				airItineraryPricingInfoElement.appendChild(ItinElement);

				// Setting Fare Info Element
				JSONArray fareInfos = airItineraryPricingInfo.getJSONArray("fareInfo");

				if (fareInfos != null && fareInfos.length() != 0) {
					Element fareInfosElement = ownerDoc.createElementNS(NS_OTA, "ns:FareInfos");

					for (int z = 0; z < fareInfos.length(); z++) {
						JSONObject fareInfo = fareInfos.getJSONObject(z);

						Element fareInfoElement = ownerDoc.createElementNS(NS_OTA, "ns:FareInfo");

						JSONObject discountPricing = fareInfo.getJSONObject("discountPricing");

						Element discountPricingElement = ownerDoc.createElementNS(NS_OTA, "ns:DiscountPricing");

						Attr attributePurpose = ownerDoc.createAttribute("Purpose");
						attributePurpose.setValue(discountPricing.getString("purpose"));
						discountPricingElement.setAttributeNode(attributePurpose);

						Attr attributeType = ownerDoc.createAttribute("Type");
						attributeType.setValue(discountPricing.getString("type"));
						discountPricingElement.setAttributeNode(attributeType);

						Attr attributeDiscount = ownerDoc.createAttribute("Discount");
						attributeDiscount.setValue(discountPricing.getString("discount"));
						discountPricingElement.setAttributeNode(attributeDiscount);

						fareInfoElement.appendChild(discountPricingElement);

						fareInfosElement.appendChild(fareInfoElement);
					}

					airItineraryPricingInfoElement.appendChild(fareInfosElement);
				}

				airComponentElement.appendChild(airItineraryPricingInfoElement);
			}

			// Creating Traveler Info Summary Element
			JSONArray travelerInfoSummary = dynamicPackageObj.getJSONArray("paxInfo");

			if (travelerInfoSummary != null && travelerInfoSummary.length() != 0) {
				Element tpaElement = ownerDoc.createElementNS(NS_OTA, "ns:TPA_Extensions");

				Element travelRefSummaryElement = ownerDoc.createElementNS(NS_PAC, "tns:TravelRefSummary");

				Element passengerTypeQuantitiesElement = ownerDoc.createElementNS(NS_OTA, "ns:PassengerTypeQuantities");

				for (int g = 0; g < travelerInfoSummary.length(); g++) {
					JSONObject travelerInfoSmry = travelerInfoSummary.getJSONObject(g);

					Element passengerTypeQuantityElement = ownerDoc.createElementNS(NS_OTA, "ns:PassengerTypeQuantity");

					String code = travelerInfoSmry.getString("paxType");
					if (code != null && code.length() != 0)
						passengerTypeQuantityElement.setAttribute("Code", code);

					String quantity = Integer.toString(travelerInfoSmry.getInt("quantity"));
					if (quantity != null && quantity.length() != 0)
						passengerTypeQuantityElement.setAttribute("Quantity", quantity);

					passengerTypeQuantitiesElement.appendChild(passengerTypeQuantityElement);
				}

				travelRefSummaryElement.appendChild(passengerTypeQuantitiesElement);
				tpaElement.appendChild(travelRefSummaryElement);
				airComponentElement.appendChild(tpaElement);
			}

			componentsElement.appendChild(airComponentElement);
		}

		return componentsElement;
	}

	public static Element getCruiseComponentElement(Document ownerDoc, JSONArray cruiseComponents, Element tpaElement) {
		Element cruiseComponentsElement = ownerDoc.createElementNS(NS_PAC, "tns:CruiseComponents");

		for (int i = 0; i < cruiseComponents.length(); i++) {
			JSONObject cruiseComponent = cruiseComponents.getJSONObject(i);

			Element cruiseComponentElement = ownerDoc.createElementNS(NS_PAC, "tns:CruiseComponent");
			
			if (!cruiseComponent.has(JSON_PROP_DYNAMICPKGACTION)) {
				//code to add dynamicPkgAction in Hotel Component
				cruiseComponent.put(JSON_PROP_DYNAMICPKGACTION, "PackageCruise");	
			}
			// Setting DynamicPkgActon
			Attr attributeDynamicPkgAction = ownerDoc.createAttribute(DYNAMICPKGACTION);
			attributeDynamicPkgAction.setValue(cruiseComponent.getString(JSON_PROP_DYNAMICPKGACTION));
			cruiseComponentElement.setAttributeNode(attributeDynamicPkgAction);

			Attr attributeID = ownerDoc.createAttribute("ID");
			attributeID.setValue(cruiseComponent.getString("id"));
			cruiseComponentElement.setAttributeNode(attributeID);

			

			//check if dynamicpkgaction not pre or post night then only this attribute can set.
			if(!cruiseComponent.getString(JSON_PROP_DYNAMICPKGACTION).equals("PreNightPackageAccomodation") && !cruiseComponent.getString(JSON_PROP_DYNAMICPKGACTION).equals("PostNightPackageAccomodation")) {
			Attr attributeName = ownerDoc.createAttribute("Name");
			attributeName.setValue(cruiseComponent.getString("name"));
			cruiseComponentElement.setAttributeNode(attributeName);
			}
			
			//check if dynamicpkgaction not pre or post night then only this attribute can set.
			if(!cruiseComponent.getString(JSON_PROP_DYNAMICPKGACTION).equals("PreNightPackageAccomodation") && !cruiseComponent.getString(JSON_PROP_DYNAMICPKGACTION).equals("PostNightPackageAccomodation")) {
			JSONArray categoryOptions = cruiseComponent.getJSONArray("categoryOptions");
			for (int j = 0; j < categoryOptions.length(); j++) {
				JSONObject catgryOptions = categoryOptions.getJSONObject(j);

				Element categoryOptionsElement = ownerDoc.createElementNS(NS_PAC, "tns:CategoryOptions");

				JSONArray categoryOption = catgryOptions.getJSONArray("categoryOption");

				for (int k = 0; k < categoryOption.length(); k++) {
					JSONObject catgryOption = categoryOption.getJSONObject(k);

					Element categoryOptionElement = ownerDoc.createElementNS(NS_PAC, "tns:CategoryOption");

					Attr attributeStatus = ownerDoc.createAttribute("AvailabilityStatus");
					attributeStatus.setValue(catgryOption.getString("availabilityStatus"));
					categoryOptionElement.setAttributeNode(attributeStatus);

					Attr attributeCatgryName = ownerDoc.createAttribute("Name");
					attributeCatgryName.setValue(catgryOption.getString("name"));
					categoryOptionElement.setAttributeNode(attributeCatgryName);

					Attr attributeDescription = ownerDoc.createAttribute("Description");
					attributeDescription.setValue(catgryOption.getString("description"));
					categoryOptionElement.setAttributeNode(attributeDescription);

					Attr attributeType = ownerDoc.createAttribute("Type");
					attributeType.setValue(catgryOption.getString("type"));
					categoryOptionElement.setAttributeNode(attributeType);

					// Setting Guest Count Element
					JSONArray guestCounts = catgryOption.getJSONArray("guestCount");

					if (guestCounts != null && guestCounts.length() != 0) {
						Element guestCountsElement = ownerDoc.createElementNS(NS_PAC, "tns:GuestCounts");

						for (int y = 0; y < guestCounts.length(); y++) {
							JSONObject guestCount = guestCounts.getJSONObject(y);

							Element guestCountElement = ownerDoc.createElementNS(NS_OTA, "ns:GuestCount");

							Attr attributeResGuestRPH = ownerDoc.createAttribute("ResGuestRPH");
							attributeResGuestRPH.setValue(Integer.toString(guestCount.getInt("resGuestRPH")));
							guestCountElement.setAttributeNode(attributeResGuestRPH);

							guestCountsElement.appendChild(guestCountElement);
						}

						categoryOptionElement.appendChild(guestCountsElement);
					}

					// Setting TimeSpan Element
					JSONObject timeSpan = catgryOption.getJSONObject("timeSpan");

					if (timeSpan != null && timeSpan.length() != 0) {
						Element timeSpanElement = ownerDoc.createElementNS(NS_PAC, "tns:TimeSpan");

						Attr attributeStart = ownerDoc.createAttribute("Start");
						attributeStart.setValue(timeSpan.getString("start"));
						timeSpanElement.setAttributeNode(attributeStart);

						Attr attributeDuration = ownerDoc.createAttribute("Duration");
						
								String duration = Integer.toString(timeSpan.optInt("duration"));
								if (duration != null && duration.length() != 0) {
									attributeDuration.setValue(Integer.toString(timeSpan.optInt("duration")));
								} else {
									attributeDuration.setValue("0");
								}
							
						timeSpanElement.setAttributeNode(attributeDuration);

						Attr attributeEnd = ownerDoc.createAttribute("End");
						attributeEnd.setValue(timeSpan.getString("end"));
						timeSpanElement.setAttributeNode(attributeEnd);

						categoryOptionElement.appendChild(timeSpanElement);
					}

					// Setting Cabin Options Element
					JSONArray cabinOptions = catgryOption.getJSONArray("cabinOption");

					if (cabinOptions != null && cabinOptions.length() != 0) {
						Element cabinOptionsElement = ownerDoc.createElementNS(NS_PAC, "tns:CabinOptions");

						for (int x = 0; x < cabinOptions.length(); x++) {
							JSONObject cabinOption = cabinOptions.getJSONObject(x);

							Element cabinOptionElement = ownerDoc.createElementNS(NS_PAC, "tns:CabinOption");

							Attr attributeCabinStatus = ownerDoc.createAttribute("Status");
							attributeCabinStatus.setValue(cabinOption.getString("status"));
							cabinOptionElement.setAttributeNode(attributeCabinStatus);

							Attr attributeCabinNumber = ownerDoc.createAttribute("CabinNumber");
							attributeCabinNumber.setValue(cabinOption.getString("cabinNumber"));
//							attributeCabinNumber.setValue(cabinOption.getInt("cabinNumber")+"");
							cabinOptionElement.setAttributeNode(attributeCabinNumber);

							Attr attributeMaxOccupancy = ownerDoc.createAttribute("MaxOccupancy");
							attributeMaxOccupancy.setValue(Integer.toString(cabinOption.getInt("maxOccupancy")));
							cabinOptionElement.setAttributeNode(attributeMaxOccupancy);

							Attr attributeMinOccupancy = ownerDoc.createAttribute("MinOccupancy");
							attributeMinOccupancy.setValue(Integer.toString(cabinOption.getInt("minOccupancy")));
							cabinOptionElement.setAttributeNode(attributeMinOccupancy);

							cabinOptionsElement.appendChild(cabinOptionElement);
						}

						categoryOptionElement.appendChild(cabinOptionsElement);
					}

					categoryOptionsElement.appendChild(categoryOptionElement);
				}

				cruiseComponentElement.appendChild(categoryOptionsElement);
			}

			}
			cruiseComponentsElement.appendChild(cruiseComponentElement);
		}

		tpaElement.appendChild(cruiseComponentsElement);

		return tpaElement;
	}

	public static Element getTransferComponentElement(Document ownerDoc, JSONArray transfersComponents,
			Element tpaElement) {
		Element transferComponentsElement = ownerDoc.createElementNS(NS_PAC, "tns:TransferComponents");

		for (int i = 0; i < transfersComponents.length(); i++) {
			JSONObject transfersComponent = transfersComponents.getJSONObject(i);

			Element transferComponentElement = ownerDoc.createElementNS(NS_PAC, "tns:TransferComponent");
			
			// Setting DynamicPkgActon
			Attr attributeDynamicPkgAction = ownerDoc.createAttribute(DYNAMICPKGACTION);
			attributeDynamicPkgAction.setValue(transfersComponent.getString(JSON_PROP_DYNAMICPKGACTION));
			transferComponentElement.setAttributeNode(attributeDynamicPkgAction);

			Attr attributeID = ownerDoc.createAttribute("ID");
			attributeID.setValue(transfersComponent.getString("id"));
			transferComponentElement.setAttributeNode(attributeID);

			Attr attributeCreatedDate = ownerDoc.createAttribute("CreatedDate");
			attributeCreatedDate.setValue(transfersComponent.getString("createdDate"));
			transferComponentElement.setAttributeNode(attributeCreatedDate);

			Attr attributeExpDate = ownerDoc.createAttribute("option_expiry_date");
			attributeExpDate.setValue(transfersComponent.getString("option_expiry_date"));
			transferComponentElement.setAttributeNode(attributeExpDate);

			Attr attributeStatus = ownerDoc.createAttribute("AvailabilityStatus");
			attributeStatus.setValue(transfersComponent.getString("availabilityStatus"));
			transferComponentElement.setAttributeNode(attributeStatus);

			JSONArray groundServices = transfersComponent.getJSONArray("groundService");

			for (int j = 0; j < groundServices.length(); j++) {
				JSONObject groundService = groundServices.getJSONObject(j);

				// Setting Ground Service Element
				Element groundServiceElement = ownerDoc.createElementNS(NS_PAC, "tns:GroundService");

				Attr attributeName = ownerDoc.createAttribute("Name");
				attributeName.setValue(groundService.getString("name"));
				groundServiceElement.setAttributeNode(attributeName);

				Attr attributeDesc = ownerDoc.createAttribute("Description");
				attributeDesc.setValue(groundService.getString("description"));
				groundServiceElement.setAttributeNode(attributeDesc);

				Attr attributeDep = ownerDoc.createAttribute("DepartureCity");
				attributeDep.setValue(groundService.getString("departureCity"));
				groundServiceElement.setAttributeNode(attributeDep);

				Attr attributeArr = ownerDoc.createAttribute("ArrivalCity");
				attributeArr.setValue(groundService.getString("arrivalCity"));
				groundServiceElement.setAttributeNode(attributeArr);

				Attr attributeDepDate = ownerDoc.createAttribute("DepartureDate");
				attributeDepDate.setValue(groundService.getString("departureDate"));
				groundServiceElement.setAttributeNode(attributeDepDate);

				Attr attributeArrDate = ownerDoc.createAttribute("ArrivalDate");
				attributeArrDate.setValue(groundService.getString("arrivalDate"));
				groundServiceElement.setAttributeNode(attributeArrDate);

				// Setting Location Element
				JSONObject location = groundService.getJSONObject("location");

				if (location != null && location.length() != 0) {
					Element locationElement = ownerDoc.createElementNS(NS_PAC, "tns:Location");

					Element pickUpElement = ownerDoc.createElementNS(NS_OTA, "ns:Pickup");

					Element airportInfoElement = ownerDoc.createElementNS(NS_OTA, "ns:AirportInfo");

					Element departureElement = ownerDoc.createElementNS(NS_OTA, "ns:Departure");

					Attr attributeLocation = ownerDoc.createAttribute("LocationCode");
					attributeLocation.setValue(location.getString("pickUpLocation"));
					departureElement.setAttributeNode(attributeLocation);

					Attr attributeAirport = ownerDoc.createAttribute("AirportName");
					attributeAirport.setValue(location.getString("airportName"));
					departureElement.setAttributeNode(attributeAirport);

					Attr attributeCodeContext = ownerDoc.createAttribute("CodeContext");
					attributeCodeContext.setValue(location.getString("codeContext"));
					departureElement.setAttributeNode(attributeCodeContext);

					airportInfoElement.appendChild(departureElement);
					pickUpElement.appendChild(airportInfoElement);
					locationElement.appendChild(pickUpElement);

					groundServiceElement.appendChild(locationElement);
				}
				
				// Setting Guest Count ELement
                JSONArray guestCounts = groundService.getJSONArray("guestCount");

                if (guestCounts != null && guestCounts.length() != 0) {
                    Element guestCountsElement = ownerDoc.createElementNS(NS_OTA, "ns:GuestCounts");

                    for (int k = 0; k < guestCounts.length(); k++) {
                        JSONObject guestCount = guestCounts.getJSONObject(k);

                        Element guestCountElement = ownerDoc.createElementNS(NS_OTA, "ns:GuestCount");

                        guestCountElement.setAttribute("ResGuestRPH",
                                Integer.toString(guestCount.getInt("resGuestRPH")));

                        /*
                         * Attr attributeResGuestRPH = ownerDoc.createAttribute("ResGuestRPH");
                         * attributeResGuestRPH.setValue(guestCount.getString("resGuestRPH"));
                         * guestCountElement.setAttributeNode(attributeResGuestRPH);
                         */

                        guestCountsElement.appendChild(guestCountElement);
                    }

                    groundServiceElement.appendChild(guestCountsElement);
                }

				// Setting Total Charge Element
				JSONArray totalCharges = groundService.getJSONArray("totalCharge");

				if (totalCharges != null && totalCharges.length() != 0) {
					for (int z = 0; z < totalCharges.length(); z++) {
						JSONObject totalCharge = totalCharges.getJSONObject(z);

						Element totalChargeElement = ownerDoc.createElementNS(NS_PAC, "tns:TotalCharge");

						Attr attributeAmount = ownerDoc.createAttribute("RateTotalAmount");
						attributeAmount.setValue(Integer.toString(totalCharge.getInt("rateTotalAmount")));
						totalChargeElement.setAttributeNode(attributeAmount);

						Attr attributeCurrency = ownerDoc.createAttribute("CurrencyCode");
						attributeCurrency.setValue(totalCharge.getString("currencyCode"));
						totalChargeElement.setAttributeNode(attributeCurrency);

						Attr attributeType = ownerDoc.createAttribute("Type");
						attributeType.setValue(totalCharge.getString("type"));
						totalChargeElement.setAttributeNode(attributeType);

						groundServiceElement.appendChild(totalChargeElement);
					}
				}

				// Setting Time Span
				JSONObject timeSpan = groundService.getJSONObject("timeSpan");

				if (timeSpan != null && timeSpan.length() != 0) {
					Element timeSpanElement = ownerDoc.createElementNS(NS_PAC, "tns:TimeSpan");

					Attr attributeStart = ownerDoc.createAttribute("Start");
					attributeStart.setValue(timeSpan.getString("start"));
					timeSpanElement.setAttributeNode(attributeStart);

					Attr attributeDuration = ownerDoc.createAttribute("Duration");
					attributeDuration.setValue(timeSpan.getString("duration"));
					timeSpanElement.setAttributeNode(attributeDuration);

					Attr attributeEnd = ownerDoc.createAttribute("End");
					attributeEnd.setValue(timeSpan.getString("end"));
					timeSpanElement.setAttributeNode(attributeEnd);

					groundServiceElement.appendChild(timeSpanElement);
				}

				// Setting remaining Elements
				Element airInclusiveBookingElement = ownerDoc.createElementNS(NS_PAC, "tns:AirInclusiveBooking");
				airInclusiveBookingElement
						.setTextContent(Boolean.toString(groundService.getBoolean("airInclusiveBooking")));
				groundServiceElement.appendChild(airInclusiveBookingElement);

				Element withExtraNightsElement = ownerDoc.createElementNS(NS_PAC, "tns:WithExtraNights");
				withExtraNightsElement.setTextContent(Boolean.toString(groundService.getBoolean("withExtraNights")));
				groundServiceElement.appendChild(withExtraNightsElement);

				Element declineRequiredElement = ownerDoc.createElementNS(NS_PAC, "tns:DeclineRequired");
				declineRequiredElement.setTextContent(Boolean.toString(groundService.getBoolean("declineRequired")));
				groundServiceElement.appendChild(declineRequiredElement);

				Element purchasableElement = ownerDoc.createElementNS(NS_PAC, "tns:Purchasable");
				purchasableElement.setTextContent(Boolean.toString(groundService.getBoolean("purchasable")));
				groundServiceElement.appendChild(purchasableElement);

				Element flightInfoRequiredElement = ownerDoc.createElementNS(NS_PAC, "tns:FlightInfoRequired");
				flightInfoRequiredElement
						.setTextContent(Boolean.toString(groundService.getBoolean("flightInfoRequired")));
				groundServiceElement.appendChild(flightInfoRequiredElement);

				Element isIncludedInTourElement = ownerDoc.createElementNS(NS_PAC, "tns:isIncludedInTour");
				isIncludedInTourElement.setTextContent(Boolean.toString(groundService.getBoolean("isIncludedInTour")));
				groundServiceElement.appendChild(isIncludedInTourElement);

				// Appending Ground Service Element to Transfer Component
				transferComponentElement.appendChild(groundServiceElement);
			}

			// Appending Transfer Component Elements to Transfer Components
			transferComponentsElement.appendChild(transferComponentElement);
		}

		// Appending Transfer Components Element to TPA Element
		tpaElement.appendChild(transferComponentsElement);

		return tpaElement;
	}

	public static Element getInsuranceComponentElement(Document ownerDoc, JSONArray insuranceComponents,
			Element tpaElement) {
		Element insuranceComponentsElement = ownerDoc.createElementNS(NS_PAC, "tns:InsuranceComponents");

		for (int i = 0; i < insuranceComponents.length(); i++) {
			JSONObject insuranceComponent = insuranceComponents.getJSONObject(i);

			Element insuranceComponentElement = ownerDoc.createElementNS(NS_PAC, "tns:InsuranceComponent");

			//check if dynamicPkgAction is present if not then set 
			if (!insuranceComponent.has(JSON_PROP_DYNAMICPKGACTION)) {
				insuranceComponent.put(JSON_PROP_DYNAMICPKGACTION, "PackageInsurance");
			}
			//end
			// Setting DynamicPkgActon
			Attr attributeDynamicPkgAction = ownerDoc.createAttribute(DYNAMICPKGACTION);
			attributeDynamicPkgAction.setValue(insuranceComponent.getString(JSON_PROP_DYNAMICPKGACTION));
			insuranceComponentElement.setAttributeNode(attributeDynamicPkgAction);

			Attr attributeIsIncludedInTour = ownerDoc.createAttribute("isIncludedInTour");
			attributeIsIncludedInTour.setValue(insuranceComponent.getString("isIncludedInTour"));
			insuranceComponentElement.setAttributeNode(attributeIsIncludedInTour);

			// Setting insCoverageDetail Element
			JSONObject insCoverageDetail = insuranceComponent.getJSONObject("insCoverageDetail");

			if (insCoverageDetail != null && insCoverageDetail.length() != 0) {
				Element insCoverageDetailElement = ownerDoc.createElementNS(NS_PAC, "tns:InsCoverageDetail");

				Attr attributeName = ownerDoc.createAttribute("Name");
				attributeName.setValue(insCoverageDetail.getString("name"));
				insCoverageDetailElement.setAttributeNode(attributeName);

				Attr attributeDesc = ownerDoc.createAttribute("Description");
				attributeDesc.setValue(insCoverageDetail.getString("description"));
				insCoverageDetailElement.setAttributeNode(attributeDesc);

				// Appending InsCoverageDetail Element to Insurance Element
				insuranceComponentElement.appendChild(insCoverageDetailElement);
			}

			// Setting planCost Element
			JSONArray planCosts = insuranceComponent.getJSONArray("planCost");

			if (planCosts != null && planCosts.length() != 0) {
				for (int j = 0; j < planCosts.length(); j++) {
					JSONObject planCost = planCosts.getJSONObject(j);

					Element planCostElement = ownerDoc.createElementNS(NS_PAC, "tns:PlanCost");

					Attr attributeCurrency = ownerDoc.createAttribute("CurrencyCode");
					attributeCurrency.setValue(planCost.getString("currencyCode"));
					planCostElement.setAttributeNode(attributeCurrency);

					Attr attributeAmount = ownerDoc.createAttribute("Amount");
					attributeAmount.setValue(Integer.toString(planCost.getInt("amount")));
					planCostElement.setAttributeNode(attributeAmount);

					// Setting Base Premium Element
					JSONObject basePremium = planCost.getJSONObject("basePremium");

					if (basePremium != null && basePremium.length() != 0) {
						Element basePremiumElement = ownerDoc.createElementNS(NS_OTA, "ns:BasePremium");

						Attr attributeBaseCurrency = ownerDoc.createAttribute("CurrencyCode");
						attributeBaseCurrency.setValue(basePremium.getString("currencyCode"));
						basePremiumElement.setAttributeNode(attributeBaseCurrency);

						Attr attributeBaseAmount = ownerDoc.createAttribute("Amount");
						attributeBaseAmount.setValue(Integer.toString(basePremium.getInt("amount")));
						basePremiumElement.setAttributeNode(attributeBaseAmount);

						// Appending Base Premium Element to Plan Cost Element
						planCostElement.appendChild(basePremiumElement);
					}

					// Setting Charge Element
					JSONArray charges = planCost.getJSONArray("charge");

					if (charges != null && charges.length() != 0) {
						Element chargesElement = ownerDoc.createElementNS(NS_OTA, "ns:Charges");

						for (int k = 0; k < charges.length(); k++) {
							JSONObject charge = charges.getJSONObject(k);

							Element chargeElement = ownerDoc.createElementNS(NS_OTA, "ns:Charge");

							Attr attributeType = ownerDoc.createAttribute("Type");
							attributeType.setValue(charge.getString("type"));
							chargeElement.setAttributeNode(attributeType);

							// Setting Taxes Element
							JSONObject taxes = charge.getJSONObject("taxes");
							if (taxes != null && taxes.length() != 0) {
								Element taxesElement = ownerDoc.createElementNS(NS_OTA, "ns:Taxes");

								Attr attributeTaxesCurrency = ownerDoc.createAttribute("CurrencyCode");
								attributeTaxesCurrency.setValue(taxes.getString("currencyCode"));
								taxesElement.setAttributeNode(attributeTaxesCurrency);

								Attr attributeTaxesAmount = ownerDoc.createAttribute("Amount");
								attributeTaxesAmount.setValue(Integer.toString(taxes.getInt("amount")));
								taxesElement.setAttributeNode(attributeTaxesAmount);

								// Setting Tax Element
								JSONArray tax = taxes.getJSONArray("tax");

								if (tax != null && tax.length() != 0) {
									for (int y = 0; y < tax.length(); y++) {
										JSONObject taxx = tax.getJSONObject(y);

										Element taxElement = ownerDoc.createElementNS(NS_OTA, "ns:Tax");

										Attr attributeTaxCurrency = ownerDoc.createAttribute("CurrencyCode");
										attributeTaxCurrency.setValue(taxx.getString("currencyCode"));
										taxElement.setAttributeNode(attributeTaxCurrency);

										Attr attributeTaxAmount = ownerDoc.createAttribute("Amount");
										attributeTaxAmount.setValue(Integer.toString(taxx.getInt("amount")));
										taxElement.setAttributeNode(attributeTaxAmount);

										Attr attributeDP = ownerDoc.createAttribute("DecimalPlaces");
										attributeDP.setValue(Integer.toString(taxx.getInt("decimalPlaces")));
										taxElement.setAttributeNode(attributeDP);

										// Setting Description Element
										Element descElement = ownerDoc.createElementNS(NS_OTA, "ns:TaxDescription");

										Attr attributeName = ownerDoc.createAttribute("Name");
										attributeName.setValue(taxx.getString("taxName"));
										descElement.setAttributeNode(attributeName);

										// Appending Tax Description to tax element
										taxElement.appendChild(descElement);

										// Appending tax element to taxes ELement
										taxesElement.appendChild(taxElement);
									}
								}

								// Appending Taxes element to charge element
								chargeElement.appendChild(taxesElement);
							}

							// Appending Charge Element to Charges Element
							chargesElement.appendChild(chargeElement);
						}

						// Appending Charges Element to Plan Cost Element
						planCostElement.appendChild(chargesElement);
					}

					// Appending Plan Cost Elements to Insurance Element
					insuranceComponentElement.appendChild(planCostElement);
				}
			}

			// Appending Insurance Component Elements to Insurance Components Element
			insuranceComponentsElement.appendChild(insuranceComponentElement);
		}

		// Appending Insurance Components Element to TPA Element
		tpaElement.appendChild(insuranceComponentsElement);

		return tpaElement;
	}

	private static JSONObject getGlobalInfo(Element dynamicPackageElem) {
		// For GlobalInfo start
		Element globalInfoElem = XMLUtils.getFirstElementAtXPath(dynamicPackageElem, "./ns:GlobalInfo");
		JSONObject globalInfoJson = getGlobalInfoComments(globalInfoElem);

		JSONArray depositPaymentsJson = getGlobalInfoDepositPayments(globalInfoElem);
		globalInfoJson.put("depositPayments", depositPaymentsJson);

		JSONObject totalObj = getGlobalInfoTotal(globalInfoElem);
		globalInfoJson.put("total", totalObj);

		// BookingRules
		Element[] bookingRulesElems = XMLUtils.getElementsAtXPath(globalInfoElem, "./ns:BookingRules/ns:BookingRule");
		JSONArray bookingRulesArray = new JSONArray();

		for (Element bookingRulesElem : bookingRulesElems) {
			JSONObject bookingRuleJson = getGlobalInfoBookingRules(bookingRulesElem);
			bookingRulesArray.put(bookingRuleJson);
		}
		globalInfoJson.put(JSON_PROP_BOOKINGRULE, bookingRulesArray);

		JSONObject totalCommissionsjson = getGlobalInfoTotalCommissions(globalInfoElem);
		globalInfoJson.put("totalCommissions", totalCommissionsjson);

		JSONObject dynamicPkgIDjson = getGlobalInfoDynamicPkgID(globalInfoElem);
		globalInfoJson.put("dynamicPkgID", dynamicPkgIDjson);

		return globalInfoJson;
	}

	private static JSONObject getGlobalInfoDynamicPkgID(Element globalInfoElem) {
		// JSONObject dynamicPkgIDJson = new JSONObject();
		JSONObject companyShortNameJSON = new JSONObject();
		JSONObject dynamicPkgIDAttributejson = new JSONObject();
		Element dynamicPkgIDElement = XMLUtils.getFirstElementAtXPath(globalInfoElem,
				"./ns:DynamicPkgIDs/ns:DynamicPkgID");
		String id_context = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElement, "./@ID_Context"));
		String id = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElement, "./@ID"));

		dynamicPkgIDAttributejson.put("id_context", id_context);
		dynamicPkgIDAttributejson.put("id", id);

		Element companyNameElement = XMLUtils.getFirstElementAtXPath(dynamicPkgIDElement, "./ns:CompanyName");
		String companyShortName = String.valueOf(XMLUtils.getValueAtXPath(companyNameElement, "./@CompanyShortName"));

		dynamicPkgIDAttributejson.put("companyName", companyShortName);
		// dynamicPkgIDJson.put("DynamicPkgID", dynamicPkgIDAttributejson);

		return dynamicPkgIDAttributejson;
	}

	// To access TotalCommissions tag values start
	private static JSONObject getGlobalInfoTotalCommissions(Element globalInfoElem) {
		JSONObject commissionCurrencyAmountAttributeJson = new JSONObject();

		Element globalInfoBookingRuleDescriptionElement = XMLUtils.getFirstElementAtXPath(globalInfoElem,
				"./ns:TotalCommissions/ns:CommissionPayableAmount");
		String currencyCode = String
				.valueOf(XMLUtils.getValueAtXPath(globalInfoBookingRuleDescriptionElement, "./@CurrencyCode"));
		String amount = String.valueOf(XMLUtils.getValueAtXPath(globalInfoBookingRuleDescriptionElement, "./@Amount"));
		commissionCurrencyAmountAttributeJson.put("currencyCode", currencyCode);
		commissionCurrencyAmountAttributeJson.put("amount", amount);

		return commissionCurrencyAmountAttributeJson;
	}
	// To access TotalCommissions tag values end

	// To access BookingRules tag values start
	private static JSONObject getGlobalInfoBookingRules(Element bookingRulesElem) {
		// JSONObject bookingRuleDescriptionJson = new JSONObject();
		JSONObject bookingRuleDescriptionAttributejson = new JSONObject();

		Element globalInfoBookingRuleDescriptionElement = XMLUtils.getFirstElementAtXPath(bookingRulesElem,
				"./ns:Description");
		String name = String.valueOf(XMLUtils.getValueAtXPath(globalInfoBookingRuleDescriptionElement, "./@Name"));
		bookingRuleDescriptionAttributejson.put("name", name);

		String text = String.valueOf(XMLUtils.getValueAtXPath(globalInfoBookingRuleDescriptionElement, "./ns:Text"));
		bookingRuleDescriptionAttributejson.put("text", text);

		// bookingRuleDescriptionJson.put("bookingRule", descriptionJson);
		return bookingRuleDescriptionAttributejson;
	}
	// To access BookingRules tag values end

	// To access <globalInfo> <total> start
	private static JSONObject getGlobalInfoTotal(Element globalInfoElem) {

		JSONObject totalObj = new JSONObject();
		Element totalElement = XMLUtils.getFirstElementAtXPath(globalInfoElem, "./ns:Total");

		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(totalElement, "./@CurrencyCode"));
		String amountAfterTax = String.valueOf(XMLUtils.getValueAtXPath(totalElement, "./@AmountAfterTax"));

		totalObj.put("currencyCode", currencyCode);
		totalObj.put("amountAfterTax", amountAfterTax);

		return totalObj;
	}
	// To access <globalInfo> <total> end

	private static JSONArray getGlobalInfoDepositPayments(Element globalInfoElem) {
		JSONObject depositPayementJson = new JSONObject();

		Element[] guaranteePaymentElements = XMLUtils.getElementsAtXPath(globalInfoElem,
				"./ns:DepositPayments/ns:GuaranteePayment");
		JSONArray guaranteePaymentArray = new JSONArray();
		for (Element guaranteePaymentElement : guaranteePaymentElements) {
			JSONObject guaranteePaymentJson = new JSONObject();

			Element globalInfoDepositPaymentsElement = XMLUtils.getFirstElementAtXPath(guaranteePaymentElement,
					"./ns:AmountPercent");
			String currencyCode = String
					.valueOf(XMLUtils.getValueAtXPath(globalInfoDepositPaymentsElement, "./@CurrencyCode"));
			String amount = String.valueOf(XMLUtils.getValueAtXPath(globalInfoDepositPaymentsElement, "./@Amount"));
			guaranteePaymentJson.put("currencyCode", currencyCode);
			guaranteePaymentJson.put("amount", amount);

			guaranteePaymentArray.put(guaranteePaymentJson);
		}

		return guaranteePaymentArray;
	}

	private static JSONObject getGlobalInfoComments(Element globalInfoElem) {

		JSONObject globalInfoCommentsjson = new JSONObject();
		JSONArray globalInfoJsonArray = new JSONArray();

		Element[] globalInfoCommentsCommentElement = XMLUtils.getElementsAtXPath(globalInfoElem,
				"./ns:Comments/ns:Comment");
		for (Element globalElement : globalInfoCommentsCommentElement) {
			JSONObject globalCommentsCommentJson = getGlobalComment(globalElement);
			globalInfoJsonArray.put(globalCommentsCommentJson);
		}

		globalInfoCommentsjson.put("comment", globalInfoJsonArray);

		return globalInfoCommentsjson;
	}

	// To access globalInfo comments comment values start
	private static JSONObject getGlobalComment(Element globalElement) {
		JSONObject globalInfoCommentsjson = new JSONObject();
		String name = String.valueOf(XMLUtils.getValueAtXPath(globalElement, "./@Name"));

		JSONArray textArray = new JSONArray();
		Element[] textElements = XMLUtils.getElementsAtXPath(globalElement, "./ns:Text");

		for (Element textElement : textElements) {

			String text = String.valueOf(XMLUtils.getElementValue(textElement));
			textArray.put(text);

		}
		// String text = String.valueOf(XMLUtils.getValueAtXPath(globalElement,
		// "./ns:Text"));
		globalInfoCommentsjson.put("name", name);
		globalInfoCommentsjson.put("text", textArray);
		// globalInfoCommentsjson.put("Comment", globalInfoCommentsjson);
		return globalInfoCommentsjson;
	}
	// To access globalInfo comments comment values end

	// For dynamic package start
	// For component start
	private static JSONObject getSupplierResponseDynamicPackageJSON(Element dynamicPackElem,
			JSONObject reqCurrentDynamicPkg) {
		JSONObject component = new JSONObject();

		// Fetching no. of Adt and Chd Passengers
		JSONArray paxInfoArray = reqCurrentDynamicPkg.getJSONArray("paxInfo");
		int adtCount = 0;
		int chdCount = 0;
		int infCount = 0;
		for (int i = 0; i < paxInfoArray.length(); i++) {
			String paxType = paxInfoArray.getJSONObject(i).getString("paxType");
			if (paxType.equals("ADT")) {
				adtCount = paxInfoArray.getJSONObject(i).getInt("quantity");
			} else if (paxType.equals("CHD")) {
				chdCount = paxInfoArray.getJSONObject(i).getInt("quantity");
			} else if (paxType.equals("INF")) {
				infCount = paxInfoArray.getJSONObject(i).getInt("quantity");
			}
		}

		// JSONObject componentJson = new JSONObject();

		// for PackageOptionComponent start
		Element packageOptionComponentElement = XMLUtils.getFirstElementAtXPath(dynamicPackElem,
				"./ns:Components/ns:PackageOptionComponent");

		Element packageOptionElement = XMLUtils.getFirstElementAtXPath(packageOptionComponentElement,
				"./ns:PackageOptions/ns:PackageOption");

		JSONArray componentArray = getPackageOptionComponentForPrice(packageOptionElement, adtCount, chdCount,
				infCount);

		// componentJson.put("price", priceJson);

		component.put("components", componentArray);
		// packageComponentJsonArr.put(component);
		return component;
	}
	// For component end
	// For dynamic package end

	// To access <n1:Price> start
	private static JSONArray getPackageOptionComponentForPrice(Element packageOptionElem, int adtCount, int chdCount,
			int infCount) {
		JSONArray packageComponentJsonArray = new JSONArray();
		JSONArray forAdt = new JSONArray();
		JSONArray forChd = new JSONArray();
		JSONArray forInf = new JSONArray();
		JSONObject adtJSON = new JSONObject();
		JSONObject chdJSON = new JSONObject();
		JSONObject infJSON = new JSONObject();
		adtJSON.put("paxType", "ADT");
		chdJSON.put("paxType", "CHD");
		infJSON.put("paxType", "INF");
		JSONObject jsonForBase;

		Element[] packageOptionComponentPriceArray = XMLUtils.getElementsAtXPath(packageOptionElem, "./ns:Price");
		for (Element priceElement : packageOptionComponentPriceArray) {

			String type = String.valueOf(XMLUtils.getValueAtXPath(priceElement, "./ns:Base/@Type"));
			jsonForBase = new JSONObject();
			if (adtCount > 0 && (type.equals("NA") || type.equals("34"))) {
				jsonForBase = getBaseAttributes(priceElement);
				forAdt.put(jsonForBase);
			}
			if (chdCount > 0 && (type.equals("NA") || type.equals("35"))) {
				jsonForBase = getBaseAttributes(priceElement);
				forChd.put(jsonForBase);
			}
			if (infCount > 0 && (type.equals("NA") || type.equals("36"))) {
				jsonForBase = getBaseAttributes(priceElement);
				forInf.put(jsonForBase);
			}
		}

		if (forAdt.length() != 0) {
			adtJSON.put("price", forAdt);
			packageComponentJsonArray.put(adtJSON);
		}
		if (forChd.length() != 0) {
			chdJSON.put("price", forChd);
			packageComponentJsonArray.put(chdJSON);
		}
		if (forInf.length() != 0) {
			infJSON.put("price", forInf);
			packageComponentJsonArray.put(infJSON);
		}
		// packageComponentPriceJson.put(jsonForBasePrice);

		return packageComponentJsonArray;
	}

	// To access <n1:Price><ns:Total> start
	private static JSONObject getTotalTagAttributesFromPriceTag(Element priceElement) {
		JSONObject totalAttributeJson = new JSONObject();
		Element totalTagAttributeElement = XMLUtils.getFirstElementAtXPath(priceElement, "./ns:Total");
		String type = String.valueOf(XMLUtils.getValueAtXPath(totalTagAttributeElement, "./@Type"));
		String amountBeforeTax = String
				.valueOf(XMLUtils.getValueAtXPath(totalTagAttributeElement, "./@AmountBeforeTax"));
		String amountAfterTax = String.valueOf(XMLUtils.getValueAtXPath(totalTagAttributeElement, "./@AmountAfterTax"));
		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(totalTagAttributeElement, "./@CurrencyCode"));
		// For Tax
		JSONObject TaxJson = getChargeTax(totalTagAttributeElement);
		// Tax Ends

		totalAttributeJson.put("type", type);
		totalAttributeJson.put("amountBeforeTax", amountBeforeTax);
		totalAttributeJson.put("amountAfterTax", amountAfterTax);
		totalAttributeJson.put("currencyCode", currencyCode);
		totalAttributeJson.put("taxes", TaxJson);
		return totalAttributeJson;
	}
	// To access <n1:Price><ns:Total> end

	// To access <n1:Price> end

	// For tax - Start
	private static JSONObject getChargeTax(Element chargeElem) {
		JSONObject taxes = new JSONObject();
		JSONArray taxArray = new JSONArray();

		Element taxesElement = XMLUtils.getFirstElementAtXPath(chargeElem, "./ns:Taxes");

		String amount2 = String.valueOf(XMLUtils.getValueAtXPath(taxesElement, "./@Amount"));
		String currencyCode2 = String.valueOf(XMLUtils.getValueAtXPath(taxesElement, "./@CurrencyCode"));

		Element[] taxElems = XMLUtils.getElementsAtXPath(chargeElem, "./ns:Taxes/ns:Tax");

		for (Element taxElem : taxElems) {
			JSONObject taxJson = new JSONObject();

			String amount1 = String.valueOf(XMLUtils.getValueAtXPath(taxElem, "./@Amount"));
			String currencyCode1 = String.valueOf(XMLUtils.getValueAtXPath(taxElem, "./@CurrencyCode"));

			taxJson.put("amount", amount1);
			taxJson.put("currencyCode", currencyCode1);

			Element taxDescriptionElem = XMLUtils.getFirstElementAtXPath(taxElem, "./ns:TaxDescription");

			String name = String.valueOf(XMLUtils.getValueAtXPath(taxDescriptionElem, "./@Name"));

			taxJson.put("taxDescription", name);

			taxArray.put(taxJson);
		}

		taxes.put("amount", amount2);
		taxes.put("currencyCode", currencyCode2);
		taxes.put("tax", taxArray);
		return taxes;
	}
	// Tax end

	// For base attributes start
	private static JSONObject getBaseAttributes(Element priceElement) {
		JSONObject jsonForBaseAttribute = new JSONObject();
		JSONObject jsonForBase = new JSONObject();
		String maxGuestApplicable = String.valueOf(XMLUtils.getValueAtXPath(priceElement, "./@MaxGuestApplicable"));
		Element baseAttributeElement = XMLUtils.getFirstElementAtXPath(priceElement, "./ns:Base");
		String type = String.valueOf(XMLUtils.getValueAtXPath(baseAttributeElement, "./@Type"));
		String amountBeforeTax = String.valueOf(XMLUtils.getValueAtXPath(baseAttributeElement, "./@AmountBeforeTax"));
		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(baseAttributeElement, "./@CurrencyCode"));

		// To add Total Tag start
		JSONObject jsonForTotalTagAttribute = getTotalTagAttributesFromPriceTag(priceElement);
		// Total end

		// To add RateDescription <n1:RateDescription>
		JSONObject jsonForRD = getRD(priceElement);
		// RD End
		jsonForBaseAttribute.put("type", type);
		jsonForBaseAttribute.put("amountBeforeTax", amountBeforeTax);
		jsonForBaseAttribute.put("currencyCode", currencyCode);
		jsonForBase.put("base", jsonForBaseAttribute);
		jsonForBase.put("maxGuestApplicable", maxGuestApplicable);
		jsonForBase.put("total", jsonForTotalTagAttribute);
		jsonForBase.put("rateDescription", jsonForRD);
		return jsonForBase;
	}
	// For base attributes end

	private static JSONObject getRD(Element priceElement) {
		JSONObject jsonForRD = new JSONObject();
		Element RateDescriptionElement = XMLUtils.getFirstElementAtXPath(priceElement, "./ns:RateDescription");
		String text = String.valueOf(XMLUtils.getValueAtXPath(RateDescriptionElement, "./ns:Text"));
		String name = String.valueOf(XMLUtils.getValueAtXPath(RateDescriptionElement, "./@Name"));
		jsonForRD.put("text", text);
		jsonForRD.put("name", name);

		return jsonForRD;
	}

	static String getRedisKeyForDynamicPackage(JSONObject dynamicPackageObj) {
		StringBuilder strBldr = new StringBuilder(dynamicPackageObj.optString(JSON_PROP_SUPPLIERID));

		strBldr.append('[');
		strBldr.append(dynamicPackageObj.getString("brandName").concat(dynamicPackageObj.getString("tourCode"))
				.concat(dynamicPackageObj.getString("subTourCode")));
		strBldr.append(']');

		return strBldr.toString();
	}

	private static void pushSuppFaresToRedisAndRemove(JSONObject resJson, JSONObject requestBody) {

		JSONObject responseHeaderJson = resJson.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject responseBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);

		JSONArray dynamicPackageArray = responseBodyJson.getJSONArray(JSON_PROP_DYNAMICPACKAGE);
		JSONArray bookReferencesArray = responseBodyJson.getJSONArray(JSON_PROP_BOOKREFERENCES);
		responseBodyJson.remove(JSON_PROP_BOOKREFERENCES);

		System.out.println("removed book references array");

		if (dynamicPackageArray == null || bookReferencesArray == null) {
			// TODO: This should never happen. Log a warning message here.
			System.out.println("never happen if condition");
			return;
		}

		Map<String, String> repriceSupplierPricesMap = new HashMap<String, String>();
		for (int i = 0; i < dynamicPackageArray.length(); i++) {

			JSONObject dynamicPackageObj = dynamicPackageArray.getJSONObject(i);
			JSONObject supplierPriceObj = dynamicPackageObj.optJSONObject("supplierInfo");
			dynamicPackageObj.remove("supplierInfo");

			String redisKey = getRedisKeyForDynamicPackage(dynamicPackageObj);

			System.out.println(redisKey);

			repriceSupplierPricesMap.put(redisKey, supplierPriceObj.toString());
		}

		System.out.println(repriceSupplierPricesMap.toString());

		String redisKey = responseHeaderJson.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT).concat("|")
				.concat("reprice");

		Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
		redisConn.hmset(redisKey, repriceSupplierPricesMap);
		redisConn.pexpire(redisKey, (long) (HolidaysConfig.getRedisTTLMinutes() * 60 * 1000));
		RedisConfig.releaseRedisConnectionToPool(redisConn);
	}

	public static JSONObject getSupplierDataToStoreInRedis(JSONObject dynamicPackageObj, Element dynamicPackageElem,
			JSONObject requestBody, JSONObject reqCurrentDynamicPkg) {
		
		JSONObject supplierInfo = new JSONObject();

		// For Total Fare of the Package
		JSONObject supplierPackageTotalPricingInfo = new JSONObject();

		JSONObject total = new JSONObject();

		total.put(JSON_PROP_AMOUNT,
				dynamicPackageObj.getJSONObject("globalInfo").getJSONObject("total").getString("amountAfterTax"));

		total.put(JSON_PROP_CURRENCYCODE,
				dynamicPackageObj.getJSONObject("globalInfo").getJSONObject("total").getString("currencyCode"));

		supplierPackageTotalPricingInfo.put(JSON_PROP_TOTAL, total);
		supplierInfo.put("supplierPackageTotalPricingInfo", supplierPackageTotalPricingInfo);

		// Fare of Each Component- to make perComponentPerPaxFares object
		JSONObject perComponentPerPaxFares = new JSONObject();
		JSONArray paxTypeFares = new JSONArray();

		JSONObject totalcomponentprices = new JSONObject();

		JSONArray prices = new JSONArray();

		JSONArray componentsArray = dynamicPackageObj.getJSONArray("components");

		for (int i = 0; i < componentsArray.length(); i++) {
			prices = componentsArray.getJSONObject(i).getJSONArray("price");
			JSONArray allComponentsJsonArray = new JSONArray();
			JSONArray supplierPriceJsonArray = new JSONArray();
			JSONArray taxJsonArray = new JSONArray();
			JSONArray surchargeJsonArray = new JSONArray();
			JSONArray insuranceJsonArray = new JSONArray();
			JSONArray transferComponentJsonArray = new JSONArray();
			JSONObject paxPriceObject = new JSONObject();
			String paxType = componentsArray.getJSONObject(i).getString("paxType");

			for (int j = 0; j < prices.length(); j++) {
				JSONObject price = prices.getJSONObject(j);

				JSONObject totalcomponentprice = price.getJSONObject("total");
				totalcomponentprice.remove("type");

				JSONObject rateDescription = new JSONObject();

				String text = price.getJSONObject("rateDescription").getString("text");

				if (text.toLowerCase().contains("room")) {
				  if(reqCurrentDynamicPkg.getJSONObject("components").getJSONArray("hotelComponent")!= null && reqCurrentDynamicPkg.getJSONObject("components").getJSONArray("hotelComponent").length() > 0)
                  {
                    rateDescription.put(JSON_PROP_DYNAMICPKGACTION, DYNAMICPKGACTION_HOTEL_TOUR);
                  }
                  else if(reqCurrentDynamicPkg.getJSONObject("components").getJSONArray("cruiseComponent")!= null && reqCurrentDynamicPkg.getJSONObject("components").getJSONArray("cruiseComponent").length() > 0)
                  {
                    rateDescription.put(JSON_PROP_DYNAMICPKGACTION, DYNAMICPKGACTION_CRUISE_TOUR);
                  }
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					supplierPriceJson.put("supplierPrice", totalcomponentprice);
					supplierPriceJson.put("roomType", text);
					allComponentsJsonArray.put(supplierPriceJson);
					totalcomponentprices.put("AccommodationComponent", allComponentsJsonArray);
				} else if (text.toLowerCase().contains("post") && text.toLowerCase().contains("night")) {
					rateDescription.put(JSON_PROP_DYNAMICPKGACTION, JSON_PROP_POSTNIGHT);
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					supplierPriceJson.put("supplierPrice", totalcomponentprice);
					supplierPriceJson.put("roomType", text);
					allComponentsJsonArray.put(supplierPriceJson);
					totalcomponentprices.put("PostNight", allComponentsJsonArray);
				} else if (text.toLowerCase().contains("pre") && text.toLowerCase().contains("night")) {
					rateDescription.put(JSON_PROP_DYNAMICPKGACTION,JSON_PROP_PRENIGHT);
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					supplierPriceJson.put("supplierPrice", totalcomponentprice);
					supplierPriceJson.put("roomType", text);
					allComponentsJsonArray.put(supplierPriceJson);
					totalcomponentprices.put("PreNight", allComponentsJsonArray);
				} else if (text.toLowerCase().contains("protection") || text.toLowerCase().contains("insurance")) {
					rateDescription.put(JSON_PROP_DYNAMICPKGACTION, DYNAMICPKGACTION_INSURANCE);
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					insuranceJsonArray.put(totalcomponentprice);
					supplierPriceJson.put("supplierPrice", insuranceJsonArray);
					totalcomponentprices.put("PackageInsurance", supplierPriceJson);
				} else if (text.toLowerCase().contains("taxes") && text.toLowerCase().contains("charges")) {
					rateDescription.put(JSON_PROP_DYNAMICPKGACTION, "Taxes");
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					taxJsonArray.put(totalcomponentprice);
					supplierPriceJson.put("supplierPrice", taxJsonArray);
					totalcomponentprices.put("Taxes", supplierPriceJson);
				} else if (text.toLowerCase().contains("surcharges")) {
					rateDescription.put(JSON_PROP_DYNAMICPKGACTION, "Surcharge");
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					surchargeJsonArray.put(totalcomponentprice);
					supplierPriceJson.put("supplierPrice", surchargeJsonArray);
					totalcomponentprices.put("Surcharge", supplierPriceJson);
				} else if ((text.toLowerCase().contains("departure") && text.toLowerCase().contains("transfer"))
						|| (text.toLowerCase().contains("arrival") && text.toLowerCase().contains("transfer"))) {
					if(text.toLowerCase().contains("departure") && text.toLowerCase().contains("transfer")) 
		        {
		          rateDescription.put(JSON_PROP_DYNAMICPKGACTION, DYNAMICPKGACTION_TRANSFER_DEPARTURE);
		          rateDescription.put("name", text);
		        }
		        else if(text.toLowerCase().contains("arrival") && text.toLowerCase().contains("transfer")) 
		        {
		          rateDescription.put(JSON_PROP_DYNAMICPKGACTION, DYNAMICPKGACTION_TRANSFER_IARRIVAL);
		          rateDescription.put("name", text);
		        }
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					transferComponentJsonArray.put(totalcomponentprice);
					supplierPriceJson.put("supplierPrice", transferComponentJsonArray);
					totalcomponentprices.put("PackageTransfers", supplierPriceJson);
				} else if (text.toLowerCase().contains("extras")) {
					rateDescription.put(JSON_PROP_DYNAMICPKGACTION, DYNAMICPKGACTION_EXTRAS);
					rateDescription.put("name", text);
					totalcomponentprice.put("rateDescription", rateDescription);
					JSONObject supplierPriceJson = new JSONObject();
					supplierPriceJsonArray.put(totalcomponentprice);
					supplierPriceJson.put("supplierPrice", supplierPriceJsonArray);
					totalcomponentprices.put("Extras", supplierPriceJson);
				} else {
					continue;
				}
			}
			// Adding total SuplierTotalComponentPrice component for extras start
			JSONObject totalJson = new JSONObject();
			JSONObject totalJsonForExtras = new JSONObject();
			JSONArray supplierPriceArray = totalcomponentprices.getJSONObject("Extras").getJSONArray("supplierPrice");
			BigDecimal totalAmountForExtras= new BigDecimal(0);
			//int totalAmountForExtras = 0;
			for (int k = 0; k < supplierPriceArray.length(); k++) {
				BigDecimal amount = supplierPriceArray.getJSONObject(k).optBigDecimal(JSON_PROP_AMOUNTAFTERTAX,
						new BigDecimal("0"));
				//int amount = Integer.parseInt(supplierPriceArray.getJSONObject(k).getString("amountAfterTax"));
				totalAmountForExtras = totalAmountForExtras.add(amount);
			}
			// System.out.println("Total of extras: "+totalAmountForExtras);
			totalJson.put("amount", totalAmountForExtras);
			totalJson.put("currencyCode", "USD");
			totalJsonForExtras.put("total", totalJson);
			totalcomponentprices.getJSONObject("Extras").put("suplierTotalComponentPrice", totalJsonForExtras);
			// end

			// Adding total SuplierTotalComponentPrice for tax component for extras start
			JSONObject totalForTaxesJson = new JSONObject();
			JSONObject totalJsonForTaxes = new JSONObject();
			JSONArray supplierPriceForTaxesArray = totalcomponentprices.getJSONObject("Taxes")
					.getJSONArray("supplierPrice");
			int totalAmountForTaxes = 0;
			for (int k = 0; k < supplierPriceForTaxesArray.length(); k++) {
				int amount = Integer.parseInt(supplierPriceForTaxesArray.getJSONObject(k).getString("amountAfterTax"));
				totalAmountForTaxes = totalAmountForTaxes + amount;
			}
			// System.out.println("Total of taxes: "+totalAmountForTaxes);
			totalForTaxesJson.put("amount", totalAmountForTaxes);
			totalForTaxesJson.put("currencyCode", "USD");
			totalJsonForTaxes.put("total", totalForTaxesJson);
			totalcomponentprices.getJSONObject("Taxes").put("suplierTotalComponentPrice", totalJsonForTaxes);
			// end

			// Adding total SuplierTotalComponentPrice for surcharge component for extras
			// start
			JSONObject totalForSurchargeJson = new JSONObject();
			JSONObject totalJsonForSurcharge = new JSONObject();
			JSONArray supplierPriceForSurchargeArray = totalcomponentprices.getJSONObject("Surcharge")
					.getJSONArray("supplierPrice");
			int totalAmountForSurcharge = 0;
			for (int k = 0; k < supplierPriceForSurchargeArray.length(); k++) {
				int amount = Integer
						.parseInt(supplierPriceForSurchargeArray.getJSONObject(k).getString("amountAfterTax"));
				totalAmountForSurcharge = totalAmountForSurcharge + amount;
			}
			// System.out.println("Total of Surcharge: "+totalAmountForSurcharge);
			totalForSurchargeJson.put("amount", totalAmountForSurcharge);
			totalForSurchargeJson.put("currencyCode", "USD");
			totalJsonForSurcharge.put("total", totalForSurchargeJson);
			totalcomponentprices.getJSONObject("Surcharge").put("suplierTotalComponentPrice", totalJsonForSurcharge);
			// end

			// Adding total SuplierTotalComponentPrice for insurance component for extras
			// start
			JSONObject totalForInsuranceJson = new JSONObject();
			JSONObject totalJsonForInsurance = new JSONObject();
			JSONArray supplierPriceForInsuranceArray = totalcomponentprices.getJSONObject("PackageInsurance")
					.getJSONArray("supplierPrice");
			int totalAmountForInsurance = 0;
			for (int k = 0; k < supplierPriceForInsuranceArray.length(); k++) {
				int amount = Integer
						.parseInt(supplierPriceForInsuranceArray.getJSONObject(k).getString("amountAfterTax"));
				totalAmountForInsurance = totalAmountForInsurance + amount;
			}
			// System.out.println("Total of Insurance: "+totalAmountForInsurance);
			totalForInsuranceJson.put("amount", totalAmountForInsurance);
			totalForInsuranceJson.put("currencyCode", "USD");
			totalJsonForInsurance.put("total", totalForInsuranceJson);
			totalcomponentprices.getJSONObject("PackageInsurance").put("suplierTotalComponentPrice",
					totalJsonForInsurance);
			// end

			// Adding total SuplierTotalComponentPrice for transfer component for extras
			// start
			JSONObject totalForTransferJson = new JSONObject();
			JSONObject totalJsonForTransfer = new JSONObject();
			JSONArray supplierPriceForTransferArray = totalcomponentprices.getJSONObject("PackageTransfers")
					.getJSONArray("supplierPrice");
			int totalAmountForTransfer = 0;
			for (int k = 0; k < supplierPriceForTransferArray.length(); k++) {
				int amount = Integer
						.parseInt(supplierPriceForTransferArray.getJSONObject(k).getString("amountAfterTax"));
				totalAmountForTransfer = totalAmountForTransfer + amount;
			}
			// System.out.println("Total of Transfer: "+totalAmountForTransfer);
			totalForTransferJson.put("amount", totalAmountForTransfer);
			totalForTransferJson.put("currencyCode", "USD");
			totalJsonForTransfer.put("total", totalForTransferJson);
			totalcomponentprices.getJSONObject("PackageTransfers").put("suplierTotalComponentPrice",
					totalJsonForTransfer);
			// end

			paxPriceObject.put("prices", totalcomponentprices);
			paxPriceObject.put("paxType", paxType);

			paxTypeFares.put(paxPriceObject);

		}
		perComponentPerPaxFares.put("paxTypeFares", paxTypeFares);

		supplierInfo.put("perComponentPerPaxFares", perComponentPerPaxFares);

		// Creating bookReferences Object and supplierBookingFare Object
		JSONObject bookReferencesJson = getBookReference(dynamicPackageElem);

		supplierInfo.put("bookReferences", bookReferencesJson);
		logger.info("new supplierInfo JSON", supplierInfo);

		return supplierInfo;
	}

	public static JSONObject getBookReference(Element dynamicPackageElem) {
		// Creating bookReferences Object and supplierBookingFare Object
		JSONObject bookReferencesJson = new JSONObject();
		JSONObject supplierBookingFareJson = new JSONObject();

		supplierBookingFareJson.put(JSON_PROP_AMOUNT, Utils.convertToBigDecimal(String
				.valueOf(XMLUtils.getValueAtXPath(dynamicPackageElem, "./ns:GlobalInfo/ns:Total/@AmountAfterTax")), 0));
		supplierBookingFareJson.put("currencyCode",
				XMLUtils.getValueAtXPath(dynamicPackageElem, "./ns:GlobalInfo/ns:Total/@CurrencyCode"));

		bookReferencesJson.put("supplierBookingFare", supplierBookingFareJson);

		String tourCode = String.valueOf(
				XMLUtils.getValueAtXPath(dynamicPackageElem, "./ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/@ID"));
		String subTourCode = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackageElem,
				"./ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@QuoteID"));
		String brandName = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackageElem,
				"./ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/ns:CompanyName/@CompanyShortName"));

		String supplierBookingId = brandName + tourCode + subTourCode;

		bookReferencesJson.put("supplierBookingId", supplierBookingId);

		return bookReferencesJson;
	}
	
	/*
	 * private static void calculatePrices_old(JSONObject requestJson, JSONObject
	 * resJson, JSONObject resSupplierCommJson, JSONObject resClientCommJson) {
	 * 
	 * JSONObject briJson, ccommPkgDtlsJson, ccommRoomDtlsJson,
	 * clientEntityCommJson, markupCalcJson, ccommPaxDtlsJson,
	 * applicableOnProductsJson; JSONArray ccommPkgDtlsJsonArr,
	 * ccommRoomDtlsJsonArr, clientEntityCommJsonArr, ccommPaxDtlsJsonArr,
	 * applicableOnProductsArr; BigDecimal amountAfterTaxSI,
	 * amountBeforeTaxSI,paxCount = new BigDecimal("0"); String rateDescriptionText;
	 * Map<String, Integer> paxQtyMap = new HashMap<String, Integer>(); Map<String,
	 * JSONObject> priceMap = new ConcurrentHashMap<String, JSONObject>();
	 * 
	 * // creating priceArray of unique elements to create Wem response JSONArray
	 * dynamicPkgArray = HolidaysUtil.createNewPriceArr(resJson,priceMap);
	 * 
	 * // retrieve passenger Qty from requestJson int paxQty =
	 * retrievePassengerQty(requestJson, paxQtyMap);
	 * 
	 * // ------populate values from client commercials to generate wem
	 * response(i.e. // replacing the values of SI json response)
	 * 
	 * JSONArray briArr =
	 * resClientCommJson.getJSONObject("result").getJSONObject("execution-results")
	 * .getJSONArray("results").getJSONObject(0).getJSONObject("value")
	 * .getJSONObject(
	 * "cnk.holidays_commercialscalculationengine.clienttransactionalrules.Root")
	 * .getJSONArray(JSON_PROP_BUSSRULEINTAKE); Map<String, Integer> suppIndexMap =
	 * new HashMap<String, Integer>(); String suppId = ""; for (int i = 0; i <
	 * briArr.length(); i++) {
	 * 
	 * briJson = (JSONObject) briArr.get(i); ccommPkgDtlsJsonArr =
	 * briJson.getJSONArray("packageDetails"); suppId =
	 * briJson.getJSONObject("commonElements").getString("supplier");
	 * 
	 * for (int a = 0; a < dynamicPkgArray.length(); a++) { JSONObject
	 * dynamicPackageJson = dynamicPkgArray.getJSONObject(a); String supplierIdSI =
	 * dynamicPackageJson.getString(JSON_PROP_SUPPLIERID);
	 * 
	 * if (!(suppId.equalsIgnoreCase(supplierIdSI))) { continue; } int idx =
	 * (suppIndexMap.containsKey(supplierIdSI)) ? (suppIndexMap.get(supplierIdSI) +
	 * 1) : 0; suppIndexMap.put(supplierIdSI, idx); ccommPkgDtlsJson =
	 * ccommPkgDtlsJsonArr.getJSONObject(idx);
	 * 
	 * JSONArray priceArray = dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS)
	 * .getJSONArray(JSON_PROP_PRICE);
	 * 
	 * for (int b = 0; b < priceArray.length(); b++) { JSONObject priceJson =
	 * priceArray.getJSONObject(b); JSONObject totalJson =
	 * priceJson.getJSONObject(JSON_PROP_TOTAL);
	 * 
	 * amountAfterTaxSI = totalJson.optBigDecimal(JSON_PROP_AMOUNTAFTERTAX, new
	 * BigDecimal("0")); amountBeforeTaxSI =
	 * totalJson.optBigDecimal(JSON_PROP_AMOUNTBEFORETAX, new BigDecimal("0"));
	 * rateDescriptionText =
	 * priceJson.getJSONObject(JSON_PROP_RATEDESC).getString(JSON_PROP_TEXT);
	 * 
	 * // for (int j = 0; j < ccommPkgDtlsJsonArr.length(); j++) { //
	 * ccommPkgDtlsJson = ccommPkgDtlsJsonArr.getJSONObject(j);
	 * 
	 * ccommRoomDtlsJsonArr = ccommPkgDtlsJson.getJSONArray(JSON_PROP_ROOMDETAILS);
	 * 
	 * // for applicableOnProducts ---start
	 * 
	 * applicableOnProductsArr =
	 * ccommPkgDtlsJson.getJSONArray("applicableOnProducts"); paxCount =
	 * BigDecimal.valueOf(paxQty); if (!(rateDescriptionText.contains("Room"))) {
	 * for (int x = 0; x < applicableOnProductsArr.length(); x++) {
	 * applicableOnProductsJson = applicableOnProductsArr.getJSONObject(x); String
	 * productName = applicableOnProductsJson.getString("productName");
	 * clientEntityCommJsonArr =
	 * applicableOnProductsJson.optJSONArray(JSON_PROP_ENTITYCOMMS); if
	 * (clientEntityCommJsonArr == null) { // TODO: Refine this warning message.
	 * Maybe log some context information also.
	 * logger.warn("Client commercials calculations not found"); continue; } // for
	 * multiple chain of entity take the latest commercials applied for (int l =
	 * (clientEntityCommJsonArr.length() - 1); l >= 0; l--) {
	 * 
	 * clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(l);
	 * markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS);
	 * if (markupCalcJson == null) { continue; } if
	 * (rateDescriptionText.contains("Night")) { String applicableOnStr =
	 * rateDescriptionText .substring(0, rateDescriptionText.indexOf("Night") + 5)
	 * .replaceAll("[^a-zA-Z]", "");
	 * 
	 * BigDecimal amountAfterTaxcc = applicableOnProductsJson
	 * .getBigDecimal(JSON_PROP_TOTALFARE); BigDecimal amountBeforeTaxcc =
	 * applicableOnProductsJson
	 * .getJSONObject(JSON_PROP_FAREBREAKUP).getBigDecimal(JSON_PROP_BASEFARE);
	 * 
	 * int amountAfterTaxvalue = (amountAfterTaxcc).compareTo(amountAfterTaxSI); int
	 * amountBeforeTaxvalue = amountBeforeTaxcc.compareTo(amountBeforeTaxSI);
	 * 
	 * if (applicableOnStr.equalsIgnoreCase(productName) && amountAfterTaxvalue == 0
	 * && amountBeforeTaxvalue == 0) {
	 * 
	 * populateFinalPrice(markupCalcJson, priceJson, paxCount);
	 * 
	 * priceMap.remove(rateDescriptionText); }
	 * 
	 * }
	 * 
	 * else if (rateDescriptionText.contains("Transfer") &&
	 * (productName.contains("Transfer"))) {
	 * 
	 * populateFinalPrice(markupCalcJson, priceJson, paxCount);
	 * priceMap.remove("Transfers"); }
	 * 
	 * else if ((rateDescriptionText.contains("Extra") ||
	 * rateDescriptionText.contains("Upgrade")) && (productName.contains("Extra")))
	 * {
	 * 
	 * populateFinalPrice(markupCalcJson, priceJson, paxCount);
	 * priceMap.remove(rateDescriptionText); }
	 * 
	 * else if ((rateDescriptionText.contains("Trip Protection") ||
	 * rateDescriptionText.contains("Insurance")) &&
	 * (productName.contains("Insurance"))) {
	 * 
	 * populateFinalPrice(markupCalcJson, priceJson, paxCount);
	 * priceMap.remove(rateDescriptionText);
	 * 
	 * } else if (rateDescriptionText.contains("Surcharge") &&
	 * (productName.contains("Surcharge"))) {
	 * 
	 * populateFinalPrice(markupCalcJson, priceJson, paxCount);
	 * priceMap.remove(rateDescriptionText);
	 * 
	 * }
	 * 
	 * }
	 * 
	 * } } // for applicableOnProducts ---end
	 * 
	 * // for roomDetails if (rateDescriptionText.contains("Room")) { for (int k =
	 * 0; k < ccommRoomDtlsJsonArr.length(); k++) { ccommRoomDtlsJson =
	 * ccommRoomDtlsJsonArr.getJSONObject(k);
	 * 
	 * String roomType = ccommRoomDtlsJson.getString(JSON_PROP_ROOMTYPE);
	 * 
	 * ccommPaxDtlsJsonArr =
	 * ccommRoomDtlsJson.getJSONArray(JSON_PROP_PASSENGERDETAILS);
	 * 
	 * for (int p = 0; p < ccommPaxDtlsJsonArr.length(); p++) {
	 * 
	 * ccommPaxDtlsJson = ccommPaxDtlsJsonArr.getJSONObject(p);
	 * 
	 * clientEntityCommJsonArr =
	 * ccommPaxDtlsJson.optJSONArray(JSON_PROP_ENTITYCOMMS); if
	 * (clientEntityCommJsonArr == null) { // TODO: Refine this warning message.
	 * Maybe log some context information also.
	 * logger.warn("Client commercials calculations not found"); continue; } // for
	 * multiple chain of entity take the latest commercials applied for (int l =
	 * (clientEntityCommJsonArr.length() - 1); l >= 0; l--) {
	 * 
	 * clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(l);
	 * markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS);
	 * if (markupCalcJson == null) {
	 * 
	 * continue; } BigDecimal amountAfterTaxcc =
	 * ccommPaxDtlsJson.getBigDecimal(JSON_PROP_TOTALFARE); BigDecimal
	 * amountBeforeTaxcc = ccommPaxDtlsJson.getJSONObject(JSON_PROP_FAREBREAKUP)
	 * .getBigDecimal(JSON_PROP_BASEFARE); int amountAfterTaxvalue =
	 * (amountAfterTaxcc).compareTo(amountAfterTaxSI); int amountBeforeTaxvalue =
	 * amountBeforeTaxcc.compareTo(amountBeforeTaxSI); if
	 * (rateDescriptionText.contains(roomType) && amountAfterTaxvalue == 0 &&
	 * amountBeforeTaxvalue == 0) { BigDecimal paxCountRoom =
	 * BigDecimal.valueOf(paxQtyMap.get(roomType.toUpperCase()));
	 * populateFinalPrice(markupCalcJson, priceJson, paxCountRoom);
	 * priceMap.remove(rateDescriptionText); }
	 * 
	 * }
	 * 
	 * }
	 * 
	 * } }
	 * 
	 * } //Added SI prices from priceArray for whom commercials are not applied to
	 * totalFare if (priceMap != null || !priceMap.isEmpty()) { for (Entry<String,
	 * JSONObject> entry : priceMap.entrySet()) { JSONObject priceMapJson =
	 * entry.getValue(); BigDecimal amountAfterTax =
	 * priceMapJson.getJSONObject("total").optBigDecimal(JSON_PROP_AMOUNTAFTERTAX,
	 * new BigDecimal("0")); totalFare =
	 * totalFare.add((amountAfterTax).multiply(paxCount));
	 * 
	 * }} // Setting the Final price for the package JSONObject totalJson =
	 * dynamicPackageJson.getJSONObject(JSON_PROP_GLOBALINFO)
	 * .getJSONObject(JSON_PROP_TOTAL);
	 * 
	 * totalJson.put(JSON_PROP_AMOUNTAFTERTAX, totalFare); }
	 * 
	 * } }
	 */
	
	/*private static void populateFinalPrice(JSONObject markupCalcJson, JSONObject priceJson, BigDecimal paxCount) {
	BigDecimal totalFareCC = markupCalcJson.optBigDecimal(JSON_PROP_TOTALFARE, new BigDecimal("0"));

	BigDecimal baseFareCC = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP).optBigDecimal(JSON_PROP_BASEFARE,
			new BigDecimal("0"));

	JSONArray taxArr = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP).getJSONArray(JSON_PROP_TAXDETAILS);
	JSONObject totalJson = priceJson.getJSONObject(JSON_PROP_TOTAL);
	JSONObject baseJson = priceJson.getJSONObject("base");

	JSONArray taxArraySI = totalJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);

	if (taxArraySI.length() > 0 && taxArr.length() > 0) {
		for (int t = 0; t < taxArr.length(); t++) {
			JSONObject taxJson = taxArr.getJSONObject(t);
			BigDecimal taxValue = taxJson.getBigDecimal(JSON_PROP_TAXVALUE);
			String taxName = taxJson.getString(JSON_PROP_TAXNAME);

			JSONObject taxJsonSI = taxArraySI.getJSONObject(t);
			taxJsonSI.put(JSON_PROP_AMOUNT, taxValue);
			taxJsonSI.put(JSON_PROP_TAXDESCRIPTION, taxName);
			// TODO : check whether we need to replace SI currency
			// code with
			// markup commercials currencycode
			// taxJsonSI.put("currencyCode", currencyCode);

		}
	}

	totalJson.put(JSON_PROP_AMOUNTAFTERTAX, totalFareCC);
	totalJson.put(JSON_PROP_AMOUNTBEFORETAX, baseFareCC);
	baseJson.put(JSON_PROP_AMOUNTBEFORETAX, baseFareCC);
	// TODO : check whether we need to replace SI currency code with
	// markup
	// commercials currencycode
	// totalJson.put("currencyCode", currencyCode);
	totalFare = totalFare.add((totalFareCC).multiply(paxCount));

}*/
}
