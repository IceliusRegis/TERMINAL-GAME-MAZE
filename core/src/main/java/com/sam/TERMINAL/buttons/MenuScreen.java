package com.sam.TERMINAL.buttons;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine; // Added
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.sam.TERMINAL.systems.EnemySystem;
import com.sam.TERMINAL.systems.SaveSystem; // Added
import com.sam.TERMINAL.Main;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;


public class MenuScreen {
    private Stage uiStage;
    private Stage settingsStage;
    private Stage inventoryStage;
    private Texture settingsTexture, backTexture, whitePixel, invTexture;
    private Texture restartTexture;
    private boolean isSettingsVisible = false;
    private boolean isInventoryVisible = false;
    private boolean isGameOver = false;
    private BitmapFont font;
    private Table inventoryWindow;
    private Table bottomTable;
    private PooledEngine engine; // Reference to trigger saves
    private Main mainGame;

    // Constructor now takes only SpriteBatch and PooledEngine
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

        // Updated SettingsButton call: onClose, onRefresh (empty), onSave
        // Inside MenuScreen constructor
        new SettingsButton(settingsRoot,
            () -> { // onClose
                isSettingsVisible = false;
                updateInputProcessor();
            },
            () -> { // onSave
                saveMapLogic();
            },
            () -> { //onReset
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
                refreshInventory();
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

        // --- SHORTCUTS (TAB & F5) ---
        InputListener globalListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.TAB) {
                    isInventoryVisible = !isInventoryVisible;
                    if (isInventoryVisible) isSettingsVisible = false;
                    refreshInventory();
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

        updateInputProcessor();
    }

    //GAME OVERRR


    private void saveMapLogic() {
        if (engine != null) {
            SaveSystem saveSys = engine.getSystem(SaveSystem.class);
            if (saveSys != null) saveSys.triggerManualSave("saveFile.json");
        }
    }

    // ... (createDimmerTexture, updateInputProcessor, render, resize, dispose remain the same)
    private void createDimmerTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void updateInputProcessor() {
        if (isGameOver) {
            Gdx.input.setInputProcessor(uiStage); // Only allow clicking Restart
        } else if (isSettingsVisible) {
            Gdx.input.setInputProcessor(settingsStage);
        } else if (isInventoryVisible) {
            com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
            multiplexer.addProcessor(inventoryStage);
            Gdx.input.setInputProcessor(multiplexer);
        } else {
            Gdx.input.setInputProcessor(uiStage);
        }
    }

    public void render(float delta) {
        uiStage.act(delta);
        uiStage.draw();
        if (isSettingsVisible) {
            drawDim(settingsStage);
            settingsStage.act(delta);
            settingsStage.draw();
        } else if (isInventoryVisible) {
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

    public void dispose() {
        uiStage.dispose(); settingsStage.dispose(); inventoryStage.dispose();
        settingsTexture.dispose(); whitePixel.dispose(); invTexture.dispose();
    }

    public boolean isSettingsVisible() { return isSettingsVisible; }
    public boolean isInventoryVisible() { return isInventoryVisible; }

    private void refreshInventory() {
        inventoryWindow.clearChildren();

        //Re ADDS Exit butt
        new InventoryButton(inventoryWindow, () -> {isInventoryVisible = false; updateInputProcessor();});
        inventoryWindow.row();

        //Check Items
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0) return;
        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        //DRAW PLEASEE
        if (inv != null && inv.hasItem("beep_card")) {
            Image icon = new Image(mainGame.getBeepRegion());
            inventoryWindow.add(icon).size(64, 64).pad(20);
            inventoryWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Beep Card", new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(new com.badlogic.gdx.graphics.g2d.BitmapFont(), com.badlogic.gdx.graphics.Color.WHITE)));
        }
    }

    // 1. Shows the Win/Lose screen
    public void showGameOver(boolean win) {
        if (isGameOver) return;
        isGameOver = true;

        uiStage.clear(); // Wipes the Settings/Inventory buttons
        isSettingsVisible = false;
        isInventoryVisible = false;

        // Dark Background
        Image dimmer = new Image(whitePixel);
        dimmer.setColor(0, 0, 0, 0.8f);
        dimmer.setFillParent(true);
        uiStage.addActor(dimmer);

        // Center Table for Text + Button
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // "YOU DIED" or "YOU ESCAPED"
        String text = win ? "YOU ESCAPED!" : "YOU DIED";
        Color color = win ? Color.GREEN : Color.RED;
        Label.LabelStyle style = new Label.LabelStyle(font, color);
        Label label = new Label(text, style);

        // Restart Button
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
    }

    // 2. Resets the UI back to normal
    public void resetUI() {
        isGameOver = false;
        uiStage.clear();

        // RE-ADD YOUR HUD BUTTONS HERE (Settings & Inventory)
        // (Copy the HUD setup code from your constructor and paste it here,
        //  or move that code into a private setupHUD() method and call it.)

        // ... Re-add Settings Button code ...
        // ... Re-add Inventory Button code ...

        updateInputProcessor();
    }

    public boolean isGameOver() { return isGameOver; }





}
