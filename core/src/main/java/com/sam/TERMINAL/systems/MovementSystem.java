package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.sam.TERMINAL.Main;
import com.sam.TERMINAL.components.*;

public class MovementSystem extends IteratingSystem {

    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<PlayerComponent> playerMapper;

    private com.sam.TERMINAL.buttons.MenuScreen menuScreen;

    // Timer Variables
    private float batteryRespawnTimer = 0f;
    private boolean isWaitingForBattery = false;
    private final float BATTERY_RESPAWN_DELAY = 15f;

    // Footsteps SFX (loop while movement keys are held).
    private Sound footstepsSound;
    private long footstepsSoundId = -1L;

    public MovementSystem() {
        super(Family.all(TransformComponent.class, PlayerComponent.class).get());
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Main game = (Main) Gdx.app.getApplicationListener();
        if (game != null && !game.isTutorialMovementAllowed()) {
            stopFootsteps();
            return;
        }

        if (menuScreen != null && (menuScreen.isSettingsVisible() || menuScreen.isInventoryVisible())) {
            stopFootsteps();
            return;
        }

        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);
        PlayerComponent pc = playerMapper.get(entity);
        BatteryComponent bc = entity.getComponent(BatteryComponent.class);
        InventoryComponent inv = entity.getComponent(InventoryComponent.class);

        // --- 1. BATTERY DRAIN & AUTO-OFF LOGIC ---
        if (bc != null && bc.flashlightOn) {
            bc.battery -= deltaTime * 1.5f; // Adjust drain speed here
            if (bc.battery <= 0) {
                bc.battery = 0;
                bc.flashlightOn = false;
                if (menuScreen != null) {
                    menuScreen.showWarningLabel("BATTERY DEAD! PRESS 'U' TO RECHARGE");
                }
            }
        }

        // --- 2. BATTERY RESPAWN LOGIC (Floor Check) ---
        boolean batteryExistsInWorld = false;
        for (Entity e : getEngine().getEntitiesFor(Family.all(SpriteComponent.class).get())) {
            SpriteComponent sc = spriteMapper.get(e);
            if (sc != null && "battery".equals(sc.name)) {
                batteryExistsInWorld = true;
                break;
            }
        }

        // Enemy spawn is now handled immediately by Main.onLilyTriggered().
        if (menuScreen != null) {
            menuScreen.hideMonsterTimer();
        }

        boolean playerHasBattery = (inv != null && inv.hasItem("battery"));

        if (game != null && game.isLilyTriggered() && !batteryExistsInWorld && !playerHasBattery) {
            if (!isWaitingForBattery) {
                isWaitingForBattery = true;
                batteryRespawnTimer = BATTERY_RESPAWN_DELAY;
            }
            batteryRespawnTimer -= deltaTime;
            if (batteryRespawnTimer <= 0) {
                spawnNewBattery();
                isWaitingForBattery = false;
            }
        } else {
            isWaitingForBattery = false;
        }

        // --- 3. INPUT HANDLING ---

