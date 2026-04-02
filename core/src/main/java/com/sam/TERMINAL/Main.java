package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
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
import com.sam.TERMINAL.screen.OpeningScene;
import com.sam.TERMINAL.screen.TutorialScene;
import com.sam.TERMINAL.screen.TitleScreen;
import com.sam.TERMINAL.systems.*;

public class Main extends ApplicationAdapter {
    private enum FlowState {
        OPENING, TITLE, TUTORIAL, GAME
    }

    private PooledEngine engine;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private MenuScreen menuScreen;
    private OpeningScene openingScene;
    private TitleScreen titleScreen;
    private TutorialScene tutorialScene;
    private FlowState flowState = FlowState.OPENING;
    private MapManager mapManager;
    private LightingSystem lightingSystem;
    private DebugManager debugManager;
    private Music titleMusic;
    private Music tutorialMusic;
    private float titleMusicDelayTimer;
    private boolean titleMusicStarted;
    private static final float CURSOR_HIDE_DELAY_SECONDS = 5f;
    private float cursorIdleTimer = CURSOR_HIDE_DELAY_SECONDS;
    private int lastPointerX = Integer.MIN_VALUE;
    private int lastPointerY = Integer.MIN_VALUE;
    private boolean cursorVisible;

    // Asset References
    private Texture playerSpriteSheet, cursorTexture, enemyTexture;
    private Texture beepTexture, flashlightTexture, batteryTexture;

    // Regions and Animation
    private TextureRegion beepRegion, enemyRegion, flashlightRegion, batteryRegion;
    private Animation<TextureRegion> walkAnimation, idleAnimation;

    // Save Files
    private static final String TEMP_SAVE_FILE = "temp_initial_state.json";
    private static final String MAIN_SAVE_FILE = "saveFile.json";

    @Override
    public void create() {
        initEngine();
        loadAssets();
        loadTitleMusic();
        openingScene = new OpeningScene(batch, this::onOpeningComplete);
        flowState = FlowState.OPENING;
        lastPointerX = Gdx.input.getX();
        lastPointerY = Gdx.input.getY();
        cursorVisible = false;
    }

    private void onOpeningComplete() {
        if (openingScene != null) {
            openingScene.dispose();
            openingScene = null;
        }
        boolean hasSave = SaveManager.load(MAIN_SAVE_FILE) != null;
        titleScreen = new TitleScreen(batch, hasSave, this::onTitleScreenChoice);
        flowState = FlowState.TITLE;
    }

