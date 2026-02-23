package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sam.TERMINAL.buttons.MenuScreen;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.entities.EntitySpawner;
import com.sam.TERMINAL.persistence.GameData;
import com.sam.TERMINAL.persistence.SaveManager;
import com.sam.TERMINAL.systems.*;
import com.sam.TERMINAL.tiles.TileRegistry;
import com.sam.TERMINAL.screen.TitleScreen;

/**
 * Main - The entry point and central manager for TERMINAL.
 * Responsibilities:
 * - Lifecycle Management: Handles creation, rendering loop, resizing, and disposal.
 * - Resource Management: Loads and holds heavy assets (Textures) to prevent memory leaks.
 * - System Initialization: Sets up the ECS Engine, Camera, and Game Systems.
 * - State Orchestration: Decides whether to load a save file or start a new game.
 */

public class Main extends ApplicationAdapter {

    // Ashley ECS engine - manages all entities, components, rendering tools and systems
    private PooledEngine engine;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private MenuScreen menuScreen;
    private TitleScreen titleScreen;
    private boolean showingTitleScreen = true;

    // Asset References (Fields for Reset Logic and Disposal)
    private Texture playerSpriteSheet, cursorTexture;
    private Texture beepTexture, doorOpenTexture, doorClosedTexture, wallTexture, floorTexture;

    // Regions and Animation (Shared)
    private TextureRegion beepRegion, doorOpenRegion, doorCloseRegion;
    private Animation<TextureRegion> walkAnimation, idleAnimation;

    //Save Files
    private static final String TEMP_SAVE_FILE = "temp_initial_state.json";
    private static final String MAIN_SAVE_FILE = "saveFile.json";


    @Override
    public void create() {

        // 1.) Load Core Tools
        initEngine();

        // 2.) Load assets
        loadAssets();

        // 3.) Register Tile Types
        registerTiles();

        // 4.) Show title screen; game systems start when user picks New Game / Continue
        boolean hasSave = SaveManager.load(MAIN_SAVE_FILE) != null;
        titleScreen = new TitleScreen(batch, hasSave, this::onTitleScreenChoice);
    }

