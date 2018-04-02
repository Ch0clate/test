package com.coxandkings.travel.bookingengine.orchestrator.holidays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.OperationConfig;
import com.coxandkings.travel.bookingengine.config.holidays.HolidaysConfig;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.utils.xml.XMLTransformer;
import com.coxandkings.travel.bookingengine.utils.xml.XMLUtils;

public class HolidaysRetrieveProcessor implements HolidayConstants {

private static final Logger logger = LogManager.getLogger(HolidaysRepriceProcessor.class);
  
  public static String process(JSONObject requestJson)
  {
    try {
      //Get the ProductConfig Collection and the operations in the Holidays document
      OperationConfig opConfig = HolidaysConfig.getOperationConfig("retrieve");
      
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
    UserContext usrCtx = UserContext.getUserContextForSession(requestHeader);
    
    Element supplierCredentialsListElement = XMLUtils.getFirstElementAtXPath(requestHeaderElement, "./com:SupplierCredentialsList");
    
    //CREATE SI REQUEST BODY
    Element requestBodyElement = XMLUtils.getFirstElementAtXPath(requestElement, "./pac1:RequestBody");
    
    Element wrapperElement = XMLUtils.getFirstElementAtXPath(requestBodyElement, "./pac:OTA_ReadRQWrapper");
    requestBodyElement.removeChild(wrapperElement);
    
    int sequence = 0;
    JSONArray dynamicPackageArray = requestBody.getJSONArray(JSON_PROP_DYNAMICPACKAGE);
    for (int i=0; i < dynamicPackageArray.length(); i++) 
    {
        JSONObject dynamicPackageObj = dynamicPackageArray.getJSONObject(i);
        sequence++;
    
        String supplierID = dynamicPackageObj.getString(JSON_PROP_SUPPREF);
        Element supWrapperElement = null;
        
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
        
        //getting UniqueID Element
        Element uniqueID = XMLUtils.getFirstElementAtXPath(supWrapperElement, "./ns:OTA_ReadRQ/ns:UniqueID");

        String invoiceNumber = dynamicPackageObj.getString("invoiceNumber");
        if(invoiceNumber != null && invoiceNumber.length() > 0)
        {
          uniqueID.setAttribute("ID", invoiceNumber);
        }
        
        String idContext = dynamicPackageObj.getString("idContext");
        if(idContext != null && idContext.length() > 0)
        {
          uniqueID.setAttribute("ID_Context", idContext);
        }
        
        String type = dynamicPackageObj.getString("type");
        if(type != null && type.length() > 0)
        {
          uniqueID.setAttribute("Type", type);
        }
      }
    
      Element responseElement = null;
      responseElement = HTTPServiceConsumer.consumeXMLService("SI", opConfig.getSIServiceURL(), HolidaysConfig.getHttpHeaders(), requestElement);
      if (responseElement == null) {
          throw new Exception("Null response received from SI");
      }
  
      System.out.println(XMLTransformer.toString(requestElement));
      
      return XMLTransformer.toString(responseElement);
    }
    catch (Exception x) {
        x.printStackTrace();
        return "{\"status\": \"ERROR\"}";
    }
  }
  
  
}
