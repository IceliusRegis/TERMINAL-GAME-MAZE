package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.persistence.GameData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.persistence.SaveManager;
import java.util.UUID;

/**
 * SaveSystem - The bridge between the active Game World (ECS) and the File System.
 *
 * Responsibilities:
 * 1. Input Listener: Monitors F5 to Save and F9 to Load.
 * 2. SAVE Flow (Export):
 * - Creates a "snapshot" of the current game state (GameData).
 * - Iterates through entities tagged with PersistenceComponent.
 * - Extracts data (e.g., Position X/Y) from Components -> GameData.
 * - Delegated writing to disk via SaveManager.
 * 3. LOAD Flow (Import):
 * - Retrieves the save file from disk via SaveManager.
 * - Matches loaded data to active entities using their unique 'saveId'.
 * - Overwrites entity Component data (Position X/Y) with saved values.
 * - CRITICAL: Updates derived data (like collision bounds) to prevent "ghost" bugs.
 */

public class SaveSystem extends IteratingSystem {

    //Declaration of Mapper,basically bookmarking the position and save state
    private ComponentMapper<PersistenceComponent> persistenceMapper;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<InventoryComponent> inventoryMapper;
    private  ComponentMapper<InteractableComponent> interactMapper;
    private  ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<CollisionComponent> collisionMapper;

    //Save State
    private GameData pendingSaveData;
    private boolean saving = false;
    private String currentSaveFile = "saveFile.json";

    //Load State
    private GameData loadedData; // reads data from disk
    private boolean loading = false; //tells the game we are loading data

    //IDs
    private String currentRunId = "";

    //Sprites
    private final TextureRegion keySprite;
    private final TextureRegion flashlightSprite;

    public SaveSystem(TextureRegion keySprite, TextureRegion flashlightSprite) {

        super(Family.all(PersistenceComponent.class).get());

        this.keySprite = keySprite;
        this.flashlightSprite = flashlightSprite;

        //Initialize Mappers
        persistenceMapper = ComponentMapper.getFor(PersistenceComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        inventoryMapper = ComponentMapper.getFor(InventoryComponent.class);
        interactMapper = ComponentMapper.getFor(InteractableComponent.class);
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
        collisionMapper = ComponentMapper.getFor(CollisionComponent.class);
    }

    public void  generateNewRunId() {
        this.currentRunId = UUID.randomUUID().toString();
    }

    public void setRunID(String id) {
        this.currentRunId = id;
    }

    @Override
    public void update(float deltaTime) {

        //Save Button Trigger
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            triggerManualSave("saveFile.json");
        }

        //Load Button Trigger
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            triggerManualLoad("saveFile.json");
        }

