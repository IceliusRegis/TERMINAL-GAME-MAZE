package com.sam.TERMINAL.systems;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.sam.TERMINAL.components.LightComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;
import com.sam.TERMINAL.components.BatteryComponent;
import com.sam.TERMINAL.components.InventoryComponent;
import com.sam.TERMINAL.components.StaticLightComponent;

/**
 * LightingSystem — Manages Box2DLights for the player's FOV cone.
 *
 * Responsibilities:
 * - Owns the Box2D World (zero-gravity, no physics stepping).
 * - Owns the RayHandler (renders light + shadow).
 * - Updates the player ConeLight position and direction each frame.
 * - Exposes render(camera) to be called from Main.render() outside SpriteBatch.
 *
 * // TODO: Phase 2 — Add static bodies for wall shadow casting.
 * Currently the cone illuminates through walls because no Box2D
 * geometry exists. Creating a StaticBody chain for every solid TMX
 * tile is a Phase 2 polish item.
 */
public class LightingSystem extends IteratingSystem {

    // --- Constants ---
    private static final int RAY_COUNT = 128; // More rays = smoother cone edges
    private static final float CONE_DISTANCE = 280f; // How far the player can see (pixels)
    private static final float CONE_DEGREES = 90f; // Focused, narrow flashlight beam
    private static final Color CONE_COLOR = new Color(1f, 0.95f, 0.85f, 0.5f); // Dimmer warm white
    private static final Color AMBIENT_COLOR = new Color(0f, 0f, 0f, 1f); // Pitch black

    // --- Box2DLights Core ---
    private final World box2dWorld; // Dummy physics world — never stepped
    private final RayHandler rayHandler;

    // --- Camera Reference (for render) ---
    private final OrthographicCamera camera;

    // --- Component Mappers ---
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper
        .getFor(TransformComponent.class);
    private final ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private final ComponentMapper<LightComponent> lightMapper = ComponentMapper.getFor(LightComponent.class);
    private final ComponentMapper<StaticLightComponent> staticLightMapper = ComponentMapper.getFor(StaticLightComponent.class);

    public boolean lightingEnabled = true;
    private com.sam.TERMINAL.buttons.MenuScreen menuScreen;

    public LightingSystem(OrthographicCamera camera) {
        super(Family.all(TransformComponent.class).one(LightComponent.class, StaticLightComponent.class).get());

        this.camera = camera;

        // 1. Create a zero-gravity world. We never call world.step() — it is
        // purely a container for Box2DLights' ray-casting geometry.
        box2dWorld = new World(new Vector2(0, 0), true);

        // 2. Create the RayHandler with the dummy world.
        RayHandler.setGammaCorrection(true);
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(box2dWorld);
        rayHandler.setAmbientLight(AMBIENT_COLOR);
    }

