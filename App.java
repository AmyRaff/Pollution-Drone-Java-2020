package uk.ac.ed.inf.aqmaps;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import java.io.IOException;
import java.lang.reflect.Type;

public class App {
	
	static double startLat = 55.944425;
	static double startLng = -3.188396;
	static String day = "01";
	static String month = "01";
	static String year = "2020";
	
	static String map_url = "http://localhost:80/maps/" + year + "/" + month +
			"/" + day + "/air-quality-data.json";
	static ArrayList<Observation> obs = Parser.getObservations(map_url);
	
	public static void main( String[] args ) {
    	var testob = obs.get(0);
    	var words_url = Parser.getWordsURL(testob);
    	System.out.println(words_url);
    }
}
