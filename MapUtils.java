package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class MapUtils {

	public static void createJSONFile(FeatureCollection fc) {
    	try {
    		// create the JSON file and write to it
    		FileWriter file = new FileWriter("readings-" + App.day + "-" + App.month + "-" + App.year + ".geojson");
    		file.write(fc.toJson());
	        file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	public static void createTextFile(String flightpath) {
		try {
    		// create the flight path text file
    		FileWriter file = new FileWriter("flightpath-" + App.day + "-" + App.month + "-" + App.year + ".txt");
    		file.write(flightpath);
	        file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// gets initial list of markers from list of sensors to be read from that day
	public static List<Feature> getInitialMarkers(String day, String month, String year) {
		var observations = FileReader.getObservations(App.map_url);
		List<Feature> feats = new ArrayList<Feature>();
		for (Observation o : observations) {
			var sensor = FileReader.getSensorInfo(o);
			var sensor_location = Point.fromLngLat(sensor.getLongitude(), sensor.getLatitude());
			var f = Feature.fromGeometry((Geometry)sensor_location);
			f.addStringProperty("rgb-string", "#aaaaaa");
			f.addStringProperty("marker-color", "#aaaaaa");
			f.addStringProperty("location", sensor.getWords());
			feats.add(f);
		}
		return feats;
	}
	
	// get battery level from a sensor reading
	private static double getBatteryFromSensor(Sensor sensor) {
		var location = sensor.getWords();
		double battery = 0;
		for (Observation o : FileReader.getObservations(App.map_url)) {
			if (o.getLocation().equals(location)) {
				battery = o.getBattery();
			}
		}
		return battery;
	}
	
	// get pollution level from sensor reading
	private static String getReadingFromSensor(Sensor sensor) {
		var location = sensor.getWords();
		String reading = "";
		for (Observation o : FileReader.getObservations(App.map_url)) {
			if (o.getLocation().equals(location)) {
				reading = o.getReading();
			}
		}
		return reading;
	}
	
	// assigns the correct colour to a marker based on reading and battery levels
	public static String getSensorColour(Sensor sensor) {
		String rgb = null;
		if (!sensor.isVisited()) {
			rgb = "#aaaaaa";
		}
    	double battery = getBatteryFromSensor(sensor);
    	if(battery < 0) throw new IllegalArgumentException("Negative Battery!");
    	else if(battery < 10) {
    		return "#000000";
    	} else if(battery > 100) throw new IllegalArgumentException("Battery over 100%!");
    	double reading = Double.parseDouble(getReadingFromSensor(sensor));
    	if(reading < 0) {
    		throw new IllegalArgumentException("Negative Pollution!");
    	} else if(reading < 32) {
    		rgb = "#00ff00";
    	} else if(reading < 64) {
    		rgb = "#40ff00";
    	} else if(reading < 96) {
    		rgb = "#80ff00";
    	} else if(reading < 128) {
    		rgb = "#c0ff00";
    	} else if(reading < 160) {
    		rgb = "#ffc000";
    	} else if(reading < 192) {
    		rgb = "#ff8000";
    	} else if(reading < 224) {
    		rgb = "#ff4000";
    	} else if(reading < 256) {
    		rgb = "#ff0000";
    	} else if(reading >= 256) {
    		throw new IllegalArgumentException("Pollution Above Range!");
    	}
    	return rgb;
    }
	
	// assigns the correct marker symbol based on pollution level
	public static String getMarkerSymbol(Sensor sensor) {
		String symbol = null;
		if (!sensor.isVisited()) {
			symbol = "";
		}
    	double battery = getBatteryFromSensor(sensor);
    	if(battery < 10) {
    		symbol = "cross";
    		return symbol;
    	} 
    	double reading = Double.parseDouble(getReadingFromSensor(sensor));
    	if(reading < 128) {
    		symbol = "lighthouse";
    	} else {
    		symbol = "danger";
    	}
    	return symbol;
	}
}
