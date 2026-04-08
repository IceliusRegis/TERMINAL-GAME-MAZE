package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

/**
 * Three-page maintenance report reader: stacked notebook panels, A/D or click
 * to turn pages,
 * ESC closes. Body text scrolls vertically with a visible scrollbar. Uses
 * {@link PapersNotebookPanel}.
 */
public class PapersReportReader extends Table {

    private static ScrollPane.ScrollPaneStyle sharedScrollStyle;
    private static Texture scrollKnobTexture;
    private static Texture scrollTrackTexture;

    private static ScrollPane.ScrollPaneStyle getSharedScrollStyle() {
        if (sharedScrollStyle != null)
            return sharedScrollStyle;
        sharedScrollStyle = new ScrollPane.ScrollPaneStyle();
        Pixmap pmKnob = new Pixmap(8, 24, Pixmap.Format.RGBA8888);
        pmKnob.setColor(0.45f, 0.48f, 0.52f, 0.98f);
        pmKnob.fill();
        scrollKnobTexture = new Texture(pmKnob);
        pmKnob.dispose();
        sharedScrollStyle.vScrollKnob = new TextureRegionDrawable(new TextureRegion(scrollKnobTexture));

        Pixmap pmTrack = new Pixmap(8, 1, Pixmap.Format.RGBA8888);
        pmTrack.setColor(0.18f, 0.2f, 0.22f, 0.85f);
        pmTrack.fill();
        scrollTrackTexture = new Texture(pmTrack);
        pmTrack.dispose();
        sharedScrollStyle.vScroll = new TextureRegionDrawable(new TextureRegion(scrollTrackTexture));
        return sharedScrollStyle;
    }

    /**
     * Call from game dispose (e.g. MenuScreen.dispose) after reader is no longer
     * used.
     */
    public static void disposeScrollStyle() {
        if (scrollKnobTexture != null) {
            scrollKnobTexture.dispose();
            scrollKnobTexture = null;
        }
        if (scrollTrackTexture != null) {
            scrollTrackTexture.dispose();
            scrollTrackTexture = null;
        }
        sharedScrollStyle = null;
    }

    private static final String[] PAGE_TEXT = {
            "STATION MAINTENANCE — LOG #441-B\n\n"
                    + "Date: [ILLEGIBLE]\n\n"
                    + "Incident: Platform auxiliary lights cycling offline during overtime block. Rail barriers malfunction.\n\n"
                    + "Contractor notes: No immediate changes (unsigned).",
            "INCIDENT REPORT -  420\n\n"
                    + "Date: [ILLEGIBLE]\n\n"
                    + "Complaint filed: strange foul odor near northwest access. Follow-up: staff identified a dead body.\n\n"
                    + "Resolution: Police identified the body of Chizo Kashima, rule as suicide.\n\n"
                    + "Notes: Not enough evidence for suicide, body suffered heavy trauma and environmental factors are ambiguous,",
            "— PERSONAL NOTE — (CONTINUED / SCRATCH PAGES)\n\n"
                    + "Who keeps leaving these doors unlocked after midnight?\n\n"
                    + "This place is trashed. Maintenance reports everywhere. "
                    + "I'm not signing off on this shift until someone explains the mess.\n\n"
                    + "Addendum A — Corridor B7 flicker log:\n"
                    + "22:04 lights dip 12% for 3s. 22:11 repeat. 22:19 smell of ozone, no source found. "
                    + "Camera 4 offline (again). Replacement ETA unknown.\n\n"
                    + "Addendum B — Breakroom:\n"
                    + "Microwave timer runs with door open. Not a wiring fault per checklist #12. "
                    + "Posted \"DO NOT USE\" sign — someone peeled it off.\n\n"
                    + "Addendum C — Tunnel access:\n"
                    + "Grate bolts sheared clean. No tool marks in file photos. Supervisor wrote "
                    + "\"cosmetic\" — I disagree. Draft email to regional (unsent).\n\n"
                    + "Addendum D — Noise complaint:\n"
                    + "Third shift reports footsteps in ceiling void. Structural says \"normal expansion.\" "
                    + "It is not normal at 03:00 in a sealed station.\n\n"
                    + "Addendum E — Inventory mismatch:\n"
                    + "Sixteen (16) high-vis vests logged. Physical count thirteen (13). "
                    + "No requisition forms. No signatures.\n\n"
                    + "Final line for whoever finds this stack:\n"
                    + "If you're reading page after page, the scrollbar is doing its job. "
                    + "Document everything. Date every entry. And don't trust the \"resolved\" stamps."
    };

