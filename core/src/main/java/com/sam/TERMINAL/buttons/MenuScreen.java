package com.sam.TERMINAL.buttons;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.sam.TERMINAL.components.InventoryComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.systems.SaveSystem;
import com.sam.TERMINAL.Main;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * MenuScreen — Owns all in-game UI stages: HUD, Settings overlay, Inventory
 * overlay,
 * and the Win/Lose screen.
 *
 * ── Bug fixes in this revision
 * ────────────────────────────────────────────────
 *
 * BUG 1 — "Return" button unresponsive after Settings opened (Priority 2)
 * Root cause: In updateInputProcessor() the uiStage was added to the
 * InputMultiplexer FIRST. Because the Settings gear icon on uiStage occupies
 * the same top-left screen position as the Back button on settingsStage,
 * uiStage consumed every click in that region before settingsStage could
 * see it — causing the gear to re-open Settings and the Back button to
 * never fire.
 * Fix: overlay stages (settingsStage / inventoryStage) are now added FIRST in
 * the multiplexer so they have input priority. uiStage is added second as a
 * fallback (it still receives global keyboard shortcuts via its listener).
 *
 * BUG 2 — Duplicate global listeners accumulating across resets
 * Root cause: setupGlobalListener() created a new local InputListener each
 * call and tried to remove() it (removing the brand-new instance, which was
 * never added), then added the new one — leaving the old one still attached.
 * After each resetUI() the stage had one extra TAB/F5 listener.
 * Fix: globalListener is stored as a field; setupGlobalListener() removes the
 * stored reference before creating and registering the new one.
 *
 * BUG 3 — SettingsButton textures never disposed
 * Root cause: The SettingsButton instance was never stored, so dispose() was
 * never reachable.
 * Fix: settingsButtonWidget is stored as a field and disposed in dispose().
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class MenuScreen {

    // ── Stages ────────────────────────────────────────────────────────────────
    private final Stage uiStage;
    private final Stage settingsStage;
    private final Stage inventoryStage;

    // ── Textures ──────────────────────────────────────────────────────────────
    private Texture settingsTexture;
    private Texture whitePixel;
    private Texture invTexture;
    private Texture restartTexture;
    // NOTE: backTexture was declared but never initialised in the original code.
    // It has been removed to eliminate dead fields and a potential NPE.

    // ── State flags ───────────────────────────────────────────────────────────
    private boolean isSettingsVisible = false;
    private boolean isInventoryVisible = false;
    private boolean isGameOver = false;

    // ── UI components ─────────────────────────────────────────────────────────
    private BitmapFont font;
    private Table inventoryWindow;
    private Table itemTable;
    private Table bottomTable;

    // ── ECS / Game references ─────────────────────────────────────────────────
    private final PooledEngine engine;
    private final Main mainGame;

    // ── BUG 2 FIX: store the global listener so it can be removed before
    // being re-added (prevents accumulation across resetUI() calls).
    private InputListener globalListener;

    // ── BUG 3 FIX: store the SettingsButton so dispose() can be called on it.
    private SettingsButton settingsButtonWidget;

    // =========================================================================
    // Constructor
    // =========================================================================

    public MenuScreen(SpriteBatch batch, final PooledEngine engine, final Main mainGame) {
        this.engine = engine;
        this.mainGame = mainGame;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        uiStage = new Stage(new ExtendViewport(w, h), batch);
        settingsStage = new Stage(new ExtendViewport(w, h), batch);
        inventoryStage = new Stage(new ExtendViewport(w, h), batch);

        font = new BitmapFont();
        font.getData().setScale(2f);

        restartTexture = new Texture(Gdx.files.internal("ui/Restart.png"));
        settingsTexture = new Texture(Gdx.files.internal("ui/settings.png"));
        invTexture = new Texture(Gdx.files.internal("ui/inventory.png"));

        createDimmerTexture();

        // ── HUD: Settings gear (top-left) ─────────────────────────────────────
        Table mainRoot = new Table();
        mainRoot.setFillParent(true);
        mainRoot.top().left();

        Image settingsBtn = new Image(settingsTexture);
        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isSettingsVisible = true;
                updateInputProcessor();
            }
        });
        mainRoot.add(settingsBtn).size(40, 40).pad(10);
        uiStage.addActor(mainRoot);

        // ── Settings overlay ─────────────────────────────────────────────────
        Table settingsRoot = new Table();
        settingsRoot.setFillParent(true);
        settingsStage.addActor(settingsRoot);

        // BUG 3 FIX: store the reference.
        settingsButtonWidget = new SettingsButton(
                settingsRoot,
                () -> { // onClose — Return button
                    isSettingsVisible = false;
                    updateInputProcessor();
                },
                () -> { // onSave
                    saveMapLogic();
                },
                () -> { // onReset
                    mainGame.resetGame();
                    isSettingsVisible = false;
                    updateInputProcessor();
                });

        // ── HUD: Inventory button (bottom-center) ─────────────────────────────
        ImageButton inventoryBtn = new ImageButton(
                new TextureRegionDrawable(new TextureRegion(invTexture)));
        bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom();
        bottomTable.add(inventoryBtn).size(55, 55).padBottom(5);
        uiStage.addActor(bottomTable);

        inventoryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = true;
                updateInputProcessor();
            }
        });

        // ── Inventory overlay ─────────────────────────────────────────────────
        Table inventoryRoot = new Table();
        inventoryRoot.setFillParent(true);
        inventoryStage.addActor(inventoryRoot);

        inventoryWindow = new Table();
        TextureRegionDrawable windowBg = new TextureRegionDrawable(new TextureRegion(whitePixel));
        inventoryWindow.setBackground(
                windowBg.tint(new Color(0.8f, 0.8f, 0.8f, 0.15f)));
        inventoryRoot.center().add(inventoryWindow).size(500, 400);

        // The exit (close) button lives in inventoryWindow and is never cleared.
        new InventoryButton(inventoryWindow, () -> {
            isInventoryVisible = false;
            updateInputProcessor();
        });
        inventoryWindow.getCells().peek().left().expandX();
        inventoryWindow.row();

        // Sub-table for item rows — cleared every frame by refreshInventory().
        itemTable = new Table();
        itemTable.top().left();
        inventoryWindow.add(itemTable).expand().fill();

        // ── Global keyboard shortcuts (TAB / F5) ──────────────────────────────
        setupGlobalListener();
        updateInputProcessor();
    }

    // =========================================================================
    // Input Routing
    // =========================================================================

    /**
     * Routes raw input to the correct stage(s).
     *
     * ── BUG 1 FIX ─────────────────────────────────────────────────────────────
     * Overlay stages (settingsStage / inventoryStage) are now added to the
     * InputMultiplexer FIRST so they receive priority for mouse/touch events.
     *
     * Previously uiStage was first, which meant the Settings gear icon on uiStage
     * intercepted clicks intended for the Back button on settingsStage (both live
     * at the top-left corner of the screen). By giving the overlay stage priority,
     * the Back button now correctly receives its click and fires onClose.
     *
     * uiStage is still included second so it continues to receive keyboard events
     * (TAB, F5) via the global listener attached to it.
     * ──────────────────────────────────────────────────────────────────────────
     */
    private void updateInputProcessor() {
        // B-5 Guard: while the game-over screen is active, uiStage owns input
        // exclusively.
        if (isGameOver) {
            Gdx.input.setInputProcessor(uiStage);
            return;
        }

        if (isSettingsVisible) {
            InputMultiplexer multiplexer = new InputMultiplexer();
            // FIRST: settings overlay — must intercept clicks before uiStage so
            // the Back/Return button is not blocked by the gear icon beneath it.
            multiplexer.addProcessor(settingsStage);
            // SECOND: HUD stage — fallback for global keyboard shortcuts (TAB, F5).
            multiplexer.addProcessor(uiStage);
            Gdx.input.setInputProcessor(multiplexer);

        } else if (isInventoryVisible) {
            InputMultiplexer multiplexer = new InputMultiplexer();
            // FIRST: inventory overlay gets priority for its close button / item clicks.
            multiplexer.addProcessor(inventoryStage);
            // SECOND: HUD stage — fallback for global keyboard shortcuts (TAB to close).
            multiplexer.addProcessor(uiStage);
            Gdx.input.setInputProcessor(multiplexer);

        } else {
            Gdx.input.setInputProcessor(uiStage);
        }
    }

    /**
     * Registers global keyboard shortcuts (TAB → toggle inventory, F5 → save).
     *
     * ── BUG 2 FIX ─────────────────────────────────────────────────────────────
     * The listener is stored in the {@code globalListener} field. Before adding a
     * new listener, the old one is explicitly removed from all three stages. This
     * prevents duplicate listeners from accumulating every time resetUI() is
     * called.
     * ──────────────────────────────────────────────────────────────────────────
     */
    private void setupGlobalListener() {
        // Remove the previously registered listener (if any) before re-adding.
        if (globalListener != null) {
            uiStage.removeListener(globalListener);
            settingsStage.removeListener(globalListener);
            inventoryStage.removeListener(globalListener);
        }

        globalListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.TAB) {
                    isInventoryVisible = !isInventoryVisible;
                    if (isInventoryVisible)
                        isSettingsVisible = false;
                    updateInputProcessor();
                    return true;
                }
                if (keycode == Input.Keys.F5) {
                    saveMapLogic();
                    return true;
                }
                return false;
            }
        };

        uiStage.addListener(globalListener);
        settingsStage.addListener(globalListener);
        inventoryStage.addListener(globalListener);
    }

    // =========================================================================
    // Render
    // =========================================================================

    public void render(float delta) {
        uiStage.act(delta);
        uiStage.draw();

        if (isSettingsVisible) {
            drawDim(settingsStage);
            settingsStage.act(delta);
            settingsStage.draw();
        } else if (isInventoryVisible) {
            // Refresh item list from live ECS state every frame the panel is visible.
            refreshInventory();
            drawInventoryDim(inventoryStage);
            inventoryStage.act(delta);
            inventoryStage.draw();
        }
    }

    // =========================================================================
    // Win / Lose Screen
    // =========================================================================

    /** Shows the Win ("YOU ESCAPED!") or Lose ("YOU DIED") end screen. */
    public void showGameOver(boolean win) {
        if (isGameOver)
            return;
        isGameOver = true;
        isSettingsVisible = false;
        isInventoryVisible = false;
        uiStage.clear();

        // Dark background
        Image dimmer = new Image(whitePixel);
        dimmer.setColor(0, 0, 0, 0.8f);
        dimmer.setFillParent(true);
        uiStage.addActor(dimmer);

        // Centred text + restart button
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        String text = win ? "YOU ESCAPED!" : "YOU DIED";
        Color color = win ? Color.GREEN : Color.RED;
        Label.LabelStyle style = new Label.LabelStyle(font, color);
        Label label = new Label(text, style);

        ImageButton restartBtn = new ImageButton(
                new TextureRegionDrawable(new TextureRegion(restartTexture)));
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mainGame.resetGame();
            }
        });

        table.add(label).padBottom(20).row();
        table.add(restartBtn).size(64, 64);

        uiStage.addActor(table);
        updateInputProcessor();
        uiStage.setKeyboardFocus(restartBtn); // ENTER/SPACE triggers restart
    }

    /**
     * Resets the UI back to the normal in-game HUD (called by Main.resetGame()).
     */
    public void resetUI() {
        isGameOver = false; // Must be first — clears the guard in updateInputProcessor()
        isSettingsVisible = false;
        isInventoryVisible = false;
        uiStage.clear();

        setupHUD();
        setupGlobalListener(); // Re-attach shortcuts (uiStage.clear() removed them)
        updateInputProcessor();
    }

    // =========================================================================
    // HUD Builder (used by constructor and resetUI)
    // =========================================================================

    private void setupHUD() {
        uiStage.clear();

        // Top-left: Settings gear
        Table mainRoot = new Table();
        mainRoot.setFillParent(true);
        mainRoot.top().left();

        Image settingsBtn = new Image(settingsTexture);
        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isSettingsVisible = true;
                updateInputProcessor();
            }
        });
        mainRoot.add(settingsBtn).size(40, 40).pad(10);
        uiStage.addActor(mainRoot);

        // Bottom-center: Inventory button
        Table localBottomTable = new Table();
        localBottomTable.setFillParent(true);
        localBottomTable.bottom();

        ImageButton inventoryBtn = new ImageButton(
                new TextureRegionDrawable(new TextureRegion(invTexture)));
        inventoryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = true;
                updateInputProcessor();
            }
        });

        localBottomTable.add(inventoryBtn).size(55, 55).padBottom(5);
        uiStage.addActor(localBottomTable);
    }

    // =========================================================================
    // Inventory Refresh
    // =========================================================================

    private void refreshInventory() {
        itemTable.clearChildren(); // Only clears item rows; exit button is untouched.

        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0)
            return;
        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        if (inv != null && inv.hasItem("beep_card")) {
            Image icon = new Image(mainGame.getBeepRegion());
            itemTable.add(icon).size(64, 64).pad(20);
            itemTable.add(new Label(
                    "Beep Card",
                    new Label.LabelStyle(
                            new BitmapFont(),
                            Color.WHITE)))
                    .padRight(20);
        }
    }

    // =========================================================================
    // Dimmer / Overlay Helpers
    // =========================================================================

    private void createDimmerTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void drawDim(Stage stage) {
        stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
        stage.getBatch().begin();
        stage.getBatch().setColor(0, 0, 0, 0.6f);
        stage.getBatch().draw(whitePixel, 0, 0,
                stage.getViewport().getWorldWidth(),
                stage.getViewport().getWorldHeight());
        stage.getBatch().setColor(1, 1, 1, 1);
        stage.getBatch().end();
    }

    private void drawInventoryDim(Stage stage) {
        stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
        stage.getBatch().begin();
        stage.getBatch().setColor(0, 0, 0, 0.5f);
        stage.getBatch().draw(whitePixel,
                (stage.getViewport().getWorldWidth() - 500) / 2f,
                (stage.getViewport().getWorldHeight() - 400) / 2f,
                500, 400);
        stage.getBatch().setColor(1, 1, 1, 1);
        stage.getBatch().end();
    }

    // =========================================================================
    // Save Helper
    // =========================================================================

    private void saveMapLogic() {
        if (engine != null) {
            SaveSystem saveSys = engine.getSystem(SaveSystem.class);
            if (saveSys != null)
                saveSys.triggerManualSave("saveFile.json");
        }
    }

    // =========================================================================
    // Resize / Dispose / Accessors
    // =========================================================================

    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);
        settingsStage.getViewport().update(width, height, true);
        inventoryStage.getViewport().update(width, height, true);
    }

    public void dispose() {
        uiStage.dispose();
        settingsStage.dispose();
        inventoryStage.dispose();

        settingsTexture.dispose();
        whitePixel.dispose();
        invTexture.dispose();
        restartTexture.dispose();

        // BUG 3 FIX: dispose SettingsButton textures via stored reference.
        if (settingsButtonWidget != null) {
            settingsButtonWidget.dispose();
        }
    }

    // ── State accessors ───────────────────────────────────────────────────────
    public boolean isSettingsVisible() {
        return isSettingsVisible;
    }

    public boolean isInventoryVisible() {
        return isInventoryVisible;
    }

    public boolean isGameOver() {
        return isGameOver;
    }
}