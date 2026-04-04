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
 * Win condition: Player must:
 * 1. Be adjacent to a non-zero tile on the "Winning" map layer, AND
 * 2. Have "beep_card" in their InventoryComponent.
 * Then pressing E triggers the win.
 *
 * The 'E' prompt is shown whenever the player is near the win tile, regardless
 * of whether they hold the Beep Card. If E is pressed without the card, a
 * timed "Find the Beep Card first!" warning appears for a few seconds.
 *
 * Lose condition: Enemy entity overlaps the player.
 */
public class WinLossSystem extends EntitySystem {

    private static final String BEEP_CARD_ITEM_ID = "beep_card";
    private String currentMissingMessage = "Find all Beep Cards!";

    private Main mainGame;
    private MenuScreen menuScreen;
    public boolean gameOver = false;
    public boolean win = false;
    public boolean neutralEnd = false;

    private final SpriteBatch batch;
    private Texture promptTexture;
    private TextureRegion promptRegion;

    // Font used to render the "missing card" warning message
    private final BitmapFont notificationFont;
    private final GlyphLayout glyphLayout;

    private static final float TILE_SIZE = 32f;
    private static final float PROMPT_WIDTH = 24f;
    private static final float PROMPT_HEIGHT = 24f;

    /**
     * How long (in seconds) the "Find the Beep Card first!" warning remains
     * visible after the player presses E without having the card.
     */
    private static final float MISSING_CARD_DISPLAY_DURATION = 2.5f;

    // Set each frame in update(); read by renderPrompt() in Main after lighting.
    private boolean nearWinTile = false;
    private boolean nearEndingTile = false;
    private boolean playerHasBeepCard = false;
    private float promptWorldX = 0f;
    private float promptWorldY = 0f;
    private float endingPromptWorldX = 0f;
    private float endingPromptWorldY = 0f;

    /**
     * Counts down from MISSING_CARD_DISPLAY_DURATION to 0.
     * While > 0 the warning message is rendered on screen.
     */
    private float missingCardWarningTimer = 0f;

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
        nearEndingTile = false;
        playerHasBeepCard = false;

        // Tick down the warning timer independently of the win/loss state so
        // the message can finish displaying even mid-frame transitions.
        if (missingCardWarningTimer > 0f) {
            missingCardWarningTimer -= deltaTime;
            if (missingCardWarningTimer < 0f) {
                missingCardWarningTimer = 0f;
            }
        }

        if (gameOver || win || neutralEnd)
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

        // Check the player's tile and 4 adjacent tiles for a Winning cell.
        nearWinTile = isWinningTile(world, playerTileX, playerTileY)
                || isWinningTile(world, playerTileX + 1, playerTileY)
                || isWinningTile(world, playerTileX - 1, playerTileY)
                || isWinningTile(world, playerTileX, playerTileY + 1)
                || isWinningTile(world, playerTileX, playerTileY - 1);

        if (!nearWinTile)
            return;

        // --- Dynamic prompt position ---
        // Place the icon to the right of the player bounding box so it never
        // overlaps the sprite itself. A small horizontal gap of 4 pixels is
        // added between the right edge and the icon's left edge.
        float promptGap = 4f;
        promptWorldX = playerTransform.pos.x + playerTransform.width + promptGap;
        // Vertically centered on the player bounding box.
        promptWorldY = playerCenterY - (PROMPT_HEIGHT / 2f);

        // --- Beep Card prerequisite check ---
        int totalExpected = com.sam.TERMINAL.entities.EntitySpawner.totalBeepCardsSpawned;
        int heldCards = 0;
        InventoryComponent inventory = player.getComponent(InventoryComponent.class);
        if (inventory != null) {
            heldCards = java.util.Collections.frequency(inventory.items, BEEP_CARD_ITEM_ID);
        }
        playerHasBeepCard = (heldCards >= totalExpected && totalExpected > 0);

        // Update dynamic missing message
        currentMissingMessage = "Find all Beep Cards! (" + heldCards + "/" + totalExpected + ")";

