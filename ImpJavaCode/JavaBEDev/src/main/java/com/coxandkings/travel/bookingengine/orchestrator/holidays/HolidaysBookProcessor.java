package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;
import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.orchestrator.air.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

import redis.clients.jedis.Jedis;

public class HolidaysBookProcessor implements HolidayConstants {

  private static final Logger logger = LogManager.getLogger(HolidaysBookProcessor.class);
  private static final DateFormat mDtFmt = new SimpleDateFormat("yyyyMMddHHmmssSSS");
  
  public static String process(JSONObject requestJson)
  {
	  logger.info(String.format("Holidaybookprocessor start"));
	  System.out.println("Inside book processor process ");
	  JSONObject resBodyJson = new JSONObject();
	  JSONObject responseJSON = new JSONObject();
	  String errorShortText = "";
	  String errorType = "";
	  String errorCode = "";
	  String errorStatus = "";
    try {
    	
   	KafkaBookProducer bookProducer = new KafkaBookProducer();
   	JSONObject kafkaMsgJson = requestJson;
    //Get the ProductConfig Collection and the operations in the Holidays document
    OperationConfig opConfig = HolidaysConfig.getOperationConfig("book");
    
    //clone shell si request from ProductConfig collection HOLIDAYS document
    Element requestElement = (Element) opConfig.getRequestXMLShell().cloneNode(true);
    
    //create Document object associated with request node, this Document object is also used to create new nodes.
    Document ownerDoc = requestElement.getOwnerDocument();
    
    TrackingContext.setTrackingContext(requestJson);
    
    JSONObject requestHeader = requestJson.getJSONObject(JSON_PROP_REQHEADER);
    JSONObject requestBody = requestJson.getJSONObject(JSON_PROP_REQBODY);
    
    //CREATE SI REQUEST HEADER
    Element requestHeaderElement = XMLUtils.getFirstElementAtXPath(requestElement, "./pac:RequestHeader");
    
    Element userIDElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:UserID");
    userIDElement.setTextContent(requestHeader.getString(JSON_PROP_USERID));
    
    Element sessionIDElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:SessionID");
    sessionIDElement.setTextContent(requestHeader.getString(JSON_PROP_SESSIONID));
    
    Element transactionIDElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:TransactionID");
    transactionIDElement.setTextContent(JSON_PROP_TRANSACTID);
    
    //UserContext for product holidays
   // UserContext usrCtx = UserContext.getUserContextForSession(requestHeader.getString(JSON_PROP_SESSIONID));
    UserContext usrCtx = UserContext.getUserContextForSession(requestHeader);
    
    Element supplierCredentialsListElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:SupplierCredentialsList");
    
    //CREATE SI REQUEST BODY
    Element requestBodyElement = XMLUtils.getFirstElementAtXPath(requestElement, "./pac1:RequestBody");
    
    Element wrapperElement = XMLUtils.getFirstElementAtXPath(requestBodyElement, "./pac:OTA_DynamicPkgBookRQWrapper");
    requestBodyElement.removeChild(wrapperElement);
     
    Map<String, String> reprcSuppFaresMap = null;
	try ( Jedis redisConn = RedisConfig.getRedisConnectionFromPool(); ) {
		String redisKey = requestHeader.optString(JSON_PROP_SESSIONID).concat("|").concat(PRODUCT).concat("|").concat("reprice");
		reprcSuppFaresMap = redisConn.hgetAll(redisKey);
		if (reprcSuppFaresMap.isEmpty()) {
			throw new Exception(String.format("Reprice context not found for %s", redisKey));
		}
	}
    
    int sequence = 0;
    JSONArray dynamicPackageArray = requestBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE);
    for (int i=0; i < dynamicPackageArray.length(); i++) 
    {
        JSONObject dynamicPackageObj = dynamicPackageArray.getJSONObject(i);
        sequence++;
    
        String supplierID = dynamicPackageObj.getString("supplierID");
        Element supWrapperElement = null;
        Element otaBookRQ = null;
        Element searchCriteria = null;
        Element dynamicPackage = null;
        
        //Making supplierCredentialsList for Each SupplierID
        supplierCredentialsListElement = HolidaysRepriceProcessor.getSupplierCredentialsList(ownerDoc, usrCtx, supplierID, sequence, supplierCredentialsListElement);
          
        //Making request body for particular supplierID
        supWrapperElement = (Element) wrapperElement.cloneNode(true);
        requestBodyElement.appendChild(supWrapperElement);
            
        //Setting supplier id in request body
        Element supplierIDElement = XMLUtils.getFirstElementAtXPath(supWrapperElement, "./pac:SupplierID");
        supplierIDElement.setTextContent(supplierID);
            
        //Setting sequence in request body
        Element sequenceElement = XMLUtils.getFirstElementAtXPath(supWrapperElement, "./pac:Sequence");
        sequenceElement.setTextContent(Integer.toString(sequence));

        //getting parent node OTA_DynamicPkgAvailRQ from SearchCriteria
        otaBookRQ = XMLUtils.getFirstElementAtXPath(supWrapperElement, "./ns:OTA_DynamicPkgBookRQ");

        //creating element dynamic package
        dynamicPackage = XMLUtils.getFirstElementAtXPath(otaBookRQ, "./ns:DynamicPackage");
        
        String allowOverrideAirDates = dynamicPackageObj.getString("allowOverrideAirDates");
        
        if(allowOverrideAirDates != null && allowOverrideAirDates.length() != 0)
        dynamicPackage.setAttribute("AllowOverrideAirDates", allowOverrideAirDates);
            
        //Creating Components element
        JSONObject components = dynamicPackageObj.getJSONObject(JSON_PROP_COMPONENTS); 
            
        if (components == null || components.length() == 0) {
          throw new Exception(String.format("Object components must be set for supplier %s", supplierID));
        }
            
        Element componentsElement = XMLUtils.getFirstElementAtXPath(dynamicPackage, "./ns:Components");
            
        //Creating Hotel Component
        JSONArray hotelComponents = components.getJSONArray(JSON_PROP_HOTEL_COMPONENT);
            
        if(hotelComponents != null && hotelComponents.length() != 0)
        {
           componentsElement = HolidaysRepriceProcessor.getHotelComponentElement(ownerDoc, hotelComponents, componentsElement);
        }
          
        //Creating Air Component
        JSONArray airComponents = components.getJSONArray(JSON_PROP_AIR_COMPONENT);
            
        if(airComponents != null && airComponents.length() != 0)
        {
          componentsElement = HolidaysRepriceProcessor.getAirComponentElement(ownerDoc, dynamicPackageObj, airComponents, componentsElement, supplierID);
        }
            
        //Creating PackageOptionComponent Element
        Element packageOptionComponentElement = ownerDoc.createElementNS(NS_OTA, "ns:PackageOptionComponent");
            
        String subTourCode = dynamicPackageObj.getString("subTourCode");
        if(subTourCode != null && subTourCode.length() > 0)
        {
          packageOptionComponentElement.setAttribute("QuoteID", subTourCode);
        }
            
        Element packageOptionsElement = ownerDoc.createElementNS(NS_OTA, "ns:PackageOptions");
            
        Element packageOptionElement = ownerDoc.createElementNS(NS_OTA, "ns:PackageOption");
        
        String brandName = dynamicPackageObj.getString("brandName");
        if(brandName != null && brandName.length() > 0)
        {
          packageOptionElement.setAttribute("CompanyShortName", brandName);
        }
        
        String tourCode = dynamicPackageObj.getString("tourCode");
        if(tourCode != null && tourCode.length() > 0)
        {
          packageOptionElement.setAttribute("ID", tourCode);
        }
            
        Element tpaElement = ownerDoc.createElementNS(NS_OTA, "ns:TPA_Extensions");
            
        //Creating Cruise Component
        JSONArray cruiseComponents = components.getJSONArray(JSON_PROP_CRUISE_COMPONENT);
            
        if(cruiseComponents != null && cruiseComponents.length() != 0)
        {
          tpaElement = HolidaysRepriceProcessor.getCruiseComponentElement(ownerDoc, cruiseComponents, tpaElement);
        }
            
        //Creating Transfers Component
        JSONArray transfersComponents = components.getJSONArray(JSON_PROP_TRANSFER_COMPONENT);
            
        if(transfersComponents != null && transfersComponents.length() != 0)
        {
           tpaElement = HolidaysRepriceProcessor.getTransferComponentElement(ownerDoc, transfersComponents, tpaElement);
        }
            
        //Creating Insurance Component
        JSONArray insuranceComponents = components.getJSONArray(JSON_PROP_INSURANCE_COMPONENT);
            
        if(insuranceComponents != null && insuranceComponents.length() != 0)
        {
           tpaElement = HolidaysRepriceProcessor.getInsuranceComponentElement(ownerDoc, insuranceComponents, tpaElement);
        }
            
        //Appending TPA element to package Option Element
        packageOptionElement.appendChild(tpaElement);
            
        //Appending package Option Element to package Options Element
        packageOptionsElement.appendChild(packageOptionElement);
            
        //Appending package Options Element to PackageOptionComponent Element
        packageOptionComponentElement.appendChild(packageOptionsElement);
            
        //Appending PackageOptionComponent Element to Components Element
        componentsElement.appendChild(packageOptionComponentElement);
            
            
        //create RestGuests xml elements
        JSONArray resGuests =  dynamicPackageObj.getJSONArray(JSON_PROP_RESGUESTS); 
            
        Element resGuestsElement = XMLUtils.getFirstElementAtXPath(dynamicPackage, "./ns:ResGuests");

        if(resGuests != null && resGuests.length() != 0)
        {
           for(int j=0;j<resGuests.length();j++)
           {
             JSONObject resGuest = resGuests.getJSONObject(j);
                
             Element resGuestElement = HolidaysRepriceProcessor.getResGuestElement(ownerDoc, resGuest);
                
             resGuestsElement.appendChild(resGuestElement);
           }
              
           //dynamicPackage.appendChild(resGuestsElement);
         }
            
            
        //Create GlobalInfo xml element
        JSONObject globalInfo = dynamicPackageObj.getJSONObject(JSON_PROP_GLOBALINFO);
            
        if(globalInfo != null && globalInfo.length() != 0)
        {
          Element globalInfoElement = XMLUtils.getFirstElementAtXPath(dynamicPackage, "./ns:GlobalInfo");
          globalInfoElement = HolidaysRepriceProcessor.getGlobalInfoElement(ownerDoc, globalInfo, globalInfoElement);
              
          //dynamicPackage.appendChild(globalInfoElement);
         }
        
        String dynamicPkgKey = HolidaysRepriceProcessor.getRedisKeyForDynamicPackage(dynamicPackageObj);
		JSONObject suppPriceBookInfoJson = new JSONObject(reprcSuppFaresMap.get(dynamicPkgKey));
		//JSONObject bookRefJson = suppPriceBookInfoJson.getJSONObject("bookReferences");
		        
		dynamicPackageObj.put("suppInfo", suppPriceBookInfoJson);
    }
    
    kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put("product", PRODUCT);
	// TODO: Remove hard-coded bookID
	kafkaMsgJson.getJSONObject(JSON_PROP_REQBODY).put("bookID", "CRT123HO12");
	System.out.println("KAfkaRQMsg->"+kafkaMsgJson);
	bookProducer.runProducer(1, kafkaMsgJson);
        
    logger.info(String.format("SI XML Request for Holidaybookprocessor = %s", XMLTransformer.toString(requestElement)));
        Element responseElement = null;
        responseElement = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), HolidaysConfig.getHttpHeaders(), requestElement);
        System.out.println("SI request: "+XMLTransformer.toString(requestElement));
        System.out.println("SI response: "+XMLTransformer.toString(responseElement));
        logger.info(String.format("SI XML Response for Holidaybookprocessor = %s",XMLTransformer.toString(responseElement)));
        if (responseElement == null) {
            throw new Exception("Null response received from SI");
        }
        
        //Response Header
	        /*Element responseHeaderElement = XMLUtils.getFirstElementAtXPath(responseElement, "./pac1:ResponseHeader");
	        JSONObject resHeader = new JSONObject();
	        String resStatus = String.valueOf(XMLUtils.getValueAtXPath(responseHeaderElement, "./pac:Status"));
	        String userID = String.valueOf(XMLUtils.getValueAtXPath(responseHeaderElement, "./pac:UserID"));
	        String sessionID = String.valueOf(XMLUtils.getValueAtXPath(responseHeaderElement, "./pac:SessionID"));
	        String transactionID = String.valueOf(XMLUtils.getValueAtXPath(responseHeaderElement, "./pac:TransactionID"));
	        resHeader.put("Status", resStatus);
	        resHeader.put("UserID", userID);
	        resHeader.put("SessionID", sessionID);
	        resHeader.put("TransactionID", transactionID);*/
        //Response Header End
		
      //--------------------------OTA_DynamicPkgBookRSWrapper Start -----------------------------------
		Element oTA_wrapperElems [] = XMLUtils.getElementsAtXPath(responseElement, "./pac1:ResponseBody/pac:OTA_DynamicPkgBookRSWrapper");
		
		
		//For Loop
		for(Element oTA_wrapperElem : oTA_wrapperElems) {
			
		String supplierID = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:SupplierID"));
		String Sequence = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:Sequence"));
		String subTourCode = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./ns:OTA_DynamicPkgBookRS/ns:DynamicPackage/ns:Components/ns:PackageOptionComponent/ns:PackageOptions/ns:PackageOption/@QuoteID"));
		String brandName = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./ns:OTA_DynamicPkgBookRS/ns:DynamicPackage/ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/ns:CompanyName/@CompanyShortName"));
		String tourCode = String.valueOf(XMLUtils.getValueAtXPath(oTA_wrapperElem, "./ns:OTA_DynamicPkgBookRS/ns:DynamicPackage/ns:GlobalInfo/ns:DynamicPkgIDs/ns:DynamicPkgID/@ID"));
		

		//OTA_DynamicPkgBookRS
		Element ota_DynamicPkgBookRSElement = XMLUtils.getFirstElementAtXPath(oTA_wrapperElem, "./ns:OTA_DynamicPkgBookRS");
		
		//Error Response from SI
		Element errorElem[] = XMLUtils.getElementsAtXPath(ota_DynamicPkgBookRSElement, "./ns:Errors/ns:Error");
		if(errorElem.length != 0) {
			for(Element error : errorElem) {
			errorShortText = String.valueOf(XMLUtils.getValueAtXPath(error, "./@ShortText"));
			errorType = String.valueOf(XMLUtils.getValueAtXPath(error, "./@Type"));
			errorCode = String.valueOf(XMLUtils.getValueAtXPath(error, "./@Code"));
			errorStatus = String.valueOf(XMLUtils.getValueAtXPath(error, "./@status"));
			logger.info(String.format("Recieved Error from SI. Error Details: ErrorCode:" + errorCode + ", Type:" + errorType + ", Status:" + errorStatus + ", ShortText:"+ errorShortText));
			}
			continue;
			
		}
		//Error Handling end
		
//		String bookingId = requestHeader.getString(JSON_PROP_SESSIONID).concat("-")
//				.concat(mDtFmt.format(Calendar.getInstance().getTime()));
//		resBodyJson.put("bookId", bookingId);
		
		//To access <n1:DynamicPackage> start
		Element dynamicPkgElems [] = XMLUtils.getElementsAtXPath(ota_DynamicPkgBookRSElement, "./ns:DynamicPackage");
		
		JSONArray dynamicPkgArray = new JSONArray();
	
		for(Element dynamicPackageElement : dynamicPkgElems) {
			
		JSONObject dynamicPkgJSON=getDynamicPkgComponent(dynamicPackageElement);
		dynamicPkgJSON.put("supplierID", supplierID);
		dynamicPkgJSON.put("sequence", Sequence);
		dynamicPkgJSON.put("subTourCode", subTourCode);
		dynamicPkgJSON.put("brandName", brandName);
		dynamicPkgJSON.put("tourCode", tourCode);
		
		JSONObject suppBookJson = new JSONObject();
		suppBookJson.put(JSON_PROP_SUPPREF, XMLUtils.getValueAtXPath(oTA_wrapperElem, "./pac:SupplierID"));
		suppBookJson.put("bookingID", XMLUtils.getValueAtXPath(dynamicPackageElement,
				"./ns:Components/ns:PackageOptionComponent/ns:UniqueID"));
		dynamicPkgJSON.put("supplierBookReferences",suppBookJson);
		dynamicPkgArray.put(dynamicPkgJSON);
		}
		//To access <n1:DynamicPackage> end
		resBodyJson.put("dynamicPackage", dynamicPkgArray);
		//OTA_DynamicPkgBookRS Ends
		}
		//--------------------------OTA_DynamicPkgBookRSWrapper end -----------------------------------
		
		responseJSON.put("responseHeader", requestHeader);
		responseJSON.put("responseBody", resBodyJson);
		
		kafkaMsgJson = new JSONObject();
		if(resBodyJson.length()!=0) {
			kafkaMsgJson.put(JSON_PROP_RESBODY, resBodyJson);
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("product", PRODUCT);
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("bookID", "CRT123HO12");
			bookProducer.runProducer(1, kafkaMsgJson);
		}
		else {
			kafkaMsgJson.put(JSON_PROP_RESBODY, resBodyJson);
			kafkaMsgJson.getJSONObject(JSON_PROP_RESBODY).put("Recieved Error from SI. Error Details: ErrorCode:" + errorCode + ", Type:" + errorType + ", Status:" + errorStatus + ", ShortText:"+ errorShortText, "Error");
			bookProducer.runProducer(1, kafkaMsgJson);
		}
		logger.info(String.format("SI JSON Response for Holidaybookprocessor = %s",resBodyJson));
		logger.info(String.format("Booking process end"));
        return responseJSON.toString();
      }
      catch (Exception x) {
          x.printStackTrace();
          logger.info(String.format("SI JSON Response for Holidaybookprocessor = %s",x));
          logger.debug("Exception in Holidaybookprocessor = %s",x);
          return "{\"status\": \"ERROR\"}";
      } 
  }
  
  
  protected static JSONObject getDynamicPkgComponent(Element dynamicPackageElem) {
		JSONObject dynamicPkgComponentJSON=new JSONObject();
		
		JSONArray uniqIDJSONArray = getUniqueID(dynamicPackageElem);
		dynamicPkgComponentJSON.put("uniqueID", uniqIDJSONArray);
		
		//resGuest
		JSONArray resGuestArray = new JSONArray();
		Element resGuestElem [] = XMLUtils.getElementsAtXPath(dynamicPackageElem, "./ns:ResGuests/ns:ResGuest");
		for(Element resGuest : resGuestElem) {
		
		JSONObject resGuestsJSON=getResGuests(resGuest);
		resGuestArray.put(resGuestsJSON);
		}
		JSONObject guestsJSON =new JSONObject();
		guestsJSON.put("resGuest", resGuestArray);
		dynamicPkgComponentJSON.put(JSON_PROP_RESGUESTS, guestsJSON);
		//resGuest ends
		
		JSONObject globalInfoJSON=getglobalinfo(dynamicPackageElem);
		dynamicPkgComponentJSON.put(JSON_PROP_GLOBALINFO, globalInfoJSON);
		
				
		return dynamicPkgComponentJSON;
	}

	private static JSONObject getglobalinfo(Element dynamicPkgElement) {
		Element globalInfoElement = XMLUtils.getFirstElementAtXPath(dynamicPkgElement, "./ns:GlobalInfo");
		JSONObject globalInfoJSON=new JSONObject();
		
		//For TimeSpan
		Element timeSpanElement = XMLUtils.getFirstElementAtXPath(globalInfoElement, "./ns:TimeSpan");
		JSONObject timeSpanJSON = new JSONObject();
		String End = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElement, "./@End"));
		String TravelStartDate = String.valueOf(XMLUtils.getValueAtXPath(timeSpanElement, "./@TravelStartDate"));
		timeSpanJSON.put(JSON_PROP_END, End);
		timeSpanJSON.put(JSON_PROP_TRAVELSTARTDATE, TravelStartDate);
		globalInfoJSON.put(JSON_PROP_TIMESPAN, timeSpanJSON);
		//Timespan End
		
		//special request start
		JSONArray specialRequestJSONArray = new JSONArray();
		JSONObject specialRequestJson=new JSONObject();
			Element[] specialRequestElementArray = XMLUtils.getElementsAtXPath(globalInfoElement, "./ns:SpecialRequests/ns:SpecialRequest");
			
			//For Text array start <ns:text>
			for(Element specialRequestElem : specialRequestElementArray ) {
				
				String name = String.valueOf(XMLUtils.getValueAtXPath(specialRequestElem, "./@Name"));
				JSONObject specialRequestJSON1=new JSONObject();
				specialRequestJSON1.put("name", name);
				JSONArray textJSONArray = new JSONArray();
				
				Element[] specialRequestTextElementArray = XMLUtils.getElementsAtXPath(specialRequestElem, "./ns:Text");
				for(Element specialRequestTextElement : specialRequestTextElementArray ) {
					String text = String.valueOf(XMLUtils.getElementValue(specialRequestTextElement));
					textJSONArray.put(text);
				
				}
				specialRequestJSON1.put("text",textJSONArray);
				specialRequestJSONArray.put(specialRequestJSON1);
			}
			specialRequestJson.put("specialRequest", specialRequestJSONArray);
			globalInfoJSON.put("specialRequests", specialRequestJson);
			//end
		//special request end
		
		//Guarantee
		JSONArray guranteePaymentsJSONArray = new JSONArray();
		Element guranteePaymentsElement[] = XMLUtils.getElementsAtXPath(globalInfoElement, "./ns:DepositPayments/ns:GuaranteePayment");
		
		for(Element guranteePaymentElement : guranteePaymentsElement ) {
		JSONObject guranteePaymentsJSONObject=getDepositPayments(guranteePaymentElement);
		guranteePaymentsJSONArray.put(guranteePaymentsJSONObject);
		}
		JSONObject depositPaymentsJSON = new JSONObject();
		depositPaymentsJSON.put("guaranteePayment", guranteePaymentsJSONArray);
		globalInfoJSON.put("depositPayments", depositPaymentsJSON);
		//Guarantee ends
		
		//Total
		JSONArray totalJsonArray = new JSONArray();
		Element totalElementPath[] = XMLUtils.getElementsAtXPath(globalInfoElement, "./ns:Total");
		
		for(Element totalElement : totalElementPath ) {
		
		JSONObject totalJSON=getTotal(totalElement);
		totalJsonArray.put(totalJSON);
		}
		globalInfoJSON.put(JSON_PROP_TOTAL, totalJsonArray);
		//Total End
		
		
		//BookingRules
		Element bookingRuleElementPath[] = XMLUtils.getElementsAtXPath(globalInfoElement, "./ns:BookingRules/ns:BookingRule");
		JSONArray bookingRuleArray = new JSONArray();
		JSONObject bookingRule = new JSONObject();
		
		for(Element bookingRuleElem : bookingRuleElementPath) {
			
		JSONObject descriptionJson = getBookingRules(bookingRuleElem);
		bookingRuleArray.put(descriptionJson);
	
		}
		bookingRule.put("bookingRule",bookingRuleArray);
		globalInfoJSON.put("bookingRules",bookingRule);
		//BookingRules End
		
		//For TotalCommissions
		Element CommissionPayableAmountElement = XMLUtils.getFirstElementAtXPath(globalInfoElement, "./ns:TotalCommissions/ns:CommissionPayableAmount");
		String Amount = String.valueOf(XMLUtils.getValueAtXPath(CommissionPayableAmountElement, "./@Amount"));
		
		JSONObject CommissionPayableAmountJSON = new JSONObject();
		CommissionPayableAmountJSON.put(JSON_PROP_AMOUNT, Amount);
		
		JSONObject totalCommisionsjson = new JSONObject();
		totalCommisionsjson.put("commissionPayableAmount", CommissionPayableAmountJSON);
		globalInfoJSON.put("totalCommissions", totalCommisionsjson);
		//TotalCommissions end
		
		JSONObject dynamicPkgIDJSON = getDynamicPkgIDs(globalInfoElement);
		globalInfoJSON.put("dynamicPkgID", dynamicPkgIDJSON);
		
		return globalInfoJSON;
	}

	private static JSONObject getDynamicPkgIDs(Element globalInfoElement) {
		Element dynamicPkgIDElement = XMLUtils.getFirstElementAtXPath(globalInfoElement, "./ns:DynamicPkgIDs/ns:DynamicPkgID");
		String id_context = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElement,"./@ID_Context"));
		String id = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElement,"./@ID"));
		
		JSONObject dynamicPkgIDJSON = new JSONObject();
		dynamicPkgIDJSON.put(JSON_PROP_ID_CONTEXT, id_context);
		dynamicPkgIDJSON.put(JSON_PROP_ID, id);
		
		String companyShortName = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElement,"./ns:CompanyName/@CompanyShortName"));
		JSONObject companyNameJson = new JSONObject();
		companyNameJson.put(JSON_PROP_COMPANYSHORTNAME, companyShortName);
		dynamicPkgIDJSON.put(JSON_PROP_COMPANYNAME, companyNameJson);
		
		String tourName = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgIDElement,"./ns:TPA_Extensions/pac:Pkgs_TPA/pac:TourDetails/pac:TourName"));
		JSONObject tourDetailsJson = new JSONObject();
		tourDetailsJson.put(JSON_PROP_TOURNAME, tourName);
		
		dynamicPkgIDJSON.put("tourDetails",tourDetailsJson);
		
		return dynamicPkgIDJSON;
	}

	private static JSONObject getBookingRules(Element bookingRule) {
		
		Element brDescription[] = XMLUtils.getElementsAtXPath(bookingRule, "./ns:Description");
		JSONArray bookingRDescriptionArray = new JSONArray();
		for(Element description :brDescription) {
		String Name = String.valueOf(XMLUtils.getValueAtXPath(description,"./@Name"));
		String Text = String.valueOf(XMLUtils.getValueAtXPath(description,"./ns:Text"));

		JSONObject descriptionJson = new JSONObject();
		descriptionJson.put(JSON_PROP_NAME, Name);
		descriptionJson.put(JSON_PROP_TEXT, Text);
		
		bookingRDescriptionArray.put(descriptionJson);
		}
		
		JSONObject descriptionJson = new JSONObject();
		descriptionJson.put(JSON_PROP_DESCRIPTION, bookingRDescriptionArray);
	
		return descriptionJson;
	}

	private static JSONObject getTotal(Element totalElement) {
		String amountAfterTax = String.valueOf(XMLUtils.getValueAtXPath(totalElement, "./@AmountAfterTax"));
		String amountBeforeTax = String.valueOf(XMLUtils.getValueAtXPath(totalElement, "./@AmountBeforeTax"));
		JSONObject totalJSON = new JSONObject();
		totalJSON.put(JSON_PROP_AMOUNTAFTERTAX, amountAfterTax);
		totalJSON.put(JSON_PROP_AMOUNTBEFORETAX, amountBeforeTax);
		
		
		Element tpa_ExtensionsElement = XMLUtils.getFirstElementAtXPath(totalElement, "./ns:TPA_Extensions");
		
		Element PaymentElement = XMLUtils.getFirstElementAtXPath(tpa_ExtensionsElement, "./pac:Payment");
		String BalanceDueGross = String.valueOf(XMLUtils.getValueAtXPath(PaymentElement, "./pac:BalanceDueGross"));
		String Payments = String.valueOf(XMLUtils.getValueAtXPath(PaymentElement, "./pac:Payments"));
		JSONObject paymentJSON = new JSONObject();
		paymentJSON.put("balanceDueGross", BalanceDueGross);
		paymentJSON.put("payments", Payments);
		totalJSON.put("payment", paymentJSON);
		
		
		Element excursionsElement = XMLUtils.getFirstElementAtXPath(tpa_ExtensionsElement, "./pac:Excursions/pac:ExcursionsTotal");
		String AmountAfterTax = String.valueOf(XMLUtils.getValueAtXPath(excursionsElement, "./@AmountAfterTax"));
		JSONObject ExcursionsTotalJSON = new JSONObject();
		JSONObject ExcursionsJSON = new JSONObject();
		ExcursionsTotalJSON.put(JSON_PROP_AMOUNTAFTERTAX, AmountAfterTax);
		
		Element taxElement = XMLUtils.getFirstElementAtXPath(excursionsElement, "./ns:Taxes/ns:Tax");
		String Amount = String.valueOf(XMLUtils.getValueAtXPath(taxElement, "./@Amount"));
		
		String Name = String.valueOf(XMLUtils.getValueAtXPath(taxElement, "./ns:TaxDescription/@Name"));
		JSONObject taxDescriptionJSON = new JSONObject();
		taxDescriptionJSON.put(JSON_PROP_NAME, Name);
		
		JSONObject taxJSON = new JSONObject();
		taxJSON.put(JSON_PROP_AMOUNT, Amount);
		taxJSON.put(JSON_PROP_TAXDESCRIPTION, taxDescriptionJSON);
		
		JSONObject taxesJSON = new JSONObject();
		taxesJSON.put(JSON_PROP_TAX, taxJSON);
		
		ExcursionsTotalJSON.put(JSON_PROP_TAXES, taxesJSON);
		
		ExcursionsJSON.put("excursionsTotal", ExcursionsTotalJSON);
				
		String ExcursionsBalanceDue = String.valueOf(XMLUtils.getValueAtXPath(tpa_ExtensionsElement, "./pac:Excursions/pac:ExcursionsBalanceDue"));
		ExcursionsJSON.put("excursionsBalanceDue", ExcursionsBalanceDue);
		
		String cAmount = String.valueOf(XMLUtils.getValueAtXPath(tpa_ExtensionsElement, "./pac:Excursions/pac:ExcursionsCommission/ns:CommissionPayableAmount/@Amount"));
		JSONObject commissionPayableAmountJSON = new JSONObject();
		commissionPayableAmountJSON.put(JSON_PROP_AMOUNT, cAmount);
		
		JSONObject excursionsCommissionJSON = new JSONObject();
		excursionsCommissionJSON.put("commissionPayableAmount", commissionPayableAmountJSON);
		
		ExcursionsJSON.put("excursionsCommission", excursionsCommissionJSON);
		totalJSON.put("excursions", ExcursionsJSON);

		return totalJSON;
	}

	private static JSONObject getDepositPayments(Element guranteePayment) {
		
		
		String Amount = String.valueOf(XMLUtils.getValueAtXPath(guranteePayment, "./ns:AmountPercent/@Amount"));
		
		Element deadlineElement[] = XMLUtils.getElementsAtXPath(guranteePayment, "./ns:Deadline");
		JSONArray deadLineArray = new JSONArray();
		for(Element deadline : deadlineElement) {
		JSONObject absoluteDeadlineJSON = new JSONObject();
		String AbsoluteDeadline = String.valueOf(XMLUtils.getValueAtXPath(deadline, "./@AbsoluteDeadline"));
		
		absoluteDeadlineJSON.put("absoluteDeadline", AbsoluteDeadline);
		deadLineArray.put(absoluteDeadlineJSON);
		
		}
		
		JSONObject guaranteePaymentJSON = new JSONObject();
		JSONObject amountPercentJSON = new JSONObject();
		
		
		guaranteePaymentJSON.put("deadline", deadLineArray);
		amountPercentJSON.put(JSON_PROP_AMOUNT, Amount);
		guaranteePaymentJSON.put("amountPercent", amountPercentJSON);
		
		return guaranteePaymentJSON;
	}

	private static JSONObject getResGuests(Element dynamicPkgElement) {
		JSONObject resGuestJSON=new JSONObject();
		
		String primaryIndicator = String.valueOf(XMLUtils.getValueAtXPath(dynamicPkgElement, "./@PrimaryIndicator"));
		resGuestJSON.put("primaryIndicator", primaryIndicator);
		
		Element customerElement = XMLUtils.getFirstElementAtXPath(dynamicPkgElement, "./ns:Profiles/ns:ProfileInfo/ns:Profile/ns:Customer");
		String birthDate = String.valueOf(XMLUtils.getValueAtXPath(customerElement, "./@BirthDate"));
		String gender = String.valueOf(XMLUtils.getValueAtXPath(customerElement, "./@Gender"));
		JSONObject customerJSON =new JSONObject();
		customerJSON.put("birthDate", birthDate);
		customerJSON.put("gender", gender);
		
		//<n1:Telephone PhoneNumber/>
		Element phoneNumberElement[] = XMLUtils.getElementsAtXPath(customerElement, "./ns:Telephone");
		JSONArray telephoneJSONArray = new JSONArray();
		
		for(Element phoneNumber : phoneNumberElement) {
		
		JSONObject telephoneJSON =new JSONObject();
		String phoneNumberString = String.valueOf(XMLUtils.getValueAtXPath(phoneNumber, "./@PhoneNumber"));
		telephoneJSON.put("phoneNumber", phoneNumberString);
		
		telephoneJSONArray.put(telephoneJSON);
		}
		customerJSON.put("telephone", telephoneJSONArray);
		//<n1:Telephone PhoneNumber/>
		
		
		//<ns:Email>
		Element emailElement[] = XMLUtils.getElementsAtXPath(customerElement, "./ns:Email");
		JSONArray emailArray = new JSONArray();
				
		for(Element email : emailElement) {	
		String emailString = String.valueOf(XMLUtils.getElementValue(email));
		emailArray.put(emailString);
		}
		customerJSON.put("email", emailArray);
		//</ns:Email>
				
		
		//<ns:Document DocID="" DocType="" />
		Element documentElement[] = XMLUtils.getElementsAtXPath(customerElement, "./ns:Document");
		JSONArray documentArray = new JSONArray();
		
		for(Element document : documentElement) {
		
		JSONObject documentJSON =new JSONObject();
		String docIDString = String.valueOf(XMLUtils.getValueAtXPath(document, "./@DocID"));
		String docTypeString = String.valueOf(XMLUtils.getValueAtXPath(document, "./@DocType"));
		documentJSON.put("docID", docIDString);
		documentJSON.put("docType", docTypeString);
		
		documentArray.put(documentJSON);
		}
		customerJSON.put("document", documentArray);
		//<ns:Document DocID="" DocType="" />
		
		Element personNameElement = XMLUtils.getFirstElementAtXPath(customerElement, "./ns:PersonName");
		
		String givenName = String.valueOf(XMLUtils.getValueAtXPath(personNameElement, "./ns:GivenName"));
		String surname = String.valueOf(XMLUtils.getValueAtXPath(personNameElement, "./ns:Surname"));
		String nameTitle = String.valueOf(XMLUtils.getValueAtXPath(personNameElement, "./ns:NameTitle"));
		String middleName = String.valueOf(XMLUtils.getValueAtXPath(personNameElement, "./ns:MiddleName"));
		
		JSONObject personNameJSON =new JSONObject();
		personNameJSON.put("givenName", givenName);
		personNameJSON.put("surname", surname);
		personNameJSON.put("nameTitle", nameTitle);
		personNameJSON.put("middleName", middleName);
		
		customerJSON.put("personName", personNameJSON);
		
		//Address Start
				JSONObject addressJSON =new JSONObject();
				
				Element addressElement = XMLUtils.getFirstElementAtXPath(customerElement, "./ns:Address");
				
				Element addressLineElement[] = XMLUtils.getElementsAtXPath(addressElement, "./ns:AddressLine");
				JSONArray addressLineArray = new JSONArray();
				
				for(Element addressLine : addressLineElement) {
					String addressLineString = String.valueOf(XMLUtils.getElementValue(addressLine));
					addressLineArray.put(addressLineString);			
				}
				String cityName = String.valueOf(XMLUtils.getValueAtXPath(addressElement, "./ns:CityName"));
				String postalCode = String.valueOf(XMLUtils.getValueAtXPath(addressElement, "./ns:PostalCode"));
				String countryName = String.valueOf(XMLUtils.getValueAtXPath(addressElement, "./ns:CountryName"));
				String stateProv = String.valueOf(XMLUtils.getValueAtXPath(addressElement, "./ns:StateProv"));
				
				addressJSON.put("addressLine", addressLineArray);
				addressJSON.put("cityName", cityName);
				addressJSON.put("postalCode", postalCode);
				addressJSON.put("countryName", countryName);
				addressJSON.put("stateProv", stateProv);
				
				customerJSON.put("address", addressJSON);
				
				//Address Ends
		
		JSONObject profileJSON =new JSONObject();
		JSONObject profileInfoJSON =new JSONObject();
		JSONObject profilesJSON =new JSONObject();
				
		profileJSON.put("customer", customerJSON);
		profileInfoJSON.put("profile", profileJSON);
		profilesJSON.put("profileInfo", profileInfoJSON);
		resGuestJSON.put("profiles", profilesJSON);
		
		return resGuestJSON;
	}

	private static JSONArray getUniqueID(Element dynamicPkgElement) {
		Element packageOptionComponentElement = XMLUtils.getFirstElementAtXPath(dynamicPkgElement, "./ns:Components/ns:PackageOptionComponent");
		JSONArray unqIdArray = new JSONArray();

		if (packageOptionComponentElement!=null) {
		Element unqIDElem [] = XMLUtils.getElementsAtXPath(packageOptionComponentElement, "./ns:UniqueID");
		
		
		//Unique ID Array
		for(Element UnqID : unqIDElem) {
		
		String id_context = String.valueOf(XMLUtils.getValueAtXPath(UnqID, "./@ID_Context"));
		String id = String.valueOf(XMLUtils.getValueAtXPath(UnqID, "./@ID"));
		String createdDate = String.valueOf(XMLUtils.getValueAtXPath(UnqID, "./@CreatedDate"));
		
		JSONObject uniqueIDJSON = new JSONObject();
		
		uniqueIDJSON.put(JSON_PROP_ID_CONTEXT, id_context);
		uniqueIDJSON.put(JSON_PROP_ID, id);
		uniqueIDJSON.put("createdDate", createdDate);
		unqIdArray.put(uniqueIDJSON);
		//Unique ID Array End
		}
		
		}
		
		return unqIdArray;
	}

}


