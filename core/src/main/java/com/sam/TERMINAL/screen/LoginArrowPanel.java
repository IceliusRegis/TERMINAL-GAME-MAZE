package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

/** Reusable nine-patch panel for the login arrow UI. */
public class LoginArrowPanel extends Table {
    private static Texture panelTexture;
    private static NinePatchDrawable panelDrawable;

    private static NinePatchDrawable getPanelDrawable() {
        if (panelDrawable == null) {
            panelTexture = new Texture(Gdx.files.internal("ui/Arrow pointer b.png"));
            NinePatch patch = new NinePatch(panelTexture, 6, 6, 6, 6);
            panelDrawable = new NinePatchDrawable(patch);
        }
        return panelDrawable;
    }

    public LoginArrowPanel() {
        this(12f);
    }

    public LoginArrowPanel(float innerPadding) {
        super();
        setBackground(getPanelDrawable());
        pad(innerPadding);
        top().left();
        defaults().left().top();
    }

    public void disposePanel() {
        if (panelTexture != null) {
            panelTexture.dispose();
            panelTexture = null;
        }
        setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
    }
}
