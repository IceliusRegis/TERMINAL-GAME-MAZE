package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
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

/**
 * Main - The entry point and central manager for TERMINAL.
 *
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

    // Asset References (Fileds for Reset Logic and Disposal)
    private Texture playerSpriteSheet, cursorTexture;
    private Texture beepTexture, doorOpenTexture, doorClosedTexture, wallTexture, floorTexture;

    // Regions and Animation (Shared)
    private TextureRegion beepRegion, doorOpenRegion, doorCloseRegion;
    private Animation<TextureRegion> walkAnimation, idleAnimation;

    @Override
    public void create() {

        // 1.) Load Core Tools
        initEngine();

        // 2.) Load assets
        loadAssets();

        // 3.) Register Tile Types
        registerTiles();

        // 4.) Boot up game sys (Move, render, save)
        initSystem();

        //5.) Game start (New vs Load)
        handleGameStart();

        //6.) Starts Up UI
        createUI();

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
        wallTexture = new Texture(Gdx.files.internal("environment/BackRoomsWall.png"));
        floorTexture = new Texture(Gdx.files.internal("environment/Floor.png"));

        //INTERACTIVES

        //Beep
        beepTexture = new Texture(Gdx.files.internal("sprites/beep.png"));
        beepRegion = new TextureRegion(beepTexture);

        //Temp Door
        doorOpenTexture = new Texture(Gdx.files.internal("environment/opendoor.png"));
        doorOpenRegion = new TextureRegion(doorOpenTexture);

        doorClosedTexture = new Texture(Gdx.files.internal("environment/closedoor.png"));
        doorCloseRegion = new TextureRegion(doorClosedTexture);

        //UI
        cursorTexture = new Texture(Gdx.files.internal("ui/cursor.png"));

        //PLAYER SPRITES
        playerSpriteSheet = new Texture("sprites/Soldier-Walk.png");

        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 100, 100);
        walkAnimation = new Animation<>(0.1f, frames[0]);

        Texture idleSheet = new Texture("sprites/Soldier-Idle.png");
        TextureRegion[][] idleframes = TextureRegion.split(idleSheet, 100, 100);
        idleAnimation = new Animation<>(0.15f, frames[0]);
    }

    private void registerTiles() {
        TileRegistry.registerTile(1, true, new TextureRegion(wallTexture));
        TileRegistry.registerTile(2, false, new TextureRegion(floorTexture));
    }



    @Override
    public void render() {
        // 1. Clear the screen
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1);

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

        // 3. Draw the Software Cursor (Calculates size based on window)
        // Reset the Projection Matrix so (0,0) is bottom-left of the WINDOW
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Calculate scaling (relative to your 800px base width)
        float scale = Gdx.graphics.getWidth() / 800f;
        float cursorSize = 48 * scale;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y

        batch.begin();
        if (cursorTexture != null) {
            batch.draw(cursorTexture, mouseX, mouseY - cursorSize, cursorSize, cursorSize);
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        menuScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        // Clean up resources to prevent memory leaks
        batch.dispose();
        if (playerSpriteSheet != null) {
            playerSpriteSheet.dispose();
        }
        if (cursorTexture != null) {
            cursorTexture.dispose();
        }
    }
}
