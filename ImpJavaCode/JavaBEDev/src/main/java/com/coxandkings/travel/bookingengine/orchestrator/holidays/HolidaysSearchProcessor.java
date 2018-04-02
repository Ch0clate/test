package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.userctx.ProductSupplier;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.RedisRoEData;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class HolidaysSearchProcessor implements HolidayConstants {
	private static final Logger logger = LogManager.getLogger(HolidaysSearchProcessor.class);
	private static BigDecimal totalFare = new BigDecimal("0");
	private static String currencyCode = "" ;
	private static Boolean assignFlag = false;
	
	public static String process(JSONObject reqJson) {
		JSONObject reqHdrJson = (reqJson.optJSONObject(JSON_PROP_REQHEADER) != null) ? reqJson.optJSONObject(JSON_PROP_REQHEADER) : new JSONObject();
	    	JSONObject reqBodyJson = (reqJson.optJSONObject(JSON_PROP_REQBODY) != null) ? reqJson.optJSONObject(JSON_PROP_REQBODY) : new JSONObject();
		try {
			
			String errorShortText = "";
			String errorType = "";
			String errorCode = "";
			String errorStatus = "";
			JSONArray errorJsonArray = new JSONArray();
			OperationConfig opConfig = HolidaysConfig.getOperationConfig("search");

			Element reqElem = (Element) opConfig.getRequestXMLShell().cloneNode(true);
			Document ownerDoc = reqElem.getOwnerDocument();
			TrackingContext.setTrackingContext(reqJson);

			String sessionID = reqHdrJson.getString(JSON_PROP_SESSIONID);
			String transactionID = reqHdrJson.getString(JSON_PROP_TRANSACTID);
			String userID = reqHdrJson.getString(JSON_PROP_USERID);

			//UserContext usrCtx = UserContext.getUserContextForSession(sessionID);
			//List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PRODUCT);
			 UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			 List<ProductSupplier> prodSuppliers = usrCtx.getSuppliersForProduct(PROD_CATEG_HOLIDAYS, PROD_CATEG_HOLIDAYS);
			 
			XMLUtils.setValueAtXPath(reqElem, "./pac:RequestHeader/com:SessionID", sessionID);
			XMLUtils.setValueAtXPath(reqElem, "./pac:RequestHeader/com:TransactionID", transactionID);
			XMLUtils.setValueAtXPath(reqElem, "./pac:RequestHeader/com:UserID", userID);

			Element suppCredsListElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac:RequestHeader/com:SupplierCredentialsList");

			int sequence = 1;
			for (ProductSupplier prodSupplier : prodSuppliers) {

				Element suppCredsElem = prodSupplier.toElement(ownerDoc);

				Element sequenceElem = ownerDoc.createElementNS(NS_COM, "com:Sequence");
				sequenceElem.setTextContent(Integer.toString(sequence++));
				suppCredsElem.appendChild(sequenceElem);

				suppCredsListElem.appendChild(suppCredsElem);
			}

			String refPointCode = reqBodyJson.get("brandName").toString();
			String optionRefCode = reqBodyJson.get("tourCode").toString();
			String quoteID = reqBodyJson.get("subTourCode").toString();
			String destinationLocation = reqBodyJson.getString("destinationLocation").toString();

			Element refPointElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/ns:OTA_DynamicPkgAvailRQ/ns:SearchCriteria/ns:PackageOptionSearch/ns:OptionSearchCriteria/ns:Criterion/ns:RefPoint");
			Element optionRefElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/ns:OTA_DynamicPkgAvailRQ/ns:SearchCriteria/ns:PackageOptionSearch/ns:OptionSearchCriteria/ns:Criterion/ns:OptionRef");

			Attr codeAttr = ownerDoc.createAttribute(JSON_PROP_CODE);
			codeAttr.setValue(refPointCode);
			refPointElem.setAttributeNode(codeAttr);

			Attr optionAttr = ownerDoc.createAttribute(JSON_PROP_CODE);
			optionAttr.setValue(optionRefCode);
			optionRefElem.setAttributeNode(optionAttr);

			Element packageOptionElem = XMLUtils.getFirstElementAtXPath(reqElem,
					"./pac1:RequestBody/ns:OTA_DynamicPkgAvailRQ/ns:DynamicPackage/ns:Components/ns:PackageOptionComponent");
			Attr quoteIdAttr = ownerDoc.createAttribute("QuoteID");
			quoteIdAttr.setValue(quoteID);
			packageOptionElem.setAttributeNode(quoteIdAttr);

			System.out.println(XMLTransformer.toString(reqElem));

			logger.info("Before opening HttpURLConnection");

			Element resElem = null;
			// logger.info(String.format("SI XML Request = %s",
			// XMLTransformer.toString(reqElem)));
			resElem = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(),
					HolidaysConfig.getHttpHeaders(), reqElem);
			if (resElem == null) {
				throw new Exception("Null response received from SI");
			}

			
			logger.info(String.format("SI XML Response = %s", XMLTransformer.toString(resElem)));
			System.out.println(XMLTransformer.toString(resElem));

			// Added code for converting SI XML response to SI JSON Res

			JSONObject resJson = new JSONObject();

			resJson.put(JSON_PROP_RESHEADER, reqHdrJson);

			JSONObject resBodyJson = new JSONObject();
			JSONArray dynamicPackageArray = new JSONArray();

			// JSONArray oTA_DynamicPkgAvailRSWrapperJsonArray = new JSONArray();
			Element[] oTA_wrapperElems = XMLUtils.getElementsAtXPath(resElem,
					"./pac1:ResponseBody/pac:OTA_DynamicPkgAvailRSWrapper");

			for (Element oTA_wrapperElem : oTA_wrapperElems) {

				String supplierIDStr = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:SupplierID"));
				String sequenceStr = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:Sequence"));

				Element[] dynamicPackageElem = XMLUtils.getElementsAtXPath(oTA_wrapperElem,
						"./ns:OTA_DynamicPkgAvailRS/ns:DynamicPackage");
				
				//Error Response from SI
				Element errorElem[] = XMLUtils.getElementsAtXPath(oTA_wrapperElem, "./ns:OTA_DynamicPkgAvailRS/ns:Errors/ns:Error");
				if(errorElem.length != 0) {
					for(Element error : errorElem) {
						JSONObject errorJSON = new JSONObject();
						errorShortText = String.valueOf(XMLUtils.getValueAtXPath(error, "./@ShortText"));
						errorJSON.put("errorShortText", errorShortText);
						errorType = String.valueOf(XMLUtils.getValueAtXPath(error, "./@Type"));
						errorJSON.put("errorType", errorType);
						errorCode = String.valueOf(XMLUtils.getValueAtXPath(error, "./@Code"));
						errorJSON.put("errorCode", errorCode);
						errorStatus = String.valueOf(XMLUtils.getValueAtXPath(error, "./@status"));
						errorJSON.put("errorStatus", errorStatus);
						errorJsonArray.put(errorJSON);
						logger.info(String.format("Recieved Error from SI. Error Details:" + errorJSON.toString()));
					}
					continue;
					
				}
				
				for (Element dynamicPackElem : dynamicPackageElem) {

					JSONObject dynamicPackJson = new JSONObject();

					dynamicPackJson = getSupplierResponseDynamicPackageJSON(dynamicPackElem);

					dynamicPackJson.put(JSON_PROP_SEQUENCE, sequenceStr);
					dynamicPackJson.put(JSON_PROP_SUPPLIERID, supplierIDStr);

					String tourCode = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem,
							"./ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/@ID"));
					String subTourCode = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem,
							"./ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@QuoteID"));
					String brandName = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem,
							"./ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/ns:CompanyName/@CompanyShortName"));

					dynamicPackJson.put("tourCode", tourCode);
					dynamicPackJson.put("subTourCode", subTourCode);
					dynamicPackJson.put("brandName", brandName);

					dynamicPackageArray.put(dynamicPackJson);

				}
			}
			
			resBodyJson.put(JSON_PROP_DYNAMICPACKAGE, dynamicPackageArray);
			// resBodyRootJson.put(JSON_PROP_RESBODY, resBodyJson);

			resJson.put(JSON_PROP_RESBODY, resBodyJson);
			
			if(dynamicPackageArray.length()!= 0) {
			logger.info(String.format("SI Transformed JSON Response = %s", resJson.toString()));

			System.out.println("SI Transformed JSON Response = " + resJson.toString());
			
			// Call BRMS Supplier and Client Commercials

			logger.info(String.format("Calling to Supplier Commercial"));
			
			JSONObject resSupplierCommJson = HolidaysSupplierCommercials.getSupplierCommercials(reqJson, resJson,
					opConfig.getOperationName());
			if (BRMS_STATUS_TYPE_FAILURE.equals(resSupplierCommJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Supplier Commercials calculation engine: %s", resSupplierCommJson.toString()));
            	return getEmptyResponse(reqHdrJson).toString();
			}

			logger.info(String.format("Supplier Commercial Response = %s", resSupplierCommJson.toString()));
			System.out.println("Supplier Commercial Response = " + resSupplierCommJson.toString());

			JSONObject resClientCommJson = HolidaysClientCommercials.getClientCommercials(resSupplierCommJson);
			if (BRMS_STATUS_TYPE_FAILURE.equals(resClientCommJson.getString(JSON_PROP_TYPE))) {
				logger.error(String.format("A failure response was received from Client Commercials calculation engine: %s", resClientCommJson.toString()));
            	return getEmptyResponse(reqHdrJson).toString();
			}
			
			logger.info(String.format("Client Commercial Response = %s", resClientCommJson.toString()));
			System.out.println("Client Commercial Response = " + resClientCommJson.toString());

			calculatePrices(reqJson, resJson, resSupplierCommJson, resClientCommJson);
			
			  // Apply company offers
            //HolidaysCompanyOffers.getCompanyOffers(reqJson, resJson, OffersConfig.Type.COMPANY_SEARCH_TIME);

			}
			
			return resJson.toString();
			// return XMLTransformer.toString(resElem);
			

		}

		catch (Exception x) {
			logger.error("Exception received while processing", x);
            return getEmptyResponse(reqHdrJson).toString();
		}

	}
	public static JSONObject getEmptyResponse(JSONObject reqHdrJson) {
        JSONObject resJson = new JSONObject();
        resJson.put(JSON_PROP_RESHEADER, reqHdrJson);
        resJson.put(JSON_PROP_RESBODY, new JSONObject());
        return resJson;
    }
	public static void calculatePrices(JSONObject reqJson, JSONObject resJson, JSONObject resSupplierCommJson,
			JSONObject resClientCommJson) {

		JSONObject briJson, ccommPkgDtlsJson, ccommRoomDtlsJson, clientEntityCommJson, markupCalcJson, ccommPaxDtlsJson;
		JSONArray ccommPkgDtlsJsonArr, ccommRoomDtlsJsonArr, clientEntityCommJsonArr, ccommPaxDtlsJsonArr,supplierCommercialDetailsArr;
        
		Map<String,String> scommToTypeMap= getSupplierCommercialsAndTheirType(resSupplierCommJson);
		Map<String, String> commToTypeMap = getClientCommercialsAndTheirType(resClientCommJson);
		
		
		String clientMarket = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
        String clientCcyCode = reqJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
		
        JSONArray briArr = resClientCommJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject(JSON_PROP_VALUE).getJSONObject(JSON_PROP_CLIENTTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);

		String suppId = "";
		
		BigDecimal totalTaxPrice = new BigDecimal(0);
		Map<String, Integer> suppIndexMap = new HashMap<String, Integer>();

		for (int i = 0; i < briArr.length(); i++) {

			briJson = (JSONObject) briArr.get(i);
			ccommPkgDtlsJsonArr = briJson.getJSONArray(JSON_PROP_PACKAGEDETAILS);
			suppId = briJson.getJSONObject(JSON_PROP_COMMONELEMENTS).getString(JSON_PROP_SUPP);
			// getting roomstay from SI response
			JSONArray dynamicPkgArray = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_DYNAMICPACKAGE);

			for (int a = 0; a < dynamicPkgArray.length(); a++) {
				JSONObject dynamicPackageJson = dynamicPkgArray.getJSONObject(a);
				totalFare = new BigDecimal("0");
				assignFlag = false;
				String supplierIdSI = dynamicPackageJson.getString(JSON_PROP_SUPPLIERID);
				if (!(suppId.equalsIgnoreCase(supplierIdSI))) {
					continue;
				}

				int idx = (suppIndexMap.containsKey(supplierIdSI)) ? (suppIndexMap.get(supplierIdSI) + 1) : 0;
				suppIndexMap.put(supplierIdSI, idx);
				ccommPkgDtlsJson = ccommPkgDtlsJsonArr.getJSONObject(idx);
				
				ccommRoomDtlsJsonArr = ccommPkgDtlsJson.getJSONArray(JSON_PROP_ROOMDETAILS);

				for (int k = 0; k < ccommRoomDtlsJsonArr.length(); k++) {
					
					ccommRoomDtlsJson = ccommRoomDtlsJsonArr.getJSONObject(k);

					String roomType = ccommRoomDtlsJson.getString(JSON_PROP_ROOMTYPE);

					ccommPaxDtlsJsonArr = ccommRoomDtlsJson.getJSONArray(JSON_PROP_PASSENGERDETAILS);

					for (int p = 0; p < ccommPaxDtlsJsonArr.length(); p++) {

						ccommPaxDtlsJson = ccommPaxDtlsJsonArr.getJSONObject(p);
						BigDecimal totalPrice = new BigDecimal(0);
						BigDecimal amountAfterTaxcc = ccommPaxDtlsJson.getBigDecimal(JSON_PROP_TOTALFARE);
						BigDecimal amountBeforeTaxcc = ccommPaxDtlsJson.getJSONObject(JSON_PROP_FAREBREAKUP)
								.getBigDecimal(JSON_PROP_BASEFARE);
						clientEntityCommJsonArr = ccommPaxDtlsJson.optJSONArray(JSON_PROP_ENTITYCOMMS);
						if (clientEntityCommJsonArr == null) {
							// TODO: Refine this warning message. Maybe log some context information also.
							logger.warn("Client commercials calculations not found");
							continue;
						}
						 else//This is to capture the comm type field from commercial head in entity details
		                    {
		                    	int len=clientEntityCommJsonArr.length();
		                    	for (int x=0; x < len; x++) {
		    	    				JSONObject ccommClientCommJson = clientEntityCommJsonArr.getJSONObject(x);
		    	    				ccommClientCommJson.put(JSON_PROP_COMMTYPE, commToTypeMap.get(ccommClientCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS).getString(JSON_PROP_COMMNAME)));
		    	    			}
		                    	
		                    }
						 supplierCommercialDetailsArr = ccommPaxDtlsJson.optJSONArray(JSON_PROP_COMMDETAILS);

		                    if (supplierCommercialDetailsArr == null) {
		        				logger.warn(String.format("No supplier commercials found for supplier %s", suppId));
		        			} 
		                    
		                    else//This is to capture the comm type field from commercial head in suppCommercialRes
		                    {
		                    	int len=supplierCommercialDetailsArr.length();
		                    	for (int x=0; x < len; x++) {
		    	    				JSONObject scommClientCommJson = supplierCommercialDetailsArr.getJSONObject(x);
		    	    				scommClientCommJson.put(JSON_PROP_COMMTYPE, scommToTypeMap.get(scommClientCommJson.getString(JSON_PROP_COMMNAME)));
		    	    			}
		                    	
						// for multiple chain of entity take the latest commercials applied
						for (int l = (clientEntityCommJsonArr.length() - 1); l >= 0; l--) {

							clientEntityCommJson = clientEntityCommJsonArr.getJSONObject(l);
							
							// TODO: In case of B2B, do we need to add additional receivable commercials for all client hierarchy levels?  
		    				JSONArray additionalCommsJsonArr = clientEntityCommJson.optJSONArray(JSON_PROP_ADDCOMMDETAILS);//take the array additionalcommercialDetails
		    				if (additionalCommsJsonArr != null) {
		    					for (int x=0; x < additionalCommsJsonArr.length(); x++) {
		    						JSONObject additionalCommsJson = additionalCommsJsonArr.getJSONObject(x);//take object of additionalcommercialDetails array one by one
		    						String additionalCommName = additionalCommsJson.optString(JSON_PROP_COMMNAME);//fetch comm	Name from additionalcommercialDetails object
		    						if (COMM_TYPE_RECEIVABLE.equals(commToTypeMap.get(additionalCommName))) {//is the additionalCommName receivable?
		    							String additionalCommCcy = additionalCommsJson.getString(JSON_PROP_COMMCCY);//get comm currency from additionalcommercialDetails Object
		    							BigDecimal additionalCommAmt = additionalCommsJson.getBigDecimal(JSON_PROP_COMMAMOUNT).multiply(RedisRoEData.getRateOfExchange(additionalCommCcy, clientCcyCode, clientMarket));
		    							totalPrice=totalPrice.add(additionalCommAmt);
		    							
		    						}
		    					}
		    				}
							markupCalcJson = clientEntityCommJson.optJSONObject(JSON_PROP_MARKUPCOMDTLS);
							if (markupCalcJson == null) {
								continue;
							}
							

							if (dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_HOTEL_COMPONENT)) {
								//JSONArray hotelComponentArray = dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS)
								//		.getJSONArray(JSON_PROP_HOTEL_COMPONENT);
								
								JSONObject hotelCompJson = dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS)
										.getJSONObject(JSON_PROP_HOTEL_COMPONENT);
								if (hotelCompJson!=null && hotelCompJson.length()>0) {
								calculatePriceHotel(markupCalcJson, roomType, amountAfterTaxcc, amountBeforeTaxcc,
										hotelCompJson,totalPrice);

							}
						}

							if (dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_CRUISE_COMPONENT)) {
								//JSONArray cruiseComponentArray = dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS)
								//		.getJSONArray(JSON_PROP_CRUISE_COMPONENT);
								
								JSONObject cruiseComponentJson = dynamicPackageJson.getJSONObject(JSON_PROP_COMPONENTS)
										.getJSONObject(JSON_PROP_CRUISE_COMPONENT);
								if (cruiseComponentJson!=null && cruiseComponentJson.length()>0) {
									calculatePriceCruise(markupCalcJson, roomType, amountAfterTaxcc, amountBeforeTaxcc,
										cruiseComponentJson,totalPrice);

								}
							} 	
						}
		            }
				}
			}
				//setting the totalFare in GlobalInfo
				
				JSONObject totalJson = new JSONObject();
				totalJson.put(JSON_PROP_AMOUNTAFTERTAX, totalFare);
				totalJson.put(JSON_PROP_CURRENCYCODE, currencyCode);
				dynamicPackageJson.getJSONObject(JSON_PROP_GLOBALINFO).put(JSON_PROP_TOTAL,totalJson);
				
				
		 }
	   }
	}
	
	public static Map<String, String> getSupplierCommercialsAndTheirType(JSONObject suppCommResJson) {
		  JSONObject scommBRIJson,commHeadJson;
		  JSONArray commHeadJsonArr = null;
		 Map<String, String> suppCommToTypeMap = new HashMap<String, String>();
		   JSONArray scommBRIJsonArr = getSupplierCommercialsBusinessRuleIntakeJSONArray(suppCommResJson);
		   for (int i=0; i < scommBRIJsonArr.length(); i++) {
	        	scommBRIJson = scommBRIJsonArr.getJSONObject(i);
	        	commHeadJsonArr = scommBRIJson.optJSONArray(JSON_PROP_COMMHEAD);
	        	if (commHeadJsonArr == null) {
	        		logger.warn("No commercial heads found in supplier commercials");
	        		continue;
	        	}
	        	
	        	for (int j=0; j < commHeadJsonArr.length(); j++) {
	        		commHeadJson = commHeadJsonArr.getJSONObject(j);
	        		suppCommToTypeMap.put(commHeadJson.getString(JSON_PROP_COMMHEADNAME), commHeadJson.getString(JSON_PROP_COMMTYPE));
	        	}
	        }
	        
	        return suppCommToTypeMap;
	
	}

	private static JSONArray getSupplierCommercialsBusinessRuleIntakeJSONArray(JSONObject suppCommResJson) {
		return suppCommResJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject(JSON_PROP_VALUE).getJSONObject(JSON_PROP_SUPPTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);
		   
		
	}
	public static Map<String, String> getClientCommercialsAndTheirType(JSONObject clientCommResJson) {
		 JSONArray commHeadJsonArr = null;
		 JSONArray entDetaiJsonArray= null;
	        JSONObject commHeadJson = null;
	        JSONObject scommBRIJson = null;
	        Map<String, String> commToTypeMap = new HashMap<String, String>();
	        JSONArray scommBRIJsonArr = getClientCommercialsBusinessRuleIntakeJSONArray(clientCommResJson);
	        
	        for (int i=0; i < scommBRIJsonArr.length(); i++) {
	        	scommBRIJson = scommBRIJsonArr.getJSONObject(i);
	         entDetaiJsonArray = scommBRIJson.getJSONArray(JSON_PROP_ENTITYDETAILS);
	        	for(int j=0;j<entDetaiJsonArray.length();j++)
	        	{
	        		commHeadJsonArr=entDetaiJsonArray.getJSONObject(j).getJSONArray(JSON_PROP_COMMHEAD);
	        	if (commHeadJsonArr == null) {
	        		logger.warn("No commercial heads found in supplier commercials");
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

	private static JSONArray getClientCommercialsBusinessRuleIntakeJSONArray(JSONObject clientCommResJson) {
		
		return clientCommResJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject("value").getJSONObject(JSON_PROP_CLIENTTRANRULES).getJSONArray(JSON_PROP_BUSSRULEINTAKE);
	}

	

	public static void calculatePriceHotel(JSONObject markupCalcJson, String roomType, BigDecimal amountAfterTaxcc,
			BigDecimal amountBeforeTaxcc, JSONObject hotelCompJson,BigDecimal totalPrice) {
		//for (int b = 0; b < hotelCompJson.length(); b++) {
		//	JSONObject hotelComponentJson = hotelCompJson.getJSONObject(b);
		
			JSONArray roomStayArray = hotelCompJson.getJSONArray(JSON_PROP_ROOMSTAY);
			for (int c = 0; c < roomStayArray.length(); c++) {
				JSONObject roomStayJson = roomStayArray.getJSONObject(c);

				String roomTypeSI = roomStayJson.getString(JSON_PROP_ROOMTYPE);
				if (roomTypeSI.equalsIgnoreCase(roomType)) {

					BigDecimal amountAfterTax = markupCalcJson.getBigDecimal(JSON_PROP_TOTALFARE);
					BigDecimal amountBeforeTax = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP)
							.getBigDecimal(JSON_PROP_BASEFARE);
					String currencyCodeCC = markupCalcJson.getString(JSON_PROP_COMMCURRENCY);
					//totalPrice = totalPrice.add(amountAfterTax);
					JSONArray taxArr = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP)
							.getJSONArray(JSON_PROP_TAXDETAILS);

					JSONArray roomRateArr = roomStayJson.getJSONArray(JSON_PROP_ROOMRATE);

					for (int r = 0; r < roomRateArr.length(); r++) {

						JSONObject totalJson = roomRateArr.getJSONObject(r).getJSONObject(JSON_PROP_TOTAL);
						BigDecimal amountAfterTaxSI;
						BigDecimal amountBeforeTaxSI;
						String ratePlanCategory = roomRateArr.getJSONObject(r).getString("ratePlanCategory");
						currencyCode =totalJson.optString("currencyCode");
						
						amountAfterTaxSI = totalJson.optBigDecimal("amountAfterTax", new BigDecimal("0"));
						amountBeforeTaxSI = totalJson.optBigDecimal("amountBeforeTax", new BigDecimal("0"));

						int amountAfterTaxvalue = (amountAfterTaxcc).compareTo(amountAfterTaxSI);
						int amountBeforeTaxvalue = amountBeforeTaxcc.compareTo(amountBeforeTaxSI);
						if (roomTypeSI.equalsIgnoreCase(roomType) && amountAfterTaxvalue == 0
								&& amountBeforeTaxvalue == 0) {
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
							
							String type = totalJson.getString("type");
							if (type.equals("34")){
								totalJson.put(JSON_PROP_TYPE, "ADT");
							}
							if (type.equals("35")) {
								totalJson.put(JSON_PROP_TYPE, "CHD");
							}
							if (type.equals("36")) {
								totalJson.put(JSON_PROP_TYPE, "INF");
							}
							
							BigDecimal componentPrice = totalPrice.add(amountAfterTax);
							totalJson.put(JSON_PROP_AMOUNTAFTERTAX, componentPrice);
							totalJson.put(JSON_PROP_AMOUNTBEFORETAX, amountBeforeTax);
							// TODO : check whether we need to replace SI currency code with
							// markup
							// commercials currencycode
							// totalJson.put("currencyCode", currencyCodeCC);
							
							//Below code to set Total package fare
							if(roomTypeSI.equalsIgnoreCase("TWIN")) {
								if(ratePlanCategory.equalsIgnoreCase("BROCHURE")) {
									//If the supplier provides two separate rates for adult and child , only Adult price will be taken into consideration,
									//the child price for the search result page will be ignored
									if(type.equals("ADT")) {

										getTotalSearchPkgFare(componentPrice);}//If no passengerType is specified and if in case there are multiple TWIN BROCHURE rooms then the lowest price would be taken into consideration
									else if(type.equals("NA")) {	
									
										getTotalSearchPkgFare(componentPrice);}
									
								
								}
								
							}
						}
					}

				}

			}
		}
	private static void getTotalSearchPkgFare(BigDecimal componentPrice) {
		if(assignFlag) {
			int num = (totalFare).compareTo(componentPrice);
			if( num == 1) {
				totalFare = componentPrice;
			}
		}else {
			totalFare = componentPrice;
			assignFlag=true;
		
}
	}
	

	public static void calculatePriceCruise(JSONObject markupCalcJson, String roomType, BigDecimal amountAfterTaxcc,
			BigDecimal amountBeforeTaxcc, JSONObject cruiseComponentJson,BigDecimal totalPrice) {
		
		
		//for (int x = 0; x < cruiseComponentJson.length(); x++) {
		//	JSONObject cruiseCompJson = cruiseComponentJson.getJSONObject(x);
			JSONArray categoryOptionsArray = cruiseComponentJson.getJSONArray("categoryOptions");
			for (int y = 0; y < categoryOptionsArray.length(); y++) {
				JSONObject categoryOptionsJson = categoryOptionsArray.getJSONObject(y);
				JSONArray categoryOptionArray = categoryOptionsJson.getJSONArray("categoryOption");
				for (int z = 0; z < categoryOptionArray.length(); z++) {
					JSONObject categoryOptionJson = categoryOptionArray.getJSONObject(z);
					String roomTypeSI = categoryOptionJson.getString("type");
					JSONArray totalArray = categoryOptionJson.getJSONArray("total");

					for (int t = 0; t < totalArray.length(); t++) {
						JSONObject totalJson = totalArray.getJSONObject(t);
						currencyCode =totalJson.optString("currencyCode");
						
						BigDecimal amountAfterTaxSI = (BigDecimal) (totalJson.optBigDecimal("amountAfterTax",
								new BigDecimal("0")));
						BigDecimal amountBeforeTaxSI = (BigDecimal) (totalJson.optBigDecimal("amountBeforeTax",
								new BigDecimal("0")));
						int amountAfterTaxvalue = (amountAfterTaxcc).compareTo(amountAfterTaxSI);
						int amountBeforeTaxvalue = amountBeforeTaxcc.compareTo(amountBeforeTaxSI);
						if (roomTypeSI.equalsIgnoreCase(roomType) && amountAfterTaxvalue == 0
								&& amountBeforeTaxvalue == 0) {
							BigDecimal amountAfterTax = markupCalcJson.getBigDecimal(JSON_PROP_TOTALFARE);
							BigDecimal amountBeforeTax = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP)
									.getBigDecimal(JSON_PROP_BASEFARE);

							String currencyCodeCC = markupCalcJson.getString(JSON_PROP_COMMCURRENCY);
							//totalPrice = totalPrice.add(amountAfterTax);
							JSONArray taxArr = markupCalcJson.getJSONObject(JSON_PROP_FAREBREAKUP)
									.getJSONArray(JSON_PROP_TAXDETAILS);

							JSONArray taxArraySI = totalJson.getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAX);
							if (taxArraySI.length() > 0 && taxArr.length() > 0) {
								for (int s = 0; s < taxArr.length(); s++) {
									JSONObject taxJson = taxArr.getJSONObject(s);
									BigDecimal taxValue = taxJson.getBigDecimal(JSON_PROP_TAXVALUE);
									String taxName = taxJson.getString(JSON_PROP_TAXNAME);

									JSONObject taxJsonSI = taxArraySI.getJSONObject(s);
									taxJsonSI.put(JSON_PROP_AMOUNT, taxValue);
									taxJsonSI.put(JSON_PROP_TAXDESCRIPTION, taxName);
									// TODO : check whether we need to replace SI currency code
									// with markup commercials currencycode
									// taxJsonSI.put("currencyCode", currencyCodeCC);

								}
							}
							
							String type = totalJson.getString("type");
							if (type.equals("34")){
								totalJson.put(JSON_PROP_TYPE, "ADT");
							}
							if (type.equals("35")) {
								totalJson.put(JSON_PROP_TYPE, "CHD");
							}
							if (type.equals("36")) {
								totalJson.put(JSON_PROP_TYPE, "INF");
							}
							
							BigDecimal componentPrice = totalPrice.add(amountAfterTax);
							totalJson.put(JSON_PROP_AMOUNTAFTERTAX, componentPrice);
							totalJson.put(JSON_PROP_AMOUNTBEFORETAX, amountBeforeTax);
							// TODO : check whether we need to replace SI currency code with
							// markup commercials currencycode
							// totalJson.put("currencyCode", currencyCode);

							//Below code to set Total package fare for cruise where we do not get ratePlanCategory as "BROCHURE"
							if(roomTypeSI.equalsIgnoreCase("TWIN")) {
								//If the supplier provides two separate rates for adult and child , only Adult price will be taken into consideration,
								//the child price for the search result page will be ignored.Also the lowest price for the TWIN paxType would be set.
							if (type.equals("ADT")) {

								getTotalSearchPkgFare(componentPrice);
							} else if (type.equals("NA")) {

								getTotalSearchPkgFare(componentPrice);
							}
						}
							
						}

					}

				}

			}

		//}
	}

	/**
	 * 
	 * @param dynamicPackElem
	 * @return
	 */
	public static JSONObject getSupplierResponseDynamicPackageJSON(Element dynamicPackElem) {

		JSONObject dynamicPacJson = new JSONObject();
		boolean allowOverrideAirDates = Boolean
				.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem, "./@AllowOverrideAirDates"));

		dynamicPacJson.put(JSON_PROP_ALLOWOVERRIDEAIRDATES, allowOverrideAirDates);

		Element componentElem = XMLUtils.getFirstElementAtXPath(dynamicPackElem, "./ns:Components");

		JSONObject componentJson = new JSONObject();

		// For Hotel Component
		Element[] hotelComponentElementArray = XMLUtils.getElementsAtXPath(componentElem, "./ns:HotelComponent");

		if (hotelComponentElementArray != null && hotelComponentElementArray.length != 0) {
			//JSONArray hotelComponentJsonArr = new JSONArray();
			for (Element hotelCompElem : hotelComponentElementArray) {
				JSONObject hotelComponentJson = getHotelComponentJSON(hotelCompElem);
				if(hotelComponentJson.getString("dynamicPkgAction").toLowerCase().contains("pre"))
					componentJson.put("preNight", hotelComponentJson);
				else if(hotelComponentJson.getString("dynamicPkgAction").toLowerCase().contains("post"))
					componentJson.put("postNight", hotelComponentJson);
				else 
					componentJson.put(JSON_PROP_HOTEL_COMPONENT, hotelComponentJson);
				//hotelComponentJsonArr.put(hotelComponentJson);
			}
			//componentJson.put(JSON_PROP_HOTEL_COMPONENT, hotelComponentJsonArr);
		}

		// Air Component

		Element airComponentELement = XMLUtils.getFirstElementAtXPath(componentElem, "./ns:AirComponent");

		if (airComponentELement != null && airComponentELement.hasChildNodes()) {
			Element[] airItineraryElemArray = XMLUtils.getElementsAtXPath(componentElem,
					"./ns:AirComponent/ns:AirItinerary");

			if (airItineraryElemArray != null && airItineraryElemArray.length != 0) {
				JSONObject airComponentJson = new JSONObject();
				JSONArray airItineraryJsonArr = new JSONArray();

				for (Element airItineryElem : airItineraryElemArray) {

					JSONObject AirItineryJson = getAitItineryComponentJSON(airItineryElem);
					airItineraryJsonArr.put(AirItineryJson);

				}

				airComponentJson.put(JSON_PROP_AIRITINERARY, airItineraryJsonArr);
				componentJson.put(JSON_PROP_AIR_COMPONENT, airComponentJson);
			}
		}

		// PackageOptionComponent

		Element packageOptionComponentElem = XMLUtils.getFirstElementAtXPath(componentElem,
				"./ns:PackageOptionComponent");

		if (packageOptionComponentElem != null && packageOptionComponentElem.hasChildNodes()) {
			// For Transfer Element
			Element transferComponentsElement = XMLUtils.getFirstElementAtXPath(packageOptionComponentElem,
					"./ns:PackageOptions/ns:PackageOption/ns:TPA_Extensions/pac:TransferComponents");

			if (transferComponentsElement != null
					&& (transferComponentsElement.hasChildNodes() || transferComponentsElement.hasAttributes())) {
				JSONArray transferComponent = new JSONArray();
				transferComponent = getTransferComponent(transferComponentsElement);
				componentJson.put(JSON_PROP_TRANSFER_COMPONENT, transferComponent);
			}

			// For Cruise Element
			Element cruiseComponentsElement = XMLUtils.getFirstElementAtXPath(packageOptionComponentElem,
					"./ns:PackageOptions/ns:PackageOption/ns:TPA_Extensions/pac:CruiseComponents");

			if (cruiseComponentsElement != null
					&& (cruiseComponentsElement.hasChildNodes() || cruiseComponentsElement.hasAttributes())) {
				Element[] cruiseComponentsElemArray = XMLUtils.getElementsAtXPath(cruiseComponentsElement,
						"./pac:CruiseComponent");
				
				for (Element cruiseCompElem : cruiseComponentsElemArray) {
				JSONObject cruiseComponent = new JSONObject();
				//JSONArray cruiseComponent = new JSONArray();
				cruiseComponent = getCruiseComponent(cruiseCompElem);
				if(cruiseComponent.getString("dynamicPkgAction").toLowerCase().contains("pre"))
					componentJson.put("preNight", cruiseComponent);
				else if(cruiseComponent.getString("dynamicPkgAction").toLowerCase().contains("post"))
					componentJson.put("postNight", cruiseComponent);
				else 
					componentJson.put(JSON_PROP_CRUISE_COMPONENT, cruiseComponent);
			}
				}

			// For Insurance Element
			Element insuranceComponentsElement = XMLUtils.getFirstElementAtXPath(packageOptionComponentElem,
					"./ns:PackageOptions/ns:PackageOption/ns:TPA_Extensions/pac:InsuranceComponents");

			if (insuranceComponentsElement != null
					&& (insuranceComponentsElement.hasChildNodes() || insuranceComponentsElement.hasAttributes())) {
				JSONArray insuranceComponent = new JSONArray();
				insuranceComponent = getInsuranceComponent(insuranceComponentsElement);
				componentJson.put(JSON_PROP_INSURANCE_COMPONENT, insuranceComponent);
			}

			// For Itinerary Element
			Element itineraryElement = XMLUtils.getFirstElementAtXPath(packageOptionComponentElem,
					"./ns:PackageOptions/ns:PackageOption/ns:TPA_Extensions/pac:itinerary");

			if (itineraryElement != null && (itineraryElement.hasChildNodes() || itineraryElement.hasAttributes())) {
				JSONObject itineraryComponent = new JSONObject();
				itineraryComponent = getItinerary(itineraryElement);
				componentJson.put("itinerary", itineraryComponent);
			}
		}
		dynamicPacJson.put(JSON_PROP_COMPONENTS, componentJson);

		// ResGuests

		Element[] resGuestElements = XMLUtils.getElementsAtXPath(dynamicPackElem, "./ns:ResGuests/ns:ResGuest");

		if (resGuestElements != null && resGuestElements.length > 0) {
			JSONObject resGuestJson = getResGuest(dynamicPackElem);

			dynamicPacJson.put(JSON_PROP_RESGUESTS, resGuestJson);
		}

		// GlobalInfo
		JSONObject globalInfoJson = new JSONObject();
		Element globalInfoElem = XMLUtils.getFirstElementAtXPath(dynamicPackElem, "./ns:GlobalInfo");

		String availabilityStatus = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem,
				"./ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@AvailabilityStatus"));
		if (availabilityStatus != null && availabilityStatus.length() > 0) {
			globalInfoJson.put("availabilityStatus", availabilityStatus);
		}

		String serviceRPH = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem,
				"./ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@ServiceRPH"));
		if (serviceRPH != null && serviceRPH.length() > 0) {
			globalInfoJson.put("serviceRPH", serviceRPH);
		}

		String serviceCategoryCode = String.valueOf(XMLUtils.getValueAtXPath(dynamicPackElem,
				"./ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@ServiceCategoryCode"));
		if (serviceCategoryCode != null && serviceCategoryCode.length() > 0) {
			globalInfoJson.put("serviceCategoryCode", serviceCategoryCode);
		}

		globalInfoJson = getGlobalInfo(globalInfoElem);

		dynamicPacJson.put(JSON_PROP_GLOBALINFO, globalInfoJson);

		return dynamicPacJson;

	}

	private static JSONObject getDynamicPkgID(Element dynamicPkgIDElem) {
		JSONObject dynamicPkgJson = new JSONObject();
		String id = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElem, "./@ID"));
		String id_Context = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElem, "./@ID_Context"));
		dynamicPkgJson.put(JSON_PROP_ID, id);
		dynamicPkgJson.put(JSON_PROP_ID_CONTEXT, id_Context);

		String url = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElem, "./@URL"));
		dynamicPkgJson.put("url", url);

		Element companyNameElem = XMLUtils.getFirstElementAtXPath(dynamicPkgIDElem, "./ns:CompanyName");
		if (companyNameElem != null && (companyNameElem.hasChildNodes() || companyNameElem.hasAttributes())) {
			String companyShortName = String.valueOf(XMLUtils.getValueAtXPath(companyNameElem, "./@CompanyShortName"));
			dynamicPkgJson.put(JSON_PROP_COMPANYNAME, companyShortName);
		}

		Element tourDetailsElem = XMLUtils.getFirstElementAtXPath(dynamicPkgIDElem,
				"./ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourDetails");
		if (tourDetailsElem != null && (tourDetailsElem.hasChildNodes() || tourDetailsElem.hasAttributes())) {
			JSONObject tourDetails = new JSONObject();

			String tourRefID = String.valueOf(XMLUtils.getValueAtXPath(tourDetailsElem, "./pac:TourRefID"));
			String tourName = String.valueOf(XMLUtils.getValueAtXPath(tourDetailsElem, "./pac:TourName"));
			String tourStartCity = String.valueOf(XMLUtils.getValueAtXPath(tourDetailsElem, "./pac:TourStartCity"));
			String tourEndCity = String.valueOf(XMLUtils.getValueAtXPath(tourDetailsElem, "./pac:TourEndCity"));

			tourDetails.put(JSON_PROP_TOURREFID, tourRefID);
			tourDetails.put(JSON_PROP_TOURNAME, tourName);
			tourDetails.put(JSON_PROP_TOURSTARTCITY, tourStartCity);
			tourDetails.put(JSON_PROP_TOURENDCITY, tourEndCity);

			// For IDInfo Elements
			Element[] idInfos = XMLUtils.getElementsAtXPath(dynamicPkgIDElem,
					"./ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourDetails/pac:IDInfo");

			if (idInfos != null && idInfos.length > 0) {
				JSONArray idInfoArray = new JSONArray();

				for (Element idInfo : idInfos) {
					JSONObject idInfoObj = new JSONObject();

					String infoID = String.valueOf(XMLUtils.getValueAtXPath(idInfo, "./pac:ID"));
					if (infoID != null && infoID.length() > 0)
						idInfoObj.put("id", infoID);

					String type = String.valueOf(XMLUtils.getValueAtXPath(idInfo, "./pac:Type"));
					if (type != null && type.length() > 0)
						idInfoObj.put("type", type);

					String name = String.valueOf(XMLUtils.getValueAtXPath(idInfo, "./pac:Name"));
					if (name != null && name.length() > 0)
						idInfoObj.put("name", name);

					idInfoArray.put(idInfoObj);
				}

				tourDetails.put("idInfo", idInfoArray);
			}

			// For GeoInfo Elements
			Element geoInfos = XMLUtils.getFirstElementAtXPath(dynamicPkgIDElem,
					"./ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourDetails/pac:GeoInfo");

			if (geoInfos != null && (geoInfos.hasChildNodes() || geoInfos.hasAttributes())) {
				JSONObject geoInfo = new JSONObject();

				Element[] continentElements = XMLUtils.getElementsAtXPath(geoInfos, "./pac:Continent");

				if (continentElements != null && continentElements.length > 0) {
					JSONArray continentArray = new JSONArray();

					for (Element continentElement : continentElements) {
						JSONObject continentObj = new JSONObject();

						String code = String.valueOf(XMLUtils.getValueAtXPath(continentElement, "./pac:Code"));
						if (code != null && code.length() > 0)
							continentObj.put("code", code);

						String name = String.valueOf(XMLUtils.getValueAtXPath(continentElement, "./pac:Name"));
						if (name != null && name.length() > 0)
							continentObj.put("name", name);

						continentArray.put(continentObj);
					}
					geoInfo.put("continent", continentArray);
				}

				Element[] countryElements = XMLUtils.getElementsAtXPath(geoInfos, "./pac:Country");

				if (countryElements != null && countryElements.length > 0) {
					JSONArray countryArray = new JSONArray();

					for (Element countryElement : countryElements) {
						JSONObject countryObj = new JSONObject();

						String code = String.valueOf(XMLUtils.getValueAtXPath(countryElement, "./pac:Code"));
						if (code != null && code.length() > 0)
							countryObj.put("code", code);

						String name = String.valueOf(XMLUtils.getValueAtXPath(countryElement, "./pac:Name"));
						if (name != null && name.length() > 0)
							countryObj.put("name", name);

						String continentCode = String
								.valueOf(XMLUtils.getValueAtXPath(countryElement, "./pac:ContinentCode"));
						if (continentCode != null && continentCode.length() > 0)
							countryObj.put("continentCode", continentCode);

						countryArray.put(countryObj);
					}
					geoInfo.put("country", countryArray);
				}

				Element[] locationElements = XMLUtils.getElementsAtXPath(geoInfos, "./pac:Country");

				if (locationElements != null && locationElements.length > 0) {
					JSONArray locationArray = new JSONArray();

					for (Element locationElement : locationElements) {
						JSONObject locationObj = new JSONObject();

						String code = String.valueOf(XMLUtils.getValueAtXPath(locationElement, "./pac:CountryCode"));
						if (code != null && code.length() > 0)
							locationObj.put("code", code);

						String name = String.valueOf(XMLUtils.getValueAtXPath(locationElement, "./pac:Name"));
						if (name != null && name.length() > 0)
							locationObj.put("name", name);

						locationArray.put(locationObj);
					}
					geoInfo.put("location", locationArray);
				}

				tourDetails.put("geoInfo", geoInfo);
			}

			// For Section Elements
			Element[] sectionElements = XMLUtils.getElementsAtXPath(dynamicPkgIDElem,
					"./ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourDetails/pac:Section");
			if (sectionElements != null && sectionElements.length > 0) {
				JSONArray sectionArray = new JSONArray();

				for (Element sectionElement : sectionElements) {
					JSONObject sectionObj = new JSONObject();

					String title = String.valueOf(XMLUtils.getValueAtXPath(sectionElement, "./pac:Title"));
					if (title != null && title.length() > 0)
						sectionObj.put("title", title);

					String type = String.valueOf(XMLUtils.getValueAtXPath(sectionElement, "./pac:Type"));
					if (type != null && type.length() > 0)
						sectionObj.put("type", type);

					Element[] textElements = XMLUtils.getElementsAtXPath(sectionElement, "./pac:Text");
					if (textElements != null && textElements.length > 0) {
						JSONArray textArray = new JSONArray();

						for (Element textElement : textElements) {
							JSONObject textObj = new JSONObject();

							String text = XMLUtils.getElementValue(textElement);
							if (text != null && text.length() > 0)
								textObj.put("text", text);

							textArray.put(textObj);
						}

						sectionObj.put("text", textArray);
					}

					sectionArray.put(sectionObj);
				}

				tourDetails.put("section", sectionArray);
			}

			dynamicPkgJson.put(JSON_PROP_TOURDETAILS, tourDetails);
		}

		return dynamicPkgJson;
	}

	private static JSONObject getBookingRule(Element bookingRulesElem) {
		JSONObject bookingRuleJson = new JSONObject();

		Element descriptionElem = XMLUtils.getFirstElementAtXPath(bookingRulesElem, "./ns:Description");

		String name = String.valueOf(XMLUtils.getValueAtXPath(descriptionElem, "./@Name"));

		bookingRuleJson.put(JSON_PROP_NAME, name);

		String text = String.valueOf(XMLUtils.getValueAtXPath(descriptionElem, "./ns:Text"));

		bookingRuleJson.put(JSON_PROP_TEXT, text);

		return bookingRuleJson;

	}

	private static JSONObject getFee(Element feeElem) {
		JSONObject feeJson = new JSONObject();

		String type = String.valueOf(XMLUtils.getValueAtXPath(feeElem, "./@Type"));

		String amount = String.valueOf(XMLUtils.getValueAtXPath(feeElem, "./@Amount"));

		String code = String.valueOf(XMLUtils.getValueAtXPath(feeElem, "./@Code"));

		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(feeElem, "./@CurrencyCode"));

		String maxChargeUnitApplies = String.valueOf(XMLUtils.getValueAtXPath(feeElem, "./@MaxChargeUnitApplies"));

		feeJson.put("type", type);
		feeJson.put(JSON_PROP_AMOUNT, amount);
		feeJson.put("feeCode", code);
		feeJson.put(JSON_PROP_CURRENCYCODE, currencyCode);
		feeJson.put(JSON_PROP_MAXCHARGEUNITAPPLIES, maxChargeUnitApplies);

		JSONObject taxJSon = getChargeTax(feeElem);

		feeJson.put(JSON_PROP_TAXES, taxJSon);

		Element descriptionElem = XMLUtils.getFirstElementAtXPath(feeElem, "./ns:Description");

		String name = String.valueOf(XMLUtils.getValueAtXPath(descriptionElem, "./@Name"));

		feeJson.put("feeName", name);

		String text = String.valueOf(XMLUtils.getValueAtXPath(descriptionElem, "./ns:Text"));

		feeJson.put(JSON_PROP_TEXT, text);

		Element pkgs_TPAElem = XMLUtils.getFirstElementAtXPath(feeElem, "./ns:TPA_Extensions/pac:Pkgs_TPA");

		String isIncludedInTour = String.valueOf(XMLUtils.getValueAtXPath(pkgs_TPAElem, "./pac:isIncludedInTour"));

		String RequiredForBooking = String.valueOf(XMLUtils.getValueAtXPath(pkgs_TPAElem, "./pac:RequiredForBooking"));
		feeJson.put(JSON_PROP_ISINCLUDEDINTOUR, isIncludedInTour);
		feeJson.put(JSON_PROP_REQUIREDFORBOOKING, RequiredForBooking);

		return feeJson;
	}

	private static JSONObject getComment(Element commentElem) {
		JSONObject commentJson = new JSONObject();

		String name = String.valueOf(XMLUtils.getValueAtXPath(commentElem, "./@Name"));
		commentJson.put(JSON_PROP_NAME, name);

		Element[] texts = XMLUtils.getElementsAtXPath(commentElem, "./ns:Text");

		if (texts != null && texts.length > 0) {
			JSONArray textArray = new JSONArray();
			for (Element text : texts) {
				String textStr = XMLUtils.getElementValue(text);

				textArray.put(textStr);
			}

			commentJson.put(JSON_PROP_TEXT, textArray);
		}

		Element[] images = XMLUtils.getElementsAtXPath(commentElem, "./ns:Image");

		if (images != null && images.length > 0) {
			JSONArray imageArray = new JSONArray();
			for (Element image : images) {
				String imageStr = XMLUtils.getElementValue(image);

				imageArray.put(imageStr);
			}

			commentJson.put("images", imageArray);
		}

		Element[] urls = XMLUtils.getElementsAtXPath(commentElem, "./ns:URL");

		if (urls != null && urls.length > 0) {
			JSONArray urlArray = new JSONArray();
			for (Element url : urls) {
				String urlStr = XMLUtils.getElementValue(url);

				urlArray.put(urlStr);
			}

			commentJson.put("urls", urlArray);
		}

		return commentJson;
	}

	public static JSONObject getGlobalInfoTimespan(Element globalInfoElem) {

		Element timeSpanElem = XMLUtils.getFirstElementAtXPath(globalInfoElem, "./ns:TimeSpan");
		JSONObject timeSpanJson = new JSONObject();

		String end = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElem, "./@End"));
		timeSpanJson.put(JSON_PROP_END, end);
		String start = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElem, "./@Start"));
		timeSpanJson.put(JSON_PROP_START, start);
		String travelEndDate = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElem, "./@TravelEndDate"));
		timeSpanJson.put(JSON_PROP_TRAVELENDDATE, travelEndDate);
		String travelStartDate = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElem, "./@TravelStartDate"));
		timeSpanJson.put(JSON_PROP_TRAVELSTARTDATE, travelStartDate);

		return timeSpanJson;
	}

	public static JSONObject getGlobalInfo(Element globalInfoElem) {

		JSONObject globalInfoJson = new JSONObject();

		// For Time Span
		JSONObject timeSpanJson = getGlobalInfoTimespan(globalInfoElem);
		globalInfoJson.put(JSON_PROP_TIMESPAN, timeSpanJson);

		// Comments
		Element[] commentElems = XMLUtils.getElementsAtXPath(globalInfoElem, "./ns:Comments/ns:Comment");
		JSONArray CommentArray = new JSONArray();

		for (Element commentElem : commentElems) {
			JSONObject CommentJson = getComment(commentElem);
			CommentArray.put(CommentJson);
		}
		globalInfoJson.put(JSON_PROP_COMMENT, CommentArray);

		// CancelPenalties
		Element cancelPenaltiesElem = XMLUtils.getFirstElementAtXPath(globalInfoElem, "./ns:CancelPenalties");
		String cancelPolicyIndicator = String
				.valueOf(XMLUtils.getValueAtXPath(cancelPenaltiesElem, "./@CancelPolicyIndicator"));

		JSONObject cancelPenaltiesJson = new JSONObject();
		cancelPenaltiesJson.put(JSON_PROP_CANCELPOLICYINDICATOR, cancelPolicyIndicator);

		globalInfoJson.put(JSON_PROP_CANCELPENALTIES, cancelPenaltiesJson);

		// Fee
		Element[] feeElems = XMLUtils.getElementsAtXPath(globalInfoElem, "./ns:Fees/ns:Fee");
		JSONArray feeArray = new JSONArray();
		for (Element feeElem : feeElems) {

			JSONObject feeJson = getFee(feeElem);
			feeArray.put(feeJson);

		}
		globalInfoJson.put(JSON_PROP_FEE, feeArray);

		// BookingRules
		Element[] bookingRulesElems = XMLUtils.getElementsAtXPath(globalInfoElem, "./ns:BookingRules/ns:BookingRule");
		JSONArray bookingRulesArray = new JSONArray();

		for (Element bookingRulesElem : bookingRulesElems) {
			JSONObject bookingRuleJson = getBookingRule(bookingRulesElem);
			bookingRulesArray.put(bookingRuleJson);
		}
		globalInfoJson.put(JSON_PROP_BOOKINGRULE, bookingRulesArray);

		// DynamicPkgID
		Element dynamicPkgIDElement = XMLUtils.getFirstElementAtXPath(globalInfoElem,
				"./ns:DynamicPkgIDs/ns:DynamicPkgID");
		if (dynamicPkgIDElement != null
				&& (dynamicPkgIDElement.hasChildNodes() || dynamicPkgIDElement.hasAttributes())) {
			JSONObject dynamicpacJson = getDynamicPkgID(dynamicPkgIDElement);

			globalInfoJson.put(JSON_PROP_DYNAMICPKGID, dynamicpacJson);
		}

		// For Promotions Element
		Element promotionsElement = XMLUtils.getFirstElementAtXPath(globalInfoElem, "./ns:Promotions");

		if (promotionsElement != null && promotionsElement.hasChildNodes()) {
			JSONArray promotion = new JSONArray();

			Element[] promotionElements = XMLUtils.getElementsAtXPath(promotionsElement, "./ns:Promotion");

			for (Element promotionElement : promotionElements) {
				JSONObject promotionObj = new JSONObject();

				String isIncludedInTour = String
						.valueOf(XMLUtils.getValueAtXPath(promotionElement, "./@isIncludedInTour"));
				promotionObj.put("isIncludedInTour", isIncludedInTour);

				String amount = String.valueOf(XMLUtils.getValueAtXPath(promotionElement, "./@Amount"));
				promotionObj.put("amount", amount);

				String description = String.valueOf(XMLUtils.getValueAtXPath(promotionElement, "./@Description"));
				promotionObj.put("description", description);

				String id = String.valueOf(XMLUtils.getValueAtXPath(promotionElement, "./@ID"));
				promotionObj.put("id", id);

				String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(promotionElement, "./@CurrencyCode"));
				promotionObj.put("currencyCode", currencyCode);

				promotion.put(promotionObj);
			}

			globalInfoJson.put("promotion", promotion);
		}

		return globalInfoJson;
	}

	public static JSONObject getResGuest(Element dynamicPackElem) {
		Element resGuestsElem = XMLUtils.getFirstElementAtXPath(dynamicPackElem,
				"./ns:ResGuests/ns:ResGuest/ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourOccupancyRules");
		JSONObject resGuestJson = new JSONObject();
		JSONObject tourOccupancyRules = new JSONObject();

		JSONObject adultJson = new JSONObject();
		Element adultElem = XMLUtils.getFirstElementAtXPath(resGuestsElem, "./pac:Adult");
		String minimumAge = String.valueOf(XMLUtils.getValueAtXPath(adultElem, "./@MinimumAge"));
		adultJson.put(JSON_PROP_MINIMUMAGE, minimumAge);

		String maximumAge = String.valueOf(XMLUtils.getValueAtXPath(adultElem, "./@MaximumAge"));
		adultJson.put(JSON_PROP_MAXIMUMAGE, maximumAge);

		JSONObject childJson = new JSONObject();
		Element childElem = XMLUtils.getFirstElementAtXPath(dynamicPackElem,
				"./ns:ResGuests/ns:ResGuest/ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourOccupancyRules/pac:Child");
		String minimumAge1 = String.valueOf(XMLUtils.getValueAtXPath(childElem, "./@MinimumAge"));
		childJson.put(JSON_PROP_MINIMUMAGE, minimumAge1);

		String maximumAge1 = String.valueOf(XMLUtils.getValueAtXPath(childElem, "./@MaximumAge"));
		childJson.put(JSON_PROP_MAXIMUMAGE, maximumAge1);

		tourOccupancyRules.put(JSON_PROP_ADULT, adultJson);
		tourOccupancyRules.put(JSON_PROP_CHILD, childJson);

		resGuestJson.put("tourOccupancyRules", tourOccupancyRules);

		return resGuestJson;
	}

	private static JSONObject getPackageOption(Element packageOptionsElem) {
		JSONObject packageOption = new JSONObject();
		Element packageOptionElem = XMLUtils.getFirstElementAtXPath(packageOptionsElem, "./ns:PackageOption");

		String availabilityStatus = String
				.valueOf(XMLUtils.getValueAtXPath(packageOptionElem, "./@AvailabilityStatus"));
		packageOption.put(JSON_PROP_AVAILABILITYSTATUS, availabilityStatus);

		String iD_Context = String.valueOf(XMLUtils.getValueAtXPath(packageOptionElem, "./@ID_Context"));
		packageOption.put(JSON_PROP_ID_CONTEXT, iD_Context);

		String quoteID = String.valueOf(XMLUtils.getValueAtXPath(packageOptionElem, "./@QuoteID"));
		packageOption.put(JSON_PROP_QUOTEID, quoteID);

		// TPA_Extensions

		JSONObject tPA_Extensions = new JSONObject();
		Element tPA_ExtensionsElem = XMLUtils.getFirstElementAtXPath(packageOptionElem, "./ns:TPA_Extensions");

		Element transferComponentsElem = XMLUtils.getFirstElementAtXPath(tPA_ExtensionsElem,
				"./pac:TransferComponents");

		Element[] transferComponentElemArray = XMLUtils.getElementsAtXPath(transferComponentsElem,
				"./pac:TransferComponent");
		JSONArray transfercompArray = new JSONArray();
		for (Element transferCompElem : transferComponentElemArray) {
			JSONObject transferCompJson = new JSONObject();
			String dynamicPkgAction = String.valueOf(XMLUtils.getValueAtXPath(transferCompElem, "./@DynamicPkgAction"));
			transferCompJson.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

			JSONArray GroundServiceJsonArray = getGroundService(transferCompElem, dynamicPkgAction);
			transferCompJson.put(JSON_PROP_GROUNDSERVICE, GroundServiceJsonArray);
			transfercompArray.put(transferCompJson);
		}

		// CruiseComponents

		Element[] cruiseComponentsElemArray = XMLUtils.getElementsAtXPath(tPA_ExtensionsElem,
				"./pac:CruiseComponents/pac:CruiseComponent");

		JSONArray cruisecompArray = new JSONArray();
		for (Element cruiseCompElem : cruiseComponentsElemArray) {
			JSONObject cruiseCompJson = new JSONObject();
			String dynamicPkgAction = String.valueOf(XMLUtils.getValueAtXPath(cruiseCompElem, "./@DynamicPkgAction"));
			cruiseCompJson.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

			String name = String.valueOf(XMLUtils.getValueAtXPath(cruiseCompElem, "./@Name"));
			cruiseCompJson.put(JSON_PROP_NAME, name);
			cruisecompArray.put(cruiseCompJson);

			JSONArray categoryOptionJsonArray = getCategoryOption(cruiseCompElem);
			cruiseCompJson.put(JSON_PROP_CATEGORYOPTION, categoryOptionJsonArray);

		}

		tPA_Extensions.put(JSON_PROP_TRANSFER_COMPONENT, transfercompArray);
		tPA_Extensions.put(JSON_PROP_CRUISE_COMPONENT, cruisecompArray);

		// InsuranceComponents
		Element insuranceComponentsElem = XMLUtils.getFirstElementAtXPath(tPA_ExtensionsElem,
				"./pac:InsuranceComponents/pac:InsuranceComponent");

		JSONObject insuranceCompJson = new JSONObject();
		String dynamicPkgAction = String
				.valueOf(XMLUtils.getValueAtXPath(insuranceComponentsElem, "./@DynamicPkgAction"));
		insuranceCompJson.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

		String isIncludedInTour = String
				.valueOf(XMLUtils.getValueAtXPath(insuranceComponentsElem, "./@isIncludedInTour"));
		insuranceCompJson.put(JSON_PROP_ISINCLUDEDINTOUR, isIncludedInTour);

		Element insCoverageDetailElem = XMLUtils.getFirstElementAtXPath(insuranceComponentsElem,
				"./pac:InsCoverageDetail");
		JSONObject insCoverageDetail = new JSONObject();
		String description = String.valueOf(XMLUtils.getValueAtXPath(insCoverageDetailElem, "./@Description"));
		String name = String.valueOf(XMLUtils.getValueAtXPath(insCoverageDetailElem, "./@Name"));

		insCoverageDetail.put(JSON_PROP_DESCRIPTION, description);
		insCoverageDetail.put(JSON_PROP_NAME, name);

		insuranceCompJson.put(JSON_PROP_INSCOVERAGEDETAIL, insCoverageDetail);

		// PlanCost

		Element[] planCostElems = XMLUtils.getElementsAtXPath(tPA_ExtensionsElem,
				"./pac:InsuranceComponents/pac:InsuranceComponent/pac:PlanCost");

		JSONArray planCostArray = new JSONArray();
		for (Element PlanCostElem : planCostElems) {

			JSONObject planCostJson = getPlanCost(PlanCostElem);

			planCostArray.put(planCostJson);

		}

		insuranceCompJson.put(JSON_PROP_PLANCOST, planCostArray);

		tPA_Extensions.put(JSON_PROP_INSURANCECOMPONENT, insuranceCompJson);

		packageOption.put(JSON_PROP_TPA_EXTENSIONS, tPA_Extensions);

		return packageOption;
	}

	private static JSONArray getCategoryOption(Element cruiseCompElem) {

		Element[] categoryOptionsElements = XMLUtils.getElementsAtXPath(cruiseCompElem, "./pac:CategoryOptions");

		JSONArray categoryOptionsArray = new JSONArray();

		for (Element categoryOptionsElement : categoryOptionsElements) {
			JSONObject categoryOptionsJSON = new JSONObject();

			Element[] categoryOptionElements = XMLUtils.getElementsAtXPath(categoryOptionsElement,
					"./pac:CategoryOption");

			JSONArray categoryOptionArray = new JSONArray();

			for (Element categoryOptionElement : categoryOptionElements) {
				JSONObject categoryOptionJSON = new JSONObject();

				String name = String.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@Name"));
				String description = String.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@Description"));
				String availabilityStatus = String
						.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@AvailabilityStatus"));
				String type = String.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@Type"));

				categoryOptionJSON.put(JSON_PROP_NAME, name);
				categoryOptionJSON.put(JSON_PROP_DESCRIPTION, description);
				categoryOptionJSON.put(JSON_PROP_AVAILABILITYSTATUS, availabilityStatus);
				categoryOptionJSON.put(JSON_PROP_TYPE, type);

				String maximumLengthOfStay = String
						.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@MaximumLengthOfStay"));
				if (maximumLengthOfStay != null && maximumLengthOfStay.length() > 0)
					categoryOptionJSON.put("maximumLengthOfStay", maximumLengthOfStay);

				String minimumLengthOfStay = String
						.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@MinimumLengthOfStay"));
				if (minimumLengthOfStay != null && minimumLengthOfStay.length() > 0)
					categoryOptionJSON.put("minimumLengthOfStay", minimumLengthOfStay);

				String isIncludedInTour = String
						.valueOf(XMLUtils.getValueAtXPath(categoryOptionElement, "./@isIncludedInTour"));
				if (isIncludedInTour != null && isIncludedInTour.length() > 0)
					categoryOptionJSON.put("isIncludedInTour", isIncludedInTour);

				// For Total Element
				Element priceElem = XMLUtils.getFirstElementAtXPath(categoryOptionElement, "./pac:Price");

				Element[] totalElems = XMLUtils.getElementsAtXPath(priceElem, "./ns:Total");
				JSONArray totalJson = new JSONArray();

				for (Element totalElem : totalElems) {
					JSONObject totalObj = new JSONObject();

					String amountBeforeTax = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@AmountBeforeTax"));
					String amountAfterTax = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@AmountAfterTax"));
					String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@CurrencyCode"));
					String totalType = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@Type"));

					totalObj.put(JSON_PROP_AMOUNTBEFORETAX, amountBeforeTax);
					totalObj.put(JSON_PROP_CURRENCYCODE, currencyCode);
					totalObj.put(JSON_PROP_AMOUNTAFTERTAX, amountAfterTax);
					totalObj.put("type", totalType);

					JSONObject taxJson = getChargeTax(totalElem);

					totalObj.put(JSON_PROP_TAXES, taxJson);

					totalJson.put(totalObj);
				}
				categoryOptionJSON.put("total", totalJson);

				// For Guest Count Element
				Element guestCountElem = XMLUtils.getFirstElementAtXPath(categoryOptionElement,
						"./pac:GuestCounts/ns:GuestCount");
				JSONObject guestCountJson = new JSONObject();
				String count = String.valueOf(XMLUtils.getValueAtXPath(guestCountElem, "./@Count"));
				guestCountJson.put(JSON_PROP_COUNT, count);
				categoryOptionJSON.put(JSON_PROP_GUESTCOUNT, guestCountJson);

				// For Address Element
				Element addressElement = XMLUtils.getFirstElementAtXPath(categoryOptionElement, "./pac:Address");

				if (addressElement != null && (addressElement.hasChildNodes() || addressElement.hasAttributes())) {
					JSONObject address = new JSONObject();

					Element[] addressLineElements = XMLUtils.getElementsAtXPath(addressElement, "./ns:AddressLine");

					JSONArray addressLineArray = new JSONArray();

					for (Element addressLineElement : addressLineElements) {
						JSONObject addressLine = new JSONObject();

						String addressLineValue = XMLUtils.getElementValue(addressLineElement);

						addressLine.put("addressLine", addressLineValue);

						addressLineArray.put(addressLine);
					}
					address.put("addressLines", addressLineArray);

					Element cityNameElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:CityName");
					String cityName = XMLUtils.getElementValue(cityNameElement);
					address.put("cityName", cityName);

					Element postalCodeElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:PostalCode");
					String postalCode = XMLUtils.getElementValue(postalCodeElement);
					address.put("postalCode", postalCode);

					Element countyElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:County");
					String county = XMLUtils.getElementValue(countyElement);
					address.put("county", county);

					Element stateProvElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:StateProv");
					String stateProv = XMLUtils.getElementValue(stateProvElement);
					address.put("stateProv", stateProv);

					Element countryNameElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:CountryName");
					String countryName = XMLUtils.getElementValue(countryNameElement);
					address.put("countryName", countryName);

					categoryOptionJSON.put("address", address);
				}

				// Last MinuteDiscount Element
				Element[] lastMinuteDiscountsElement = XMLUtils.getElementsAtXPath(categoryOptionElement,
						"./tns:LastMinuteDiscount/tns:LastMinuteDiscounts");

				if (lastMinuteDiscountsElement != null && lastMinuteDiscountsElement.length > 0) {
					JSONArray lastMinuteDiscounts = new JSONArray();
					for (Element lastMinuteDiscountElement : lastMinuteDiscountsElement) {
						JSONObject lastMinuteDiscount = new JSONObject();

						String amountBeforeTaxLMD = String
								.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@AmountBeforeTax"));
						lastMinuteDiscount.put("amountBeforeTax", amountBeforeTaxLMD);

						String amountAfterTaxLMD = String
								.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@AmountAfterTax"));
						lastMinuteDiscount.put("amountAfterTax", amountAfterTaxLMD);

						String CurrencyCodeLMD = String
								.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@CurrencyCode"));
						lastMinuteDiscount.put("currencyCode", CurrencyCodeLMD);

						String typeLMD = String.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@Type"));
						lastMinuteDiscount.put("type", typeLMD);

						lastMinuteDiscounts.put(lastMinuteDiscount);
					}

					categoryOptionJSON.put("lastMinuteDiscounts", lastMinuteDiscounts);
				}

				// For Passenger Element
				Element[] passengerElements = XMLUtils.getElementsAtXPath(categoryOptionElement, "./pac:Passenger");

				if (passengerElements != null && passengerElements.length > 0) {
					JSONArray passengers = new JSONArray();

					for (Element passengerElement : passengerElements) {
						JSONObject passenger = new JSONObject();

						String maximumPassengers = String
								.valueOf(XMLUtils.getValueAtXPath(passengerElement, "./@MaximumPassengers"));
						if (maximumPassengers != null & maximumPassengers.length() > 0)
							passenger.put("maximumPassengers", maximumPassengers);

						String minimumPassengers = String
								.valueOf(XMLUtils.getValueAtXPath(passengerElement, "./@MinimumPassengers"));
						if (minimumPassengers != null & minimumPassengers.length() > 0)
							passenger.put("minimumPassengers", minimumPassengers);

						String miniimumPayingPassengers = String
								.valueOf(XMLUtils.getValueAtXPath(passengerElement, "./@MinimumPayingPassengers"));
						if (miniimumPayingPassengers != null & miniimumPayingPassengers.length() > 0)
							passenger.put("miniimumPayingPassengers", miniimumPayingPassengers);

						passengers.put(passenger);
					}

					categoryOptionJSON.put("passengers", passengers);
				}

				// For Cabin Options Element
				Element cabinOptionsElem = XMLUtils.getFirstElementAtXPath(categoryOptionElement, "./pac:CabinOptions");

				Element[] cabinOptionElems = XMLUtils.getElementsAtXPath(cabinOptionsElem, "./pac:CabinOption");
				JSONArray cabinOptionArr = new JSONArray();
				for (Element cabinOptionElem : cabinOptionElems) {
					JSONObject cabinOptionJson = new JSONObject();

					String cabinNumber = String.valueOf(XMLUtils.getValueAtXPath(cabinOptionElem, "./@CabinNumber"));
					String status = String.valueOf(XMLUtils.getValueAtXPath(cabinOptionElem, "./@Status"));
					String maxOccupancy = String.valueOf(XMLUtils.getValueAtXPath(cabinOptionElem, "./@MaxOccupancy"));
					String minOccupancy = String.valueOf(XMLUtils.getValueAtXPath(cabinOptionElem, "./@MinOccupancy"));

					cabinOptionJson.put(JSON_PROP_CABINNUMBER, cabinNumber);
					cabinOptionJson.put(JSON_PROP_STATUS, status);
					cabinOptionJson.put("maxOccupancy", maxOccupancy);
					cabinOptionJson.put("minOccupancy", minOccupancy);

					cabinOptionArr.put(cabinOptionJson);

				}
				categoryOptionJSON.put(JSON_PROP_CABINOPTION, cabinOptionArr);

				categoryOptionArray.put(categoryOptionJSON);
			}

			categoryOptionsJSON.put("categoryOption", categoryOptionArray);

			categoryOptionsArray.put(categoryOptionsJSON);
		}

		return categoryOptionsArray;
	}

	private static JSONArray getTransferComponent(Element transferComponentsElement) {
		Element[] transferComponentElemArray = XMLUtils.getElementsAtXPath(transferComponentsElement,
				"./pac:TransferComponent");

		JSONArray transfercompArray = new JSONArray();

		for (Element transferCompElem : transferComponentElemArray) {
			JSONObject transferCompJson = new JSONObject();
			String dynamicPkgAction = String.valueOf(XMLUtils.getValueAtXPath(transferCompElem, "./@DynamicPkgAction"));
			transferCompJson.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

			String id = String.valueOf(XMLUtils.getValueAtXPath(transferCompElem, "./@ID"));
			if (id != null && id.length() > 0)
				transferCompJson.put("id", id);

			JSONArray GroundServiceJsonArray = getGroundService(transferCompElem, dynamicPkgAction);
			transferCompJson.put(JSON_PROP_GROUNDSERVICE, GroundServiceJsonArray);
			transfercompArray.put(transferCompJson);
		}

		return transfercompArray;
	}

	private static JSONObject getCruiseComponent(Element cruiseComponentsElement) {
//		Element[] cruiseComponentsElemArray = XMLUtils.getElementsAtXPath(cruiseComponentsElement,
//				"./pac:CruiseComponent");

		//JSONArray cruisecompArray = new JSONArray();

		//for (Element cruiseCompElem : cruiseComponentsElemArray) {
			JSONObject cruiseCompJson = new JSONObject();
			String dynamicPkgAction = String.valueOf(XMLUtils.getValueAtXPath(cruiseComponentsElement, "./@DynamicPkgAction"));
			cruiseCompJson.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

			String name = String.valueOf(XMLUtils.getValueAtXPath(cruiseComponentsElement, "./@Name"));
			cruiseCompJson.put(JSON_PROP_NAME, name);

			String id = String.valueOf(XMLUtils.getValueAtXPath(cruiseComponentsElement, "./@ID"));
			cruiseCompJson.put("id", id);

			JSONArray categoryOptionJsonArray = getCategoryOption(cruiseComponentsElement);
			cruiseCompJson.put("categoryOptions", categoryOptionJsonArray);

			//cruisecompArray.put(cruiseCompJson);
		//}

		return cruiseCompJson;
	}

	private static JSONArray getInsuranceComponent(Element insuranceComponentsElement) {
		// InsuranceComponents
		Element[] insuranceComponentElements = XMLUtils.getElementsAtXPath(insuranceComponentsElement,
				"./pac:InsuranceComponent");

		JSONArray insuranceComponentArray = new JSONArray();

		for (Element insuranceComponentElement : insuranceComponentElements) {
			JSONObject insuranceComponent = new JSONObject();

			String dynamicPkgAction = String
					.valueOf(XMLUtils.getValueAtXPath(insuranceComponentElement, "./@DynamicPkgAction"));
			insuranceComponent.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

			String isIncludedInTour = String
					.valueOf(XMLUtils.getValueAtXPath(insuranceComponentElement, "./@isIncludedInTour"));
			insuranceComponent.put(JSON_PROP_ISINCLUDEDINTOUR, isIncludedInTour);

			Element insCoverageDetailElem = XMLUtils.getFirstElementAtXPath(insuranceComponentElement,
					"./pac:InsCoverageDetail");

			JSONObject insCoverageDetail = new JSONObject();
			String description = String.valueOf(XMLUtils.getValueAtXPath(insCoverageDetailElem, "./@Description"));
			String name = String.valueOf(XMLUtils.getValueAtXPath(insCoverageDetailElem, "./@Name"));
			String type = String.valueOf(XMLUtils.getValueAtXPath(insCoverageDetailElem, "./@Type"));
			String planID = String.valueOf(XMLUtils.getValueAtXPath(insCoverageDetailElem, "./@PlanID"));

			insCoverageDetail.put(JSON_PROP_DESCRIPTION, description);
			insCoverageDetail.put(JSON_PROP_NAME, name);
			insCoverageDetail.put("type", type);
			insCoverageDetail.put("planID", planID);

			insuranceComponent.put(JSON_PROP_INSCOVERAGEDETAIL, insCoverageDetail);

			// PlanCost
			Element[] planCostElems = XMLUtils.getElementsAtXPath(insuranceComponentElement, "./pac:PlanCost");

			JSONArray planCostArray = new JSONArray();
			for (Element PlanCostElem : planCostElems) {

				JSONObject planCostJson = getPlanCost(PlanCostElem);

				planCostArray.put(planCostJson);
			}
			insuranceComponent.put(JSON_PROP_PLANCOST, planCostArray);

			insuranceComponentArray.put(insuranceComponent);
		}

		return insuranceComponentArray;
	}

	private static JSONObject getItinerary(Element itineraryElements) {
		Element[] days = XMLUtils.getElementsAtXPath(itineraryElements, "./pac:days/pac:day");

		JSONObject itinerary = new JSONObject();

		JSONArray daysArray = new JSONArray();

		for (Element dayElement : days) {
			JSONObject dayObj = new JSONObject();

			String day = String.valueOf(XMLUtils.getValueAtXPath(dayElement, "./pac:day"));
			if (day != null && day.length() > 0)
				dayObj.put("day", day);

			String label = String.valueOf(XMLUtils.getValueAtXPath(dayElement, "./pac:label"));
			if (label != null && label.length() > 0)
				dayObj.put("label", label);

			String accommodation = String.valueOf(XMLUtils.getValueAtXPath(dayElement, "./pac:Accommodation"));
			if (accommodation != null && accommodation.length() > 0)
				dayObj.put("accommodation", accommodation);

			String duration = String.valueOf(XMLUtils.getValueAtXPath(dayElement, "./pac:Duration"));
			if (duration != null && duration.length() > 0)
				dayObj.put("duration", duration);

			// For Description Element
			Element[] descriptionElements = XMLUtils.getElementsAtXPath(dayElement, "./pac:description");

			JSONArray descArray = new JSONArray();

			for (Element descriptionElement : descriptionElements) {
				String description = descriptionElement.getTextContent();
				if (description != null && description.length() > 0)
					descArray.put(description);
			}
			dayObj.put("descriptions", descArray);

			// For Meal Element
			Element[] mealElements = XMLUtils.getElementsAtXPath(dayElement, "./pac:Meals/pac:Meal");

			JSONArray mealArray = new JSONArray();

			for (Element mealElement : mealElements) {
				JSONObject mealObj = new JSONObject();

				String type = String.valueOf(XMLUtils.getValueAtXPath(mealElement, "./pac:Type"));
				if (type != null && type.length() > 0)
					mealObj.put("type", type);

				String mealNumber = String.valueOf(XMLUtils.getValueAtXPath(mealElement, "./pac:MealNumber"));
				if (mealNumber != null && mealNumber.length() > 0)
					mealObj.put("mealNumber", mealNumber);

				mealArray.put(mealObj);
			}
			dayObj.put("meals", mealArray);

			// For Meal Element
			Element[] locationElements = XMLUtils.getElementsAtXPath(dayElement, "./pac:Locations/pac:Location");

			JSONArray locationArray = new JSONArray();

			for (Element locationElement : locationElements) {
				JSONObject locationObj = new JSONObject();

				String type = String.valueOf(XMLUtils.getValueAtXPath(locationElement, "./pac:Type"));
				if (type != null && type.length() > 0)
					locationObj.put("type", type);

				String name = String.valueOf(XMLUtils.getValueAtXPath(locationElement, "./pac:Name"));
				if (name != null && name.length() > 0)
					locationObj.put("name", name);

				String countryCode = String.valueOf(XMLUtils.getValueAtXPath(locationElement, "./pac:CountryCode"));
				if (countryCode != null && countryCode.length() > 0)
					locationObj.put("countryCode", countryCode);

				locationArray.put(locationObj);
			}
			dayObj.put("location", locationArray);

			daysArray.put(dayObj);
		}

		itinerary.put("days", daysArray);

		return itinerary;
	}

	private static JSONObject getPlanCost(Element planCostElem) {
		JSONObject planCostJson = new JSONObject();

		String amount = String.valueOf(XMLUtils.getValueAtXPath(planCostElem, "./@Amount"));
		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(planCostElem, "./@CurrencyCode"));

		planCostJson.put(JSON_PROP_AMOUNT, amount);
		planCostJson.put(JSON_PROP_CURRENCYCODE, currencyCode);

		Element basePremiumElem = XMLUtils.getFirstElementAtXPath(planCostElem, "./ns:BasePremium");

		JSONObject basePremiumJson = new JSONObject();
		String amount1 = String.valueOf(XMLUtils.getValueAtXPath(basePremiumElem, "./@Amount"));
		String currencyCode1 = String.valueOf(XMLUtils.getValueAtXPath(basePremiumElem, "./@CurrencyCode"));

		basePremiumJson.put(JSON_PROP_AMOUNT, amount1);
		basePremiumJson.put(JSON_PROP_CURRENCYCODE, currencyCode1);

		planCostJson.put("basePremium", basePremiumJson);

		Element[] chargeElements = XMLUtils.getElementsAtXPath(planCostElem, "./ns:Charges/ns:Charge");

		JSONArray chargeArray = new JSONArray();

		for (Element chargeElement : chargeElements) {
			JSONObject chargeObj = new JSONObject();

			String type = String.valueOf(XMLUtils.getValueAtXPath(chargeElement, "./@Type"));

			String chargeAmount = String.valueOf(XMLUtils.getValueAtXPath(chargeElement, "./@Amount"));

			String curencyCode = String.valueOf(XMLUtils.getValueAtXPath(chargeElement, "./@CurrencyCode"));

			JSONObject taxJson = getChargeTax(chargeElement);

			if (type != null && type.length() > 0)
				chargeObj.put("type", type);

			if (chargeAmount != null && chargeAmount.length() > 0)
				chargeObj.put("amount", chargeAmount);

			if (curencyCode != null && curencyCode.length() > 0)
				chargeObj.put("curencyCode", curencyCode);

			chargeObj.put("taxes", taxJson);

			chargeArray.put(chargeObj);
		}
		planCostJson.put("charge", chargeArray);

		return planCostJson;
	}

	private static JSONObject getChargeTax(Element chargeElem) {

		JSONObject taxes = new JSONObject();

		JSONArray taxArray = new JSONArray();

		Element[] taxElems = XMLUtils.getElementsAtXPath(chargeElem, "./ns:Taxes/ns:Tax");

		for (Element taxElem : taxElems) {
			JSONObject taxJson = new JSONObject();

			String amount1 = String.valueOf(XMLUtils.getValueAtXPath(taxElem, "./@Amount"));
			String currencyCode1 = String.valueOf(XMLUtils.getValueAtXPath(taxElem, "./@CurrencyCode"));

			taxJson.put(JSON_PROP_AMOUNT, amount1);
			taxJson.put(JSON_PROP_CURRENCYCODE, currencyCode1);

			Element taxDescriptionElem = XMLUtils.getFirstElementAtXPath(taxElem, "./ns:TaxDescription");

			String name = String.valueOf(XMLUtils.getValueAtXPath(taxDescriptionElem, "./@Name"));

			taxJson.put(JSON_PROP_TAXDESCRIPTION, name);

			taxArray.put(taxJson);
		}

		taxes.put(JSON_PROP_TAX, taxArray);
		return taxes;
	}

	private static JSONArray getGroundService(Element transferCompElem, String dynamicPkgAction) {
		JSONArray groundServiceArray = new JSONArray();
		Element[] groundServiceElemArray = XMLUtils.getElementsAtXPath(transferCompElem, "./pac:GroundService");

		for (Element groundServiceElem : groundServiceElemArray) {
			JSONObject groundServiceJson = new JSONObject();

			String name = String.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./@Name"));
			if (name != null && name.length() > 0)
				groundServiceJson.put("name", name);

			String description = String.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./@Description"));
			if (description != null && description.length() > 0)
				groundServiceJson.put("description", description);

			Element locationElem = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./pac:Location");
			if (locationElem != null && (locationElem.hasChildNodes() || locationElem.hasAttributes())) {
				JSONObject locationJson = getLoction(locationElem, dynamicPkgAction);
				groundServiceJson.put(JSON_PROP_LOCATION, locationJson);
			}

			Element[] totalChargeElements = XMLUtils.getElementsAtXPath(groundServiceElem, "./pac:TotalCharge");
			if (totalChargeElements != null && totalChargeElements.length > 0) {
				JSONArray totlChargeArray = new JSONArray();

				for (Element totalChargeElement : totalChargeElements) {
					JSONObject totalChargeJson = getTotalCharge(totalChargeElement);

					totlChargeArray.put(totalChargeJson);
				}
				groundServiceJson.put(JSON_PROP_TOTALCHARGE, totlChargeArray);
			}

			JSONObject timeSpanJson = getTimeSpan(groundServiceElem);
			groundServiceJson.put(JSON_PROP_TIMESPAN, timeSpanJson);

			String airInclusiveBooking = String
					.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./pac:AirInclusiveBooking"));
			String declineRequired = String
					.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./pac:DeclineRequired"));
			String withExtraNights = String
					.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./pac:WithExtraNights"));
			String purchasable = String.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./pac:Purchasable"));
			String isIncludedInTour = String
					.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./pac:isIncludedInTour"));
			String flightInfoRequired = String
					.valueOf(XMLUtils.getValueAtXPath(groundServiceElem, "./pac:FlightInfoRequired"));

			// PerServicePricing

			JSONArray perServicePricingJson = getPerServicePricing(groundServiceElem);

			groundServiceJson.put(JSON_PROP_AIRINCLUSIVEBOOKING, airInclusiveBooking);
			groundServiceJson.put("declineRequired", declineRequired);
			groundServiceJson.put(JSON_PROP_WITHEXTRANIGHTS, withExtraNights);
			groundServiceJson.put(JSON_PROP_PURCHASABLE, purchasable);
			groundServiceJson.put(JSON_PROP_ISINCLUDEDINTOUR, isIncludedInTour);
			groundServiceJson.put(JSON_PROP_FLIGHTINFOREQUIRED, flightInfoRequired);
			groundServiceJson.put(JSON_PROP_PERSERVICEPRICING, perServicePricingJson);
			groundServiceArray.put(groundServiceJson);
		}
		return groundServiceArray;
	}

	private static JSONArray getPerServicePricing(Element groundServiceElem) {

		Element[] PerServicePricingElems = XMLUtils.getElementsAtXPath(groundServiceElem, "./pac:PerServicePricing");
		JSONArray perServicePricingArray = new JSONArray();

		for (Element PerServicePricingElem : PerServicePricingElems) {
			JSONObject perServicePricingJson = new JSONObject();
			String maxPassengersInParty = String
					.valueOf(XMLUtils.getValueAtXPath(PerServicePricingElem, "./@MaxPassengersInParty"));
			String minPassengersInParty = String
					.valueOf(XMLUtils.getValueAtXPath(PerServicePricingElem, "./@MinPassengersInParty"));
			String price = String.valueOf(XMLUtils.getValueAtXPath(PerServicePricingElem, "./@Price"));
			perServicePricingJson.put(JSON_PROP_MAXPASSENGERSINPARTY, maxPassengersInParty);
			perServicePricingJson.put(JSON_PROP_MINPASSENGERSINPARTY, minPassengersInParty);
			perServicePricingJson.put(JSON_PROP_PRICE, price);
			perServicePricingArray.put(perServicePricingJson);

		}

		return perServicePricingArray;
	}

	private static JSONObject getTimeSpan(Element groundServiceElem) {
		JSONObject timeSpanJson = new JSONObject();

		Element timeSpanElem = XMLUtils.getFirstElementAtXPath(groundServiceElem, "./pac:TimeSpan");

		String end = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElem, "./@End"));

		String start = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElem, "./@Start"));

		timeSpanJson.put(JSON_PROP_END, end);
		timeSpanJson.put(JSON_PROP_START, start);
		return timeSpanJson;

	}

	private static JSONObject getTotalCharge(Element totalChargeElement) {
		JSONObject totalChargeJson = new JSONObject();

		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(totalChargeElement, "./@CurrencyCode"));

		String rateTotalAmount = String.valueOf(XMLUtils.getValueAtXPath(totalChargeElement, "./@RateTotalAmount"));

		String type = String.valueOf(XMLUtils.getValueAtXPath(totalChargeElement, "./@Type"));

		totalChargeJson.put(JSON_PROP_CURRENCYCODE, currencyCode);
		totalChargeJson.put(JSON_PROP_RATETOTALAMOUNT, rateTotalAmount);
		totalChargeJson.put("type", type);

		return totalChargeJson;
	}

	private static JSONObject getLoction(Element locationElem, String dynamicPkgAction) {

		JSONObject locationJson = new JSONObject();
		Element PickupElem = XMLUtils.getFirstElementAtXPath(locationElem, "./ns:Pickup");

		Element airportInfoElem = XMLUtils.getFirstElementAtXPath(PickupElem, "./ns:AirportInfo");

		Element departArrivElem = null;
		if (dynamicPkgAction.equalsIgnoreCase("PackageDepartureTransfer")) {

			departArrivElem = XMLUtils.getFirstElementAtXPath(airportInfoElem, "./ns:Departure");
		}

		if (dynamicPkgAction.equalsIgnoreCase("PackageArrivalTransfer")) {

			departArrivElem = XMLUtils.getFirstElementAtXPath(airportInfoElem, "./ns:Arrival");
		}

		String airportName = String.valueOf(XMLUtils.getValueAtXPath(departArrivElem, "./@AirportName"));
		locationJson.put(JSON_PROP_AIRPORTNAME, airportName);

		String codeContext = String.valueOf(XMLUtils.getValueAtXPath(departArrivElem, "./@CodeContext"));
		locationJson.put(JSON_PROP_CODECONTEXT, codeContext);

		String pickUpLocation = String.valueOf(XMLUtils.getValueAtXPath(departArrivElem, "./@LocationCode"));
		locationJson.put("pickUpLocation", pickUpLocation);

		return locationJson;
	}

	private static JSONObject getAitItineryComponentJSON(Element airItineryElem) {

		JSONObject airItineraryObj = new JSONObject();
		Element originDestOptionsElem = XMLUtils.getFirstElementAtXPath(airItineryElem,
				"./ns:OriginDestinationOptions");

		Element originDestOptionElem = XMLUtils.getFirstElementAtXPath(originDestOptionsElem,
				"./ns:OriginDestinationOption");

		airItineraryObj = getFlightSegment(originDestOptionElem);

		return airItineraryObj;
	}

	private static JSONObject getFlightSegment(Element originDestOptionElem) {
		JSONObject flightSegJson = new JSONObject();
		Element flightSegElem = XMLUtils.getFirstElementAtXPath(originDestOptionElem, "./ns:FlightSegment");

		JSONObject departureAirportJson = new JSONObject();
		Element departureAirportElem = XMLUtils.getFirstElementAtXPath(flightSegElem, "./ns:DepartureAirport");

		String codeContext = String.valueOf(XMLUtils.getValueAtXPath(departureAirportElem, "./@CodeContext"));
		departureAirportJson.put(JSON_PROP_CODECONTEXT, codeContext);

		String locationCode = String.valueOf(XMLUtils.getValueAtXPath(departureAirportElem, "./@LocationCode"));
		departureAirportJson.put(JSON_PROP_LOCATIONCODE, locationCode);

		JSONObject arrivalAirportJson = new JSONObject();
		Element arrivalAirportElem = XMLUtils.getFirstElementAtXPath(flightSegElem, "./ns:ArrivalAirport");

		String ArrivelocationCode = String.valueOf(XMLUtils.getValueAtXPath(arrivalAirportElem, "./@LocationCode"));
		arrivalAirportJson.put(JSON_PROP_LOCATIONCODE, ArrivelocationCode);

		JSONArray bookingArray = new JSONArray();
		Element[] bookingClassAvailsArray = XMLUtils.getElementsAtXPath(flightSegElem, "./ns:BookingClassAvails");

		for (Element bookingClassAvailsElem : bookingClassAvailsArray) {
			JSONObject bookingAvailJson = getBookingAvailsJson(bookingClassAvailsElem);
			bookingArray.put(bookingAvailJson);

		}

		flightSegJson.put(JSON_PROP_DEPARTUREAIRPORT, departureAirportJson);
		flightSegJson.put(JSON_PROP_ARRIVALAIRPORT, arrivalAirportJson);
		flightSegJson.put(JSON_PROP_BOOKINGCLASSAVAILS, bookingArray);
		return flightSegJson;
	}

	private static JSONObject getBookingAvailsJson(Element bookingClassAvailsElem) {

		JSONObject cabinTypeJson = new JSONObject();
		String cabinType = String.valueOf(XMLUtils.getValueAtXPath(bookingClassAvailsElem, "./@CabinType"));
		cabinTypeJson.put(JSON_PROP_CABINTYPE, cabinType);

		return cabinTypeJson;
	}

	private static JSONObject getHotelComponentJSON(Element hotelCompElem) {
		JSONObject hotelComponentJson = new JSONObject();
		String dynamicPkgAction = String.valueOf(XMLUtils.getValueAtXPath(hotelCompElem, "./@DynamicPkgAction"));

		hotelComponentJson.put(JSON_PROP_DYNAMICPKGACTION, dynamicPkgAction);

		JSONArray roomStayArr = new JSONArray();

		Element[] roomStayArray = XMLUtils.getElementsAtXPath(hotelCompElem, "./ns:RoomStays/ns:RoomStay");
		for (Element roomStayElem : roomStayArray) {

			JSONObject roomStay = getRoomStayJSON(roomStayElem);

			roomStayArr.put(roomStay);

		}

		hotelComponentJson.put(JSON_PROP_ROOMSTAY, roomStayArr);

		return hotelComponentJson;
	}

	private static JSONObject getRoomStayJSON(Element roomStayElem) {
		JSONObject roomStayJson = new JSONObject();

		String roomStayStatus = String.valueOf(XMLUtils.getValueAtXPath(roomStayElem, "./@RoomStayStatus"));
		roomStayJson.put(JSON_PROP_ROOMSTAYSTATUS, roomStayStatus);

		Element roomTypeElem = XMLUtils.getFirstElementAtXPath(roomStayElem, "./ns:RoomTypes/ns:RoomType");

		String roomTypeStr = String.valueOf(XMLUtils.getValueAtXPath(roomTypeElem, "./@RoomType"));
		String roomCategory = String.valueOf(XMLUtils.getValueAtXPath(roomTypeElem, "./@RoomCategory"));

		roomStayJson.put(JSON_PROP_ROOMTYPE, roomTypeStr);
		roomStayJson.put("roomCategory", roomCategory);

		// For Occupancy Element
		Element[] occupancyElements = XMLUtils.getElementsAtXPath(roomStayElem,
				"./ns:RoomTypes/ns:RoomType/ns:Occupancy");

		if (occupancyElements != null && occupancyElements.length != 0) {
			JSONArray occupancyObjects = new JSONArray();

			for (Element occupancyElement : occupancyElements) {
				JSONObject occupancyObject = new JSONObject();

				String minAge = String.valueOf(XMLUtils.getValueAtXPath(occupancyElement, "./@MinAge"));
				occupancyObject.put("minAge", minAge);

				String maxAge = String.valueOf(XMLUtils.getValueAtXPath(occupancyElement, "./@MaxAge"));
				occupancyObject.put("maxAge", maxAge);

				String maxOccupancy = String.valueOf(XMLUtils.getValueAtXPath(occupancyElement, "./@MaxOccupancye"));
				occupancyObject.put("maxOccupancy", maxOccupancy);

				String minOccupancy = String.valueOf(XMLUtils.getValueAtXPath(occupancyElement, "./@MinOccupancy"));
				occupancyObject.put("minOccupancy", minOccupancy);

				String ageQualifyingCode = String
						.valueOf(XMLUtils.getValueAtXPath(occupancyElement, "./@AgeQualifyingCode"));
				occupancyObject.put("ageQualifyingCode", ageQualifyingCode);

				occupancyObjects.put(occupancyObject);
			}

			roomStayJson.put("occupancy", occupancyObjects);
		}

		// For Room Rules Element
		Element roomRulesElement = XMLUtils.getFirstElementAtXPath(roomTypeElem,
				"./ns:TPA_Extensions/tns:Pkgs_TPA/tns:RoomRules");

		if (roomRulesElement != null && (roomRulesElement.hasChildNodes() || roomRulesElement.hasAttributes())) {
			JSONObject roomRules = new JSONObject();

			String minimumPayingPassengers = String
					.valueOf(XMLUtils.getValueAtXPath(roomRulesElement, "./@MinimumPayingPassengers"));

			roomRules.put("minimumPayingPassengers", minimumPayingPassengers);

			roomStayJson.put("roomRules", roomRules);
		}

		Element[] roomRateElems = XMLUtils.getElementsAtXPath(roomStayElem, "./ns:RoomRates/ns:RoomRate");
		

		JSONArray roomRateArray = new JSONArray();

		for (Element roomRateElem : roomRateElems) {
			JSONObject roomRateJson = new JSONObject();
			JSONObject total = new JSONObject();
			JSONObject totalRoomRate = getTotalRoomRateJSON(roomRateElem, total);
			String ratePlanCategory = String.valueOf(XMLUtils.getValueAtXPath(roomRateElem, "./@RatePlanCategory"));
			roomRateJson.put("ratePlanCategory", ratePlanCategory);
			roomRateJson.put(JSON_PROP_TOTAL, totalRoomRate);
			roomRateArray.put(roomRateJson);
		}

		roomStayJson.put(JSON_PROP_ROOMRATE, roomRateArray);

		// for GuestCount started
		Element guestCountsElem = XMLUtils.getFirstElementAtXPath(roomStayElem, "./ns:GuestCounts");

		Element guestCountElem = XMLUtils.getFirstElementAtXPath(guestCountsElem, "./ns:GuestCount");
		JSONObject guestCountJson = new JSONObject();

		String count = String.valueOf(XMLUtils.getValueAtXPath(guestCountElem, "./@Count"));
		guestCountJson.put(JSON_PROP_COUNT, count);

		roomStayJson.put(JSON_PROP_GUESTCOUNTS, guestCountJson);

		// for GuestCount Ended

		// For TimeSpan Element
		Element timeSpanElement = XMLUtils.getFirstElementAtXPath(roomStayElem, "./ns:TimeSpan");

		if (timeSpanElement != null && (timeSpanElement.hasChildNodes() || timeSpanElement.hasAttributes())) {
			JSONObject timeSpan = new JSONObject();

			String start = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElement, "./@Start"));
			timeSpan.put("start", start);

			String duration = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElement, "./@Duration"));
			timeSpan.put("duration", duration);

			String end = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElement, "./@End"));
			timeSpan.put("end", end);

			roomStayJson.put("timeSpan", timeSpan);
		}

		// for BookingRules started

		Element bookingRulesElem = XMLUtils.getFirstElementAtXPath(roomStayElem, "./ns:BookingRules");

		if (bookingRulesElem != null && (bookingRulesElem.hasChildNodes() || bookingRulesElem.hasAttributes())) {
			JSONArray bookingRulesJson = new JSONArray();

			Element[] bookingRuleElems = XMLUtils.getElementsAtXPath(bookingRulesElem, "./ns:BookingRule");

			for (Element bookingRuleElem : bookingRuleElems) {
				JSONObject bookingRuleJson = new JSONObject();

				Element[] lengthsOfStayArray = XMLUtils.getElementsAtXPath(bookingRuleElem,
						"./ns:LengthsOfStay/ns:LengthOfStay");

				JSONArray lengthStayArr = new JSONArray();

				for (Element lengthOfStayElem : lengthsOfStayArray) {

					JSONObject lengthStayJson = getLengthOfStayElemJSON(lengthOfStayElem);

					lengthStayArr.put(lengthStayJson);

				}

				bookingRuleJson.put(JSON_PROP_LENGTHSOFSTAY, lengthStayArr);
				bookingRulesJson.put(bookingRuleJson);
			}

			roomStayJson.put("bookingRules", bookingRulesJson);
		}
		// for BookingRules Ended

		// For BasicPropertyInfo Element
		JSONArray basicPropertyInfoJson = new JSONArray();
		Element[] basicPropertyInfoElements = XMLUtils.getElementsAtXPath(roomStayElem, "./ns:BasicPropertyInfo");
		
		for (Element basicPropertyInfoElement : basicPropertyInfoElements) {
			if (basicPropertyInfoElement != null
					&& (basicPropertyInfoElement.hasChildNodes() || basicPropertyInfoElement.hasAttributes())) {
				JSONObject basicPropertyInfo = new JSONObject();

				String hotelCode = String.valueOf(XMLUtils.getValueAtXPath(basicPropertyInfoElement, "./@HotelCode"));
				basicPropertyInfo.put("hotelCode", hotelCode);

				String hotelName = String.valueOf(XMLUtils.getValueAtXPath(basicPropertyInfoElement, "./@HotelName"));
				basicPropertyInfo.put("hotelName", hotelName);

				Element addressElement = XMLUtils.getFirstElementAtXPath(basicPropertyInfoElement, "./ns:Address");
				if (addressElement != null && (addressElement.hasChildNodes() || addressElement.hasAttributes())) {
					JSONObject address = new JSONObject();

					Element[] addressLinesElement = XMLUtils.getElementsAtXPath(addressElement, "./AddressLine");
					if (addressLinesElement != null && addressLinesElement.length > 0) {
						JSONArray addressLines = new JSONArray();
						for (Element addressLineElement : addressLinesElement) {
							addressLineElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:AddressLine");
							String addressLineStr = XMLUtils.getElementValue(addressLineElement);
							addressLines.put(addressLineStr);
						}

						address.put("addressLine", addressLines);
					}

					Element cityNameElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:CityName");
					if (cityNameElement != null) {
						String cityName = XMLUtils.getElementValue(cityNameElement);
						address.put("cityName", cityName);
					}

					Element postalCodeElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:PostalCode");
					if (postalCodeElement != null) {
						String postalCode = XMLUtils.getElementValue(postalCodeElement);
						address.put("postalCode", postalCode);
					}

					Element countyElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:County");
					if (countyElement != null) {
						String county = XMLUtils.getElementValue(countyElement);
						address.put("county", county);
					}

					Element stateProvElement = XMLUtils.getFirstElementAtXPath(addressElement, "./ns:StateProv");
					if (stateProvElement != null) {
						String stateProv = XMLUtils.getElementValue(stateProvElement);
						address.put("stateProv", stateProv);
					}

					basicPropertyInfo.put("address", address);
					
				}

				basicPropertyInfoJson.put(basicPropertyInfo);
			}
		}
		roomStayJson.put("basicPropertyInfo", basicPropertyInfoJson);
		

		return roomStayJson;
	}

	private static JSONObject getLengthOfStayElemJSON(Element lengthOfStayElem) {
		JSONObject lengthOfStayJson = new JSONObject();
		// Element bookingRuleElem = XMLUtils.getFirstElementAtXPath(lengthOfStayElem,
		// "./ns:LengthOfStay");

		String minMaxMessageType = String.valueOf(XMLUtils.getValueAtXPath(lengthOfStayElem, "./@MinMaxMessageType"));
		lengthOfStayJson.put(JSON_PROP_MINMAXMESSAGETYPE, minMaxMessageType);

		String time = String.valueOf(XMLUtils.getValueAtXPath(lengthOfStayElem, "./@Time"));
		lengthOfStayJson.put(JSON_PROP_TIME, time);

		String timeUnit = String.valueOf(XMLUtils.getValueAtXPath(lengthOfStayElem, "./@TimeUnit"));
		lengthOfStayJson.put(JSON_PROP_TIMEUNIT, timeUnit);

		return lengthOfStayJson;
	}

	private static JSONObject getTotalRoomRateJSON(Element roomRateElem, JSONObject total) {
		Element totalElem = XMLUtils.getFirstElementAtXPath(roomRateElem, "./ns:Total");

		String type = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@Type"));
		total.put(JSON_PROP_TYPE, type);

		String amountAfterTax = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@AmountAfterTax"));
		total.put(JSON_PROP_AMOUNTAFTERTAX, amountAfterTax);

		String amountBeforeTax = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@AmountBeforeTax"));
		total.put(JSON_PROP_AMOUNTBEFORETAX, amountBeforeTax);

		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(totalElem, "./@CurrencyCode"));
		total.put(JSON_PROP_CURRENCYCODE, currencyCode);

		

		JSONArray taxArr = new JSONArray();

		Element taxesElem = XMLUtils.getFirstElementAtXPath(totalElem, "./ns:Taxes");

		JSONObject taxesJson = new JSONObject();

		Element[] taxArray = XMLUtils.getElementsAtXPath(taxesElem, "./ns:Tax");
		for (Element taxElem : taxArray) {

			JSONObject tax = getTaxJson(taxElem);
			taxArr.put(tax);

		}
		taxesJson.put(JSON_PROP_TAX, taxArr);
		total.put(JSON_PROP_TAXES, taxesJson);

		Element tPA_ExtensionsElem = XMLUtils.getFirstElementAtXPath(roomRateElem, "./ns:TPA_Extensions");

		Element pkgs_TPAElem = XMLUtils.getFirstElementAtXPath(tPA_ExtensionsElem, "./tns:Pkgs_TPA");

		Boolean isIncludedInTour = Boolean.valueOf(XMLUtils.getValueAtXPath(pkgs_TPAElem, "./@isIncludedInTour"));

		total.put(JSON_PROP_ISINCLUDEDINTOUR, isIncludedInTour);

		// Last MinuteDiscount Element
		Element[] lastMinuteDiscountsElement = XMLUtils.getElementsAtXPath(totalElem,
				"./ns:TPA_Extensions/tns:LastMinuteDiscount/tns:LastMinuteDiscounts");

		if (lastMinuteDiscountsElement != null && lastMinuteDiscountsElement.length > 0) {
			JSONArray lastMinuteDiscounts = new JSONArray();
			for (Element lastMinuteDiscountElement : lastMinuteDiscountsElement) {
				JSONObject lastMinuteDiscount = new JSONObject();

				String amountBeforeTaxLMD = String
						.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@AmountBeforeTax"));
				lastMinuteDiscount.put("amountBeforeTax", amountBeforeTaxLMD);

				String amountAfterTaxLMD = String
						.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@AmountAfterTax"));
				lastMinuteDiscount.put("amountAfterTax", amountAfterTaxLMD);

				String CurrencyCodeLMD = String
						.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@CurrencyCode"));
				lastMinuteDiscount.put("currencyCode", CurrencyCodeLMD);

				String typeLMD = String.valueOf(XMLUtils.getValueAtXPath(lastMinuteDiscountElement, "./@Type"));
				lastMinuteDiscount.put("type", typeLMD);

				lastMinuteDiscounts.put(lastMinuteDiscount);
			}

			total.put("lastMinuteDiscounts", lastMinuteDiscounts);
		}

		return total;

	}

	private static JSONObject getTaxJson(Element taxElem) {

		JSONObject tax = new JSONObject();
		String amount = String.valueOf(XMLUtils.getValueAtXPath(taxElem, "./@Amount"));
		tax.put(JSON_PROP_AMOUNT, amount);

		String currencyCode = String.valueOf(XMLUtils.getValueAtXPath(taxElem, "./@CurrencyCode"));
		tax.put(JSON_PROP_CURRENCYCODE, currencyCode);

		Element taxDescriptionElem = XMLUtils.getFirstElementAtXPath(taxElem, "./ns:TaxDescription");

		String name = String.valueOf(XMLUtils.getValueAtXPath(taxDescriptionElem, "./@Name"));

		tax.put(JSON_PROP_TAXDESCRIPTION, name);

		return tax;
	}

}
