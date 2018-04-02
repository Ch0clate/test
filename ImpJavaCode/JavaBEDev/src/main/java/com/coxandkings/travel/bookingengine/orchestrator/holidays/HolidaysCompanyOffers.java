package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.CommercialTypeConfig;
import com.coxandkings.travel.bookingengine.config.OffersConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo;
import com.coxandkings.travel.bookingengine.userctx.OrgHierarchy;
import com.coxandkings.travel.bookingengine.userctx.UserContext;

public class HolidaysCompanyOffers implements HolidayConstants{

	private static final Logger logger = LogManager.getLogger(HolidaysCompanyOffers.class);
	private static String travelDateFrom ="";
	private static String travelDateTo ="";
	private static Boolean childWithBed = false;
	
	public static void getCompanyOffers(JSONObject reqJson, JSONObject resJson, OffersConfig.Type invocationType) {
		
		 JSONObject reqHdrJson = reqJson.getJSONObject(JSON_PROP_REQHEADER);
	     JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
	     JSONObject clientCtxJson = reqHdrJson.getJSONObject(JSON_PROP_CLIENTCONTEXT);

	     JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY);
	     JSONArray dynamicPackageArr = resBodyJson.getJSONArray(JSON_PROP_DYNAMICPACKAGE);
		
		 OffersConfig offConfig = HolidaysConfig.getOffersConfig();
		 CommercialTypeConfig offTypeConfig = offConfig.getOfferTypeConfig(invocationType);
		 
		 JSONObject breCpnyOffReqJson = new JSONObject(new JSONTokener(offTypeConfig.getRequestJSONShell()));
		 JSONArray briJsonArr = breCpnyOffReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert").getJSONObject("object").getJSONObject("cnk.holidays_companyoffers.withoutredemption.Root").getJSONArray("businessRuleIntake");
		 JSONObject briJson = new JSONObject();
		 
		 UserContext usrCtx = UserContext.getUserContextForSession(reqHdrJson);
			List<ClientInfo> clientCommHierarchyList = usrCtx.getClientHierarchy();
			String clientGroup = "";
			if (clientCommHierarchyList != null && clientCommHierarchyList.size() > 0) {
				ClientInfo clInfo = clientCommHierarchyList.get(clientCommHierarchyList.size() - 1);
				if (clInfo.getCommercialsEntityType() == ClientInfo.CommercialsEntityType.ClientGroup) {
					clientGroup = clInfo.getCommercialsEntityId();
				}
			}
			
			JSONObject clientDtlsJson = new JSONObject();
			clientDtlsJson.put(JSON_PROP_CLIENTNAME, usrCtx.getClientName());
			clientDtlsJson.put(JSON_PROP_CLIENTCAT, usrCtx.getClientCategory());
			clientDtlsJson.put(JSON_PROP_CLIENTSUBCAT, usrCtx.getClientSubCategory());
			clientDtlsJson.put(JSON_PROP_CLIENTTYPE, clientCtxJson.getString(JSON_PROP_CLIENTTYPE));
			clientDtlsJson.put(JSON_PROP_POS, clientCtxJson.optString(JSON_PROP_POS, ""));
			clientDtlsJson.put(JSON_PROP_CLIENTGROUP, clientGroup);
			// TODO: Check if this is correct
			clientDtlsJson.put(JSON_PROP_NATIONALITY, clientCtxJson.getString(JSON_PROP_CLIENTMARKET));
			briJson.put(JSON_PROP_CLIENTDETAILS, clientDtlsJson);	
		
