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

}
