package com.sam.TERMINAL;

import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.sam.TERMINAL.entities.EntityFactory;
import com.sam.TERMINAL.systems.CameraFollowSystem;
import com.sam.TERMINAL.systems.MovementSystem;
import com.sam.TERMINAL.systems.RenderSystem;

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

    // TODO: Move to AssetManager later
    private Texture playerSpriteSheet;

    @Override
    public void create() {
        // === 1. INITIALIZE RENDERING ===
        batch = new SpriteBatch();

        // Set up camera (800x600 viewport)
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        camera.update();

        // === 2. INITIALIZE ECS ENGINE ===
        engine = new PooledEngine();

        // === 3. REGISTER SYSTEMS (Order matters! Logic before rendering) ===
        engine.addSystem(new MovementSystem());
        engine.addSystem(new CameraFollowSystem(camera));
        engine.addSystem(new RenderSystem(batch));

        // === 4. LOAD ASSETS ===
        // TODO: Replace with placeholder if mc_walk.png doesn't exist
        try {
            playerSpriteSheet = new Texture("mc_walk.png");
        } catch (Exception e) {
            Gdx.app.error("TERMINAL", "Could not load mc_walk.png - using placeholder");
            // Create a simple 32x32 white square as fallback
            playerSpriteSheet = new Texture(Gdx.files.internal("badlogic.jpg")); // LibGDX default
        }

        // Split sprite sheet into frames (assumes 32x32 tiles)
        TextureRegion[][] frames = TextureRegion.split(playerSpriteSheet, 32, 32);

        // Create walking animation from first row (0.1 seconds per frame)
        Animation<TextureRegion> walkAnimation = new Animation<>(0.1f, frames[0]);

        Texture wallTileset = new Texture("Tilesetv3.png");
        TextureRegion[][] wallTiles = TextureRegion.split(wallTileset, 32, 32);

        // Get specific wall tile (e.g., first tile in sheet)
        TextureRegion wallSprite = wallTiles[9][5];


        // === 5. CREATE INITIAL ENTITIES ===
        EntityFactory.createPlayer(engine, walkAnimation);

        // Create test walls to demonstrate collision
        EntityFactory.createWall(engine, 100, 200, wallSprite);
        EntityFactory.createWall(engine, 300, 200, wallSprite); // Adjacent wall
        EntityFactory.createWall(engine, 500, 200, wallSprite); // Another adjacent wall

        Gdx.app.log("TERMINAL", "Week 0 initialization complete!");
    }

    @Override
    public void render() {
        // Clear screen to black
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        // Update camera
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Begin rendering
        batch.begin();

        // Run all systems (movement, rendering, etc.)
        // Delta time ensures smooth, framerate-independent updates
        engine.update(Gdx.graphics.getDeltaTime());

        batch.end();
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
