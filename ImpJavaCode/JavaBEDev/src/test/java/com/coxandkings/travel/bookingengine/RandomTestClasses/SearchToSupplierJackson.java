package com.coxandkings.travel.bookingengine.RandomTestClasses;

        import java.io.FileReader;

        import java.io.IOException;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

        import com.fasterxml.jackson.core.JsonProcessingException;
        import com.fasterxml.jackson.core.type.TypeReference;
        import com.fasterxml.jackson.databind.JsonNode;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import com.fasterxml.jackson.databind.node.ArrayNode;
        import com.fasterxml.jackson.databind.node.ObjectNode;


        import springfox.documentation.spring.web.json.Json;

public class SearchToSupplierJackson {

    JsonNode supplierresponseJson;
    ObjectMapper mapper = new ObjectMapper();

    public void readJson(){
        try {
            long inTime=System.currentTimeMillis();
            supplierresponseJson =	mapper.readTree(new FileReader("src/main/resources/JSON_SearchRS.json"));


            ArrayNode otaSearchWrapper = mapper.createArrayNode();
            JsonNode responseBody = mapper.createObjectNode();

            //creating requestnode,adding lookup and commands[]
            ObjectNode supplierReq=new ObjectNode(mapper.getNodeFactory());
            supplierReq.put("lookup", "suppliertransactionalsession");
            supplierReq.putArray("commands");


            ArrayNode commands= (ArrayNode) supplierReq.get("commands");
            ObjectNode command=new ObjectNode(mapper.getNodeFactory());
            ObjectNode insert=new ObjectNode(mapper.getNodeFactory());
            ObjectNode fireallrules=new ObjectNode(mapper.getNodeFactory());

            for(int i=0;i<1;i++)
            {
                command.putObject("insert");
                insert= (ObjectNode) command.get("insert");
                insert.put("out-identifier", "Response");
                insert.put("return-object", "true");
                insert.put("entry-point", "DEFAULT");
                insert.putObject("object");
                ObjectNode object=(ObjectNode) insert.get("object");
                object.putObject("cnk.air_commercialscalculationengine.suppliertransactionalrules.Root");
                ObjectNode cnkRoot=(ObjectNode) object.get("cnk.air_commercialscalculationengine.suppliertransactionalrules.Root");
                cnkRoot.put("transactional","true");


                //header
                cnkRoot.putObject("header");
                ObjectNode header = (ObjectNode) cnkRoot.get("header");
                header = createheader(header);



                //businessRuleIntake
                cnkRoot.putArray("businessRuleIntake");
                ArrayNode businessRuleIntakearray = (ArrayNode) cnkRoot.get("businessRuleIntake");
                int suppcnt=1;
                for(int j=0;j<suppcnt;j++) {
                    ObjectNode businessRuleIntake=new ObjectNode(mapper.getNodeFactory());


                    //This element need to be set instead of put
                    //common Element
                    businessRuleIntake.putObject("commonElements");
                    ObjectNode commonElements = (ObjectNode) businessRuleIntake.get("commonElements");
                    commonElements = createcommonElements(commonElements);


                    //This element need to be set instead of put
                    //advancedDefinition
                    businessRuleIntake.putObject("advancedDefinition");
                    ObjectNode advancedDefinition = (ObjectNode) businessRuleIntake.get("advancedDefinition");
                    advancedDefinition = createadvancedDefinition(advancedDefinition);



                    //journeyDetails
                    businessRuleIntake.putArray("journeyDetails");
                    ArrayNode journeyDetailsarray = (ArrayNode) businessRuleIntake.get("journeyDetails");
                    responseBody= supplierresponseJson.get("ResponseBody");

                    otaSearchWrapper=(ArrayNode) responseBody.get("OTA_AirLowFareSearchRSWrapper");
                    ObjectNode otaSearchWrapper1=(ObjectNode) otaSearchWrapper.get(i);
                    ObjectNode oTA_AirLowFareSearchRS=(ObjectNode) otaSearchWrapper1.get("OTA_AirLowFareSearchRS");
                    ObjectNode pricedItineraries=(ObjectNode) oTA_AirLowFareSearchRS.get("PricedItineraries");
                    ArrayNode pricedIternary=(ArrayNode) pricedItineraries.get("PricedItinerary");

                    int PricedItineraryLength =pricedIternary.size();



                    for(int k=0;k<PricedItineraryLength;k++) {

                        //use set instead of put
                        ObjectNode journeyDetails=new ObjectNode(mapper.getNodeFactory());

                        journeyDetails = createjourneyDetails(journeyDetails);


                        ObjectNode priceIternary1=(ObjectNode) pricedIternary.get(k);
                        ArrayNode AirItineraries = (ArrayNode) priceIternary1.get("AirItinerary");
                        ObjectNode AirItinerary=(ObjectNode) AirItineraries.get(0);


                        ObjectNode OriginDestinationOptions = (ObjectNode) AirItinerary.get("OriginDestinationOptions");
                        ArrayNode OriginDestinationOption = (ArrayNode) OriginDestinationOptions.get("OriginDestinationOption");

                        int odoLength = OriginDestinationOption.size();
                        int flightSegmentLength=0;



                        //flightDetails

                        journeyDetails.putArray("flightDetails");
                        ArrayNode flightDetailsArray = (ArrayNode) journeyDetails.get("flightDetails");
                        for(int m=0;m<odoLength;m++) {

                            ObjectNode originDestinationOption1= (ObjectNode) OriginDestinationOption.get(m);
                            ArrayNode FlightSegmentArray=(ArrayNode) originDestinationOption1.get("FlightSegment");
                            int flightSegLength=flightSegmentLength+FlightSegmentArray.size();



                            for(int n=0;n<flightSegLength;n++) {

                                ObjectNode flightDetail=new ObjectNode(mapper.getNodeFactory());
                                JsonNode flightSegLen = OriginDestinationOption.get(m).get("FlightSegment").get(n);
                                flightDetail = createflightDetail(flightDetail,flightSegLen);

                                flightDetailsArray.add(flightDetail);


                            }


                            //passengerDetails
                            journeyDetails.putArray("passengerDetails");
                            ArrayNode passengerDetailsNodeArray=(ArrayNode) journeyDetails.get("passengerDetails");

                            int ptcSize = pricedIternary.get(k).get("AirItineraryPricingInfo").get(0).get("PTC_FareBreakdowns").get(0).get("PTC_FareBreakdown").size();
                            for(int p=0;p<ptcSize;p++) {

                                JsonNode passengerLen  =  pricedIternary.get(k).get("AirItineraryPricingInfo").get(0).get("PTC_FareBreakdowns").get(0).get("PTC_FareBreakdown").get(p);

                                ObjectNode passengerDetail = new ObjectNode(mapper.getNodeFactory());
                                passengerDetail = createPassengerDetail(passengerDetail,passengerLen);



                                //filling fareBreakUp
                                ObjectNode fareBreakup=(ObjectNode) passengerDetail.get("fareBreakUp");
                                fareBreakup.set("baseFare",passengerLen.get("PassengerFare").get("BaseFare").get("@Amount"));
                                fareBreakup.putArray("taxDetails");
                                ArrayNode taxDetails=(ArrayNode) fareBreakup.get("taxDetails");


                                int taxDetailLenth=passengerLen.get("PassengerFare").get("Taxes").get("Tax").size();
                                for(int q=0;q<taxDetailLenth;q++)
                                {
                                    JsonNode taxLen = passengerLen.get("PassengerFare").get("Taxes").get("Tax").get(q);
                                    ObjectNode taxDetail=new ObjectNode(mapper.getNodeFactory());

                                    taxDetail = createtaxDetail(taxDetail,taxLen);
                                    taxDetails.add(taxDetail);

                                }
                                passengerDetailsNodeArray.add(passengerDetail);
                            }
                        }



                        journeyDetailsarray.add(journeyDetails);

                    }



                    businessRuleIntakearray.add(businessRuleIntake);



                    commands.add(command);
                }






                long outTime=System.currentTimeMillis();
                System.out.println("TotalTime"+(outTime-inTime));
                System.out.println(supplierReq.toString());

		/*	XmlMapper xmlMapper = new XmlMapper();
				//
			   String browsersAsXml = xmlMapper.writeValueAsString(supplierReq.toString());

			   System.out.println("XML OP"+browsersAsXml);
			*/
            }
        }catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }





