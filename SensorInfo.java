package uk.ac.ed.inf.aqmaps;

public class SensorInfo {

	private String country;
	private Square square;
	private String nearestPlace;
	private Coordinate coordinates;
	private String words;
	private String language;
	private String map;
	
	public static class Coordinate {
		private double lng;
		private double lat;
	}
	
	public static class Square {
		private Coordinate southwest;
		private Coordinate northeast;
	}

	// turns a Coordinate object into readable format
	public double[] parseCoords(Coordinate c) {
		final double[] coords = new double[2];
		coords[0] = c.lng;
		coords[1] = c.lat;
		return coords;
	}
	
	// sw - getSquare[0][0] returns lng, getSquare[0][1] returns lat
	// ne - getSquare[1][0] returns lng, getSquare[1][1] returns lat
	public double[][] getSquare() {
		Coordinate sw = square.southwest;
		Coordinate ne = square.northeast;
		// 2D array - 2 Coordinate objects each with lng and lat
		double[][] sq = new double[2][2];
		sq[0] = parseCoords(sw);
		sq[1] = parseCoords(ne);
		return sq;
	}

	// getCoordinates[0] returns lng, getCoordinates[1] returns lat
	public double[] getCoordinates() {
		return parseCoords(coordinates);
	}

	public String getWords() {
		return words;
	}
	
}
