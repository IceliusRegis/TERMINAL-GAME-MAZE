package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * RenderSystem - Draws all entities with sprites to the screen.
 *
 * Processes entities that have Transform + Sprite components.
 * Advances animation frames and renders them at entity positions.
 */
public class RenderSystem extends IteratingSystem {

    private final SpriteBatch batch;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;

    /**
     * @param batch The SpriteBatch used for drawing (must be managed externally)
     */
    public RenderSystem(SpriteBatch batch) {
        // Only process entities with BOTH Transform and Sprite components
        super(Family.all(TransformComponent.class, SpriteComponent.class).get());

        this.batch = batch;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);

        TextureRegion currentFrame;

        // === 1. GET SIZES ===
        // If drawWidth is 0 (forgot to set it), fallback to transform width
        float width = (sprite.drawWidth > 0) ? sprite.drawWidth : transform.width;
        float height = (sprite.drawHeight > 0) ? sprite.drawHeight : transform.height;

        // === 2. CENTER THE IMAGE ===
        // We calculate where to draw so the image is centered over the hitbox
        // Logic: (HitboxX) - (ExtraWidth / 2)
        float drawX = transform.pos.x - (width - transform.width) / 2;
        float drawY = transform.pos.y - (height - transform.height) / 2;


        if (sprite.isStatic) {
            // Use static sprite for walls/tiles
            currentFrame = sprite.staticSprite;
        } else {
            // Use animation for player/enemies
            sprite.stateTime += deltaTime;
            currentFrame = sprite.walkAnimation.getKeyFrame(
                sprite.stateTime,
                sprite.looping
            );

            // Inside RenderSystem processEntity

            // Logic: "If I want to face LEFT, but the sprite is flipped RIGHT -> Flip it"
            if (!sprite.facingRight && !currentFrame.isFlipX()) {
                currentFrame.flip(true, false);
            }
            // Logic: "If I want to face RIGHT, but the sprite is flipped LEFT -> Flip it"
            else if (sprite.facingRight && currentFrame.isFlipX()) {
                currentFrame.flip(true, false);
            }
        }

        // Draw sprite at entity's position
        batch.draw(
            currentFrame,
            drawX,
            drawY,
            width,
            height
        );
    }
}
