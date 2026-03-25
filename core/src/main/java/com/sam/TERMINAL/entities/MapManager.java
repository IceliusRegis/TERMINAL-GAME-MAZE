package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Array;
import com.sam.TERMINAL.components.RoofComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.components.TransformComponent;
import com.sam.TERMINAL.components.WallComponent;

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
    private int[] backgroundLayers;
    private final Array<Entity> wallEntities = new Array<>();

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
            // 4. Log layer findings and setup background rendering
            MapLayers allLayers = tiledMap.getLayers();
            MapLayer groundLayer = allLayers.get("Ground");
            MapLayer wallsLayer = allLayers.get("Walls");

            int groundIndex = allLayers.getIndex(groundLayer);
            if (groundIndex != -1) {
                backgroundLayers = new int[] { groundIndex };
                Gdx.app.log("MAP_MANAGER", "Ground layer: found at index " + groundIndex);
            } else {
                backgroundLayers = new int[0];
                Gdx.app.log("MAP_MANAGER", "Ground layer: MISSING");
            }

            // --- UPDATED WALL & ROOF EXTRACTION LOGIC ---
            String[] structuralLayers = { "Walls", "Wall Roofs" };
            for (String layerName : structuralLayers) {
                MapLayer layer = allLayers.get(layerName);
                if (layer instanceof TiledMapTileLayer) {
                    TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null) {
                                Entity wallEntity = engine.createEntity();
                                TransformComponent transform = engine.createComponent(TransformComponent.class);
                                SpriteComponent sprite = engine.createComponent(SpriteComponent.class);

                                // 1. Get the actual texture region first
                                sprite.staticSprite = cell.getTile().getTextureRegion();
                                sprite.isStatic = true;

                                // 1b. Capture Tiled flip metadata from the Cell
                                sprite.flipX = cell.getFlipHorizontally();
                                sprite.flipY = cell.getFlipVertically();

                                // 2. Use the TEXTURE's true dimensions
                                float actualWidth = sprite.staticSprite.getRegionWidth();
                                float actualHeight = sprite.staticSprite.getRegionHeight();

                                // 3. Add Tiled's native offsets to prevent shifting
                                float offsetX = cell.getTile().getOffsetX();
                                float offsetY = cell.getTile().getOffsetY();

                                // 4. Set the transform
                                transform.pos.set((x * tileLayer.getTileWidth()) + offsetX,
                                        (y * tileLayer.getTileHeight()) + offsetY);
                                transform.width = actualWidth;
                                transform.height = actualHeight;
                                transform.updateBounds();

                                wallEntity.add(transform);
                                wallEntity.add(sprite);

                                // === THE NEW IMPLEMENTATION ===
                                if (layerName.equals("Wall Roofs")) {
                                    wallEntity.add(engine.createComponent(RoofComponent.class));
                                } else {
                                    wallEntity.add(engine.createComponent(WallComponent.class));
                                }

                                // ── NEW SMART SCANNING LOGIC ─────────────────────────────
                                // Scan downwards to find the true bottom base of this wall column
                                int baseY = y;
                                while (baseY > 0) {
                                    boolean currentIsWall = isTileInLayer(x, baseY, allLayers, "Walls");
                                    boolean nextIsWall = isTileInLayer(x, baseY - 1, allLayers, "Walls");
                                    boolean nextIsRoof = isTileInLayer(x, baseY - 1, allLayers, "Wall Roofs");

                                    if (nextIsWall) {
                                        baseY--; // Always link downwards into a wall face
                                    } else if (nextIsRoof) {
                                        if (!currentIsWall) {
                                            baseY--; // Link Roof to Roof (for multi-tile tall roofs)
                                        } else {
                                            // DANGER: We found a Roof directly under a Wall Face.
                                            // This means it's a completely different wall behind the corridor!
                                            break;
                                        }
                                    } else {
                                        break; // Empty floor
                                    }
                                }
                                // ─────────────────────────────────────────────────────────

                                float shiftAmount = (y - baseY) * tileLayer.getTileHeight();
                                if (layerName.equals("Wall Roofs")) {
                                    RoofComponent rc = engine.createComponent(RoofComponent.class);
                                    rc.sortYShift = shiftAmount;
                                    wallEntity.add(rc);
                                } else {
                                    WallComponent wc = engine.createComponent(WallComponent.class);
                                    wc.sortYShift = shiftAmount;
                                    wallEntity.add(wc);
                                }

                                engine.addEntity(wallEntity);
                                wallEntities.add(wallEntity);
                            }
                        }
                    }
                    Gdx.app.log("MAP_MANAGER", layerName + " layer: found and converted to static entities");
                } else {
                    Gdx.app.log("MAP_MANAGER", layerName + " layer: MISSING or not a TileLayer");
                }
            }

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

    public void render(OrthographicCamera camera) {
        // Guard clause: Don't render if the map hasn't loaded yet
        if (mapRenderer == null)
            return;

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
        if (engine != null) {
            for (Entity wall : wallEntities) {
                engine.removeEntity(wall);
            }
        }
        wallEntities.clear();
    }

    // --- HELPER METHOD TO CHECK TILE LAYERS SAFELY ---
    private boolean isTileInLayer(int x, int y, MapLayers layers, String layerName) {
        MapLayer layer = layers.get(layerName);
        if (layer instanceof TiledMapTileLayer) {
            return ((TiledMapTileLayer) layer).getCell(x, y) != null;
        }
        return false;
    }
}