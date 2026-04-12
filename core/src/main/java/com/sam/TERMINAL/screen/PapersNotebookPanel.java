package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;


public class PapersNotebookPanel extends Table {
    private static Texture panelTexture;
    private static NinePatchDrawable panelDrawable;

    private static NinePatchDrawable getPanelDrawable() {
        if (panelDrawable == null && Gdx.files.internal("ui/papersUI.png").exists()) {
            panelTexture = new Texture(Gdx.files.internal("ui/papersUI.png"));
            // Tuned for spiral-left notebook: generous left pad, softer corners right.
            NinePatch patch = new NinePatch(panelTexture, 26, 20, 14, 14);
            panelDrawable = new NinePatchDrawable(patch);
        }
        return panelDrawable;
    }

    public PapersNotebookPanel() {
        this(14f);
    }

    public PapersNotebookPanel(float innerPadding) {
        super();
        NinePatchDrawable bg = getPanelDrawable();
        if (bg != null)
            setBackground(bg);
        pad(innerPadding);
        top().left();
        defaults().left().top();
    }

    public static void disposeShared() {
        if (panelTexture != null) {
            panelTexture.dispose();
            panelTexture = null;
            panelDrawable = null;
        }
    }
}
