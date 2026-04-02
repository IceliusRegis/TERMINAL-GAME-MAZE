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

    // Default hardcoded tile positions
    private static final float PLAYER_X = 5 * TILE_SIZE;
    private static final float PLAYER_Y = 5 * TILE_SIZE;
    private static final int KEY_TILE_X = 20;
    private static final int KEY_TILE_Y = 10;
    private static final float ENEMY_X = 5 * TILE_SIZE;
    private static final float ENEMY_Y = 40 * TILE_SIZE;

    public static final String KEY_SAVE_ID  = "KEY_BEEP_MAIN";
    public static final String FLASHLIGHT_SAVE_ID = "ITEM_FLASHLIGHT";
    public static final String BATTERY_SAVE_ID = "ITEM_BATTERY";
    public static final String POTION_SAVE_ID = "ITEM_POTION";

    public static int totalBeepCardsSpawned = 0;

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
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);

        spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, potionRegion, world, pTileX, pTileY, mapWidth, mapHeight);
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
            com.badlogic.gdx.math.GridPoint2 randomPotSpawn = (world != null) ? world.getRandomSpawnPoint(usedPoint) : null;
            if (randomPotSpawn != null) {
                potTileX = randomPotSpawn.x;
                potTileY = randomPotSpawn.y;
                usedPoint = randomPotSpawn;
            } else {
                int[] safe = findSafeTileRandom(world, pTileX, pTileY, 20, 10, mapWidth, mapHeight);
                potTileX = safe[0];
                potTileY = safe[1];
            }
            EntityFactory.createPotion(engine, potTileX * TILE_SIZE, potTileY * TILE_SIZE, potionRegion, POTION_SAVE_ID + "_" + i);
        }

        // --- SAFE BEEP CARDS POSITION ---
        totalBeepCardsSpawned = 3 + (int)(Math.random() * 3);
        for (int i = 0; i < totalBeepCardsSpawned; i++) {
            com.badlogic.gdx.math.GridPoint2 randomCardSpawn = world != null ? world.getRandomSpawnPoint(usedPoint) : null;
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
            EntityFactory.createKey(engine, keyTileX * TILE_SIZE, keyTileY * TILE_SIZE, beepRegion, KEY_SAVE_ID + "_" + i);
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
        EntityFactory.createFlashlight(engine, flTileX * TILE_SIZE, flTileY * TILE_SIZE, flashlightRegion, FLASHLIGHT_SAVE_ID);

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
    public static void spawnForLoad(PooledEngine engine, GameData saveData,
                                    TextureRegion beepRegion,
                                    Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
                                    TextureRegion enemyRegion, TextureRegion flashlightRegion,
                                    TextureRegion batteryRegion, TextureRegion potionRegion) {

        TileWorldComponent world = getWorldComponent(engine);
        com.badlogic.gdx.math.GridPoint2 usedPoint = null;

        EntityFactory.createPlayer(engine, saveData.playerX, saveData.playerY, 24f, 15f, walkAnimation, idleAnimation);
        totalBeepCardsSpawned = saveData.totalBeepCardsSpawned > 0 ? saveData.totalBeepCardsSpawned : 3;

        for (int i = 0; i < 1; i++) {
            com.badlogic.gdx.math.GridPoint2 randomPotSpawn = (world != null) ? world.getRandomSpawnPoint(usedPoint) : null;
            int potTileX = (randomPotSpawn != null) ? randomPotSpawn.x : 10;
            int potTileY = (randomPotSpawn != null) ? randomPotSpawn.y : 10;
            usedPoint = randomPotSpawn;
            EntityFactory.createPotion(engine, potTileX * TILE_SIZE, potTileY * TILE_SIZE, potionRegion, POTION_SAVE_ID + "_" + i);
        }

        for (int i = 0; i < totalBeepCardsSpawned; i++) {
            com.badlogic.gdx.math.GridPoint2 randomCardSpawn = world != null ? world.getRandomSpawnPoint(usedPoint) : null;
            int kX = (randomCardSpawn != null) ? randomCardSpawn.x : KEY_TILE_X;
            int kY = (randomCardSpawn != null) ? randomCardSpawn.y : KEY_TILE_Y;
            usedPoint = randomCardSpawn;
            EntityFactory.createKey(engine, kX * TILE_SIZE, kY * TILE_SIZE, beepRegion, KEY_SAVE_ID + "_" + i);
        }

        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);
    }

    private static TileWorldComponent getWorldComponent(PooledEngine engine) {
        ImmutableArray<Entity> worldEntities = engine.getEntitiesFor(Family.all(TileWorldComponent.class).get());
        return (worldEntities.size() == 0) ? null : worldEntities.first().getComponent(TileWorldComponent.class);
    }

    private static int[] findSafeTileRandom(TileWorldComponent world, int centerX, int centerY, int radius, int minDist, int mapW, int mapH) {
        for (int i = 0; i < MAX_RANDOM_ATTEMPTS; i++) {
            int offX = (int) (Math.random() * (radius * 2 + 1)) - radius;
            int offY = (int) (Math.random() * (radius * 2 + 1)) - radius;
            int cX = centerX + offX; int cY = centerY + offY;
            if (cX < 0 || cX >= mapW || cY < 0 || cY >= mapH) continue;
            if (Math.abs(offX) + Math.abs(offY) < minDist) continue;
            if (world.isSolidForSpawning(cX, cY)) continue;
            return new int[] { cX, cY };
        }
        return findSafeTile(world, centerX, centerY, Math.max(radius, 20), centerX, centerY, minDist);
    }

    private static int[] findSafeTile(TileWorldComponent world, int originX, int originY, int searchRadius, int avoidX, int avoidY, int minAvoidDist) {
        if (!world.isSolidForSpawning(originX, originY)) {
            if (Math.abs(originX - avoidX) + Math.abs(originY - avoidY) >= minAvoidDist) return new int[] { originX, originY };
        }
        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int cX = originX + dx; int cY = originY + dy;
                    if (world.isSolidForSpawning(cX, cY)) continue;
                    if (Math.abs(cX - avoidX) + Math.abs(cY - avoidY) < minAvoidDist) continue;
                    return new int[] { cX, cY };
                }
            }
        }
        return new int[] { originX, originY };
    }
}
