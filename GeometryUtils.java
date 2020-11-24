package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class GeometryUtils {

	// get Euclidean distance between two points
	public static double getDistance(double lng1, double lat1, double lng2, double lat2) {
		var xs = Math.pow((lng1 - lng2), 2);
		var ys = Math.pow((lat1 - lat2), 2);
		var distance = Math.sqrt(xs + ys);
		return distance;
	}
		
	// checks if a point is in any of the no fly zones
	public static boolean inNoFlyZone(Point point) {
		var zones = FileReader.getNoFlyZones();
		for (Polygon zone : zones) {
			if (pointInPolygon(zone, point)) {
				return true;
			}
		}
		return false;
	}
		
	// returns whether a point is within a polygon
	private static boolean pointInPolygon(Polygon polygon, Point point) {
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

}
