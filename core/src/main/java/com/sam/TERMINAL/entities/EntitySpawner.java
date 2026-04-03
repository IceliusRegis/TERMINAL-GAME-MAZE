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
 */
public class EntitySpawner {

    private static final float TILE_SIZE = 32f;

    // Default hardcoded tile positions (calculated from LibGDX bottom-left origin)
    // Tiled coordinates (15, 7) on a 50x50 map equal LibGDX coordinates (15, 42)
    private static final float PLAYER_X = 15 * TILE_SIZE;
    private static final float PLAYER_Y = 42 * TILE_SIZE;
    // Default hardcoded tile positions (calculated from LibGDX bottom-left origin)
    // Tiled coordinates (15, 7) on a 50x50 map equal LibGDX coordinates (15, 42)
    private static final float PLAYER_X = 15 * TILE_SIZE;
    private static final float PLAYER_Y = 42 * TILE_SIZE;
    private static final int KEY_TILE_X = 20;
    private static final int KEY_TILE_Y = 10;
    private static final float ENEMY_X = 5 * TILE_SIZE;
    private static final float ENEMY_Y = 40 * TILE_SIZE;

    public static final String KEY_SAVE_ID = "KEY_BEEP_MAIN";
    public static final String KEY_SAVE_ID = "KEY_BEEP_MAIN";
    public static final String FLASHLIGHT_SAVE_ID = "ITEM_FLASHLIGHT";
    public static final String BATTERY_SAVE_ID = "ITEM_BATTERY";
    public static final String POTION_SAVE_ID = "ITEM_POTION";

    public static int totalBeepCardsSpawned = 0;
    public static final int REQUIRED_BEEP_CARDS = 3;

    /** Maximum random attempts before falling back to spiral scan. */
    private static final int MAX_RANDOM_ATTEMPTS = 100;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * THIS IS THE METHOD CALLED BY MovementSystem AFTER 15 SECONDS.
     * It spawns a single battery at the specified world coordinates.
     */
    public static void spawnBattery(PooledEngine engine, float x, float y) {
        // We use your existing Factory to ensure the battery has the right components
        // Note: You may need to pass a TextureRegion here if your Factory requires it.
        // If your Factory doesn't take a region, remove the null parameter.
        EntityFactory.createBattery(engine, x, y, null, BATTERY_SAVE_ID + "_RESPAWN");
        Gdx.app.log("SPAWNER", "Dynamic battery spawned at: " + x + ", " + y);
    }

