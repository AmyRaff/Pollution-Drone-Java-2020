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

public class Parser {

	// takes JSON file and returns body as String if no errors occur
	public static String getFileContent(String url) {
		var client = HttpClient.newHttpClient();
	   	var request = HttpRequest.newBuilder().uri(URI.create(url)).build();
	   	try {
			var response = client.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				// return body
				return response.body();
			} else {
				// error in file name
				throw new IllegalArgumentException("Check File!");
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
		
		// get Sensor Info from words file (single object)
		public static SensorInfo getSensorInfo(String url) {
			var output = getFileContent(url);
	    	var sensor = new Gson().fromJson(output, SensorInfo.class);
	    	return sensor;
		}
		
		// TODO
		public static void getNoFlyZones() {
			final String url = "http://localhost:80/buildings/no-fly-zones.geojson";
			var output = getFileContent(url);
			FeatureCollection fc = FeatureCollection.fromJson(output);
			List<Feature> features = fc.features();
		}
		
		// given an Observation instance, gets the URL for the corresponding words file
		public static String getWordsURL(Observation observation) {
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
	    	// concat URL together
	    	return "http://localhost:80/words/" + word1 + "/" +
	    			word2 + "/" + word3 + "/details.json";
		}
}
