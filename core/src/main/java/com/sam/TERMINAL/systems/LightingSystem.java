package com.sam.TERMINAL.systems;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.sam.TERMINAL.components.LightComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;

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

    public boolean lightingEnabled = true;

    public LightingSystem(OrthographicCamera camera) {
        super(Family.all(
                PlayerComponent.class,
                TransformComponent.class,
                SpriteComponent.class,
                LightComponent.class).get());

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
    public LightComponent createPlayerLight(Entity playerEntity) {
        TransformComponent transform = transformMapper.get(playerEntity);

        ConeLight cone = new ConeLight(
                rayHandler,
                RAY_COUNT,
                CONE_COLOR,
                CONE_DISTANCE,
                transform.pos.x + transform.width / 2f, // initial X center
                transform.pos.y + transform.height / 2f, // initial Y center
                0f, // initial direction (updated each frame)
                CONE_DEGREES / 2f // half-angle (Box2DLights uses half the FOV)
        );
        cone.setSoft(false); // Hard shadows — critical for horror atmosphere

        PointLight point = new PointLight(
                rayHandler,
                30, // even fewer rays needed for a blur
                new Color(1f, 1f, 1f, 0.15f), // incredibly dim, highly subtle white glow
                82f, // radius just covers the immediate area
                transform.pos.x + transform.width / 2f,
                transform.pos.y + transform.height / 2f);
        point.setXray(true); // Passes through walls so it explicitly NEVER casts physical shadows
        point.setSoft(true); // Enables edge blurring
        point.setSoftnessLength(45f); // Massive blur to eliminate the harsh circle edge completely

        LightComponent lightComponent = new LightComponent(cone, point);
        playerEntity.add(lightComponent);
        return lightComponent;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);
        LightComponent light = lightMapper.get(entity);

        if (light == null || light.cone == null) {
            return;
        }

        // 1. Calculate entity center
        float centerX = transform.pos.x + transform.width / 2f;
        float centerY = transform.pos.y + transform.height / 2f;

        // Center the dim PointLight on the player
        if (light.pointLight != null) {
            light.pointLight.setPosition(centerX, centerY);
        }

        // 2. Update cone position with a recalculated offset
        // 10f appropriately anchors the origin of the cone to the chest/hand area
        float offset = 10f;
        float coneX = centerX;
        float coneY = centerY;
        float angle = sprite.facingAngle;

        if (angle == 0f) { // Right
            coneX += offset;
        } else if (angle == 180f) { // Left
            coneX -= offset;
        } else if (angle == 90f) { // Up
            coneY += offset;
        } else if (angle == 270f) { // Down
            coneY -= offset;
        }

        light.cone.setPosition(coneX, coneY);
        light.cone.setDirection(angle);
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
}
