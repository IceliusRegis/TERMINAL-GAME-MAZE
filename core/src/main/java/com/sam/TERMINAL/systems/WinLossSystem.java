package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.Main;
import com.sam.TERMINAL.buttons.MenuScreen;

/**
 * WinLossSystem — Checks win and lose conditions each frame.
 *
 * Win condition (UPDATED): Player must:
 *   1. Be adjacent to a non-zero tile on the "Winning" map layer, AND
 *   2. Have "beep_card" in their InventoryComponent.
 *   Then pressing E triggers the win.
 *
 * If the player is near the win tile but lacks the card, a
 * "Find the Beep Card first!" notification is shown instead of the prompt.
 *
 * Lose condition: Enemy entity overlaps the player.
 */
public class WinLossSystem extends EntitySystem {

    private static final String BEEP_CARD_ITEM_ID = "beep_card";
    private static final String MISSING_CARD_MESSAGE = "Find the Beep Card first!";

    private Main mainGame;
    private MenuScreen menuScreen;
    public boolean gameOver = false;
    public boolean win = false;

    private final SpriteBatch batch;
    private Texture promptTexture;
    private TextureRegion promptRegion;

    // Font used to render the "missing card" warning message
    private final BitmapFont notificationFont;
    private final GlyphLayout glyphLayout;

    private static final float TILE_SIZE = 32f;
    private static final float PROMPT_WIDTH = 24f;
    private static final float PROMPT_HEIGHT = 24f;
    private static final float PROMPT_OFFSET_Y = 8f;

    // Set each frame in update(); read by renderPrompt() in Main after lighting.
    private boolean nearWinTile = false;
    private boolean playerHasBeepCard = false;
    private float promptWorldX = 0f;
    private float promptWorldY = 0f;

    public WinLossSystem(Main main, SpriteBatch batch) {
        this.mainGame = main;
        this.batch = batch;

        // Load the "Press E" prompt texture (same asset as InteractionSystem)
        if (Gdx.files.internal("ui/press_e.png").exists()) {
            promptTexture = new Texture(Gdx.files.internal("ui/press_e.png"));
            promptRegion = new TextureRegion(promptTexture);
        }

        // Build the notification font — default BitmapFont is always available
        notificationFont = new BitmapFont();
        notificationFont.getData().setScale(1.4f);
        notificationFont.setColor(Color.YELLOW);
        glyphLayout = new GlyphLayout();
    }

    /** Allows Main to inject MenuScreen after construction. */
    public void setMenuScreen(MenuScreen menuScreen) {
        this.menuScreen = menuScreen;
    }

    @Override
    public void update(float deltaTime) {
        // Reset per-frame state flags at the top of every frame.
        nearWinTile = false;
        playerHasBeepCard = false;

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

        // --- WIN: Proximity to Winning layer tiles + Beep Card check ---
        if (players.size() == 0)
            return;

        ImmutableArray<Entity> worldEntities = getEngine()
                .getEntitiesFor(Family.all(TileWorldComponent.class).get());
        if (worldEntities.size() == 0)
            return;

        TileWorldComponent world = worldEntities.first().getComponent(TileWorldComponent.class);
        if (world.winningLayer == null)
            return;

        Entity player = players.first();
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        float playerCenterX = playerTransform.pos.x + (playerTransform.width / 2f);
        float playerCenterY = playerTransform.pos.y + (playerTransform.height / 2f);

        int playerTileX = (int) (playerCenterX / TILE_SIZE);
        int playerTileY = (int) (playerCenterY / TILE_SIZE);

        // Check the player's tile and 4 adjacent tiles for a Winning cell
        nearWinTile = isWinningTile(world, playerTileX, playerTileY)
                || isWinningTile(world, playerTileX + 1, playerTileY)
                || isWinningTile(world, playerTileX - 1, playerTileY)
                || isWinningTile(world, playerTileX, playerTileY + 1)
                || isWinningTile(world, playerTileX, playerTileY - 1);

        if (!nearWinTile)
            return;

        // Cache prompt world position for renderPrompt().
        promptWorldX = playerCenterX - PROMPT_WIDTH / 2f;
        promptWorldY = playerTransform.pos.y + playerTransform.height + PROMPT_OFFSET_Y + 20f;

        // --- BEEP CARD prerequisite check ---
        InventoryComponent inventory = player.getComponent(InventoryComponent.class);
        playerHasBeepCard = (inventory != null) && inventory.hasItem(BEEP_CARD_ITEM_ID);

        if (!playerHasBeepCard) {
            // Player is at the exit but missing the card — do nothing further this frame.
            // renderPrompt() will draw the warning message instead of the Press-E icon.
            return;
        }

        // Player has the card: listen for the E key to complete the win.
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Gdx.app.log("TERMINAL", "YOU WIN - ESCAPED!");
            win = true;
        }
    }

    /**
     * Called by Main.renderInteractionPrompts() AFTER the lighting pass so
     * indicators are always visible above the darkness overlay.
     *
     * Behavior:
     *   - Player near win tile WITHOUT beep_card → yellow "Find the Beep Card first!" text.
     *   - Player near win tile WITH beep_card    → standard "Press E" sprite prompt.
     */
    public void renderPrompt() {
        if (!nearWinTile) {
            return;
        }

        if (!playerHasBeepCard) {
            // Draw the warning message in screen-space so it is always legible.
            renderMissingCardWarning();
            return;
        }

        // Player has the card: draw the standard Press-E icon in world-space.
        if (promptRegion != null) {
            batch.draw(promptRegion, promptWorldX, promptWorldY, PROMPT_WIDTH, PROMPT_HEIGHT);
        }
    }

    /**
     * Draws the "Find the Beep Card first!" warning message centred horizontally
     * in screen-space above the middle of the screen.
     *
     * The batch is already open and using the world projection matrix when this
     * is called from Main. We temporarily switch to a screen-space ortho matrix,
     * draw the text, then restore the world matrix so subsequent draws are not
     * disrupted.
     */
    private void renderMissingCardWarning() {
        // Snapshot the current world-space projection so we can restore it later.
        com.badlogic.gdx.math.Matrix4 worldMatrix = batch.getProjectionMatrix().cpy();

        // Build a brand-new screen-space ortho matrix and hand it to the batch
        // via setProjectionMatrix(). Simply mutating the object returned by
        // getProjectionMatrix() does NOT push the change to the GPU — the batch
        // only re-uploads the matrix when setProjectionMatrix() is called explicitly.
        com.badlogic.gdx.math.Matrix4 screenMatrix = new com.badlogic.gdx.math.Matrix4();
        screenMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(screenMatrix);

        glyphLayout.setText(notificationFont, MISSING_CARD_MESSAGE);
        float screenX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2f;
        float screenY = Gdx.graphics.getHeight() * 0.72f; // upper portion of screen

        notificationFont.draw(batch, MISSING_CARD_MESSAGE, screenX, screenY);

        // Restore the world-space matrix so subsequent world-space draws are correct.
        batch.setProjectionMatrix(worldMatrix);
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
        notificationFont.dispose();
    }
}
