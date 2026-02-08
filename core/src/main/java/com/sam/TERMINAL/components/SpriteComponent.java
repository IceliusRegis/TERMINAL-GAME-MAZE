package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * SpriteComponent - Holds visual animation data for rendering.
 *
 * Entities with this component can be drawn by the RenderSystem.
 * Tracks animation state and current frame.
 */
public class SpriteComponent implements Component {

    /** The walking animation from sprite sheet */
    public Animation<TextureRegion> walkAnimation;

    /** Tracks elapsed time for animation frame switching */
    public float stateTime = 0f;

    /** Whether animation should loop */
    public boolean looping = true;

    public TextureRegion staticSprite;

    // Set this to true if using staticSprite instead of animation
    public boolean isStatic = false;

    public static boolean facingRight = true; // Default to facing right

    public float drawWidth;
    public float drawHeight;
}
