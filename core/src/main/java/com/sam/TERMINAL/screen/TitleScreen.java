package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class TitleScreen {

    public interface TitleScreenListener {
        void onStart(boolean loadExisting);
    }

    private enum Screen { MAIN, OPTIONS, CONTROLS, ABOUT, CREDITS }

    private static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    private static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    private static final Color DISABLED_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);
    private static final int SHADOW_OFFSET_X = 2;
    private static final int SHADOW_OFFSET_Y = -2;
    private static final float SUBMENU_PANEL_WIDTH = 400f;
    private static final float SCROLL_AMOUNT = 40f;

    private final Stage stage;
    private final Texture backgroundTexture;
    private final BitmapFont titleFont;
    private final BitmapFont menuFont;
    private final BitmapFont bodyFont;
    private final TitleScreenListener listener;
    private final boolean hasSaveFile;
    private final Table rootTable;

    private Screen currentScreen = Screen.MAIN;
    private int selectedIndex;
    private ScrollPane creditsScrollPane;
    private Texture scrollBarKnobTexture;
    private Texture scrollBarBgTexture;

    private Sound soundSelect;
    private Sound soundConfirm;
    private Sound soundReturn;

    private float stateTime;

    public TitleScreen(SpriteBatch batch, boolean hasSaveFile, TitleScreenListener listener) {
        this.hasSaveFile = hasSaveFile;
        this.listener = listener;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        stage = new Stage(new ExtendViewport(w, h), batch);

        backgroundTexture = new Texture(Gdx.files.internal("ui/title_background.jpg"));

        titleFont = loadFont("fonts/BIOSfontII.ttf", 72);
        menuFont = loadFont("fonts/BIOSfontII.ttf", 32);
        bodyFont = loadFont("fonts/Abaddon Light.ttf", 22);

        loadSounds();
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        stage.addActor(rootTable);

        showMainMenu();

        InputMultiplexer multi = new InputMultiplexer();
        multi.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                return handleKey(keycode);
            }
        });
        multi.addProcessor(stage);
        Gdx.input.setInputProcessor(multi);
    }

    private void loadSounds() {
        if (Gdx.files.internal("sfx/ui 1 - select b.ogg").exists())
            soundSelect = Gdx.audio.newSound(Gdx.files.internal("sfx/ui 1 - select b.ogg"));
        if (Gdx.files.internal("sfx/ui 2 - confirm b.ogg").exists())
            soundConfirm = Gdx.audio.newSound(Gdx.files.internal("sfx/ui 2 - confirm b.ogg"));
        if (Gdx.files.internal("sfx/ui 3 - return b.ogg").exists())
            soundReturn = Gdx.audio.newSound(Gdx.files.internal("sfx/ui 3 - return b.ogg"));
    }

    private boolean handleKey(int keycode) {
        if (currentScreen == Screen.CREDITS && creditsScrollPane != null) {
            if (keycode == Input.Keys.W || keycode == Input.Keys.UP) {
                float y = Math.max(0, creditsScrollPane.getScrollY() - SCROLL_AMOUNT);
                creditsScrollPane.setScrollY(y);
                return true;
            }
            if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
                float maxY = creditsScrollPane.getMaxY();
                float y = Math.min(maxY, creditsScrollPane.getScrollY() + SCROLL_AMOUNT);
                creditsScrollPane.setScrollY(y);
                return true;
            }
            if (keycode == Input.Keys.X) {
                if (soundReturn != null) soundReturn.play(0.6f);
                showOptionsMenu();
                return true;
            }
            return false;
        }

        int optionsCount = getOptionsCount();
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP) {
            selectedIndex = (selectedIndex - 1 + optionsCount) % optionsCount;
            if (!isOptionEnabled(selectedIndex)) selectedIndex = findNextEnabled(selectedIndex, -1);
            if (soundSelect != null) soundSelect.play(0.6f);
            refreshScreen();
            return true;
        }
        if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
            selectedIndex = (selectedIndex + 1) % optionsCount;
            if (!isOptionEnabled(selectedIndex)) selectedIndex = findNextEnabled(selectedIndex, 1);
            if (soundSelect != null) soundSelect.play(0.6f);
            refreshScreen();
            return true;
        }
        if (keycode == Input.Keys.X) {
            triggerSelected();
            return true;
        }
        return false;
    }

    private int getOptionsCount() {
        switch (currentScreen) {
            case MAIN: return 3;
            case OPTIONS: return 4;
            case CONTROLS: case ABOUT: return 1;
            case CREDITS: return 1;
            default: return 1;
        }
    }

    private boolean isOptionEnabled(int index) {
        if (currentScreen == Screen.MAIN && index == 1 && !hasSaveFile) return false;
        return true;
    }

    private int findNextEnabled(int from, int dir) {
        int n = getOptionsCount();
        for (int i = 1; i < n; i++) {
            int idx = (from + i * dir + n) % n;
            if (isOptionEnabled(idx)) return idx;
        }
        return from;
    }

    private void triggerSelected() {
        switch (currentScreen) {
            case MAIN:
                if (selectedIndex == 0) {
                    if (soundConfirm != null) soundConfirm.play(0.6f);
                    listener.onStart(false);
                } else if (selectedIndex == 1 && hasSaveFile) {
                    if (soundConfirm != null) soundConfirm.play(0.6f);
                    listener.onStart(true);
                } else if (selectedIndex == 1 || selectedIndex == 2) {
                    if (soundConfirm != null) soundConfirm.play(0.6f);
                    showOptionsMenu();
                }
                break;
            case OPTIONS:
                if (soundConfirm != null) soundConfirm.play(0.6f);
                if (selectedIndex == 0) showControls();
                else if (selectedIndex == 1) showAbout();
                else if (selectedIndex == 2) showCredits();
                else showMainMenu();
                break;
            case CONTROLS: case ABOUT: case CREDITS:
                if (soundReturn != null) soundReturn.play(0.6f);
                showOptionsMenu();
                break;
        }
    }

    private void refreshScreen() {
        switch (currentScreen) {
            case MAIN: showMainMenu(); break;
            case OPTIONS: showOptionsMenu(); break;
            case CONTROLS: showControls(); break;
            case ABOUT: showAbout(); break;
            case CREDITS: showCredits(); break;
        }
    }

    private void addHoverAndClickListeners(Group g, int idx, Runnable onRefresh) {
        g.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (selectedIndex != idx && isOptionEnabled(idx)) {
                    selectedIndex = idx;
                    if (soundSelect != null) soundSelect.play(0.6f);
                    onRefresh.run();
                }
            }
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isOptionEnabled(idx)) return;
                selectedIndex = idx;
                triggerSelected();
            }
        });
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
        BitmapFont def = new BitmapFont();
        def.getData().setScale(size / 15f);
        return def;
    }

    private Group createLabelWithShadow(String text, BitmapFont font, Color textColor, Color shadowColor, int alignment) {
        Label shadow = new Label(text, new Label.LabelStyle(font, shadowColor));
        Label main = new Label(text, new Label.LabelStyle(font, textColor));
        shadow.setAlignment(alignment);
        main.setAlignment(alignment);
        shadow.setPosition(SHADOW_OFFSET_X, SHADOW_OFFSET_Y);
        Group group = new Group();
        group.addActor(shadow);
        group.addActor(main);
        group.setSize(main.getPrefWidth() + Math.abs(SHADOW_OFFSET_X) * 2, main.getPrefHeight() + Math.abs(SHADOW_OFFSET_Y) * 2);
        return group;
    }

    private Container<Group> wrapForTable(Group shadowGroup) {
        return new Container<>(shadowGroup).size(shadowGroup.getWidth(), shadowGroup.getHeight());
    }

    private Group createMenuOptionWithSelector(String buttonText, boolean selected, Color textColor) {
        float selectorWidth = new GlyphLayout(menuFont, "> ").width;
        Group selector = createLabelWithShadow(selected ? "> " : "", menuFont, textColor, BLACK, Align.left);
        Group text = createLabelWithShadow(buttonText, menuFont, textColor, BLACK, Align.left);
        Table row = new Table();
        row.add(new Container<>(selector).width(selectorWidth).left()).left();
        row.add(wrapForTable(text)).left();
        row.pack();
        Group out = new Group();
        out.addActor(row);
        out.setSize(row.getWidth(), row.getHeight());
        return out;
    }

    private void showMainMenu() {
        currentScreen = Screen.MAIN;
        if (selectedIndex >= getOptionsCount()) selectedIndex = 0;
        rootTable.clear();

        Group titleGroup = createLabelWithShadow("TERMINAL", titleFont, BLACK, WHITE, Align.center);
        rootTable.add(wrapForTable(titleGroup)).padBottom(60f).row();

        String[] labels = {"NEW GAME", "CONTINUE", "OPTIONS"};
        for (int i = 0; i < labels.length; i++) {
            boolean sel = (selectedIndex == i);
            Color c = (i == 1 && !hasSaveFile) ? DISABLED_COLOR : WHITE;
            Group g = createMenuOptionWithSelector(labels[i], sel, c);
            final int idx = i;
            addHoverAndClickListeners(g, idx, this::showMainMenu);
            rootTable.add(wrapForTable(g)).center().padBottom(24f).row();
        }
    }

    private void showOptionsMenu() {
        currentScreen = Screen.OPTIONS;
        creditsScrollPane = null;
        if (selectedIndex >= 4) selectedIndex = 0;
        rootTable.clear();

        SubmenuPanel panel = new SubmenuPanel(24f);
        String[] labels = {"CONTROLS", "ABOUT", "CREDITS", "BACK"};
        for (int i = 0; i < labels.length; i++) {
            Group g = createMenuOptionWithSelector(labels[i], selectedIndex == i, WHITE);
            final int idx = i;
            addHoverAndClickListeners(g, idx, this::showOptionsMenu);
            panel.add(wrapForTable(g)).center().padBottom(24f).row();
        }
        rootTable.add(panel).width(SUBMENU_PANEL_WIDTH).center();
    }

    private void showControls() {
        currentScreen = Screen.CONTROLS;
        creditsScrollPane = null;
        selectedIndex = 0;
        rootTable.clear();

        SubmenuPanel panel = new SubmenuPanel(24f);
        Group controlsBody = createLabelWithShadow("WASD - Move\nE - Interact\nX - Continue\nTAB - Inventory\nF5 - Quick Save\nESC - (in-game) Settings", bodyFont, WHITE, BLACK, Align.center);
        panel.add(wrapForTable(controlsBody)).center().padBottom(24f).row();
        Group back = createMenuOptionWithSelector("BACK", true, WHITE);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (soundReturn != null) soundReturn.play(0.6f);
                showOptionsMenu();
            }
        });
        panel.add(wrapForTable(back)).center();
        rootTable.add(panel).width(SUBMENU_PANEL_WIDTH).center();
    }

    private void showAbout() {
        currentScreen = Screen.ABOUT;
        creditsScrollPane = null;
        selectedIndex = 0;
        rootTable.clear();

        SubmenuPanel panel = new SubmenuPanel(24f);
        Group aboutBody = createLabelWithShadow("TERMINAL\nA 2D horror maze.\n\nDSA FINAL PROJECT.", bodyFont, WHITE, BLACK, Align.center);
        panel.add(wrapForTable(aboutBody)).center().padBottom(24f).row();
        Group back = createMenuOptionWithSelector("BACK", true, WHITE);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (soundReturn != null) soundReturn.play(0.6f);
                showOptionsMenu();
            }
        });
        panel.add(wrapForTable(back)).center();
        rootTable.add(panel).width(SUBMENU_PANEL_WIDTH).center();
    }

    private void showCredits() {
        currentScreen = Screen.CREDITS;
        selectedIndex = 0;
        rootTable.clear();

        SubmenuPanel panel = new SubmenuPanel(24f);
        String creditsText = "Credits\n\n" +
            "Assets:\n\n" +
            "Fonts: Nimble Beats on itch.io, Caffinate on itch.io\n\n" +
            "UI: Gameboy dialogue/text frame pack by 2bitcrook on itch.io\n\n" +
            "Music: Moonlight by Josh James Lim on itch.io\n\n" +
            "SFX: Survival Horror UI SFX by bedsideseraphim on itch.io\n\n" +
            "Special thanks blahblahblah. Sana pumasa tayo. Godbless.";
        Label creditsLabel = new Label(creditsText, new Label.LabelStyle(bodyFont, WHITE));
        creditsLabel.setWrap(true);
        creditsLabel.setAlignment(Align.center);
        Table textTable = new Table();
        textTable.add(creditsLabel).width(SUBMENU_PANEL_WIDTH - 80f).center();
        ScrollPane scroll = new ScrollPane(textTable, createScrollPaneStyle());
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        creditsScrollPane = scroll;
        panel.add(scroll).width(SUBMENU_PANEL_WIDTH - 48f).height(200f).center().padBottom(24f).row();
        Group back = createMenuOptionWithSelector("BACK", true, WHITE);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (soundReturn != null) soundReturn.play(0.6f);
                showOptionsMenu();
            }
        });
        panel.add(wrapForTable(back)).center();
        rootTable.add(panel).width(SUBMENU_PANEL_WIDTH).center();
    }

    private ScrollPane.ScrollPaneStyle scrollPaneStyle;

    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        if (scrollPaneStyle != null) return scrollPaneStyle;
        scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        Pixmap pm = new Pixmap(10, 20, Pixmap.Format.RGBA8888);
        pm.setColor(0.6f, 0.6f, 0.6f, 0.95f);
        pm.fill();
        scrollBarKnobTexture = new Texture(pm);
        pm.dispose();
        scrollPaneStyle.vScrollKnob = new TextureRegionDrawable(new TextureRegion(scrollBarKnobTexture));
        Pixmap pmBg = new Pixmap(10, 1, Pixmap.Format.RGBA8888);
        pmBg.setColor(0.25f, 0.25f, 0.25f, 0.8f);
        pmBg.fill();
        scrollBarBgTexture = new Texture(pmBg);
        pmBg.dispose();
        scrollPaneStyle.vScroll = new TextureRegionDrawable(new TextureRegion(scrollBarBgTexture));
        return scrollPaneStyle;
    }


    public void render(float delta) {
        stateTime += delta;

        // brightness
        float base = 0.9f + MathUtils.sin(stateTime * 30f) * 0.05f;

        // main flicker
        if (MathUtils.randomBoolean(0.02f)) {
            base = MathUtils.random(0.2f, 0.6f);
        }

        // black drop
        if (MathUtils.randomBoolean(0.01f)) {
            base = 0.05f;
        }

        stage.act(delta);

        Batch batch = stage.getBatch();
        batch.setProjectionMatrix(stage.getCamera().combined);
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();

        batch.begin();
        batch.setColor(base, base, base, 1f);
        batch.draw(backgroundTexture, 0, 0, w, h);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
        if (scrollBarKnobTexture != null) scrollBarKnobTexture.dispose();
        if (scrollBarBgTexture != null) scrollBarBgTexture.dispose();
        if (soundSelect != null) soundSelect.dispose();
        if (soundConfirm != null) soundConfirm.dispose();
        if (soundReturn != null) soundReturn.dispose();
        SubmenuPanel.disposeShared();
        titleFont.dispose();
        menuFont.dispose();
        bodyFont.dispose();
    }
}
