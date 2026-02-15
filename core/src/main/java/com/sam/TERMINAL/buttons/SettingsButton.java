package com.sam.TERMINAL.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;

public class SettingsButton {
    private Array<Texture> textures = new Array<>();

    // UPDATED: Constructor now accepts onClose, onRefresh, and onSave
    public SettingsButton(Table settingsRoot, final Runnable onClose, final Runnable onSave) {
        settingsRoot.clear();
        settingsRoot.setFillParent(true);

        // A Stack lets us put the back button and the menu on different layers
        Stack stack = new Stack();
        settingsRoot.add(stack).expand().fill();

        // --- LAYER 1: BACK BUTTON (Top-Left) ---
        Table backLayer = new Table();
        backLayer.top().left();

        ImageButton backBtn = createBtn("Restart.png");
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClose.run();
            }
        });

        // Responsive size for back button
        Value backSize = Value.percentHeight(0.08f, settingsRoot);
        backLayer.add(backBtn).size(backSize).pad(10);

        // --- LAYER 2: CENTER MENU ---
        Table menuLayer = new Table();
        menuLayer.center();

        ImageButton saveBtn = createBtn("saveMap.png");
        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onSave.run(); // Calls the saveMapLogic from MenuScreen
            }
        });

        // This is your actual Restart/Refresh button in the center
        ImageButton refreshBtn = createBtn("refresh.png");

        ImageButton quitBtn = createBtn("quit.png");
        quitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Responsive sizes for the menu buttons
        Value btnSize = Value.percentHeight(0.12f, settingsRoot);
        Value gap = Value.percentWidth(0.02f, settingsRoot);

        menuLayer.add(saveBtn).size(btnSize).pad(gap);
        menuLayer.add(refreshBtn).size(btnSize).pad(gap); // The refresh logic
        menuLayer.add(quitBtn).size(btnSize).pad(gap);

        // Add both layers to the stack
        stack.add(backLayer);
        stack.add(menuLayer);
    }

    private ImageButton createBtn(String path) {
        Texture tex = new Texture(Gdx.files.internal(path));
        textures.add(tex);
        return new ImageButton(new TextureRegionDrawable(new TextureRegion(tex)));
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}
