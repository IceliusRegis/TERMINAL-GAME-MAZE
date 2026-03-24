package com.sam.TERMINAL.buttons;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.sam.TERMINAL.Main;
import com.sam.TERMINAL.components.InventoryComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.systems.LightingSystem;
import com.sam.TERMINAL.systems.SaveSystem;

/**
 * MenuScreen — Owns all in-game UI stages: HUD, Settings overlay, Inventory overlay,
 * and the Win/Lose screen.
 *
 * ── Bug fixes in this revision ────────────────────────────────────────────────
 * BUG 1 — "Return" button unresponsive after Settings opened
 * BUG 2 — Duplicate global listeners accumulating across resets
 * BUG 3 — SettingsButton textures never disposed
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
    private Texture jumpscareTexture;
    private Sound jumpscareSound;

    // ── State flags ───────────────────────────────────────────────────────────
    private boolean isSettingsVisible = false;
    private boolean isInventoryVisible = false;
    private boolean isGameOver = false;
    private boolean isJumpscaring = false;
    
    // ── UI components ─────────────────────────────────────────────────────────
    private BitmapFont font;
    private Table inventoryWindow;
    private Table itemTable;
    private Table bottomTable;

    // ── ECS / Game references ─────────────────────────────────────────────────
    private final PooledEngine engine;
    private final Main mainGame;

    // ── BUG 2 FIX: store the global listener
    private InputListener globalListener;

    // ── BUG 3 FIX: store the SettingsButton
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
        jumpscareSound = Gdx.audio.newSound(Gdx.files.internal("sfx/jumpscare.mp3"));

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

    private void updateInputProcessor() {
        if (isGameOver || isJumpscaring) {
            Gdx.input.setInputProcessor(uiStage);
            return;
        }

        if (isSettingsVisible) {
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(settingsStage);
            multiplexer.addProcessor(uiStage);
            Gdx.input.setInputProcessor(multiplexer);

        } else if (isInventoryVisible) {
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(inventoryStage);
            multiplexer.addProcessor(uiStage);
            Gdx.input.setInputProcessor(multiplexer);

        } else {
            Gdx.input.setInputProcessor(uiStage);
        }
    }

    private void setupGlobalListener() {
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
                } if (keycode == Input.Keys.F5) {
                    saveMapLogic();
                    return true;
                } if (keycode == Input.Keys.F1) {
                    LightingSystem lightSys = engine.getSystem(LightingSystem.class);
                    if (lightSys != null) {
                        lightSys.lightingEnabled = !lightSys.lightingEnabled;
                        Gdx.app.log("DEBUG", "Lighting Enabled: " + lightSys.lightingEnabled);
                    }
                    return true;
                } if (keycode == Input.Keys.ESCAPE) {
                    isSettingsVisible = !isSettingsVisible;
                    if (isSettingsVisible) isInventoryVisible = false;
                    updateInputProcessor();
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
            refreshInventory();
            drawInventoryDim(inventoryStage);
            inventoryStage.act(delta);
            inventoryStage.draw();
        }
    }

    // =========================================================================
    // Win / Lose / Jumpscare Screen
    // =========================================================================

    public void showGameOver(boolean win) {
        if (isGameOver)
            return;
        isGameOver = true;
        isSettingsVisible = false;
        isInventoryVisible = false;
        uiStage.clear();

        Image dimmer = new Image(whitePixel);
        dimmer.setColor(0, 0, 0, 0.8f);
        dimmer.setFillParent(true);
        uiStage.addActor(dimmer);

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
        uiStage.setKeyboardFocus(restartBtn);
    }

    public void showJumpscare() {
        isJumpscaring = true;

        if (jumpscareTexture != null) jumpscareTexture.dispose();
        jumpscareTexture = new Texture(Gdx.files.internal("ui/jumpscare.jpeg"));

        uiStage.clear();
        isSettingsVisible = false;
        isInventoryVisible = false;

        Image blackOverlay = new Image(whitePixel);
        blackOverlay.setColor(Color.BLACK);
        blackOverlay.setFillParent(true);
        uiStage.addActor(blackOverlay);

        Image jumpscareImg = new Image(jumpscareTexture);
        jumpscareImg.setFillParent(true);
        uiStage.addActor(jumpscareImg);

        if (jumpscareSound != null) {
            jumpscareSound.play(1.0f);
        }

        jumpscareImg.addAction(Actions.sequence(
            Actions.delay(3.0f),
            Actions.fadeOut(1.0f),
            Actions.run(() -> {
                isJumpscaring = false;
                if (jumpscareTexture != null) {
                    jumpscareTexture.dispose();
                    jumpscareTexture = null;
                }
                showGameOver(false);
            })
        ));

        Gdx.input.setInputProcessor(uiStage);
    }

    public void resetUI() {
        isGameOver = false;
        isSettingsVisible = false;
        isInventoryVisible = false;
        isJumpscaring = false;
        uiStage.clear();

        setupHUD();
        setupGlobalListener();
        updateInputProcessor();
    }

    // =========================================================================
    // HUD Builder
    // =========================================================================

    private void setupHUD() {
        uiStage.clear();

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
        itemTable.clearChildren();

        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0)
            return;
        
        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        if (inv != null && inv.hasItem("beep_card")) {
            Image icon = new Image(mainGame.getBeepRegion());
            itemTable.add(icon).size(64, 64).pad(20);
            itemTable.add(new Label(
                    "Beep Card",
                    new Label.LabelStyle(font, Color.WHITE)))
                    .padRight(20);
        } 
        
        if (inv != null && inv.hasItem("flashlight")) {
            Image icon = new Image(mainGame.getFlashlightRegion());
            itemTable.add(icon).size(64, 64).pad(10);
            itemTable.add(new Label("Flashlight",
                new Label.LabelStyle(font, Color.WHITE))).padRight(20);
        }
        itemTable.invalidateHierarchy();
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
        font.dispose();

        if (jumpscareTexture != null) jumpscareTexture.dispose();
        if (jumpscareSound != null) jumpscareSound.dispose();
        
        if (settingsButtonWidget != null) {
            settingsButtonWidget.dispose();
        }
    }

    public boolean isSettingsVisible() {
        return isSettingsVisible;
    }

    public boolean isInventoryVisible() {
        return isInventoryVisible;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isJumpscaring() {
        return isJumpscaring;
    }
}
