package com.atomicobject.rts;
import org.json.simple.JSONObject;

public class Tile {

	Object resources;
	Boolean visible;
	Boolean blocked;
	Long x;
	Long y;
	Object units;

	public Tile(JSONObject json) {
		resources = (Object) json.get("resources");
		x = (Long) json.get("x");
		y = (Long) json.get("y");
		blocked = (Boolean) json.get("blocked");
		units = (Object) json.get("units");
		visible = (Boolean) json.get("visible");

		
	}
	

	


}
