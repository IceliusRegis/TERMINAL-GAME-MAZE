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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.entities.EntityFactory;
import com.sam.TERMINAL.systems.*;
import com.sam.TERMINAL.tiles.TileRegistry;

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
        engine.addSystem((new InteractionSystem()));

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

        // 1. Load the original pixmap
        Pixmap originalPixmap = new Pixmap(Gdx.files.internal("cursor.png"));

        //TEMPORARY KEY SPAWNING
        Entity beep = engine.createEntity();
        TransformComponent beepTrans = engine.createComponent(TransformComponent.class);
        beepTrans.pos.set(startPixelX + 50, startPixelY);
        beep.add(beepTrans);

        SpriteComponent beepSprite = engine.createComponent(SpriteComponent.class);
        beepSprite.staticSprite = new TextureRegion(new Texture(Gdx.files.internal("beep.png")));
        beepSprite.isStatic = true;
        beepSprite.drawHeight = 16; beepSprite.drawWidth = 16;
        beep.add(beepSprite);

        beep.add(new InteractableComponent("beep", 60f));
        engine.addEntity(beep);

    }

    @Override
    public void render() {
        // 1. Clear the screen
        ScreenUtils.clear(0f, 0f, 0f, 1);

        // 2. Draw the Game World (Walls, Player)
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        engine.update(Gdx.graphics.getDeltaTime());
        batch.end();

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
