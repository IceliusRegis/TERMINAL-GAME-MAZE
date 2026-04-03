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
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.sam.TERMINAL.components.BatteryComponent;
import com.sam.TERMINAL.components.InventoryComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.systems.LightingSystem;
import com.sam.TERMINAL.systems.SaveSystem;
import com.sam.TERMINAL.Main;
import com.sam.TERMINAL.screen.SubmenuPanel;

public class MenuScreen {
    private Stage uiStage;
    private Stage settingsStage;
    private Stage inventoryStage;
    private Texture settingsTexture, backTexture, whitePixel, invTexture;
    private Texture restartTexture;
    private Texture jumpscareTexture;

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

    // --- Dynamic HUD Elements ---
    private Label beepCardLabel;
    private Label batteryLabel;
    private Label lowBatteryWarningLabel;
    private Label monsterWarningLabel;

    // ── ECS / Game references ─────────────────────────────────────────────────
    private final PooledEngine engine;
    private final Main mainGame;

    // ── Audio ─────────────────────────────────────────────────────────────────
    private Sound jumpscareSound;

    // ── BUG 2 FIX: store the global listener so it can be removed before
    // being re-added (prevents accumulation across resetUI() calls).
    private InputListener globalListener;

    // ── BUG 3 FIX: store the SettingsButton so dispose() can be called on it.
    private SettingsButton settingsButtonWidget;
    private InventoryButton inventoryButtonWidget;

    // --- HUD Elements & Timers ---
    private Label stingTimerLabel;
    private Label noFlashlightWarningLabel;
    private SubmenuPanel narrativePanel;
    private Label narrativeLabel;
    private float narrativeTimer = 0f;

    private float stingEffectTimer = 0f;
    private float warningDisplayTimer = 0f;

    private final float STING_DURATION = 10f;
    private final float WARNING_DURATION = 3f;

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

        font = loadUIFont("fonts/Abaddon Light.ttf", 28);
        restartTexture = new Texture(Gdx.files.internal("ui/Restart.png"));
        settingsTexture = new Texture(Gdx.files.internal("ui/settings.png"));
        invTexture = new Texture(Gdx.files.internal("ui/inventory.png"));
        jumpscareSound = Gdx.audio.newSound(Gdx.files.internal("sfx/jumpscare.mp3"));

        createDimmerTexture();

        // --- HUD SETUP ---
        setupHUD();

        // --- SETTINGS WINDOW SETUP ---
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

        // --- INVENTORY SETUP ---
        Table inventoryRoot = new Table();
        inventoryRoot.setFillParent(true);
        inventoryStage.addActor(inventoryRoot);
        inventoryWindow = new Table();
        TextureRegionDrawable windowBg = new TextureRegionDrawable(new TextureRegion(whitePixel));
        inventoryWindow.setBackground(windowBg.tint(new com.badlogic.gdx.graphics.Color(0.8f, 0.8f, 0.8f, 0.15f)));
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