			OrgHierarchy orgHier = usrCtx.getOrganizationHierarchy();
			JSONObject cpnyDtlsJson = new JSONObject();
			cpnyDtlsJson.put(JSON_PROP_SBU, orgHier.getSBU());
			cpnyDtlsJson.put(JSON_PROP_BU, orgHier.getBU());
			cpnyDtlsJson.put(JSON_PROP_DIVISION, orgHier.getDivision());
			cpnyDtlsJson.put(JSON_PROP_SALESOFFICELOC, orgHier.getSalesOfficeLoc());
			cpnyDtlsJson.put(JSON_PROP_SALESOFFICE, orgHier.getSalesOfficeName());
			cpnyDtlsJson.put(JSON_PROP_COMPANYMKT, clientCtxJson.getString(JSON_PROP_CLIENTMARKET));
			briJson.put(JSON_PROP_COMPANYDETAILS, cpnyDtlsJson);
		
			JSONObject commonElemsJson = new JSONObject();
			commonElemsJson.put(JSON_PROP_BOOKINGDATE, DATE_FORMAT.format(new Date()));
			// Following is discussed and confirmed with Offers team. The travelDate is the 
			
			for (int i = 0; i < dynamicPackageArr.length(); i++) {

				if (!dynamicPackageArr.getJSONObject(i).has("error")) {
					JSONObject dynamicPkgJson = dynamicPackageArr.getJSONObject(i);
					if (dynamicPkgJson.optJSONObject(JSON_PROP_GLOBALINFO).has(JSON_PROP_TIMESPAN)) {
						travelDateFrom = dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO).getJSONObject(JSON_PROP_TIMESPAN)
								.getString(JSON_PROP_TRAVELSTARTDATE);
						travelDateTo = dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO).getJSONObject(JSON_PROP_TIMESPAN)
								.getString(JSON_PROP_TRAVELENDDATE);
						
				}}}
			    convertDate(commonElemsJson, travelDateFrom,JSON_PROP_TRAVELDATEFROM);
			    
			    convertDate(commonElemsJson, travelDateTo,JSON_PROP_TRAVELDATETO);
			
			// TODO: Populate Target Set (Slabs) .Need to find out from where values will come
			JSONArray targetSetArray = new JSONArray();
			JSONObject targetSetJsonPass = new JSONObject();
			JSONObject targetSetJsonRoom = new JSONObject();
			
			targetSetJsonPass.put(JSON_PROP_TYPE, "Number of Passengers");
			targetSetJsonPass.put(JSON_PROP_VALUE, 496);
			targetSetJsonRoom.put(JSON_PROP_TYPE, "no of rooms");
			targetSetJsonRoom.put(JSON_PROP_VALUE, 8);
			
			targetSetArray.put(targetSetJsonPass);
			targetSetArray.put(targetSetJsonRoom);
			commonElemsJson.put("targetSet",targetSetArray);
			
			briJson.put(JSON_PROP_COMMONELEMS, commonElemsJson);
		
			JSONArray prodDtlsJsonArr = new JSONArray();
			for (int i = 0; i < dynamicPackageArr.length(); i++) {

				JSONObject dynamicPkgJson = dynamicPackageArr.getJSONObject(i);
				JSONObject prodDtlsJson = new JSONObject();
				prodDtlsJson.put(JSON_PROP_PRODCATEG, PROD_CATEG_HOLIDAYS);
				prodDtlsJson.put(JSON_PROP_PRODCATEGSUBTYPE, PROD_CATEG_HOLIDAYS);
				
				//TODO : packageType,flavourType and productFlavourName is hardcoded below.Need to find out from where values will come.
				//WEM is supposed to pass the flag to identify online or offline(custom) package for packageType.
				prodDtlsJson.put("packageType","custom");
				prodDtlsJson.put("flavourType", "");
				prodDtlsJson.put("productFlavourName","");
				
				String reqBrandName = reqBodyJson.getString("brandName");
				String reqDestination = reqBodyJson.getString("destinationLocation");
					
				/*if (dynamicPkgJson.has(JSON_PROP_GLOBALINFO)) {
				JSONObject dynamicpackageId = dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO)
						.getJSONObject(JSON_PROP_DYNAMICPKGID);
				String brandName = dynamicpackageId.get(JSON_PROP_COMPANYNAME).toString();
				}*/
				
				prodDtlsJson.put("brand", reqBrandName);
				prodDtlsJson.put("destination", reqDestination);
				//TODO : country is hardcoded to India.Need to check on this.
				prodDtlsJson.put(JSON_PROP_COUNTRY, "India");
				
				String productName = "Amazing India";
				// TODO: ProductName is set to "Amazing India".Below code was for setting
				// tourName as productName from SI response

				/*
				 * if (dynamicpackageId.has(JSON_PROP_TPA_EXTENSIONS)) { productName =
				 * dynamicpackageId.getJSONObject(JSON_PROP_TPA_EXTENSIONS).getJSONObject(
				 * JSON_PROP_PKGS_TPA)
				 * .getJSONObject(JSON_PROP_TOURDETAILS).get(JSON_PROP_TOURNAME).toString(); }
				 */

				prodDtlsJson.put(JSON_PROP_PRODUCTNAME, productName);
				//TODO : Search calculate price total fare to be set below
				BigDecimal totalFare = dynamicPkgJson.getJSONObject(JSON_PROP_GLOBALINFO).getJSONObject(JSON_PROP_TOTAL).getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
				prodDtlsJson.put(JSON_PROP_TOTALFARE,totalFare);
				
			JSONArray hotelDetailsArray = new JSONArray();
			JSONObject hotelDetailsJson = new JSONObject();
			JSONArray roomDetailsArr = new JSONArray();
			
			if (dynamicPkgJson.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_HOTEL_COMPONENT)) {

				JSONObject hotelCompJson = dynamicPkgJson.getJSONObject(JSON_PROP_COMPONENTS)
						.getJSONObject(JSON_PROP_HOTEL_COMPONENT);

				if (hotelCompJson != null && hotelCompJson.length() > 0) {
					String dynamicPkgAction = hotelCompJson.getString(JSON_PROP_DYNAMICPKGACTION);
					if (dynamicPkgAction.equalsIgnoreCase(DYNAMICPKGACTION_HOTEL_TOUR)) {
						JSONArray roomStayArr = hotelCompJson.getJSONArray(JSON_PROP_ROOMSTAY);

						for (int j = 0; j < roomStayArr.length(); j++) {
							JSONObject roomDetails = new JSONObject();
							JSONObject roomStayJson = roomStayArr.getJSONObject(j);
							JSONArray roomrateArr = roomStayJson.getJSONArray(JSON_PROP_ROOMRATE);
							String roomType = roomStayJson.get(JSON_PROP_ROOMTYPE).toString();
							roomDetails.put(JSON_PROP_ROOMTYPE, roomType);
							for (int k = 0; k < roomrateArr.length(); k++) {
								JSONObject roomRateJson = roomrateArr.getJSONObject(k);
								String roomCategory = roomRateJson.optString("ratePlanCategory");
								roomDetails.put(JSON_PROP_ROOMCATEGORY, roomCategory);
							}
						
							roomDetails.put(JSON_PROP_PASSENGERDETAILS, getOffersPassengerDetailsJSON(roomStayJson));

							roomDetailsArr.put(roomDetails);
						}
					}
				}
			}
			
			if (dynamicPkgJson.getJSONObject(JSON_PROP_COMPONENTS).has(JSON_PROP_CRUISE_COMPONENT)) {
				JSONObject cruiseCompJson = dynamicPkgJson.getJSONObject(JSON_PROP_COMPONENTS)
						.getJSONObject(JSON_PROP_CRUISE_COMPONENT);

				if(cruiseCompJson!=null && cruiseCompJson.length()>0) {
					String dynamicPkgAction = cruiseCompJson.getString(JSON_PROP_DYNAMICPKGACTION);
					if (dynamicPkgAction.equalsIgnoreCase(DYNAMICPKGACTION_CRUISE_TOUR)) {
						JSONArray categoryOptionsArr = cruiseCompJson.getJSONArray(JSON_PROP_CATEGORYOPTIONS);
						for (int k = 0; k < categoryOptionsArr.length(); k++) {
							JSONObject categoryOptionsJson = categoryOptionsArr.getJSONObject(k);
							JSONArray categoryOptionArr = categoryOptionsJson.getJSONArray(JSON_PROP_CATEGORYOPTION);

							for (int j = 0; j < categoryOptionArr.length(); j++) {
								JSONObject categoryOptionJson = categoryOptionArr.getJSONObject(j);
								JSONObject roomDetails = new JSONObject();
								String type = categoryOptionJson.getString(JSON_PROP_TYPE);
								roomDetails.put(JSON_PROP_ROOMTYPE, type);
								roomDetails.put(JSON_PROP_ROOMCATEGORY, "");
								roomDetails.put(JSON_PROP_PASSENGERDETAILS, getOffersCruisePassengerDetailsJSON(categoryOptionJson));
								roomDetailsArr.put(roomDetails);
							}
						}
					}
				}
			}
			hotelDetailsJson.put(JSON_PROP_ROOMDETAILS, roomDetailsArr);
			hotelDetailsArray.put(hotelDetailsJson);
			prodDtlsJson.put("hotelDetails", hotelDetailsArray);
			prodDtlsJsonArr.put(prodDtlsJson);
		}

			briJson.put(JSON_PROP_PRODUCTDETAILS, prodDtlsJsonArr);
			
			JSONObject paymentDetailsJson = new JSONObject();
			paymentDetailsJson.put("bankName", "");
			paymentDetailsJson.put("modeOfPayment", "");
			paymentDetailsJson.put("paymentType", "");
			
			JSONObject cardDetails = new JSONObject();
			cardDetails.put("cardNo","");
			cardDetails.put("cardType","");
			cardDetails.put("nthBooking","");
			paymentDetailsJson.put("cardDetails", cardDetails);
			
			briJson.put("paymentDetails", paymentDetailsJson);
			
			briJsonArr.put(briJson);
		
			JSONObject breOffResJson = null;
	        try {
	            breOffResJson = HTTPServiceConsumer.consumeJSONService("BRMS/ComOffers", offTypeConfig.getServiceURL(), offConfig.getHttpHeaders(), breCpnyOffReqJson);
	        }
	        catch (Exception x) {
	            logger.warn("An exception occurred when calling company offers", x);
	        }

	        if (BRMS_STATUS_TYPE_FAILURE.equals(breOffResJson.getString(JSON_PROP_TYPE))) {
	        	logger.warn(String.format("A failure response was received from Company Offers calculation engine: %s", breOffResJson.toString()));
	        	return;
	        }
	        System.out.println("Offer request: "+breCpnyOffReqJson.toString());
		System.out.println("Offer response : "+breOffResJson.toString());
		
		
	}

	private static JSONArray getOffersCruisePassengerDetailsJSON(JSONObject categoryOptionJson) {
		JSONArray passengerDetailsArr = new JSONArray();
		JSONObject paxDetail = new JSONObject();
		
		JSONArray totalArr = categoryOptionJson.getJSONArray(JSON_PROP_TOTAL);

		for (int i = 0; i < totalArr.length(); i++) {
			JSONObject totalJson = totalArr.getJSONObject(i);
			
			String type = totalJson.get(JSON_PROP_TYPE).toString();
			if (type.equals("NA")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "");
			}else {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, type);
			}
			//TODO : We do not get "childWithBed" indicator from supplier.Need to check when will this be true.
			paxDetail.put("childWithBed", childWithBed);
			//TODO : age and gender is blank as we do not get these details in Holidays Search
			paxDetail.put("age", "");
			paxDetail.put("gender", "");
			BigDecimal totalFare = totalJson.getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
			paxDetail.put(JSON_PROP_TOTALFARE, totalFare);
			paxDetail.put(JSON_PROP_FAREDETAILS,getFareDetailsJsonArray(totalJson));
			passengerDetailsArr.put(paxDetail);
		}
		
		return passengerDetailsArr;
	}

	private static JSONArray getOffersPassengerDetailsJSON(JSONObject roomStayJson) {
		JSONArray passengerDetailsArr = new JSONArray();
		JSONArray roomrateArr = roomStayJson.getJSONArray(JSON_PROP_ROOMRATE);
		
		
		for (int i = 0; i < roomrateArr.length(); i++) {
			JSONObject paxDetail = new JSONObject();
			JSONObject roomRateJson = roomrateArr.getJSONObject(i);
			JSONObject totalJson = roomRateJson.getJSONObject(JSON_PROP_TOTAL);
			// TODO :when Type=34 set as "ADT" and Type=35 set as "CHD" in case of
			// Contiki/Trafalgar/Monograms response

			String type = roomRateJson.getJSONObject(JSON_PROP_TOTAL).get(JSON_PROP_TYPE).toString();
			
			if (type.equals("NA")) {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, "");
			}else {
				paxDetail.put(JSON_PROP_PASSENGERTYPE, type);
			}
			//TODO : We do not get "childWithBed" indicator from supplier.Need to check when will this be true.
			paxDetail.put("childWithBed", childWithBed);
			//TODO : age and gender is blank as we do not get these details in Holidays Search
			paxDetail.put("age", "");
			paxDetail.put("gender", "");
			BigDecimal totalFare = roomRateJson.getJSONObject(JSON_PROP_TOTAL).getBigDecimal(JSON_PROP_AMOUNTAFTERTAX);
			paxDetail.put(JSON_PROP_TOTALFARE, totalFare);
			
			paxDetail.put(JSON_PROP_FAREDETAILS, getFareDetailsJsonArray(totalJson));
			
			passengerDetailsArr.put(paxDetail);
			
		}
		return passengerDetailsArr;
	}

	private static JSONArray getFareDetailsJsonArray(JSONObject totalJson) {
		JSONArray fareDtlsJsonArr = new JSONArray();
		JSONObject fareDtlsJson = new JSONObject();
		BigDecimal baseFare = totalJson.getBigDecimal(JSON_PROP_AMOUNTBEFORETAX);
		fareDtlsJson.put(JSON_PROP_FARENAME, JSON_VAL_BASE);
		fareDtlsJson.put(JSON_PROP_FAREVAL, baseFare);
		fareDtlsJsonArr.put(fareDtlsJson);
		
		JSONObject taxesJson = totalJson.getJSONObject(JSON_PROP_TAXES);
		JSONArray taxJsonArr = taxesJson.getJSONArray(JSON_PROP_TAX);
		for (int j=0; j < taxJsonArr.length(); j++) {
			JSONObject taxJson = taxJsonArr.getJSONObject(j);
			fareDtlsJson = new JSONObject();
			fareDtlsJson.put(JSON_PROP_FARENAME, taxJson.getString(JSON_PROP_TAXDESCRIPTION));
			fareDtlsJson.put(JSON_PROP_FAREVAL, taxJson.getBigDecimal(JSON_PROP_AMOUNT));
			fareDtlsJsonArr.put(fareDtlsJson);
		}
		
		return fareDtlsJsonArr;
	}

	private static void convertDate(JSONObject commonElemsJson, String travelDate,String travelDateStr) {
		if ((!travelDate.equals("") || !travelDate.isEmpty())){

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date date = formatter.parse(travelDate);
				
				commonElemsJson.put(travelDateStr, mDateFormat.format(date));
				

			} catch (ParseException e) {

				e.printStackTrace();
			}
		} 
		else {
			commonElemsJson.put(travelDateStr,"");
		}
	}

}