    private ObjectNode createtaxDetail(ObjectNode taxDetail1, JsonNode taxLen) {
        ObjectNode taxDetail = taxDetail1;
        taxDetail.set("taxName",taxLen.get("@TaxName"));
        taxDetail.set("taxValue",taxLen.get("@Amount"));


        return taxDetail;
    }





    private ObjectNode createflightDetail(ObjectNode flightDetail1, JsonNode flightSegLen) {
        ObjectNode flightDetail = flightDetail1;
        flightDetail.set("flightNumber",flightSegLen.get("OperatingAirline").get("@FlightNumber"));
        flightDetail.set("flightTiming",flightSegLen.get("@DepartureDateTime"));
        flightDetail.put("cabinClass", "Economy");
        flightDetail.put("rbd", "s");

        return flightDetail;
    }





    private ObjectNode createheader(ObjectNode header1) {
        ObjectNode header = header1;
        header.put("transactionID", "AIRARABIA_RKD2");
        header.put("sessionID", "AIRARABIA_RKD2");
        header.put("operationName", "Search");
        //to copy value from one json file to another use "set" instead of "put".
        header.set("userID", supplierresponseJson.get("ResponseHeader").get("UserID"));

        return header;
    }





    private ObjectNode createjourneyDetails(ObjectNode journeyDetails1) {
        ObjectNode journeyDetails = journeyDetails1;
        journeyDetails.put("viaContinent","Asia");
        journeyDetails.put("viaCountry","India");
        journeyDetails.put("viaCity","HYD");
        journeyDetails.put("flightType", "Via");
        journeyDetails.put("flightLineType", "Online");
        journeyDetails.put("codeShareFlightIncluded", "true");
        journeyDetails.put("travelProductName", "Flights");

        return journeyDetails;
    }





