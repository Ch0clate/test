package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.HolidaysExtensionDetails;
import com.coxandkings.travel.bookingengine.db.model.HolidaysExtrasDetails;
import com.coxandkings.travel.bookingengine.db.model.HolidaysOrders;
import com.coxandkings.travel.bookingengine.db.model.InsuranceOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.HolidaysDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Holidays")
@Transactional(readOnly = false)
public class HolidaysDatabaseServiceImpl implements DataBaseService,Constants,ErrorConstants{

	@Autowired
	@Qualifier("Holidays")
	private HolidaysDatabaseRepository holidaysRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
    @Qualifier("Passenger")
    private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
    
    JSONObject response=new JSONObject(); 
	
	@Override
    public boolean isResponsibleFor(String product) {
		return "Holidays".equals(product);
	}


	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {
	    
	    JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		{
		  booking = populateBookingData(bookRequestJson);
		}
		else
		{
		  booking.setIsHolidayBooking("YES");
		}
		
		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PKGS_DYNAMICPACKAGEINFO)) {
			
		    Map<String, PassengerDetails> paxIndexMap = new HashMap<String, PassengerDetails>();
		  
			HolidaysOrders holidaysOrder = populateHolidaysData((JSONObject) orderJson, bookRequestHeader,booking,paxIndexMap);
		      
			JSONArray paxIDs = new JSONArray();
		      for (Map.Entry<String, PassengerDetails> entry : paxIndexMap.entrySet())
		      {
		        JSONObject paxJson = new JSONObject();
		        paxJson.put("paxId", entry.getValue().getPassanger_id());
		        paxIDs.put(paxJson);
		        System.out.println(entry.getKey() + "/" + entry.getValue().getPassanger_id());
		      }
		    holidaysOrder.setPaxDetails(paxIDs.toString());
			
			HolidaysOrders holidaysOrdersResponse = saveHolidaysOrder(holidaysOrder, "");
			
		}
		myLogger.info(String.format("Holidays Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
	}

	private HolidaysOrders populateHolidaysData(JSONObject holidayOrderJson, JSONObject bookRequestHeader, Booking booking,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
	  try
	  {
		HolidaysOrders holidaysOrder = new HolidaysOrders();
		
		/*// Creating a random UUID (Universally unique identifier).
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();
        String id = String.format("%s-%d", uuid.toString(), now);

		holidaysOrder.setId(id);*/
		
		holidaysOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		holidaysOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));

		holidaysOrder.setBooking(booking);
		holidaysOrder.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		holidaysOrder.setStatus("OnRequest");
		/*HolidaysOrder.setClientIATANumber(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		HolidaysOrder.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		HolidaysOrder.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		HolidaysOrder.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));*/
		
		holidaysOrder.setSupplierID(holidayOrderJson.getString(JSON_PROP_SUPPREF));
		holidaysOrder.setOperationType("insert");
		
		//Total Packages Price
		holidaysOrder.setTotalPrice(holidayOrderJson.getJSONObject("bookingPriceInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
		holidaysOrder.setTotalPriceCurrencyCode(holidayOrderJson.getJSONObject("bookingPriceInfo").getString(JSON_PROP_CURRENCYCODE));
		holidaysOrder.setTotalPriceTaxes(holidayOrderJson.getJSONObject("bookingPriceInfo").getJSONObject(JSON_PROP_TAXES).toString());
        
        //Supplier Package Price
		holidaysOrder.setSupplierPrice(holidayOrderJson.getJSONObject("supplierBookingPriceInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
		holidaysOrder.setSupplierPriceCurrencyCode(holidayOrderJson.getJSONObject("supplierBookingPriceInfo").getString(JSON_PROP_CURRENCYCODE));
		holidaysOrder.setSuppPriceTaxes(holidayOrderJson.getJSONObject("supplierBookingPriceInfo").getJSONObject(JSON_PROP_TAXES).toString());

		//Packages Field
        holidaysOrder.setBrandName(holidayOrderJson.getString(JSON_PROP_PKGS_BRANDNAME));
        holidaysOrder.setTourCode(holidayOrderJson.getString(JSON_PROP_PKGS_TOURCODE));
        holidaysOrder.setSubTourCode(holidayOrderJson.getString(JSON_PROP_PKGS_SUBTOURCODE));
        holidaysOrder.setTourName(holidayOrderJson.getString("tourName"));
        holidaysOrder.setTourStartCity(holidayOrderJson.getString("tourStartCity"));
        holidaysOrder.setTourEndCity(holidayOrderJson.getString("tourEndCity"));
        holidaysOrder.setTourStart(holidayOrderJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
        holidaysOrder.setTourEnd(holidayOrderJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
        holidaysOrder.setTravelStartDate(holidayOrderJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANTRAVELSTART));
        holidaysOrder.setTravelEndDate(holidayOrderJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANTRAVELEND));
		
		Set<SupplierCommercial> setSupplierCommercials = new HashSet<SupplierCommercial>();
		setSupplierCommercials = readSupplierCommercials(holidayOrderJson, holidaysOrder);
		holidaysOrder.setSuppcommercial(setSupplierCommercials);
        
        Set<ClientCommercial> setClientCommercials = new HashSet<ClientCommercial>();
        setClientCommercials = readClientCommercials(holidayOrderJson.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS), holidaysOrder);
        holidaysOrder.setClientCommercial(setClientCommercials);
   
        
        //Accommodation in Packages
        AccoOrders accoOrders = new AccoOrders();
        accoOrders = populateAccoData(holidayOrderJson, bookRequestHeader,paxIndexMap, holidaysOrder);     
        holidaysOrder.setAccoOrders(accoOrders);
          
        /*//Activities in Packages
        JSONArray activitiesConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_ACTIVITYCONFIG);
        if(activitiesConfigArray != null && activitiesConfigArray.length()>0)
        {
          Set<ActivitiesOrders> activitiesOrders = new HashSet<ActivitiesOrders>();
          activitiesOrders = readActivityDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setActivitiesOrders(activitiesOrders);
        }*/
        
        //Transfers in Packages
        JSONArray transferConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_TRANSFERCONFIG);
        if(transferConfigArray != null && transferConfigArray.length()>0)
        {
          Set<TransfersOrders> transferOrders = new HashSet<TransfersOrders>();
          transferOrders = readTransferDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setTransfersOrders(transferOrders);
        }
        
        //Insurance in Packages
        JSONArray insuranceConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_INSURANCECONFIG);
        if(insuranceConfigArray != null && insuranceConfigArray.length()>0)
        {
          Set<InsuranceOrders> insuranceOrders = new HashSet<InsuranceOrders>();
          insuranceOrders = readInsuranceDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setInsuranceOrders(insuranceOrders);
        }
        
        //ExtensionNights in Packages
        JSONArray extensionNightConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_EXTENSIONNIGHTSCONFIG);
        if(extensionNightConfigArray != null && extensionNightConfigArray.length()>0)
        {
          Set<HolidaysExtensionDetails> holidaysExtensionDetails = new HashSet<HolidaysExtensionDetails>();
          holidaysExtensionDetails = readExtensionNightDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setHolidaysExtensionDetails(holidaysExtensionDetails);
        }
        
        //Extras in Packages
        JSONArray extrasConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_EXTRASCONFIG);
        if(extrasConfigArray != null && extrasConfigArray.length()>0)
        {
          Set<HolidaysExtrasDetails> holidaysExtrasDetails = new HashSet<HolidaysExtrasDetails>();
          holidaysExtrasDetails = readExtrasDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setHolidaysExtrasDetails(holidaysExtrasDetails);
        }
        
        
		return holidaysOrder;
      }
      catch(Exception e)
      {
          myLogger.fatal("Failed to populate Holidays Data "+ e);
          throw new BookingEngineDBException("Failed to populate Holidays Data");
      }
		
	}
	
	public AccoOrders populateAccoData(JSONObject requestBodyObject, JSONObject bookRequestHeader, Map<String, PassengerDetails> paxIndexMap, HolidaysOrders holidaysOrder) throws BookingEngineDBException {

      try {
        
      JSONObject accommodationConfig = requestBodyObject.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONObject(JSON_PROP_PKGS_ACCOCONFIG);
  
      AccoOrders order = new AccoOrders();
      
      order.setHolidaysOrders(holidaysOrder);
      
      order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
      
      order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
      order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
      order.setStatus("OnRequest");
      
      order.setSupplierID(requestBodyObject.getString(JSON_PROP_SUPPREF));
      order.setOperationType("insert");
      
      order.setSupplierPrice(accommodationConfig.getJSONObject("supplierAccommodationPriceInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
      order.setSupplierPriceCurrencyCode(accommodationConfig.getJSONObject("supplierAccommodationPriceInfo").getString(JSON_PROP_CURRENCYCODE));
      order.setTotalPrice(accommodationConfig.getJSONObject("totalAccommodationPriceInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
      order.setTotalPriceCurrencyCode(accommodationConfig.getJSONObject("totalAccommodationPriceInfo").getString(JSON_PROP_CURRENCYCODE));

      order.setSuppPriceTaxes(accommodationConfig.getJSONObject("supplierAccommodationPriceInfo").getJSONObject(JSON_PROP_TAXES).toString());
      order.setTotalPriceTaxes(accommodationConfig.getJSONObject("totalAccommodationPriceInfo").getJSONObject(JSON_PROP_TAXES).toString());
      
      
      //TODO: check if we need to put taxes as well here
      Set<AccoRoomDetails> setRoomDetails = new HashSet<AccoRoomDetails>();
      setRoomDetails = readAccommodationDetails(accommodationConfig, order,paxIndexMap);      
      order.setRoomDetails(setRoomDetails);

      return order;
      }
      catch(Exception e)
      {
          
          myLogger.fatal("Failed to populate Acco Data "+ e);
          throw new BookingEngineDBException("Failed to populate Acco Data");
      }
  }

	public Map<String, Object> getPaxIndexMap(JSONObject componentConfigJson, Map<String, Object> paxIndexMap) {

	//MultiValueMap<String, String> paxComponentWise = new LinkedMultiValueMap<String, String>();
    
    JSONArray paxInfoArray = componentConfigJson.getJSONArray(JSON_PROP_PAXINFO);
      
    //List<String> resGuestList = new ArrayList<String>();
      
    for (int j = 0; j < paxInfoArray.length(); j++) 
    {
      JSONObject currentPax = paxInfoArray.getJSONObject(j);
        
      String resGuestRPH = currentPax.getString(JSON_PROP_PKGS_RESGUESTRPH); 

      if(paxIndexMap.containsKey(resGuestRPH))
      {
        continue;
      }
      else
      {
        paxIndexMap.put(resGuestRPH, currentPax);
      }
       
    }

    return paxIndexMap;
}
	
	private Set<PassengerDetails> readPassengerDetails(JSONObject componentConfigJson,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
	  
	  JSONArray paxInfoArray = componentConfigJson.getJSONArray(JSON_PROP_PAXINFO);

      Set<PassengerDetails> HolidaysPassengerDetailsSet = new HashSet<PassengerDetails>();
      
      for (int i = 0; i < paxInfoArray.length(); i++) {
          
        JSONObject currenntPaxDetails = paxInfoArray.getJSONObject(i);
        
        String resGuestRPH = currenntPaxDetails.getString(JSON_PROP_PKGS_RESGUESTRPH); 
        
        PassengerDetails passengerDetailsResponse = new PassengerDetails();

        if(paxIndexMap.containsKey(resGuestRPH))
        {
          passengerDetailsResponse = paxIndexMap.get(resGuestRPH);
        }
        else
        {
          PassengerDetails resGuestDetails = new PassengerDetails();
          
          resGuestDetails.setTitle(currenntPaxDetails.getJSONObject(JSON_PROP_PERSONALDETAILS).getString("title"));
          resGuestDetails.setFirstName(currenntPaxDetails.getJSONObject(JSON_PROP_PERSONALDETAILS).getString("firstName"));
          resGuestDetails.setMiddleName(currenntPaxDetails.getJSONObject(JSON_PROP_PERSONALDETAILS).getString("middleName"));
          resGuestDetails.setLastName(currenntPaxDetails.getJSONObject(JSON_PROP_PERSONALDETAILS).getString("lastName"));
          resGuestDetails.setBirthDate(currenntPaxDetails.getJSONObject(JSON_PROP_PERSONALDETAILS).getString("birthDate"));
          resGuestDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
          resGuestDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE));
          
          resGuestDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
          resGuestDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
          
          resGuestDetails.setDocumentDetails(currenntPaxDetails.getJSONArray(JSON_PROP_DOCUMENTDETAILS).toString());
          resGuestDetails.setSpecialRequests(currenntPaxDetails.getJSONArray(JSON_PROP_SPECIALREQUESTS).toString());
          
          resGuestDetails.setLastModifiedBy("");
          resGuestDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
          resGuestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
          
          resGuestDetails.setRph(currenntPaxDetails.getString(JSON_PROP_PKGS_RESGUESTRPH));
          resGuestDetails.setIsHolidayPassenger(true);
          
          passengerDetailsResponse = savePaxDetails(resGuestDetails,"");
          
          paxIndexMap.put(resGuestRPH, passengerDetailsResponse);
 
        }
          
        HolidaysPassengerDetailsSet.add(passengerDetailsResponse);

      }
      return HolidaysPassengerDetailsSet;
	}

	/*private Set<ActivitiesOrders> readActivityDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {

	  JSONArray activitiesConfigArray = requestBody.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_ACTIVITYCONFIG);
      
      Set<ActivitiesOrders> holidaysActivitiesDetailsSet = new HashSet<ActivitiesOrders>();
      
      for(int i=0;i<activitiesConfigArray.length();i++)
      {
        ActivitiesOrders holidaysActivitiesDetails = new ActivitiesOrders();
        
        JSONObject currentAccommodationObj = activitiesConfigArray.getJSONObject(i);
        
        holidaysActivitiesDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysActivitiesDetails.setConfigType(currentAccommodationObj.getString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysActivitiesDetails.setActivityType(currentAccommodationObj.getString(JSON_PROP_PKGS_ACTIVITYTYPE));
        
        holidaysActivitiesDetails.setSupplierPrice(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysActivitiesDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysActivitiesDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysActivitiesDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysActivitiesDetails.setTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysActivitiesDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysActivitiesDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysActivitiesDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
       
        holidaysActivitiesDetails.setTotalPaxTypeFares(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).toString());
        holidaysActivitiesDetails.setSuppPaxTypeFares(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).toString());
        holidaysActivitiesDetails.setSuppCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_SUPPCOMM).toString());
        holidaysActivitiesDetails.setClientCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_CLIENTCOMM).toString());
        
        holidaysActivitiesDetails.setAvailabilityStatus(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysActivitiesDetails.setName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString("name"));
        holidaysActivitiesDetails.setActivityCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString("code"));
        holidaysActivitiesDetails.setQuantity(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString("quantity"));
        holidaysActivitiesDetails.setDescription(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString("description"));
        holidaysActivitiesDetails.setType(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString("type"));
        
        if(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART)!=null && currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART).length() > 0)
        holidaysActivitiesDetails.setStartDate(readStartDateEndDate(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART)));
        
        holidaysActivitiesDetails.setDuration(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
        
        if(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND)!=null && currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND).length() > 0)
        holidaysActivitiesDetails.setEndDate(readStartDateEndDate(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACTIVITYINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND)));
        
        holidaysActivitiesDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysActivitiesDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysActivitiesDetails.setPaxDetails(paxIDs.toString());
        
        //ActivitiesOrders activitiesOrderResponse = saveActivitiesOrder(holidaysActivitiesDetails);
        
        holidaysActivitiesDetailsSet.add(holidaysActivitiesDetails);
      }
      
        return holidaysActivitiesDetailsSet;
	}
*/

	private Set<InsuranceOrders> readInsuranceDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException  {
	   
      JSONArray insuranceConfigArray = requestBody.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_INSURANCECONFIG);
      
      Set<InsuranceOrders> holidaysInsuranceDetailsSet = new HashSet<InsuranceOrders>();
      
      for(int i=0;i<insuranceConfigArray.length();i++)
      {
        InsuranceOrders holidaysInsuranceDetails = new InsuranceOrders();
        
        JSONObject currentAccommodationObj = insuranceConfigArray.getJSONObject(i);
        
        holidaysInsuranceDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysInsuranceDetails.setConfigType(currentAccommodationObj.getString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysInsuranceDetails.setInsuranceType(currentAccommodationObj.getString(JSON_PROP_PKGS_INSURANCETYPE));
        
        holidaysInsuranceDetails.setSupplierPrice(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysInsuranceDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysInsuranceDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysInsuranceDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysInsuranceDetails.setTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysInsuranceDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysInsuranceDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysInsuranceDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysInsuranceDetails.setSuppCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_SUPPCOMM).toString());
        holidaysInsuranceDetails.setClientCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_CLIENTCOMM).toString());
        
        holidaysInsuranceDetails.setInsDescription(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).getString("name"));
        holidaysInsuranceDetails.setInsId(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).getString("description"));
        holidaysInsuranceDetails.setInsName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).getString("id"));
        
        holidaysInsuranceDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysInsuranceDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysInsuranceDetails.setPaxDetails(paxIDs.toString());
        
        holidaysInsuranceDetailsSet.add(holidaysInsuranceDetails);
      }
      
        return holidaysInsuranceDetailsSet;
	}

	private Set<TransfersOrders> readTransferDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
	   
	  JSONArray transferConfigArray = requestBody.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_TRANSFERCONFIG);
      
      Set<TransfersOrders> holidaysTransferDetailsSet = new HashSet<TransfersOrders>();
      
      for(int i=0;i<transferConfigArray.length();i++)
      {
        TransfersOrders holidaysTransferDetails = new TransfersOrders();
        
        JSONObject currentAccommodationObj = transferConfigArray.getJSONObject(i);
        
        holidaysTransferDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysTransferDetails.setConfigType(currentAccommodationObj.getString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysTransferDetails.setTransferType(currentAccommodationObj.getString(JSON_PROP_PKGS_TRANSFERTYPE));
        
        holidaysTransferDetails.setSupplierTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysTransferDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        /*holidaysTransferDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysTransferDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        */
        
        holidaysTransferDetails.setTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysTransferDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        /*holidaysTransferDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysTransferDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        */
        
        holidaysTransferDetails.setTotalFares(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).toString());
        holidaysTransferDetails.setSuppFares(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).toString());
        
        holidaysTransferDetails.setSuppCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_SUPPCOMM).toString());
        holidaysTransferDetails.setClientCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_CLIENTCOMM).toString());
   
        holidaysTransferDetails.setAvailabilityStatus(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysTransferDetails.setPickUpLocation(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERLOCATION).getString("pickUpLocation"));
        holidaysTransferDetails.setAirportName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERLOCATION).getString("airportName"));
        
        holidaysTransferDetails.setTransferName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERDETAILS).getString("name"));
        holidaysTransferDetails.setTransferDescription(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERDETAILS).getString("description"));
        holidaysTransferDetails.setDepartureCity(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERDETAILS).getString("departureCity"));
        holidaysTransferDetails.setArrivalCity(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERDETAILS).getString("arrivalCity"));
        holidaysTransferDetails.setDepartureDate(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERDETAILS).getString("departureDate"));
        holidaysTransferDetails.setArrivalDate(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TRANSFERDETAILS).getString("arrivalDate"));
        
        holidaysTransferDetails.setStart(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
        holidaysTransferDetails.setDuration(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
        holidaysTransferDetails.setEnd(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TRANSFERINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
        
        holidaysTransferDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysTransferDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysTransferDetails.setPaxDetails(paxIDs.toString());
        
        holidaysTransferDetailsSet.add(holidaysTransferDetails);
      }
      
        return holidaysTransferDetailsSet;
	}


	private Set<HolidaysExtrasDetails> readExtrasDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {

	  JSONArray extrasConfigArray = requestBody.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_EXTRASCONFIG);
      
      Set<HolidaysExtrasDetails> holidaysExtrasDetailsSet = new HashSet<HolidaysExtrasDetails>();
      
      for(int i=0;i<extrasConfigArray.length();i++)
      {
        HolidaysExtrasDetails holidaysExtrasDetails = new HolidaysExtrasDetails();
        
        JSONObject currentAccommodationObj = extrasConfigArray.getJSONObject(i);
        
        holidaysExtrasDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysExtrasDetails.setConfigType(currentAccommodationObj.getString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysExtrasDetails.setExtraType(currentAccommodationObj.getString(JSON_PROP_PKGS_EXTRASTYPE));
        
        holidaysExtrasDetails.setSupplierPrice(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtrasDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysExtrasDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtrasDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysExtrasDetails.setTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtrasDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysExtrasDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtrasDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysExtrasDetails.setSuppCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_SUPPCOMM).toString());
        holidaysExtrasDetails.setClientCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_CLIENTCOMM).toString());
        
        holidaysExtrasDetails.setAvailabilityStatus(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_EXTRASINFO).getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysExtrasDetails.setExtraName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_EXTRASINFO).getJSONObject(JSON_PROP_PKGS_EXTRASDETAILS).getString("name"));
        holidaysExtrasDetails.setExtraCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_EXTRASINFO).getJSONObject(JSON_PROP_PKGS_EXTRASDETAILS).getString("code"));
        holidaysExtrasDetails.setExtraQuantity(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_EXTRASINFO).getJSONObject(JSON_PROP_PKGS_EXTRASDETAILS).getString("quantity"));
        holidaysExtrasDetails.setExtraDescription(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_EXTRASINFO).getJSONObject(JSON_PROP_PKGS_EXTRASDETAILS).getString("description"));
       
        holidaysExtrasDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysExtrasDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysExtrasDetails.setPaxDetails(paxIDs.toString());
        
        holidaysExtrasDetailsSet.add(holidaysExtrasDetails);
      }
      
        return holidaysExtrasDetailsSet;
	}


	private Set<HolidaysExtensionDetails> readExtensionNightDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException{
	  
	  JSONArray extensionNightsConfigArray = requestBody.getJSONObject(JSON_PROP_PKGS_COMPONENTSCONFIG).getJSONArray(JSON_PROP_PKGS_EXTENSIONNIGHTSCONFIG);
      
      Set<HolidaysExtensionDetails> holidaysExtensionNightsDetailsSet = new HashSet<HolidaysExtensionDetails>();
      
      for(int i=0;i<extensionNightsConfigArray.length();i++)
      {
        HolidaysExtensionDetails holidaysExtensionDetails = new HolidaysExtensionDetails();
        
        JSONObject currentAccommodationObj = extensionNightsConfigArray.getJSONObject(i);
        
        holidaysExtensionDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysExtensionDetails.setConfigType(currentAccommodationObj.getString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysExtensionDetails.setExtensionType(currentAccommodationObj.getString(JSON_PROP_PKGS_EXTENSIONTYPE));
        
        holidaysExtensionDetails.setSupplierPrice(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtensionDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysExtensionDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtensionDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysExtensionDetails.setTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtensionDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysExtensionDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtensionDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysExtensionDetails.setSuppCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_SUPPCOMM).toString());
        holidaysExtensionDetails.setClientCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_CLIENTCOMM).toString());
        
        holidaysExtensionDetails.setAvailabilityStatus(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysExtensionDetails.setHotelCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELCODE));
        holidaysExtensionDetails.setHotelName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELNAME));
        holidaysExtensionDetails.setHotelRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelRef"));
        holidaysExtensionDetails.setHotelSegmentCategoryCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelSegmentCategoryCode"));
        
        holidaysExtensionDetails.setAddress(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
        
        holidaysExtensionDetails.setRoomType(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_ACCO_ROOMTYPECODE));
        holidaysExtensionDetails.setRoomCategory(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_ACCO_ROOMCATEGORYCODE));
        holidaysExtensionDetails.setRoomName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString("roomName"));
        holidaysExtensionDetails.setInvBlockCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_PKGS_ACCOBLOCKCODE));
        
        
        holidaysExtensionDetails.setRatePlanName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANNAME));
        holidaysExtensionDetails.setRatePlanCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANCODE));
        holidaysExtensionDetails.setBookingRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
        
        holidaysExtensionDetails.setStart(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
        holidaysExtensionDetails.setDuration(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
        holidaysExtensionDetails.setEnd(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
        
        holidaysExtensionDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysExtensionDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysExtensionDetails.setPaxDetails(paxIDs.toString());
        
        holidaysExtensionNightsDetailsSet.add(holidaysExtensionDetails);
      }
      
        return holidaysExtensionNightsDetailsSet;
	}

	private Set<AccoRoomDetails> readAccommodationDetails(JSONObject accommodationCofnfig, AccoOrders accoOrders, Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
	  JSONArray accommodationConfigArray = accommodationCofnfig.getJSONArray("roomConfig");
	  
	  Set<AccoRoomDetails> holidaysAccoDetailsSet = new HashSet<AccoRoomDetails>();
	  
	  for(int i=0;i<accommodationConfigArray.length();i++)
	  {
	    AccoRoomDetails holidaysAccoDetails = new AccoRoomDetails();
	    
	    JSONObject currentAccommodationObj = accommodationConfigArray.getJSONObject(i);
	    
	    holidaysAccoDetails.setAccoOrders(accoOrders);
	    
	    holidaysAccoDetails.setAccomodationType(currentAccommodationObj.getString(JSON_PROP_PKGS_ACCOTYPE));
	    
	    holidaysAccoDetails.setSupplierPrice(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
	    holidaysAccoDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
	    holidaysAccoDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	    holidaysAccoDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
	    
	    holidaysAccoDetails.setTotalPrice(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysAccoDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        holidaysAccoDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysAccoDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).getJSONArray(JSON_PROP_TAXBREAKUP).toString());
        
        holidaysAccoDetails.setSuppCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_SUPPCOMM).toString());
        holidaysAccoDetails.setClientCommercials(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_CLIENTCOMM).toString());
        
        holidaysAccoDetails.setAvailabilityStatus(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysAccoDetails.setHotelInfo(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONArray(JSON_PROP_PKGS_ACCOHOTELINFO).toString());
        /*holidaysAccoDetails.setHotelCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELCODE));
        holidaysAccoDetails.setHotelName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELNAME));
        holidaysAccoDetails.setHotelRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelRef"));
        holidaysAccoDetails.setHotelSegmentCategoryCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelSegmentCategoryCode"));
        */
        
        holidaysAccoDetails.setAddress(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
        
        holidaysAccoDetails.setRoomType(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_ACCO_ROOMTYPECODE));
        holidaysAccoDetails.setRoomCategory(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_ACCO_ROOMCATEGORYCODE));
        holidaysAccoDetails.setRoomName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString("roomName"));
        holidaysAccoDetails.setCabinNumber(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_PKGS_ACCOCABINNUMBER));
        holidaysAccoDetails.setInvBlockCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_PKGS_ACCOBLOCKCODE));
        
        
        holidaysAccoDetails.setRatePlanName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANNAME));
        holidaysAccoDetails.setRatePlanCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANCODE));
        holidaysAccoDetails.setBookingRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
        
        holidaysAccoDetails.setStart(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
        holidaysAccoDetails.setDuration(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
        holidaysAccoDetails.setEnd(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
        
        holidaysAccoDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysAccoDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysAccoDetails.setPaxDetails(paxIDs.toString());
        
        holidaysAccoDetailsSet.add(holidaysAccoDetails);
	  }
	  
		return holidaysAccoDetailsSet;
	}
	
	private Set<SupplierCommercial> readSupplierCommercials(JSONObject holidayOrdersJson, HolidaysOrders order) {

	  JSONArray suppCommsJsonArray = holidayOrdersJson.getJSONArray(JSON_PROP_SUPPCOMMTOTALS);
      Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
      SupplierCommercial suppCommercials;
      for (int i = 0; i < suppCommsJsonArray.length(); i++) {
          JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

          suppCommercials = new SupplierCommercial();
          suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
          suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
          suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
          suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
          
          suppCommercials.setProduct(JSON_PROP_PRODUCTHOLIDAYS);
          suppCommercials.setOrder(order);
          suppCommercialsSet.add(suppCommercials);

      }
      return suppCommercialsSet;
  }

  private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, HolidaysOrders order) {
       
    Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
    ClientCommercial clientCommercials;
    
    for(int i=0;i<clientCommsJsonArray.length();i++)    {
        
        JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
        
         String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
         String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;      
         String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;      
         String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
        
         boolean companyFlag = (i==0)?true:false;
        
    
    JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_CLIENTCOMMERCIALSTOTAL);
    
    for(int j=0;j<clientComms.length();j++) {
    
    JSONObject clientComm = clientComms.getJSONObject(j);
    
    clientCommercials =new ClientCommercial();
    clientCommercials.setCommercialName(clientComm.getString(JSON_PROP_COMMERCIALNAME));
    clientCommercials.setCommercialType(clientComm.getString(JSON_PROP_COMMERCIALTYPE));
    clientCommercials.setCommercialAmount(clientComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
    clientCommercials.setCommercialCurrency(clientComm.getString(JSON_PROP_COMMERCIALCURRENCY));
    clientCommercials.setClientID(clientID);
    clientCommercials.setParentClientID(parentClientID);
    clientCommercials.setCommercialEntityType(commercialEntityType);
    clientCommercials.setCommercialEntityID(commercialEntityID);
    clientCommercials.setCompanyFlag(companyFlag);

    clientCommercials.setProduct(JSON_PROP_PRODUCTHOLIDAYS);
    clientCommercials.setOrder(order);
    clientCommercialsSet.add(clientCommercials);
    }
    }
    return clientCommercialsSet;
}

  
  private Booking populateBookingData(JSONObject bookRequestJson) throws BookingEngineDBException {
    try {
    Booking order =new Booking();
    order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
    order.setStatus("OnRequest");
    
    order.setLastModifiedBy(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
    order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
    order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
    
    order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
    order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
    order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
    order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
    order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));
    order.setIsHolidayBooking("YES");
    order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
    order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTLANGUAGE));
    order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET));
    
    //TODO: Later check what other details we need to populate for booking table. Also confirm whther BE will get those additional details from Redis.

    order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));
    
    return order;
    }
    catch(Exception e)
    {
        myLogger.fatal("Failed to populate Booking Data "+ e);
        throw new BookingEngineDBException("Failed to populate Booking Data");
    }
  }

  //TODO: WEM needs to confirm what all info they are going to pass to BE
  private Set<PaymentInfo> readPaymentInfo(JSONArray PaymentInfo, Booking booking) {

      Set<PaymentInfo> paymentInfoSet = new HashSet<PaymentInfo>();

      for (int i = 0; i < PaymentInfo.length(); i++) {
          PaymentInfo paymentInfo = new PaymentInfo();
          JSONObject currentPaymentInfo = PaymentInfo.getJSONObject(i);
          paymentInfo.setPaymentMethod(currentPaymentInfo.getString(JSON_PROP_PAYMENTMETHOD));
          paymentInfo.setPaymentAmount(currentPaymentInfo.getString(JSON_PROP_PAYMENTAMOUNT));
          paymentInfo.setPaymentType(currentPaymentInfo.getString(JSON_PROP_PAYMENTTYPE));
          paymentInfo.setAmountCurrency(currentPaymentInfo.getString(JSON_PROP_AMOUNTCURRENCY));
          paymentInfo.setCardType(currentPaymentInfo.getString(JSON_PROP_CARDTYPE));
          paymentInfo.setCardNumber(currentPaymentInfo.getString(JSON_PROP_CARDNO));
          paymentInfo.setCardExpiry(currentPaymentInfo.getString(JSON_PROP_CARDEXPIRY));
          paymentInfo.setEncryptionKey(currentPaymentInfo.getString(JSON_PROP_ENCRYPTIONKEY));
          paymentInfo.setToken(currentPaymentInfo.getString(JSON_PROP_TOKEN));
          paymentInfo.setAccountType(currentPaymentInfo.getString(JSON_PROP_ACCOUNTTYPE));
          paymentInfo.setBooking(booking);
          paymentInfoSet.add(paymentInfo);

      }
      return paymentInfoSet;
  }
  
  public String processBookResponse(JSONObject bookResponseJson) throws BookingEngineDBException {

    //TODO: We need to put logic to update status for booking based on the statuses of individual products.
    
    Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
    if(booking==null)
    {
        myLogger.warn(String.format("Holiday Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject("responseBody").getString("bookID")));
        response.put("ErrorCode","BE_ERR_HOLIDAYS_004");
        response.put("ErrorMsg", BE_ERR_HOLIDAYS_004);
        return response.toString();
    }
    else
    {
    List<HolidaysOrders> orders = holidaysRepository.findByBooking(booking);
    if(orders.size()==0)
    {
        myLogger.warn(String.format("Holiday Booking Response could not be populated since no holiday orders found for req with bookID %s", bookResponseJson.getJSONObject("responseBody").getString("bookID")));
        response.put("ErrorCode", "BE_ERR_HOLIDAYS_005");
        response.put("ErrorMsg", BE_ERR_HOLIDAYS_005);
        return response.toString();
    }
    else
    {
    int count =0;
    for(HolidaysOrders order:orders) {
        String prevOrder = order.toString();
        order.setStatus("confirmed");
        order.setSupp_booking_reference(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES).getJSONObject(count).getString(JSON_PROP_BOOKREFID));
        count++;
        order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        //For Accommodation Orders
        for(AccoRoomDetails room: order.getAccoOrders().getRoomDetails()) {
            
            room.setStatus("Confirmed");
        }
        
        //For Activities Orders
        for(ActivitiesOrders activitiesOrder: order.getActivitiesOrders()) {
          
          activitiesOrder.setStatus("Confirmed");
        }
        
        //For Transfers Orders
        for(TransfersOrders transfersOrder: order.getTransfersOrders()) {
          
          transfersOrder.setStatus("Confirmed");
        }
        
        //For Insurance Orders
        for(InsuranceOrders insuranceOrder: order.getInsuranceOrders()) {
          
          insuranceOrder.setStatus("Confirmed");
        }
        
        //For Extension Nights
        for(HolidaysExtensionDetails holidaysExtensionDetail: order.getHolidaysExtensionDetails()) {
          
          holidaysExtensionDetail.setStatus("Confirmed");
        }
        
        //For Extras
        for(HolidaysExtrasDetails holidaysExtrasDetail: order.getHolidaysExtrasDetails()) {
          
          holidaysExtrasDetail.setStatus("Confirmed");
        }
        
        saveHolidaysOrder(order, prevOrder);
    }
    myLogger.info(String.format("Acco Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject("responseBody").getString("bookID"),bookResponseJson.toString()));
    return "SUCCESS";
    }
    }
}
  
    //Have To make Method for update and cancellation - 
    //Make methods - processAmClRequest, processAmClResponse, fullCancel, updateRoom, updatePaxDetails, saveAccoAmCl

	public Booking saveBookingOrder(Booking order,String prevOrder) {
		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj,prevOrder);
	}

	
	private HolidaysOrders saveHolidaysOrder(HolidaysOrders order, String prevOrder) {
		HolidaysOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, HolidaysOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return holidaysRepository.saveOrder(orderObj, prevOrder);
		
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException 
    {
        PassengerDetails orderObj = null;
        try {
            orderObj = CopyUtils.copy(pax, PassengerDetails.class);

        }
        catch (InvocationTargetException | IllegalAccessException e) {
             myLogger.fatal("Error while saving Holidays Passenger order object : " + e);
             //myLogger.error("Error while saving order object: " + e);
            throw new BookingEngineDBException("Failed to save order object");
        }
        return passengerRepository.saveOrder(orderObj,prevOrder);
    }

	private ZonedDateTime readStartDateEndDate(String stringInDate) {
      try {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          Instant instant = sdf.parse(stringInDate).toInstant();

          // TODO: done
          ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC );
//        String stringOutDate = zonedDateTime.toString();
          return zonedDateTime;
      } catch (ParseException e) {
          e.printStackTrace();
      }
      return null;
  }
}
