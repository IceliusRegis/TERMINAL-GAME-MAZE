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
    // Tiled coordinates (14, 14) on a 50x50 map equal LibGDX coordinates (14, 35)
    private static final float PLAYER_X = 14 * TILE_SIZE;
    private static final float PLAYER_Y = 35 * TILE_SIZE;
    private static final int KEY_TILE_X = 20;
    private static final int KEY_TILE_Y = 10;
    private static final float ENEMY_X = 5 * TILE_SIZE;
    private static final float ENEMY_Y = 40 * TILE_SIZE;

    public static final String KEY_SAVE_ID = "KEY_BEEP_MAIN";
    public static final String FLASHLIGHT_SAVE_ID = "ITEM_FLASHLIGHT";
    public static final String BATTERY_SAVE_ID = "ITEM_BATTERY";
    public static final String POTION_SAVE_ID = "ITEM_POTION";
    public static final String STUD_ID_SAVE_ID = "ITEM_STUD_ID";
    public static final String PAPERS_SAVE_ID = "ITEM_PAPERS";
    public static final String CONFRONTATION_SAVE_ID = "TRIGGER_CONFRONTATION";

    public static int totalBeepCardsSpawned = 0;
    public static final int REQUIRED_BEEP_CARDS = 3;

    /** Maximum random attempts before falling back to spiral scan. */
    private static final int MAX_RANDOM_ATTEMPTS = 100;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Scans from the bottom-left corner of the map outward to find the first
     * non-collision, non-wall tile suitable for player spawning.
     * Uses the same safety checks as Level 1 item/player spawning.
     *
     * @return int[] { tileX, tileY } in LibGDX coordinates (origin bottom-left).
     */
    public static int[] findSafeBottomLeftSpawn(TileWorldComponent world) {
        if (world == null) {
            return new int[] { 2, 2 };
        }
        // Start searching from tile (1, 1) — just inside the map border.
        // Spiral outward using the existing findSafeTile logic with a large radius.
        int searchRadius = Math.max(world.mapWidthTiles, world.mapHeightTiles);
        return findSafeTile(world, 1, 1, searchRadius, -1, -1, 0);
    }

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
            Animation<TextureRegion> enemyAnimation, TextureRegion flashlightRegion,
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

        // Lily to the left side of the player spawn point.
        int targetX = pTileX - 3;
        int targetY = pTileY;
        int[] lilySafe = findSafeTile(world, targetX, targetY, 4, pTileX, pTileY, 1, true);
        EntityFactory.createLily(engine, lilySafe[0] * TILE_SIZE, lilySafe[1] * TILE_SIZE, lilyRegion, "LILY_TRIGGER");
    }

    public static void spawnEnemy(PooledEngine engine, Animation<TextureRegion> enemyAnimation,
            float frameDrawWidth, float frameDrawHeight) {
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyAnimation, frameDrawWidth, frameDrawHeight);
        Gdx.app.log("SPAWNER", "The hunter has entered the maze...");
    }

    /** Minimum Manhattan distance (in tiles) between beep card spawns. */
    private static final int MIN_BEEP_CARD_DISTANCE = 5;

    public static void spawnItems(PooledEngine engine,
            TextureRegion beepRegion,
            TextureRegion flashlightRegion,
            TextureRegion batteryRegion,
            TextureRegion potionRegion,
            TileWorldComponent world,
            int pTileX, int pTileY,
            int mapWidth, int mapHeight) {
        // Default: spawn flashlight (legacy callers)
        spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, potionRegion, world,
                pTileX, pTileY, mapWidth, mapHeight, false);
    }

    public static void spawnItems(PooledEngine engine,
            TextureRegion beepRegion,
            TextureRegion flashlightRegion,
            TextureRegion batteryRegion,
            TextureRegion potionRegion,
            TileWorldComponent world,
            int pTileX, int pTileY,
            int mapWidth, int mapHeight,
            boolean playerHasFlashlight) {

        // Track ALL previously-used spawn points so beep cards stay far apart.
        java.util.List<com.badlogic.gdx.math.GridPoint2> usedPoints = new java.util.ArrayList<>();

        // --- SAFE POTION POSITION ---
        int numPotions = 1;
        for (int i = 0; i < numPotions; i++) {
            int potTileX;
            int potTileY;
            com.badlogic.gdx.math.GridPoint2 randomPotSpawn = (world != null)
                    ? getSpawnPointAvoidingAll(world, usedPoints, 0)
                    : null;
            if (randomPotSpawn != null) {
                potTileX = randomPotSpawn.x;
                potTileY = randomPotSpawn.y;
                usedPoints.add(randomPotSpawn);
            } else {
                int[] safe = findSafeTileRandom(world, pTileX, pTileY, 20, 10, mapWidth, mapHeight);
                potTileX = safe[0];
                potTileY = safe[1];
                usedPoints.add(new com.badlogic.gdx.math.GridPoint2(potTileX, potTileY));
            }
            EntityFactory.createPotion(engine, potTileX * TILE_SIZE, potTileY * TILE_SIZE, potionRegion,
                    POTION_SAVE_ID + "_" + i);
        }

        // --- SAFE BEEP CARDS POSITION (enforced minimum distance) ---
        totalBeepCardsSpawned = REQUIRED_BEEP_CARDS;
        for (int i = 0; i < totalBeepCardsSpawned; i++) {
            com.badlogic.gdx.math.GridPoint2 randomCardSpawn = (world != null)
                    ? getSpawnPointAvoidingAll(world, usedPoints, MIN_BEEP_CARD_DISTANCE)
                    : null;
            int keyTileX = KEY_TILE_X;
            int keyTileY = KEY_TILE_Y;

            if (randomCardSpawn != null) {
                keyTileX = randomCardSpawn.x;
                keyTileY = randomCardSpawn.y;
            } else if (world != null && world.isSolidForSpawning(keyTileX, keyTileY)) {
                int[] safe = findSafeTile(world, keyTileX, keyTileY, 8, pTileX, pTileY, 0);
                keyTileX = safe[0];
                keyTileY = safe[1];
            }
            usedPoints.add(new com.badlogic.gdx.math.GridPoint2(keyTileX, keyTileY));
            EntityFactory.createKey(engine, keyTileX * TILE_SIZE, keyTileY * TILE_SIZE, beepRegion,
                    KEY_SAVE_ID + "_" + i);
        }

        // --- SAFE FLASHLIGHT POSITION ---
        // Only spawn ONE flashlight if the player does NOT already have one.
        if (!playerHasFlashlight && flashlightRegion != null) {
            int flTileX = pTileX + 6;
            int flTileY = pTileY + 6;
            com.badlogic.gdx.math.GridPoint2 randomFlSpawn = (world != null)
                    ? getSpawnPointAvoidingAll(world, usedPoints, 0)
                    : null;
            if (randomFlSpawn != null) {
                flTileX = randomFlSpawn.x;
                flTileY = randomFlSpawn.y;
                usedPoints.add(randomFlSpawn);
            } else if (world != null) {
                int[] flSafe = findSafeTileRandom(world, pTileX, pTileY, 12, 4, mapWidth, mapHeight);
                flTileX = flSafe[0];
                flTileY = flSafe[1];
                usedPoints.add(new com.badlogic.gdx.math.GridPoint2(flTileX, flTileY));
            }
            EntityFactory.createFlashlight(engine, flTileX * TILE_SIZE, flTileY * TILE_SIZE, flashlightRegion,
                FLASHLIGHT_SAVE_ID);
        }

        // --- SAFE BATTERY POSITION ---
        int batTileX = pTileX + 8;
        int batTileY = pTileY + 8;
        com.badlogic.gdx.math.GridPoint2 randomBatSpawn = (world != null)
                ? getSpawnPointAvoidingAll(world, usedPoints, 0)
                : null;
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

    // =========================================================================
    // Spawn-point helper: avoids ALL previously-used points with min distance
    // =========================================================================

    /**
     * Picks a random valid spawn point that is at least {@code minDist}
     * Manhattan tiles away from every point in {@code usedPoints}.
     *
     * @return a safe GridPoint2, or null if no suitable point was found.
     */
    private static com.badlogic.gdx.math.GridPoint2 getSpawnPointAvoidingAll(
            TileWorldComponent world,
            java.util.List<com.badlogic.gdx.math.GridPoint2> usedPoints,
            int minDist) {

        if (world == null || world.validSpawnPoints.isEmpty()) {
            return null;
        }

        int maxAttempts = 80;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int index = com.badlogic.gdx.math.MathUtils.random(world.validSpawnPoints.size() - 1);
            com.badlogic.gdx.math.GridPoint2 candidate = world.validSpawnPoints.get(index);

            boolean tooClose = false;
            for (com.badlogic.gdx.math.GridPoint2 used : usedPoints) {
                if (used == null) {
                    continue;
                }
                int manhattan = Math.abs(candidate.x - used.x) + Math.abs(candidate.y - used.y);
                if (manhattan < minDist) {
                    tooClose = true;
                    break;
                }
                // Also reject exact overlaps regardless of minDist
                if (candidate.x == used.x && candidate.y == used.y) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                return candidate;
            }
        }
        // Fallback: return any point that at least doesn't overlap exactly
        for (com.badlogic.gdx.math.GridPoint2 candidate : world.validSpawnPoints) {
            boolean overlaps = false;
            for (com.badlogic.gdx.math.GridPoint2 used : usedPoints) {
                if (used != null && candidate.x == used.x && candidate.y == used.y) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                return candidate;
            }
        }
        return null;
    }

    // =========================================================================
    // Level 2 Ending Key Items
    // =========================================================================

    /**
     * Spawns Level 2 specific key items: studID and the invisible confrontation trigger.
     * Called from Main.loadLevelTwo().
     */
    public static void spawnLevel2KeyItems(PooledEngine engine, com.badlogic.gdx.graphics.g2d.TextureRegion studIDRegion) {
        TileWorldComponent world = getWorldComponent(engine);
        int mapWidth = (world != null) ? world.mapWidthTiles : 50;
        int mapHeight = (world != null) ? world.mapHeightTiles : 50;

        // StudID — randomized spawn in Level 2
        int studTileX = 25;
        int studTileY = 25;
        com.badlogic.gdx.math.GridPoint2 studSpawn = (world != null) ? world.getRandomSpawnPoint() : null;
        if (studSpawn != null) {
            studTileX = studSpawn.x;
            studTileY = studSpawn.y;
        } else if (world != null) {
            int[] safe = findSafeTileRandom(world, 25, 25, 15, 5, mapWidth, mapHeight);
            studTileX = safe[0];
            studTileY = safe[1];
        }
        EntityFactory.createStudID(engine, studTileX * TILE_SIZE, studTileY * TILE_SIZE, studIDRegion, STUD_ID_SAVE_ID);

        // Confrontation trigger — invisible spot at a fixed location
        int confTileX = 20;
        int confTileY = 30;
        if (world != null) {
            int[] confSafe = findSafeTile(world, confTileX, confTileY, 8, 0, 0, 0);
            confTileX = confSafe[0];
            confTileY = confSafe[1];
        }
        EntityFactory.createConfrontationSpot(engine, confTileX * TILE_SIZE, confTileY * TILE_SIZE, CONFRONTATION_SAVE_ID);
    }

    /**
     * Spawns the papers item in Level 1 (called from spawnPostLilyEntities in Main).
     * Papers only appear after the lily has been triggered.
     */
    public static void spawnPapers(PooledEngine engine, com.badlogic.gdx.graphics.g2d.TextureRegion papersRegion,
            TileWorldComponent world, int pTileX, int pTileY) {
        int mapWidth = (world != null) ? world.mapWidthTiles : 50;
        int mapHeight = (world != null) ? world.mapHeightTiles : 50;

        int papersTileX = pTileX + 10;
        int papersTileY = pTileY + 10;
        com.badlogic.gdx.math.GridPoint2 papersSpawn = (world != null) ? world.getRandomSpawnPoint() : null;
        if (papersSpawn != null) {
            papersTileX = papersSpawn.x;
            papersTileY = papersSpawn.y;
        } else if (world != null) {
            int[] safe = findSafeTileRandom(world, pTileX, pTileY, 20, 8, mapWidth, mapHeight);
            papersTileX = safe[0];
            papersTileY = safe[1];
        }
        EntityFactory.createPapers(engine, papersTileX * TILE_SIZE, papersTileY * TILE_SIZE, papersRegion, PAPERS_SAVE_ID);
    }

    // Rest of class remains unchanged (spawnForLoad, helper methods)...
    public static void spawnForLoad(PooledEngine engine, GameData saveData,
            TextureRegion beepRegion,
            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
            Animation<TextureRegion> enemyAnimation, TextureRegion flashlightRegion,
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
        return findSafeTileRandom(world, centerX, centerY, radius, minDistFromCenter, mapWidth, mapHeight, false);
    }

    private static int[] findSafeTileRandom(TileWorldComponent world,
            int centerX, int centerY,
            int radius, int minDistFromCenter,
            int mapWidth, int mapHeight,
            boolean ignoreNoSpawn) {
        for (int attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
            int offX = (int) (Math.random() * (radius * 2 + 1)) - radius;
            int offY = (int) (Math.random() * (radius * 2 + 1)) - radius;
            int cX = centerX + offX;
            int cY = centerY + offY;
            if (cX < 0 || cX >= mapWidth || cY < 0 || cY >= mapHeight)
                continue;
            if (Math.abs(offX) + Math.abs(offY) < minDistFromCenter)
                continue;
            if (world.isSolidForSpawning(cX, cY, ignoreNoSpawn))
                continue;
            return new int[] { cX, cY };
        }
        return findSafeTile(world, centerX, centerY, Math.max(radius, 20), centerX, centerY, minDistFromCenter, ignoreNoSpawn);
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
        return findSafeTile(world, originX, originY, searchRadius, avoidX, avoidY, minAvoidDist, false);
    }

    private static int[] findSafeTile(TileWorldComponent world,
            int originX, int originY, int searchRadius,
            int avoidX, int avoidY, int minAvoidDist,
            boolean ignoreNoSpawn) {
        // Try the origin first
        if (!world.isSolidForSpawning(originX, originY, ignoreNoSpawn)) {
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
                    if (world.isSolidForSpawning(cX, cY, ignoreNoSpawn))
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
