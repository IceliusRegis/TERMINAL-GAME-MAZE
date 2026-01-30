package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.CollisionComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * EntityFactory - Blueprint for creating game entities.
 *
 * Centralizes entity creation logic to keep Main.java clean.
 * Follows the factory pattern for consistent entity construction.
 */
public class EntityFactory {

    /**
     * Creates the player entity with movement and rendering capabilities.
     *
     * @param engine The Ashley engine to add the entity to
     * @param walkAnimation The player's walking animation
     */
    public static void createPlayer(PooledEngine engine, Animation<TextureRegion> walkAnimation) {
        Entity player = engine.createEntity();

        // Add transform component for position
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(100, 100); // Starting position
        transform.width = 64;
        transform.height = 64;
        transform.updateBounds();
        player.add(transform);

        // Add sprite component for rendering
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.walkAnimation = walkAnimation;
        sprite.looping = true;
        player.add(sprite);

        // Add player marker so systems can identify this entity
        player.add(engine.createComponent(PlayerComponent.class));

        engine.addEntity(player);
    }

    /**
     * Creates a solid wall entity at the specified position.
     *
     * @param engine The Ashley engine to add the entity to
     * @param x X-coordinate of wall position
     * @param y Y-coordinate of wall position
     */
    public static void createWall(PooledEngine engine,float x, float y, TextureRegion wallSprite) {
        Entity wall = engine.createEntity();

        // Add transform component
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 32;
        transform.height = 32;
        transform.updateBounds();
        wall.add(transform);

        SpriteComponent s = engine.createComponent(SpriteComponent.class);
        // Since walls don't walk, we create a "Still" animation with just 1 frame
        s.walkAnimation = new Animation<>(1f, wallSprite);
        s.looping = false;
        wall.add(s);

        // Add sprite component with static texture
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = wallSprite;
        sprite.isStatic = true;  // Mark as non-animated
        wall.add(sprite);

        wall.add(new CollisionComponent());

        // Add collision marker so player can bump into it
        wall.add(engine.createComponent(CollisionComponent.class));

        engine.addEntity(wall);
    }

    /**
     * TODO: Future entity creation methods
     * - createCommuter(engine, x, y)
     * - createConductor(engine, x, y)
     * - createTurnstile(engine, x, y)
     */
}
