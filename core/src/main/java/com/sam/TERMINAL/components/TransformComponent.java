package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * TransformComponent - Handles position and collision data for entities.
 *
 * Every entity that exists in the game world needs this component.
 * Contains position, collision bounds, and size information.
 */
public class TransformComponent implements Component {

    /** Current position in world coordinates */
    public Vector2 pos = new Vector2(100, 100);

    /** Collision rectangle for bumping detection */
    public Rectangle bounds = new Rectangle();

    /** Visual size of the entity in pixels */
    public float width = 32;
    public float height = 32;

    public float collisionWidth = 0;
    public float collisionHeight = 0;
    public float collisionOffsetX = 0;
    public float collisionOffsetY = 0;

    /**
     * Updates the collision bounds to match current position.
     * Call this after modifying pos.
     */
    public void updateBounds() {
        float bw = (collisionWidth > 0) ? collisionWidth : width;
        float bh = (collisionHeight > 0) ? collisionHeight : height;
        bounds.set(
                pos.x + collisionOffsetX,
                pos.y + collisionOffsetY,
                bw,
                bh);
    }
}
