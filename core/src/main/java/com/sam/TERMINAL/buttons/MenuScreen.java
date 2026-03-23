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
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import com.badlogic.gdx.audio.Sound;

public class MenuScreen {
    private Stage uiStage;
    private Stage settingsStage;
    private Stage inventoryStage;
    private Texture settingsTexture, backTexture, whitePixel, invTexture;
    private Texture restartTexture;
    private boolean isSettingsVisible = false;
    private boolean isInventoryVisible = false;
    private boolean isGameOver = false;
    private boolean isJumpscaring = false;
    private BitmapFont font;
    private Table inventoryWindow;
    private Table itemTable;
    private Table bottomTable;
    private PooledEngine engine;
    private Main mainGame;
    private Texture jumpscareTexture;
    private Sound jumpscareSound; // ← sound field

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
        jumpscareSound = Gdx.audio.newSound(Gdx.files.internal("sfx/jumpscare.mp3")); // ← load sound

        createDimmerTexture();

        // --- HUD SETUP ---
        Table mainRoot = new Table();
        mainRoot.setFillParent(true);
        mainRoot.top().left();

        settingsTexture = new Texture(Gdx.files.internal("ui/settings.png"));
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

        // --- SETTINGS WINDOW SETUP ---
        Table settingsRoot = new Table();
        settingsRoot.setFillParent(true);
        settingsStage.addActor(settingsRoot);

        new SettingsButton(settingsRoot,
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
            }
        );

        // --- INVENTORY SETUP ---
        invTexture = new Texture(Gdx.files.internal("ui/inventory.png"));
        ImageButton inventoryBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(invTexture)));
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
        InputListener globalListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.TAB) {
                    isInventoryVisible = !isInventoryVisible;
                    if (isInventoryVisible) isSettingsVisible = false;
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
        uiStage.removeListener(globalListener);
        settingsStage.removeListener(globalListener);
        inventoryStage.removeListener(globalListener);

        uiStage.addListener(globalListener);
        settingsStage.addListener(globalListener);
        inventoryStage.addListener(globalListener);
    }

    private void saveMapLogic() {
        if (engine != null) {
            SaveSystem saveSys = engine.getSystem(SaveSystem.class);
            if (saveSys != null) saveSys.triggerManualSave("saveFile.json");
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
        // Always act and draw uiStage — keeps the jumpscare timer ticking
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

    private void drawInventoryDim(Stage stage) {
        stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
        stage.getBatch().begin();
        stage.getBatch().setColor(0, 0, 0, 0.5f);
        stage.getBatch().draw(whitePixel, (stage.getViewport().getWorldWidth() - 500) / 2, (stage.getViewport().getWorldHeight() - 400) / 2, 500, 400);
        stage.getBatch().setColor(1, 1, 1, 1);
        stage.getBatch().end();
    }

    private void drawDim(Stage stage) {
        stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
        stage.getBatch().begin();
        stage.getBatch().setColor(0, 0, 0, 0.6f);
        stage.getBatch().draw(whitePixel, 0, 0, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.getBatch().setColor(1, 1, 1, 1);
        stage.getBatch().end();
    }

    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);
        settingsStage.getViewport().update(width, height, true);
        inventoryStage.getViewport().update(width, height, true);
    }

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

        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom();

        ImageButton inventoryBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(invTexture)));
        inventoryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = true;
                updateInputProcessor();
            }
        });

        bottomTable.add(inventoryBtn).size(55, 55).padBottom(5);
        uiStage.addActor(bottomTable);
    }

    public void dispose() {
        uiStage.dispose(); settingsStage.dispose(); inventoryStage.dispose();
        settingsTexture.dispose(); whitePixel.dispose(); invTexture.dispose();
        restartTexture.dispose();
        font.dispose();
        if (jumpscareTexture != null) jumpscareTexture.dispose();
        if (jumpscareSound != null) jumpscareSound.dispose(); // ← dispose sound
    }

    public boolean isSettingsVisible() { return isSettingsVisible; }
    public boolean isInventoryVisible() { return isInventoryVisible; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isJumpscaring() { return isJumpscaring; }

    private void refreshInventory() {
        itemTable.clearChildren();

        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0) return;
        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        if (inv != null && inv.hasItem("beep_card")) {
            Image icon = new Image(mainGame.getBeepRegion());
            itemTable.add(icon).size(64, 64).pad(20);
            itemTable.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Beep Card", new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(new com.badlogic.gdx.graphics.g2d.BitmapFont(), com.badlogic.gdx.graphics.Color.WHITE))).padRight(20);
            itemTable.invalidateHierarchy();
        }
    }

    public void showGameOver(boolean win) {
        if (isGameOver) return;
        isGameOver = true;

        uiStage.clear();
        isSettingsVisible = false;
        isInventoryVisible = false;

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

        ImageButton restartBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(restartTexture)));
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

        Image jumpscareImg = new Image(jumpscareTexture);
        jumpscareImg.setFillParent(true);
        uiStage.addActor(jumpscareImg);

        jumpscareSound.play(1.0f); // ← play sound at full volume

        // Action on the ACTOR not the stage — actors process actions, stages don't
        jumpscareImg.addAction(Actions.sequence(
            Actions.delay(3f),
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
}
