package org.marta.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.marta.RealTimeBase;
import org.marta.model.Location;
import org.marta.server.model.GatewayResponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.geo.GeoRadiusParam;

public class RealtimeHandler extends RealTimeBase implements RequestHandler<Object, Object> {

	@Override
	public Object handleRequest(Object input, Context context) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Access-Control-Allow-Origin", "*");

		if (input instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, String> inputMap = (Map<String, String>) (input);
			String body = inputMap.get("body");
			if (body != null && !body.isEmpty()) {
				JsonParser parser = new JsonParser();
				JsonObject inputJson = parser.parse(body).getAsJsonObject();
				
				JsonElement lonEl = inputJson.get("lon");
				JsonElement latEl = inputJson.get("lat");
				JsonElement radEl = inputJson.get("radius");
				
				if (lonEl == null || latEl == null || radEl == null) {
					return new GatewayResponse("{ \"message\": \"invalid input params lat, lon or radius\"}", headers, 400);
				}

				Double lon = lonEl.getAsDouble();
				Double lat = latEl.getAsDouble();
				Double rad = radEl.getAsDouble();
				if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
					return new GatewayResponse("{ \"message\": \"invalid input params lat or lon\"}", headers, 400);
				}

				JedisPool pool = getPoolFromRedis();
				if (pool == null) {
					return new GatewayResponse("{ \"message\": \"Unable to init pool\"}", headers, 500);
				}
				List<Location> busList;
				try {
					busList = getBusses(pool, new GeoCoordinate(lon, lat),
							rad);
				} catch (ParseException e) {
					return new GatewayResponse("{ \"message\": \"Issue when building bus list\"}", headers, 500);
				}
				Gson gson = new Gson();
				return new GatewayResponse(gson.toJson(busList), headers, 200);

			} else {
				return new GatewayResponse("{ \"message\": \"Request not well formed\"}", headers, 400);
			}

		} else {
			return new GatewayResponse("{ \"message\": \"Server Error\"}", headers, 500);
		}
	}

	private List<Location> getBusses(JedisPool pool, GeoCoordinate coordinate, double radius) throws ParseException {

		Jedis jedis = getJedisFromPool(pool);

		System.out.println("Retriving Data for vehicles at distance: " + radius);

		List<GeoRadiusResponse> members = jedis.georadius("mbusloc", coordinate.getLongitude(),
				coordinate.getLatitude(), radius, GeoUnit.M,
				GeoRadiusParam.geoRadiusParam().sortAscending().withCoord().withDist());
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);
		Date maxDateTime = null;
		List<Location> vehicleList = new LinkedList<Location>();
		for (GeoRadiusResponse member : members) {
			List<String> values = jedis.hmget(member.getMemberByString(), "TRIPID", "ROUTE", "ADHERENCE", "DIRECTION",
					"MSGTIME");
			GeoCoordinate geo = member.getCoordinate();
			Date datetime = dateFormat.parse(values.get(4));
			Location location = new Location(member.getMemberByString(),
					values.get(0),
					values.get(1),
					values.get(2),
					values.get(3),
					datetime,
					member.getDistance(),
					geo.getLatitude(),
					geo.getLongitude()
					);
			
			vehicleList.add(location);

			if (maxDateTime != null) {
				
				if (datetime.after(maxDateTime)) {
					maxDateTime = datetime;
				}
			} else {
				maxDateTime = dateFormat.parse(values.get(4));
			}
		}

		jedis.close();
		System.out.println("Found busses near you : " + vehicleList.size());
		
		Iterator<Location> iter = vehicleList.iterator();
		while(iter.hasNext()){
			Calendar cal = Calendar.getInstance();
			cal.setTime(iter.next().getMsgtime());
			cal.add(Calendar.MINUTE, 3);
			cal.getTime();
			if (cal.getTime().before(maxDateTime)) {
				 iter.remove();
			}  
		}

		System.out.println("Returning only : " + vehicleList.size());
		return vehicleList;
	}
}
