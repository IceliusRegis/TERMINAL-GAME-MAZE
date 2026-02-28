package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

//reusable frame panel
public class SubmenuPanel extends Table {

    private static Texture frameTexture;
    private static NinePatchDrawable frameDrawable;

    private static NinePatchDrawable getFrameDrawable() {
        if (frameDrawable == null) {
            frameTexture = new Texture(Gdx.files.internal("ui/Chunky white 2b.png"));
            // Split border into scalable 9-patch regions.
            // Tune these numbers if you adjust the source asset.
            NinePatch ninePatch = new NinePatch(frameTexture, 6, 6, 6, 6);
            frameDrawable = new NinePatchDrawable(ninePatch);
        }
        return frameDrawable;
    }

    public SubmenuPanel() {
        this(24f);
    }

    public SubmenuPanel(float innerPadding) {
        super();
        setBackground(getFrameDrawable());
        pad(innerPadding);
        center();              // center content
        defaults().center();
    }

    /** Dispose shared NinePatch texture if you no longer need any SubmenuPanels. */
    public static void disposeShared() {
        if (frameTexture != null) {
            frameTexture.dispose();
            frameTexture = null;
            frameDrawable = null;
        }
    }
}