    private void onTitleScreenChoice(boolean loadExisting) {
        if (titleScreen != null) {
            titleScreen.dispose();
            titleScreen = null;
        }

        if (loadExisting) {
            startGameProper();
        } else {
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);
            startTutorial();
        }

    }

    private void startTutorial() {
        stopTitleMusic();
        playTutorialMusic();
        tutorialScene = new TutorialScene(batch, this::onTutorialComplete);
        flowState = FlowState.TUTORIAL;
    }

    private void onTutorialComplete() {
        // Never dispose the tutorial Stage or swap game state from inside Scene2D
        // input;
        // that re-enters Stage and can crash. Run after the frame/input stack unwinds.
        Gdx.app.postRunnable(() -> {
            if (tutorialScene != null) {
                tutorialScene.dispose();
                tutorialScene = null;
            }
            startGameProper();
        });
    }

    private void startGameProper() {
        stopTitleMusic();
        createUI();
        initSystems();
        handleGameStart();
        flowState = FlowState.GAME;
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
        beepTexture = new Texture(Gdx.files.internal("sprites/beep.png"));
        beepRegion = new TextureRegion(beepTexture);

        flashlightTexture = new Texture(Gdx.files.internal("sprites/flash_off.png"));
        flashlightRegion = new TextureRegion(flashlightTexture);

        batteryTexture = new Texture(Gdx.files.internal("sprites/battery.png"));
        batteryRegion = new TextureRegion(batteryTexture);

        cursorTexture = new Texture(Gdx.files.internal("ui/cursor.png"));

        playerSpriteSheet = new Texture("sprites/MC (Walk).png");
        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 128, 250);
        walkAnimation = new Animation<>(0.1f, frames[0]);

        Texture idleSheet = new Texture("sprites/MC (Idle).png");
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 128, 250);
        idleAnimation = new Animation<>(0.3f, idleFrames[0]);

        enemyTexture = new Texture(Gdx.files.internal("sprites/enemy.png"));
        enemyRegion = new TextureRegion(enemyTexture);
    }

    private void loadTitleMusic() {
        if (!Gdx.files.internal("music/TSMusic.wav").exists())
            return;
        try {
            titleMusic = Gdx.audio.newMusic(Gdx.files.internal("music/TSMusic.wav"));
            titleMusic.setLooping(true);
            titleMusic.setVolume(0.6f);
        } catch (com.badlogic.gdx.utils.GdxRuntimeException e) {
            Gdx.app.log("Main", "Could not load TSMusic.wav: " + e.getMessage());
        }
    }

    private void updateTitleMusic(float delta) {
        if (titleMusic == null || titleMusicStarted)
            return;
        titleMusicDelayTimer += delta;
        if (titleMusicDelayTimer >= 2f) {
            titleMusic.play();
            titleMusicStarted = true;
        }
    }

    private void stopTitleMusic() {
        if (titleMusic != null) {
            titleMusic.stop();
        }
    }

    private void playTutorialMusic() {
        if (!Gdx.files.internal("music/secBG.wav").exists())
            return;
        try {
            tutorialMusic = Gdx.audio.newMusic(Gdx.files.internal("music/secBG.wav"));
            tutorialMusic.setLooping(true);
            tutorialMusic.setVolume(0.65f);
            tutorialMusic.play();
        } catch (com.badlogic.gdx.utils.GdxRuntimeException e) {
            Gdx.app.log("Main", "Could not load secBG.wav: " + e.getMessage());
        }
    }

    private void stopTutorialMusic() {
        if (tutorialMusic != null) {
            tutorialMusic.stop();
            tutorialMusic.dispose();
            tutorialMusic = null;
        }
    }

    private void initSystems() {
        // --- MOVEMENT SYSTEM LINKING ---
        MovementSystem moveSystem = new MovementSystem();
        if (menuScreen != null) {
            moveSystem.setMenuScreen(menuScreen);
        }
        engine.addSystem(moveSystem);
        // -------------------------------

        engine.addSystem(new EnemySystem(() -> {
            Gdx.app.postRunnable(() -> {
                if (menuScreen != null)
                    menuScreen.showJumpscare();
            });
        }));

        WinLossSystem winLossSystem = new WinLossSystem(this, batch);
        if (menuScreen != null) {
            winLossSystem.setMenuScreen(menuScreen);
        }
        engine.addSystem(winLossSystem);
        engine.addSystem(new AnimationSystem());
        engine.addSystem(new CameraFollowSystem(camera));
        engine.addSystem(new SaveSystem(beepRegion, flashlightRegion, enemyRegion, batteryRegion));
        engine.addSystem(new RenderSystem(batch, camera));
        engine.addSystem(new InteractionSystem(batch));

        lightingSystem = new LightingSystem(camera);
        if (menuScreen != null) {
            lightingSystem.setMenuScreen(menuScreen); // Add this line!
        }
        engine.addSystem(lightingSystem);

        debugManager = new DebugManager();
    }

    private void createUI() {
        menuScreen = new MenuScreen(batch, engine, this);
    }

    private void handleGameStart() {
        GameData mainSave = SaveManager.load(MAIN_SAVE_FILE);
        GameData tempSave = SaveManager.load(TEMP_SAVE_FILE);

        mapManager = new MapManager(engine);
        mapManager.loadMap("maps/mapTest.tmx");

        if (mainSave != null) {
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
            EntitySpawner.spawnForLoad(engine, mainSave, beepRegion, walkAnimation, idleAnimation,
                    enemyRegion, flashlightRegion, batteryRegion);
            engine.getSystem(SaveSystem.class).triggerManualLoad(MAIN_SAVE_FILE);

            if (!snapshotIsValid) {
                engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            }
            Gdx.app.log("TERMINAL", "Save file loaded");
        } else {
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);
            engine.getSystem(SaveSystem.class).generateNewRunId();
            EntitySpawner.spawnInitialEntities(engine, beepRegion, walkAnimation, idleAnimation,
                    enemyRegion, flashlightRegion, batteryRegion);
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
        Gdx.app.log("TERMINAL", "Resetting game to initial state...");

        // 1. Reset the WinLossSystem flags first so update() runs normally again.
        WinLossSystem wls = engine.getSystem(WinLossSystem.class);
        if (wls != null) {
            wls.reset();
        }

        // 2. Reset the EnemySystem triggered flag so the jumpscare can fire again.
        EnemySystem enemySys = engine.getSystem(EnemySystem.class);
        if (enemySys != null) {
            enemySys.reset();
        }

        // 3. Remove all enemies from the old run
        ImmutableArray<Entity> enemies = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
        com.badlogic.gdx.utils.Array<Entity> toRemoveEnemies = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : enemies)
            toRemoveEnemies.add(e);
        for (Entity e : toRemoveEnemies)
            engine.removeEntity(e);

        // 4. First, physically remove the old items so we can re-generate a new random
        // count
        ImmutableArray<Entity> currentItems = engine.getEntitiesFor(Family.all(InteractableComponent.class).get());
        com.badlogic.gdx.utils.Array<Entity> toRemove = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : currentItems)
            toRemove.add(e);
        for (Entity e : toRemove)
            engine.removeEntity(e);

        // 5. Load the temp save — this restores player position, inventory, and resets
        // battery component context
        // (It won't affect items because we just removed them!)
        engine.getSystem(SaveSystem.class).forceImmediateLoad(TEMP_SAVE_FILE);

        // 6. Provide a clean slate for the player's runtime components (sometimes items
        // could erroneously persist in load state if not checked)
        ImmutableArray<Entity> players = engine.getEntitiesFor(Family.all(PlayerComponent.class).get());
        if (players.size() > 0) {
            Entity p = players.first();
            InventoryComponent inv = p.getComponent(InventoryComponent.class);
            if (inv != null)
                inv.items.clear();

            BatteryComponent bat = p.getComponent(BatteryComponent.class);
            if (bat != null) {
                bat.battery = bat.maxBattery;
                bat.flashlightOn = false;
            }
        }

        // 7. Spawn fresh completely randomized items (Beep Cards, Battery, Flashlight)
        // AND spawn a fresh enemy.
        ImmutableArray<Entity> worlds = engine.getEntitiesFor(Family.all(TileWorldComponent.class).get());
        TileWorldComponent world = worlds.size() > 0 ? worlds.first().getComponent(TileWorldComponent.class) : null;
        if (world != null) {
            int pTileX = 15;
            int pTileY = 42;
            if (players.size() > 0) {
                TransformComponent t = players.first().getComponent(TransformComponent.class);
                if (t != null) {
                    pTileX = (int) (t.pos.x / 32f);
                    pTileY = (int) (t.pos.y / 32f);
                }
            }
            EntitySpawner.spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, world, pTileX, pTileY,
                    world.mapWidthTiles, world.mapHeightTiles);

            // Re-spawn the enemy cleanly
            com.sam.TERMINAL.entities.EntityFactory.createEnemy(engine, 5 * 32f, 40 * 32f, enemyRegion);

            // Re-save temp snapshot to cement these new random locations and the new random
            // count
            engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
        }

        // 8. Revert the player's lighting back to the no-flashlight state.
        if (players.size() > 0 && lightingSystem != null) {
            lightingSystem.createPlayerLight(players.first(), false);
        }

        // 9. Restore the HUD to its normal in-game state.
        menuScreen.resetUI();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1);
        updateCursorVisibility(delta);

        if (flowState == FlowState.OPENING) {
            updateTitleMusic(delta);
            if (openingScene != null)
                openingScene.render(delta);
            drawCursor();
            return;
        }

        if (flowState == FlowState.TITLE) {
            updateTitleMusic(delta);
            if (titleScreen != null)
                titleScreen.render(delta);
            drawCursor();
            return;
        }

        if (flowState == FlowState.TUTORIAL) {
            if (tutorialScene != null)
                tutorialScene.render(delta);
            drawCursor();
            return;
        }

        // --- MAP LAYER (behind sprites) ---
        if (mapManager != null) {
            mapManager.render(camera);
        }

        // 2. Draw the Game World (ECS entities)
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Update engine — freeze during settings, game over, or jumpscare
        batch.begin();
        if (!menuScreen.isSettingsVisible() && !menuScreen.isGameOver() && !menuScreen.isJumpscaring()) {
            engine.update(delta);
        } else {
            // When paused, we draw the last known state without moving anything
            // In Ashley, we call update with 0 delta to "freeze" time:
            engine.update(0);
        }
        batch.end();

        // 3. Lighting overlay (outside SpriteBatch, after sprites)
        if (lightingSystem != null) {
            lightingSystem.render();
        }

        // 4. Draw interaction prompts on top of the lighting layer so they
        // are never blacked out by the Box2DLights ambient darkness.
        renderInteractionPrompts();

        // --- DEBUG POLLING & HITBOX RENDERING ---
        if (debugManager != null) {
            debugManager.update(lightingSystem);
            debugManager.renderHitboxes(engine, camera);

            // Draw BFS path lines when debug hitboxes are visible
            if (debugManager.showHitboxes) {
                EnemySystem enemySys = engine.getSystem(EnemySystem.class);
                if (enemySys != null) {
                    enemySys.renderDebug(camera);
                }
            }
        }

        // Draw UI on top
        menuScreen.render(delta);

        // Check win/loss — guarded so jumpscare isn't interrupted.
        // During a jumpscare, showGameOver() is called by the jumpscare's own
        // fade-out action — do NOT call it here or it races the animation.
        WinLossSystem wls = engine.getSystem(WinLossSystem.class);
        if (!menuScreen.isJumpscaring()) {
            if (wls.win && !menuScreen.isGameOver()) {
                menuScreen.showGameOver(true);
            } else if (wls.gameOver && !menuScreen.isGameOver()) {
                menuScreen.showGameOver(false);
            }
        }

        drawCursor();
    }

    /**
     * Draws the "Press E" interaction prompts in world-space AFTER the lighting
     * layer finishes. This guarantees they are always visible on top of the
     * Box2DLights ambient-darkness overlay.
     *
     * InteractionSystem and WinLossSystem expose delegated render methods so
     * that all prompt drawing is consolidated here rather than inside update().
     */
    private void renderInteractionPrompts() {
        // Only draw during active gameplay — not during menus or end-screens.
        if (menuScreen == null || menuScreen.isGameOver()) {
            return;
        }

        // Re-apply world-space projection so prompts sit on the map correctly.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        InteractionSystem interactSys = engine.getSystem(InteractionSystem.class);
        if (interactSys != null) {
            interactSys.renderPrompts();
        }

        WinLossSystem winLossSys = engine.getSystem(WinLossSystem.class);
        if (winLossSys != null) {
            winLossSys.renderPrompt();
        }

        batch.end();
    }

    private void drawCursor() {
        if (!cursorVisible)
            return;
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

    private void updateCursorVisibility(float delta) {
        int currentX = Gdx.input.getX();
        int currentY = Gdx.input.getY();
        boolean pointerMoved = currentX != lastPointerX || currentY != lastPointerY;
        if (pointerMoved) {
            cursorVisible = true; // show cursor immediately when mouse moves
            cursorIdleTimer = 0f;
        } else if (cursorVisible) {
            cursorIdleTimer += delta;
            if (cursorIdleTimer >= CURSOR_HIDE_DELAY_SECONDS) {
                cursorVisible = false; // hide cursor after 5 seconds without movement
            }
        }
        lastPointerX = currentX;
        lastPointerY = currentY;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        if (openingScene != null)
            openingScene.resize(width, height);
        if (titleScreen != null)
            titleScreen.resize(width, height);
        if (tutorialScene != null)
            tutorialScene.resize(width, height);
        if (menuScreen != null)
            menuScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        // Clean up resources to prevent memory leaks
        batch.dispose();
        if (mapManager != null)
            mapManager.dispose();
        if (lightingSystem != null)
            lightingSystem.dispose();
        if (debugManager != null)
            debugManager.dispose();
        if (openingScene != null)
            openingScene.dispose();
        if (titleScreen != null)
            titleScreen.dispose();
        if (tutorialScene != null)
            tutorialScene.dispose();
        if (menuScreen != null)
            menuScreen.dispose();
        if (titleMusic != null)
            titleMusic.dispose();
        stopTutorialMusic();
        if (playerSpriteSheet != null)
            playerSpriteSheet.dispose();
        if (cursorTexture != null)
            cursorTexture.dispose();
        if (beepTexture != null)
            beepTexture.dispose();
        if (enemyTexture != null)
            enemyTexture.dispose();
        if (flashlightTexture != null)
            flashlightTexture.dispose();
        if (batteryTexture != null)
            batteryTexture.dispose();
        WinLossSystem wlsDispose = engine.getSystem(WinLossSystem.class);
        if (wlsDispose != null)
            wlsDispose.dispose();
    }

    public TextureRegion getBeepRegion() {
        return beepRegion;
    }

    public TextureRegion getFlashlightRegion() {
        return flashlightRegion;
    }

    public TextureRegion getBatteryRegion() {
        return batteryRegion;
    }
}
