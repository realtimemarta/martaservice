// The latitude and longitude of your business / place
const position = [33.753570, -84.391956];
const options = { enableHighAccuracy: true, maximumAge: 0, timeout: 10000 };
var markersArray = [];
var map;
var center;
var rad;
var watchID;
function initMap() {
  var latLng = new google.maps.LatLng(position[0], position[1]);

  var mapOptions = {
    zoom: 16, // initialize zoom level - the max value is 21
    streetViewControl: false, // hide the yellow Street View pegman
    scaleControl: true, // allow users to zoom the Google Map
    mapTypeId: google.maps.MapTypeId.ROADMAP,
    center: latLng
  };

  map = new google.maps.Map(document.getElementById('googlemaps'),
    mapOptions);

  var transitLayer = new google.maps.TransitLayer();
  transitLayer.setMap(map);
  // Show the default red marker at the location
  marker = new google.maps.Marker({
    position: latLng,
    map: map,
    draggable: false,
    animation: google.maps.Animation.DROP
  });
  markersArray.push(marker);
  var tempcenter = {
    lat: position[0],
    lng: position[1]
  };
  caliculateRadius(tempcenter);
}

function clearWatch() {
    if (watchID != null) {
        navigator.geolocation.clearWatch(watchID);
        watchID = null;
    }
}

function showGoogleMaps() {
  initMap();
  centerWindow = new google.maps.InfoWindow;

  // Try HTML5 geolocation.
  if (navigator.geolocation) {
    watchID = navigator.geolocation.watchPosition(function(position) {
      center = {
        lat: position.coords.latitude,
        lng: position.coords.longitude
      };
      clearWatch();
      map.setCenter(center);
      showPositions();
    }, function() {
      handleLocationError(true, centerWindow, map.getCenter());
    }, options);
  } else {
    // Browser doesn't support Geolocation
    handleLocationError(false, centerWindow, map.getCenter());
  }
}

function handleLocationError(browserHasGeolocation, centerWindow, pos) {
  centerWindow.setPosition(pos);
  centerWindow.setContent(browserHasGeolocation ?
    'Error: The Geolocation service failed.' :
    'Error: Your browser doesn\'t support geolocation.');
  centerWindow.open(map);
}

function caliculateRadius(loc) {
  var bounds = map.getBounds();
  if (bounds && pos) {
    var ne = bounds.getNorthEast();
    // Calculate radius (in meters).
    var _pCord = new google.maps.LatLng(center.lat, center.lng);
    rad = google.maps.geometry.spherical.computeDistanceBetween(_pCord, ne);
  }
}

function getLocationsAndDisplay(rad = 1000.00) {
  var requestdata = {
    lat: center.lat,
    lon: center.lng,
    radius: rad
  };
  var jsonString = JSON.stringify(requestdata);

  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;

  xhr.addEventListener("readystatechange", function() {
    if (this.readyState === 4) {
      buildMarkers(JSON.parse(this.responseText));
    }
  });

  xhr.open("POST", "https://d1gvommypjdnvg.cloudfront.net/api/v1/locations");
  xhr.setRequestHeader("content-type", "application/json");
  xhr.setRequestHeader("accept", "application/json");
  xhr.setRequestHeader("cache-control", "no-cache");

  xhr.send(jsonString);
}

function showPositions() {
  getLocationsAndDisplay(rad);
}

function buildMarkers(locs) {
  while(markersArray.length) { markersArray.pop().setMap(null);}

  var centermark = new google.maps.Marker({
    position: new google.maps.LatLng(center.lat, center.lng),
    map: map,
    animation: google.maps.Animation.DROP
  });

  var centerinfo = new google.maps.InfoWindow;
  centerinfo.setPosition(center);
  centerinfo.setContent('Me');
  centerinfo.open(map, centermark);

  for (i = 0; i < locs.length; i++) {
    var marker = new google.maps.Marker({
      position: new google.maps.LatLng(locs[i].lat, locs[i].lon),
      map: map,
      animation: google.maps.Animation.DROP
    });

    var infowindow = new google.maps.InfoWindow({
      content: locs[i].route + " " + locs[i].direction
    });
    infowindow.open(map, marker);
  }
}
