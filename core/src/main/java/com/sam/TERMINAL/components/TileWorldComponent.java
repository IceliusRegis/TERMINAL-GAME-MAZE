package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * TileWorldComponent - Holds the data for the TMX game map.
 *
 * Responsibilities:
 * - Stores the TiledMap and specific named layers.
 * - Provides isSolid() for movement and spawn validation (Collision + Walls).
 * - Provides isWall() for line-of-sight checks (Walls layer only).
 * - Exposes the Winning layer for proximity-based win checks.
 */
public class TileWorldComponent implements Component {

    public TiledMap tiledMap;
    public TiledMapTileLayer collisionLayer;
    public TiledMapTileLayer wallsLayer;
    public TiledMapTileLayer winningLayer;
    public TiledMapTileLayer groundLayer;
    public TiledMapTileLayer noSpawnLayer;
    public java.util.List<com.badlogic.gdx.math.GridPoint2> validSpawnPoints = new java.util.ArrayList<>();

    // Size of map in tiles
    public int mapWidthTiles;
    public int mapHeightTiles;

    // Size of one tile in pixels
    public int tileWidth = 32;
    public int tileHeight = 32;

    // Non-default constructor: Forces the map to load collision
    public TileWorldComponent(TiledMap tiledMap) {
        this.tiledMap = tiledMap;
        this.collisionLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Collision");

        if (this.collisionLayer == null) {
            throw new IllegalStateException("CRITICAL: TMX Map is missing the required 'Collision' layer!");
        }

        this.mapWidthTiles = collisionLayer.getWidth();
        this.mapHeightTiles = collisionLayer.getHeight();

        // Optional layers — may be null if the map doesn't have them
        Object wallsRaw = tiledMap.getLayers().get("Walls");
        this.wallsLayer = (wallsRaw instanceof TiledMapTileLayer) ? (TiledMapTileLayer) wallsRaw : null;

        Object winningRaw = tiledMap.getLayers().get("Winning");
        this.winningLayer = (winningRaw instanceof TiledMapTileLayer) ? (TiledMapTileLayer) winningRaw : null;

        Object noSpawnRaw = tiledMap.getLayers().get("No Spawn");
        if (noSpawnRaw == null) {
            noSpawnRaw = tiledMap.getLayers().get("NoSpawn");
        }
        this.noSpawnLayer = (noSpawnRaw instanceof TiledMapTileLayer) ? (TiledMapTileLayer) noSpawnRaw : null;

        for (com.badlogic.gdx.maps.MapLayer layer : tiledMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                String name = layer.getName().toLowerCase();
                if (name.contains("ground") || name.contains("floor")) {
                    this.groundLayer = (TiledMapTileLayer) layer;
                    break;
                }
            }
        }

        cacheValidSpawnPoints();
    }

    private void cacheValidSpawnPoints() {
        if (groundLayer == null)
            return;

        for (int x = 0; x < mapWidthTiles; x++) {
            for (int y = 0; y < mapHeightTiles; y++) {
                // Must have a tile on the ground/floor layer
                if (groundLayer.getCell(x, y) != null) {
                    // Must not be a wall or collision tile
                    if (!isSolidForSpawning(x, y)) {
                        validSpawnPoints.add(new com.badlogic.gdx.math.GridPoint2(x, y));
                    }
                }
            }
        }
    }

    public com.badlogic.gdx.math.GridPoint2 getRandomSpawnPoint(com.badlogic.gdx.math.GridPoint2... avoidPoints) {
        if (validSpawnPoints.isEmpty())
            return null;

        com.badlogic.gdx.math.GridPoint2 pt;
        int maxAttempts = 50;
        int attempts = 0;
        do {
            int index = com.badlogic.gdx.math.MathUtils.random(validSpawnPoints.size() - 1);
            pt = validSpawnPoints.get(index);

            boolean conflict = false;
            for (com.badlogic.gdx.math.GridPoint2 avoid : avoidPoints) {
                if (avoid != null && avoid.x == pt.x && avoid.y == pt.y) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict)
                return pt;
            attempts++;
        } while (attempts < maxAttempts);

        return pt;
    }

    /**
     * Checks if a specific tile coordinate is blocked for MOVEMENT.
     * Only checks the Collision layer — this is what MovementSystem and
     * EnemySystem use. Visual wall tiles on the Walls layer do NOT block
     * movement (entrances, corridors have wall art but no collision).
     */
    public boolean isSolid(int tileX, int tileY) {
        // 1. Map border check: Everything outside the map is a solid wall
        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return true;
        }

        // 2. Collision layer check only
        return collisionLayer.getCell(tileX, tileY) != null;
    }

    /**
     * Checks if a tile is blocked for ITEM SPAWNING.
     * Checks both Collision and Walls layers so items never appear inside
     * visual walls, even if there is no collision tile there.
     */
    public boolean isSolidForSpawning(int tileX, int tileY) {
        if (isSolid(tileX, tileY)) {
            return true;
        }
        if (wallsLayer != null && wallsLayer.getCell(tileX, tileY) != null) {
            return true;
        }
        if (noSpawnLayer != null && noSpawnLayer.getCell(tileX, tileY) != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a tile is specifically a wall (Walls layer only).
     * Used for line-of-sight checks where we want to know about visual walls,
     * not generic collision.
     */
    public boolean isWall(int tileX, int tileY) {
        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return true;
        }
        if (wallsLayer != null && wallsLayer.getCell(tileX, tileY) != null) {
            return true;
        }
        return false;
    }
}
