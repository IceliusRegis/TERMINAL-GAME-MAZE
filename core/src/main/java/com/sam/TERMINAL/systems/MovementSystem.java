package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.components.*;

/**
 * MovementSystem - Handles player input, physics, and collision.
 *
 * Responsibilities:
 * 1. Reads WASD Input.
 * 2. Moves the entity on X/Y axes independently.
 * 3. Checks for collisions against Walls or Map Borders and reverts movement if hit.
 */

public class MovementSystem extends IteratingSystem {

    // ComponentMapper provides fast access to components
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    /** Movement speed in pixels per second */
    private static final float PLAYER_SPEED = 200f;
    private com.sam.TERMINAL.buttons.MenuScreen menuScreen;

    public MovementSystem() {
        // Only process entities with Transform AND Player components
        super(Family.all(TransformComponent.class, PlayerComponent.class).get());
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (menuScreen != null && (menuScreen.isSettingsVisible() || menuScreen.isInventoryVisible())) {
            return;
        }

        TransformComponent transform = transformMapper.get(entity);
        // Need this to update the flashlight direction
        SpriteComponent sprite = spriteMapper.get(entity);

        // Calculate how much we want to move
        float xInput = 0;
        float yInput = 0;

        // Handle WASD input
        if (Gdx.input.isKeyPressed(Input.Keys.W)) yInput += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) yInput -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) xInput -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) xInput += 1;

        // --- NEW: 8-DIRECTIONAL ANGLE & NORMALIZATION ---
        if (xInput != 0 || yInput != 0) {
            // Calculate diagonal angle (e.g., W+D = 45 degrees)
            float angle = (float) Math.toDegrees(Math.atan2(yInput, xInput));
            if (angle < 0) angle += 360;
            sprite.facingAngle = angle;

            // Normalizing speed so diagonals aren't faster (Speed * 0.707)
            if (xInput != 0 && yInput != 0) {
                float length = (float) Math.sqrt(xInput * xInput + yInput * yInput);
                xInput /= length;
                yInput /= length;
            }
        }

        float xMove = xInput * PLAYER_SPEED * deltaTime;
        float yMove = yInput * PLAYER_SPEED * deltaTime;
        // -----------------------------------------------

        // Get Map Data
        TileWorldComponent world = null;
        if (getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).size() > 0) {
            Entity worldEntity = getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).first();
            world = worldEntity.getComponent(TileWorldComponent.class);
        }

        // == X-Axis ==
        float oldX = transform.pos.x;
        transform.pos.x += xMove;
        transform.updateBounds();

        if(checkEntityCollison(entity, transform) || checkTileCollision(transform, world)) {
            transform.pos.x = oldX;
            transform.updateBounds();
        }

        // == Y-Axis ==
        float oldY = transform.pos.y;
        transform.pos.y += yMove;
        transform.updateBounds();

        if(checkEntityCollison(entity, transform) || checkTileCollision(transform, world)) {
            transform.pos.y = oldY;
            transform.updateBounds();
        }
    }

    private boolean checkEntityCollison(Entity player, TransformComponent playerTransfrom) {
        for (Entity wall : getEngine().getEntitiesFor(Family.all(CollisionComponent.class).get())) {
            if (wall == player) continue;
            TransformComponent wallTransform = transformMapper.get(wall);
            if (playerTransfrom.bounds.overlaps(wallTransform.bounds)) {
                return true;
            }
        }
        return  false;
    }

        private boolean checkTileCollision (TransformComponent transform, TileWorldComponent world) {
            if (world == null) return false;

            //World Border Check

            // A. Calculate Maps length in Pixels
            float mapPixelWidth = world.mapWidthTiles* world.tileWidth;
            float mapPixelHeight = world.mapHeightTiles * world.tileHeight;

            // B. Checks if near the Left & Bottom Coordinates border
            if (transform.bounds.x < 0) return true;
            if (transform.bounds.y < 0) return true;

            // C. Checks is player is near at the Right and Top Border Coords
            if (transform.bounds.x + transform.bounds.width > mapPixelWidth) return true;
            if (transform.bounds.y + transform.bounds.height > mapPixelHeight) return true;


            // 1.) Get the 4 corners of the player's hitbox in Grid Coordinates (Pixels to Coordinates)
            int startX = (int) (transform.bounds.x / world.tileWidth);
            int endX = (int) ((transform.bounds.x + transform.bounds.width) / world.tileWidth);
            int startY = (int) (transform.bounds.y / world.tileHeight);
            int endY = (int) ((transform.bounds.y + transform.bounds.height) / world.tileHeight);

            //2.) Loop through every tile the player is touching
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    //3.) Checks the Collison Layer
                    if (world.isSolid(x, y)) {
                        return true;
                    }
                }
            }
            return false;
        }

    public void setMenuScreen(com.sam.TERMINAL.buttons.MenuScreen menuScreen) {
        this.menuScreen = menuScreen;
    }
    }

