package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.sam.TERMINAL.Main;

/**
 * Full-screen ending sequence: black intro, typewriter dialogue, per-ending beats, shared rolling
 * credits (from bottom, stopping when the top of the block reaches 70% of viewport height), then
 * restart.
 */
public final class EndingCutsceneRoot extends WidgetGroup implements Disposable {

    private static final float TYPE_SLOW = 0.10f;
    private static final float TYPE_NORMAL = 0.055f;

    private static final String[] NEUTRAL_LINES = {
            "Finally... I made it out of that hellhole.",
            "Too much crazy, unnatural things.. way out of my salary grade.",
            "I need a beer, and a resignation letter..."
    };

    private static final String[] BAD_PHASE1 = {
            "Serves you ri-",
            ".....",
            "W.. what?",
            "This can't be......"
    };

    private static final String[] BAD_PHASE2 = {
            "It wasn't a monster at all..",
            "I did that?",
            "Poor girl... I killed her.",
            "I killed her...",
            "I'm... a murderer?",
            "I'm..."
    };

    private static final String[] GOOD_LINES = {
            "...I know what happened.",
            "I'm not the one responsible for you, but I get that it doesn't make a difference to you.",
            "It was unfortunate.. you didn't deserve that, if it was me I wouldn't come to save you..",
            "Still, there's no reason for you to keep wandering restlessly.",
            "You deserve peace... please let me grant that to you."
    };

    private final Main mainGame;
    private final Endings.Kind kind;
    private final Runnable onRestart;
    private final Texture sharedWhitePixel;
    private final Texture restartTexture;

    private final Image blackBase;
    private final Image endBgImage;
    private final Image chizoImage;
    private final Image blinkWhite;
    private final Image whiteFlash;
    private final SubmenuPanel dialoguePanel;
    private final Label dialogueLabel;
    private final Label centerTitleLabel;
    private final Label creditsLabel;
    private final ImageButton restartBtn;

    private final Typewriter typewriter;
    private Sound heartbeatSound;
    private long heartbeatId = -1;

    private Texture ownedEndBgTexture;
    private Texture ownedChizoTexture;

    private String[] dialogueLines;
    private int dialogueIndex;
    private float typeInterval;

    /** Bad: 0 = phase1 dialogue, 1 = chizo + phase2, 2 = blink, 3 = title, 4 = credits */
    private int badStep;
    private float badShakeTimer;
    private float blinkTimer;
    private float neutralPhaseTimer;
    private boolean goodTransitioning;
    private float creditsScrollY;
    private float creditsBlockHeight;
    private boolean creditsRolling;
    private boolean creditsFinished;
    private boolean restartVisible;

    private float layoutW;
    private float layoutH;