        // The 'E' prompt is always shown from here on (nearWinTile == true).
        // If E is pressed, attempt the win or start the warning timer.
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (playerHasBeepCard) {
                Gdx.app.log("TERMINAL", "WIN CONDITION MET — Showing Escape Screen");

                win = true; // Set the local flag to stop system updates

                if (menuScreen != null) {
                    menuScreen.showGameOver(true); // This shows your "YOU ESCAPED" window
                }
            } else {
                // Restart (or extend) the warning display timer.
                missingCardWarningTimer = MISSING_CARD_DISPLAY_DURATION;
                Gdx.app.log("TERMINAL", "Win attempt blocked — Beep Card not found.");
            }
        }

        // --- NEUTRAL END: Proximity to Ending layer tiles ---
        if (world.endingLayer != null) {
            nearEndingTile = isEndingTile(world, playerTileX, playerTileY)
                    || isEndingTile(world, playerTileX + 1, playerTileY)
                    || isEndingTile(world, playerTileX - 1, playerTileY)
                    || isEndingTile(world, playerTileX, playerTileY + 1)
                    || isEndingTile(world, playerTileX, playerTileY - 1);

            if (nearEndingTile) {
                float endingPromptGap = 4f;
                endingPromptWorldX = playerTransform.pos.x + playerTransform.width + endingPromptGap;
                endingPromptWorldY = playerCenterY - (PROMPT_HEIGHT / 2f);

                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    Gdx.app.log("TERMINAL", "NEUTRAL END triggered via Ending layer.");
                    neutralEnd = true;
                    if (menuScreen != null) {
                        menuScreen.showEndScreen("neutral");
                    }
                }
            }
        }
    }

    /**
     * Called by Main.renderInteractionPrompts() AFTER the lighting pass so
     * indicators are always visible above the darkness overlay.
     *
     * Behavior:
     * - Player near win tile → always draws the Press-E icon to the side of
     * the player bounding box.
     * - E pressed without Beep Card → also draws the timed warning text until
     * missingCardWarningTimer reaches zero.
     */
    public void renderPrompt() {
        // Draw the warning message whenever its timer is still active,
        // independently of the nearWinTile flag so the text persists even if
        // the player briefly steps away.
        if (missingCardWarningTimer > 0f) {
            renderMissingCardWarning();
        }

        if (!nearWinTile) {
            return;
        }

        // Always draw the Press-E icon when the player is adjacent to the win tile.
        if (promptRegion != null) {
            batch.draw(promptRegion, promptWorldX, promptWorldY, PROMPT_WIDTH, PROMPT_HEIGHT);
        }

        // Also draw prompt for Ending tile if near
        if (nearEndingTile && promptRegion != null) {
            batch.draw(promptRegion, endingPromptWorldX, endingPromptWorldY, PROMPT_WIDTH, PROMPT_HEIGHT);
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
     *
     * Fades the text out over the last 0.5 s of the timer to give a smooth end.
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

        // Fade out the text smoothly over the final 0.5 s of the timer.
        float fadeWindow = 0.5f;
        float alpha = (missingCardWarningTimer < fadeWindow)
                ? missingCardWarningTimer / fadeWindow
                : 1f;
        notificationFont.setColor(1f, 1f, 0f, alpha); // yellow with fade

        glyphLayout.setText(notificationFont, currentMissingMessage);
        float screenX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2f;
        float screenY = Gdx.graphics.getHeight() * 0.72f; // upper portion of screen

        notificationFont.draw(batch, currentMissingMessage, screenX, screenY);

        // Reset alpha to fully opaque so future draws with this font are unaffected.
        notificationFont.setColor(1f, 1f, 0f, 1f);

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

    /**
     * Checks if a tile on the Ending layer is non-zero.
     */
    private boolean isEndingTile(TileWorldComponent world, int tileX, int tileY) {
        if (tileX < 0 || tileX >= world.mapWidthTiles || tileY < 0 || tileY >= world.mapHeightTiles) {
            return false;
        }
        if (world.endingLayer == null) {
            return false;
        }
        return world.endingLayer.getCell(tileX, tileY) != null;
    }

    public void reset() {
        gameOver = false;
        win = false;
        neutralEnd = false;
        missingCardWarningTimer = 0f;
    }

    public void dispose() {
        if (promptTexture != null) {
            promptTexture.dispose();
            promptTexture = null;
        }
        notificationFont.dispose();
    }
}
