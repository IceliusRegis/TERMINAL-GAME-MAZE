package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * TileWorldComponent - Holds the data for the TMX game map.
 * * Responsibilities:
 * - Stores the TiledMap and the specific Collision layer.
 * - Provides the isSolid() helper for the MovementSystem to read map boundaries.
 */

public class TileWorldComponent implements Component {

    public TiledMap tiledMap;
    public TiledMapTileLayer collisionLayer;

    //Size of map in tiles (50x50)
    public int mapWidthTiles;
    public int mapHeightTiles;

    //Size of one tile in pixels
    public int tileWidth = 32;
    public int tileHeight = 32;

    //Non-default constructor: Forces the map to load collision
   public TileWorldComponent(TiledMap tiledMap) {
       this.tiledMap = tiledMap;
       this.collisionLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Collision");

       if (this.collisionLayer == null ) {
           throw  new IllegalStateException("CRITICAL: TMX Map is missing the required 'Collision' layer!");
       }

       this.mapWidthTiles = collisionLayer.getWidth();
       this.mapHeightTiles =collisionLayer.getHeight();
   }

    /**
     * Checks if a specific tile coordinate is a solid wall or out of bounds.
     */

    public boolean isSolid(int tileX, int tileY) {
        // 1. Map border check: Everything outside the map is a solid wall
        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return true;
        }

        // 2. Collision layer check: If a tile is painted here, it's solid
        return collisionLayer.getCell(tileX, tileY) != null;
    }

}
