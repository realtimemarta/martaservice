package org.marta.model;

import java.io.Serializable;

import com.univocity.parsers.annotations.Parsed;

public class Stop implements Serializable {

	private static final long serialVersionUID = 3127359097645144549L;
	
	@Parsed(field = "stop_id")
	private String stopId;
	
	@Parsed(field = "stop_code")
	private String stopCode;
	
	@Parsed(field = "stop_name")
	private String stopName;
	
	@Parsed(field = "stop_lat")
	private Double stopLat;
	
	@Parsed(field = "stop_lon")
	private Double stopLon;

	public String getStopId() {
		return stopId;
	}

	public void setStopId(String stopId) {
		this.stopId = stopId;
	}

	public String getStopCode() {
		return stopCode;
	}

	public void setStopCode(String stopCode) {
		this.stopCode = stopCode;
	}

	public String getStopName() {
		return stopName;
	}

	public void setStopName(String stopName) {
		this.stopName = stopName;
	}

	public Double getStopLat() {
		return stopLat;
	}

	public void setStopLat(Double stopLat) {
		this.stopLat = stopLat;
	}

	public Double getStopLon() {
		return stopLon;
	}

	public void setStopLon(Double stopLon) {
		this.stopLon = stopLon;
	}
}
