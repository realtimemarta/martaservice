package org.marta.server;

import java.text.ParseException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.marta.RealTimeBase;
import org.marta.cache.CacheService;
import org.marta.model.RealtimeSchedule;
import org.marta.model.Route;
import org.marta.model.Schedule;
import org.marta.model.Stop;
import org.marta.model.Trip;
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

public class RealtimeStopsHandler extends RealTimeBase implements RequestHandler<Object, Object> {

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
					return new GatewayResponse("{ \"message\": \"invalid input params lat, lon or radius\"}", headers,
							400);
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
				List<RealtimeSchedule> busList;
				try {
					busList = getSchedules(pool, new GeoCoordinate(lon, lat), rad);
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

	private List<RealtimeSchedule> getSchedules(JedisPool pool, GeoCoordinate coordinate, double radius)
			throws ParseException {

		Jedis jedis = getJedisFromPool(pool);

		System.out.println("Retriving Data for vehicles at distance: " + radius);

		List<GeoRadiusResponse> members = jedis.georadius("mstop", coordinate.getLongitude(), coordinate.getLatitude(),
				radius, GeoUnit.M, GeoRadiusParam.geoRadiusParam().sortAscending().withCoord().withDist());

		CacheService cache = new CacheService(pool);

		List<RealtimeSchedule> scheduleList = new LinkedList<RealtimeSchedule>();

		for (GeoRadiusResponse member : members) {
			RealtimeSchedule schedule = new RealtimeSchedule();
			String stopKey = member.getMemberByString();
			Set<Stop> stops = cache.getMembers(stopKey, Stop.class);
			Stop stop = stops.iterator().next();
			Set<Schedule> schedules = cache.getMembers(stopKey.replace("S", "SC"), Schedule.class);
			removeUnwantedSchedules(schedules);
			Set<Trip> trips = new HashSet<Trip>();
			schedules.forEach(sc -> trips.addAll(cache.getMembers("T" + sc.getTripId(), Trip.class)));
			Set<Route> routes = new HashSet<Route>();
			trips.forEach(trip -> routes.addAll(cache.getMembers("R" + trip.getRouteId(), Route.class)));
			schedule.setStop(stop);
			schedule.setSchedules(schedules);
			schedule.setRoutes(routes);
			schedule.setTrips(trips);
			scheduleList.add(schedule);
		}

		jedis.close();
		System.out.println("Found schedules near you : " + scheduleList.size());
		return scheduleList;
	}

	private void removeUnwantedSchedules(Set<Schedule> schedules) {
		ZoneId america = ZoneId.of("America/New_York");
		LocalTime maxtime = LocalTime.now(america).plusMinutes(5);
		LocalTime mintime = LocalTime.now(america).minusMinutes(5);
		Iterator<Schedule> iter = schedules.iterator();
		while (iter.hasNext()) {
			String departTime = iter.next().getDepartureTime();
			if (departTime == null || departTime.isEmpty()) { 
				iter.remove();
				continue;
			} else if (departTime.startsWith("24")) {
				departTime = departTime.replaceFirst("24", "00");
			} else if (departTime.startsWith("25")) {
				departTime = departTime.replaceFirst("25", "01");
			} else if (departTime.indexOf(":") < 2) {
				departTime = "0" + departTime;
			}
			try {
				LocalTime depart = LocalTime.parse(departTime);
				if (depart.isAfter(maxtime) || depart.isBefore(mintime)) {
					iter.remove();
				}
			} catch (Exception e) {
				System.out.println("Error Parsing Date " + e.getMessage());
			}
		}
	}
}
