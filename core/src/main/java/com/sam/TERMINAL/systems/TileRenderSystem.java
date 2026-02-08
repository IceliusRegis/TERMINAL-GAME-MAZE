package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.tiles.Tile;
import com.sam.TERMINAL.tiles.TileRegistry;

/**
 * TileRenderSystem - Renders the visible portion of the tile map.
 *
 * What it does:
 * - It finds the entity holding the world data (TileWorldComponent).
 * - It calculates which tiles are currently visible to the camera ("Culling").
 * - It iterates through that specific chunk of the map array.
 * - It looks up the correct texture for each tile ID and draws it.
 *
 * Why it's efficient:
 * - Instead of drawing the entire 100x100 map every frame (10,000 tiles),
 * it only draws the ~25x20 tiles the camera can actually see.
 * - It separates rendering logic from the map data, allowing for different
 * rendering strategies (e.g., mini-maps) later.
 */

public class TileRenderSystem extends IteratingSystem {

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private ComponentMapper<TileWorldComponent> worldMapper;

    public TileRenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        super(Family.all(TileWorldComponent.class).get());

        this.batch = batch;
        this.camera = camera;
        this.worldMapper = ComponentMapper.getFor(TileWorldComponent.class);

    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {

        //Gets the map Data
        TileWorldComponent world = worldMapper.get(entity);

        //1.) It finds the left side of the camera to start drawing there
        float viewLeft = camera.position.x - (camera.viewportWidth / 2);
        float viewBottom = camera.position.y - (camera.viewportHeight / 2);

        //2.) Once the pixel is found it converts it to a coordinate
        // Pixel Pos / 32 = The tile index the player is standing on
        int startCol = (int) (viewLeft / world.tileWidth);
        int startRow = (int) (viewBottom / world.tileHeight);

        //3.) Safety buffer to still draw pixels whoose half are still on screen
        startCol -= 1;
        startRow -= 1;

        //4.)  Checks max tiles that can fit on screen
        // Start index + (screen width or height / 32) + Safety Buffer (+2)
        int endCol = startCol + (int) (camera.viewportWidth / world.tileWidth) + 4;
        int endRow = startRow + (int) (camera.viewportHeight / world.tileHeight) + 4;

        //5.) If camera goes far to the left (negatives) it goes back to 0
        if (startCol < 0) startCol =  0;
        if  (startRow < 0) startRow = 0;

        //6.) If the camera goes too far to the right it stops at edge of the map
        if (endCol >= world.mapWidth) endCol = world.mapWidth;
        if (endRow >= world.mapHeight) endRow = world.mapHeight;

        //7.) Actual Loop/Drawing Basically we darw a rectangle above or the view window which is the cam
        for (int x = startCol; x < endCol; x++) {
            for (int y = startRow; y < endRow; y++) {

                //Checks what TileID is at a specific coordinates
                int tileId = world.map[x][y];

                //Picks up the actual block to draw
                Tile tile = TileRegistry.getTile(tileId);

                //Draw the actual block/tile if the tileID returns air it skips it
                if (tile !=null && tile.texture !=null) {
                    batch.draw(
                        tile.texture,
                        x * world.tileWidth, // Converst the array back to pixels
                        y * world.tileHeight,
                        world.tileWidth,
                        world.tileHeight
                    );
                }
            }
        }
    }
}


