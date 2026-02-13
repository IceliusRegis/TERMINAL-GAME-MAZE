package com.sam.TERMINAL.buttons;

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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class MenuScreen {
    private Stage uiStage;
    private Stage settingsStage;
    private Stage inventoryStage;
    private Texture settingsTexture, backTexture, whitePixel, invTexture;
    private boolean isSettingsVisible = false;
    private boolean isInventoryVisible = false;
    private Table inventoryWindow;
    private Table bottomTable;

    public MenuScreen(SpriteBatch batch) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        uiStage = new Stage(new ExtendViewport(w, h), batch);
        settingsStage = new Stage(new ExtendViewport(w, h), batch);
        inventoryStage = new Stage(new ExtendViewport(w, h), batch);

        createDimmerTexture();

        // --- SETTINGS SETUP ---
        Table mainRoot = new Table();
        mainRoot.setFillParent(true);
        mainRoot.top().left();

        settingsTexture = new Texture(Gdx.files.internal("settings.png"));
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

        Table settingsRoot = new Table();
        settingsRoot.setFillParent(true);
        settingsRoot.top().left();

        backTexture = new Texture(Gdx.files.internal("Restart.png"));
        Image backBtn = new Image(backTexture);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isSettingsVisible = false;
                updateInputProcessor();
            }
        });
        settingsRoot.add(backBtn).size(40, 40).pad(10);
        settingsStage.addActor(settingsRoot);

        // --- INVENTORY HUD BUTTON ---
        invTexture = new Texture(Gdx.files.internal("inventory.png"));
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

        // inventory window
        Table inventoryRoot = new Table();
        inventoryRoot.setFillParent(true);
        inventoryStage.addActor(inventoryRoot);

        inventoryWindow = new Table();

        TextureRegionDrawable windowBg = new TextureRegionDrawable(new TextureRegion(whitePixel));
        inventoryWindow.setBackground(windowBg.tint(new com.badlogic.gdx.graphics.Color(0.8f, 0.8f, 0.8f, 0.15f)));

        inventoryRoot.center();
        inventoryRoot.add(inventoryWindow).size(500, 400);

        inventoryWindow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isInventoryVisible = false;
                updateInputProcessor();
            }
        });

        new InventoryButton(inventoryWindow, new Runnable() {
            @Override
            public void run() {
                isInventoryVisible = false;
                updateInputProcessor();
            }
        });

        updateInputProcessor();

        // settings window
        settingsRoot.setFillParent(true);
        settingsStage.addActor(settingsRoot);

        // Just call the class using the existing variable
        new SettingsButton(settingsRoot, new Runnable() {
            @Override
            public void run() {
                isSettingsVisible = false;
                updateInputProcessor();
            }
        });

        // to use tab-key as shortcut for inventory
        InputListener tabListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.TAB) {
                    // Toggle inventory visibility
                    isInventoryVisible = !isInventoryVisible;

                    // If opening inventory, make sure settings are closed
                    if (isInventoryVisible) isSettingsVisible = false;

                    updateInputProcessor();
                    return true;
                }
                return false;
            }
        };

// Add the listener to all stages so Tab works regardless of what is open
        uiStage.addListener(tabListener);
        inventoryStage.addListener(tabListener);
        settingsStage.addListener(tabListener);
    }

    // dim effect of settings window
    private void createDimmerTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void updateInputProcessor() {
        if (isSettingsVisible) {
            Gdx.input.setInputProcessor(settingsStage); // does not allow to move the character when in settings window
        } else if (isInventoryVisible) {
            com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
            multiplexer.addProcessor(inventoryStage); // allows to move the character when in inventory window
            Gdx.input.setInputProcessor(multiplexer);
        } else {
            Gdx.input.setInputProcessor(uiStage);
        }
    }

    public void render(float delta) {
        // ALWAYS draw the game/HUD first so it's visible in the background
        uiStage.act(delta);
        uiStage.draw();

        if (isSettingsVisible) {
            drawDim(settingsStage);
            settingsStage.act(delta);
            settingsStage.draw();
        } else if (isInventoryVisible) {
            // This only draws the 500x400 box and its dim over the uiStage
            drawInventoryDim(inventoryStage);
            inventoryStage.act(delta);
            inventoryStage.draw();
        }
    }

    // New logic: This dims ONLY the area of the inventory window, not the whole screen
    private void drawInventoryDim(Stage stage) {
        stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
        stage.getBatch().begin();
        stage.getBatch().setColor(0, 0, 0, 0.5f); // Semi-transparent black
        // Draws ONLY in the middle area where the window is
        stage.getBatch().draw(whitePixel,
            (stage.getViewport().getWorldWidth() - 500) / 2,
            (stage.getViewport().getWorldHeight() - 400) / 2,
            500, 400);
        stage.getBatch().setColor(1, 1, 1, 1);
        stage.getBatch().end();
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
        backTexture.dispose();
        whitePixel.dispose();
        invTexture.dispose();
    }

    public boolean isSettingsVisible() {
        return isSettingsVisible;
    }

    public boolean isInventoryVisible() {
        return isInventoryVisible;
    }
}