    // Called by TitleScreen when user picks New Game (loadExisting=false) or Continue (loadExisting=true)
    private void onTitleScreenChoice(boolean loadExisting) {
        if (!loadExisting) {
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);
        }
        initSystems();
        handleGameStart();
        createUI();
        if (titleScreen != null) {
            titleScreen.dispose();
            titleScreen = null;
        }
        showingTitleScreen = false;
    }

    private void initEngine() {
        engine = new PooledEngine();
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(800, 600, camera);
        viewport.apply();
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
    }

    private void loadAssets() {

        //ENVIRONMENTS
        wallTexture = new Texture(Gdx.files.internal("environments/BackRoomsWall.png"));
        floorTexture = new Texture(Gdx.files.internal("environments/Floor.png"));

        //INTERACTIVES

        //Beep
        beepTexture = new Texture(Gdx.files.internal("sprites/beep.png"));
        beepRegion = new TextureRegion(beepTexture);

        //Temp Door
        doorOpenTexture = new Texture(Gdx.files.internal("environments/opendoor.png"));
        doorOpenRegion = new TextureRegion(doorOpenTexture);

        doorClosedTexture = new Texture(Gdx.files.internal("environments/closedoor.png"));
        doorCloseRegion = new TextureRegion(doorClosedTexture);

        //UI
        cursorTexture = new Texture(Gdx.files.internal("ui/cursor.png"));

        //PLAYER SPRITES
        playerSpriteSheet = new Texture("sprites/Soldier-Walk.png");

        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 100, 100);
        walkAnimation = new Animation<>(0.1f, frames[0]);

        Texture idleSheet = new Texture("sprites/Soldier-Idle.png");
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 100, 100);
        idleAnimation = new Animation<>(0.15f, idleFrames[0]);
    }

    private void registerTiles() {
        TileRegistry.registerTile(1, true, new TextureRegion(wallTexture));
        TileRegistry.registerTile(2, false, new TextureRegion(floorTexture));
    }

    private void initSystems() {
        engine.addSystem(new MovementSystem());
        engine.addSystem(new AnimationSystem());
        engine.addSystem(new CameraFollowSystem(camera));
        engine.addSystem(new SaveSystem(doorOpenRegion, doorCloseRegion, beepRegion));
        engine.addSystem(new TileRenderSystem(batch, camera));
        engine.addSystem(new RenderSystem(batch, camera));
        engine.addSystem(new InteractionSystem(doorOpenRegion));
    }

    private void createUI() {

        menuScreen = new MenuScreen(batch, engine, this);
    }

    //GAME STATE LOGIC
    private void handleGameStart() {
        GameData mainSave = SaveManager.load(MAIN_SAVE_FILE);
        GameData tempSave = SaveManager.load(TEMP_SAVE_FILE);
        TileWorldComponent world =generateNewMap();

        //LOAD GAME
        if (mainSave != null) {

            //Checks ID of Temp and Main Save File
            boolean snapshotIsValid = false;
            if (tempSave != null && mainSave.runId != null && tempSave.runId.equals(mainSave.runId)) {
                snapshotIsValid = true;
                Gdx.app.log("TERMINAL", "Snapshot verified. Reset enabled.");
            } else {
                Gdx.app.log("TERMINAL", "Snapshot missing or ID mismatch. Creating new safety snapshot from CURRENT loaded state.");
                // NOTE: This isn't a "true" reset (it resets to this save, not the start of the game),
                // but it prevents crashes if the player clicks Reset.
            }

            if (mainSave.runId != null) {
                engine.getSystem(SaveSystem.class).setRunID(mainSave.runId);
            }
            EntitySpawner.spawnForLoad(engine, mainSave, beepRegion, doorCloseRegion, walkAnimation, idleAnimation);
            engine.getSystem(SaveSystem.class).triggerManualLoad(MAIN_SAVE_FILE);

            if (!snapshotIsValid) {
                engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            }

            Gdx.app.log("TERMINAL", "Save file loaded");
        } else {
            //NEW GAME

            //Delete old saves
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);

            //Make new ID
            engine.getSystem(SaveSystem.class).generateNewRunId();

            //EntitySpawner now spawns initial stuff
            EntitySpawner.spawnInitialEntities(engine, world, beepRegion, doorCloseRegion, walkAnimation, idleAnimation);

            //Initial Save Mechanic
            engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            Gdx.app.log("TERMINAL", "New Instance Started");
        }
    }

    private Entity createWorldEntity() {
        Entity worldEntity = engine.createEntity();
        worldEntity.add(new TileWorldComponent());
        worldEntity.add(new PersistenceComponent("MAP", "ZA_WARDO"));
        engine.addEntity(worldEntity);
        return worldEntity;
    }

    private TileWorldComponent generateNewMap() {
        Entity worldEntity = createWorldEntity();
        TileWorldComponent tileCom = worldEntity.getComponent(TileWorldComponent.class);
        tileCom.init(50,50);

        for(int x=0; x<50; x++) {
            for(int y=0; y<50; y++) {
                tileCom.map[x][y] = 2;
                if (Math.random() <0.2) tileCom.map[x][y] = 1;
            }
        }
        return tileCom;
    }

    public void resetGame() {
        Gdx.app.log("TERMINAL", "Resetting Game to Initial Save...");
        engine.getSystem(SaveSystem.class).triggerManualLoad(TEMP_SAVE_FILE);

    }

    @Override
    public void render() {
        // 1. Clear the screen
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1);

        if (showingTitleScreen && titleScreen != null) {
            titleScreen.render(delta);
            drawCursor();
            return;
        }

        // 2. Draw the Game World (Walls, Player)
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (!menuScreen.isSettingsVisible()) {
            engine.update(delta);
        } else {
            // When paused, we draw the last known state without moving anything
            // This is done by passing 0 to the engine update or calling specific render systems
            // In Ashley, usually we just stop the update entirely to "freeze" time
            // However, to keep things visible while frozen, we call update with 0 delta:
            engine.update(0);
        }

        batch.end();

        // 4. Draw the Menu (Drawn last so it sits on top of the character)
        menuScreen.render(delta);

        /*WinLossSystem wls = engine.getSystem(WinLossSystem.class);
        if (wls.win && !menuScreen.isGameOver()) {
            menuScreen.showGameOver(true);
        } else if (wls.gameOver && !menuScreen.isGameOver()) {
            menuScreen.showGameOver(false);
        }*/

        drawCursor();
    }

    private void drawCursor() {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float scale = Gdx.graphics.getWidth() / 800f;
        float cursorSize = 48 * scale;
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        batch.begin();
        if (cursorTexture != null) {
            batch.draw(cursorTexture, mouseX, mouseY - cursorSize, cursorSize, cursorSize);
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        if (titleScreen != null) titleScreen.resize(width, height);
        if (menuScreen != null) menuScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        // Clean up resources to prevent memory leaks
        batch.dispose();
        if (titleScreen != null) titleScreen.dispose();
        if (menuScreen != null) menuScreen.dispose();
        if (playerSpriteSheet != null) playerSpriteSheet.dispose();
        if (cursorTexture != null) cursorTexture.dispose();
        if (beepTexture != null) beepTexture.dispose();
        if (doorOpenTexture != null) doorOpenTexture.dispose();
        if (doorClosedTexture != null) doorClosedTexture.dispose();
        if (wallTexture != null) wallTexture.dispose();
        if (floorTexture != null) floorTexture.dispose();
    }
}
