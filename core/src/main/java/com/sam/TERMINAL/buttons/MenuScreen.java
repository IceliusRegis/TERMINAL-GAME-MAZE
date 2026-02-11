package com.sam.TERMINAL.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class MenuScreen {
    private Stage uiStage;
    private Stage settingsStage;
    private Texture settingsTexture, backTexture, whitePixel;
    private boolean isSettingsVisible = false;

    public MenuScreen(SpriteBatch batch) {
        // We use the current window size as the base for the StretchViewport
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        uiStage = new Stage(new ExtendViewport(w, h), batch);
        settingsStage = new Stage(new ExtendViewport(w, h), batch);

        createDimmerTexture();

        // --- Layout Tables ---
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

        updateInputProcessor();
    }

    private void createDimmerTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void updateInputProcessor() {
        if (isSettingsVisible) {
            Gdx.input.setInputProcessor(settingsStage);
        } else {
            Gdx.input.setInputProcessor(uiStage);
        }
    }

    public void render(float delta) {
        if (isSettingsVisible) {
            // Draw Dimmer
            settingsStage.getBatch().setProjectionMatrix(settingsStage.getCamera().combined);
            settingsStage.getBatch().begin();
            settingsStage.getBatch().setColor(0, 0, 0, 0.6f);

            // Use getWorldWidth/Height so it stretches to fill the whole screen
            // regardless of the window size or aspect ratio
            settingsStage.getBatch().draw(whitePixel, 0, 0,
                settingsStage.getViewport().getWorldWidth(),
                settingsStage.getViewport().getWorldHeight());

            settingsStage.getBatch().setColor(1, 1, 1, 1);
            settingsStage.getBatch().end();

            settingsStage.act(delta);
            settingsStage.draw();
        } else {
            uiStage.act(delta);
            uiStage.draw();
        }
    }

    public void resize(int width, int height) {
        // update(width, height, true) handles the centering and recalculation
        uiStage.getViewport().update(width, height, true);
        settingsStage.getViewport().update(width, height, true);
    }

    public void dispose() {
        uiStage.dispose();
        settingsStage.dispose();
        settingsTexture.dispose();
        backTexture.dispose();
        whitePixel.dispose();
    }

    public boolean isSettingsVisible() {
        return isSettingsVisible;
    }
}
