package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.TileWorldComponent;
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
    private ComponentMapper<TileWorldComponent> worldMapper =
        ComponentMapper.getFor(TileWorldComponent.class);

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
        TransformComponent transform = positionMapper.get(entity);

        //SMOOTH camera follow
        // This creates a cinematic "lag" effect
        camera.position.x += (transform.pos.x - camera.position.x) * 0.1f;
        camera.position.y += (transform.pos.y - camera.position.y) * 0.1f;

        //Dynamically read map dimensions from TileWorldComponent (fallback to 50x50)
        ImmutableArray<Entity> worldEntities = getEngine()
            .getEntitiesFor(Family.all(TileWorldComponent.class).get());

        float mapWidth  = 50 * 32f;  // fallback default
        float mapHeight = 50 * 32f;

        if (worldEntities.size() > 0) {
            TileWorldComponent world = worldMapper.get(worldEntities.first());
            mapWidth  = world.mapWidthTiles  * world.tileWidth;
            mapHeight = world.mapHeightTiles * world.tileHeight;
        }

        //Calculate the "Half-Sizes" of the camera view
        // The camera position is its CENTER, not its corner.
        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;

        //The math is basically it doesn't let the center get too close to the edge

        //X-Axis
        if (camera.position.x < halfW) camera.position.x = halfW; // If Camera is near the left side stop the cam mid way through
        if (camera.position.x > mapWidth - halfW) camera.position.x = mapWidth - halfW; // If Camera is near at the right to the same

        //Y-Axis
        if (camera.position.y < halfH) camera.position.y = halfH;
        if (camera.position.y > mapHeight - halfH) camera.position.y = mapHeight - halfH;


        // Update camera matrices
        camera.update();
    }
}
