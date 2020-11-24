package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class App {
	
	private final static double maxLng = -3.184319;
	private final static double minLng = -3.192473;
	private final static double maxLat =  55.946233;
	private final static double minLat = 55.942617;
	static String[] inputs;
	static double startLng;
	static double startLat;
	static String day;
	static String month;
	static String year;
	static String map_url;
	static long seed;
	static String port;
	
	// returns the sensor closest to the current position of the drone, which has not been visited yet
	private static Sensor getClosestSensor(List<Sensor> unvisited_sensors, Point currLocation) {
		List<Double> distances = new ArrayList<Double>();
		for (Sensor s : unvisited_sensors) {
			var distance = GeometryUtils.getDistance(currLocation.longitude(), currLocation.latitude(), s.getLongitude(), s.getLatitude());
			distances.add(distance);
		}
		var min = Collections.min(distances);
		var index = distances.indexOf(min);
		return unvisited_sensors.get(index);
	}
	
	// get the new position of the drone given its current position and the angle it travels in
	private static Point getNewLoc(int angle, Point currPosition) {
		double newLat = 0;
		double newLng = 0;
		var currLat = currPosition.latitude();
		var currLng = currPosition.longitude();
		if (angle == 0) { // East
			newLat = currLat;
			newLng = currLng + 0.0003;
		} else if (angle > 0 && angle < 90) {
			newLat = currLat + (0.0003 * Math.sin(Math.toRadians(angle)));
			newLng = currLng + (0.0003 * Math.cos(Math.toRadians(angle)));
		} else if (angle == 90) { // North
			newLat = currLat + 0.0003;
			newLng = currLng;
		} else if (angle > 90 && angle < 180) {
			angle = angle - 90;
			newLat = currLat + (0.0003 * Math.cos(Math.toRadians(angle)));
			newLng = currLng - (0.0003 * Math.sin(Math.toRadians(angle)));
		} else if (angle == 180) { // West
			newLat = currLat;
			newLng = currLng - 0.0003;
		} else if (angle > 180 && angle < 270) {
			angle = 270 - angle;
			newLat = currLat - (0.0003 * Math.cos(Math.toRadians(angle)));
			newLng = currLng - (0.0003 * Math.sin(Math.toRadians(angle)));
		} else if (angle == 270) { // South
			newLat = currLat - 0.0003;
			newLng = currLng;
		} else if (angle > 270 && angle < 360) {
			angle = 360 - angle;
			newLat = currLat - (0.0003 * Math.sin(Math.toRadians(angle)));
			newLng = currLng + (0.0003 * Math.cos(Math.toRadians(angle)));
		}
		return Point.fromLngLat(newLng, newLat);
	}
	
	// helper function for finding the index of the minimum of an array of distances
	private static int indexOfMinDistance(List<Double> distances) {
		double min = distances.get(0);
		int minIndex = 0;
        for(int i = 1; i < distances.size(); i++ ){
        	if( distances.get(i) < min ){
                min = distances.get(i);
                minIndex = i;
            }
        }
        return minIndex;
	}
	
	// finds the angle the drone must travel in to get as close as possible to the closest sensor
	private static int getAngle(Point currPosition, Point target) {
		var targetLat = target.latitude();
		var targetLng = target.longitude();
		List<Double> distances = new ArrayList<Double>();
		// possible directions
		int[] angles = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260,
		              270, 280, 290, 300, 310, 320, 330, 340, 350};
		// find distance to closest sensor after moving in every possible direction
		for (int angle : angles) {
			var newPosition = getNewLoc(angle, currPosition);
			var dist = GeometryUtils.getDistance(newPosition.longitude(), newPosition.latitude(), targetLng, targetLat);
			distances.add(dist);
		}
		// smallest distance tells us which angle is the best
		var best_angle = angles[indexOfMinDistance(distances)];
		// if best angle takes drone into no fly zone, or out of the confinement area, use next best angle
		while (GeometryUtils.inNoFlyZone(getNewLoc(best_angle, currPosition)) || 
				!inRange(getNewLoc(best_angle, currPosition))) {
			distances.remove(distances.get(indexOfMinDistance(distances)));
			if (distances.isEmpty()) {
				break;
			}
			best_angle = angles[indexOfMinDistance(distances)];
		}
		return best_angle;
	}
	
	// checks if a point is in the boundary for the drone
	private static boolean inRange(Point point) {
		if (point.latitude() < maxLat && point.latitude() > minLat && point.longitude() > minLng 
				&& point.longitude() < maxLng) {
			return true;
		}
		return false;
	}
	
	public static void main( String[] args ) {
		// command line arguments
		inputs = args;
		day = inputs[0];
		month = inputs[1];
		year = inputs[2];
		startLat = Double.parseDouble(inputs[3]);
		startLng = Double.parseDouble(inputs[4]);
		seed = Long.parseLong(inputs[5]);
		port = inputs[6];
		map_url = "http://localhost:" + port + "/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json";
		var startLocation = Point.fromLngLat(startLng, startLat);
		Point currLocation = startLocation;
		// for flight path text file
		String flightpath = "";
		// list of sensors not yet visited
		ArrayList<Sensor> unvisited_sensors = (ArrayList<Sensor>) FileReader.getAllSensors();
		// path the drone takes
		List<Point> path = new ArrayList<Point>();
		path.add(startLocation);
		int turn = 0;
		// markers for the sensors
		List<Feature> markers = new ArrayList<Feature>();
		// list of grey initial markers for that day, replaced later by coloured markers
		List<Feature> initials = MapUtils.getInitialMarkers(day, month, year);
		while(turn < 150) {
			// variables for flight path file
			var num = turn + 1;
			var strtLng = currLocation.longitude();
			var strtLat = currLocation.latitude();
			int angle = 0;
			String words = null;
			if (unvisited_sensors.isEmpty()) {
				// when all sensors have been read from, go back to starting point with remaining moves
				var dist_to_end = GeometryUtils.getDistance(currLocation.longitude(), currLocation.latitude(), startLocation.longitude(), startLocation.latitude());
				var dir = getAngle(currLocation, startLocation);
				angle = dir;
				var newLoc = getNewLoc(dir, currLocation);
				// if new position is valid, move to it
				if (inRange(newLoc)) {
					path.add(newLoc);
					currLocation = newLoc;
					dist_to_end = GeometryUtils.getDistance(currLocation.longitude(), currLocation.latitude(), startLocation.longitude(), startLocation.latitude());
				} else { // if out of bounds, error - shouldn't happen
					System.out.println("Error - out of bounds!");
					break;
				}
				if (dist_to_end < 0.0003) { // if position within 0.0003 degrees of starting position, stop
					System.out.println("Made it back to start!");
					break;
				}
			} else { // when sensors still need to be read
				var closest_sensor = getClosestSensor(unvisited_sensors, currLocation);
				// sensor converted to Point for use in getAngle(Point, Point)
				var sensor_point = Point.fromLngLat(closest_sensor.getLongitude(), closest_sensor.getLatitude());
				var dist_to_sensor = GeometryUtils.getDistance(currLocation.longitude(), currLocation.latitude(), 
						closest_sensor.getLongitude(), closest_sensor.getLatitude());
				var dir = getAngle(currLocation, sensor_point);
				angle = dir;
				var newLocation = getNewLoc(dir, currLocation);
				// if the drone starts going backwards and forwards between two points (eg. stuck behind a no fly zone)
				if (path.indexOf(currLocation) > 0 && path.get(path.indexOf(currLocation) - 1).equals(newLocation)) {
					@SuppressWarnings("unchecked")
					ArrayList<Sensor> new_sensor_list = (ArrayList<Sensor>) unvisited_sensors.clone();
					// if there's more than one sensor still to read, and cant get to closest sensor, go to next closest sensor instead
					if (new_sensor_list.size() != 1) {
						new_sensor_list.remove(new_sensor_list.indexOf(closest_sensor));
						closest_sensor = getClosestSensor(new_sensor_list, currLocation);
						sensor_point = Point.fromLngLat(closest_sensor.getLongitude(), closest_sensor.getLatitude());
						dist_to_sensor = GeometryUtils.getDistance(currLocation.longitude(), currLocation.latitude(), 
								closest_sensor.getLongitude(), closest_sensor.getLatitude());
						dir = getAngle(currLocation, sensor_point);
						angle = dir;
						newLocation = getNewLoc(dir, currLocation);
					} else {
						// if there's only one sensor left and can't get to it, try the 4 axes directions N/E/S/W to get out
						int[] axes = {0, 90, 180, 270};
						for (int ax : axes) {
							var newLoc = getNewLoc(ax, currLocation);
							angle = ax;
							if (inRange(newLoc) && (!GeometryUtils.inNoFlyZone(newLoc)) && 
									!path.get(path.indexOf(currLocation) - 1).equals(newLoc)) {
								newLocation = newLoc;
								break;
							}
						}
					}
				}
				// if new location valid, move to it
				if (inRange(newLocation)) { // for valid new location
					path.add(newLocation);
					currLocation = newLocation;
					dist_to_sensor = GeometryUtils.getDistance(currLocation.longitude(), currLocation.latitude(), 
							closest_sensor.getLongitude(), closest_sensor.getLatitude());
				} else { // if out of bounds, error - shouldn't happen
					System.out.println("Error - out of bounds!");
					break;
				}
				if (dist_to_sensor < 0.0002) {
					// read the sensor if close enough to it
					var sensor_location = Point.fromLngLat(closest_sensor.getLongitude(), closest_sensor.getLatitude());
					words = closest_sensor.getWords();
					var f = Feature.fromGeometry((Geometry)sensor_location);
					// format the marker for the sensor
					f.addStringProperty("rgb-string", closest_sensor.getColour());
					f.addStringProperty("marker-color", closest_sensor.getColour());
					f.addStringProperty("marker-symbol", closest_sensor.getSymbol());
					f.addStringProperty("location", closest_sensor.getWords());
					markers.add(f);
					// update unvisited markers list
					unvisited_sensors.remove(closest_sensor);
					// remove initial grey marker once sensor is read, replaced with coloured marker
					for (Feature initial : MapUtils.getInitialMarkers(day, month, year)) {
						var loc = initial.getStringProperty("location");
						var cLoc = closest_sensor.getWords();
						if (loc.equals(cLoc)) {
							initials.remove(initial);
						}
					}
				}
			}
			// longitude and latitude of new position, for flight path
			var nLng = currLocation.longitude();
			var nLat = currLocation.latitude();
			// add line to flight path
			var flight_entry = num + "," + strtLng + "," + strtLat + "," + angle + "," + nLng + "," + nLat + "," + words + "\n";
			flightpath += flight_entry;
			// next turn
			turn++;
		}
		// outputs for checking success of drone
		System.out.println(day + "/" + month + "/" + year);
		System.out.println("Turns: " + turn);
		System.out.println("Missed Sensors: " + unvisited_sensors.size());
		// create GEOJSON map
		LineString pathway = LineString.fromLngLats(path);
		Feature path_way = Feature.fromGeometry((Geometry)pathway);
		List<Feature> features = new ArrayList<Feature>();
		features.addAll(initials); // initial markers, shouldn't have any at the end - only if sensors were missed
		features.add(path_way); // path travelled
		features.addAll(markers); // marker for each sensor
		FeatureCollection fc = FeatureCollection.fromFeatures(features);
		MapUtils.createJSONFile(fc);
		// create flight path file
		MapUtils.createTextFile(flightpath);
    }
}