    public EndingCutsceneRoot(Main main, BitmapFont dialogueFont, BitmapFont creditsFont, Texture whitePixel,
            Texture restartTexture, Endings.Kind kind, Runnable onRestart) {
        this.mainGame = main;
        this.kind = kind;
        this.onRestart = onRestart;
        this.sharedWhitePixel = whitePixel;
        this.restartTexture = restartTexture;

        setTouchable(Touchable.enabled);
        layoutW = Gdx.graphics.getWidth();
        layoutH = Gdx.graphics.getHeight();
        setSize(layoutW, layoutH);

        blackBase = new Image(whitePixel);
        blackBase.setColor(Color.BLACK);
        blackBase.setSize(layoutW, layoutH);
        addActor(blackBase);

        endBgImage = new Image(whitePixel);
        endBgImage.setColor(Color.WHITE);
        endBgImage.setSize(layoutW, layoutH);
        endBgImage.getColor().a = 0f;
        endBgImage.setVisible(false);
        addActor(endBgImage);

        chizoImage = new Image(whitePixel);
        chizoImage.setVisible(false);
        addActor(chizoImage);

        creditsLabel = new Label(Endings.rollingCreditsText(), new Label.LabelStyle(creditsFont, kind == Endings.Kind.GOOD ? Color.BLACK : Color.WHITE));
        creditsLabel.setAlignment(Align.center);
        creditsLabel.setWrap(true);
        creditsLabel.setFontScale(0.82f);
        creditsLabel.setVisible(false);
        addActor(creditsLabel);

        whiteFlash = new Image(whitePixel);
        whiteFlash.setColor(Color.WHITE);
        whiteFlash.setSize(layoutW, layoutH);
        whiteFlash.getColor().a = 0f;
        whiteFlash.setVisible(false);
        addActor(whiteFlash);

        blinkWhite = new Image(whitePixel);
        blinkWhite.setColor(Color.WHITE);
        blinkWhite.setSize(layoutW, layoutH);
        blinkWhite.getColor().a = 0f;
        blinkWhite.setVisible(false);
        addActor(blinkWhite);

        dialoguePanel = new SubmenuPanel(18f);
        dialogueLabel = new Label("", new Label.LabelStyle(dialogueFont, Color.WHITE));
        dialogueLabel.setWrap(true);
        dialogueLabel.setAlignment(Align.topLeft);
        dialoguePanel.add(dialogueLabel).width(Math.min(720f, layoutW - 48f)).left().top();
        dialoguePanel.setVisible(true);
        addActor(dialoguePanel);

        centerTitleLabel = new Label("", new Label.LabelStyle(dialogueFont, Color.WHITE));
        centerTitleLabel.setAlignment(Align.center);
        centerTitleLabel.setFontScale(1.15f);
        centerTitleLabel.getColor().a = 0f;
        centerTitleLabel.setVisible(false);
        addActor(centerTitleLabel);

        restartBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(restartTexture)));
        restartBtn.setVisible(false);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onRestart != null)
                    onRestart.run();
            }
        });
        addActor(restartBtn);

        typewriter = new Typewriter(dialogueLabel, TYPE_SLOW);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                handleAdvanceTap();
                return true;
            }
        });

        switch (kind) {
            case NEUTRAL:
                mainGame.startEndingMusicNeutral();
                dialogueLines = NEUTRAL_LINES;
                typeInterval = TYPE_SLOW;
                typewriter.setInterval(typeInterval);
                dialogueIndex = 0;
                typewriter.start(dialogueLines[0]);
                layoutDialogue();
                break;
            case BAD:
                dialogueLines = BAD_PHASE1;
                typeInterval = TYPE_SLOW;
                typewriter.setInterval(typeInterval);
                badStep = 0;
                dialogueIndex = 0;
                typewriter.start(dialogueLines[0]);
                layoutDialogue();
                break;
            case GOOD:
                dialogueLines = GOOD_LINES;
                typeInterval = TYPE_NORMAL;
                typewriter.setInterval(typeInterval);
                dialogueIndex = 0;
                typewriter.start(dialogueLines[0]);
                layoutDialogue();
                break;
            default:
                dialogueLines = NEUTRAL_LINES;
                typewriter.start("");
                break;
        }
    }

    @Override
    public void layout() {
        super.layout();
        float w = getWidth();
        float h = getHeight();
        if (w > 0.5f && h > 0.5f
                && (Math.abs(w - layoutW) > 0.5f || Math.abs(h - layoutH) > 0.5f)) {
            onResize(w, h);
        }
    }

    public void onResize(float worldWidth, float worldHeight) {
        layoutW = worldWidth;
        layoutH = worldHeight;
        blackBase.setSize(layoutW, layoutH);
        endBgImage.setSize(layoutW, layoutH);
        whiteFlash.setSize(layoutW, layoutH);
        blinkWhite.setSize(layoutW, layoutH);
        layoutDialogue();
        layoutChizo();
        layoutCredits();
        restartBtn.setPosition((layoutW - 64f) / 2f, 24f);
        centerTitleLabel.setPosition(0f, layoutH * 0.5f - centerTitleLabel.getPrefHeight() * 0.5f);
        centerTitleLabel.setWidth(layoutW);
    }

    private void layoutDialogue() {
        float panelW = Math.min(760f, layoutW - 32f);
        float panelH = Math.min(200f, layoutH * 0.24f);
        dialoguePanel.setSize(panelW, panelH);
        dialoguePanel.setPosition((layoutW - panelW) / 2f, 12f);
        dialoguePanel.getCell(dialogueLabel).width(panelW - 40f);
    }

    private void layoutChizo() {
        if (ownedChizoTexture == null || !chizoImage.isVisible())
            return;
        float pad = 48f;
        float maxW = layoutW - pad * 2f;
        float maxH = layoutH - pad * 2f;
        float tw = ownedChizoTexture.getWidth();
        float th = ownedChizoTexture.getHeight();
        float scale = Math.min(maxW / tw, maxH / th);
        float dw = tw * scale;
        float dh = th * scale;
        chizoImage.setSize(dw, dh);
        chizoImage.setPosition((layoutW - dw) / 2f, (layoutH - dh) / 2f);
    }

    private void layoutCredits() {
        float cw = layoutW - 64f;
        creditsLabel.setWidth(cw);
        creditsLabel.setPosition((layoutW - cw) / 2f, creditsScrollY);
        creditsLabel.validate();
        creditsBlockHeight = creditsLabel.getPrefHeight();
    }

    private void handleAdvanceTap() {
        if (restartVisible)
            return;
        if (kind == Endings.Kind.GOOD && goodTransitioning)
            return;

        if (!typewriter.isDone()) {
            typewriter.skip();
            return;
        }

        switch (kind) {
            case NEUTRAL:
                advanceNeutral();
                break;
            case BAD:
                advanceBad();
                break;
            case GOOD:
                advanceGood();
                break;
            default:
                break;
        }
    }

    private void advanceNeutral() {
        if (dialogueIndex < dialogueLines.length - 1) {
            dialogueIndex++;
            typewriter.start(dialogueLines[dialogueIndex]);
            return;
        }
        dialoguePanel.setVisible(false);
        centerTitleLabel.setText("You left them behind.");
        centerTitleLabel.setVisible(true);
        centerTitleLabel.pack();
        centerTitleLabel.setPosition(0f, layoutH * 0.5f - centerTitleLabel.getHeight() * 0.5f);
        centerTitleLabel.setWidth(layoutW);
        centerTitleLabel.clearActions();
        centerTitleLabel.addAction(Actions.fadeIn(1.0f));
        neutralPhaseTimer = 2.2f;
        dialogueIndex = -1;
    }

    private void advanceBad() {
        if (badStep == 0) {
            if (dialogueIndex < dialogueLines.length - 1) {
                if (dialogueIndex == 0)
                    startHeartbeat();
                dialogueIndex++;
                typewriter.start(dialogueLines[dialogueIndex]);
                return;
            }
            enterBadChizoPhase();
            return;
        }
        if (badStep == 1) {
            if (dialogueIndex < dialogueLines.length - 1) {
                dialogueIndex++;
                typewriter.start(dialogueLines[dialogueIndex]);
                return;
            }
            stopHeartbeat();
            dialoguePanel.setVisible(false);
            badStep = 2;
            blinkTimer = 1.35f;
            blinkWhite.setVisible(true);
            return;
        }
    }

    private void enterBadChizoPhase() {
        if (Gdx.files.internal("ends/chizoDeath.png").exists()) {
            ownedChizoTexture = new Texture(Gdx.files.internal("ends/chizoDeath.png"));
            chizoImage.setDrawable(new TextureRegionDrawable(new TextureRegion(ownedChizoTexture)));
            chizoImage.setVisible(true);
            layoutChizo();
        }
        blackBase.getColor().a = 1f;
        mainGame.startEndingMusicBad();
        badStep = 1;
        badShakeTimer = 0f;
        dialogueLines = BAD_PHASE2;
        dialogueIndex = 0;
        typewriter.start(dialogueLines[0]);
        dialoguePanel.setVisible(true);
    }

    private void advanceGood() {
        if (dialogueIndex < dialogueLines.length - 1) {
            dialogueIndex++;
            typewriter.start(dialogueLines[dialogueIndex]);
            return;
        }
        dialoguePanel.setVisible(false);
        startGoodFlashAndCredits();
    }

    private void startGoodFlashAndCredits() {
        goodTransitioning = true;
        if (Gdx.files.internal("ends/goodENDBG.png").exists()) {
            ownedEndBgTexture = new Texture(Gdx.files.internal("ends/goodENDBG.png"));
            endBgImage.setDrawable(new TextureRegionDrawable(new TextureRegion(ownedEndBgTexture)));
        }
        endBgImage.setVisible(true);
        endBgImage.getColor().a = 0f;
        whiteFlash.setVisible(true);
        whiteFlash.getColor().a = 0f;
        whiteFlash.clearActions();
        endBgImage.clearActions();
        endBgImage.addAction(Actions.fadeIn(1.4f));
        whiteFlash.addAction(Actions.sequence(
                Actions.fadeIn(0.25f),
                Actions.fadeOut(0.45f),
                Actions.delay(0.55f),
                Actions.run(() -> {
                    whiteFlash.setVisible(false);
                    beginCreditsScroll();
                    goodTransitioning = false;
                })));
    }

    private void startHeartbeat() {
        if (heartbeatSound == null && Gdx.files.internal("sfx/heart_beat_monster_a_fast.ogg").exists()) {
            heartbeatSound = Gdx.audio.newSound(Gdx.files.internal("sfx/heart_beat_monster_a_fast.ogg"));
        }
        if (heartbeatSound == null)
            return;
        if (heartbeatId != -1)
            heartbeatSound.stop(heartbeatId);
        heartbeatId = heartbeatSound.loop(0.45f);
    }

    private void stopHeartbeat() {
        if (heartbeatSound != null && heartbeatId != -1) {
            heartbeatSound.stop(heartbeatId);
            heartbeatId = -1;
        }
    }

    private void beginCreditsScroll() {
        creditsLabel.setVisible(true);
        creditsRolling = true;
        creditsFinished = false;
        layoutCredits();
        creditsBlockHeight = creditsLabel.getPrefHeight();
        creditsScrollY = -creditsBlockHeight - 20f;
        creditsLabel.setY(creditsScrollY);
    }

    private void showRestart() {
        restartVisible = true;

        // Ensure the button is hidden and disabled
        restartBtn.setVisible(false);
        restartBtn.setTouchable(Touchable.disabled);

        // Move black base to front to cover the credits
        blackBase.toFront();
        blackBase.getColor().a = 0f;

        // Sequence: Fade to black -> Wait 5 seconds -> Return to Title
        blackBase.addAction(Actions.sequence(
            Actions.fadeIn(1.5f),      // Smooth fade to black
            Actions.delay(5.0f),       // Hold for 5 seconds
            Actions.run(() -> {
                if (onRestart != null) {
                    onRestart.run();   // Triggers the Title Screen via Main
                }
            })
        ));
    }

    private float stopScrollTopY() {
        return layoutH * (0.5f + 0.2f);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            handleAdvanceTap();
        }

        typewriter.update(delta);

        if (kind == Endings.Kind.NEUTRAL && dialogueIndex < 0 && neutralPhaseTimer > 0f) {
            neutralPhaseTimer -= delta;
            if (neutralPhaseTimer <= 0f) {
                neutralPhaseTimer = 0f;
                loadNeutralBgAndCredits();
            }
        }

        if (kind == Endings.Kind.BAD && badStep == 1) {
            badShakeTimer += delta;
            float ramp = Math.min(1f, badShakeTimer * 0.12f);
            float amp = 3f + ramp * 18f;
            if (chizoImage.isVisible()) {
                float cx = (layoutW - chizoImage.getWidth()) / 2f;
                float cy = (layoutH - chizoImage.getHeight()) / 2f;
                chizoImage.setPosition(
                        cx + MathUtils.random(-amp, amp),
                        cy + MathUtils.random(-amp, amp));
            }
        }

        if (kind == Endings.Kind.BAD && badStep == 2 && blinkTimer > 0f) {
            blinkTimer -= delta;
            float pulse = (MathUtils.sin(badShakeTimer * 28f) * 0.5f + 0.5f);
            blinkWhite.getColor().a = pulse * 0.85f;
            badShakeTimer += delta;
            if (blinkTimer <= 0f) {
                blinkWhite.setVisible(false);
                blinkWhite.getColor().a = 0f;
                blackBase.getColor().a = 1f;
                chizoImage.setVisible(false);
                centerTitleLabel.setText("Everyone will know what you did.");
                centerTitleLabel.pack();
                centerTitleLabel.setVisible(true);
                centerTitleLabel.setPosition(0f, layoutH * 0.5f - centerTitleLabel.getHeight() * 0.5f);
                centerTitleLabel.setWidth(layoutW);
                centerTitleLabel.getColor().a = 0f;
                centerTitleLabel.clearActions();
                centerTitleLabel.addAction(Actions.fadeIn(1.0f));
                badStep = 3;
                neutralPhaseTimer = 2.0f;
            }
        }

        if (kind == Endings.Kind.BAD && badStep == 3 && neutralPhaseTimer > 0f) {
            neutralPhaseTimer -= delta;
            if (neutralPhaseTimer <= 0f) {
                badStep = 4;
                if (Gdx.files.internal("ends/badENDBG.png").exists()) {
                    if (ownedEndBgTexture != null)
                        ownedEndBgTexture.dispose();
                    ownedEndBgTexture = new Texture(Gdx.files.internal("ends/badENDBG.png"));
                    endBgImage.setDrawable(new TextureRegionDrawable(new TextureRegion(ownedEndBgTexture)));
                }
                endBgImage.setVisible(true);
                endBgImage.getColor().a = 0f;
                endBgImage.clearActions();
                endBgImage.addAction(Actions.fadeIn(1.5f));
                beginCreditsScroll();
            }
        }

        if (creditsRolling && !creditsFinished) {
            float speed = 42f;
            creditsScrollY += speed * delta;
            creditsLabel.setY(creditsScrollY);

            // Stop when the BOTTOM of the credits block (last line) reaches 70% from top
            float lastLineY = creditsScrollY;
            if (lastLineY >= layoutH * 0.10f) {
                creditsFinished = true;
                creditsRolling = false;
                showRestart();
            }
        }
    }

    private void loadNeutralBgAndCredits() {
        if (Gdx.files.internal("ends/neutralENDBG.png").exists()) {
            ownedEndBgTexture = new Texture(Gdx.files.internal("ends/neutralENDBG.png"));
            endBgImage.setDrawable(new TextureRegionDrawable(new TextureRegion(ownedEndBgTexture)));
        }
        endBgImage.setVisible(true);
        endBgImage.getColor().a = 0f;
        endBgImage.clearActions();
        endBgImage.addAction(Actions.fadeIn(1.8f));
        centerTitleLabel.clearActions();
        centerTitleLabel.addAction(Actions.sequence(Actions.fadeOut(0.8f), Actions.hide()));
        beginCreditsScroll();
    }

    @Override
    public void dispose() {
        stopHeartbeat();
        if (heartbeatSound != null) {
            heartbeatSound.dispose();
            heartbeatSound = null;
        }
        if (ownedEndBgTexture != null) {
            ownedEndBgTexture.dispose();
            ownedEndBgTexture = null;
        }
        if (ownedChizoTexture != null) {
            ownedChizoTexture.dispose();
            ownedChizoTexture = null;
        }
    }

    private static final class Typewriter {
        private final Label label;
        private float interval;
        private String text = "";
        private int visibleChars;
        private float timer;
        private boolean done = true;

        Typewriter(Label label, float interval) {
            this.label = label;
            this.interval = interval;
        }

        void setInterval(float interval) {
            this.interval = interval;
        }

        void start(String text) {
            this.text = text == null ? "" : text;
            visibleChars = 0;
            timer = 0f;
            done = false;
            label.setText("");
        }

        void update(float delta) {
            if (done)
                return;
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
