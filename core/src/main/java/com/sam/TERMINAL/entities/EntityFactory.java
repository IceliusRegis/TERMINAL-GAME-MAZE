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
    public static void createPlayer(PooledEngine engine, float x, float y, float bodyWidth, float bodyHeight,
                                    Animation<TextureRegion> walkAnimation, Animation<TextureRegion> idleAnimation) {
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

        sprite.offsetY = 24f;

        player.add(engine.createComponent(PlayerComponent.class));

        // Persistence Data this is where player position is saved
        player.add(new PersistenceComponent("PLAYER", "PLAYER-POGI"));
        player.add(engine.createComponent(InventoryComponent.class));
        player.add(new BatteryComponent(100f));
        engine.addEntity(player);
    }

    public static void createKey(PooledEngine engine, float x, float y, TextureRegion beepRegion, String saveId) {
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
        t.pos.set(x, y);
        t.width = 32;
        t.height = 32;
        t.updateBounds();
        enemy.add(t);

        SpriteComponent s = engine.createComponent(SpriteComponent.class);
        s.staticSprite = texture;
        s.isStatic = true;
        s.drawWidth = 32;
        s.drawHeight = 32;
        enemy.add(s);

        enemy.add(new EnemyComponent());
        enemy.add(new PersistenceComponent("ENEMY", "ENEMY_" + java.util.UUID.randomUUID().toString()));
        engine.addEntity(enemy);
    }

    /** Tutorial trigger object: Lily. */
    public static void createLily(PooledEngine engine, float x, float y, TextureRegion lilyRegion, String saveId) {
        Entity lily = engine.createEntity();

        TransformComponent t = engine.createComponent(TransformComponent.class);
        t.pos.set(x, y);
        t.width = 48f;
        t.height = 48f;
        t.updateBounds();
        lily.add(t);

        SpriteComponent s = engine.createComponent(SpriteComponent.class);
        s.staticSprite = lilyRegion;
        s.isStatic = true;
        s.drawWidth = 48f;
        s.drawHeight = 48f;
        s.name = "lily";
        lily.add(s);

        lily.add(new InteractableComponent("lily", 80f));
        lily.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(lily);
    }

    /** Decorative lily left at the Level 2 accident / ending site (not interactable). */
    public static void createLilyMemorial(PooledEngine engine, float x, float y, TextureRegion lilyRegion) {
        Entity e = engine.createEntity();

        TransformComponent t = engine.createComponent(TransformComponent.class);
        t.pos.set(x, y);
        t.width = 48f;
        t.height = 48f;
        t.updateBounds();
        e.add(t);

        SpriteComponent s = engine.createComponent(SpriteComponent.class);
        s.staticSprite = lilyRegion;
        s.isStatic = true;
        s.drawWidth = 48f;
        s.drawHeight = 48f;
        s.name = "lily_memorial";
        e.add(s);

        engine.addEntity(e);
    }

    public static void createFlashlight(PooledEngine engine, float x, float y, TextureRegion texture, String saveId) {
        Entity flashlight = engine.createEntity();

        // Position and Bounds
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 50; // Slightly bigger than the beep card
        transform.height = 50;
        transform.updateBounds();
        flashlight.add(transform);

        // Visuals
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = texture;
        sprite.isStatic = true;
        sprite.drawWidth = 50; // Visual scale
        sprite.drawHeight = 50;
        flashlight.add(sprite);

        // Interaction - We name the interactable "flashlight" so the
        // InteractionSystem knows to trigger the light when picked up.
        flashlight.add(new InteractableComponent("flashlight", 45f));

        // Persistence
        flashlight.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(flashlight);
    }

    public static void createBattery(PooledEngine engine, float x, float y, TextureRegion texture, String saveId) {
        Entity battery = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 70;
        transform.height = 70;
        transform.updateBounds();
        battery.add(transform);

        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = texture;
        sprite.isStatic = true;
        sprite.drawWidth = 70;
        sprite.drawHeight = 70;
        battery.add(sprite);

        battery.add(new InteractableComponent("battery", 40f));
        battery.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(battery);
    }

    public static Entity createPotion(PooledEngine engine, float x, float y, TextureRegion region, String saveId) {
        Entity potion = engine.createEntity();

        // 1. Position and Bounds (Matching your TransformComponent style)
        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 50;  // Adjust size as needed
        transform.height = 50;
        transform.updateBounds();
        potion.add(transform);

        // 2. Visuals (Matching your SpriteComponent style)
        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = region;
        sprite.isStatic = true;
        sprite.drawWidth = 50;
        sprite.drawHeight = 50;
        potion.add(sprite);

        // 3. Interaction (This makes it pick-up-able)
        // We call the type "potion" so your Inventory knows what it is
        potion.add(new InteractableComponent("potion", 40f));

        // 4. Persistence (So it saves/loads correctly)
        potion.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(potion);
        return potion;
    }

    /**
     * TODO: Future entity creation methods
     * - createCommuter(engine, x, y)
     * - createConductor(engine, x, y)
     * - createTurnstile(engine, x, y)
     */

    // =========================================================================
    // Level 2 Ending Key Items
    // =========================================================================

    /** Creates a Student ID pickup for the Good Ending path. */
    public static void createStudID(PooledEngine engine, float x, float y, TextureRegion texture, String saveId) {
        Entity studID = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 40;
        transform.height = 30;
        transform.updateBounds();
        studID.add(transform);

        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = texture;
        sprite.isStatic = true;
        sprite.drawWidth = 40;
        sprite.drawHeight = 30;
        studID.add(sprite);

        studID.add(new InteractableComponent("studID", 40f));
        studID.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(studID);
    }

    /** Creates a Papers pickup for the Good Ending path. */
    public static void createPapers(PooledEngine engine, float x, float y, TextureRegion texture, String saveId) {
        Entity papers = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 40;
        transform.height = 40;
        transform.updateBounds();
        papers.add(transform);

        SpriteComponent sprite = engine.createComponent(SpriteComponent.class);
        sprite.staticSprite = texture;
        sprite.isStatic = true;
        sprite.drawWidth = 40;
        sprite.drawHeight = 40;
        papers.add(sprite);

        papers.add(new InteractableComponent("papers", 72f));
        papers.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(papers);
    }

    /**
     * Creates an invisible confrontation trigger spot.
     * No sprite — only an InteractableComponent so the player can interact with it.
     */
    public static void createConfrontationSpot(PooledEngine engine, float x, float y, String saveId) {
        Entity spot = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.pos.set(x, y);
        transform.width = 32;
        transform.height = 32;
        transform.updateBounds();
        spot.add(transform);

        spot.add(new InteractableComponent("confrontation", 60f));
        spot.add(new PersistenceComponent("INTERACTABLE", saveId));

        engine.addEntity(spot);
    }
}
