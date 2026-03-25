package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.*;

/**
 * InteractionSystem - Handles the logic for using objects in the world.
 *
 * Responsibilities:
 * 1. Checks distance between Player and all Interactable entities.
 * 2. Visualizes prompt (e.g. "Press E") if close enough (Future Todo).
 * 3. Listens for the 'E' key input.
 * 4. Executes specific logic based on item type (Key -> Pickup, Door -> Open).
 *
 * Note: This extends EntitySystem (not IteratingSystem) because we need to
 * compare
 * one entity (Player) against many others (Keys/Doors) manually.
 */

public class InteractionSystem extends EntitySystem {

    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<InteractableComponent> interactMapper;
    private ComponentMapper<InventoryComponent> inventoryMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;

    private final TextureRegion openDoorSprite;

    // Rendering context for the "Press E" prompt
    private final SpriteBatch batch;
    private Texture promptTexture;
    private TextureRegion promptRegion;

    private static final float PROMPT_WIDTH = 24f;
    private static final float PROMPT_HEIGHT = 24f;
    private static final float PROMPT_OFFSET_Y = 8f; // pixels above the entity top

    public InteractionSystem(TextureRegion openDoorSprite, SpriteBatch batch) {
        this.openDoorSprite = openDoorSprite;
        this.batch = batch;

        // Null-guarded asset load — game runs fine if the file is missing
        if (Gdx.files.internal("ui/press_e.png").exists()) {
            promptTexture = new Texture(Gdx.files.internal("ui/press_e.png"));
            promptRegion = new TextureRegion(promptTexture);
        }

        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        interactMapper = ComponentMapper.getFor(InteractableComponent.class);
        inventoryMapper = ComponentMapper.getFor(InventoryComponent.class);
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    }

    @Override
    public void update(float deltaTime) {
        // 1.) Find the player tag and their position first
        ImmutableArray<Entity> players = getEngine()
                .getEntitiesFor(Family.all(PlayerComponent.class, TransformComponent.class).get());

        // Mkake sures player is loaded first
        if (players.size() == 0)
            return;
        // Makes player the first in the list and gets their position
        Entity player = players.first();
        TransformComponent playerPos = transformMapper.get(player);

        // 2.) Find the those with the interact tag and their position
        ImmutableArray<Entity> interactables = getEngine()
                .getEntitiesFor(Family.all(InteractableComponent.class, TransformComponent.class).get());

        // 3.) Check Distance of items from player using dst(built in libgdx)
        for (Entity target : interactables) {
            InteractableComponent interact = interactMapper.get(target);
            if (!interact.isActive)
                continue; // If item or tile or anything has been touched by the player it skips it

            TransformComponent targetPos = transformMapper.get(target);

            // Gets the distance between item and object (center-to-center)
            float playerCenterX = playerPos.pos.x + (playerPos.width / 2f);
            float playerCenterY = playerPos.pos.y + (playerPos.height / 2f);
            float targetCenterX = targetPos.pos.x + (targetPos.width / 2f);
            float targetCenterY = targetPos.pos.y + (targetPos.height / 2f);
            float dist = (float) Math.sqrt(
                    Math.pow(playerCenterX - targetCenterX, 2) +
                            Math.pow(playerCenterY - targetCenterY, 2));

            if (dist <= interact.radius) {
                if (dist <= interact.radius) {
                    // Draw the "Press E" prompt above the entity center
                    if (promptRegion != null) {
                        float promptX = targetCenterX - PROMPT_WIDTH / 2f;
                        float promptY = targetPos.pos.y + targetPos.height + PROMPT_OFFSET_Y;
                        batch.draw(promptRegion, promptX, promptY, PROMPT_WIDTH, PROMPT_HEIGHT);
                    }

                    // 4.) Receives Interact input
                    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        executeInteraction(player, target, interact);
                    }
                }

                // 4.) Receives Interact input
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    executeInteraction(player, target, interact);
                }
            }
        }
    }

    private void executeInteraction(Entity player, Entity target, InteractableComponent typeData) {
        InventoryComponent inventory = inventoryMapper.get(player);

        switch (typeData.type) {
            case "beep":
                System.out.println("Picked up a BEEP CARD!");
                if (inventory != null)
                    inventory.addItem("beep_card");
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                break;

            case "door":
                if (inventory != null && inventory.hasItem("beep_card")) {
                    System.out.println("Door Unlocked!");

                    SpriteComponent doorSprite = spriteMapper.get(target);
                    if (doorSprite != null) {
                        doorSprite.staticSprite = openDoorSprite;
                    }

                    target.remove(CollisionComponent.class);
                    typeData.isActive = false;
                } else {
                    System.out.println("Door Locked! Find the Beep Card.");
                }
                break;

            case "flashlight":
                System.out.println("Picked up the FLASHLIGHT!");
                if (inventory != null) {
                    inventory.addItem("flashlight");
                }
                LightingSystem lightingSystem = getEngine().getSystem(LightingSystem.class);
                if (lightingSystem != null) {
                    lightingSystem.createPlayerLight(player, true);
                }
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                break;

            default:
                System.out.println("Interacted with " + typeData.type);
        }

    }

    public void dispose() {
        if (promptTexture != null) {
            promptTexture.dispose();
            promptTexture = null;
        }
    }

}
