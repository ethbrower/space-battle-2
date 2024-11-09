package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Client {
	
	BufferedReader input;
	OutputStreamWriter out;
	LinkedBlockingQueue<Map<String, Object>> updates;
	Map<Long, Unit> units;
	Tile[][] tiles;

	public Client(Socket socket) {
		updates = new LinkedBlockingQueue<Map<String, Object>>();
		units = new HashMap<Long, Unit>();
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new OutputStreamWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		System.out.println("Starting client threads ...");
		new Thread(() -> readUpdatesFromServer()).start();
		new Thread(() -> runClientLoop()).start();
	}
	
	public void readUpdatesFromServer() {
		String nextLine;
		try {
			while ((nextLine = input.readLine()) != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> update = (Map<String, Object>) JSONValue.parse(nextLine.trim());
				
				updates.add(update);
			}
		} catch (IOException e) {
			// exit thread
		}		
	}

	public void runClientLoop() {
		System.out.println("Starting client update/command processing ...");
		try {
			while (true) {
				processUpdateFromServer();
				respondWithCommands();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeStreams();
	}

	private void processUpdateFromServer() throws InterruptedException {
		Map<String, Object> update = updates.take();
		if (update != null) {
			System.out.println("Processing udpate: " + update);
			@SuppressWarnings("unchecked")
			Collection<JSONObject> unitUpdates = (Collection<JSONObject>) update.get("unit_updates");
			@SuppressWarnings("unchecked")
			Collection<JSONObject> tileUpdates = (Collection<JSONObject>) update.get("tile_updates");
			@SuppressWarnings("unchecked")
			JSONObject gameInfo = (JSONObject) update.get("game_info");		
			//Instantiate Tile 2D array
			if(gameInfo != null){				
				Long width = (Long)gameInfo.get("map_width");
				Long height = (Long)gameInfo.get("map_height");
				tiles = new Tile[width.intValue() * 2 + 1][height.intValue() * 2 + 1];
			}
			addUnitUpdate(unitUpdates);
			addTileUpdate(tileUpdates);
			for(int i = 0; i < tiles.length; i++){
				for(int j = 0; j < tiles[0].length; j++){
					if(tiles[i][j] != null){
						// System.out.println(tiles[i][j].x);
						// System.out.println(tiles[i][j].y);
					}
					else{
						//System.out.println("null tile");
					}
				}
			}
		}
	}

	private void addUnitUpdate(Collection<JSONObject> unitUpdates) {
		unitUpdates.forEach((unitUpdate) -> {
			Long id = (Long) unitUpdate.get("id");
			String type = (String) unitUpdate.get("type");
			if (!type.equals("base")) {
				units.put(id, new Unit(unitUpdate));
			}
			//System.out.println("Units: \n " + units);
		});
	}

	private void addTileUpdate(Collection<JSONObject> tileUpdates) {
		tileUpdates.forEach((tileUpdate) -> {
			Long x = (Long) tileUpdate.get("x");
			Long y = (Long) tileUpdate.get("y");
			//System.out.println(y);
			Tile tile = new Tile(tileUpdate);
			System.out.println(tile.y);
			tiles[x.intValue() + (tiles.length / 2)][y.intValue() + (tiles[0].length / 2)] = tile;
			//System.out.println("Units: \n " + units);

		});
	}

	private void respondWithCommands() throws IOException {
		if (units.size() == 0) return;
		
		JSONArray commands = buildCommandList();		
		sendCommandListToServer(commands);
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildCommandList() {
		String[] directions = {"N","E","S","W"};
		String direction = directions[(int) Math.floor(Math.random() * 4)];

		Long[] unitIds = units.keySet().toArray(new Long[units.size()]);
		Long unitId = unitIds[(int) Math.floor(Math.random() * unitIds.length)];

		JSONArray commands = new JSONArray();
		JSONObject command = new JSONObject();	
		command.put("command", "MOVE");
		command.put("dir", direction);
		command.put("unit", unitId);
		commands.add(command);
		return commands;
	}

	@SuppressWarnings("unchecked")
	private void sendCommandListToServer(JSONArray commands) throws IOException {
		JSONObject container = new JSONObject();
		container.put("commands", commands);
		System.out.println("Sending commands: " + container.toJSONString());
		out.write(container.toJSONString());
		out.write("\n");
		out.flush();
	}

	private void closeStreams() {
		closeQuietly(input);
		closeQuietly(out);
	}

	private void closeQuietly(Closeable stream) {
		try {
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
