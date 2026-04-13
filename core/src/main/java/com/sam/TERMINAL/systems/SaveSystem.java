package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.persistence.GameData;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.persistence.SaveManager;
import java.util.UUID;

/**
 * SaveSystem - The bridge between the active Game World (ECS) and the File
 * System.
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
 * - CRITICAL: Updates derived data (like collision bounds) to prevent "ghost"
 * bugs.
 */

public class SaveSystem extends IteratingSystem {

    // Declaration of Mapper,basically bookmarking the position and save state
    private ComponentMapper<PersistenceComponent> persistenceMapper;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<InventoryComponent> inventoryMapper;
    private ComponentMapper<InteractableComponent> interactMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<CollisionComponent> collisionMapper;

    // Save State
    private GameData pendingSaveData;
    private boolean saving = false;
    private String currentSaveFile = "saveFile.json";

    // Load State
    private GameData loadedData; // reads data from disk
    private boolean loading = false; // tells the game we are loading data

    // IDs
    private String currentRunId = "";

    // Sprites
    private final TextureRegion keySprite;
    private final TextureRegion flashlightSprite;
    private final Animation<TextureRegion> enemyAnimation;
    private final TextureRegion batterySprite;
    private final TextureRegion potionSprite;

    public SaveSystem(TextureRegion keySprite, TextureRegion flashlightSprite, Animation<TextureRegion> enemyAnimation,
            TextureRegion batterySprite, TextureRegion potionSprite) {

        super(Family.all(PersistenceComponent.class).get());

        this.keySprite = keySprite;
        this.flashlightSprite = flashlightSprite;
        this.enemyAnimation = enemyAnimation;
        this.batterySprite = batterySprite;
        this.potionSprite = potionSprite;

        // Initialize Mappers
        persistenceMapper = ComponentMapper.getFor(PersistenceComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        inventoryMapper = ComponentMapper.getFor(InventoryComponent.class);
        interactMapper = ComponentMapper.getFor(InteractableComponent.class);
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
        collisionMapper = ComponentMapper.getFor(CollisionComponent.class);
    }

    public void generateNewRunId() {
        this.currentRunId = UUID.randomUUID().toString();
    }

    public void setRunID(String id) {
        this.currentRunId = id;
    }

    @Override
    public void update(float deltaTime) {

        // Save Button Trigger
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            triggerManualSave("saveFile.json");
        }

        // Load Button Trigger
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            triggerManualLoad("saveFile.json");
        }

