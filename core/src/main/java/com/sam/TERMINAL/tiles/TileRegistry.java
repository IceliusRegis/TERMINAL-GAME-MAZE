package com.sam.TERMINAL.tiles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import  java.util.HashMap;
import java.util.Map;

/**
 * TileRegistry - The central dictionary for all tile types.
 * * Responsibilities:
 * - Stores unique Tile definitions mapped to an Integer ID.
 * - Allows Systems to look up "What is Tile #5?" instantly.
 */

public class TileRegistry {

    //It assigns the ID to the tiles basically like a dictionary
   private static Map<Integer, Tile> tiles = new HashMap<>();

   //This Defines a new tile type. Called upon during game initialization. This puts the tile in the hashmap
    public static void registerTile(int id, boolean isSolid, TextureRegion texture){
        tiles.put(id, new Tile(id, isSolid, texture));
    }

    //Retrives the tile itself
    public static Tile getTile(int id) {
        return tiles.get(id);
    }
}