        // Use Battery
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            if (menuScreen != null)
                menuScreen.useBatteryFromInventory();
        }

        boolean playerHasFlashlight = (inv != null && inv.hasItem("flashlight"));

        // Toggle Flashlight with EMPTY warning
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (playerHasFlashlight && bc != null) {
                if (bc.battery <= 0) {
                    if (menuScreen != null)
                        menuScreen.showWarningLabel("BATTERY EMPTY! NEED RECHARGE");
                    bc.flashlightOn = false;
                } else {
                    bc.flashlightOn = !bc.flashlightOn;
                }
            }
        }

        // --- 4. MOVEMENT LOGIC ---
        float xInput = 0;
        float yInput = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W))
            yInput += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S))
            yInput -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A))
            xInput -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D))
            xInput += 1;

        updateFootstepsSound(xInput, yInput);

        if (xInput != 0 || yInput != 0) {
            float angle = (float) Math.toDegrees(Math.atan2(yInput, xInput));
            if (angle < 0)
                angle += 360;
            sprite.facingAngle = angle;

            if (xInput != 0 && yInput != 0) {
                float length = (float) Math.sqrt(xInput * xInput + yInput * yInput);
                xInput /= length;
                yInput /= length;
            }
        }

        float effectiveSpeed = pc.speed;
        if (game != null && !game.isLilyTriggered()) {
            // Pre-entity phase: slightly slower movement.
            effectiveSpeed *= 0.72f;
        }
        float xMove = xInput * effectiveSpeed * deltaTime;
        float yMove = yInput * effectiveSpeed * deltaTime;

        TileWorldComponent world = null;
        if (getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).size() > 0) {
            Entity worldEntity = getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).first();
            world = worldEntity.getComponent(TileWorldComponent.class);
        }

        // X-Axis Collision
        float oldX = transform.pos.x;
        transform.pos.x += xMove;
        transform.updateBounds();
        if (checkEntityCollison(entity, transform) || checkTileCollision(transform, world)) {
            transform.pos.x = oldX;
            transform.updateBounds();
        }

        // Y-Axis Collision
        float oldY = transform.pos.y;
        transform.pos.y += yMove;
        transform.updateBounds();
        if (checkEntityCollison(entity, transform) || checkTileCollision(transform, world)) {
            transform.pos.y = oldY;
            transform.updateBounds();
        }
    }

    private void updateFootstepsSound(float xInput, float yInput) {
        boolean isMovingIntent = (xInput != 0 || yInput != 0);

        if (!isMovingIntent) {
            stopFootsteps();
            return;
        }

        if (footstepsSound == null) {
            if (!Gdx.files.internal("sfx/walking.ogg").exists())
                return;
            footstepsSound = Gdx.audio.newSound(Gdx.files.internal("sfx/walking.ogg"));
        }

        if (footstepsSoundId == -1L && footstepsSound != null) {
            footstepsSoundId = footstepsSound.loop(0.35f);
        }
    }

    private void stopFootsteps() {
        if (footstepsSound != null && footstepsSoundId != -1L) {
            footstepsSound.stop(footstepsSoundId);
            footstepsSoundId = -1L;
        }
    }

    private void spawnNewBattery() {
        TileWorldComponent world = null;
        if (getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).size() > 0) {
            Entity worldEntity = getEngine().getEntitiesFor(Family.all(TileWorldComponent.class).get()).first();
            world = worldEntity.getComponent(TileWorldComponent.class);
        }

        float spawnX = 200f, spawnY = 200f;
        if (world != null) {
            boolean valid = false;
            int attempts = 0;
            while (!valid && attempts < 50) {
                spawnX = com.badlogic.gdx.math.MathUtils.random(50, (world.mapWidthTiles * world.tileWidth) - 50);
                spawnY = com.badlogic.gdx.math.MathUtils.random(50, (world.mapHeightTiles * world.tileHeight) - 50);
                if (!world.isSolid((int) (spawnX / world.tileWidth), (int) (spawnY / world.tileHeight))) {
                    valid = true;
                }
                attempts++;
            }
        }

        com.sam.TERMINAL.entities.EntitySpawner.spawnBattery((com.badlogic.ashley.core.PooledEngine) getEngine(),
                spawnX, spawnY);
        Gdx.app.log("SPAWNER", "Battery respawned at: " + spawnX + ", " + spawnY);
    }

    private boolean checkEntityCollison(Entity player, TransformComponent playerTransform) {
        for (Entity wall : getEngine().getEntitiesFor(Family.all(CollisionComponent.class).get())) {
            if (wall == player)
                continue;
            TransformComponent wallTransform = transformMapper.get(wall);
            if (wallTransform != null && playerTransform.bounds.overlaps(wallTransform.bounds))
                return true;
        }
        return false;
    }

    private boolean checkTileCollision(TransformComponent transform, TileWorldComponent world) {
        if (world == null)
            return false;
        int startX = (int) (transform.bounds.x / world.tileWidth);
        int endX = (int) ((transform.bounds.x + transform.bounds.width) / world.tileWidth);
        int startY = (int) (transform.bounds.y / world.tileHeight);
        int endY = (int) ((transform.bounds.y + transform.bounds.height) / world.tileHeight);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (world.isSolid(x, y))
                    return true;
            }
        }
        return false;
    }

    public void setMenuScreen(com.sam.TERMINAL.buttons.MenuScreen menuScreen) {
        this.menuScreen = menuScreen;
    }

    public void resetEnemyTimer() {
        Gdx.app.log("MOVEMENT_SYSTEM", "Enemy timer disabled (lily-trigger flow)");
    }
}