        if (saving || loading) {
            // MAGIC LINE: This tells Ashley to find all matching entities and run
            // processEntity() on them right now.
            super.update(deltaTime);

            if (saving) {
                //Finished collecting data, now write it to disk.
                SaveManager.save(pendingSaveData, currentSaveFile);
                System.out.println("Saved to: " + currentSaveFile);

                //Reset the system so it stops saving
                saving = false;
                pendingSaveData = null;

            }

            if (loading) {
                System.out.println("Loaded from: " + currentSaveFile);
                loading = false;
                loadedData = null;
            }


        }
    }

    public void triggerManualSave(String fileName) {
        if (!saving) { // Prevent double-saving if already in progress
            this.currentSaveFile = fileName;
            this.saving = true;
            this.loading = false;
            this.pendingSaveData = new GameData();
            this.pendingSaveData.runId = this.currentRunId;
        }
    }

    public void triggerManualLoad(String fileName) {
        if(!loading) {
            //Check if file exist first to avoid crashes
            GameData data = SaveManager.load(fileName);
            if (data != null) {
                this.currentSaveFile = fileName;
                this.loadedData = data;
                this.loading = true;
                this.saving = false;
                if (data.runId !=null) this.currentRunId = data.runId;
            } else {
                System.out.println("Cannot load: " + fileName + " does not exist");
            }
        }
    }


    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        // This method runs ONCE for every valid entity found.

        // Get the actual data components from the entity
        PersistenceComponent persistence = persistenceMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);


        // Saving (Entity to Data)
        if (saving) {
            switch (persistence.type) {

                case "PLAYER":
                    TransformComponent pTrans = transformMapper.get(entity);
                    pendingSaveData.playerX = pTrans.pos.x;
                    pendingSaveData.playerY = pTrans.pos.y;

                        InventoryComponent pInventory = inventoryMapper.get(entity);
                        if (pInventory !=null) {
                            pendingSaveData.inventoryItems.clear();
                            pendingSaveData.inventoryItems.addAll(pInventory.items);
                        }
                    break;

                case "INTERACTABLE":
                    InteractableComponent interact = interactMapper.get(entity);
                    if (interact !=null) {
                        pendingSaveData.interactableStates.put(persistence.saveId, interact.isActive);
                    }
                    break;
            }
        }

        // Loading (Data to Entity)
        else if (loading) {

            if (loadedData != null && !loadedData.runId.isEmpty()) {
                this.currentRunId = loadedData.runId;
            }

            switch (persistence.type) {

                case "PLAYER":
                    TransformComponent pTrans = transformMapper.get(entity);
                    pTrans.pos.x = loadedData.playerX;
                    pTrans.pos.y = loadedData.playerY;
                    pTrans.updateBounds();


                    InventoryComponent pInventoryLoad = inventoryMapper.get(entity);
                    if (pInventoryLoad != null && loadedData.inventoryItems !=null) {
                        pInventoryLoad.items.clear();
                        pInventoryLoad.items.addAll(loadedData.inventoryItems);
                        System.out.println("Inventory Loaded: " + pInventoryLoad.items.size() + " item/s.");
                    }

                    break;


                case "INTERACTABLE":
                    if (loadedData.interactableStates.containsKey(persistence.saveId)) {
                        boolean shouldBeActive = loadedData.interactableStates.get(persistence.saveId);

                        InteractableComponent interactLoad = interactMapper.get(entity);
                        if (interactLoad !=null) {
                            interactLoad.isActive = shouldBeActive;

                            //If Item was taken
                            if (!shouldBeActive) {
                                if (interactLoad.type.equals("beep")) {
                                    entity.remove((SpriteComponent.class));
                                } else if (interactLoad.type.equals("flashlight")) {
                                    entity.remove((SpriteComponent.class));
                                }
                            }

                            //If item was not picked, restore its sprite
                            else {
                                if (spriteMapper.get(entity) == null) {
                                    // createComponent() returns a pooled instance that may carry
                                    // stale field values from a previous lifecycle. Explicitly
                                    // reset every field we care about to match EntityFactory's
                                    // canonical dimensions so the item renders at the correct size.
                                    SpriteComponent restoredSprite = getEngine().createComponent(SpriteComponent.class);
                                    restoredSprite.isStatic = true;
                                    restoredSprite.staticSprite = null;
                                    restoredSprite.drawWidth = 0;
                                    restoredSprite.drawHeight = 0;

                                    if (interactLoad.type.equals("beep")) {
                                        // Canonical beep card dimensions from EntityFactory.createKey()
                                        restoredSprite.staticSprite = keySprite;
                                        restoredSprite.drawWidth  = 40;
                                        restoredSprite.drawHeight = 30;

                                        // Also reset the TransformComponent so collision bounds
                                        // reflect the correct world-unit size, not pooled leftovers.
                                        TransformComponent beepTransform = transformMapper.get(entity);
                                        if (beepTransform != null) {
                                            beepTransform.width  = 40;
                                            beepTransform.height = 30;
                                            beepTransform.updateBounds();
                                        }

                                    } else if (interactLoad.type.equals("flashlight")) {
                                        // Canonical flashlight dimensions from EntityFactory.createFlashlight()
                                        restoredSprite.staticSprite = flashlightSprite;
                                        restoredSprite.drawWidth  = 50;
                                        restoredSprite.drawHeight = 50;

                                        // Same guard on the TransformComponent.
                                        TransformComponent flTransform = transformMapper.get(entity);
                                        if (flTransform != null) {
                                            flTransform.width  = 50;
                                            flTransform.height = 50;
                                            flTransform.updateBounds();
                                        }
                                    }

                                    // Only add the component if we assigned a valid sprite
                                    if (restoredSprite.staticSprite != null) {
                                        entity.add(restoredSprite);
                                    }
                                }
                            }
                        }
                    }
                    break;

            }
        }
    }
}

