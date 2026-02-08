package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.components.CollisionComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.components.TransformComponent;
import com.sam.TERMINAL.tiles.Tile;
import com.sam.TERMINAL.tiles.TileRegistry;

/**
 * MovementSystem - Handles player input, physics, and collision.
 *
 * Responsibilities:
 * 1. Reads WASD Input.
 * 2. Moves the entity on the X-Axis -> Checks Collision -> Reverts if hit.
 * 3. Moves the entity on the Y-Axis -> Checks Collision -> Reverts if hit.
 * (This separation allows "Wall Sliding" without getting stuck).
 * 4. Checks against both other Entities (CollisionComponent) AND the Map (TileWorldComponent).
 */
public class MovementSystem extends IteratingSystem {

    // ComponentMapper provides fast access to components
    private ComponentMapper<TransformComponent> transformMapper;

    /** Movement speed in pixels per second */
    private static final float PLAYER_SPEED = 200f;

    public MovementSystem() {
        // Only process entities with Transform AND Player components
        super(Family.all(TransformComponent.class, PlayerComponent.class).get());
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);

        //Calculate how much we want to move
        float xMove = 0;
        float yMove = 0;
        // Calculate movement based on deltaTime for smooth, framerate-independent motion
        float speed = PLAYER_SPEED * deltaTime;

        // Handle WASD input
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            yMove += speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            yMove -= speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            xMove -= speed;
            SpriteComponent.facingRight = false;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            xMove += speed;
            SpriteComponent.facingRight = true;
        }

        // Get Map Data so that we know where the walls at
        TileWorldComponent world = null;
        if (getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).size() > 0) {
            Entity worldEntity = getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).first();
            world = worldEntity.getComponent(TileWorldComponent.class);
        }

        // == X-Axis ==

        float oldX = transform.pos.x; // Store old position for collision rollback
        transform.pos.x += xMove; //Player is moving
        transform.updateBounds(); //Updates hixbox

        //Checks if we hit anything on the x-axis
        if(checkEntityCollison(entity, transform) || checkTileCollision(transform, world)) {
            transform.pos.x = oldX;
            transform.updateBounds();
        }

        //== Y-Axis ==
        float oldY = transform.pos.y; // Store old position for collision rollback
        transform.pos.y += yMove; //Player is moving
        transform.updateBounds(); //Updates hixbox

        //Checks if we hit anything on the y-axis
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
            float mapPixelWidth = world.mapWidth * world.tileWidth;
            float mapPixelHeight = world.mapHeight * world.tileHeight;

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

                    //Makes everything Outside the map as a solid wall
                    if (x < 0 || x >= world.mapWidth || y < 0 || y >= world.mapHeight) {
                        return true;
                    }

                    //3.) Looks up the actual wall/tileId
                    int tileId = world.map[x][y];
                    Tile tile = TileRegistry.getTile(tileId);

                    //4.) Checks if Tile isSolid
                    if (tile != null && tile.isSolid) {
                        return true; //Player has bumped into something
                    }
                }
            }
            return false;
        }

    }

