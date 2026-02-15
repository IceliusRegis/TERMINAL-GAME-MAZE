package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ETC1;
import com.sam.TERMINAL.components.InteractableComponent;
import com.sam.TERMINAL.components.InventoryComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * InteractionSystem - Handles the logic for using objects in the world.
 *
 * Responsibilities:
 * 1. Checks distance between Player and all Interactable entities.
 * 2. Visualizes prompt (e.g. "Press E") if close enough (Future Todo).
 * 3. Listens for the 'E' key input.
 * 4. Executes specific logic based on item type (Key -> Pickup, Door -> Open).
 *
 * Note: This extends EntitySystem (not IteratingSystem) because we need to compare
 * one entity (Player) against many others (Keys/Doors) manually.
 */

public class InteractionSystem extends EntitySystem {

    private ComponentMapper<TransformComponent> transformMapper;
    private  ComponentMapper<InteractableComponent> interactMapper;
    private  ComponentMapper<InventoryComponent> inventoryMapper;


    public InteractionSystem() {
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        interactMapper = ComponentMapper.getFor(InteractableComponent.class);
        inventoryMapper =  ComponentMapper.getFor(InventoryComponent.class);
    }

    @Override
    public void update(float deltaTime){
        //1.) Find the player tag and their position first
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(PlayerComponent.class, TransformComponent.class).get());

        //Mkake sures player is loaded first
        if (players.size() == 0) return;
        //Makes player the first in the list and gets their position
        Entity player = players.first();
        TransformComponent playerPos = transformMapper.get(player);

        //2.) Find the those with the interact tag and their position
        ImmutableArray<Entity> interactables = getEngine().getEntitiesFor(Family.all(InteractableComponent.class, TransformComponent.class).get());

        //3.) Check Distance of items from player using dst(built in libgdx)
        for (Entity target : interactables) {
            InteractableComponent interact = interactMapper.get(target);
            if (!interact.isActive) continue; //If item or tile or anything has been touched by the player it skips it

            TransformComponent targetPos = transformMapper.get(target);

            //Gets the distance between item and object
            float dist = playerPos.pos.dst(targetPos.pos);

            if (dist <= interact.radius) {
                // TODO: In the future, draw a "Press E" tooltip here!

                //4.) Receives Interact input
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    executeInteraction(player, target, interact);
                }
            }
        }
    }

    private void  executeInteraction(Entity player, Entity target, InteractableComponent typeData) {
        InventoryComponent inventory = inventoryMapper.get(player);

        switch (typeData.type) {
            case "beep":
                System.out.println("Picked up a BEEP CARD!");
                if (inventory != null) inventory.addItem("beep_card");
                //Remove the picked up item
                getEngine().removeEntity(target);
                break;

            case "door":
                if (inventory != null && inventory.hasItem("beep_card")) {
                    System.out.println("Door Unlocked!");
                    //Change the sprite of door
                    typeData.isActive = false;
                    //Remove door for now since no asset yet
                    getEngine().removeEntity(target);
                } else {
                    System.out.println("Door Locked! Find the Beep Card.");
                }
                break;

            default:
                System.out.println("Interacted with " + typeData.type);
        }

    }

}
