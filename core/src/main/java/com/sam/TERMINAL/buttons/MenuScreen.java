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

    private boolean isSettingsVisible = false;
    private boolean isInventoryVisible = false;
    private boolean isGameOver = false;
    private boolean isJumpscaring = false;
    private boolean isConfrontationVisible = false;

    private BitmapFont font;
    private Table inventoryWindow;
    private Table itemTable;
    private Table bottomTable;

    private Label beepCardLabel;
    private Label batteryLabel;
    private Label lowBatteryWarningLabel;
    private Label monsterWarningLabel;

    private final PooledEngine engine;
    private final Main mainGame;

    private Sound jumpscareSound;
    private InputListener globalListener;

    private SettingsButton settingsButtonWidget;
    private InventoryButton inventoryButtonWidget;

    private Label stingTimerLabel;
    private Label noFlashlightWarningLabel;

    // Narrative Fields (garc)
    private SubmenuPanel narrativePanel;
    private Label narrativeLabel;
    private float narrativeTimer = 0f;

    private float stingEffectTimer = 0f;
    private float warningDisplayTimer = 0f;

    private final float STING_DURATION = 10f;
    private final float WARNING_DURATION = 3f;

    private BitmapFont bodyFont;
    private BitmapFont terminalFont;

    public MenuScreen(SpriteBatch batch, final PooledEngine engine, final Main mainGame) {
        this.engine = engine;
        this.mainGame = mainGame;
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        uiStage = new Stage(new ExtendViewport(w, h), batch);
        settingsStage = new Stage(new ExtendViewport(w, h), batch);
        inventoryStage = new Stage(new ExtendViewport(w, h), batch);

        font = loadUIFont("fonts/Abaddon Light.ttf", 28);
        bodyFont = loadUIFont("fonts/Abaddon Light.ttf", 34);
        terminalFont = loadUIFont("fonts/BIOSfontII.ttf", 76);

        restartTexture = new Texture(Gdx.files.internal("ui/Restart.png"));
        settingsTexture = new Texture(Gdx.files.internal("ui/settings.png"));
        invTexture = new Texture(Gdx.files.internal("ui/inventory.png"));
        jumpscareSound = Gdx.audio.newSound(Gdx.files.internal("sfx/jumpscare.mp3"));

        createDimmerTexture();
        setupHUD();

        Table settingsRoot = new Table();
        settingsRoot.setFillParent(true);
        settingsStage.addActor(settingsRoot);

        settingsButtonWidget = new SettingsButton(
                settingsRoot,
                () -> {
                    isSettingsVisible = false;
                    updateInputProcessor();
                },
                () -> {
                    saveMapLogic();
                },
                () -> {
                    mainGame.resetGame();
                    isSettingsVisible = false;
                    updateInputProcessor();
                });

        Table inventoryRoot = new Table();
        inventoryRoot.setFillParent(true);
        inventoryStage.addActor(inventoryRoot);
        inventoryWindow = new Table();
        TextureRegionDrawable windowBg = new TextureRegionDrawable(new TextureRegion(whitePixel));
        inventoryWindow.setBackground(windowBg.tint(new com.badlogic.gdx.graphics.Color(0.8f, 0.8f, 0.8f, 0.15f)));
        inventoryRoot.center().add(inventoryWindow).size(500, 400);

        new InventoryButton(inventoryWindow, () -> {
            isInventoryVisible = false;
            updateInputProcessor();
        });
        inventoryWindow.getCells().peek().left().expandX();
        inventoryWindow.row();

        itemTable = new Table();
        itemTable.top().left();
        inventoryWindow.add(itemTable).expand().fill();

        setupGlobalListener();
        updateInputProcessor();
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
                    isSettingsVisible = !isSettingsVisible;
                    if (isSettingsVisible)
                        isInventoryVisible = false;
                    updateInputProcessor();
                    return true;
                }
                if (keycode == Input.Keys.U) {
                    useBatteryFromInventory();
                    return true;
                }
                if (keycode == Input.Keys.P) {
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

    private void usePotionFromInventory() {
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0)
            return;

        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);
        final PlayerComponent pc = player.getComponent(PlayerComponent.class);

        if (inv != null && inv.hasItem("potion") && pc != null) {
            inv.items.remove("potion");
            pc.speed = pc.baseSpeed + 70f;
            stingEffectTimer = STING_DURATION;

            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    pc.speed = pc.baseSpeed;
                    stingEffectTimer = 0;
                    Gdx.app.log("GAME", "Sting wore off. Speed reset to: " + pc.speed);
                }
            }, STING_DURATION);
        }
    }

    public void useBatteryFromInventory() {
        Entity player = getPlayerEntity();
        if (player == null)
            return;

        BatteryComponent bc = player.getComponent(BatteryComponent.class);
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        if (inv == null || !inv.hasItem("flashlight")) {
            showWarningLabel("NEED FLASHLIGHT TO RELOAD!");
            return;
        }
        if (!inv.hasItem("battery")) {
            showWarningLabel("NO BATTERIES IN INVENTORY!");
            return;
        }
        if (bc != null && bc.battery >= bc.maxBattery) {
            showWarningLabel("FLASHLIGHT IS ALREADY FULL!");
            return;
        }

        inv.removeItem("battery");
        if (bc != null)
            bc.battery = bc.maxBattery;
        showWarningLabel("FLASHLIGHT RECHARGED!");
    }

    public void showWarningLabel(String text) {
        if (noFlashlightWarningLabel != null) {
            noFlashlightWarningLabel.setText(text);
            warningDisplayTimer = WARNING_DURATION;
        }
    }

    public void showNarrativeDialog(String text) {
        showNarrativeDialog(text, 0f);
    }

    public void showNarrativeDialog(String text, float seconds) {
        if (narrativePanel == null || narrativeLabel == null)
            return;
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
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0)
            return null;
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

    public void render(float delta) {
        updateDynamicHUD();
        if (narrativePanel != null && narrativePanel.isVisible() && narrativeTimer > 0f) {
            narrativeTimer -= delta;
            if (narrativeTimer <= 0f) {
                hideNarrativeDialog();
            }
        }

        uiStage.act(delta);
        uiStage.draw();

        if (isConfrontationVisible) {
            drawDim(settingsStage);
            settingsStage.act(delta);
            settingsStage.draw();
        } else if (isSettingsVisible) {
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

    public void showGameOver(final boolean win) {
        if (isGameOver)
            return;
        isGameOver = true;
        isSettingsVisible = false;
        isInventoryVisible = false;
        uiStage.clear();

        final Image dimmer = new Image(whitePixel);
        dimmer.setColor(Color.BLACK);
        dimmer.getColor().a = 1f;
        dimmer.setFillParent(true);
        uiStage.addActor(dimmer);

        final Table table = new Table();
        table.setFillParent(true);
        table.center();
        uiStage.addActor(table);

        String text = win ? "YOU ESCAPED!" : "YOU DIED";
        Color color = win ? Color.GREEN : Color.RED;
        Label.LabelStyle style = new Label.LabelStyle(font, color);
        Label label = new Label(text, style);

        ImageButton actionBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(restartTexture)));

        actionBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (win) {
                    table.clearChildren();
                    Label.LabelStyle loadingStyle = new Label.LabelStyle(bodyFont, Color.WHITE);
                    Label loadingLabel = new Label("Loading...", loadingStyle);
                    table.add(loadingLabel).center();

                    loadingLabel.addAction(Actions.sequence(
                            Actions.delay(5.0f),
                            Actions.fadeOut(1.0f)));

                    dimmer.addAction(Actions.sequence(
                            Actions.delay(5.0f),
                            Actions.fadeOut(1.5f),
                            Actions.run(() -> {
                                Gdx.app.log("TERMINAL", "Transition complete. Revealing Level 2.");
                                mainGame.loadLevelTwo();
                            })));
                } else {
                    mainGame.resetGame();
                }
            }
        });

        table.add(label).padBottom(20).row();
        table.add(actionBtn).size(64, 64);

        updateInputProcessor();
        uiStage.setKeyboardFocus(actionBtn);
    }

    /**
     * Shows one of the three ending screens: "neutral", "bad", or "good".
     * Each displays a full-screen overlay with the ending label and a restart button.
     */
    public void showEndScreen(String endingType) {
        if (isGameOver)
            return;
        isGameOver = true;
        isSettingsVisible = false;
        isInventoryVisible = false;
        isConfrontationVisible = false;
        uiStage.clear();

        Image dimmer = new Image(whitePixel);
        dimmer.setColor(Color.BLACK);
        dimmer.getColor().a = 1f;
        dimmer.setFillParent(true);
        uiStage.addActor(dimmer);

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        uiStage.addActor(table);

        String text;
        Color color;
        switch (endingType) {
            case "bad":
                text = "BAD END\nThe Curse Continues";
                color = Color.RED;
                Gdx.app.log("TERMINAL", "[BAD END] You destroyed the spirit. Another will take its place.");
                break;
            case "good":
                text = "GOOD END\nSouls At Rest";
                color = Color.GREEN;
                Gdx.app.log("TERMINAL", "[GOOD END] You showed mercy. The ghost is finally at peace.");
                break;
            default:
                text = "NEUTRAL END\nYou Left Them Behind";
                color = Color.WHITE;
                Gdx.app.log("TERMINAL", "[NEUTRAL END] You escaped, but the ghost remains trapped forever.");
                break;
        }

        Label.LabelStyle style = new Label.LabelStyle(font, color);
        Label label = new Label(text, style);
        label.setAlignment(Align.center);
        label.setFontScale(2f);

        ImageButton restartBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(restartTexture)));
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mainGame.resetGame();
            }
        });

        table.add(label).padBottom(30).row();
        table.add(restartBtn).size(64, 64);

        updateInputProcessor();
        uiStage.setKeyboardFocus(restartBtn);
    }

    /**
     * Shows the Confrontation UI with two options:
     * - Kill Ghost (always clickable → Bad End)
     * - Show Mercy (locked unless player has studID AND papers → Good End)
     */
    public void showConfrontation() {
        isConfrontationVisible = true;
        isSettingsVisible = false;
        isInventoryVisible = false;

        // Clear the settings stage and re-use it for the confrontation overlay
        settingsStage.clear();

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        settingsStage.addActor(root);

        // Panel background
        TextureRegionDrawable panelBg = new TextureRegionDrawable(new TextureRegion(whitePixel));
        Table panel = new Table();
        panel.setBackground(panelBg.tint(new Color(0.1f, 0.1f, 0.1f, 0.9f)));
        root.add(panel).width(420).height(280);

        // Title
        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.YELLOW);
        Label titleLabel = new Label("CONFRONTATION", titleStyle);
        titleLabel.setFontScale(1.5f);
        panel.add(titleLabel).padTop(20).padBottom(20).colspan(1).center().row();

        // Kill Ghost button — always available
        Label.LabelStyle killStyle = new Label.LabelStyle(font, Color.RED);
        Label killLabel = new Label("[ Kill Ghost ]", killStyle);
        killLabel.setFontScale(1.2f);
        killLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isConfrontationVisible = false;
                showEndScreen("bad");
            }
        });
        panel.add(killLabel).padBottom(15).center().row();

        // Show Mercy button — conditional on studID + papers
        boolean hasStudID = false;
        boolean hasPapers = false;
        Entity player = getPlayerEntity();
        if (player != null) {
            InventoryComponent inv = player.getComponent(InventoryComponent.class);
            if (inv != null) {
                hasStudID = inv.hasItem("studID");
                hasPapers = inv.hasItem("papers");
            }
        }

        boolean mercyUnlocked = hasStudID && hasPapers;
        Color mercyColor = mercyUnlocked ? Color.GREEN : Color.GRAY;
        Label.LabelStyle mercyStyle = new Label.LabelStyle(font, mercyColor);
        Label mercyLabel = new Label("[ Show Mercy ]", mercyStyle);
        mercyLabel.setFontScale(1.2f);

        if (mercyUnlocked) {
            mercyLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    isConfrontationVisible = false;
                    showEndScreen("good");
                }
            });
        }
        panel.add(mercyLabel).padBottom(10).center().row();

        if (!mercyUnlocked) {
            Label.LabelStyle hintStyle = new Label.LabelStyle(font, Color.DARK_GRAY);
            Label hintLabel = new Label("Requires Student ID and Papers", hintStyle);
            hintLabel.setFontScale(0.8f);
            panel.add(hintLabel).padBottom(10).center().row();
        }

        // Set input to the confrontation stage
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(settingsStage);
        multiplexer.addProcessor(uiStage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    public void showJumpscare() {
        isJumpscaring = true;
        if (jumpscareTexture != null)
            jumpscareTexture.dispose();
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

        jumpscareSound.play(0.6f);

        jumpscareImg.addAction(Actions.sequence(
                Actions.delay(3.0f),
                Actions.fadeOut(1.0f),
                Actions.run(() -> {
                    isJumpscaring = false;
                    if (jumpscareTexture != null) {
                        jumpscareTexture.dispose();
                        jumpscareTexture = null;
                    }
                    isGameOver = false;
                    showGameOver(false);
                })));

        Gdx.input.setInputProcessor(uiStage);
    }

    public void resetUI() {
        isGameOver = false;
        isSettingsVisible = false;
        isInventoryVisible = false;
        isJumpscaring = false;
        isConfrontationVisible = false;
        uiStage.clear();

        setupHUD();
        setupGlobalListener();
        updateInputProcessor();
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

    private void updateDynamicHUD() {
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0)
            return;
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
            boolean showCards = mainGame.getCurrentLevel() != 1 || mainGame.isLilyTriggered();
            beepCardLabel.setVisible(showCards);
        }

        if (bat != null && inv != null && inv.hasItem("flashlight")) {
            if (batteryLabel != null) {
                batteryLabel.setText(String.format("Flashlight Battery: %.0f%%", bat.battery));
                batteryLabel.getStyle().fontColor = (bat.battery <= 15f) ? Color.RED : Color.GREEN;
            }
            if (lowBatteryWarningLabel != null) {
                lowBatteryWarningLabel.setVisible(bat.battery <= 15f && bat.battery > 0f);
            }
        } else {
            if (batteryLabel != null)
                batteryLabel.setText("");
            if (lowBatteryWarningLabel != null)
                lowBatteryWarningLabel.setVisible(false);
        }

        float delta = Gdx.graphics.getDeltaTime();

        if (noFlashlightWarningLabel != null) {
            if (warningDisplayTimer > 0) {
                warningDisplayTimer -= delta;
                noFlashlightWarningLabel.setVisible(true);
            } else {
                noFlashlightWarningLabel.setVisible(false);
            }
        }

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

        Table bottomCenterTable = new Table();
        bottomCenterTable.setFillParent(true);
        bottomCenterTable.bottom();
        uiStage.addActor(bottomCenterTable);

        monsterWarningLabel = new Label("", new Label.LabelStyle(font, Color.YELLOW));
        monsterWarningLabel.setFontScale(1.1f);
        monsterWarningLabel.setVisible(false);
        bottomCenterTable.add(monsterWarningLabel).padBottom(20).row();

        ImageButton inventoryBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(invTexture)));
        inventoryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = true;
                updateInputProcessor();
            }
        });
        bottomCenterTable.add(inventoryBtn).size(55, 55).padBottom(5);

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

        Table bottomRightTable = new Table();
        bottomRightTable.setFillParent(true);
        bottomRightTable.bottom().right();
        uiStage.addActor(bottomRightTable);

        stingTimerLabel = new Label("", new Label.LabelStyle(font, Color.CYAN));
        stingTimerLabel.setFontScale(2f);
        stingTimerLabel.setVisible(false);
        bottomRightTable.add(stingTimerLabel).right().padRight(20).padBottom(20);

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

    private void refreshInventory() {
        itemTable.clearChildren();

        Entity player = getPlayerEntity();
        if (player == null)
            return;
        InventoryComponent inv = player.getComponent(InventoryComponent.class);
        if (inv == null)
            return;

        Label.LabelStyle itemLabelStyle = new Label.LabelStyle(new BitmapFont(), Color.WHITE);
        itemLabelStyle.font.getData().setScale(1.2f);

        int itemsInRow = 0;
        int maxColumns = 2;

        if (inv.hasItem("beep_card")) {
            addItemToTable("Beep Card", mainGame.getBeepRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        if (inv.hasItem("flashlight")) {
            addItemToTable("Flashlight", mainGame.getFlashlightRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        if (inv.hasItem("battery")) {
            addItemToTable("Battery", mainGame.getBatteryRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        if (inv.hasItem("potion")) {
            addItemToTable("Sting", mainGame.getPotionRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        if (inv.hasItem("lily")) {
            addItemToTable("Lily", mainGame.getLilyRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        if (inv.hasItem("studID")) {
            addItemToTable("Student ID", mainGame.getStudIDRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        if (inv.hasItem("papers")) {
            addItemToTable("Papers", mainGame.getPapersRegion(), itemLabelStyle);
            itemsInRow++;
            if (itemsInRow % maxColumns == 0)
                itemTable.row();
        }

        itemTable.invalidateHierarchy();
    }

    private void addItemToTable(String name, TextureRegion region, Label.LabelStyle style) {
        Table slot = new Table();
        Image icon = new Image(region);
        icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);

        slot.add(icon).size(64, 64).pad(5);
        slot.add(new Label(name, style)).padLeft(10).padRight(20);

        itemTable.add(slot).pad(10).left();
    }

    public void updateMonsterTimer(int seconds) {
        if (monsterWarningLabel != null) {
            monsterWarningLabel.setVisible(true);
            monsterWarningLabel.setText("Something's Coming " + seconds + "s");
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
        if (bodyFont != null)
            bodyFont.dispose();
        if (terminalFont != null)
            terminalFont.dispose();
        if (settingsButtonWidget != null)
            settingsButtonWidget.dispose();
        if (jumpscareTexture != null)
            jumpscareTexture.dispose();
        if (jumpscareSound != null)
            jumpscareSound.dispose();
        if (inventoryButtonWidget != null)
            inventoryButtonWidget.dispose();
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

    public void reapplyInputProcessor() {
        updateInputProcessor();
    }
}