package uk.ac.ed.inf.heatmap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class App 
{
    public static void main( String[] args )
    {
    	// Corner points of region
    	double max_lat = 55.946233;
    	double min_lat = 55.942617;
    	double max_lng = -3.184319;
    	double min_lng = -3.192473;
    	// initialise list of features to be added
    	List<Feature> features = new ArrayList<Feature>();
        // x (width) and y (height) dimensions of the polygons
        double poly_x = (max_lat - min_lat) / 10;
        double poly_y = (max_lng - min_lng) / 10;
        // creating the polygons
        for(int i = 0; i < 10; i++) {
        	for(int j = 0; j < 10; j++) {
        		List<Point> coords = new ArrayList<Point>();
        		// coordinates of polygons
            	coords.add(Point.fromLngLat(min_lng + i*poly_y, min_lat + j*poly_x));
            	coords.add(Point.fromLngLat(min_lng + i*poly_y, min_lat + (j+1)*poly_x));
            	coords.add(Point.fromLngLat(min_lng + (i+1)*poly_y, min_lat + (j+1)*poly_x));
            	coords.add(Point.fromLngLat(min_lng + (i+1)*poly_y, min_lat + j*poly_x));
            	coords.add(Point.fromLngLat(min_lng + i*poly_y, min_lat + j*poly_x));
            	Polygon polygon = Polygon.fromLngLats(List.of(coords));
            	// formatting polygons
            	Feature poly = Feature.fromGeometry((Geometry)polygon);
            	poly.addNumberProperty("fill-opacity", 0.75);
            	poly.addStringProperty("rgb-string", getPolygonColour(i, j));
            	poly.addStringProperty("fill", getPolygonColour(i, j));
            	features.add(poly);
        	}
        }
        // creating the GEOJSON graph
        FeatureCollection fc = FeatureCollection.fromFeatures(features);
        createJSONFile(fc);
    };
    
    public static void createJSONFile(FeatureCollection fc) {
    	try {
    		// create the GEOJSON file and write to it the contents of the feature collection
    		FileWriter file = new FileWriter("output.geojson");
    		file.write(fc.toJson());
	        file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    };
    
    public static String getPolygonPollution(int i, int j) {
    	// gets pollution value of each polygon as a string
    	int partitions = 10;
    	String line;
    	// lines of text file
		List<String> lines = new ArrayList<String>();
		// matrix for pollution values corresponding to matrix of polygons
		String[][] pollution = new String[partitions][partitions];
    	try {
    		// reading predictions.txt
			FileReader file = new FileReader("predictions.txt");
			BufferedReader buffered = new BufferedReader(file);
			try {
				while ((line = buffered.readLine()) != null) {
					// while file has not ended
					lines.add(line);
					// convert text file into a 2D array
					for(int n = 0; n < lines.size(); n++) {
						String current = lines.get(n);
						String[] split = current.split(", ");
						for(int k = 0; k < split.length; k++) {
							pollution[n][k] = split[k];
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	return pollution[partitions - 1 -j][i];
    };
    
    public static String getPolygonColour(int i, int j) {
    	// returns rgb string corresponding to the colour of a specific polygon
    	int pollution_level = Integer.parseInt(getPolygonPollution(i, j));
    	String rgb = null;
    	// find colour for polygon based on pollution level
    	if(pollution_level < 0) {
    		throw new IllegalArgumentException("Negative Pollution!");
    	} else if(pollution_level < 32) {
    		rgb = "#00ff00";
    	} else if(pollution_level < 64) {
    		rgb = "#40ff00";
    	} else if(pollution_level < 96) {
    		rgb = "#80ff00";
    	} else if(pollution_level < 128) {
    		rgb = "#c0ff00";
    	} else if(pollution_level < 160) {
    		rgb = "#ffc000";
    	} else if(pollution_level < 192) {
    		rgb = "#ff8000";
    	} else if(pollution_level < 224) {
    		rgb = "#ff4000";
    	} else if(pollution_level < 256) {
    		rgb = "#ff0000";
    	} else if(pollution_level >= 256) {
    		throw new IllegalArgumentException("Pollution Above Range!");
    	}
    	return rgb;
    }
}