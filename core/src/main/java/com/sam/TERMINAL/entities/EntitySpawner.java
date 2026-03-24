package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.persistence.GameData;

/**
 * EntitySpawner - Helper class to calculate spawn positions and create entities.
 */
public class EntitySpawner {

    private static final float TILE_SIZE = 32f;

    // Default hardcoded positions
    private static final float PLAYER_X = 5 * TILE_SIZE;
    private static final float PLAYER_Y = 5 * TILE_SIZE;
    private static final float KEY_X = 20 * TILE_SIZE;
    private static final float KEY_Y = 10 * TILE_SIZE;
    private static final float DOOR_X = 40 * TILE_SIZE;
    private static final float DOOR_Y = 40 * TILE_SIZE;
    private static final float ENEMY_X = 5 * TILE_SIZE;
    private static final float ENEMY_Y = 40 * TILE_SIZE;

    public static final String KEY_SAVE_ID  = "KEY_BEEP_MAIN";
    public static final String DOOR_SAVE_ID = "DOOR_EXIT_MAIN";
    public static final String FLASHLIGHT_SAVE_ID = "ITEM_FLASHLIGHT";

    public static void spawnInitialEntities(PooledEngine engine,
                                            TextureRegion beepRegion, TextureRegion doorRegion,
                                            Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
                                            TextureRegion enemyRegion, TextureRegion flashlightRegion) {

        int mapWidth = 50;
        int mapHeight = 50;

        float pStartX = PLAYER_X;
        float pStartY = PLAYER_Y;
        int pTileX = (int)(pStartX / TILE_SIZE);
        int pTileY = (int)(pStartY / TILE_SIZE);

        // --- RANDOM FLASHLIGHT SPAWNING ---
        int flSpawnX = 0;
        int flSpawnY = 0;
        boolean flSafeFound = false;
        int minFlDist = 4;
        int maxFlRadius = 12;

        while (!flSafeFound) {
            int offSetX = (int) (Math.random() * (maxFlRadius * 2 + 1)) - maxFlRadius;
            int offSetY = (int) (Math.random() * (maxFlRadius * 2 + 1)) - maxFlRadius;

            int candidateX = pTileX + offSetX;
            int candidateY = pTileY + offSetY;

            if (candidateX < 0 || candidateX >= mapWidth || candidateY < 0 || candidateY >= mapHeight) continue;
            if (Math.abs(offSetX) + Math.abs(offSetY) < minFlDist) continue;

            flSpawnX = candidateX;
            flSpawnY = candidateY;
            flSafeFound = true;
        }

        float flPixelX = flSpawnX * TILE_SIZE;
        float flPixelY = flSpawnY * TILE_SIZE;

        // --- ACTUAL CREATION ---
        EntityFactory.createPlayer(engine, pStartX, pStartY, 24f, 15f, walkAnimation, idleAnimation);
        EntityFactory.createKey(engine, KEY_X, KEY_Y, beepRegion, KEY_SAVE_ID);

        // Using the specific flashlight factory method
        EntityFactory.createFlashlight(engine, flPixelX, flPixelY, flashlightRegion, FLASHLIGHT_SAVE_ID);

        EntityFactory.createDoor(engine, DOOR_X, DOOR_Y, doorRegion, DOOR_SAVE_ID);
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);
    }

    /**
     * Updated spawnForLoad to accept flashlightRegion
     */
    public static void spawnForLoad(PooledEngine engine, GameData saveData,
                                    TextureRegion beepRegion, TextureRegion doorRegion,
                                    Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation,
                                    TextureRegion enemyRegion, TextureRegion flashlightRegion) {

        // Restore Player position from save
        EntityFactory.createPlayer(engine, saveData.playerX, saveData.playerY, 24f, 15f, walkAnimation, idleAnimation);

        // Re-create items (SaveSystem will usually move/remove these if they were already picked up)
        EntityFactory.createKey(engine, KEY_X, KEY_Y, beepRegion, KEY_SAVE_ID);
        EntityFactory.createFlashlight(engine, KEY_X + 32, KEY_Y, flashlightRegion, FLASHLIGHT_SAVE_ID);

        EntityFactory.createDoor(engine, DOOR_X, DOOR_Y, doorRegion, DOOR_SAVE_ID);
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);
    }
}