        if (saving || loading) {
            // MAGIC LINE: This tells Ashley to find all matching entities and run
            // processEntity() on them right now.
            super.update(deltaTime);

            if (saving) {
                // Finished collecting data, now write it to disk.
                SaveManager.save(pendingSaveData, currentSaveFile);
                System.out.println("Saved to: " + currentSaveFile);

                // Reset the system so it stops saving
                saving = false;
                pendingSaveData = null;

            }

            if (loading) {
                spawnLoadedEntities();

                System.out.println("Loaded from: " + currentSaveFile);
                loading = false;
                loadedData = null;
            }

        }
    }

    private void spawnLoadedEntities() {
        // Clear existing dynamic entities completely to prevent duplication
        com.badlogic.gdx.utils.Array<Entity> toRemove = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : getEngine().getEntitiesFor(Family.all(EnemyComponent.class).get())) {
            toRemove.add(e);
        }
        for (Entity e : getEngine().getEntitiesFor(Family.all(InteractableComponent.class).get())) {
            toRemove.add(e);
        }
        for (Entity e : toRemove) {
            getEngine().removeEntity(e);
        }

        // Dynamically spawn the entities based on the saved coordinates
        if (loadedData != null) {
            if (loadedData.enemies != null && !loadedData.enemies.isEmpty()) {
                for (GameData.EnemySaveData eData : loadedData.enemies) {
                    com.sam.TERMINAL.entities.EntityFactory.createEnemy(
                            (com.badlogic.ashley.core.PooledEngine) getEngine(), eData.x, eData.y, enemyAnimation,
                            com.sam.TERMINAL.Main.ENEMY_DRAW_W, com.sam.TERMINAL.Main.ENEMY_DRAW_H);
                }
            }

            if (loadedData.items != null && !loadedData.items.isEmpty()) {
                for (GameData.ItemSaveData iData : loadedData.items) {
                    if (iData.type.equals("beep")) {
                        com.sam.TERMINAL.entities.EntityFactory.createKey(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(), iData.x, iData.y, keySprite,
                                iData.saveId);
                    } else if (iData.type.equals("flashlight")) {
                        com.sam.TERMINAL.entities.EntityFactory.createFlashlight(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(), iData.x, iData.y, flashlightSprite,
                                iData.saveId);
                    } else if (iData.type.equals("battery")) {
                        com.sam.TERMINAL.entities.EntityFactory.createBattery(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(), iData.x, iData.y, batterySprite,
                                iData.saveId);
                    } else if (iData.type.equals("potion")) {
                        com.sam.TERMINAL.entities.EntityFactory.createPotion(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(), iData.x, iData.y, potionSprite,
                                iData.saveId);
                    } else if (iData.type.equals("studID")) {
                        com.sam.TERMINAL.Main game = (com.sam.TERMINAL.Main) Gdx.app.getApplicationListener();
                        com.sam.TERMINAL.entities.EntityFactory.createStudID(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(), iData.x, iData.y, game.getStudIDRegion(),
                                iData.saveId);
                    } else if (iData.type.equals("papers")) {
                        com.sam.TERMINAL.Main game = (com.sam.TERMINAL.Main) Gdx.app.getApplicationListener();
                        com.sam.TERMINAL.entities.EntityFactory.createPapers(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(), iData.x, iData.y, game.getPapersRegion(),
                                iData.saveId);
                    }
                }
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
        if (!loading) {
            // Check if file exist first to avoid crashes
            GameData data = SaveManager.load(fileName);
            if (data != null) {
                this.currentSaveFile = fileName;
                this.loadedData = data;
                this.loading = true;
                this.saving = false;
                if (data.runId != null)
                    this.currentRunId = data.runId;
            } else {
                System.out.println("Cannot load: " + fileName + " does not exist");
            }
        }
    }

    public void forceImmediateLoad(String fileName) {
        triggerManualLoad(fileName);
        if (loading) {
            super.update(0f); // Processes entities synchronously right now

            // Execute the dynamic spawning logic immediately
            spawnLoadedEntities();

            System.out.println("Force loaded from: " + currentSaveFile);
            loading = false;
            loadedData = null;
        }
    }

    /**
     * Mirrors {@link #forceImmediateLoad(String)} for the save path.
     * Builds a snapshot, processes all persistence entities synchronously,
     * and writes the result to disk — all within this single call.
     * Use this instead of {@link #triggerManualSave(String)} when the
     * save MUST be committed before any subsequent load could fire
     * (e.g. level-start checkpoints).
     */
    public void forceImmediateSave(String fileName) {
        this.currentSaveFile = fileName;
        this.saving = true;
        this.loading = false;
        this.pendingSaveData = new GameData();
        this.pendingSaveData.runId = this.currentRunId;

        super.update(0f); // Processes all persistence entities synchronously

        SaveManager.save(pendingSaveData, currentSaveFile);
        System.out.println("Force saved to: " + currentSaveFile);

        saving = false;
        pendingSaveData = null;
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

                    BatteryComponent pBat = entity.getComponent(BatteryComponent.class);
                    if (pBat != null) {
                        pendingSaveData.playerBattery = pBat.battery;
                    }

                    // Save total beep cards spawned from global context
                    pendingSaveData.totalBeepCardsSpawned = com.sam.TERMINAL.entities.EntitySpawner.totalBeepCardsSpawned;

                    InventoryComponent pInventory = inventoryMapper.get(entity);
                    if (pInventory != null) {
                        pendingSaveData.inventoryItems.clear();
                        pendingSaveData.inventoryItems.addAll(pInventory.items);
                    }
                    break;

                case "INTERACTABLE":
                    InteractableComponent interact = interactMapper.get(entity);
                    TransformComponent iTrans = transformMapper.get(entity);
                    if (interact != null && iTrans != null) {
                        pendingSaveData.interactableStates.put(persistence.saveId, interact.isActive);
                        pendingSaveData.items.add(new GameData.ItemSaveData(iTrans.pos.x, iTrans.pos.y, interact.type,
                                persistence.saveId, interact.isActive));
                    }
                    break;

                case "ENEMY":
                    TransformComponent eTrans = transformMapper.get(entity);
                    if (eTrans != null) {
                        pendingSaveData.enemies.add(new GameData.EnemySaveData(eTrans.pos.x, eTrans.pos.y));
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

                    BatteryComponent pBatLoad = entity.getComponent(BatteryComponent.class);
                    if (pBatLoad != null) {
                        pBatLoad.battery = loadedData.playerBattery;
                    }

                    com.sam.TERMINAL.entities.EntitySpawner.totalBeepCardsSpawned = loadedData.totalBeepCardsSpawned;

                    InventoryComponent pInventoryLoad = inventoryMapper.get(entity);
                    if (pInventoryLoad != null && loadedData.inventoryItems != null) {
                        pInventoryLoad.items.clear();
                        pInventoryLoad.items.addAll(loadedData.inventoryItems);
                        System.out.println("Inventory Loaded: " + pInventoryLoad.items.size() + " item/s.");
                    }

                    break;

                case "INTERACTABLE":
                    if (loadedData.interactableStates.containsKey(persistence.saveId)) {
                        boolean shouldBeActive = loadedData.interactableStates.get(persistence.saveId);

                        InteractableComponent interactLoad = interactMapper.get(entity);
                        if (interactLoad != null) {
                            interactLoad.isActive = shouldBeActive;

                            // If Item was taken
                            if (!shouldBeActive) {
                                entity.remove((SpriteComponent.class));
                            }

                            // If item was not picked, restore its sprite
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
                                        restoredSprite.drawWidth = 40;
                                        restoredSprite.drawHeight = 30;

                                        // Also reset the TransformComponent so collision bounds
                                        // reflect the correct world-unit size, not pooled leftovers.
                                        TransformComponent beepTransform = transformMapper.get(entity);
                                        if (beepTransform != null) {
                                            beepTransform.width = 40;
                                            beepTransform.height = 30;
                                            beepTransform.updateBounds();
                                        }

                                    } else if (interactLoad.type.equals("flashlight")) {
                                        // Canonical flashlight dimensions from EntityFactory.createFlashlight()
                                        restoredSprite.staticSprite = flashlightSprite;
                                        restoredSprite.drawWidth = 50;
                                        restoredSprite.drawHeight = 50;

                                        // Same guard on the TransformComponent.
                                        TransformComponent flTransform = transformMapper.get(entity);
                                        if (flTransform != null) {
                                            flTransform.width = 50;
                                            flTransform.height = 50;
                                            flTransform.updateBounds();
                                        }
                                    } else if (interactLoad.type.equals("battery")) {
                                        restoredSprite.staticSprite = batterySprite;
                                        restoredSprite.drawWidth = 70;
                                        restoredSprite.drawHeight = 70;

                                        TransformComponent batTransform = transformMapper.get(entity);
                                        if (batTransform != null) {
                                            batTransform.width = 70;
                                            batTransform.height = 70;
                                            batTransform.updateBounds();
                                        }
                                    } else if (interactLoad.type.equals("potion")) {
                                        restoredSprite.staticSprite = potionSprite;
                                        restoredSprite.drawWidth = 50;
                                        restoredSprite.drawHeight = 50;

                                        TransformComponent potTransform = transformMapper.get(entity);
                                        if (potTransform != null) {
                                            potTransform.width = 50;
                                            potTransform.height = 50;
                                            potTransform.updateBounds();
                                        }
                                    } else if (interactLoad.type.equals("studID")) {
                                        com.sam.TERMINAL.Main game = (com.sam.TERMINAL.Main) Gdx.app.getApplicationListener();
                                        restoredSprite.staticSprite = game.getStudIDRegion();
                                        restoredSprite.drawWidth = 40;
                                        restoredSprite.drawHeight = 30;

                                        TransformComponent sidTransform = transformMapper.get(entity);
                                        if (sidTransform != null) {
                                            sidTransform.width = 40;
                                            sidTransform.height = 30;
                                            sidTransform.updateBounds();
                                        }
                                    } else if (interactLoad.type.equals("papers")) {
                                        com.sam.TERMINAL.Main game = (com.sam.TERMINAL.Main) Gdx.app.getApplicationListener();
                                        restoredSprite.staticSprite = game.getPapersRegion();
                                        restoredSprite.drawWidth = 40;
                                        restoredSprite.drawHeight = 40;

                                        TransformComponent papTransform = transformMapper.get(entity);
                                        if (papTransform != null) {
                                            papTransform.width = 40;
                                            papTransform.height = 40;
                                            papTransform.updateBounds();
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
