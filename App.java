package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class App {
	
	static double startLat = 55.944425;
	final static double maxLng = -3.184319;
	final static double minLng = -3.192473;
	final static double maxLat =  55.946233;
	final static double minLat = 55.942617;
	static double startLng = -3.188396;
	static String day = "01";
	static String month = "01";
	static String year = "2020";
	static String map_url = "http://localhost:80/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json";
	
	// returns the list of 33 sensors for that specific day
	public static List<Sensor> getAllSensors() {
		var observations = Parser.getObservations(map_url);
		List<Sensor> sensors = new ArrayList<Sensor>();
		for (Observation ob : observations) {
			var sensor = Parser.getSensorInfo(ob);
			sensors.add(sensor);
		}
		return sensors;
	}
	
	// returns the sensor closest to the current position of the drone, which has not been visited yet
	public static Sensor getClosestSensor(List<Sensor> unvisited_sensors, Point currLocation) {
		List<Double> distances = new ArrayList<Double>();
		for (Sensor s : unvisited_sensors) {
			var distance = Parser.getDistance(currLocation.longitude(), currLocation.latitude(), s.getLongitude(), s.getLatitude());
			distances.add(distance);
		}
		var min = Collections.min(distances);
		var index = distances.indexOf(min);
		return unvisited_sensors.get(index);
	}
	
	// returns the new coordinates of the drone given its previous coordinates, the angle it needs to travel in, and where it is trying to go
	public static Point getCoordsFromAngle(int angle, Point currPosition, Point target) {
		double newLat = 0;
		double newLng = 0;
		var targetLat = target.latitude();
		var targetLng = target.longitude();
		var currLat = currPosition.latitude();
		var currLng = currPosition.longitude();
		if (angle == 0) { // East
			newLat = currLat;
			newLng = currLng + 0.0003;
		} else if (angle > 0 && angle < 90) {
			newLat = currLat + (0.0003 * Math.sin(Math.toDegrees(Math.atan((targetLat - currLat) / (currLng - targetLng)))));
			newLng = currLng + (0.0003 * Math.cos(Math.toDegrees(Math.atan((targetLat - currLat) / (currLng - targetLng)))));
		} else if (angle == 90) { // North
			newLat = currLat + 0.0003;
			newLng = currLng;
		} else if (angle > 90 && angle < 180) {
			newLat = currLat + (0.0003 * Math.sin(Math.toDegrees(Math.atan((targetLat - currLat) / (targetLng - currLng)))));
			newLng = currLng - (0.0003 * Math.cos(Math.toDegrees(Math.atan((targetLat - currLat) / (targetLng - currLng)))));
		} else if (angle == 180) { // West
			newLat = currLat;
			newLng = currLng - 0.0003;
		} else if (angle > 180 && angle < 270) {
			newLat = currLat - (0.0003 * Math.sin(Math.toDegrees(Math.atan((currLat - targetLat) / (targetLng - currLng)))));
			newLng = currLng - (0.0003 * Math.cos(Math.toDegrees(Math.atan((currLat - targetLat) / (targetLng - currLng)))));
		} else if (angle == 270) { // South
			newLat = currLat - 0.0003;
			newLng = currLng;
		} else if (angle > 270 && angle < 360) {
			newLat = currLat - (0.0003 * Math.sin(Math.toDegrees(Math.atan((currLat - targetLat) / (currLng - targetLng)))));
			newLng = currLng + (0.0003 * Math.cos(Math.toDegrees(Math.atan((currLat - targetLat) / (currLng - targetLng)))));
		}
		return Point.fromLngLat(newLng, newLat);
	}
	
	// finds the angle the drone must travel in to get as close as possible to the closest sensor
	public static Point getNewPosition(Point currPosition, Sensor closest_sensor) {
		var sensorLat = closest_sensor.getLatitude();
		var sensorLng = closest_sensor.getLongitude();
		List<Double> distances = new ArrayList<Double>();
		// possible directions
		int[] angles = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260,
		              270, 280, 290, 300, 310, 320, 330, 340, 350};
		// find distance to closest sensor after moving in every possible direction
		for (int angle : angles) {
			var newPosition = getCoordsFromAngle(angle, currPosition, Point.fromLngLat(sensorLng, sensorLat));
			var dist = Parser.getDistance(newPosition.longitude(), newPosition.latitude(), sensorLng, sensorLat);
			distances.add(dist);
		}
		// smallest distance tells us which angle is the best
		var best = distances.indexOf(Collections.min(distances));
		var best_angle = angles[best];
		// if best angle takes drone into no fly zone, use next best angle
		while (inNoFlyZone(getCoordsFromAngle(best_angle, currPosition, Point.fromLngLat(sensorLng, sensorLat)))) {
			distances.remove(distances.get(best));
			var next_best = distances.indexOf(Collections.min(distances));
			best_angle = angles[next_best];
		}
		// return the new coordinate using the calculated angle value
		return getCoordsFromAngle(best_angle, currPosition, Point.fromLngLat(sensorLng, sensorLat));
	}
	
	// takes the drone as close as possible to its starting position
	public static Point backToStart(Point currPosition, Point startPosition) {
		List<Double> distances = new ArrayList<Double>();
		int[] angles = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260,
		              270, 280, 290, 300, 310, 320, 330, 340, 350};
		for (int angle : angles) {
			var newPosition = getCoordsFromAngle(angle, currPosition, startPosition);
			var dist = Parser.getDistance(newPosition.longitude(), newPosition.latitude(), startPosition.longitude(), startPosition.latitude());
			distances.add(dist);
		}
		var best = distances.indexOf(Collections.min(distances));
		var best_angle = angles[best];
		// if best angle takes drone into no fly zone, use next best angle
		while (inNoFlyZone(getCoordsFromAngle(best_angle, currPosition, startPosition))) {
			distances.remove(distances.get(best));
			var next_best = distances.indexOf(Collections.min(distances));
			best_angle = angles[next_best];
		}
		return getCoordsFromAngle(best_angle, currPosition, startPosition);
	}
	
	// checks if a point is in the boundary for the drone
	public static boolean inRange(Point point) {
		if (point.latitude() < maxLat && point.latitude() > minLat && point.longitude() > minLng 
				&& point.longitude() < maxLng) {
			return true;
		}
		return false;
	}
	
	// checks if a point is in any of the no fly zones
	public static boolean inNoFlyZone(Point point) {
		var zones = Parser.getNoFlyZones();
		for (Polygon zone : zones) {
			if (pointInPolygon(zone, point)) {
				return true;
			}
		}
		return false;
	}
	
	// returns whether a point is within a polygon
	public static boolean pointInPolygon(Polygon polygon, Point point) {
		var poly_points = polygon.coordinates().get(0);
		// convert polygon to array of i coordinates where poly[i][0] is the longitude and poly[i][1] is the latitude of coordinate i
		double[][] poly = new double[poly_points.size()][2];
		for (int i = 0; i < poly_points.size(); i++) {
			poly[i][0] = poly_points.get(i).longitude();
			poly[i][1] = poly_points.get(i).latitude();
		}
		// convert point to (longitude, latitude) array
		double[] point_coords = {point.longitude(), point.latitude()};
		// a point is in a polygon if a line from the point to infinity crosses the polygon an odd number of times
        boolean odd = false;
        // check how many crossings there are
        for (int i = 0, j = poly.length - 1; i < poly.length; i++) { 
            if (((poly[i][1] > point_coords[1]) != (poly[j][1] > point_coords[1]))
                    && (point_coords[0] < (poly[j][0] - poly[i][0]) * (point_coords[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0])) {
                odd = !odd;
            }
            j = i;
        }
        //If the number of crossings was odd, the point is in the polygon
        if (odd) {
        	return true;
        }
        return false;
    }

	
	public static void main( String[] args ) {
		var startLocation = Point.fromLngLat(startLng, startLat);
		Point currLocation = startLocation;
		// list of sensors not yet visited
		var unvisited_sensors = getAllSensors();
		// path the drone takes
		List<Point> path = new ArrayList<Point>();
		path.add(startLocation);
		int turn = 0;
		// markers for the sensors
		List<Feature> markers = new ArrayList<Feature>();
		while(turn < 150) {
			if (unvisited_sensors.isEmpty()) {
				// when all sensors have been read from, go back to starting point with remaining moves
				var dist_to_end = Parser.getDistance(currLocation.longitude(), currLocation.latitude(), startLocation.longitude(), startLocation.latitude());
				var newLoc = backToStart(currLocation, startLocation);
				if (inRange(newLoc)) {
					path.add(newLoc);
					currLocation = newLoc;
					dist_to_end = Parser.getDistance(currLocation.longitude(), currLocation.latitude(), startLocation.longitude(), startLocation.latitude());
				} else { // if out of bounds, error - shouldn't happen
					System.out.println("Error - out of bounds!");
					break;
				}
				if (dist_to_end < 0.0003) { // if position within 0.0003 degrees of starting position, stop
					break;
				}
			} else { // when sensors still need to be read
				var closest_sensor = getClosestSensor(unvisited_sensors, currLocation);
				var dist_to_sensor = Parser.getDistance(currLocation.longitude(), currLocation.latitude(), 
						closest_sensor.getLongitude(), closest_sensor.getLatitude());
				var newLocation = getNewPosition(currLocation, closest_sensor);
				if (inRange(newLocation)) { // for valid new location
					path.add(newLocation);
					currLocation = newLocation;
					dist_to_sensor = Parser.getDistance(currLocation.longitude(), currLocation.latitude(), 
							closest_sensor.getLongitude(), closest_sensor.getLatitude());
				} else { // if out of bounds, error - shouldn't happen
					System.out.println("Error - out of bounds!");
					break;
				}
				if (dist_to_sensor < 0.0002) {
					// read the sensor if close enough to it
					var sensor_location = Point.fromLngLat(closest_sensor.getLongitude(), closest_sensor.getLatitude());
					var f = Feature.fromGeometry((Geometry)sensor_location);
					// format the marker for the sensor
					f.addStringProperty("rgb-string", closest_sensor.getColour());
					f.addStringProperty("marker-color", closest_sensor.getColour());
					f.addStringProperty("marker-symbol", closest_sensor.getSymbol());
					f.addStringProperty("location", closest_sensor.getWords());
					markers.add(f);
					// update unvisited markers list
					unvisited_sensors.remove(closest_sensor);
				}
			}
			turn++;
		}
		// create GEOJSON map
		var initials = MapCreator.getInitialMarkers(day, month, year);
		LineString pathway = LineString.fromLngLats(path);
		Feature path_way = Feature.fromGeometry((Geometry)pathway);
		List<Feature> features = new ArrayList<Feature>();
		features.addAll(initials); // initial markers
		features.add(path_way); // path travelled
		features.addAll(markers); // updated markers for sensors read from
		FeatureCollection fc = FeatureCollection.fromFeatures(features);
		MapCreator.createJSONFile(fc);	
    }
}
