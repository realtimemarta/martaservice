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

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

public class RealtimeDataHandler extends RealTimeBase implements RequestHandler<Object, Object> {

	@SuppressWarnings("serial")
	private static final HashMap<String, String> dir = new HashMap<String, String>() {
		{
			put("Southbound", "S");
			put("Northbound", "N");
			put("Eastbound", "E");
			put("Westbound", "W");
		}
	};

	public Object handleRequest(final Object input, final Context context) {
		Map<String, GeoCoordinate> coordinateMap = new HashMap<String, GeoCoordinate>();
		Map<String, Map<String, String>> metaMap = new HashMap<String, Map<String, String>>();
		Map<String, Double> metaTimer = new HashMap<String, Double>();
		long unixTimeAct = (System.currentTimeMillis() / 1000L);
		long unixTime = unixTimeAct + 1200;
		JsonArray vehicleArray = null;

		try {
			vehicleArray = downloadVehicleDetails();
			System.out.println("Found busses for db : " + vehicleArray.size());
			for (int i = 0; i < vehicleArray.size(); ++i) {

				JsonObject obj = vehicleArray.get(i).getAsJsonObject();
				String vehicleId = obj.get("VEHICLE").getAsString();
				String tripId = obj.get("TRIPID").getAsString();
				String direction = obj.get("DIRECTION").getAsString();
				String addr = obj.get("ADHERENCE").getAsString();
				String route = obj.get("ROUTE").getAsString();
				double lat = obj.get("LATITUDE").getAsDouble();
				double lon = obj.get("LONGITUDE").getAsDouble();
				String msgtime = obj.get("MSGTIME").getAsString();
				String uuid = route + "-" + ((tripId != null && !tripId.isEmpty()) ? tripId : vehicleId);
				if (lat < -90 || lat > 90 || lon < -180 || lon > 180)
					continue;
				coordinateMap.put(uuid, new GeoCoordinate(lon, lat));
				Map<String, String> meta = new HashMap<String, String>();
				meta.put("TRIPID", tripId);
				meta.put("ROUTE", route);
				meta.put("ADHERENCE", addr);
				meta.put("DIRECTION", dir.get(direction));
				meta.put("MSGTIME", msgtime);
				metaMap.put(uuid, meta);
				metaTimer.put(uuid, (double) unixTime);
			}
		} catch (Exception e) {
			System.out.println("Unable to add busses execption : " + e.getMessage());
		}
		JedisPool pool = getPoolFromRedis();
		if (pool == null) {
			System.out.println("Unable to get pool connection : ");
		} else {

			if (vehicleArray != null && vehicleArray.size() > 0) {
				addLocationsToRedis(pool, coordinateMap);
				addMetaToRedis(pool, metaMap);
				addMetaTimer(pool, metaTimer);
			}
			deleteExpiredEntities(pool, unixTimeAct);
			pool.close();
		}
		return null;
	}

	private JsonArray downloadVehicleDetails() throws IOException {
		JsonParser parser = new JsonParser();
		URL testURL = new URL("http://developer.itsmarta.com/BRDRestService/BRDRestService.svc/GetAllBus");
		BufferedReader reader = new BufferedReader(new InputStreamReader(testURL.openStream()));
		JsonArray vehiclesArray = parser.parse(IOUtils.toString(reader)).getAsJsonArray();
		return vehiclesArray;
	}

	private void addLocationsToRedis(JedisPool pool, Map<String, GeoCoordinate> coordinateMap) {
		Jedis jedis = getJedisFromPool(pool);
		jedis.geoadd("mbusloc", coordinateMap);
		jedis.close();
		System.out.println("Added location to db : " + coordinateMap.size());
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
		jedis.zadd("mbustime", metaTimer);
		jedis.close();
	}

	private void deleteExpiredEntities(JedisPool pool, long unixtime) {
		Jedis jedis = getJedisFromPool(pool);
		Set<String> listtoremove = jedis.zrangeByScore("mbustime", Double.NEGATIVE_INFINITY, (double) unixtime);
		long count = 0l;
		long count1 = 0l;
		long count2 = 0l;
		for (String remove : listtoremove) {
			count = count + jedis.del(remove);
			count1 = count1 + jedis.zrem("mbusloc", remove);
			count2 = count2 + jedis.zrem("mbustime", remove);
		}
		jedis.close();
		System.out.println("Removed bus locations from db : " + listtoremove.size() + " Removed :" + count + ":"
				+ count1 + ":" + count2);
	}
}
