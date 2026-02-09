package com.sam.TERMINAL;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
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

    @Override
    public void create() {
        // === 1. INITIALIZE RENDERING AND ECS ===
        engine = new PooledEngine();
        batch = new SpriteBatch();

        // Set up camera (800x600 viewport)
        camera = new OrthographicCamera();
        viewport = new StretchViewport(800, 600, camera);
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
    }

    @Override
    public void render() {
        // 1. Clear the screen
        ScreenUtils.clear(0f, 0f, 0f, 1);

        // 2. Update the Viewport and Camera math
        viewport.apply();
        camera.update();

        // 3. IMPORTANT: Tell the Batch to use the new Camera math
        batch.setProjectionMatrix(camera.combined);

        // 4. Draw everything
        batch.begin();
        engine.update(Gdx.graphics.getDeltaTime());
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        // Clean up resources to prevent memory leaks
        batch.dispose();
        if (playerSpriteSheet != null) {
            playerSpriteSheet.dispose();
        }
    }
}