        setupGlobalListener();
        updateInputProcessor();
    }

    private void setupGlobalListener() {
        // BUG 2 FIX: remove the old listener before creating a new one.
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
                if (keycode == Input.Keys.F1) {
                    LightingSystem lightSys = engine.getSystem(LightingSystem.class);
                    if (lightSys != null) {
                        lightSys.lightingEnabled = !lightSys.lightingEnabled;
                        Gdx.app.log("DEBUG", "Lighting Enabled: " + lightSys.lightingEnabled);
                    }
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    isSettingsVisible = !isSettingsVisible; // Toggle on/off
                    if (isSettingsVisible)
                        isInventoryVisible = false; // Close inventory if opening settings
                    updateInputProcessor();
                    return true;
                }
                if (keycode == Input.Keys.U) {
                    useBatteryFromInventory();
                    return true;
                }
                if (keycode == Input.Keys.P) { // 'P' for Potion
                    usePotionFromInventory();
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
    // Helper Methods
    // =========================================================================
    private void usePotionFromInventory() {
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0) return;

        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);
        final PlayerComponent pc = player.getComponent(PlayerComponent.class);

        if (inv != null && inv.hasItem("potion") && pc != null) {
            inv.items.remove("potion");

            // 1. Set the speed directly (e.g., Normal + 70)
            pc.speed = pc.baseSpeed + 70f;

            stingEffectTimer = STING_DURATION;

            // 2. Schedule the reset
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    // 3. Reset directly to the base speed (Guaranteed to be 170f)
                    pc.speed = pc.baseSpeed;
                    stingEffectTimer = 0;
                    Gdx.app.log("GAME", "Sting wore off. Speed reset to: " + pc.speed);
                }
            }, STING_DURATION);
        }
    }
    public void useBatteryFromInventory() {
        Entity player = getPlayerEntity();
        if (player == null) return;

        BatteryComponent bc = player.getComponent(BatteryComponent.class);
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        // 1. Check if player has the Flashlight item first
        if (inv == null || !inv.hasItem("flashlight")) {
            showWarningLabel("NEED FLASHLIGHT TO RELOAD!");
            return;
        }

        // 2. Check if player has a battery in inventory
        if (!inv.hasItem("battery")) {
            showWarningLabel("NO BATTERIES IN INVENTORY!");
            return;
        }

        // 3. Check if battery is already full
        if (bc != null && bc.battery >= bc.maxBattery) {
            showWarningLabel("FLASHLIGHT IS ALREADY FULL!");
            return;
        }

        // 4. Success: Use battery
        inv.removeItem("battery");
        if (bc != null) bc.battery = bc.maxBattery;
        showWarningLabel("FLASHLIGHT RECHARGED!");
    }

    /** Helper to trigger the warning label for a few seconds */
    public void showWarningLabel(String text) {
        if (noFlashlightWarningLabel != null) {
            noFlashlightWarningLabel.setText(text);
            warningDisplayTimer = WARNING_DURATION; // Uses your existing timer field
        }
    }

    public void showNarrativeDialog(String text) {
        showNarrativeDialog(text, 0f);
    }

    public void showNarrativeDialog(String text, float seconds) {
        if (narrativePanel == null || narrativeLabel == null) return;
        narrativeLabel.setText(text == null ? "" : text);
        narrativePanel.setVisible(true);
        narrativeTimer = Math.max(0f, seconds);
    }

    public void hideNarrativeDialog() {
        if (narrativePanel != null) {
            narrativePanel.setVisible(false);
        }
        narrativeTimer = 0f;
    }

    private Entity getPlayerEntity() {
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0) return null;
        return engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
    }

    private void saveMapLogic() {
        if (engine != null) {
            SaveSystem saveSys = engine.getSystem(SaveSystem.class);
            if (saveSys != null)
                saveSys.triggerManualSave("saveFile.json");
        }
    }

    private void createDimmerTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void updateInputProcessor() {
        if (isGameOver) {
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

    // =========================================================================
    // Render
    // =========================================================================

    public void render(float delta) {
        updateDynamicHUD();
        if (narrativePanel != null && narrativePanel.isVisible() && narrativeTimer > 0f) {
            narrativeTimer -= delta;
            if (narrativeTimer <= 0f) {
                hideNarrativeDialog();
            }
        }
        // Always act and draw uiStage — keeps the jumpscare timer ticking
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
        dimmer.setColor(0, 0, 0, 1f);
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

    // =========================================================================
    // Jumpscare
    // =========================================================================

    public void showJumpscare() {
        isJumpscaring = true;

        if (jumpscareTexture != null)
            jumpscareTexture.dispose();
        jumpscareTexture = new Texture(Gdx.files.internal("ui/jumpscare.jpeg"));

        uiStage.clear();
        isSettingsVisible = false;
        isInventoryVisible = false;

        // 1. Create a solid black background so it doesn't fade into the game world
        Image blackOverlay = new Image(whitePixel);
        blackOverlay.setColor(Color.BLACK);
        blackOverlay.setFillParent(true);
        uiStage.addActor(blackOverlay);

        // 2. Create the jumpscare image
        Image jumpscareImg = new Image(jumpscareTexture);
        jumpscareImg.setFillParent(true);
        uiStage.addActor(jumpscareImg);

        jumpscareSound.play(0.6f);

        // 3. Sequence: Stay full alpha -> Fade out -> Clean up & Game Over
        jumpscareImg.addAction(Actions.sequence(
            Actions.delay(3.0f), // Show jumpscare for 3 seconds
            Actions.fadeOut(1.0f), // Fade to black over 1 second
            Actions.run(() -> {
                isJumpscaring = false;
                if (jumpscareTexture != null) {
                    jumpscareTexture.dispose();
                    jumpscareTexture = null;
                }
                // Reset the guard so showGameOver() rebuilds the UI even if
                // WinLossSystem set gameOver = true in an earlier frame.
                isGameOver = false;
                showGameOver(false); // Trigger the death screen
            })));

        Gdx.input.setInputProcessor(uiStage);
    }

    /**
     * Resets the UI back to the normal in-game HUD (called by Main.resetGame()).
     */
    public void resetUI() {
        isGameOver = false; // Must be first — clears the guard in updateInputProcessor()
        isSettingsVisible = false;
        isInventoryVisible = false;
        isJumpscaring = false;
        uiStage.clear();

        setupHUD();
        setupGlobalListener(); // Re-attach shortcuts (uiStage.clear() removed them)
        updateInputProcessor();
    }

    // =========================================================================
    // Dimmer / Overlay Helpers
    // =========================================================================

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

    private void updateDynamicHUD() {
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0) return;
        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);
        BatteryComponent bat = player.getComponent(BatteryComponent.class);

        int totalExpected = com.sam.TERMINAL.entities.EntitySpawner.totalBeepCardsSpawned;
        int heldCards = 0;
        if (inv != null) {
            heldCards = java.util.Collections.frequency(inv.items, "beep_card");
        }

        if (beepCardLabel != null) {
            beepCardLabel.setText("Beep Cards: " + heldCards + " / " + totalExpected);
        }

        if (bat != null && inv != null && inv.hasItem("flashlight")) {
            if (batteryLabel != null) {
                batteryLabel.setText(String.format("Flashlight Battery: %.0f%%", bat.battery));
                batteryLabel.getStyle().fontColor = (bat.battery <= 15f) ? Color.RED : Color.GREEN;
            }
            // NULL CHECK: lowBatteryWarningLabel
            if (lowBatteryWarningLabel != null) {
                lowBatteryWarningLabel.setVisible(bat.battery <= 15f && bat.battery > 0f);
            }
        } else {
            if (batteryLabel != null) batteryLabel.setText("");
            if (lowBatteryWarningLabel != null) lowBatteryWarningLabel.setVisible(false);
        }

        float delta = Gdx.graphics.getDeltaTime();

        // NULL CHECK: Warning Label Logic
        if (noFlashlightWarningLabel != null) {
            if (warningDisplayTimer > 0) {
                warningDisplayTimer -= delta;
                noFlashlightWarningLabel.setVisible(true);
            } else {
                noFlashlightWarningLabel.setVisible(false);
            }
        }

        // NULL CHECK: Sting Timer Logic
        if (stingTimerLabel != null) {
            if (stingEffectTimer > 0) {
                stingEffectTimer -= delta;
                stingTimerLabel.setText(String.format("Sting Boost: %.1fs", stingEffectTimer));
                stingTimerLabel.setVisible(true);
            } else {
                stingTimerLabel.setVisible(false);
            }
        }
    }

    // =========================================================================
    // Resize / Dispose / Accessors
    // =========================================================================

    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);
        settingsStage.getViewport().update(width, height, true);
        inventoryStage.getViewport().update(width, height, true);
        if (narrativePanel != null && narrativeLabel != null) {
            float vw = uiStage.getViewport().getWorldWidth();
            float vh = uiStage.getViewport().getWorldHeight();
            float panelW = com.badlogic.gdx.math.MathUtils.clamp(vw * 0.82f, 320f, 760f);
            float panelH = com.badlogic.gdx.math.MathUtils.clamp(vh * 0.22f, 130f, 210f);
            narrativePanel.setSize(panelW, panelH);
            narrativePanel.setPosition((vw - panelW) / 2f, 8f);
            narrativePanel.getCell(narrativeLabel).width(panelW - 40f);
        }
    }

    private void setupHUD() {
        uiStage.clear();

        // --- 1. TOP LEFT: Settings ---
        Table topLeftTable = new Table();
        topLeftTable.setFillParent(true);
        topLeftTable.top().left();
        uiStage.addActor(topLeftTable);

        Image settingsBtn = new Image(settingsTexture);
        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isSettingsVisible = true;
                updateInputProcessor();
            }
        });
        topLeftTable.add(settingsBtn).size(40, 40).pad(10);

        // --- 2. BOTTOM CENTER: Inventory & Monster Timer ---
        Table bottomCenterTable = new Table();
        bottomCenterTable.setFillParent(true);
        bottomCenterTable.bottom(); // Align table to bottom
        uiStage.addActor(bottomCenterTable);

        // Initialize the Monster Label
        monsterWarningLabel = new Label("", new Label.LabelStyle(font, Color.YELLOW));
        monsterWarningLabel.setFontScale(2f); // Same scale as stingTimerLabel
        monsterWarningLabel.setVisible(false);

        // Add Label FIRST so it sits ABOVE the inventory button
        bottomCenterTable.add(monsterWarningLabel).padBottom(20).row();

        ImageButton inventoryBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(invTexture)));
        inventoryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = true;
                updateInputProcessor();
            }
        });
        // Add Inventory Button SECOND
        bottomCenterTable.add(inventoryBtn).size(55, 55).padBottom(5);

        // --- 3. TOP RIGHT: Stats & Warnings ---
        Table topRightTable = new Table();
        topRightTable.setFillParent(true);
        topRightTable.top().right();
        uiStage.addActor(topRightTable);

        beepCardLabel = new Label("Beep Cards: 0 / 0", new Label.LabelStyle(font, Color.WHITE));
        batteryLabel = new Label("", new Label.LabelStyle(font, Color.GREEN));

        lowBatteryWarningLabel = new Label("LOW BATTERY", new Label.LabelStyle(font, Color.RED));
        lowBatteryWarningLabel.setVisible(false);

        noFlashlightWarningLabel = new Label("", new Label.LabelStyle(font, Color.ORANGE));
        noFlashlightWarningLabel.setVisible(false);

        topRightTable.add(beepCardLabel).right().padRight(20).padTop(10).row();
        topRightTable.add(batteryLabel).right().padRight(20).padTop(10).row();
        topRightTable.add(lowBatteryWarningLabel).right().padRight(20).row();
        topRightTable.add(noFlashlightWarningLabel).right().padRight(20).padTop(5).row();

        // --- 4. BOTTOM RIGHT: Boost Timers ---
        Table bottomRightTable = new Table();
        bottomRightTable.setFillParent(true);
        bottomRightTable.bottom().right();
        uiStage.addActor(bottomRightTable);

        stingTimerLabel = new Label("", new Label.LabelStyle(font, Color.CYAN));
        stingTimerLabel.setFontScale(2f); // Ensuring size matches
        stingTimerLabel.setVisible(false);
        bottomRightTable.add(stingTimerLabel).right().padRight(20).padBottom(20);

        // --- 5. Narrative Dialogue (SubmenuPanel-style, bottom center) ---
        narrativePanel = new SubmenuPanel(18f);
        narrativeLabel = new Label("", new Label.LabelStyle(font, Color.WHITE));
        narrativeLabel.setWrap(true);
        narrativeLabel.setAlignment(Align.topLeft);
        narrativePanel.add(narrativeLabel).width(520f).left().top();
        narrativePanel.setVisible(false);
        narrativePanel.setSize(560f, 150f);
        narrativePanel.setPosition((uiStage.getViewport().getWorldWidth() - 560f) / 2f, 8f);
        uiStage.addActor(narrativePanel);
    }

    private BitmapFont loadUIFont(String path, int size) {
        if (Gdx.files.internal(path).exists()) {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(path));
            FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = size;
            BitmapFont generated = gen.generateFont(param);
            gen.dispose();
            return generated;
        }
        BitmapFont fallback = new BitmapFont();
        fallback.getData().setScale(size / 16f);
        return fallback;
    }

    // =========================================================================
    // Inventory Refresh
    // =========================================================================

    private void refreshInventory() {
        itemTable.clearChildren();

        Entity player = getPlayerEntity();
        if (player == null) return;
        InventoryComponent inv = player.getComponent(InventoryComponent.class);
        if (inv == null) return;

        // Define a consistent style for the labels
        Label.LabelStyle itemLabelStyle = new Label.LabelStyle(new BitmapFont(), Color.WHITE);
        itemLabelStyle.font.getData().setScale(1.2f);

        int itemsInRow = 0;
        int maxColumns = 2; // Change this to 3 if your window is wide enough

        // Check for each item type and add them
        if (inv.hasItem("beep_card")) {
            addItemToTable("Beep Card", mainGame.getBeepRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0) itemTable.row();
        }

        if (inv.hasItem("flashlight")) {
            addItemToTable("Flashlight", mainGame.getFlashlightRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0) itemTable.row();
        }

        if (inv.hasItem("battery")) {
            addItemToTable("Battery", mainGame.getBatteryRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0) itemTable.row();
        }

        if (inv.hasItem("potion")) {
            addItemToTable("Sting", mainGame.getPotionRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0) itemTable.row();
        }

        itemTable.invalidateHierarchy();
    }

    /** * Helper method to create a consistent "Slot" for each item
     */
    private void addItemToTable(String name, TextureRegion region, Label.LabelStyle style) {
        Table slot = new Table();

        Image icon = new Image(region);
        icon.setScaling(com.badlogic.gdx.utils.Scaling.fit); // Prevent stretching

        slot.add(icon).size(64, 64).pad(5);
        slot.add(new Label(name, style)).padLeft(10).padRight(20);

        // Add the whole slot to the main itemTable
        itemTable.add(slot).pad(10).left();
    }


    public void updateMonsterTimer(int seconds) {
        if (monsterWarningLabel != null) {
            monsterWarningLabel.setVisible(true);
            monsterWarningLabel.setText("A monster will enter the station in " + seconds + "s");
        }
    }

    public void hideMonsterTimer() {
        if (monsterWarningLabel != null) {
            monsterWarningLabel.setVisible(false);
        }
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

        // BUG 3 FIX: dispose SettingsButton textures via stored reference.
        if (settingsButtonWidget != null) {
            settingsButtonWidget.dispose();
        }
        if (jumpscareTexture != null)
            jumpscareTexture.dispose();
        if (jumpscareSound != null)
            jumpscareSound.dispose();
        if (inventoryButtonWidget != null) {
            inventoryButtonWidget.dispose();
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

    public boolean isJumpscaring() {
        return isJumpscaring;
    }

    /** Restores the correct input processor based on current overlay state. */
    public void reapplyInputProcessor() {
        updateInputProcessor();
    }
}
