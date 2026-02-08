package com.sam.TERMINAL.tiles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Tile - Represents a type of block in the world.
 * * This is NOT a component. It's a lightweight definition.
 * We don't create a new "Tile" object for every grass block.
 * We just reuse one definition for ID #2.
 */

public class Tile {
    public int id;
    public boolean isSolid;
    public TextureRegion texture;

    public Tile(int id, boolean isSolid, TextureRegion texture) {
        this.id = id;
        this.isSolid = isSolid;
        this.texture = texture;
    }
}
