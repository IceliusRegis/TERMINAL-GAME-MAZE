package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.sam.TERMINAL.components.TileWorldComponent;



/**
 * MapManager - Owns the TiledMap lifecycle and rendering.
 *
 * Responsibilities:
 * - Loads the .tmx file from disk.
 * - Creates the TileWorldComponent and adds it to the ECS Engine.
 * - Renders the visual map layers independently of the main SpriteBatch.
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

            // 4. Create your new TileWorldComponent
            TileWorldComponent worldComp = new TileWorldComponent(tiledMap);

            // 5. Create an Ashley Entity to hold the map data
            mapEntity = engine.createEntity();
            mapEntity.add(worldComp);
            engine.addEntity(mapEntity);

            Gdx.app.log("MAP_MANAGER", "Successfully loaded map: " + tmxPath +
                " (" + worldComp.mapWidthTiles + "x" + worldComp.mapHeightTiles + " tiles)");

        } catch (Exception e) {
            Gdx.app.error("MAP_MANAGER", "Failed to load map: " + tmxPath, e);
        }
    }

    public void render(OrthographicCamera camera) {
        // Guard clause: Don't render if the map hasn't loaded yet
        if (mapRenderer == null) return;

        // Tell the renderer what the camera is looking at, then draw the tiles
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
