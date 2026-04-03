package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Timer;
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
    private Music triBgmMusic;
    private float titleMusicDelayTimer;
    private boolean titleMusicStarted;
    private static final float CURSOR_HIDE_DELAY_SECONDS = 5f;
    private float cursorIdleTimer = CURSOR_HIDE_DELAY_SECONDS;
    private int lastPointerX = Integer.MIN_VALUE;
    private int lastPointerY = Integer.MIN_VALUE;
    private boolean cursorVisible;
    private boolean startedGameProperFromTutorial = false;
    private boolean disableLightingDuringTutorial = false;
    private boolean timeSignalPlayedForRun = false;
    private boolean tutorialMovementAllowed = true;
    private boolean lilyTriggered = false;

    private Sound timeSignalSound;
    private Sound lilyTriggerSound;

    // Asset References
    private Texture playerSpriteSheet, cursorTexture, enemyTexture;
    private Texture beepTexture, flashlightTexture, batteryTexture, potionTexture; // ADDED potionTexture
    private Texture lilyTexture;

    // Regions and Animation
    private TextureRegion beepRegion, enemyRegion, flashlightRegion, batteryRegion, potionRegion; // ADDED potionRegion
    private TextureRegion lilyRegion;
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
        fadeOutTitleMusic(1.0f);
        playTutorialMusic();
        timeSignalPlayedForRun = false;
        // Start in full-bright mode for the entire tutorial run.
        disableLightingDuringTutorial = true;
        tutorialMovementAllowed = false;
        lilyTriggered = false;
        tutorialScene = new TutorialScene(batch, new TutorialScene.TutorialListener() {
            @Override
            public void onSpawnIntoMaze() {
                if (startedGameProperFromTutorial) return;
                startedGameProperFromTutorial = true;
                // Start the actual game after the current Scene2D frame finishes.
                Gdx.app.postRunnable(Main.this::startGameProper);
            }

            @Override
            public void onTutorialComplete() {
                // Never dispose the tutorial Stage inside Scene2D update/render.
                Gdx.app.postRunnable(() -> {
                    if (tutorialScene != null) {
                        tutorialScene.dispose();
                        tutorialScene = null;
                    }

                    tutorialMovementAllowed = true;

                    if (menuScreen != null) {
                        menuScreen.reapplyInputProcessor();
                    }
                });
            }
        });
        flowState = FlowState.TUTORIAL;
    }

    // onTutorialComplete handled via the TutorialListener in startTutorial()

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

        potionTexture = new Texture(Gdx.files.internal("sprites/sting.png")); // Ensure this file exists!
        potionRegion = new TextureRegion(potionTexture);

        cursorTexture = new Texture(Gdx.files.internal("ui/cursor.png"));

        playerSpriteSheet = new Texture("sprites/MC (Walk).png");
        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 128, 250);
        walkAnimation = new Animation<>(0.1f, frames[0]);

        Texture idleSheet = new Texture("sprites/MC (Idle).png");
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 128, 250);
        idleAnimation = new Animation<>(0.3f, idleFrames[0]);

        enemyTexture = new Texture(Gdx.files.internal("sprites/enemy.png"));
        enemyRegion = new TextureRegion(enemyTexture);

        if (Gdx.files.internal("ui/LilyOUTLINED.png").exists()) {
            lilyTexture = new Texture(Gdx.files.internal("ui/LilyOUTLINED.png"));
            lilyRegion = new TextureRegion(lilyTexture);
        }
    }

    public boolean isTutorialMovementAllowed() {
        return tutorialMovementAllowed;
    }

    public boolean isLilyTriggered() {
        return lilyTriggered;
    }

    public void onLilyTriggered() {
        if (lilyTriggered) return;
        lilyTriggered = true;

        // Lily interaction transitions BGM to triBGM.wav.
        transitionToTriBgm();

        if (Gdx.files.internal("sfx/shdemo-sfx-79.ogg").exists()) {
            if (lilyTriggerSound == null) {
                lilyTriggerSound = Gdx.audio.newSound(Gdx.files.internal("sfx/shdemo-sfx-79.ogg"));
            }
            lilyTriggerSound.play(0.7f);
        }

        // Flicker before fully switching darkness ON.
        if (lightingSystem != null) {
            lightingSystem.lightingEnabled = false;
            Timer.schedule(new Timer.Task() {
                @Override public void run() { if (lightingSystem != null) lightingSystem.lightingEnabled = true; }
            }, 0.08f);
            Timer.schedule(new Timer.Task() {
                @Override public void run() { if (lightingSystem != null) lightingSystem.lightingEnabled = false; }
            }, 0.16f);
            Timer.schedule(new Timer.Task() {
                @Override public void run() { if (lightingSystem != null) lightingSystem.lightingEnabled = true; }
            }, 0.24f);
            Timer.schedule(new Timer.Task() {
                @Override public void run() { if (lightingSystem != null) lightingSystem.lightingEnabled = false; }
            }, 0.32f);
            Timer.schedule(new Timer.Task() {
                @Override public void run() { if (lightingSystem != null) lightingSystem.lightingEnabled = true; }
            }, 0.45f);
        }
        disableLightingDuringTutorial = false;

        if (menuScreen != null) {
            menuScreen.showNarrativeDialog("?!\nThe lights went off! That sound must have something to do with it...");
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    if (menuScreen != null) {
                        menuScreen.showNarrativeDialog("This overtime shift is by far the weirdest.", 2.2f);
                    }
                }
            }, 2.0f);
        }

        // Spawn items + enemy only after the post-lily dialogue sequence ends.
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                spawnPostLilyEntities();
            }
        }, 4.3f);
    }

    private void spawnPostLilyEntities() {
        // Spawn items now (beep cards, flashlight, battery, sting).
        ImmutableArray<Entity> players = engine.getEntitiesFor(Family.all(PlayerComponent.class).get());
        ImmutableArray<Entity> worldEntities = engine.getEntitiesFor(Family.all(TileWorldComponent.class).get());
        if (players.size() > 0 && worldEntities.size() > 0) {
            TransformComponent t = players.first().getComponent(TransformComponent.class);
            TileWorldComponent world = worldEntities.first().getComponent(TileWorldComponent.class);
            int pTileX = (int) (t.pos.x / 32f);
            int pTileY = (int) (t.pos.y / 32f);
            EntitySpawner.spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, potionRegion, world,
                    pTileX, pTileY, world.mapWidthTiles, world.mapHeightTiles);

            // Spawn enemy immediately after dialogue closes.
            if (enemyRegion != null) {
                EntitySpawner.spawnEnemy(engine, enemyRegion);
            }
        }
    }

    private void fadeOutTitleMusic(float durationSeconds) {
        if (titleMusic == null) return;
        final float startVolume = titleMusic.getVolume();
        final float step = 0.1f;
        final int steps = Math.max(1, (int) (durationSeconds / step));
        for (int i = 1; i <= steps; i++) {
            final int idx = i;
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    if (titleMusic == null) return;
                    float t = idx / (float) steps;
                    float v = startVolume * (1f - t);
                    titleMusic.setVolume(Math.max(0f, v));
                    if (idx == steps) {
                        titleMusic.stop();
                        titleMusic.setVolume(startVolume);
                    }
                }
            }, i * step);
        }
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

    private void transitionToTriBgm() {
        if (!Gdx.files.internal("music/triBGM.wav").exists()) return;

        if (triBgmMusic == null) {
            triBgmMusic = Gdx.audio.newMusic(Gdx.files.internal("music/triBGM.wav"));
            triBgmMusic.setLooping(true);
            triBgmMusic.setVolume(0f);
        }
        if (!triBgmMusic.isPlaying()) {
            triBgmMusic.play();
        }

        final float duration = 1.2f;
        final float step = 0.1f;
        final int steps = Math.max(1, (int) (duration / step));
        final float tutorialStart = tutorialMusic != null ? tutorialMusic.getVolume() : 0f;
        final float triTarget = 0.65f;

        for (int i = 1; i <= steps; i++) {
            final int idx = i;
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    float t = idx / (float) steps;
                    if (tutorialMusic != null) {
                        tutorialMusic.setVolume(Math.max(0f, tutorialStart * (1f - t)));
                        if (idx == steps) {
                            tutorialMusic.stop();
                            tutorialMusic.dispose();
                            tutorialMusic = null;
                        }
                    }
                    if (triBgmMusic != null) {
                        triBgmMusic.setVolume(Math.min(triTarget, triTarget * t));
                    }
                }
            }, i * step);
        }
    }

    private void stopTutorialMusic() {
        if (tutorialMusic != null) {
            tutorialMusic.stop();
            tutorialMusic.dispose();
            tutorialMusic = null;
        }
        if (triBgmMusic != null) {
            triBgmMusic.stop();
            triBgmMusic.dispose();
            triBgmMusic = null;
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
        engine.addSystem(new SaveSystem(beepRegion, flashlightRegion, enemyRegion, batteryRegion, potionRegion));
        engine.addSystem(new RenderSystem(batch, camera));
        engine.addSystem(new InteractionSystem(batch));

        lightingSystem = new LightingSystem(camera);
        if (menuScreen != null) {
            lightingSystem.setMenuScreen(menuScreen); // Add this line!
        }
        if (disableLightingDuringTutorial && lightingSystem != null) {
            lightingSystem.lightingEnabled = false;
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
                    enemyRegion, flashlightRegion, batteryRegion, potionRegion);
            engine.getSystem(SaveSystem.class).triggerManualLoad(MAIN_SAVE_FILE);

            if (!snapshotIsValid) {
                engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            }
            Gdx.app.log("TERMINAL", "Save file loaded");
        } else {
            SaveManager.delete(MAIN_SAVE_FILE);
            SaveManager.delete(TEMP_SAVE_FILE);
            engine.getSystem(SaveSystem.class).generateNewRunId();
            if (startedGameProperFromTutorial && lilyRegion != null) {
                // Tutorial run: spawn only player + lily trigger.
                EntitySpawner.spawnTutorialStart(engine, walkAnimation, idleAnimation, lilyRegion);
            } else {
                EntitySpawner.spawnInitialEntities(engine, beepRegion, walkAnimation, idleAnimation,
                        enemyRegion, flashlightRegion, batteryRegion, potionRegion);
            }
            engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
            Gdx.app.log("TERMINAL", "New Instance Started");
        }

        // Attach the player's ConeLight after all entities have been spawned
        ImmutableArray<Entity> players = engine.getEntitiesFor(
                Family.all(PlayerComponent.class).get());
        if (players.size() > 0) {
            lightingSystem.createPlayerLight(players.first(), false);

            // Snap the camera directly to the player on spawn so they are
            // immediately visible when the maze appears (no initial pan
            // from the map corner).
            Entity player = players.first();
            TransformComponent t = player.getComponent(TransformComponent.class);
            if (t != null) {
                camera.position.x = t.pos.x;
                camera.position.y = t.pos.y;
                camera.update();
            }
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

        // 3. NEW: Reset the MovementSystem Enemy Timer (The 45s countdown)
        MovementSystem moveSystem = engine.getSystem(MovementSystem.class);
        if (moveSystem != null) {
            moveSystem.resetEnemyTimer();
        }

        // 4. Remove all enemies from the old run
        ImmutableArray<Entity> enemies = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
        com.badlogic.gdx.utils.Array<Entity> toRemoveEnemies = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : enemies)
            toRemoveEnemies.add(e);
        for (Entity e : toRemoveEnemies)
            engine.removeEntity(e);

        // 5. Physically remove the old items so we can re-generate a new random count
        ImmutableArray<Entity> currentItems = engine.getEntitiesFor(Family.all(InteractableComponent.class).get());
        com.badlogic.gdx.utils.Array<Entity> toRemove = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : currentItems)
            toRemove.add(e);
        for (Entity e : toRemove)
            engine.removeEntity(e);

        // 6. Load the temp save
        engine.getSystem(SaveSystem.class).forceImmediateLoad(TEMP_SAVE_FILE);

        // 7. Clean slate for player components
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

        // 8. Spawn fresh randomized items
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
            EntitySpawner.spawnItems(engine, beepRegion, flashlightRegion, batteryRegion, potionRegion, world, pTileX,
                pTileY, world.mapWidthTiles, world.mapHeightTiles);

            // --- ENEMY SPAWN REMOVED FROM HERE ---
            // We no longer call EntityFactory.createEnemy here.
            // The MovementSystem will handle it once the 45s timer hits zero.

            // Re-save temp snapshot
            engine.getSystem(SaveSystem.class).triggerManualSave(TEMP_SAVE_FILE);
        }

        // 9. Revert player's lighting
        if (players.size() > 0 && lightingSystem != null) {
            lightingSystem.createPlayerLight(players.first(), false);
        }

        // 10. Restore the HUD
        menuScreen.resetUI();
    }

    // =========================================================================
    // LEVEL TRANSITION — Level 1 → Level 2
    // =========================================================================

    /**
     * Purges all Level 1 entities from the engine, repositions the player,
     * loads the Level 2 map, and spawns fresh items and an enemy.
     *
     * <p><b>Purge strategy:</b> We iterate each component family that belongs
     * to the old level (walls, roofs, tile-world, enemies, interactables) and
     * collect matching entities into a temporary buffer before removal.
     * Removing directly from an {@code ImmutableArray} while iterating would
     * throw a {@code ConcurrentModificationException}, so the two-pass
     * collect-then-remove pattern is required.</p>
     *
     * <p>The {@link PlayerComponent} entity is explicitly <b>kept</b> — only
     * its position is updated to a valid starting coordinate on Level 2.</p>
     */
    public void loadLevelTwo() {
        Gdx.app.log("TERMINAL", "=== LOADING LEVEL 2 ===");

        // Play "time signal" once when the player successfully exits the station.
        if (!timeSignalPlayedForRun) {
            timeSignalPlayedForRun = true;
            if (Gdx.files.internal("sfx/time-signal.ogg").exists()) {
                if (timeSignalSound == null) {
                    timeSignalSound = Gdx.audio.newSound(Gdx.files.internal("sfx/time-signal.ogg"));
                }
                try {
                    timeSignalSound.play(0.7f);
                } catch (Exception ignored) {
                    // If audio fails to start, don't break gameplay.
                }
            }
        }

        // ------------------------------------------------------------------
        // 1. Reset system state flags so update() runs normally on the new map
        // ------------------------------------------------------------------
        WinLossSystem wls = engine.getSystem(WinLossSystem.class);
        if (wls != null) {
            wls.reset();
        }

        EnemySystem enemySys = engine.getSystem(EnemySystem.class);
        if (enemySys != null) {
            enemySys.reset();
        }

        // ------------------------------------------------------------------
        // 2. Purge Level 1 entities (walls, roofs, enemies, interactables)
        //    MapManager.loadMap() internally disposes the old TiledMap,
        //    renderer, wall entities, and the TileWorldComponent entity.
        //    We still need to clean up ECS-only entities it does NOT manage:
        //    enemies and interactables.
        // ------------------------------------------------------------------
        purgeFamily(Family.all(EnemyComponent.class).get());
        purgeFamily(Family.all(InteractableComponent.class).get());

        // ------------------------------------------------------------------
        // 3. Reposition the surviving player to a safe Level 2 starting tile
        // ------------------------------------------------------------------
        ImmutableArray<Entity> players = engine.getEntitiesFor(
                Family.all(PlayerComponent.class).get());

        if (players.size() > 0) {
            Entity player = players.first();
            TransformComponent playerTransform = player.getComponent(TransformComponent.class);
            // 10,10 in tile coords → 320,320 px. Adjust if your Level 2 spawn differs.
            playerTransform.pos.set(10 * 32f, 10 * 32f);
            playerTransform.updateBounds();

            // Clear inventory so the player starts Level 2 with a clean slate
            InventoryComponent inv = player.getComponent(InventoryComponent.class);
            if (inv != null) {
                inv.items.clear();
            }

            // Reset battery to full, flashlight off
            BatteryComponent bat = player.getComponent(BatteryComponent.class);
            if (bat != null) {
                bat.battery = bat.maxBattery;
                bat.flashlightOn = false;
            }
        }

        // ------------------------------------------------------------------
        // 4. Load the new map — this creates fresh Wall/Roof/TileWorld entities
        // ------------------------------------------------------------------
        mapManager.loadMap("maps/Level2.tmx");

        // ------------------------------------------------------------------
        // 5. Spawn fresh items and a new enemy for Level 2
        // ------------------------------------------------------------------
        ImmutableArray<Entity> worldEntities = engine.getEntitiesFor(
                Family.all(TileWorldComponent.class).get());
        TileWorldComponent world = worldEntities.size() > 0
                ? worldEntities.first().getComponent(TileWorldComponent.class)
                : null;

        if (world != null) {
            int pTileX = 10;
            int pTileY = 10;
            if (players.size() > 0) {
                TransformComponent t = players.first().getComponent(TransformComponent.class);
                if (t != null) {
                    pTileX = (int) (t.pos.x / 32f);
                    pTileY = (int) (t.pos.y / 32f);
                }
            }

            EntitySpawner.spawnItems(engine, beepRegion, flashlightRegion,
                    batteryRegion, potionRegion, world,
                    pTileX, pTileY,
                    world.mapWidthTiles, world.mapHeightTiles);

            // Spawn the enemy at a safe distance from the player
            com.sam.TERMINAL.entities.EntityFactory.createEnemy(engine, 5 * 32f, 5 * 32f, enemyRegion);
        }

        // ------------------------------------------------------------------
        // 6. Re-attach the player's lighting for the new level
        // ------------------------------------------------------------------
        if (players.size() > 0 && lightingSystem != null) {
            lightingSystem.createPlayerLight(players.first(), false);
        }

        // ------------------------------------------------------------------
        // 7. Reset HUD indicators
        // ------------------------------------------------------------------
        if (menuScreen != null) {
            menuScreen.resetUI();
        }

        Gdx.app.log("TERMINAL", "=== LEVEL 2 LOADED SUCCESSFULLY ===");
    }

    /**
     * Removes every entity matching the given {@link Family} from the engine.
     *
     * <p>Uses a two-pass collect-then-remove pattern to avoid concurrent
     * modification of Ashley's internal entity arrays.</p>
     */
    private void purgeFamily(Family family) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(family);
        com.badlogic.gdx.utils.Array<Entity> buffer = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : entities) {
            buffer.add(e);
        }
        for (Entity e : buffer) {
            engine.removeEntity(e);
        }
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
        // Tutorial overlay (post-spawn) still renders above the game HUD.
        if (tutorialScene != null) {
            tutorialScene.render(delta);
        }

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
        if (potionTexture != null)
            potionTexture.dispose();
        if (lilyTexture != null)
            lilyTexture.dispose();
        if (lilyTriggerSound != null)
            lilyTriggerSound.dispose();
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

    public TextureRegion getPotionRegion() {
        return potionRegion;
    }

    public TextureRegion getEnemyRegion() { return enemyRegion; }

    public MenuScreen getMenuScreen() { return menuScreen; }
}
