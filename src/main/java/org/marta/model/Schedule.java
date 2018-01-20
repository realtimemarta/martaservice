package org.marta.model;

import java.io.Serializable;

import com.univocity.parsers.annotations.Parsed;

public class Schedule implements Serializable {

	private static final long serialVersionUID = 7782188450387426033L;
	
	@Parsed(field = "trip_id")
	private String tripId;
	
	@Parsed(field = "stop_id")
	private String stopId;
	
	@Parsed(field = "arrival_time")
	private String arrivalTime;
	
	@Parsed(field = "departure_time")
	private String departureTime;
	
	@Parsed(field = "stop_sequence")
	private String stopSequence;

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public String getStopId() {
		return stopId;
	}

	public void setStopId(String stopId) {
		this.stopId = stopId;
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public String getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}

	public String getStopSequence() {
		return stopSequence;
	}

	public void setStopSequence(String stopSequence) {
		this.stopSequence = stopSequence;
	}
}
