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

    // Cached layer references for selective rendering
    private MapLayer groundLayer;
    private MapLayer wallsLayer;
    private int[] groundIndices;
    private int[] wallsIndices;

    // Dynamic wall opacity state
    private float currentWallOpacity = 1f;

    /** Speed of the opacity lerp (units per second). Higher = faster fade. */
    private static final float FADE_SPEED = 5f;

    /** Opacity when the player is behind the Walls layer. */
    private static final float OCCLUDED_ALPHA = 0.5f;

    /** Full opacity when the player is in front of the Walls layer. */
    private static final float FULL_ALPHA = 1f;

    /**
     * Y-coordinate threshold (in world pixels) below which the player is
     * considered "behind" the wall region.  Walls in the current TMX occupy
     * roughly tile-rows 15-40 of a 50-row map (32 px tiles, origin bottom-left).
     * The topmost wall row is ~row 15 from the top = row 35 from bottom →
     * 35 * 32 = 1120 px.  Any player Y above ~1120 is behind the upper walls.
     * We use a generous threshold so the effect triggers as soon as the player
     * enters the wall band.  This value can be tuned per-map.
     */
    private static final float WALL_REGION_TOP_Y = 1120f;

    /**
     * Bottom edge of the wall region in world-Y.
     * Row 40 from top = row 10 from bottom → 10 * 32 = 320 px.
     */
    private static final float WALL_REGION_BOTTOM_Y = 320f;

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

            // 4. Cache layer references by name
            MapLayers allLayers = tiledMap.getLayers();
            groundLayer = allLayers.get("Ground");
            wallsLayer  = allLayers.get("Walls");

            // Build index arrays for selective rendering
            if (groundLayer != null) {
                groundIndices = new int[]{ allLayers.getIndex("Ground") };
            }
            if (wallsLayer != null) {
                wallsIndices = new int[]{ allLayers.getIndex("Walls") };
            }

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
     * Renders only the Ground layer (background pass).
     * Call this BEFORE the SpriteBatch / ECS entity rendering.
     */
    public void renderBackground(OrthographicCamera camera) {
        if (mapRenderer == null) return;

        mapRenderer.setView(camera);

        if (groundIndices != null) {
            mapRenderer.render(groundIndices);
        }
    }

    /**
     * Renders the Walls layer (foreground pass) with dynamic opacity.
     * Call this AFTER SpriteBatch / ECS entity rendering but BEFORE lighting.
     *
     * @param camera          the game camera
     * @param playerTransform the player's TransformComponent (nullable for safety)
     */
    public void renderForeground(OrthographicCamera camera, TransformComponent playerTransform) {
        if (mapRenderer == null) return;

        mapRenderer.setView(camera);

        // --- Dynamic opacity calculation ---
        float targetAlpha = FULL_ALPHA;

        if (playerTransform != null && wallsLayer != null) {
            float playerY = playerTransform.pos.y;

            // If the player is inside the wall band, they are "behind" the walls
            if (playerY >= WALL_REGION_BOTTOM_Y && playerY <= WALL_REGION_TOP_Y) {
                targetAlpha = OCCLUDED_ALPHA;
            }
        }

        // Smooth lerp toward the target opacity
        float delta = Gdx.graphics.getDeltaTime();
        currentWallOpacity = MathUtils.lerp(currentWallOpacity, targetAlpha, FADE_SPEED * delta);

        // Clamp to avoid floating-point drift
        if (Math.abs(currentWallOpacity - targetAlpha) < 0.01f) {
            currentWallOpacity = targetAlpha;
        }

        // Apply opacity and render
        if (wallsLayer != null) {
            wallsLayer.setOpacity(currentWallOpacity);
        }

        if (wallsIndices != null) {
            mapRenderer.render(wallsIndices);
        }
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
        groundLayer = null;
        wallsLayer = null;
        groundIndices = null;
        wallsIndices = null;
    }
}
