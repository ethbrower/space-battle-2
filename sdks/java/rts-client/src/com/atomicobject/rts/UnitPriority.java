package com.atomicobject.rts;

import java.util.Comparator;

class UnitPriority implements Comparator<Unit> {
    private Tile[][] tiles;  // The tile map

    // Constructor that takes the tile map
    public UnitPriority(Tile[][] tiles) {
        this.tiles = tiles;
    }

    @Override
    public int compare(Unit u1, Unit u2) {
        // First, prioritize units with resources (i.e., actively doing something)
        if (u1.resource > 0 && u2.resource == 0) {
            return -1; // u1 has resources, so it's more important
        } else if (u1.resource == 0 && u2.resource > 0) {
            return 1; // u2 has resources, so it's more important
        }

        // If both have resources or both don't have resources, compare distance to base
        int distance1 = getDistanceToBase(u1);
        int distance2 = getDistanceToBase(u2);

        // Priority: units farther from base should act first
        return Integer.compare(distance2, distance1);  // Higher distance -> higher priority
    }


    // // Calculate the Euclidean distance from the unit to the base 
    // private double calculateDistanceToBase(Unit unit) {
    //     return Math.sqrt(Math.pow(unit.x, 2) + Math.pow(unit.y, 2)); 
    // }    

    private int getDistanceToBase(Unit unit) {
        // Assuming units have x, y coordinates that can be used for the distance calculation
        int dx = Math.abs(unit.x.intValue() - 0);
        int dy = Math.abs(unit.y.intValue() - 0);
        return dx + dy;  // Manhattan distance (change this if you want diagonal distance)
    }


    // Check if the unit is near an enemy by checking surrounding tiles
    // for later melee implementation
    private boolean isUnitNearEnemy(Unit unit) {
        Long unitX = unit.x;
        Long unitY = unit.y;

        if (tiles == null) {
            System.err.println("Error: Tiles array is not initialized.");
            return false;
        }        

        // Check tiles around the unit
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (unitX + dx >= 0 && unitX + dx < tiles.length &&
                    unitY + dy >= 0 && unitY + dy < tiles[0].length) {
                    Tile tile = tiles[(int) (unitX + dx)][(int) (unitY + dy)];
                    if (tile != null && tile.units != null){
                        if(tile.units.size() != 0) {
                        return true;  // There are enemy units nearby
                        }
                    }
                }
            }
        }
        return false;  // No enemies nearby
    }
}
