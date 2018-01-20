package org.marta.realtime.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class TrainService {
	
	
	private void trainsByStation(String stationName) {
		
	}
	
	private JsonArray downloadTrainDetails() throws IOException {
		JsonParser parser = new JsonParser();
		URL testURL = new URL("http://developer.itsmarta.com/RealtimeTrain/RestServiceNextTrain/GetRealtimeArrivals?apiKey=69c85439-3e90-44b2-9cd3-303520c01585");
		BufferedReader reader = new BufferedReader(new InputStreamReader(testURL.openStream()));
		JsonArray trainsArray = parser.parse(IOUtils.toString(reader)).getAsJsonArray();
		return trainsArray;
	}
}
