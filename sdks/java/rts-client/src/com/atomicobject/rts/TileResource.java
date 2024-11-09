package com.atomicobject.rts;
import org.json.simple.JSONObject;

public class TileResource {

	Long id;
	String type;
	Long total;
	Long value;

	public TileResource(JSONObject json) {
		id = (Long) json.get("id");
		type = (String) json.get("type");
		total= (Long) json.get("total");
		value = (Long) json.get("value");


		
	}

	


}
