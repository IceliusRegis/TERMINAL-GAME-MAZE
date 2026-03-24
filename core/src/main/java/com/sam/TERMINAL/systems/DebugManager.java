package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.TimeUtils;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * DebugManager - Handles developer shortcuts.
 * 
 * Separated from gameplay systems so debug polling never interferes
 * with or consumes regular gameplay input.
 */
public class DebugManager {
    private long lastLTapTime = 0;
    private long lastBTapTime = 0;
    private static final long DOUBLE_TAP_MAX_DELAY = 400; // milliseconds

    public boolean showHitboxes = false;
    private ShapeRenderer shapeRenderer;

    public DebugManager() {
        this.shapeRenderer = new ShapeRenderer();
    }

    /**
     * Polls debug inputs. Call this from Main.render() outside the ECS update loop.
     */
    public void update(LightingSystem lightingSystem) {
        if (lightingSystem == null) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            long currentTime = TimeUtils.millis();
            
            if (currentTime - lastLTapTime < DOUBLE_TAP_MAX_DELAY) {
                // Double tap detected! Toggle lighting.
                lightingSystem.lightingEnabled = !lightingSystem.lightingEnabled;
                Gdx.app.log("TERMINAL_DEBUG", "Lighting Enabled: " + lightingSystem.lightingEnabled);
                
                // Reset tap time so a third tap doesn't trigger it again instantly
                lastLTapTime = 0; 
            } else {
                lastLTapTime = currentTime;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            long currentTime = TimeUtils.millis();
            
            if (currentTime - lastBTapTime < DOUBLE_TAP_MAX_DELAY) {
                // Double tap detected! Toggle hitboxes.
                showHitboxes = !showHitboxes;
                Gdx.app.log("TERMINAL_DEBUG", "Hitboxes Enabled: " + showHitboxes);
                
                // Reset tap time
                lastBTapTime = 0; 
            } else {
                lastBTapTime = currentTime;
            }
        }
    }

    /**
     * Renders outlines of the hitboxes for all entities with a TransformComponent.
     */
    public void renderHitboxes(PooledEngine engine, OrthographicCamera camera) {
        if (!showHitboxes) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);

        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(TransformComponent.class).get());
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            TransformComponent transform = e.getComponent(TransformComponent.class);
            if (transform != null && transform.bounds != null) {
                shapeRenderer.rect(transform.bounds.x, transform.bounds.y, transform.bounds.width, transform.bounds.height);
            }
        }
        
        shapeRenderer.end();
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
