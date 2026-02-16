package com.sam.TERMINAL.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class InventoryButton {
    private Table window;
    private Texture exitTexture;

    public InventoryButton(Table inventoryWindow, final Runnable onClose) {
        this.window = inventoryWindow;

        // Load the specific picture for the exit button here
        // Make sure "exit_icon.png" is in your assets folder!
        exitTexture = new Texture(Gdx.files.internal("ui/exit.png"));

        TextureRegionDrawable exitDrawable = new TextureRegionDrawable(new TextureRegion(exitTexture));
        ImageButton exitBtn = new ImageButton(exitDrawable);

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClose.run();
            }
        });

        // Set the window alignment and add the button to the top right
        window.top().left();
        window.add(exitBtn).size(30, 30).pad(10);
    }

    // Call this from your MenuScreen dispose() to prevent memory leaks
    public void dispose() {
        if (exitTexture != null) exitTexture.dispose();
    }
}
