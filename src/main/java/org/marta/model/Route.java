package org.marta.model;

import java.io.Serializable;

import com.univocity.parsers.annotations.Parsed;

public class Route implements Serializable {
	
	private static final long serialVersionUID = 70965951183782314L;
	
	@Parsed(field = "route_id")
	private String routeId;
    
	@Parsed(field = "route_short_name")
	private String routeShortName;
	
	@Parsed(field = "route_long_name")
	private String routeLongName;
	
	@Parsed(field = "route_desc")
	private String routeDesc;
	
	@Parsed(field = "route_type")
	private String routeType;

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getRouteShortName() {
		return routeShortName;
	}

	public void setRouteShortName(String routeShortName) {
		this.routeShortName = routeShortName;
	}

	public String getRouteLongName() {
		return routeLongName;
	}

	public void setRouteLongName(String routeLongName) {
		this.routeLongName = routeLongName;
	}

	public String getRouteDesc() {
		return routeDesc;
	}

	public void setRouteDesc(String routeDesc) {
		this.routeDesc = routeDesc;
	}

	public String getRouteType() {
		return routeType;
	}

	public void setRouteType(String routeType) {
		this.routeType = routeType;
	}
}
