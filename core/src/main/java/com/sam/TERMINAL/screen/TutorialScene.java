package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.math.MathUtils;

public class TutorialScene {
    public interface TutorialCompleteListener {
        void onComplete();
    }

    private static final float CHAR_INTERVAL = 0.03f;

    private final Stage stage;
    private final Texture background;
    private final BitmapFont dialogueFont;
    private final Label dialogueLabel;
    private final Table root;
    private final SubmenuPanel panel;
    private final TutorialCompleteListener listener;

    private final String[] dialogueLines = {
        "System boot successful. Initializing training protocol... Continue with SPACE or x.",
        "Use WASD to move. Stay alert and watch your surroundings.",
        "Interact with nearby objects using E.",
        "Open inventory using TAB.",
        "Use ESC to open settings.",
        "Tutorial complete. Entering the maze..."
    };

    private int lineIndex = 0;
    private int visibleChars = 0;
    private float charTimer = 0f;
    private boolean finished;

    public TutorialScene(SpriteBatch batch, TutorialCompleteListener listener) {
        this.listener = listener;
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        stage = new Stage(new ExtendViewport(w, h), batch);

        background = new Texture(Gdx.files.internal("ui/TutorialBG.png"));
        dialogueFont = loadFont("fonts/Abaddon Light.ttf", 26);

        Label.LabelStyle style = new Label.LabelStyle(dialogueFont, Color.WHITE);
        dialogueLabel = new Label("", style);
        dialogueLabel.setWrap(true);
        dialogueLabel.setAlignment(Align.topLeft);

        panel = new SubmenuPanel(18f);
        panel.add(dialogueLabel).width(700f).left().top();

        root = new Table();
        root.setFillParent(true);
        root.bottom().padBottom(32f);
        root.add(panel);
        stage.addActor(root);
        refreshLayout();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER || keycode == Input.Keys.X) {
                    advance();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                advance();
                return true;
            }
        });
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    public void render(float delta) {
        if (!finished) {
            updateTypewriter(delta);
        }

        SpriteBatch batch = (SpriteBatch) stage.getBatch();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();
        batch.draw(background, 0, 0, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void updateTypewriter(float delta) {
        String line = dialogueLines[lineIndex];
        if (visibleChars >= line.length()) {
            dialogueLabel.setText(line);
            return;
        }

        charTimer += delta;
        while (charTimer >= CHAR_INTERVAL && visibleChars < line.length()) {
            visibleChars++;
            charTimer -= CHAR_INTERVAL;
        }
        dialogueLabel.setText(line.substring(0, visibleChars));
    }

    private void advance() {
        if (finished) return;

        String line = dialogueLines[lineIndex];
        if (visibleChars < line.length()) {
            visibleChars = line.length();
            dialogueLabel.setText(line);
            return;
        }

        lineIndex++;
        if (lineIndex >= dialogueLines.length) {
            finished = true;
            listener.onComplete();
            return;
        }

        visibleChars = 0;
        charTimer = 0f;
        dialogueLabel.setText("");
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
        stage.getViewport().update(width, height, true);
        refreshLayout();
    }

    private void refreshLayout() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();
        float panelWidth = MathUtils.clamp(vw * 0.9f, 320f, 760f);
        float panelHeight = MathUtils.clamp(vh * 0.28f, 150f, 220f);
        float textWidth = panelWidth - 60f;

        dialogueLabel.setWidth(textWidth);
        panel.getCell(dialogueLabel).width(textWidth);
        root.getCell(panel).width(panelWidth).height(panelHeight);
    }

    public void dispose() {
        stage.dispose();
        background.dispose();
        dialogueFont.dispose();
        SubmenuPanel.disposeShared();
    }
}
