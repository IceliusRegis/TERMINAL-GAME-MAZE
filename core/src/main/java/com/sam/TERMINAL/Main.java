package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
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
import com.sam.TERMINAL.entities.MapManager;
import com.sam.TERMINAL.persistence.GameData;
import com.sam.TERMINAL.persistence.SaveManager;
import com.sam.TERMINAL.screen.TitleScreen;
import com.sam.TERMINAL.systems.*;

/**
 * Main - The entry point and central manager for TERMINAL.
 * Responsibilities:
 * - Lifecycle Management: Handles creation, rendering loop, resizing, and
 * disposal.
 * - Resource Management: Loads and holds heavy assets (Textures) to prevent
 * memory leaks.
 * - System Initialization: Sets up the ECS Engine, Camera, and Game Systems.
 * - State Orchestration: Decides whether to load a save file or start a new
 * game.
 */
public class Main extends ApplicationAdapter {

    // Ashley ECS engine - manages all entities, components, rendering tools and
    // systems
    private PooledEngine engine;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private MenuScreen menuScreen;
    private TitleScreen titleScreen;
    private boolean showingTitleScreen = true;
    private MapManager mapManager;
    private LightingSystem lightingSystem;
    private DebugManager debugManager;

    // Asset References (Fields for Reset Logic and Disposal)
    private Texture playerSpriteSheet, cursorTexture, enemyTexture;
    private Texture beepTexture, doorOpenTexture, doorClosedTexture, flashlightTexture;

    // Regions and Animation (Shared)
    private TextureRegion beepRegion, doorOpenRegion, doorCloseRegion, enemyRegion, flashlightRegion;
    private Animation<TextureRegion> walkAnimation, idleAnimation;

    // Save Files
    private static final String TEMP_SAVE_FILE = "temp_initial_state.json";
    private static final String MAIN_SAVE_FILE = "saveFile.json";

    @Override
    public void create() {
        // 1.) Load Core Tools
        initEngine();

        // 2.) Load assets
        loadAssets();

        // 3.) Show title screen; game systems start when user picks New Game / Continue
        boolean hasSave = SaveManager.load(MAIN_SAVE_FILE) != null;
        titleScreen = new TitleScreen(batch, hasSave, this::onTitleScreenChoice);
    }

    // Called by TitleScreen when user picks New Game (loadExisting=false) or
    // Continue (loadExisting=true)
    private void onTitleScreenChoice(boolean loadExisting) {
        if (!loadExisting) {
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);
        }

        // UI must be created first so menuScreen can be injected into systems
        createUI();
        initSystems();

        // 4.) Game start (New vs Load)
        handleGameStart();

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
        // INTERACTIVES

        // Beep
        beepTexture = new Texture(Gdx.files.internal("sprites/beep.png"));
        beepRegion = new TextureRegion(beepTexture);

        // Doors
        doorOpenTexture = new Texture(Gdx.files.internal("environments/opendoor.png"));
        doorOpenRegion = new TextureRegion(doorOpenTexture);

        doorClosedTexture = new Texture(Gdx.files.internal("environments/closedoor.png"));
        doorCloseRegion = new TextureRegion(doorClosedTexture);

        // Flashlight
        flashlightTexture = new Texture(Gdx.files.internal("sprites/flash_off.png"));
        flashlightRegion = new TextureRegion(flashlightTexture);

        // UI
        cursorTexture = new Texture(Gdx.files.internal("ui/cursor.png"));

