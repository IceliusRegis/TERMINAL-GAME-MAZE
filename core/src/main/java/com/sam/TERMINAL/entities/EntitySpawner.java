package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.persistence.GameData;

/**
 * EntitySpawner — Calculates spawn positions and creates entities.
 *
 * All item spawns are validated against TileWorldComponent.isSolidForSpawning()
 * to guarantee items never appear inside walls.
 */
public class EntitySpawner {

    private static final float TILE_SIZE = 32f;

    // Default hardcoded tile positions (calculated from LibGDX bottom-left origin)
    // Tiled coordinates (15, 7) on a 50x50 map equal LibGDX coordinates (15, 42)
    private static final float PLAYER_X = 15 * TILE_SIZE;
    private static final float PLAYER_Y = 42 * TILE_SIZE;
    private static final int KEY_TILE_X = 20;
    private static final int KEY_TILE_Y = 10;
    private static final float ENEMY_X = 5 * TILE_SIZE;
    private static final float ENEMY_Y = 40 * TILE_SIZE;

    public static final String KEY_SAVE_ID = "KEY_BEEP_MAIN";
    public static final String FLASHLIGHT_SAVE_ID = "ITEM_FLASHLIGHT";
    public static final String BATTERY_SAVE_ID = "ITEM_BATTERY";

    public static int totalBeepCardsSpawned = 0;

    /** Maximum random attempts before falling back to spiral scan. */
    private static final int MAX_RANDOM_ATTEMPTS = 100;

    // =========================================================================
    // Public API
    // =========================================================================

    public static void spawnInitialEntities(PooledEngine engine,
            TextureRegion beepRegion,
            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
            TextureRegion enemyRegion, TextureRegion flashlightRegion, TextureRegion batteryRegion) {

        TileWorldComponent world = getWorldComponent(engine);

        int mapWidth = (world != null) ? world.mapWidthTiles : 50;
        int mapHeight = (world != null) ? world.mapHeightTiles : 50;

        int pTileX = (int) (PLAYER_X / TILE_SIZE);
        int pTileY = (int) (PLAYER_Y / TILE_SIZE);

        // --- ACTUAL CREATION ---
        EntityFactory.createPlayer(engine, PLAYER_X, PLAYER_Y, 24f, 15f, walkAnimation, idleAnimation);
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);

        spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, world, pTileX, pTileY, mapWidth, mapHeight);
    }

    public static void spawnItems(PooledEngine engine,
            TextureRegion beepRegion,
            TextureRegion flashlightRegion,
            TextureRegion batteryRegion,
            TileWorldComponent world,
            int pTileX, int pTileY,
            int mapWidth, int mapHeight) {

        // --- SAFE BEEP CARDS POSITION ---
        // Spawn 3 to 5 beep cards
        totalBeepCardsSpawned = 3 + (int) (Math.random() * 3);
        com.badlogic.gdx.math.GridPoint2 usedPoint = null;
        for (int i = 0; i < totalBeepCardsSpawned; i++) {
            com.badlogic.gdx.math.GridPoint2 randomCardSpawn = world != null ? world.getRandomSpawnPoint(usedPoint)
                    : null;
            int keyTileX = KEY_TILE_X;
            int keyTileY = KEY_TILE_Y;

            if (randomCardSpawn != null) {
                keyTileX = randomCardSpawn.x;
                keyTileY = randomCardSpawn.y;
                usedPoint = randomCardSpawn;
            } else if (world != null && world.isSolidForSpawning(keyTileX, keyTileY)) {
                int[] safe = findSafeTile(world, keyTileX, keyTileY, 8, pTileX, pTileY, 0);
                keyTileX = safe[0];
                keyTileY = safe[1];
            }
            EntityFactory.createKey(engine, keyTileX * TILE_SIZE, keyTileY * TILE_SIZE, beepRegion,
                    KEY_SAVE_ID + "_" + i);
        }

        // --- SAFE FLASHLIGHT POSITION ---
        int flTileX = pTileX + 6;
        int flTileY = pTileY + 6;

        com.badlogic.gdx.math.GridPoint2 randomFlSpawn = world != null ? world.getRandomSpawnPoint(usedPoint) : null;
        if (randomFlSpawn != null) {
            flTileX = randomFlSpawn.x;
            flTileY = randomFlSpawn.y;
            usedPoint = randomFlSpawn;
            Gdx.app.log("SPAWNER", "Flashlight relocated to safe random tile (" + flTileX + ", " + flTileY + ")");
        } else if (world != null) {
            int maxFlRadius = 12;
            int minFlDist = 4;
            int[] flSafe = findSafeTileRandom(world, pTileX, pTileY, maxFlRadius, minFlDist, mapWidth, mapHeight);
            flTileX = flSafe[0];
            flTileY = flSafe[1];
        }

        float flPixelX = flTileX * TILE_SIZE;
        float flPixelY = flTileY * TILE_SIZE;

        // --- SAFE BATTERY POSITION ---
        int batTileX = pTileX + 8;
        int batTileY = pTileY + 8;
        com.badlogic.gdx.math.GridPoint2 randomBatSpawn = world != null ? world.getRandomSpawnPoint(usedPoint) : null;
        if (randomBatSpawn != null) {
            batTileX = randomBatSpawn.x;
            batTileY = randomBatSpawn.y;
        } else if (world != null) {
            int[] batSafe = findSafeTileRandom(world, pTileX, pTileY, 15, 6, mapWidth, mapHeight);
            batTileX = batSafe[0];
            batTileY = batSafe[1];
        }

        EntityFactory.createFlashlight(engine, flPixelX, flPixelY, flashlightRegion, FLASHLIGHT_SAVE_ID);
        EntityFactory.createBattery(engine, batTileX * TILE_SIZE, batTileY * TILE_SIZE, batteryRegion, BATTERY_SAVE_ID);
    }

    /**
     * Re-creates entities from save data. Item positions are validated
     * against the collision layer before placement.
     */
    public static void spawnForLoad(PooledEngine engine, GameData saveData,
            TextureRegion beepRegion,
            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
            TextureRegion enemyRegion, TextureRegion flashlightRegion, TextureRegion batteryRegion) {

        TileWorldComponent world = getWorldComponent(engine);

        // Restore Player position from save
        EntityFactory.createPlayer(engine, saveData.playerX, saveData.playerY, 24f, 15f, walkAnimation, idleAnimation);
        totalBeepCardsSpawned = saveData.totalBeepCardsSpawned > 0 ? saveData.totalBeepCardsSpawned : 3;

        // --- SAFE BEEP CARDS POSITION ---
        com.badlogic.gdx.math.GridPoint2 usedPoint = null;
        for (int i = 0; i < totalBeepCardsSpawned; i++) {
            int keyTileX = KEY_TILE_X;
            int keyTileY = KEY_TILE_Y;
            com.badlogic.gdx.math.GridPoint2 randomCardSpawn = world != null ? world.getRandomSpawnPoint(usedPoint)
                    : null;
            if (randomCardSpawn != null) {
                keyTileX = randomCardSpawn.x;
                keyTileY = randomCardSpawn.y;
                usedPoint = randomCardSpawn;
            } else if (world != null && world.isSolidForSpawning(keyTileX, keyTileY)) {
                int playerTX = (int) (saveData.playerX / TILE_SIZE);
                int playerTY = (int) (saveData.playerY / TILE_SIZE);
                int[] safe = findSafeTile(world, keyTileX, keyTileY, 8, playerTX, playerTY, 0);
                keyTileX = safe[0];
                keyTileY = safe[1];
            }
            EntityFactory.createKey(engine, keyTileX * TILE_SIZE, keyTileY * TILE_SIZE, beepRegion,
                    KEY_SAVE_ID + "_" + i);
        }

        // --- SAFE FLASHLIGHT POSITION (load path) ---
        int flTileX = KEY_TILE_X + 1;
        int flTileY = KEY_TILE_Y;
        com.badlogic.gdx.math.GridPoint2 randomFlSpawn = world != null ? world.getRandomSpawnPoint(usedPoint) : null;
        if (randomFlSpawn != null) {
            flTileX = randomFlSpawn.x;
            flTileY = randomFlSpawn.y;
            usedPoint = randomFlSpawn;
        } else if (world != null && world.isSolidForSpawning(flTileX, flTileY)) {
            int playerTX = (int) (saveData.playerX / TILE_SIZE);
            int playerTY = (int) (saveData.playerY / TILE_SIZE);
            int[] safe = findSafeTile(world, flTileX, flTileY, 8, playerTX, playerTY, 0);
            flTileX = safe[0];
            flTileY = safe[1];
        }
        EntityFactory.createFlashlight(engine, flTileX * TILE_SIZE, flTileY * TILE_SIZE,
                flashlightRegion, FLASHLIGHT_SAVE_ID);

        // --- SAFE BATTERY POSITION (load path) ---
        int batTileX = KEY_TILE_X + 2;
        int batTileY = KEY_TILE_Y;
        com.badlogic.gdx.math.GridPoint2 randomBatSpawn = world != null ? world.getRandomSpawnPoint(usedPoint) : null;
        if (randomBatSpawn != null) {
            batTileX = randomBatSpawn.x;
            batTileY = randomBatSpawn.y;
        } else if (world != null && world.isSolidForSpawning(batTileX, batTileY)) {
            int playerTX = (int) (saveData.playerX / TILE_SIZE);
            int playerTY = (int) (saveData.playerY / TILE_SIZE);
            int[] safe = findSafeTile(world, batTileX, batTileY, 8, playerTX, playerTY, 0);
            batTileX = safe[0];
            batTileY = safe[1];
        }
        EntityFactory.createBattery(engine, batTileX * TILE_SIZE, batTileY * TILE_SIZE, batteryRegion, BATTERY_SAVE_ID);

        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Retrieves the TileWorldComponent from the engine.
     * Returns null if the map hasn't been loaded yet.
     */
    private static TileWorldComponent getWorldComponent(PooledEngine engine) {
        ImmutableArray<Entity> worldEntities = engine
                .getEntitiesFor(Family.all(TileWorldComponent.class).get());
        if (worldEntities.size() == 0) {
            return null;
        }
        return worldEntities.first().getComponent(TileWorldComponent.class);
    }

    /**
     * Finds a random non-solid tile within a radius of a center point,
     * with a minimum Manhattan distance from the player's tile.
     *
     * Used for randomized item spawns (e.g. flashlight).
     */
    private static int[] findSafeTileRandom(TileWorldComponent world,
            int centerX, int centerY,
            int radius, int minDistFromCenter,
            int mapWidth, int mapHeight) {
        for (int attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
            int offX = (int) (Math.random() * (radius * 2 + 1)) - radius;
            int offY = (int) (Math.random() * (radius * 2 + 1)) - radius;

            int candidateX = centerX + offX;
            int candidateY = centerY + offY;

            // Out of map bounds
            if (candidateX < 0 || candidateX >= mapWidth
                    || candidateY < 0 || candidateY >= mapHeight) {
                continue;
            }
            // Too close to center (player)
            if (Math.abs(offX) + Math.abs(offY) < minDistFromCenter) {
                continue;
            }
            // Inside a wall
            if (world.isSolidForSpawning(candidateX, candidateY)) {
                continue;
            }

            return new int[] { candidateX, candidateY };
        }

        // Fallback: spiral outward from center until a non-solid tile is found
        Gdx.app.log("SPAWNER", "Random search exhausted — falling back to spiral scan");
        return findSafeTile(world, centerX, centerY, Math.max(radius, 20),
                centerX, centerY, minDistFromCenter);
    }

    /**
     * Spiral outward from (originX, originY) searching for the nearest
     * non-solid tile within the given radius. Optionally enforces a
     * minimum Manhattan distance from (avoidX, avoidY).
     *
     * Used as a fallback for hardcoded positions that land inside walls.
     *
     * @return int[] { tileX, tileY } of the safe tile, or the origin if
     *         nothing was found (should not happen on a valid map).
     */
    private static int[] findSafeTile(TileWorldComponent world,
            int originX, int originY, int searchRadius,
            int avoidX, int avoidY, int minAvoidDist) {
        // Try the origin first
        if (!world.isSolidForSpawning(originX, originY)) {
            int dist = Math.abs(originX - avoidX) + Math.abs(originY - avoidY);
            if (dist >= minAvoidDist) {
                return new int[] { originX, originY };
            }
        }

        // Spiral: radius 1 → searchRadius
        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    // Only check the perimeter of each ring
                    if (Math.abs(dx) != r && Math.abs(dy) != r) {
                        continue;
                    }
                    int candidateX = originX + dx;
                    int candidateY = originY + dy;

                    if (world.isSolidForSpawning(candidateX, candidateY)) {
                        continue;
                    }

                    int avoidDist = Math.abs(candidateX - avoidX) + Math.abs(candidateY - avoidY);
                    if (avoidDist < minAvoidDist) {
                        continue;
                    }

                    return new int[] { candidateX, candidateY };
                }
            }
        }

        // Absolute fallback — should never reach here on a valid map
        Gdx.app.log("SPAWNER", "WARNING: No safe tile found within radius " + searchRadius
                + " of (" + originX + ", " + originY + "). Using origin.");
        return new int[] { originX, originY };
    }
}