    /**
     * Creates a ConeLight for the player and attaches it via a LightComponent.
     * Call this from Main.java after spawning the player entity.
     *
     * @param playerEntity The player entity (must already have TransformComponent +
     *                     SpriteComponent)
     * @return The newly created LightComponent for reference
     */
    public LightComponent createPlayerLight(Entity playerEntity, boolean hasFlashlight) {
        TransformComponent transform = transformMapper.get(playerEntity);

        // --- LIGHT CONFIGURATION ---
        // If no flashlight, the cone is tiny/invisible. If hasFlashlight, it's your 90+80 degree beam.
        float finalDistance = hasFlashlight ? (CONE_DISTANCE + 100f) : 0f;
        float finalDegrees = hasFlashlight ? (CONE_DEGREES + 100f) : 0f;

        // The "Small Circle" around the player
        // We make it slightly larger if they don't have a flashlight so they can at least see their feet.
        float finalPointRadius = hasFlashlight ? 150f : 180f;
        float brightness = hasFlashlight ? 0.7f : 0.5f;

        // Cleanup old light
        if (lightMapper.has(playerEntity)) {
            LightComponent oldLight = lightMapper.get(playerEntity);
            if (oldLight.cone != null) oldLight.cone.remove();
            if (oldLight.pointLight != null) oldLight.pointLight.remove();
            playerEntity.remove(LightComponent.class);
        }

        // Create the Cone (Flashlight Beam)
        ConeLight cone = new ConeLight(
            rayHandler,
            RAY_COUNT,
            CONE_COLOR,
            finalDistance,
            transform.pos.x + transform.width / 2f,
            transform.pos.y + transform.height / 2f,
            0f,
            finalDegrees / 2f
        );
        cone.setSoft(false);
        // If no flashlight, make the cone effectively inactive
        cone.setActive(hasFlashlight);

        // Create the PointLight (The "Small Circle" around player)
        PointLight point = new PointLight(
            rayHandler,
            30,
            new Color(1f, 1f, 1f, brightness), // Very dim white
            finalPointRadius,
            transform.pos.x + transform.width / 2f,
            transform.pos.y + transform.height / 2f
        );

        point.setXray(true);
        point.setSoft(true);
        point.setSoftnessLength(45f);

        LightComponent lightComponent = new LightComponent(cone, point);
        playerEntity.add(lightComponent);

        return lightComponent;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        // 1. Menu check
        if (menuScreen != null && (menuScreen.isSettingsVisible() || menuScreen.isInventoryVisible())) {
            return;
        }

        TransformComponent transform = transformMapper.get(entity);
        StaticLightComponent staticLight = staticLightMapper.get(entity);

        if (staticLight != null) {
            if (staticLight.pointLight == null) {
                float centerX = transform.pos.x + transform.width / 2f;
                float centerY = transform.pos.y + transform.height / 2f;
                staticLight.pointLight = new PointLight(
                    rayHandler,
                    50,
                    new Color(1f, 0.95f, 0.85f, 0.8f), 
                    250f,
                    centerX,
                    centerY
                );
                staticLight.pointLight.setXray(true);
                staticLight.pointLight.setSoft(true);
                staticLight.pointLight.setSoftnessLength(60f);
            }
            return;
        }

        SpriteComponent sprite = spriteMapper.get(entity);
        LightComponent light = lightMapper.get(entity);

        if (light == null || light.cone == null || sprite == null) return;

        // 2. Update Position to Player Center
        float centerX = transform.pos.x + transform.width / 2f;
        float centerY = transform.pos.y + transform.height / 2f;
        light.cone.setPosition(centerX, centerY);

        // 3. Smooth Turning
        float targetAngle = sprite.facingAngle;
        float currentDir = light.cone.getDirection();
        float smoothAngle = com.badlogic.gdx.math.MathUtils.lerpAngleDeg(currentDir, targetAngle, 0.15f);
        light.cone.setDirection(smoothAngle);

        if (light.pointLight != null) {
            light.pointLight.setPosition(centerX, centerY);
        }

        // --- UPDATED FLASH LIGHT BATTERY LOGIC ---
        BatteryComponent batteryState = entity.getComponent(BatteryComponent.class);
        InventoryComponent inv = entity.getComponent(InventoryComponent.class);
        boolean hasFlashlight = (inv != null && inv.hasItem("flashlight"));

        if (batteryState != null) {
            // Sync cone activity
            if (light.cone != null) {
                // Light is active only if you have the item AND it's toggled on
                light.cone.setActive(hasFlashlight && batteryState.flashlightOn);
            }
        }
    }

    /**
     * Renders the lighting layer. Must be called from Main.render() AFTER
     * batch.end() and BEFORE menuScreen.render().
     *
     * Full draw order in Main.render():
     * 1. mapManager.render(camera) — TMX tiles
     * 2. batch.begin() / engine.update() / batch.end() — Sprites
     * 3. lightingSystem.render() — Lighting overlay
     * 4. menuScreen.render(delta) — UI
     */
    public void render() {
        if (!lightingEnabled)
            return;

        rayHandler.setCombinedMatrix(camera.combined);
        rayHandler.updateAndRender();
    }

    @Override
    public void removedFromEngine(Engine engine) {
        dispose();
    }

    /**
     * Disposes Box2DLights resources. The RayHandler MUST be disposed before
     * the World to prevent EXCEPTION_ACCESS_VIOLATION errors.
     */
    public void dispose() {
        rayHandler.dispose();
        box2dWorld.dispose();
    }

    public void setMenuScreen(com.sam.TERMINAL.buttons.MenuScreen menuScreen) {
        this.menuScreen = menuScreen;
    }

}
