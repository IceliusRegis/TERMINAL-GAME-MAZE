package com.sam.TERMINAL.entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.*;

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
    public static void createPlayer(PooledEngine engine, float bodyWidth, float bodyHeight, Animation<TextureRegion> walkAnimation) {
        Entity player = engine.createEntity();

        // Add transform component for position
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(100, 100); // Starting position
        transform.width = bodyWidth;
        transform.height = bodyHeight;
        transform.updateBounds();
        player.add(transform);

        // Add sprite component for rendering
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.walkAnimation = walkAnimation;
        sprite.looping = true;
        player.add(sprite);

        sprite.drawWidth = 150f;  // The actual size of the sprite frame
        sprite.drawHeight = 150f;

        // Add player marker so systems can identify this entity
        player.add(engine.createComponent(PlayerComponent.class));


        //Persistence Data this is where player position is saved
        PersistenceComponent persistence = engine.createComponent(PersistenceComponent.class);
        persistence.saveId = "PLAYER-POGI"; //UID of the player
        player.add(persistence);




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


        // Add sprite component with static texture
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = wallSprite;
        sprite.isStatic = true;  // Mark as non-animated
        wall.add(sprite);

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