        // PLAYER SPRITES
        playerSpriteSheet = new Texture("sprites/MC (Walk).png");
        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 128, 250);
        walkAnimation = new Animation<>(0.1f, frames[0]);

        Texture idleSheet = new Texture("sprites/MC (Idle).png");
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 128, 250);
        idleAnimation = new Animation<>(0.3f, idleFrames[0]);

        // ENEMY
        enemyTexture = new Texture(Gdx.files.internal("sprites/enemy.png"));
        enemyRegion = new TextureRegion(enemyTexture);
    }

    private void initSystems() {
        // MovementSystem receives menuScreen for pause/settings awareness
        MovementSystem moveSystem = new MovementSystem();
        if (menuScreen != null) {
            moveSystem.setMenuScreen(menuScreen);
        }
        engine.addSystem(moveSystem);

        // EnemySystem fires jumpscare callback on catch
        engine.addSystem(new EnemySystem(() -> {
            Gdx.app.postRunnable(() -> {
                if (menuScreen != null)
                    menuScreen.showJumpscare();
            });
        }));

        engine.addSystem(new WinLossSystem(this));
        engine.addSystem(new AnimationSystem());
        engine.addSystem(new CameraFollowSystem(camera));
        engine.addSystem(new SaveSystem(doorOpenRegion, doorCloseRegion, beepRegion));
        engine.addSystem(new RenderSystem(batch, camera));
        engine.addSystem(new InteractionSystem(doorOpenRegion, batch));

        // Lighting system — stored in field so we can call render() and dispose()
        lightingSystem = new LightingSystem(camera);
        if (menuScreen != null) {
            lightingSystem.setMenuScreen(menuScreen);
        }
        engine.addSystem(lightingSystem);

        debugManager = new DebugManager();
    }

    private void createUI() {
        menuScreen = new MenuScreen(batch, engine, this);
    }

    // GAME STATE LOGIC
    private void handleGameStart() {
        GameData mainSave = SaveManager.load(MAIN_SAVE_FILE);
        GameData tempSave = SaveManager.load(TEMP_SAVE_FILE);

        mapManager = new MapManager(engine);
        mapManager.loadMap("maps/mapTest.tmx");

        // LOAD GAME
        if (mainSave != null) {
            // Checks ID of Temp and Main Save File
            boolean snapshotIsValid = false;
            if (tempSave != null && mainSave.runId != null && tempSave.runId.equals(mainSave.runId)) {
                snapshotIsValid = true;
                Gdx.app.log("TERMINAL", "Snapshot verified. Reset enabled.");
            } else {
                Gdx.app.log("TERMINAL", "Snapshot missing or ID mismatch. Creating new safety snapshot.");
            }

            if (mainSave.runId != null) {
                engine.getSystem(SaveSystem.class).setRunID(mainSave.runId);
            }
            EntitySpawner.spawnForLoad(engine, mainSave, beepRegion, doorCloseRegion, walkAnimation, idleAnimation,
                    enemyRegion, flashlightRegion);
            engine.getSystem(SaveSystem.class).triggerManualLoad(MAIN_SAVE_FILE);

            if (!snapshotIsValid) {
                engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            }
            Gdx.app.log("TERMINAL", "Save file loaded");
        } else {
            // NEW GAME

            // Delete old saves
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);

            // Make new ID
            engine.getSystem(SaveSystem.class).generateNewRunId();

            // EntitySpawner spawns initial entities
            EntitySpawner.spawnInitialEntities(engine, beepRegion, doorCloseRegion, walkAnimation, idleAnimation,
                    enemyRegion, flashlightRegion);

            // Initial Save
            engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            Gdx.app.log("TERMINAL", "New Instance Started");
        }

        // Attach the player's ConeLight after all entities have been spawned
        ImmutableArray<Entity> players = engine.getEntitiesFor(
                Family.all(PlayerComponent.class).get());
        if (players.size() > 0) {
            lightingSystem.createPlayerLight(players.first(), false);
        }
    }

    public void resetGame() {
        Gdx.app.log("TERMINAL", "Resetting Game to Initial Save...");

        // Lock all doors before reset
        ImmutableArray<Entity> doors = engine.getEntitiesFor(Family.all(InteractableComponent.class).get());
        for (Entity door : doors) {
            door.getComponent(InteractableComponent.class).isActive = true;
        }

        engine.getSystem(SaveSystem.class).triggerManualLoad(TEMP_SAVE_FILE);

        // Hard reset enemy to avoid spawn killing
        ImmutableArray<Entity> enemies = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
        for (Entity enemy : enemies) {
            TransformComponent t = enemy.getComponent(TransformComponent.class);
            t.pos.set(5 * 32f, 40 * 32f);
            t.updateBounds();

            // Clear stale BFS path after teleporting enemy back to spawn
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec != null) {
                ec.path.clear();
                ec.pathTimer = 0f;
            }
        }

        // Reset EnemySystem triggered flag so it can fire again
        engine.getSystem(EnemySystem.class).reset();

        WinLossSystem wls = engine.getSystem(WinLossSystem.class);
        wls.gameOver = false;
        wls.win = false;

        menuScreen.resetUI();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1);

        if (showingTitleScreen && titleScreen != null) {
            titleScreen.render(delta);
            drawCursor();
            return;
        }

        // Map layer (behind sprites)
        if (mapManager != null) {
            mapManager.renderMap(camera);
        }

        // Draw the Game World (ECS entities)
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Freeze engine during settings, game over, or jumpscare
        if (!menuScreen.isSettingsVisible() && !menuScreen.isGameOver() && !menuScreen.isJumpscaring()) {
            engine.update(delta);
        } else {
            engine.update(0);
        }

        batch.end();

        // Debug BFS path lines (between sprites and lighting)
        EnemySystem enemySystem = engine.getSystem(EnemySystem.class);
        if (enemySystem != null) {
            enemySystem.renderDebug(camera);
        }

        // Lighting overlay (outside SpriteBatch, after sprites)
        if (lightingSystem != null) {
            lightingSystem.render();
        }

        // Debug polling
        if (debugManager != null) {
            debugManager.update(lightingSystem);
            debugManager.renderHitboxes(engine, camera);
        }

        // Draw UI on top
        menuScreen.render(delta);

        // Win/loss check — guarded so jumpscare isn't interrupted
        WinLossSystem wls = engine.getSystem(WinLossSystem.class);
        if (wls.win && !menuScreen.isGameOver() && !menuScreen.isJumpscaring()) {
            menuScreen.showGameOver(true);
        } else if (wls.gameOver && !menuScreen.isGameOver() && !menuScreen.isJumpscaring()) {
            menuScreen.showGameOver(false);
        }

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
        if (titleScreen != null)
            titleScreen.resize(width, height);
        if (menuScreen != null)
            menuScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        InteractionSystem interactionSystem = engine.getSystem(InteractionSystem.class);
        if (interactionSystem != null)
            interactionSystem.dispose();
        batch.dispose();
        if (mapManager != null)
            mapManager.dispose();
        if (lightingSystem != null)
            lightingSystem.dispose();
        if (debugManager != null)
            debugManager.dispose();
        if (titleScreen != null)
            titleScreen.dispose();
        if (menuScreen != null)
            menuScreen.dispose();
        if (playerSpriteSheet != null)
            playerSpriteSheet.dispose();
        if (cursorTexture != null)
            cursorTexture.dispose();
        if (beepTexture != null)
            beepTexture.dispose();
        if (doorOpenTexture != null)
            doorOpenTexture.dispose();
        if (doorClosedTexture != null)
            doorClosedTexture.dispose();
        if (enemyTexture != null)
            enemyTexture.dispose();
        if (flashlightTexture != null)
            flashlightTexture.dispose();
    }

    // HELPER FUNCTIONS
    public TextureRegion getBeepRegion() {
        return beepRegion;
    }

    public TextureRegion getFlashlightRegion() {
        return flashlightRegion;
    }
}