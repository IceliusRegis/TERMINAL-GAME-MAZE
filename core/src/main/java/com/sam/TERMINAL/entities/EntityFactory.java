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
     * @param engine        The Ashley engine to add the entity to
     * @param walkAnimation The player's walking animation
     * @param idleAnimation
     */
    public static void createPlayer(PooledEngine engine, float x, float y, float bodyWidth, float bodyHeight, Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation) {
        Entity player = engine.createEntity();

        // Add transform component for position
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y); // Starting position
        transform.width = bodyWidth;
        transform.height = bodyHeight;
        transform.updateBounds();
        player.add(transform);

        // Add sprite component for rendering
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.walkAnimation = walkAnimation;
        sprite.idleAnimation = idleAnimation;
        sprite.currentAnimation = idleAnimation;
        sprite.looping = true;
        player.add(sprite);


        // The actual size of the sprite frame HITBOX
        sprite.drawWidth = 130f;
        sprite.drawHeight = 130f;

        player.add(engine.createComponent(PlayerComponent.class));

        //Persistence Data this is where player position is saved
        player.add(new PersistenceComponent("PLAYER", "PLAYER-POGI"));
        player.add(engine.createComponent(InventoryComponent.class));
        engine.addEntity(player);
    }

    public static  void createDoor(PooledEngine engine, float x, float y, TextureRegion closedSprite) {
        Entity door = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 32;
        transform.height = 32;
        transform.updateBounds();
        door.add(transform);

        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = closedSprite;
        sprite.isStatic = true;
        door.add(sprite);

        door.add(engine.createComponent(CollisionComponent.class));
        door.add(new InteractableComponent("door", 40f));

        engine.addEntity(door);


    }



    /**
     * TODO: Future entity creation methods
     * - createCommuter(engine, x, y)
     * - createConductor(engine, x, y)
     * - createTurnstile(engine, x, y)
     */
}
