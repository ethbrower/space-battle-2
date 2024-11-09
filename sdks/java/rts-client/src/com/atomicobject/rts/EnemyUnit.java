package com.atomicobject.rts;
import org.json.simple.JSONObject;

public class EnemyUnit {

	Long health;
	String type;
	Long playerId;
	Long id;
	String status;

	public EnemyUnit(JSONObject json) {
		health = (Long) json.get("health");
		type = (String) json.get("type");
		playerId = (Long) json.get("player_id");
		id = (Long) json.get("id");
		status = (String) json.get("status");
	}
}
