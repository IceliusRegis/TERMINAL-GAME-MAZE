package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.*;
import com.sam.TERMINAL.buttons.MenuScreen;
import com.sam.TERMINAL.components.PersistenceComponent;
import com.sam.TERMINAL.entities.EntityFactory;
import com.sam.TERMINAL.systems.CameraFollowSystem;
import com.sam.TERMINAL.systems.MovementSystem;
import com.sam.TERMINAL.systems.RenderSystem;
import com.sam.TERMINAL.systems.SaveSystem;
import com.sam.TERMINAL.tiles.TileRegistry;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.systems.TileRenderSystem;

/**
 * Main - The entry point and manager for TERMINAL.
 *
 * Responsibilities:
 * - Initialize Ashley ECS engine
 * - Register systems (workers)
 * - Load assets
 * - Create initial entities
 * - Run the game loop
 */
public class Main extends ApplicationAdapter {

    // Ashley ECS engine - manages all entities, components, and systems
    private PooledEngine engine;

    // LibGDX rendering tools
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    // TODO: Move to AssetManager later
    private Texture playerSpriteSheet;
    private Texture cursorTexture;

    private MenuScreen menuScreen;

    @Override
    public void create() {
        // === 1. INITIALIZE RENDERING AND ECS ===
        engine = new PooledEngine();
        batch = new SpriteBatch();

        // Set up camera (800x600 viewport)
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(800, 600, camera);
        viewport.apply();

        // Load Tiles

        //Creates walls
        Texture wallTexture = new Texture(Gdx.files.internal("BackRoomsWall.png"));
        TextureRegion wallRegion = new TextureRegion(wallTexture);

        //Temp Floor
        Texture floorTexture = new Texture(Gdx.files.internal("Floor.png"));
        TextureRegion floorRegion = new TextureRegion(floorTexture);

        //Registers the Walls
        TileRegistry.registerTile(1, true, wallRegion);
        TileRegistry.registerTile(2, false, floorRegion);

        //Creates the actual Game world
        Entity worldEntity = engine.createEntity();
        TileWorldComponent tileCom = new TileWorldComponent();
        tileCom.init(50,50);

        // Fill the floor
        for(int x=0; x<50; x++) {
            for(int y=0; y<50; y++) {
                tileCom.map[x][y] = 2;
                if (Math.random() <0.2) tileCom.map[x][y] = 1;
            }
        }

        worldEntity.add(tileCom);

        //Saves Current Map
        worldEntity.add(new PersistenceComponent("MAP","ZA_WARDO"));

        engine.addEntity(worldEntity);

        // Spawning Mechanics
        int spawnTileX = 0;
        int spawnTileY = 0;
        boolean safeSpotFound = false;

        while (!safeSpotFound) {
            //Pick Random Coordinates
            spawnTileX = (int) (Math.random() * 50);
            spawnTileY = (int) (Math.random() * 50);

            //Checks if the tile found is a floor
            if (tileCom.map[spawnTileX][spawnTileY] == 2) {
                safeSpotFound = true;
            }
        }

        //Convert found coordinates into Pixels
        //(Example: Tile 5 * 32 pixels = Pixel 160)
        float startPixelX = spawnTileX * 32f;
        float startPixelY = spawnTileY * 32f;



        // === 2. REGISTER SYSTEMS (Order matters! Logic before rendering) ===
        engine.addSystem(new MovementSystem());
        engine.addSystem(new CameraFollowSystem(camera));
        engine.addSystem(new SaveSystem());
        engine.addSystem(new TileRenderSystem(batch, camera));
        engine.addSystem(new RenderSystem(batch, camera));

        // === 3. LOAD ASSETS ===
        // TODO: Replace with placeholder if mc_walk.png doesn't exist

        //Walking Animation
        try {
            playerSpriteSheet = new Texture("Soldier-walk.png");
        } catch (Exception e) {
            Gdx.app.error("TERMINAL", "Could not load mc_walk.png - using placeholder");
            // Create a simple 32x32 white square as fallback
            playerSpriteSheet = new Texture(Gdx.files.internal("badlogic.jpg")); // LibGDX default
        }
        // Split sprite sheet into frames (assumes 32x32 tiles)
        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 100, 100);
        // Create walking animation from first row (0.1 seconds per frame)
        Animation<TextureRegion> walkAnimation = new Animation<>(0.1f, frames[0]);

        //Idle Animation
        Texture idleSheet = new Texture("Soldier-Idle.png");
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 100, 100);

        // 0.15f makes the idle "breathing" slightly slower than walking
        Animation<TextureRegion> idleAnimation = new Animation<>(0.15f, idleFrames[0]);


        // === 4. CREATE INITIAL ENTITIES ===
        EntityFactory.createPlayer(engine, startPixelX, startPixelY, 20f, 20f, walkAnimation, idleAnimation);

        Gdx.app.log("TERMINAL", "Week 0 initialization complete!");

        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
        cursorTexture = new Texture(Gdx.files.internal("cursor.png"));

        Pixmap originalPixmap = new Pixmap(Gdx.files.internal("cursor.png"));

        menuScreen = new MenuScreen(batch);
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
