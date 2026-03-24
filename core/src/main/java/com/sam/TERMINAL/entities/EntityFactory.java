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
        sprite.drawWidth = 128f;
        sprite.drawHeight = 250f;

        player.add(engine.createComponent(PlayerComponent.class));

        //Persistence Data this is where player position is saved
        player.add(new PersistenceComponent("PLAYER", "PLAYER-POGI"));
        player.add(engine.createComponent(InventoryComponent.class));
        engine.addEntity(player);
    }

    public static  void createDoor(PooledEngine engine, float x, float y, TextureRegion closedSprite, String saveId) {
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

        door.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(door);
    }

    public static void createKey (PooledEngine engine, float x, float y, TextureRegion beepRegion, String saveId) {
        Entity beep = engine.createEntity();

        TransformComponent beepTrans = engine.createComponent(TransformComponent.class);
        beepTrans.pos.set(x, y);
        beepTrans.width = 40;
        beepTrans.height = 30;
        beepTrans.updateBounds();
        beep.add(beepTrans);

        SpriteComponent beepSprite = engine.createComponent(SpriteComponent.class);
        beepSprite.staticSprite = beepRegion;
        beepSprite.isStatic = true;
        beepSprite.drawWidth = 40;
        beepSprite.drawHeight = 30;
        beep.add(beepSprite);

        beep.add(new InteractableComponent("beep", 40f));
        beep.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(beep);
    }

    public static void createEnemy(PooledEngine engine, float x, float y, TextureRegion texture) {
        Entity enemy = engine.createEntity();
        TransformComponent t = engine.createComponent(TransformComponent.class);
        t.pos.set(x, y); t.width = 32; t.height = 32; t.updateBounds();
        enemy.add(t);

        SpriteComponent s = engine.createComponent(SpriteComponent.class);
        s.staticSprite = texture; s.isStatic = true; s.drawWidth = 32; s.drawHeight = 32;
        enemy.add(s);

        enemy.add(new EnemyComponent());
        engine.addEntity(enemy);
    }

    public static void createFlashlight(PooledEngine engine, float x, float y, TextureRegion texture, String saveId) {
        Entity flashlight = engine.createEntity();

        // Position and Bounds
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 50;   // Slightly bigger than the beep card
        transform.height = 50;
        transform.updateBounds();
        flashlight.add(transform);

        // Visuals
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = texture;
        sprite.isStatic = true;
        sprite.drawWidth = 50;  // Visual scale
        sprite.drawHeight = 50;
        flashlight.add(sprite);

        // Interaction - We name the interactable "flashlight" so the
        // InteractionSystem knows to trigger the light when picked up.
        flashlight.add(new InteractableComponent("flashlight", 45f));

        // Persistence
        flashlight.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(flashlight);
    }





    /**
     * TODO: Future entity creation methods
     * - createCommuter(engine, x, y)
     * - createConductor(engine, x, y)
     * - createTurnstile(engine, x, y)
     */
}
