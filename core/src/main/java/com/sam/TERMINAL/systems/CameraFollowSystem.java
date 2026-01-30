package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * CameraFollowSystem - Makes the camera smoothly follow the player.
 *
 * This system:
 * - Finds entities with PlayerComponent (should only be one)
 * - Updates camera position to center on player
 * - Can add smooth lerp movement for cinematic effect
 */
public class CameraFollowSystem extends IteratingSystem {
    private final OrthographicCamera camera;

    // Component mappers for fast access
    private ComponentMapper<TransformComponent> positionMapper;

    // Smoothing factor (1.0 = instant, lower = smoother but slower)
    private static final float LERP_FACTOR = 0.1f;

    public CameraFollowSystem(OrthographicCamera camera) {
        // Only process entities with PlayerComponent and PositionComponent
        super(Family.all(PlayerComponent.class, TransformComponent.class).get());
        this.camera = camera;

        // Initialize component mappers
        positionMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent position = positionMapper.get(entity);

        // Option 1: INSTANT camera follow (no smoothing)
        camera.position.x = position.pos.x;
        camera.position.y = position.pos.y;

        // Option 2: SMOOTH camera follow (uncomment to use)
        // This creates a cinematic "lag" effect
        /*
        camera.position.x += (position.x - camera.position.x) * LERP_FACTOR;
        camera.position.y += (position.y - camera.position.y) * LERP_FACTOR;
        */

        // Update camera matrices
        camera.update();
    }
}
