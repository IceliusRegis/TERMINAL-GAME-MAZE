package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * MapManager - Owns the TiledMap lifecycle and layered rendering.
 *
 * Responsibilities:
 * - Loads the .tmx file from disk.
 * - Creates the TileWorldComponent and adds it to the ECS Engine.
 * - Renders map layers in two passes (Ground behind sprites, Walls in front)
 *   to produce correct depth ordering with Y-sorted ECS entities.
 * - Dynamically fades the Walls layer when the player is behind it, so the
 *   player sprite remains visible underneath.
 * - Safely disposes of heavy map assets to prevent memory leaks.
 */
public class MapManager {

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private Entity mapEntity;
    private PooledEngine engine;

    public MapManager(PooledEngine engine) {
        this.engine = engine;
    }

    public void loadMap(String tmxPath) {
        // 1. Clean up any existing map before loading a new one
        this.dispose();

        try {
            // 2. Load the actual TMX file from assets
            tiledMap = new TmxMapLoader().load(tmxPath);

            // 3. Create the LibGDX renderer for this specific map
            mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f);

            // 4. Log layer findings
            MapLayers allLayers = tiledMap.getLayers();
            MapLayer groundLayer = allLayers.get("Ground");
            MapLayer wallsLayer  = allLayers.get("Walls");

            Gdx.app.log("MAP_MANAGER", "Ground layer: " + (groundLayer != null ? "found" : "MISSING"));
            Gdx.app.log("MAP_MANAGER", "Walls layer: "  + (wallsLayer  != null ? "found" : "MISSING"));

            // 5. Create your new TileWorldComponent
            TileWorldComponent worldComp = new TileWorldComponent(tiledMap);

            // 6. Create an Ashley Entity to hold the map data
            mapEntity = engine.createEntity();
            mapEntity.add(worldComp);
            engine.addEntity(mapEntity);

            Gdx.app.log("MAP_MANAGER", "Successfully loaded map: " + tmxPath +
                " (" + worldComp.mapWidthTiles + "x" + worldComp.mapHeightTiles + " tiles)");

        } catch (Exception e) {
            Gdx.app.error("MAP_MANAGER", "Failed to load map: " + tmxPath, e);
        }
    }

    /**
     * Renders the entire map in one single pass.
     * Call this BEFORE the SpriteBatch / ECS entity rendering.
     */
    public void renderMap(OrthographicCamera camera) {
        if (mapRenderer == null) return;

        mapRenderer.setView(camera);
        mapRenderer.render();
    }

    public void dispose() {
        if (mapRenderer != null) {
            mapRenderer.dispose();
            mapRenderer = null;
        }
        if (tiledMap != null) {
            tiledMap.dispose();
            tiledMap = null;
        }
        if (mapEntity != null && engine != null) {
            engine.removeEntity(mapEntity);
            mapEntity = null;
        }
    }
}
