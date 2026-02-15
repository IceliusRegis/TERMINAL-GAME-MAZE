package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.persistence.GameData;
import com.sam.TERMINAL.persistence.SaveManager;
import com.sam.TERMINAL.components.PersistenceComponent;
import com.sam.TERMINAL.components.TransformComponent;
import com.sam.TERMINAL.components.TileWorldComponent;

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
    private ComponentMapper<TileWorldComponent> tileMapper;

    //Save State
    private GameData pendingSaveData;
    private boolean saving = false;

    //Load State
    private GameData loadedData; // reads data from disk
    private boolean loading = false; //tells the game we are loading data

    public SaveSystem() {

        super(Family.all(PersistenceComponent.class).get());

        //Initialize Mappers
        persistenceMapper = ComponentMapper.getFor(PersistenceComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        tileMapper = ComponentMapper.getFor(TileWorldComponent.class);
    }

    @Override
    public void update(float deltaTime) {

        //Save Button Trigger
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            startSave();
        }

        //Load Button Trigger
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            startLoad();
        }

        if (saving || loading) {
            // MAGIC LINE: This tells Ashley to find all matching entities and run
            // processEntity() on them right now.
            super.update(deltaTime);

            if (saving) {
                //Finished collecting data, now write it to disk.
                SaveManager.save(pendingSaveData);
                System.out.println("Game Saved");

                //Reset the system so it stops saving
                saving = false;
                pendingSaveData = null;

            }

            if (loading) {
                System.out.println("Game Loaded!");
                loading = false;
                loadedData = null;
            }


        }
    }

    private void startSave() {
        saving = true;
        loading = false;
        pendingSaveData = new GameData(); // Create a clean, empty SAve File
    }

    public void triggerManualSave() {
        if (!saving) { // Prevent double-saving if already in progress
            startSave();
        }
    }

    private void startLoad() {
        GameData data = SaveManager.load();

        if (data != null) {
            loadedData = data;
            loading = true;
            saving = false;
        } else {
            System.out.println("No save file found :(");
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
                    break;

                case "MAP":
                    TileWorldComponent tileWorld = tileMapper.get(entity);
                    if (tileWorld != null) {
                        pendingSaveData.map = tileWorld.map;
                    } else {
                        // Optional: Warn us in the console so we know something is wrong
                        System.out.println("WARNING: Found a MAP entity with no TileWorldComponent!");
                    }
                    break;
            }
        }

        // Loading (Data to Entity)
        else if (loading) {
            switch (persistence.type) {

                case "PLAYER":
                    TransformComponent pTrans = transformMapper.get(entity);
                    pTrans.pos.x = loadedData.playerX;
                    pTrans.pos.y = loadedData.playerY;
                    pTrans.updateBounds();
                    break;

                case "MAP":
                    TileWorldComponent tileWorld = tileMapper.get(entity);
                    if (loadedData.map != null) {
                        tileWorld.map = loadedData.map; // Restore the saved layout
                    }
                    break;
            }
        }
    }
}

