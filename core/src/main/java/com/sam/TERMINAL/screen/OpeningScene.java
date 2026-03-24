package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class OpeningScene {
    public interface OpeningCompleteListener {
        void onComplete();
    }

    private final SpriteBatch batch;
    private final Viewport viewport;
    private final BitmapFont bodyFont;
    private final BitmapFont terminalFont;
    private final OpeningCompleteListener listener;
    private final GlyphLayout layout = new GlyphLayout();

    private float elapsed;
    private boolean completed;

    public OpeningScene(SpriteBatch batch, OpeningCompleteListener listener) {
        this.batch = batch;
        this.listener = listener;
        this.viewport = new ExtendViewport(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            new OrthographicCamera()
        );
        this.viewport.apply(true);

        bodyFont = loadFont("fonts/Abaddon Light.ttf", 34);
        terminalFont = loadFont("fonts/BIOSfontII.ttf", 76);
    }

    public void render(float delta) {
        elapsed += delta;
        if (!completed && elapsed >= 21f) {
            completed = true;
            listener.onComplete();
            return;
        }

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        drawCenteredText("Use headphones for better experience.", bodyFont, 2f, 5f);
        drawCenteredText("Gateway presents", bodyFont, 8f, 5f);
        drawCenteredText("TERMINAL", terminalFont, 14f, 5f);
        batch.end();
    }

    private void drawCenteredText(String text, BitmapFont font, float start, float duration) {
        float alpha = windowAlpha(elapsed, start, duration);
        if (alpha <= 0f) return;

        font.setColor(1f, 1f, 1f, alpha);
        layout.setText(font, text);
        float x = (viewport.getWorldWidth() - layout.width) * 0.5f;
        float y = viewport.getWorldHeight() * 0.55f;
        font.draw(batch, text, x, y, 0, Align.left, false);
        font.setColor(Color.WHITE);
    }

    private float windowAlpha(float time, float start, float duration) {
        float t = time - start;
        if (t < 0f || t > duration) return 0f;
        float in = Math.min(1f, t / 0.45f);
        float out = Math.min(1f, (duration - t) / 0.45f);
        return Math.min(in, out);
    }

    private BitmapFont loadFont(String path, int size) {
        if (Gdx.files.internal(path).exists()) {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(path));
            FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = size;
            BitmapFont font = gen.generateFont(param);
            gen.dispose();
            return font;
        }
        BitmapFont fallback = new BitmapFont();
        fallback.getData().setScale(size / 16f);
        return fallback;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        bodyFont.dispose();
        terminalFont.dispose();
    }
}
