package com.atomicobject.rts;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Tile {

	TileResource resources;
	Boolean visible;
	Boolean blocked;
	Long x;
	Long y;
	ArrayList<EnemyUnit> units;

	public Tile(JSONObject json) {
		JSONObject resourceJson = (JSONObject)json.get("resources");
		if(resourceJson != null){
			resources = new TileResource((resourceJson));	
		}
		else{
			resources = null;
		}
		x = (Long) json.get("x");
		y = (Long) json.get("y");
		blocked = (Boolean) json.get("blocked");
		units = new ArrayList<>();
		JSONArray unitsArray = (JSONArray) json.get("units");
		if(unitsArray != null) {
			for (Object unitObj : unitsArray) { 
				// Assuming EnemyUnit has a constructor that takes a JSONObject 
				units.add(new EnemyUnit((JSONObject) unitObj)); }
		}
		visible = (Boolean) json.get("visible");
	}
	

	


}
