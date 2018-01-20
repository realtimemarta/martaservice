package org.marta.model;

import java.io.Serializable;

import com.univocity.parsers.annotations.Parsed;

public class Trip implements Serializable {

	private static final long serialVersionUID = 5126208688023778777L;

	@Parsed(field = "route_id")
	private String routeId;
	
	@Parsed(field = "service_id")
	private String serviceId;
	
	@Parsed(field = "trip_id")
	private String tripId;
	
	@Parsed(field = "direction_id")
	private String directionId;
	
	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public String getDirectionId() {
		return directionId;
	}

	public void setDirectionId(String directionId) {
		this.directionId = directionId;
	}
}
