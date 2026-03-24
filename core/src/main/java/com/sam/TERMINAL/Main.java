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
    private enum FlowState { OPENING, TITLE, TUTORIAL, GAME }

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

    // Asset References
    private Texture playerSpriteSheet, cursorTexture, enemyTexture;
    private Texture beepTexture, doorOpenTexture, doorClosedTexture, flashlightTexture;

    // Regions and Animation
    private TextureRegion beepRegion, doorOpenRegion, doorCloseRegion, enemyRegion, flashlightRegion;
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
        // Never dispose the tutorial Stage or swap game state from inside Scene2D input;
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

        doorOpenTexture = new Texture(Gdx.files.internal("environments/opendoor.png"));
        doorOpenRegion = new TextureRegion(doorOpenTexture);

        doorClosedTexture = new Texture(Gdx.files.internal("environments/closedoor.png"));
        doorCloseRegion = new TextureRegion(doorClosedTexture);

        flashlightTexture = new Texture(Gdx.files.internal("sprites/flash_off.png"));
        flashlightRegion = new TextureRegion(flashlightTexture);

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
        if (!Gdx.files.internal("music/TSMusic.wav").exists()) return;
        try {
            titleMusic = Gdx.audio.newMusic(Gdx.files.internal("music/TSMusic.wav"));
            titleMusic.setLooping(true);
            titleMusic.setVolume(0.6f);
        } catch (com.badlogic.gdx.utils.GdxRuntimeException e) {
            Gdx.app.log("Main", "Could not load TSMusic.wav: " + e.getMessage());
        }
    }

    private void updateTitleMusic(float delta) {
        if (titleMusic == null || titleMusicStarted) return;
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
        if (!Gdx.files.internal("music/secBG.wav").exists()) return;
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
                if (menuScreen != null) menuScreen.showJumpscare();
            });
        }));

        engine.addSystem(new WinLossSystem(this));
        engine.addSystem(new AnimationSystem());
        engine.addSystem(new CameraFollowSystem(camera));
        engine.addSystem(new SaveSystem(doorOpenRegion, doorCloseRegion, beepRegion));
        engine.addSystem(new RenderSystem(batch, camera));
        engine.addSystem(new InteractionSystem(doorOpenRegion));

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
            EntitySpawner.spawnForLoad(engine, mainSave, beepRegion, doorCloseRegion, walkAnimation, idleAnimation, enemyRegion, flashlightRegion);
            engine.getSystem(SaveSystem.class).triggerManualLoad(MAIN_SAVE_FILE);

            if (!snapshotIsValid) {
                engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            }
            Gdx.app.log("TERMINAL", "Save file loaded");
        } else {
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);
            engine.getSystem(SaveSystem.class).generateNewRunId();
            EntitySpawner.spawnInitialEntities(engine, beepRegion, doorCloseRegion, walkAnimation, idleAnimation, enemyRegion, flashlightRegion);
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

        com.badlogic.ashley.utils.ImmutableArray<Entity> doors = engine
            .getEntitiesFor(Family.all(InteractableComponent.class).get());
        for (Entity door : doors) {
            door.getComponent(InteractableComponent.class).isActive = true;
        }

        engine.getSystem(SaveSystem.class).triggerManualLoad(TEMP_SAVE_FILE);

        com.badlogic.ashley.utils.ImmutableArray<Entity> enemies = engine
            .getEntitiesFor(Family.all(EnemyComponent.class).get());
        for (Entity enemy : enemies) {
            TransformComponent t = enemy.getComponent(TransformComponent.class);
            t.pos.set(5 * 32f, 40 * 32f);
            t.updateBounds();
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

        if (flowState == FlowState.OPENING) {
            updateTitleMusic(delta);
            if (openingScene != null) openingScene.render(delta);
            drawCursor();
            return;
        }

        if (flowState == FlowState.TITLE) {
            updateTitleMusic(delta);
            if (titleScreen != null) titleScreen.render(delta);
            drawCursor();
            return;
        }

        if (flowState == FlowState.TUTORIAL) {
            if (tutorialScene != null) tutorialScene.render(delta);
            drawCursor();
            return;
        }

        // Draw map
        if (mapManager != null) {
            mapManager.render(camera);
        }

        // Update viewport and camera
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Update engine — freeze during settings, game over, or jumpscare
        batch.begin();
        if (!menuScreen.isSettingsVisible() && !menuScreen.isGameOver() && !menuScreen.isJumpscaring()) {
            engine.update(delta);
        } else {
            engine.update(0);
        }
        batch.end();

        // 3. Lighting overlay (outside SpriteBatch, after sprites)
        if (lightingSystem != null) {
            lightingSystem.render();
        }

        // --- DEBUG POLLING ---
        if (debugManager != null) {
            debugManager.update(lightingSystem);
        }

        // Draw UI on top
        menuScreen.render(delta);

        // Check win/loss — guarded so jumpscare isn't interrupted
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
        if (openingScene != null) openingScene.resize(width, height);
        if (titleScreen != null) titleScreen.resize(width, height);
        if (tutorialScene != null) tutorialScene.resize(width, height);
        if (menuScreen != null) menuScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (mapManager != null) mapManager.dispose();
        if (lightingSystem != null) lightingSystem.dispose();
        if (openingScene != null) openingScene.dispose();
        if (titleScreen != null) titleScreen.dispose();
        if (tutorialScene != null) tutorialScene.dispose();
        if (menuScreen != null) menuScreen.dispose();
        if (titleMusic != null) titleMusic.dispose();
        stopTutorialMusic();
        if (playerSpriteSheet != null) playerSpriteSheet.dispose();
        if (cursorTexture != null) cursorTexture.dispose();
        if (beepTexture != null) beepTexture.dispose();
        if (doorOpenTexture != null) doorOpenTexture.dispose();
        if (doorClosedTexture != null) doorClosedTexture.dispose();
        if (enemyTexture != null) enemyTexture.dispose();
        if (flashlightTexture != null) flashlightTexture.dispose();
    }

    public TextureRegion getBeepRegion() { return beepRegion; }
    public TextureRegion getFlashlightRegion() { return flashlightRegion; }
}

