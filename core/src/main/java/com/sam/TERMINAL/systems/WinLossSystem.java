package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.Main;
import com.sam.TERMINAL.buttons.MenuScreen;

/**
 * WinLossSystem — Checks win and lose conditions each frame.
 *
 * Win condition: Player is adjacent to a non-zero tile on the "Winning"
 * map layer and presses E. Reuses the same "Press E" prompt UI as
 * InteractionSystem.
 *
 * Lose condition: Enemy entity overlaps the player.
 */
public class WinLossSystem extends EntitySystem {

    private Main mainGame;
    private MenuScreen menuScreen;
    public boolean gameOver = false;
    public boolean win = false;

    private final SpriteBatch batch;
    private Texture promptTexture;
    private TextureRegion promptRegion;

    private static final float TILE_SIZE = 32f;
    private static final float PROMPT_WIDTH = 24f;
    private static final float PROMPT_HEIGHT = 24f;
    private static final float PROMPT_OFFSET_Y = 8f;

    public WinLossSystem(Main main, SpriteBatch batch) {
        this.mainGame = main;
        this.batch = batch;

        // Load the "Press E" prompt texture (same asset as InteractionSystem)
        if (Gdx.files.internal("ui/press_e.png").exists()) {
            promptTexture = new Texture(Gdx.files.internal("ui/press_e.png"));
            promptRegion = new TextureRegion(promptTexture);
        }
    }

    /** Allows Main to inject MenuScreen after construction. */
    public void setMenuScreen(MenuScreen menuScreen) {
        this.menuScreen = menuScreen;
    }

    @Override
    public void update(float deltaTime) {
        if (gameOver || win)
            return;

        // --- LOSE: Enemy touches player ---
        ImmutableArray<Entity> players = getEngine()
                .getEntitiesFor(Family.all(PlayerComponent.class).get());
        ImmutableArray<Entity> enemies = getEngine()
                .getEntitiesFor(Family.all(EnemyComponent.class).get());

        if (players.size() > 0 && enemies.size() > 0) {
            TransformComponent pT = players.first().getComponent(TransformComponent.class);
            for (Entity e : enemies) {
                TransformComponent eT = e.getComponent(TransformComponent.class);
                if (eT != null && eT.bounds.overlaps(pT.bounds)) {
                    if (menuScreen != null && menuScreen.isJumpscaring()) {
                        return;
                    }
                    Gdx.app.log("TERMINAL", "YOU DIED - GAME OVER");
                    gameOver = true;
                    return;
                }
            }
        }

        // --- WIN: Proximity to Winning layer tiles ---
        if (players.size() == 0)
            return;

        ImmutableArray<Entity> worldEntities = getEngine()
                .getEntitiesFor(Family.all(TileWorldComponent.class).get());
        if (worldEntities.size() == 0)
            return;

        TileWorldComponent world = worldEntities.first().getComponent(TileWorldComponent.class);
        if (world.winningLayer == null)
            return;

        TransformComponent playerTransform = players.first().getComponent(TransformComponent.class);
        float playerCenterX = playerTransform.pos.x + (playerTransform.width / 2f);
        float playerCenterY = playerTransform.pos.y + (playerTransform.height / 2f);

        int playerTileX = (int) (playerCenterX / TILE_SIZE);
        int playerTileY = (int) (playerCenterY / TILE_SIZE);

        // Check the player's tile and 4 adjacent tiles for a Winning cell
        boolean nearWinTile = isWinningTile(world, playerTileX, playerTileY)
                || isWinningTile(world, playerTileX + 1, playerTileY)
                || isWinningTile(world, playerTileX - 1, playerTileY)
                || isWinningTile(world, playerTileX, playerTileY + 1)
                || isWinningTile(world, playerTileX, playerTileY - 1);

        if (nearWinTile) {
            // Draw the "Press E" prompt above the player
            if (promptRegion != null) {
                float promptX = playerCenterX - PROMPT_WIDTH / 2f;
                float promptY = playerTransform.pos.y + playerTransform.height + PROMPT_OFFSET_Y + 20f;
                batch.draw(promptRegion, promptX, promptY, PROMPT_WIDTH, PROMPT_HEIGHT);
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                Gdx.app.log("TERMINAL", "YOU WIN - ESCAPED!");
                win = true;
            }
        }
    }

    /**
     * Checks if a tile on the Winning layer is non-zero.
     */
    private boolean isWinningTile(TileWorldComponent world, int tileX, int tileY) {
        if (tileX < 0 || tileX >= world.mapWidthTiles || tileY < 0 || tileY >= world.mapHeightTiles) {
            return false;
        }
        return world.winningLayer.getCell(tileX, tileY) != null;
    }

    public void reset() {
        gameOver = false;
        win = false;
    }

    public void dispose() {
        if (promptTexture != null) {
            promptTexture.dispose();
            promptTexture = null;
        }
    }
}
