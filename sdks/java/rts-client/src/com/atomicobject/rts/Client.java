package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Node;

public class Client {
	
	BufferedReader input;
	OutputStreamWriter out;
	LinkedBlockingQueue<Map<String, Object>> updates;
	Map<Long, Unit> units;
	Tile[][] tiles;
	ArrayList<Tile> resourceTiles = new ArrayList<Tile>();

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

		
		ArrayList<Unit> idleUnits = new ArrayList<Unit>();
		for(int i = 0; i < unitIds.length; i++){
			Unit unit = units.get(unitIds[i]);
			if(unit != null){
				if(unit.status.equals("idle")){
					idleUnits.add(unit);
				}
			}
		}
		
		for(int i = 0; i < tiles.length; i++){
			for(int j = 0; j < tiles[0].length; j++){
				if(tiles[i][j] != null){
					if(tiles[i][j].resources != null){
						resourceTiles.add(tiles[i][j]);
					}
				}
				
			}
		}

		Unit unit = idleUnits.get((int) Math.floor(Math.random() * idleUnits.size()));
		Long unitId = unit.id;

		JSONArray commands = new JSONArray();
		JSONObject command = new JSONObject();	
		int[] coords = {unit.x.intValue(), unit.y.intValue()};
		int[] baseCoords = {0,0};
		
		//Check to see if unit is directly next to a resource
		boolean nextToResource = false;
		String resourceDirection = "";
		if(tiles[coords[0] + tiles.length / 2 + 1 ][coords[1]+ tiles[0].length / 2].resources != null){
			nextToResource = true;
			resourceDirection = "E";
		}
		else if(tiles[coords[0] + tiles.length / 2 -1 ][coords[1]+ tiles[0].length / 2].resources != null){
			nextToResource = true;
			resourceDirection = "W";
		}
		else if(tiles[coords[0]+ tiles.length / 2][coords[1] + tiles[0].length / 2 + 1].resources != null){
			nextToResource = true;
			resourceDirection = "S";
		}
		else if(tiles[coords[0]+ tiles.length / 2][coords[1] + tiles[0].length / 2 - 1].resources != null){
			nextToResource = true;
			resourceDirection = "N";
		}
		if(nextToResource && unit.resource == 0){
			command.put("command", "GATHER");
			command.put("unit", unitId);
			command.put("dir", resourceDirection);
			commands.add(command);
			System.out.println("Resource count:" + unit.resource);

		}

		//If the unit has resources
		else if(unit.resource != null && unit.resource > 0){
			System.out.println("GO HOME\n\n");
			if((Math.abs(coords[0] - baseCoords[0]) <= 1) && (Math.abs(coords[1] - baseCoords[1]) == 0) || (Math.abs(coords[0] - baseCoords[0]) == 0) && (Math.abs(coords[1] - baseCoords[1]) <= 1)){
				System.out.println("DROP\n\n");
				direction = whereToDrop(coords, baseCoords);			
				command.put("command", "DROP");
				command.put("unit", unitId);
				command.put("dir", direction);
				command.put("value", unit.resource);
				commands.add(command);
			}
			else{
				System.out.println("FIND HOME\n\n");
				direction = findPathToPlace(tiles, coords, baseCoords);
				command.put("command", "MOVE");
				command.put("dir", direction);
				command.put("unit", unitId);
				commands.add(command);
			}
			
		}
		else{
			command.put("command", "MOVE");
			command.put("dir", direction);
			command.put("unit", unitId);
			commands.add(command);
		}
		return commands;
	}

	public String whereToDrop(int[] coords, int[] place){
		if(coords[0] - place[0] == 0){
			if(coords[1] - place[1] == 1)
				return "S";
			return "N";
		}
		else{
			if(coords[0] - place[0] == 1)
				return "W";
			return "E";
		}
	}


	public static String findPathToPlace(Tile[][] grid, int[] start, int[] place) {

		final int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

		place[0] = place[0] + grid.length / 2;
		place[1] = place[1] + grid[0].length / 2;

		start[0] = start[0] + grid.length / 2;
		start[1] = start[1] + grid[0].length / 2;

        int rows = grid.length;
        int cols = grid[0].length;
        Queue<Node> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Initialize the queue with the starting position and an empty path
        queue.offer(new Node(start[0], start[1], new ArrayList<>()));
        visited.add(start[0] + "," + start[1]);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int x = current.x;
            int y = current.y;
            List<int[]> path = current.path;

            // Check if we've reached the base
            if (x == place[0] && y == place[1]) {
                //path.add(new int[]{x, y});
				int[] direction = path.get(1);
				System.out.println("x: "+ direction[0]);
				System.out.println("y: "+ direction[1]);
				System.out.println("x-coords: "+ start[0]);
				System.out.println("y-coords: "+ start[1]);
                if(direction[0] - start[0]== 0){
					if(direction[1] - start[1]== 1)
						return "S";
					return "N";
				}
				else{
					if(direction[0] - start[0]== 1)
						return "E";
					return "W";
				}
            }

            // Explore each cardinal direction
            for (int[] direction : directions) {
                int nx = x + direction[0];
                int ny = y + direction[1];
                // Check bounds and if the cell is open and not visited
                if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && grid[nx][ny] != null && grid[nx][ny].blocked != null && grid[nx][ny].blocked == false) {
                    String posKey = nx + "," + ny;
                    if (!visited.contains(posKey)) {
                        visited.add(posKey);
                        List<int[]> newPath = new ArrayList<>(path);
                        newPath.add(new int[]{x, y});
                        queue.offer(new Node(nx, ny, newPath));
                    }
                }
            }
        }

        // If no path is found
        return null;
    }

	// Helper class to store node position and path taken to reach it
    private static class Node {
        int x, y;
        List<int[]> path;

        Node(int x, int y, List<int[]> path) {
            this.x = x;
            this.y = y;
            this.path = path;
        }
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
