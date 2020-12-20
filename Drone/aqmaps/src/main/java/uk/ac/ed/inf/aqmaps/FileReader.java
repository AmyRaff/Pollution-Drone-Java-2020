package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

public class FileReader {

	// takes JSON file and returns body as String if no errors occur
	private static String getFileContent(String url) {
		var client = HttpClient.newHttpClient();
	   	var request = HttpRequest.newBuilder().uri(URI.create(url)).build();
	   	try {
			var response = client.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				// return body
				return response.body();
			} else {
				// error in file name
				throw new IllegalArgumentException("Check Inputs!");
			}
		} catch (java.net.ConnectException e) {
			// error in web server
			System.out.println("WebServer Down!");
			System.exit(1);
		} catch (IllegalArgumentException e) {
			System.out.println("Check URL!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null; // shouldn't happen
	}
		
	// gets ArrayList of Observations from maps file
	public static ArrayList<Observation> getObservations(String url) {
		var output = getFileContent(url);
	   	Type listType = new TypeToken<ArrayList<Observation>>() {}.getType();
	   	ArrayList<Observation> observations = new Gson().fromJson(output, listType);
	   	return observations;
	}
	
	// given an Observation instance, gets the URL for the corresponding words file
	private static String getSensorURL(Observation observation) {
		// get location words for observation "a.b.c"
		var location = observation.getLocation();
		// position of first "." breaks up words 1 and 2
	    var end_point_1 = location.indexOf(".");
	   	var word1 = location.substring(0, end_point_1);
	   	var rest = location.substring(end_point_1 + 1, location.length());
	   	// position of second "." breaks up words 2 and 3
	   	var end_point_2 = rest.indexOf(".");
    	var word2 = rest.substring(0, end_point_2);
	   	var word3 = rest.substring(end_point_2 + 1, rest.length());
	   	// concatenate URL together
	   	return "http://localhost:" + App.port + "/words/" + word1 + "/" +
	    	word2 + "/" + word3 + "/details.json";
	}
	
	// get Sensor Info from an Observation
	public static Sensor getSensorInfo(Observation observation) {
		var output = getFileContent(getSensorURL(observation));
	   	var sensor = new Gson().fromJson(output, Sensor.class);
	   	return sensor;
	}
	
	// returns the list of 33 sensors for that specific day
	public static List<Sensor> getAllSensors() {
		var observations = getObservations(App.map_url);
		List<Sensor> sensors = new ArrayList<Sensor>();
		for (Observation ob : observations) {
			var sensor = getSensorInfo(ob);
			sensors.add(sensor);
		}
		return sensors;
	}
		
	
	// get no fly zones from file
	public static List<Polygon> getNoFlyZones() {
		String noFlyURL = "http://localhost:" + App.port + "/buildings/no-fly-zones.geojson";
		var flyzones = getFileContent(noFlyURL);
		FeatureCollection fc = FeatureCollection.fromJson(flyzones);
		List<Feature> buildings = fc.features();
		List<Polygon> noFlyZones = new ArrayList<Polygon>();
		for (Feature building : buildings) {
			Polygon poly = (Polygon)building.geometry();
			noFlyZones.add(poly);
		}
		return noFlyZones;
	}
	
	
}
