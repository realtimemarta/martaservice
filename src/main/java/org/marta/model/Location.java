package org.marta.model;

import java.text.ParseException;
import java.util.Date;

public class Location {
	
	private String uuid;
	private String tripId;
	private String route;
	private int addr;
	private String direction;
	private Date msgtime;
	private Double distance;
	private Double lat;
	private Double lon;
	
	public Location(String uuid, String tripId, String route, String addr, String direction, Date msgtime, Double distance,
			Double lat, Double lon) throws ParseException {
		super();
		this.uuid = uuid;
		this.tripId = tripId;
		this.route = route;
		this.addr = Integer.parseInt(addr);
		this.direction = direction;
		this.msgtime = msgtime;
		this.distance = distance;
		this.lat = lat;
		this.lon = lon;
	}
	
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getTripId() {
		return tripId;
	}
	public void setTripId(String tripId) {
		this.tripId = tripId;
	}
	public String getRoute() {
		return route;
	}
	public void setRoute(String route) {
		this.route = route;
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public Date getMsgtime() {
		return msgtime;
	}
	public void setMsgtime(Date msgtime) {
		this.msgtime = msgtime;
	}
	public Double getDistance() {
		return distance;
	}
	public void setDistance(Double distance) {
		this.distance = distance;
	}
	public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLon() {
		return lon;
	}
	public void setLon(Double lon) {
		this.lon = lon;
	}
	public int getAddr() {
		return addr;
	}
	public void setAddr(int addr) {
		this.addr = addr;
	}

}
