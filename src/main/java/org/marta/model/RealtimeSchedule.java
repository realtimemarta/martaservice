package org.marta.model;

import java.io.Serializable;
import java.util.Set;

public class RealtimeSchedule implements Serializable {

	private static final long serialVersionUID = -5028381406705583239L;

	private Stop stop;
	private Set<Schedule> schedules;
	private Set<Trip> trips;
	private Set<Route> routes;

	public Stop getStop() {
		return stop;
	}

	public void setStop(Stop stop) {
		this.stop = stop;
	}

	public Set<Schedule> getSchedules() {
		return schedules;
	}

	public void setSchedules(Set<Schedule> schedules) {
		this.schedules = schedules;
	}

	public Set<Trip> getTrips() {
		return trips;
	}

	public void setTrips(Set<Trip> trips) {
		this.trips = trips;
	}

	public Set<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(Set<Route> routes) {
		this.routes = routes;
	}

}
