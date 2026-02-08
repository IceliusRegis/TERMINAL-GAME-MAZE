package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * TileWorldComponent - Holds the data for the game map.
 * * Responsibilities:
 * - Stores the 2D grid of tile IDs (int[][]).
 * - Defines the size of the world and individual tiles.
 * - Acts as the "Level Data" for the TileRenderSystem.
 */

public class TileWorldComponent implements Component {

    //Map Data Height and Width position of the tile map[x][y] = tileID
    public int[][] map;

    //Size of map not in pixels
    public int mapWidth;
    public int mapHeight;

    //Size of one tile in pixels
    public int tileWidth = 32;
    public int tileHeight = 32;

    //Initializes map data
    public void init(int width, int height) {
        this.mapWidth = width;
        this.mapHeight = height;
        this.map = new int[width][height];
    }
}