    private ObjectNode createcommonElements(ObjectNode commonElements1) {
        ObjectNode commonElements = commonElements1;
        commonElements.put("supplier", "Indigo");
        commonElements.put("supplierMarket", "India");
        commonElements.put("contractValidity", "2017-11-10T00:00:00");
        commonElements.put("productName", "Flights");
        commonElements.put("segment", "Active");
        commonElements.put("clientType", "B2B");
        commonElements.put("clientGroup", "TravelAgent");
        commonElements.put("clientName", "AkbarTravels");
        commonElements.put("iatanumber", "123455");

        return commonElements;
    }



    private ObjectNode createadvancedDefinition(ObjectNode advancedDefinition1) {
        ObjectNode advancedDefinition=advancedDefinition1;
        advancedDefinition.put("ticketingDate", "2017-02-10T00:00:00");
        advancedDefinition.put("travelType", "SOTO");
        advancedDefinition.put("journeyType", "OneWay");
        advancedDefinition.put("fromContinent", "Asia");
        advancedDefinition.put("fromCountry", "India");
        advancedDefinition.put("fromCity", "Mumbai");
        advancedDefinition.put("toContinent", "Asia");
        advancedDefinition.put("toCountry", "India");
        advancedDefinition.put("toCity", "Delhi");
        advancedDefinition.put("connectivitySupplierType", "Indigo");
        advancedDefinition.put("connectivitySupplier", "LCC");
        advancedDefinition.put("credentialsName", "Indigo");
        advancedDefinition.put("bookingType", "Online");
        return advancedDefinition;
    }



    private ObjectNode createPassengerDetail(ObjectNode passengerDetail1,JsonNode passengerLen) {
        ObjectNode passengerDetail = passengerDetail1;
        passengerDetail.set("passengerType", passengerLen.get("PassengerTypeQuantity").get("@Code"));
        passengerDetail.putArray("fareBasisValue");
        ArrayNode passengerDetails=(ArrayNode) passengerDetail.get("fareBasisValue");
        passengerDetail.put("dealCode", "");
        passengerDetail.set("totalFare",passengerLen.get("PassengerFare").get("TotalFare").get("@Amount"));
        passengerDetail.putObject("fareBreakUp");

        return passengerDetail;
    }



    public static void main(String[] args) {

        SearchToSupplierJackson searchToSupplierJackson =
                new SearchToSupplierJackson();
        searchToSupplierJackson.readJson();
    }



}