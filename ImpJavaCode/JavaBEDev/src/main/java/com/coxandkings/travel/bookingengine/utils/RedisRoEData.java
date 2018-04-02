package com.coxandkings.travel.bookingengine.utils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
//import org.json.JSONArray;
//import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.mongodb.DBCollection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class RedisRoEData {

	private static final Logger logger = LogManager.getLogger(RedisRoEData.class);
	private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static final String COLL_ROE = "roe";
	//private static final String JSON_PROP_ROE_DATEARR = "dateWiseRoe";
	//private static final String JSON_PROP_ROE_EFFDATE = "effectiveDate";
	//private static final String JSON_PROP_ROEVALUE = "roeValue";
	//private static final boolean ASCEN_SORT=true;

	public static void loadConfig() {
		insertDailyROEData();
	}

	@SuppressWarnings("unchecked")
	private static void insertDailyROEData() {
		
		ArrayList<Document> roeDocLst = getROEData("Daily ROE");
		Map<String,TreeMap<String,String>> roeMap = getDateSortedROEMap(roeDocLst);
		
		//Jedis jedis = null;
		//Pipeline pipeline = null;
		try ( Jedis jedis = RedisConfig.getRedisConnectionFromPool();
			  Pipeline pipeline = jedis.pipelined(); ) {
			//JSONObject rootJson=new JSONObject(),dateJson;
			//JSONArray roeDateArr;
			//TreeMap<String, String> roeDateMap;

			//While retrieving jedis returns hashmap where order of keys is not guaranteed.Hence converted to json
			/*for(Entry<String, TreeMap<String, String>> roeEntry:roeMap.entrySet()) {
				
				rootJson = new JSONObject();
				roeDateArr = new JSONArray();
				rootJson.put(JSON_PROP_ROE_DATEARR, roeDateArr);
				roeDateMap = roeEntry.getValue();
				
				int index = roeDateMap.size()-1;
				for( Entry<String, String> roedateEntry:roeDateMap.entrySet()) {
					
					dateJson = new JSONObject();
					dateJson.put(JSON_PROP_ROE_EFFDATE, roedateEntry.getKey());
					dateJson.put(JSON_PROP_ROEVALUE, roedateEntry.getValue());
					
					if(ASCEN_SORT) {
						roeDateArr.put(dateJson);
						continue;
					}
					
					roeDateArr.put(index,dateJson);
					index--;

				}

				pipeline.set(roeEntry.getKey(), rootJson.toString());
			}*/
			
			//here data needs to be sorted again while retrieveing
			for(Entry<String, TreeMap<String, String>> roeEntry:roeMap.entrySet()) {
				pipeline.hmset(roeEntry.getKey(), roeEntry.getValue());
			}
			
			pipeline.sync(); 
			pipeline.close();
		}
		catch (Exception x) {
		}
//		finally {
//			if (pipeline != null) {
//				try { 
//					pipeline.close(); 
//				}
//				catch (Exception x) { 
//					// TODO: Check if it is safe to eat this exception 
//				}
//			}
//			if (jedis != null) {
//				RedisConfig.releaseRedisConnectionToPool(jedis);
//			}
//		}
	}


	@SuppressWarnings("unchecked")
	private static Map<String,TreeMap<String,String>> getDateSortedROEMap(ArrayList<Document> roeDocLst){
		
		//Note:If Roe type other than daily needs to be loaded expire date and roeValueType(sellinROE,ROE) depending on type needs to be incorporated
		HashMap<String,TreeMap<String,String>> roeMap = new HashMap<String,TreeMap<String,String>>();
		ArrayList<Document> roeDataLst,mrktDataLst;
		Document roeData;
		String fromCcy,toCcy,roeVal,mrktName,roeKey;
		Object effDate;
		//TreeMap<String,String> roeDateMap;
		Calendar cal;
		
		for(Document roeDoc:roeDocLst) {
			
			roeDataLst= (ArrayList<Document>) roeDoc.getOrDefault("ROE", new ArrayList<Document>());
			
			effDate = roeDoc.get("effectiveFrom");
			//Ideally this should not be done as date in ui seems to be in ISO date format
			//But prev documents saved have date defined as year,month and day in a document.
			if(effDate instanceof Date) {
				effDate = mDateFormat.format((Date)effDate);
			}
			else if(effDate instanceof Document) {
				cal = Calendar.getInstance();
				cal.set(((Document) effDate).getInteger("year"),((Document) effDate).getInteger("month")-1,((Document) effDate).getInteger("day"));
				effDate =  mDateFormat.format(cal.getTime());
			}
			else {
				//As per BRD eff Date is mandatory but not in UI.This should not reach ideally
				logger.warn(String.format("Effective date not found for ROE document with id: %s",roeDoc.get(DBCollection.ID_FIELD_NAME)));
				continue;
			}
			
			for(int i=0;i<roeDataLst.size();i++) {
				
				roeData = roeDataLst.get(i);
				fromCcy = roeData.getString("fromCurrency");
				toCcy = roeData.getString("toCurrency");
				if(!roeData.containsKey("sellingROE")) {
					logger.warn(String.format("No selling ROE value found for document with id: %s, fromCurrency: %s, toCurrency: %s",roeDoc.get(DBCollection.ID_FIELD_NAME),fromCcy,toCcy));
					//This should never happen as selling roe is mandatory when daily roe is defined.This check needs to be added in UI!!!
					continue;
				}
				roeVal = roeData.get("sellingROE").toString();
				mrktDataLst= (ArrayList<Document>) roeDoc.getOrDefault("companyMarkets", new ArrayList<Document>());
				int j=0;
				do {
					//TODO:As per BRD only one market should be present for this type of roe.But it is disabled in UI.Need to change!!!
					mrktName = mrktDataLst.isEmpty()?"":(String) mrktDataLst.get(j).getOrDefault("name", "");
					roeKey = getROEKey(fromCcy, toCcy, mrktName);
					if(!roeMap.containsKey(roeKey))
						roeMap.put(roeKey, new TreeMap<String,String>());
					roeMap.get(roeKey).put(effDate.toString(), roeVal);
					j++;
				}while(j<mrktDataLst.size());
				
				
			}
		}
		return roeMap;
	}
	
	private static ArrayList<Document> getROEData(String roeType) {

		MongoCollection<Document> roeCollection = MDMConfig.getCollection(COLL_ROE);
		ArrayList<Document> result = new ArrayList<Document>();
		Map<String, Object> props = new HashMap<String, Object>();
		
		props.put("roeType", roeType);
		props.put("deleted", false);
		FindIterable<Document> res = roeCollection.find( new Document(props));
		MongoCursor<Document> roeDataCrsr = res.iterator();
		while (roeDataCrsr.hasNext()) {
			result.add(roeDataCrsr.next());
		}
		roeDataCrsr.close();

		return result;
	}


	public static BigDecimal getRateOfExchange(String fromCcy, String toCcy, String market) {
		try (Jedis redisConn = RedisConfig.getRedisConnectionFromPool();) {
			//Retrieve map and sort
			Map<String,String> roeDateMap = redisConn.hgetAll(getROEKey(fromCcy, toCcy, market));
			if(roeDateMap == null || roeDateMap.isEmpty())
				return new BigDecimal(1);
			TreeMap<String,String> sortedRoeDateMap = new TreeMap<String,String>(roeDateMap);
			Entry<String, String> roeEntry = sortedRoeDateMap.floorEntry(mDateFormat.format(new Date()));
			return (roeEntry != null) ? Utils.convertToBigDecimal(roeEntry.getValue(), 1) : new BigDecimal(1);
		}
	}
	
	public static BigDecimal getRateOfExchange(String fromCcy, String toCcy) {
		return getRateOfExchange(fromCcy, toCcy, "");
	}

	private static String getROEKey(String fromCcy, String toCcy, String market) {
		return String.format("%s|%s|%s", fromCcy,toCcy,market);
	}

}
