package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class TutorialScene {
    public interface TutorialListener {
        void onSpawnIntoMaze();

        void onTutorialComplete();
    }

    private static final float CHAR_INTERVAL = 0.06f;
    private static final float UI_BASE_W = 800f;
    private static final float UI_BASE_H = 600f;

    private enum Phase {
        COMPLAINT,
        FILLER,
        LOGINSAM,
        GLITCH,
        BLINK,
        POST_SPAWN,
        TUTORIAL,
        DONE
    }

    private final Stage stage;
    private final TutorialListener listener;

    // Backgrounds + overlay image.
    private final Texture stationBg;
    private final Texture stationTripBg;

    private final BitmapFont dialogueFont;

    // Panels (SubmenuPanel.java dialogue stays visible).
    private final SubmenuPanel complaintPanel;
    private final Label complaintLabel;
    private final Typewriter complaintTypewriter;

    // loginsam overlay.
    private final Image darkOverlay;
    private final Image vignetteOverlay;
    private final Image blinkOverlay;
    private final LoginArrowPanel loginArrowPanel;
    private final Label loginsamTextLabel;
    private final Typewriter loginsamTypewriter;

    private final Texture whitePixel;

    private Phase phase = Phase.COMPLAINT;
    private float phaseTimer = 0f;
    private boolean spawnTriggered = false;
    private boolean completeTriggered = false;
    private float introBlackTimer = 0.9f;

    private InputMultiplexer inputMultiplexer;

    private Sound typingSfx;
    private long typingSfxId = -1L;

    private static final float GLITCH_DURATION = 2.4f;
    private static final float BLINK_DURATION = 2.4f;

    private int tutorialIndex = 0;

    private final String complaintText =
        "They're all calling out.\nI'm stuck covering for coworkers again.\n\nEnter x, space, or click to continue.";

    private final String fillerTextBeforeLoginsam =
        "Another shift...\nmy eyes feel heavy.";

    private final String postGlitchDialogue =
        "?!\nWhat...?\nThis isn't...";

    private final String postSpawnDialogue =
        "Huh...?\nWhat happened?";

    private final String employeeIdText =
        "> EMPLOYEE ID: 88-SAM\n>STATUS: OVERTIME";
    private final String studentIdText =
        ">STUDENT ID: 1423-CHIZO\n>STATUS: DEAD";

    private final String[] tutorialLines = {
        "Emergency protocol online.",
        "Use WASD to move. Stay alert and watch your surroundings.",
        "Look for clues to solve the mystery.",
        "Focus on objectives, collect items to get out.",
        "Interact with nearby objects using E.",
        "Turn on flashlight using F, reload batteries using U.",
        "Open inventory using TAB, F5 to quicksave.",
        "Use ESC to open settings.",
        "Tutorial complete."
    };

    public TutorialScene(SpriteBatch batch, TutorialListener listener) {
        this.listener = listener;

        stage = new Stage(new FitViewport(UI_BASE_W, UI_BASE_H), batch);

        stationBg = new Texture(Gdx.files.internal("ui/Station.png"));
        stationTripBg = new Texture(Gdx.files.internal("ui/StationTrip.png"));

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        if (Gdx.files.internal("sfx/type.ogg").exists()) {
            typingSfx = Gdx.audio.newSound(Gdx.files.internal("sfx/type.ogg"));
        }

        dialogueFont = loadFont("fonts/Abaddon Light.ttf", 26);
        Label.LabelStyle whiteStyle = new Label.LabelStyle(dialogueFont, Color.WHITE);

        complaintPanel = new SubmenuPanel(18f);
        complaintLabel = new Label("", whiteStyle);
        complaintLabel.setWrap(true);
        complaintLabel.setAlignment(Align.topLeft);
        complaintPanel.add(complaintLabel).width(600f).left().top();
        complaintTypewriter = new Typewriter(complaintLabel, CHAR_INTERVAL);

        // loginsam overlay + dark overlays (vignette + blink).
        darkOverlay = new Image(whitePixel);
        darkOverlay.setColor(0, 0, 0, 0f);
        darkOverlay.setFillParent(true);

        vignetteOverlay = new Image(whitePixel);
        vignetteOverlay.setColor(0, 0, 0, 0f);
        vignetteOverlay.setFillParent(true);

        blinkOverlay = new Image(whitePixel);
        blinkOverlay.setColor(0, 0, 0, 0f);
        blinkOverlay.setFillParent(true);

        loginArrowPanel = new LoginArrowPanel(10f);
        loginArrowPanel.setVisible(false);

        loginsamTextLabel = new Label("", whiteStyle);
        loginsamTextLabel.setWrap(true);
        loginsamTextLabel.setAlignment(Align.topLeft);
        loginsamTypewriter = new Typewriter(loginsamTextLabel, CHAR_INTERVAL);
        loginArrowPanel.add(loginsamTextLabel).left().top().expand().fill();

        // Layer order: dark overlay -> loginsam image -> panels -> vignette/blink.
        stage.addActor(darkOverlay);
        stage.addActor(loginArrowPanel);
        stage.addActor(complaintPanel);
        stage.addActor(vignetteOverlay);
        stage.addActor(blinkOverlay);

        refreshLayout();

        // Input: fast-forward typewriting; phase progression requires user input.
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER || keycode == Input.Keys.X) {
                    onUserInputAdvance();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                onUserInputAdvance();
                return true;
            }
        });
        inputMultiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(inputMultiplexer);

        // Start from scratch.
        complaintTypewriter.start(complaintText);
        loginArrowPanel.setVisible(false);
        darkOverlay.setColor(0, 0, 0, 0f);
        vignetteOverlay.setColor(0, 0, 0, 0f);
        blinkOverlay.setColor(0, 0, 0, 0f);
    }

    private void onUserInputAdvance() {
        if (introBlackTimer > 0f) return;
        if (phase == Phase.DONE) return;

        // 1) If any active typewriter isn't done, advance it immediately.
        if (!complaintTypewriter.isDone()) {
            complaintTypewriter.skip();
            return;
        }
        if (loginArrowPanel.isVisible() && !loginsamTypewriter.isDone()) {
            loginsamTypewriter.skip();
            return;
        }

        // 2) Otherwise, move to the next step (only via input).
        switch (phase) {
            case COMPLAINT: {
                phase = Phase.FILLER;
                phaseTimer = 0f;
                complaintTypewriter.start(fillerTextBeforeLoginsam);
                break;
            }
            case FILLER: {
                // Show loginsam + employee id, then wait for input.
                phase = Phase.LOGINSAM;
                phaseTimer = 0f;

                darkOverlay.setColor(0f, 0f, 0f, 0.45f);
                loginArrowPanel.setVisible(true);
                loginsamTypewriter.start(employeeIdText);

                // Keep the coworker complaint visible while loginsam overlay shows.
                complaintTypewriter.start(complaintText);
                break;
            }
            case LOGINSAM: {
                // Prepare the glitch reaction.
                phase = Phase.GLITCH;
                phaseTimer = 0f;

                complaintTypewriter.start(postGlitchDialogue);
                loginsamTypewriter.start(studentIdText);
                break;
            }
            case GLITCH: {
                // Start the blink effect (phase animation runs automatically).
                phase = Phase.BLINK;
                phaseTimer = 0f;
                vignetteOverlay.setColor(0, 0, 0, 0.15f);
                blinkOverlay.setColor(0, 0, 0, 0f);
                break;
            }
            case POST_SPAWN: {
                // Begin multi-step tutorial lines, one per user input.
                phase = Phase.TUTORIAL;
                tutorialIndex = 0;
                complaintTypewriter.start(tutorialLines[tutorialIndex]);
                break;
            }
            case TUTORIAL: {
                tutorialIndex++;
                if (tutorialIndex >= tutorialLines.length) {
                    phase = Phase.DONE;
                    loginArrowPanel.setVisible(false);
                    darkOverlay.setColor(0, 0, 0, 0f);
                    vignetteOverlay.setColor(0, 0, 0, 0f);
                    blinkOverlay.setColor(0, 0, 0, 0f);
                    complaintPanel.setVisible(false);
                    if (!completeTriggered) {
                        completeTriggered = true;
                        listener.onTutorialComplete();
                    }
                } else {
                    complaintTypewriter.start(tutorialLines[tutorialIndex]);
                }
                break;
            }
        }
    }

    public void render(float delta) {
        // After Main starts the real game, MenuScreen will replace the input processor.
        // Re-apply ours so the tutorial dialogue can still advance and close.
        if (phase != Phase.DONE && inputMultiplexer != null) {
            Gdx.input.setInputProcessor(inputMultiplexer);
        }

        if (phase == Phase.DONE) {
            loginArrowPanel.setVisible(false);
            darkOverlay.setColor(0, 0, 0, 0f);
            vignetteOverlay.setColor(0, 0, 0, 0f);
            blinkOverlay.setColor(0, 0, 0, 0f);
            complaintPanel.setVisible(false);
            stage.act(delta);
            stage.draw();
            return;
        }

        phaseTimer += delta;
        if (introBlackTimer > 0f) {
            introBlackTimer -= delta;
            float alpha = MathUtils.clamp(introBlackTimer / 0.9f, 0f, 1f);
            darkOverlay.setColor(0, 0, 0, alpha);
        } else if (phase != Phase.BLINK && phase != Phase.POST_SPAWN && phase != Phase.TUTORIAL) {
            // Ensure overlay doesn't accidentally stay pitch black if not in a transition
            if (phase != Phase.LOGINSAM) darkOverlay.setColor(0, 0, 0, 0f);
        }

        // Update typewriters (bottom dialogue + optional loginsam overlay).
        complaintTypewriter.update(delta);
        if (loginArrowPanel.isVisible()) {
            loginsamTypewriter.update(delta);
        }

        // Keep typing SFX in sync with any active typewriter.
        updateTypingSfx();

        updatePhase(delta);

        // Draw pre-spawn backgrounds behind the Stage UI.
        SpriteBatch batch = (SpriteBatch) stage.getBatch();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();

        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // Fix: Explicitly draw backgrounds as long as we haven't reached the maze spawn
        if (!spawnTriggered) {
            if (phase == Phase.COMPLAINT || phase == Phase.FILLER || phase == Phase.LOGINSAM) {
                drawBackgroundCover(batch, stationBg, vw, vh);
            } else if (phase == Phase.GLITCH || phase == Phase.BLINK) {
                if (phase == Phase.GLITCH) {
                    drawGlitchedBackground(batch, stationTripBg, stationBg, vw, vh);
                } else {
                    drawBackgroundCover(batch, stationTripBg, vw, vh);
                }
            }
        }

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    private boolean showPreGameBackground() {
        // Once we spawn into the maze, TutorialScene should not cover it with a fake background.
        return phase != Phase.DONE && !spawnTriggered;
    }

    private void updateTypingSfx() {
        if (typingSfx == null) return;

        boolean complaintTyping = !complaintTypewriter.isDone();
        boolean loginsamTyping = loginArrowPanel.isVisible() && !loginsamTypewriter.isDone();

        boolean shouldPlay = complaintTyping || loginsamTyping;

        if (shouldPlay) {
            if (typingSfxId == -1L) {
                typingSfxId = typingSfx.loop(0.6f);
            }
        } else {
            if (typingSfxId != -1L) {
                typingSfx.stop(typingSfxId);
                typingSfxId = -1L;
            }
        }
    }

    private void drawGlitchedBackground(SpriteBatch batch, Texture primary, Texture secondary, float vw, float vh) {
        // Light “RGB split + jitter” glitch. Cheap but effective for the vibe.
        float scale = MathUtils.clamp(Math.min(vw, vh) / 800f, 0.6f, 1.4f);
        float j = MathUtils.random(-8f, 8f) * scale;

        // Cyan layer
        batch.setColor(0.2f, 1f, 1f, 1f);
        drawBackgroundCover(batch, primary, vw, vh, j, 0f);
        // Red layer
        batch.setColor(1f, 0.2f, 0.2f, 1f);
        drawBackgroundCover(batch, secondary, vw, vh, -j, 0f);
        // Normal layer
        drawBackgroundCover(batch, primary, vw, vh);
    }

    private void drawBackgroundCover(SpriteBatch batch, Texture texture, float vw, float vh) {
        drawBackgroundCover(batch, texture, vw, vh, 0f, 0f);
    }

    /** Draws texture scaled to cover viewport while preserving aspect ratio. */
    private void drawBackgroundCover(SpriteBatch batch, Texture texture, float vw, float vh, float offsetX, float offsetY) {
        batch.setColor(Color.WHITE);

        float tw = texture.getWidth();
        float th = texture.getHeight();
        float scale = Math.max(vw / tw, vh / th);

        float dw = tw * scale;
        float dh = th * scale;
        float dx = (vw - dw) * 0.5f + offsetX;
        float dy = (vh - dh) * 0.5f + offsetY;

        batch.draw(texture, dx, dy, dw, dh);
    }

    private void updatePhase(float delta) {
        switch (phase) {
            case GLITCH: {
                // Gradually increase vignette while glitching. Phase transition is input-driven.
                float t = MathUtils.clamp(phaseTimer / GLITCH_DURATION, 0f, 1f);
                vignetteOverlay.setColor(0, 0, 0, 0.15f + t * 0.6f);
                // Background swap is handled in render() via phase == GLITCH.
                break;
            }
            case BLINK: {
                // Increase vignette and perform the "two blinks". Spawn happens when this finishes.
                float t = MathUtils.clamp(phaseTimer / BLINK_DURATION, 0f, 1f);
                vignetteOverlay.setColor(0, 0, 0, 0.35f + t * 0.65f);

                // Two black “blinks” (spread out to feel slower).
                boolean blink1 = phaseTimer >= 0.80f && phaseTimer < 1.00f;
                boolean blink2 = phaseTimer >= 1.60f && phaseTimer < 1.80f;
                blinkOverlay.setColor(0, 0, 0, (blink1 || blink2) ? 1f : 0f);

                if (phaseTimer >= BLINK_DURATION) {
                    vignetteOverlay.setColor(0, 0, 0, 0f);
                    blinkOverlay.setColor(0, 0, 0, 0f);

                    if (!spawnTriggered) {
                        spawnTriggered = true;
                        listener.onSpawnIntoMaze();
                    }

                    // Hide overlays so the real maze becomes visible.
                    // Keep map dimmed until tutorial dialogue fully finishes.
                    darkOverlay.setColor(0, 0, 0, 0.42f);
                    loginArrowPanel.setVisible(false);

                    phase = Phase.POST_SPAWN;
                    phaseTimer = 0f;
                    complaintTypewriter.start(postSpawnDialogue);
                }
                break;
            }
            default: {
                // All other phases are either static waiting-for-input or handled by the input callback.
                break;
            }
        }
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        refreshLayout();
    }

    private void refreshLayout() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // Complaint panel (bottom).
        float complaintW = MathUtils.clamp(vw * 0.84f, 300f, 740f);
        float complaintH = MathUtils.clamp(vh * 0.22f, 140f, 220f);
        complaintPanel.setSize(complaintW, complaintH);
        complaintPanel.setPosition((vw - complaintW) / 2f, 32f);

        float complaintTextW = complaintW - 60f;
        complaintLabel.setWidth(complaintTextW);
        complaintPanel.getCell(complaintLabel).width(complaintTextW);

        // Arrow panel (center) — rectangular and stable under resize.
        float logiW = MathUtils.clamp(vw * 0.50f, 300f, 500f);
        float logiH = MathUtils.clamp(vh * 0.16f, 90f, 120f);
        loginArrowPanel.setSize(logiW, logiH);
        loginArrowPanel.setPosition((vw - logiW) / 2f, (vh - logiH) / 2f + vh * 0.05f);

        float logiTextW = logiW * 0.78f;
        float logiTextH = logiH * 0.70f;
        loginsamTextLabel.setFontScale(0.9f);
        loginsamTextLabel.setWidth(logiTextW);
        loginsamTextLabel.setHeight(logiTextH);
        loginArrowPanel.getCell(loginsamTextLabel).width(logiTextW).height(logiTextH).left().top();
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

    public void dispose() {
        stage.dispose();
        stationBg.dispose();
        stationTripBg.dispose();
        whitePixel.dispose();
        if (typingSfxId != -1L && typingSfx != null) {
            typingSfx.stop(typingSfxId);
            typingSfxId = -1L;
        }
        if (typingSfx != null) {
            typingSfx.dispose();
            typingSfx = null;
        }
        dialogueFont.dispose();
        loginArrowPanel.disposePanel();
    }

    private static class Typewriter {
        private final Label label;
        private final float interval;

        private String text = "";
        private int visibleChars = 0;
        private float timer = 0f;
        private boolean done = true;

        Typewriter(Label label, float interval) {
            this.label = label;
            this.interval = interval;
        }

        void start(String text) {
            this.text = text == null ? "" : text;
            this.visibleChars = 0;
            this.timer = 0f;
            this.done = false;
            label.setText("");
        }

        void update(float delta) {
            if (done) return;
            timer += delta;
            while (timer >= interval && visibleChars < text.length()) {
                visibleChars++;
                timer -= interval;
            }
            if (visibleChars >= text.length()) {
                done = true;
                label.setText(text);
                return;
            }
            label.setText(text.substring(0, visibleChars));
        }

        boolean isDone() {
            return done;
        }

        void skip() {
            done = true;
            visibleChars = text.length();
            label.setText(text);
        }
    }
}
