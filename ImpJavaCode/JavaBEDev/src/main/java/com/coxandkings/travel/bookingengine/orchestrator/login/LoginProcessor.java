package com.coxandkings.travel.bookingengine.orchestrator.login;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.common.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.config.RedisConfig;
import com.coxandkings.travel.bookingengine.userctx.UserContext;

import redis.clients.jedis.Jedis;

public class LoginProcessor implements LoginConstants {

	public static String process(JSONObject reqJson) {
		try {
			
			JSONObject reqHeader = reqJson.getJSONObject(JSON_PROP_REQHEADER);
			JSONObject reqBody = reqJson.getJSONObject(JSON_PROP_REQBODY);
			JSONObject loginRes =new JSONObject();
		
			
			String redisKey = getRedisKey(reqBody);			
			Jedis redisConn = RedisConfig.getRedisConnectionFromPool();
			Map<String, String> loginResMap = redisConn.hgetAll(redisKey);
		    
		   if(loginResMap!=null &&loginResMap.size()!=0) {
			   
			   	loginRes.put(VERSION, loginResMap.get(VERSION));
			   	loginRes.put(USER, loginResMap.get(USER));
			   	loginRes.put(TOKEN, loginResMap.get(TOKEN));
			   	loginRes.put(LAST_UPDATED, loginResMap.get(LAST_UPDATED));
			   	loginRes.put(LOGIN_TIME, loginResMap.get(LOGIN_TIME));
			   	loginRes.put(EXPIRE_IN, loginResMap.get(EXPIRE_IN));
			   	loginRes.put(STATUS, loginResMap.get(STATUS));
			   	loginRes.put(ID, loginResMap.get(ID));
			   
		   }
		   
		   else {
			loginRes = HTTPServiceConsumer.consumeJSONService("MDM/Login", MDMConfig.getLoginServiceURL(),
					MDMConfig.getmHttpHeaders(), reqBody);
			
			//TODO: also handle for http code 204 response as we dont have user in the cache and MDM is saying user is logged in.
			if(loginRes==null)
				return "Error occured while logging in";
		
			loginResMap =   new HashMap<String,String>();
			
			//TODO: check what all details we need to put in redis for login response
			loginResMap.put(VERSION, loginRes.get(VERSION).toString());
			loginResMap.put(USER, loginRes.get(USER).toString());
			loginResMap.put(TOKEN, loginRes.get(TOKEN).toString());
			loginResMap.put(LAST_UPDATED, loginRes.get(LAST_UPDATED).toString());
			loginResMap.put(LOGIN_TIME, loginRes.get(LOGIN_TIME).toString());
			loginResMap.put(EXPIRE_IN, loginRes.get(EXPIRE_IN).toString());
			loginResMap.put(STATUS, loginRes.get(STATUS).toString());
			loginResMap.put(ID, loginRes.get(ID).toString());

			
			redisConn.hmset(redisKey, loginResMap);
			redisConn.pexpire(redisKey, (long) (MDMConfig.getmRedisTTLMins() * 60 * 1000));
		   
		   }
			RedisConfig.releaseRedisConnectionToPool(redisConn);

			UserContext usrCtx = UserContext.getUserContextForSession(reqHeader);

			//TODO: we will confirm from WEM that what all fields they need from us. Also on what condition we will add thode details in the login response
			loginRes.put(USER_CONTEXT, new JSONObject(usrCtx.toString()));
			
			JSONObject resJson = new JSONObject();
			resJson.put(JSON_PROP_RESHEADER, reqHeader);
			resJson.put(JSON_PROP_RESBODY, loginRes);
			
			return resJson.toString();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO: change it later :)
		return "Failure";

	}
	
	
	private static String getRedisKey(JSONObject reqJson) {
		
		String redisKey = String.format("%s%c%s%c%s",reqJson.optString("username"),KEYSEPARATOR,reqJson.optString("password"),KEYSEPARATOR,reqJson.optString("clientType"));
		
		return redisKey;
		
	}

}