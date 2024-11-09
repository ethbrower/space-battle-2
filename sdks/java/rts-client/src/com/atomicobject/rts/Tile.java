package com.atomicobject.rts;
import org.json.simple.JSONObject;

public class Tile {

	TileResource resources;
	Boolean visible;
	Boolean blocked;
	Long x;
	Long y;
	Object units;

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
		units = (Object) json.get("units");
		visible = (Boolean) json.get("visible");

		
	}
	

	


}
