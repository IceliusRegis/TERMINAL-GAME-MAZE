package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.persistence.GameData;

/**
 * EntitySpawner - Helper class to calculate spawn positions and create entities.
 *
 * Responsibilities:
 * - Encapsulates the algorithms for finding valid "Safe Spots" on the map.
 * - Delegates the actual object construction to EntityFactory.
 * - Ensures entities (like the Key) spawn at a fun distance from the player.
 */

public class EntitySpawner {

    // Helper coordinates (So we don't typo them)
    private static final float TILE_SIZE = 32f;
    private static final float PLAYER_X = 5 * TILE_SIZE;
    private static final float PLAYER_Y = 5 * TILE_SIZE;
    private static final float KEY_X = 20 * TILE_SIZE;
    private static final float KEY_Y = 10 * TILE_SIZE;
    private static final float DOOR_X = 40 * TILE_SIZE;
    private static final float DOOR_Y = 40 * TILE_SIZE;
    private static final float ENEMY_X = 5 * TILE_SIZE;
    private static final float ENEMY_Y = 40 * TILE_SIZE;

    public static void spawnInitialEntities (PooledEngine engine, TileWorldComponent tileCom,
                                             TextureRegion beepRegion, TextureRegion doorRegion,
                                             Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation, TextureRegion enemyRegion) {

       /* Disable complex spawn for noww


        //PLAYER SPAWN MECHANICS


        // 1.) Find Safe Spot for Player
        int spawnTileX = 0;
        int spawnTileY = 0;
        boolean safeSpotFound = false;

        //Keep Finding till we found a floor tile
        while (!safeSpotFound) {
            spawnTileX = (int) (Math.random() * 50);
            spawnTileY = (int) (Math.random() * 50);

            //Checks if the tile found is a floor
            if (tileCom.map[spawnTileX][spawnTileY] == 2) {
                safeSpotFound = true;
            }
        }

        //Convert found coordinates into Pixels
        //(Example: Tile 5 * 32 pixels = Pixel 160)
        float startPixelX = spawnTileX * 32f;
        float startPixelY = spawnTileY * 32f;


        //BEEP CARD SPAWNING MECHANICS

        //Temporary Beep Spawning mechanics
        int beepSpawnX = 0;
        int beepSpawnY = 0;
        boolean beepSafeSpawnFound = false;

        int minDistance = 5;
        int maxRadius = 15;

        while (!beepSafeSpawnFound) {
            // Picks a range between 5-15
            int offSetX = (int) (Math.random() * (maxRadius * 2 + 1)) - maxRadius;
            int offSetY = (int) (Math.random() * (maxRadius * 2 + 1)) - maxRadius;

            int beepCandidateX = spawnTileX + offSetX;
            int beepCandidateY = spawnTileY + offSetY;

            if (beepCandidateX < 0 || beepCandidateX >=50 || beepCandidateY < 0 || beepCandidateY >=50) {
                //Do not spawn their since it is out of bounds
                continue;
            }

            if (tileCom.map[beepCandidateX][beepCandidateY] != 2) {
                //Dont spawn also here cause walls
                continue;
            }

            if (Math.abs(offSetX) + Math.abs(offSetY) < minDistance) {
                //Avoid Spawning too close to the player
                continue;
            }

            //If all checks passed
            beepSpawnX = beepCandidateX;
            beepSpawnY = beepCandidateY;
            beepSafeSpawnFound = true;

        }

        //DOOR SPAWNING
        int doorSpawnX = 0;
        int doorSpawnY = 0;
        boolean doorSafeSpawnFound = false;

        int minDoorDistance = 10;
        int maxDoorRadius = 24;

        while (!doorSafeSpawnFound) {
            int offSetX = (int) (Math.random() * (maxDoorRadius * 2 + 1)) - maxDoorRadius;
            int offSetY = (int) (Math.random() * (maxDoorRadius * 2 + 1)) - maxDoorRadius;

            int doorCandidateX = spawnTileX + offSetX;
            int doorCandidateY = spawnTileY + offSetY;

            if (doorCandidateX < 0 || doorCandidateX >=50 || doorCandidateY < 0 || doorCandidateY >=50) {
                //Do not spawn their since it is out of bounds
                continue;
            }

            if (tileCom.map[doorCandidateX][doorCandidateY] != 2) {
                //Dont spawn also here cause walls
                continue;
            }

            if (Math.abs(offSetX) + Math.abs(offSetY) < minDoorDistance) {
                //Avoid Spawning too close to the player
                continue;
            }

            //If all checks passed
            doorSpawnX = doorCandidateX;
            doorSpawnY = doorCandidateY;
            doorSafeSpawnFound = true;

        }

        **/

        //CREATE THE ENTITIES THROUGH ENTITY FACTORY

        //Create Beep
        EntityFactory.createKey(engine, KEY_X, KEY_Y, beepRegion);

        //Create Door
        EntityFactory.createDoor(engine, DOOR_X, DOOR_Y, doorRegion);

        //Create Player
        EntityFactory.createPlayer(engine, PLAYER_X, PLAYER_Y, 24f, 15f, walkAnimation, idleAnimation);

        //Create Enemy
        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);

        Gdx.app.log("TERMINAL", "Spawned Player at (" + PLAYER_X + "," + PLAYER_Y + ")");
        Gdx.app.log("TERMINAL", "Spawned Key nearby at (" + KEY_X + "," + KEY_Y + ")");
        Gdx.app.log("TERMINAL", "Spawned Door at (" + DOOR_X + "," + DOOR_Y + ")");
        Gdx.app.log("TERMINAL", "Spawned Enemy at (" + ENEMY_X + "," + ENEMY_Y + ")");

    }

    public static void spawnForLoad(PooledEngine engine, GameData saveData,
                                    TextureRegion beepRegion, TextureRegion doorRegion,
                                    Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation, TextureRegion enemyRegion) {
        EntityFactory.createPlayer(engine, saveData.playerX, saveData.playerY, 24f, 15f, walkAnimation, idleAnimation);

        EntityFactory.createKey(engine, KEY_X, KEY_Y, beepRegion);
        EntityFactory.createDoor(engine, DOOR_X, DOOR_Y, doorRegion);

        EntityFactory.createEnemy(engine, ENEMY_X, ENEMY_Y, enemyRegion);

        Gdx.app.log("SPAWNER", "Restored entities for Save Load.");
    }



}