    private static final float BODY_FONT_SCALE = 0.48f;
    private static final float HINT_FONT_SCALE = 0.62f;
    /** Pixels to move the report scroll position per W / S key press. */
    private static final float SCROLL_KEY_STEP = 52f;

    private final Label bodyLabel;
    private final Label hintLabel;
    private final ScrollPane bodyScroll;
    private final Runnable onClose;
    private int pageIndex;

    public PapersReportReader(BitmapFont font, Runnable onClose) {
        this.onClose = onClose;
        Label.LabelStyle style = new Label.LabelStyle(font, new Color(0.12f, 0.14f, 0.18f, 1f));

        bodyLabel = new Label("", style);
        bodyLabel.setAlignment(Align.topLeft);
        bodyLabel.setWrap(true);
        bodyLabel.setFontScale(BODY_FONT_SCALE);

        hintLabel = new Label("", new Label.LabelStyle(font, new Color(0.25f, 0.28f, 0.32f, 1f)));
        hintLabel.setAlignment(Align.center);
        hintLabel.setFontScale(HINT_FONT_SCALE);

        float sheetW = 380f;
        // Taller notebook area so the scroll viewport is obvious; final page text is
        // long for scrollbar testing.
        float sheetH = 330f;
        float padL = 30f;
        float padR = 28f;
        float padT = 20f;
        float padB = 16f;
        float innerW = sheetW - padL - padR;
        float innerH = sheetH - padT - padB;

        Table scrollInner = new Table();
        scrollInner.top().left();
        scrollInner.add(bodyLabel).width(innerW - 10f).top().left();

        bodyScroll = new ScrollPane(scrollInner, getSharedScrollStyle());
        bodyScroll.setScrollingDisabled(true, false);
        bodyScroll.setFadeScrollBars(false);
        bodyScroll.setScrollbarsOnTop(false);
        bodyScroll.setOverscroll(false, false);

        Group stack = new Group();
        stack.setSize(sheetW + 24f, sheetH + 28f);

        PapersNotebookPanel back = new PapersNotebookPanel(16f);
        back.setSize(sheetW, sheetH);
        back.setPosition(18f, -18f);
        stack.addActor(back);

        PapersNotebookPanel mid = new PapersNotebookPanel(16f);
        mid.setSize(sheetW, sheetH);
        mid.setPosition(9f, -9f);
        stack.addActor(mid);

        PapersNotebookPanel front = new PapersNotebookPanel(16f);
        front.setSize(sheetW, sheetH);
        front.setPosition(0f, 0f);
        front.add(bodyScroll).size(innerW, innerH).pad(padT, padL, padB, padR).top().left();
        stack.addActor(front);

        setFillParent(true);
        center();
        add(stack).padBottom(8f);
        row();
        add(hintLabel).padTop(12f);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isActorOrDescendant(event.getTarget(), bodyScroll)) {
                    return;
                }
                advancePageOrClose();
            }
        });

        setTouchable(Touchable.enabled);

        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
                    if (pageIndex > 0) {
                        pageIndex--;
                        syncPage();
                    }
                    return true;
                }
                if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
                    advancePageOrClose();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    finish();
                    return true;
                }
                // Scroll report body: W up, S down (same sense as typical mouse wheel).
                if (keycode == Input.Keys.W) {
                    bodyScroll.setScrollY(bodyScroll.getScrollY() - SCROLL_KEY_STEP);
                    return true;
                }
                if (keycode == Input.Keys.S) {
                    bodyScroll.setScrollY(bodyScroll.getScrollY() + SCROLL_KEY_STEP);
                    return true;
                }
                return false;
            }
        });

        syncPage();
    }

    private static boolean isActorOrDescendant(Actor leaf, Actor ancestor) {
        for (Actor a = leaf; a != null; a = a.getParent()) {
            if (a == ancestor)
                return true;
        }
        return false;
    }

    public void resetToFirstPage() {
        pageIndex = 0;
        syncPage();
    }

    private void syncPage() {
        bodyLabel.setText(PAGE_TEXT[pageIndex]);
        hintLabel.setText("Page " + (pageIndex + 1) + " / " + PAGE_TEXT.length
                + "     |     A: prev     D / click: next     W/S: scroll     ESC: close");
        if (bodyScroll != null) {
            bodyScroll.layout();
            bodyScroll.validate();
            bodyScroll.setScrollPercentY(0f);
        }
    }

    private void advancePageOrClose() {
        if (pageIndex < PAGE_TEXT.length - 1) {
            pageIndex++;
            syncPage();
        } else {
            finish();
        }
    }

    private void finish() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