    public static void spawnInitialEntities(PooledEngine engine,
            TextureRegion beepRegion,
            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
            TextureRegion enemyRegion, TextureRegion flashlightRegion,
            TextureRegion batteryRegion, TextureRegion potionRegion) {

        TileWorldComponent world = getWorldComponent(engine);

        int mapWidth = (world != null) ? world.mapWidthTiles : 50;
        int mapHeight = (world != null) ? world.mapHeightTiles : 50;

        int pTileX = (int) (PLAYER_X / TILE_SIZE);
        int pTileY = (int) (PLAYER_Y / TILE_SIZE);

        EntityFactory.createPlayer(engine, PLAYER_X, PLAYER_Y, 24f, 15f, walkAnimation, idleAnimation);

        spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, potionRegion, world, pTileX, pTileY, mapWidth,
                mapHeight);
    }

    /** Tutorial start: spawn only player + lily trigger (no items/enemy). */
    public static void spawnTutorialStart(PooledEngine engine,
            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
            TextureRegion lilyRegion) {
        TileWorldComponent world = getWorldComponent(engine);
        int mapWidth = (world != null) ? world.mapWidthTiles : 50;
        int mapHeight = (world != null) ? world.mapHeightTiles : 50;

        int pTileX = (int) (PLAYER_X / TILE_SIZE);
        int pTileY = (int) (PLAYER_Y / TILE_SIZE);

        EntityFactory.createPlayer(engine, PLAYER_X, PLAYER_Y, 24f, 15f, walkAnimation, idleAnimation);

        // Lily near the player.
        int[] lilySafe = findSafeTileRandom(world, pTileX, pTileY, 5, 3, mapWidth, mapHeight);
        EntityFactory.createLily(engine, lilySafe[0] * TILE_SIZE, lilySafe[1] * TILE_SIZE, lilyRegion, "LILY_TRIGGER");
    }

    public static void spawnEnemy(PooledEngine engine, TextureRegion enemyRegion) {
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);
        Gdx.app.log("SPAWNER", "The hunter has entered the maze...");
    }

    public static void spawnItems(PooledEngine engine,
            TextureRegion beepRegion,
            TextureRegion flashlightRegion,
            TextureRegion batteryRegion,
            TextureRegion potionRegion,
            TileWorldComponent world,
            int pTileX, int pTileY,
            int mapWidth, int mapHeight) {

        com.badlogic.gdx.math.GridPoint2 usedPoint = null;

        // --- SAFE POTION POSITION ---
        int numPotions = 1;
        for (int i = 0; i < numPotions; i++) {
            int potTileX;
            int potTileY;
            com.badlogic.gdx.math.GridPoint2 randomPotSpawn = (world != null) ? world.getRandomSpawnPoint(usedPoint)
                    : null;
            if (randomPotSpawn != null) {
                potTileX = randomPotSpawn.x;
                potTileY = randomPotSpawn.y;
                usedPoint = randomPotSpawn;
            } else {
                int[] safe = findSafeTileRandom(world, pTileX, pTileY, 20, 10, mapWidth, mapHeight);
                potTileX = safe[0];
                potTileY = safe[1];
            }
            EntityFactory.createPotion(engine, potTileX * TILE_SIZE, potTileY * TILE_SIZE, potionRegion,
                    POTION_SAVE_ID + "_" + i);
        }

        // --- SAFE BEEP CARDS POSITION ---
        totalBeepCardsSpawned = REQUIRED_BEEP_CARDS;
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
        } else if (world != null) {
            int[] flSafe = findSafeTileRandom(world, pTileX, pTileY, 12, 4, mapWidth, mapHeight);
            flTileX = flSafe[0];
            flTileY = flSafe[1];
        }
        EntityFactory.createFlashlight(engine, flTileX * TILE_SIZE, flTileY * TILE_SIZE, flashlightRegion,
                FLASHLIGHT_SAVE_ID);

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
        EntityFactory.createBattery(engine, batTileX * TILE_SIZE, batTileY * TILE_SIZE, batteryRegion, BATTERY_SAVE_ID);
    }

    // Rest of class remains unchanged (spawnForLoad, helper methods)...
    // Rest of class remains unchanged (spawnForLoad, helper methods)...
    public static void spawnForLoad(PooledEngine engine, GameData saveData,
            TextureRegion beepRegion,
            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
            TextureRegion enemyRegion, TextureRegion flashlightRegion,
            TextureRegion batteryRegion, TextureRegion potionRegion) {

        TileWorldComponent world = getWorldComponent(engine);
        com.badlogic.gdx.math.GridPoint2 usedPoint = null;

        EntityFactory.createPlayer(engine, saveData.playerX, saveData.playerY, 24f, 15f, walkAnimation, idleAnimation);
        totalBeepCardsSpawned = REQUIRED_BEEP_CARDS;

        for (int i = 0; i < 1; i++) {
            com.badlogic.gdx.math.GridPoint2 randomPotSpawn = (world != null) ? world.getRandomSpawnPoint(usedPoint)
                    : null;
            int potTileX = (randomPotSpawn != null) ? randomPotSpawn.x : 10;
            int potTileY = (randomPotSpawn != null) ? randomPotSpawn.y : 10;
            usedPoint = randomPotSpawn;
            EntityFactory.createPotion(engine, potTileX * TILE_SIZE, potTileY * TILE_SIZE, potionRegion,
                    POTION_SAVE_ID + "_" + i);
        }

        for (int i = 0; i < totalBeepCardsSpawned; i++) {
            com.badlogic.gdx.math.GridPoint2 randomCardSpawn = world != null ? world.getRandomSpawnPoint(usedPoint)
                    : null;
            int kX = (randomCardSpawn != null) ? randomCardSpawn.x : KEY_TILE_X;
            int kY = (randomCardSpawn != null) ? randomCardSpawn.y : KEY_TILE_Y;
            usedPoint = randomCardSpawn;
            EntityFactory.createKey(engine, kX * TILE_SIZE, kY * TILE_SIZE, beepRegion,
                    KEY_SAVE_ID + "_" + i);
        }
    }

    private static TileWorldComponent getWorldComponent(PooledEngine engine) {
        ImmutableArray<Entity> worldEntities = engine.getEntitiesFor(Family.all(TileWorldComponent.class).get());
        return (worldEntities.size() == 0) ? null : worldEntities.first().getComponent(TileWorldComponent.class);
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
            int cX = centerX + offX;
            int cY = centerY + offY;
            if (cX < 0 || cX >= mapWidth || cY < 0 || cY >= mapHeight)
                continue;
            if (Math.abs(offX) + Math.abs(offY) < minDistFromCenter)
                continue;
            if (world.isSolidForSpawning(cX, cY))
                continue;
            return new int[] { cX, cY };
        }
        return findSafeTile(world, centerX, centerY, Math.max(radius, 20), centerX, centerY, minDistFromCenter);
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
            if (Math.abs(originX - avoidX) + Math.abs(originY - avoidY) >= minAvoidDist)
                return new int[] { originX, originY };
        }
        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r)
                        continue;
                    int cX = originX + dx;
                    int cY = originY + dy;
                    if (world.isSolidForSpawning(cX, cY))
                        continue;
                    if (Math.abs(cX - avoidX) + Math.abs(cY - avoidY) < minAvoidDist)
                        continue;
                    return new int[] { cX, cY };
                }
            }
        }
        return new int[] { originX, originY };
    }
}
