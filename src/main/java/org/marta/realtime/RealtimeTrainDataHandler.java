package org.marta.realtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.marta.RealTimeBase;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

public class RealtimeTrainDataHandler extends RealTimeBase implements RequestHandler<Object, Object> {

	public Object handleRequest(final Object input, final Context context) {
		Map<String, Map<String, String>> metaMap = new HashMap<String, Map<String, String>>();
		Map<String, Double> metaTimer = new HashMap<String, Double>();
		long unixTimeAct = (System.currentTimeMillis() / 1000L);
		long unixTime = unixTimeAct + 1200;
		JsonArray trainsArray = null;

		try {
			trainsArray = downloadTrainDetails();
			System.out.println("Found trains for db : " + trainsArray.size());
			for (int i = 0; i < trainsArray.size(); ++i) {

				JsonObject obj = trainsArray.get(i).getAsJsonObject();
				String station = obj.get("STATION").getAsString();
				String waiting = obj.get("WAITING_TIME").getAsString();
				String direction = obj.get("DIRECTION").getAsString();
				String destination = obj.get("DESTINATION").getAsString();
				String line = obj.get("LINE").getAsString();
				String msgtime = obj.get("EVENT_TIME").getAsString();
			
				
				Map<String, String> meta = new HashMap<String, String>();

				meta.put("DIRECTION", direction);
				meta.put("MSGTIME", msgtime);
			}
		} catch (Exception e) {
			System.out.println("Unable to add trains execption : " + e.getMessage());
		}
		JedisPool pool = getPoolFromRedis();
		if (pool == null) {
			System.out.println("Unable to get pool connection : ");
		} else {

			if (trainsArray != null && trainsArray.size() > 0) {
				addMetaToRedis(pool, metaMap);
				addMetaTimer(pool, metaTimer);
			}
			deleteExpiredEntities(pool, unixTimeAct);
			pool.close();
		}
		return null;
	}

	private JsonArray downloadTrainDetails() throws IOException {
		JsonParser parser = new JsonParser();
		URL testURL = new URL("http://developer.itsmarta.com/RealtimeTrain/RestServiceNextTrain/GetRealtimeArrivals?apiKey=69c85439-3e90-44b2-9cd3-303520c01585");
		BufferedReader reader = new BufferedReader(new InputStreamReader(testURL.openStream()));
		JsonArray trainsArray = parser.parse(IOUtils.toString(reader)).getAsJsonArray();
		return trainsArray;
	}


	private void addMetaToRedis(JedisPool pool, Map<String, Map<String, String>> metaMap) {
		Jedis jedis = getJedisFromPool(pool);
		Pipeline p = jedis.pipelined();
		for (Entry<String, Map<String, String>> loc : metaMap.entrySet()) {
			p.hmset(loc.getKey(), loc.getValue());
		}
		p.syncAndReturnAll();
		jedis.close();
		System.out.println("Added location meta to db : " + metaMap.size());
	}

	private void addMetaTimer(JedisPool pool, Map<String, Double> metaTimer) {
		Jedis jedis = pool.getResource();
		jedis.zadd("mtraintime", metaTimer);
		jedis.close();
	}

	private void deleteExpiredEntities(JedisPool pool, long unixtime) {
		Jedis jedis = getJedisFromPool(pool);
		Set<String> listtoremove = jedis.zrangeByScore("mtraintime", Double.NEGATIVE_INFINITY, (double) unixtime);
		long count = 0l;
		long count1 = 0l;
		long count2 = 0l;
		for (String remove : listtoremove) {
			count = count + jedis.del(remove);
			count2 = count2 + jedis.zrem("mtraintime", remove);
		}
		jedis.close();
		System.out.println("Removed train data from db : " + listtoremove.size() + " Removed :" + count + ":"
				+ count1 + ":" + count2);
	}
}
