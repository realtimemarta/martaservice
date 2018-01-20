package org.marta.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.marta.RealTimeBase;
import org.marta.cache.CacheService;
import org.marta.model.Route;
import org.marta.model.Schedule;
import org.marta.model.Stop;
import org.marta.model.Trip;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class LoadMartaDataToCache extends RealTimeBase implements RequestHandler<S3Event, String> {

	@Override
	public String handleRequest(S3Event s3event, Context arg1) {

		BufferedReader br = null;
		try {
			S3EventNotificationRecord record = s3event.getRecords().get(0);

			String bkt = record.getS3().getBucket().getName();
			String key = record.getS3().getObject().getKey().replace('+', ' ');
			key = URLDecoder.decode(key, "UTF-8");
			System.out.println("File Found :" + key);

			// Read the source file as text
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withEndpointConfiguration(
					new AwsClientBuilder.EndpointConfiguration("https://s3.us-east-2.amazonaws.com", "us-east-2"))
					.build();
			System.out.println("Created Client :" + key);
			S3Object object = s3Client.getObject(bkt, key);
			System.out.println("Object Retrived :" + key);

			InputStream objectData = object.getObjectContent();
			br = new BufferedReader(new InputStreamReader(objectData));
			System.out.println("Buffer Created :" + key);
			switch (key) {
			case "stops.txt":
				System.out.println("Started Processing stop file :" + key);
				writeStops(br);
				System.out.println("Completed Processing stop file :" + key);
				break;
			case "trips.txt":
				System.out.println("Started Processing trips file :" + key);
				writeTrips(br);
				System.out.println("Completed Processing trips file :" + key);
				break;
			case "routes.txt":
				System.out.println("Started Processing routes file :" + key);
				writeRoutes(br);
				System.out.println("Completed Processing routes file :" + key);
				break;
			case "stop_times.txt":
				System.out.println("Started Processing schedules file :" + key);
				writeSchedules(br);
				System.out.println("Completed Processing schedules file :" + key);
				break;
			default:
				break;
			}

			br.close();
			object.close();
			s3Client.deleteObject(new DeleteObjectRequest(bkt, key));

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		return "" + System.currentTimeMillis();
	}

	public void writeStops(BufferedReader br) throws IOException {
		try {
			BeanListProcessor<Stop> rowProcessor = new BeanListProcessor<Stop>(Stop.class);
			
			CsvParserSettings parserSettings = new CsvParserSettings();
			parserSettings.setProcessor(rowProcessor);
			parserSettings.setHeaderExtractionEnabled(true);
			
			CsvParser parser = new CsvParser(parserSettings);
			parser.parse(br);
			
			List<Stop> list = rowProcessor.getBeans();
			
			Map<String, Object> objectmap = new HashMap<String, Object>();
			Map<String, GeoCoordinate> coordinateMap = new HashMap<String, GeoCoordinate>();
			for (Stop stop : list) {
				objectmap.put("S" + stop.getStopId(), stop);
				coordinateMap.put("S" + stop.getStopId(), new GeoCoordinate(stop.getStopLon(), stop.getStopLat()));
			}
			System.out.println("Writing Stops to Cache :" + list.size());
			storeObjectsToCache(objectmap);
			addStopsToCache(coordinateMap);
			System.out.println("Completed Writing Stops to Cache :" + list.size());
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}
	
	private void addStopsToCache(Map<String, GeoCoordinate> coordinateMap) {	
		JedisPool pool = getPoolFromRedis();
		Jedis jedis = getJedisFromPool(pool);
		jedis.geoadd("mstop", coordinateMap);
		jedis.close();
		System.out.println("Added stops to Cache : " + coordinateMap.size());
	}

	public void writeTrips(BufferedReader br) throws IOException {
		try {
            BeanListProcessor<Trip> rowProcessor = new BeanListProcessor<Trip>(Trip.class);
			
			CsvParserSettings parserSettings = new CsvParserSettings();
			parserSettings.setProcessor(rowProcessor);
			parserSettings.setHeaderExtractionEnabled(true);
			
			CsvParser parser = new CsvParser(parserSettings);
			parser.parse(br);
			
			List<Trip> list = rowProcessor.getBeans();

			Map<String, Object> objectmap = new HashMap<String, Object>();
			for (Trip trip : list) {
				objectmap.put("T" + trip.getTripId(), trip);
			}
			System.out.println("Writing Trips to Cache :" + list.size());
			storeObjectsToCache(objectmap);
			System.out.println("Completed Writing Trips to Cache :" + list.size());
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	public void writeRoutes(BufferedReader br) throws IOException {
		try {
            BeanListProcessor<Route> rowProcessor = new BeanListProcessor<Route>(Route.class);
			
			CsvParserSettings parserSettings = new CsvParserSettings();
			parserSettings.setProcessor(rowProcessor);
			parserSettings.setHeaderExtractionEnabled(true);
			
			CsvParser parser = new CsvParser(parserSettings);
			parser.parse(br);
			List<Route> list = rowProcessor.getBeans();
			
			Map<String, Object> objectmap = new HashMap<String, Object>();
			for (Route route : list) {
				objectmap.put("R" + route.getRouteId(), route);
			}
			System.out.println("Writing Routes to Cache :" + list.size());
			storeObjectsToCache(objectmap);
			System.out.println("Completed Writing Routes to Cache :" + list.size());
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	public void writeSchedules(BufferedReader br) throws IOException {
		try {
            BeanListProcessor<Schedule> rowProcessor = new BeanListProcessor<Schedule>(Schedule.class);
			
			CsvParserSettings parserSettings = new CsvParserSettings();
			parserSettings.setProcessor(rowProcessor);
			parserSettings.setHeaderExtractionEnabled(true);
			
			CsvParser parser = new CsvParser(parserSettings);
			parser.parse(br);
			List<Schedule> list = rowProcessor.getBeans();
			
			Map<String, Set<Object>> objectmap = new HashMap<String, Set<Object>>();
			for (Schedule schedule : list) {
				String key = "SC" + schedule.getStopId();
				if (objectmap.get(key) != null) {
					objectmap.get(key).add(schedule);
				} else {
					Set<Object> byStop = new HashSet<Object>();
					byStop.add(schedule);
					objectmap.put(key, byStop);
				}
			}
			System.out.println("Writing Schedules to Cache :" + list.size());
			
			JedisPool pool = getPoolFromRedis();
			CacheService cache = new CacheService(pool);
			cache.bulkSetInsert(objectmap);
			
			System.out.println("Completed writing Schedules to Cache :" + list.size());
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	private void storeObjectsToCache(Map<String, Object> objectmap) {
		JedisPool pool = getPoolFromRedis();
		CacheService cache = new CacheService(pool);
		cache.bulkObjectInsert(objectmap);
	}
}
