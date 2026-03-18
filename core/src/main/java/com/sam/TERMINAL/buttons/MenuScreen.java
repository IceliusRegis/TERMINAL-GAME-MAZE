package com.sam.TERMINAL.buttons;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine; // Added
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
import com.sam.TERMINAL.systems.EnemySystem;
import com.sam.TERMINAL.systems.SaveSystem; // Added
import com.sam.TERMINAL.Main;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;


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
    private Table itemTable;
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

        // B-6 Fix: Removed refreshInventory() from ClickListener — it now runs in render()
        inventoryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = true;
                updateInputProcessor();       // refreshInventory() now runs in render()
            }
        });

        Table inventoryRoot = new Table();
        inventoryRoot.setFillParent(true);
        inventoryStage.addActor(inventoryRoot);
        inventoryWindow = new Table();
        TextureRegionDrawable windowBg = new TextureRegionDrawable(new TextureRegion(whitePixel));
        inventoryWindow.setBackground(windowBg.tint(new com.badlogic.gdx.graphics.Color(0.8f, 0.8f, 0.8f, 0.15f)));
        inventoryRoot.center().add(inventoryWindow).size(500, 400);

        // Stable exit button — added once, never cleared by refreshInventory()
        // We wrap it in a cell and align it to the left.
        new InventoryButton(inventoryWindow, () -> {
            isInventoryVisible = false;
            updateInputProcessor();
        });
        
        // Ensure the cell containing the exit button aligns left
        inventoryWindow.getCells().peek().left().expandX();
        inventoryWindow.row();

        // Sub-table for item rows — only this gets cleared every frame
        // Align the item table to the top-left so items flow left-to-right naturally
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
        // Re-adding a listener doesn't duplicate it if we clear() first, but to be safe:
        uiStage.removeListener(globalListener); 
        settingsStage.removeListener(globalListener);
        inventoryStage.removeListener(globalListener);
        
        uiStage.addListener(globalListener);
        settingsStage.addListener(globalListener);
        inventoryStage.addListener(globalListener);
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
        // B-5 GUARD: If the game-over screen is showing, it owns input exclusively.
        // No other state should be able to override this.
        if (isGameOver) {
            Gdx.input.setInputProcessor(uiStage);
            return;
        }

        // B-7 Fix: Settings branch now wraps uiStage + settingsStage in a multiplexer
        if (isSettingsVisible) {
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(uiStage);         // First: global HUD keys
            multiplexer.addProcessor(settingsStage);    // Second: clicks on settings actors
            Gdx.input.setInputProcessor(multiplexer);
        } else if (isInventoryVisible) {
            // B-7 Fix: Inventory branch now includes uiStage for TAB/global keys
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(uiStage);        // First: global HUD keys (TAB to close, etc.)
            multiplexer.addProcessor(inventoryStage); // Second: clicks on inventory window actors
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
            // B-6 Fix: Refresh inventory from live ECS state every frame the panel is visible
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
        uiStage.clear(); // 1. Clean the slate

        // 2. Create Top-Left Settings Button
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

        // 3. Create Bottom-Center Inventory Button
        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom();

        ImageButton inventoryBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(invTexture)));
        // B-6 Fix: No refreshInventory() in ClickListener — handled in render()
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
    }

    public boolean isSettingsVisible() { return isSettingsVisible; }
    public boolean isInventoryVisible() { return isInventoryVisible; }

    private void refreshInventory() {
        // Only clear the item rows — the exit button lives in inventoryWindow and is never touched
        itemTable.clearChildren();

        //Check Items
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).size() == 0) return;
        Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first();
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        //DRAW PLEASEE
        if (inv != null && inv.hasItem("beep_card")) {
            Image icon = new Image(mainGame.getBeepRegion());
            
            // Note: Expand X stops this single item from sticking exactly to the left edge if it's the only one,
            // but since itemTable is left-aligned, it will start on the left.
            itemTable.add(icon).size(64, 64).pad(20);
            itemTable.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Beep Card", new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(new com.badlogic.gdx.graphics.g2d.BitmapFont(), com.badlogic.gdx.graphics.Color.WHITE))).padRight(20);
        }
    }

    // 1. Shows the Win/Lose screen
    public void showGameOver(boolean win) {
        if (isGameOver) return;
        isGameOver = true;                  // 1. Set flag first

        uiStage.clear();                    // 2. Wipe old HUD
        isSettingsVisible = false;          // 3. Reset sub-states
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

        uiStage.addActor(table);            // 5. Add restart button to stage
        updateInputProcessor();             // 6. Transfer input LAST, after stage is built

        // B-5 Fix: Set keyboard focus on restart button so ENTER/SPACE works
        uiStage.setKeyboardFocus(restartBtn);
    }

    // 2. Resets the UI back to normal
    public void resetUI() {
        isGameOver = false;          // MUST be first — clears the guard in updateInputProcessor()
        isSettingsVisible = false;
        isInventoryVisible = false;
        uiStage.clear();

        // RE-ADD YOUR HUD BUTTONS HERE (Settings & Inventory)
        setupHUD();
        setupGlobalListener();       // Re-attach TAB/F5 shortcut listener that uiStage.clear() deleted
        updateInputProcessor();      // Now routes correctly to the HUD multiplexer
    }

    public boolean isGameOver() { return isGameOver; }




}
