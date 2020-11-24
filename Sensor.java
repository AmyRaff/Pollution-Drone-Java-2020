package uk.ac.ed.inf.aqmaps;

public class Sensor {

	private Coordinate coordinates;
	private String words;
	private boolean visited = false;
	
	public static class Coordinate {
		private double lng;
		private double lat;
	}

	public double getLatitude() {
		return coordinates.lat;
	}
	
	public double getLongitude() {
		return coordinates.lng;
	}

	public String getWords() {
		return words;
	}
	
	// get colour of marker for the sensor
	public String getColour() {
		var rgb = MapUtils.getSensorColour(this);
		return rgb;
	}
	
	// get symbol of marker for the sensor
	public String getSymbol() {
		var symbol = MapUtils.getMarkerSymbol(this);
		return symbol;
	}
	
	public void setVisited() {
		visited = true;
	}
	
	public boolean isVisited() {
		return visited;
	}
	
}
