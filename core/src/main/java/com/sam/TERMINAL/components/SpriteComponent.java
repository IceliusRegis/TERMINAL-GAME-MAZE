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
    public Animation<TextureRegion> idleAnimation;

    // Track which Animation is playing
    public Animation<TextureRegion> currentAnimation;

    /** Tracks elapsed time for animation frame switching */
    public float stateTime = 0f;
    /** Whether animation should loop */
    public boolean looping = true;
    public TextureRegion staticSprite;
    // Set this to true if using staticSprite instead of animation
    public boolean isStatic = false;

    public boolean facingRight = true; // Default to facing right
    public float facingAngle = 0f; // 0=Right, 90=Up, 180=Left, 270=Down

    public float drawWidth;
    public float drawHeight;

    public float offsetX = 0f;
    public float offsetY = 0f;

    public boolean flipX = false;
    public boolean flipY = false;
}
