package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.components.CollisionComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * MovementSystem - Handles player input and collision detection.
 *
 * Processes entities that have Transform + Player components.
 * Reads WASD input, moves the player, and prevents wall collisions.
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

        // Store old position for collision rollback
        float oldX = transform.pos.x;
        float oldY = transform.pos.y;

        // Calculate movement based on deltaTime for smooth, framerate-independent motion
        float speed = PLAYER_SPEED * deltaTime;

        // Handle WASD input
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            transform.pos.y += speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            transform.pos.y -= speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            transform.pos.x -= speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            transform.pos.x += speed;
        }

        // Update collision bounds to new position
        transform.updateBounds();

        // Check collision with all solid entities (walls)
        for (Entity wall : getEngine().getEntitiesFor(Family.all(CollisionComponent.class).get())) {
            TransformComponent wallTransform = transformMapper.get(wall);

            // If player overlaps wall, reset to old position (BUMP!)
            if (transform.bounds.overlaps(wallTransform.bounds)) {
                transform.pos.set(oldX, oldY);
                transform.updateBounds();
                break; // No need to check other walls


            }
            System.out.println("MC Position: X=" + transform.pos.x + ", Y=" + transform.pos.y);
        }
    }
}
