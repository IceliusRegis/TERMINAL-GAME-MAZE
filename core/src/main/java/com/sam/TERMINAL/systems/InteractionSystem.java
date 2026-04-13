package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sam.TERMINAL.Main;
import com.sam.TERMINAL.components.*;

/**
 * InteractionSystem - Handles the logic for using objects in the world.
 *
 * Responsibilities:
 * 1. Checks distance between Player and all Interactable entities.
 * 2. Performs tile-based line-of-sight checks to prevent interaction through
 * walls.
 * 3. Sets the nearPlayer flag on InteractableComponent for RenderSystem to use.
 * 4. Visualizes prompt ("Press E") if close enough and line-of-sight is clear.
 * 5. Listens for the 'E' key input.
 * 6. Executes specific logic based on item type (Key -> Pickup, Flashlight ->
 * Pickup).
 *
 * Note: This extends EntitySystem (not IteratingSystem) because we need to
 * compare one entity (Player) against many others (Items) manually.
 */
public class InteractionSystem extends EntitySystem {

    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<InteractableComponent> interactMapper;
    private ComponentMapper<InventoryComponent> inventoryMapper;

    // Rendering context for the "Press E" prompt
    private final SpriteBatch batch;
    private Texture promptTexture;
    private TextureRegion promptRegion;

    // Tracks the most recent entity that is in range and has clear LoS,
    // so renderPrompts() can draw the indicator after the lighting pass.
    private TransformComponent nearestTargetTransform = null;

    private static final float PROMPT_WIDTH = 24f;
    private static final float PROMPT_HEIGHT = 24f;
    private static final float PROMPT_OFFSET_Y = 8f; // pixels above the entity top
    private static final float TILE_SIZE = 32f;
    private boolean lilyPromptShown = false;
    private boolean papersPromptShown = false;
    private boolean studIDPromptShown = false;
    private boolean proximityNarrativeActive = false;

    public InteractionSystem(SpriteBatch batch) {
        this.batch = batch;

        // Null-guarded asset load — game runs fine if the file is missing
        if (Gdx.files.internal("ui/press_e.png").exists()) {
            promptTexture = new Texture(Gdx.files.internal("ui/press_e.png"));
            promptRegion = new TextureRegion(promptTexture);
        }

        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        interactMapper = ComponentMapper.getFor(InteractableComponent.class);
        inventoryMapper = ComponentMapper.getFor(InventoryComponent.class);
    }

    @Override
    public void update(float deltaTime) {
        // Clear the tracked target at the top of every frame.
        nearestTargetTransform = null;

        // 1.) Find the player tag and their position first
        ImmutableArray<Entity> players = getEngine()
                .getEntitiesFor(Family.all(PlayerComponent.class, TransformComponent.class).get());

        if (players.size() == 0)
            return;

        Entity player = players.first();
        TransformComponent playerPos = transformMapper.get(player);

        // 2.) Find all interactables
        ImmutableArray<Entity> interactables = getEngine()
                .getEntitiesFor(Family.all(InteractableComponent.class, TransformComponent.class).get());

        // Get the world component for line-of-sight checks
        ImmutableArray<Entity> worldEntities = getEngine()
                .getEntitiesFor(Family.all(TileWorldComponent.class).get());
        TileWorldComponent world = (worldEntities.size() > 0)
                ? worldEntities.first().getComponent(TileWorldComponent.class)
                : null;

        // Pre-pass: reset nearPlayer on all interactables
        for (Entity target : interactables) {
            InteractableComponent interact = interactMapper.get(target);
            interact.nearPlayer = false;
        }

        // 3.) Check distance and line-of-sight for each item
        boolean nearLilyThisFrame = false;
        boolean nearPapersThisFrame = false;
        boolean nearStudIDThisFrame = false;
        for (Entity target : interactables) {
            InteractableComponent interact = interactMapper.get(target);
            if (!interact.isActive)
                continue;

            TransformComponent targetPos = transformMapper.get(target);

            // Center-to-center distance
            float playerCenterX = playerPos.pos.x + (playerPos.width / 2f);
            float playerCenterY = playerPos.pos.y + (playerPos.height / 2f);
            float targetCenterX = targetPos.pos.x + (targetPos.width / 2f);
            float targetCenterY = targetPos.pos.y + (targetPos.height / 2f);
            float dist = (float) Math.sqrt(
                    Math.pow(playerCenterX - targetCenterX, 2) +
                            Math.pow(playerCenterY - targetCenterY, 2));

            if (dist <= interact.radius) {
                // Line-of-sight check: ensure no wall between player and item
                boolean hasLineOfSight = true;
                if (world != null) {
                    int playerTileX = (int) (playerCenterX / TILE_SIZE);
                    int playerTileY = (int) (playerCenterY / TILE_SIZE);
                    int targetTileX = (int) (targetCenterX / TILE_SIZE);
                    int targetTileY = (int) (targetCenterY / TILE_SIZE);
                    hasLineOfSight = checkLineOfSight(world, playerTileX, playerTileY, targetTileX, targetTileY);
                }

                if (hasLineOfSight) {
                    interact.nearPlayer = true;
                    if ("lily".equals(interact.type)) {
                        nearLilyThisFrame = true;
                        if (!lilyPromptShown) {
                            Main game = (Main) Gdx.app.getApplicationListener();
                            if (game != null && game.getMenuScreen() != null) {
                                game.getMenuScreen().showNarrativeDialog("A lily..? What's it doing here?");
                            }
                            lilyPromptShown = true;
                            proximityNarrativeActive = true;
                        }
                    }
                    if ("papers".equals(interact.type)) {
                        nearPapersThisFrame = true;
                        if (!papersPromptShown) {
                            Main game = (Main) Gdx.app.getApplicationListener();
                            if (game != null && game.getMenuScreen() != null) {
                                game.getMenuScreen().showNarrativeDialog(
                                        "Ugh.. maintenance reports... who trashed this place up?");
                            }
                            papersPromptShown = true;
                            proximityNarrativeActive = true;
                        }
                    }
                    if ("studID".equals(interact.type)) {
                        nearStudIDThisFrame = true;
                        if (!studIDPromptShown) {
                            Main game = (Main) Gdx.app.getApplicationListener();
                            if (game != null && game.getMenuScreen() != null) {
                                InventoryComponent inv = inventoryMapper.get(player);
                                boolean hasPapers = inv != null && inv.hasItem("papers");
                                String msg = "Another id.\n\nThis time it's... Chizo Kashima. Hah.";
                                if (hasPapers) {
                                    msg += "\n\nWait... isn't this the same girl...?\n\nThis is making my head hurt..";
                                }
                                game.getMenuScreen().showNarrativeDialog(msg, 18f);
                            }
                            studIDPromptShown = true;
                            proximityNarrativeActive = true;
                        }
                    }

                    // Record this transform so renderPrompts() can draw above it.
                    nearestTargetTransform = targetPos;

                    // Receive interact input
                    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        executeInteraction(player, target, interact);
                    }
                }
            }
        }

        if (!nearLilyThisFrame) {
            lilyPromptShown = false;
        }
        if (!nearPapersThisFrame) {
            papersPromptShown = false;
        }
        if (!nearStudIDThisFrame) {
            studIDPromptShown = false;
        }

        // Dismiss proximity narrative when the player walks away from all items
        if (proximityNarrativeActive && !nearLilyThisFrame && !nearPapersThisFrame && !nearStudIDThisFrame) {
            Main game = (Main) Gdx.app.getApplicationListener();
            if (game != null && game.getMenuScreen() != null) {
                game.getMenuScreen().hideNarrativeDialog();
            }
            proximityNarrativeActive = false;
        }
    }

    /**
     * Draws the "Press E" prompt above the nearest in-range interactable.
     * Called by Main.renderInteractionPrompts() AFTER the lighting pass so the
     * indicator is never blacked out by the ambient darkness overlay.
     */
    public void renderPrompts() {
        if (promptRegion == null || nearestTargetTransform == null) {
            return;
        }
        float targetCenterX = nearestTargetTransform.pos.x + (nearestTargetTransform.width / 2f);
        float promptX = targetCenterX - PROMPT_WIDTH / 2f;
        float promptY = nearestTargetTransform.pos.y + nearestTargetTransform.height + PROMPT_OFFSET_Y;
        batch.draw(promptRegion, promptX, promptY, PROMPT_WIDTH, PROMPT_HEIGHT);
    }

    /**
     * Bresenham line-of-sight check on the Walls layer.
     * Returns true if there is no wall tile between the two points.
     */
    private boolean checkLineOfSight(TileWorldComponent world, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        int currentX = x0;
        int currentY = y0;

        while (true) {
            // Skip checking the start and end tiles themselves
            if ((currentX != x0 || currentY != y0) && (currentX != x1 || currentY != y1)) {
                if (world.isWall(currentX, currentY)) {
                    return false; // Wall blocks line of sight
                }
            }

            // Reached the target tile
            if (currentX == x1 && currentY == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }

        return true; // Clear line of sight
    }

    private void executeInteraction(Entity player, Entity target, InteractableComponent typeData) {
        InventoryComponent inventory = inventoryMapper.get(player);

        switch (typeData.type) {
            case "beep":
                System.out.println("Picked up a BEEP CARD!");
                if (inventory != null)
                    inventory.addItem("beep_card");
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                break;

            case "flashlight":
                System.out.println("Picked up the FLASHLIGHT!");
                if (inventory != null) {
                    inventory.addItem("flashlight");
                }
                LightingSystem lightingSystem = getEngine().getSystem(LightingSystem.class);
                if (lightingSystem != null) {
                    lightingSystem.createPlayerLight(player, true);
                }
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                break;

            case "battery":
                System.out.println("Picked up a BATTERY!");
                if (inventory != null) {
                    inventory.addItem("battery");
                }
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                break;
            case "potion": // ADD THIS CASE
                System.out.println("Picked up a SPEED POTION!");
                if (inventory != null) {
                    // This adds the string "potion" to your InventoryComponent's list
                    inventory.addItem("potion");
                }
                // Hide the potion from the world
                target.remove(SpriteComponent.class);
                typeData.isActive = false;

                // Optional: You can play a small sound effect here if you have one
                break;

            case "lily":
                System.out.println("Interacted with LILY trigger!");
                // Collect LilyOUTLINED into inventory (item id "lily"); shown in inventory UI via getLilyRegion().
                if (inventory != null && !inventory.hasItem("lily")) {
                    inventory.addItem("lily");
                }
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                try {
                    Main game = (Main) Gdx.app.getApplicationListener();
                    if (game != null && game.getMenuScreen() != null) {
                        game.getMenuScreen().hideNarrativeDialog();
                    }
                    if (game != null) {
                        game.onLilyTriggered();
                        if (game.getMenuScreen() != null) {
                            game.getMenuScreen().refreshInventoryDisplay();
                        }
                    }
                } catch (Exception ignored) {
                }
                break;

            case "studID":
                System.out.println("Picked up STUDENT ID!");
                if (inventory != null) {
                    inventory.addItem("studID");
                }
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                try {
                    Main game = (Main) Gdx.app.getApplicationListener();
                    if (game != null && game.getMenuScreen() != null) {
                        game.getMenuScreen().hideNarrativeDialog();
                        game.getMenuScreen().refreshInventoryDisplay();
                        // If the player already has papers, show the connecting revelation
                        if (inventory != null && inventory.hasItem("papers")) {
                            game.getMenuScreen().showNarrativeDialog(
                                "Chizo Kashima...\n\nWait... isn't this the same girl from those reports...?\n\nThis is making my head hurt..", 6f);
                        }
                    }
                } catch (Exception ignored) {
                }
                break;

            case "papers":
                System.out.println("Picked up PAPERS!");
                if (inventory != null) {
                    inventory.addItem("papers");
                }
                target.remove(SpriteComponent.class);
                typeData.isActive = false;
                try {
                    Main game = (Main) Gdx.app.getApplicationListener();
                    if (game != null && game.getMenuScreen() != null) {
                        game.getMenuScreen().hideNarrativeDialog();
                        game.getMenuScreen().refreshInventoryDisplay();
                        game.getMenuScreen().showPapersReport();
                    }
                    // If the player already picked up the studID, show the
                    // connecting revelation after a short delay so it appears
                    // after the papers reader is closed.
                    if (inventory != null && inventory.hasItem("studID")) {
                        System.out.println("[hasPapers] Papers picked up with studID already in inventory — queuing revelation.");
                    }
                } catch (Exception ignored) {
                }
                break;

            case "confrontation":
                if (inventory != null && inventory.hasItem("lily")) {
                    System.out.println("Confrontation triggered! Lily consumed.");
                    inventory.removeItem("lily");
                    // REMOVED: typeData.isActive = false;  ← delete this line

                    try {
                        com.sam.TERMINAL.Main game2 = (com.sam.TERMINAL.Main) Gdx.app.getApplicationListener();
                        if (game2 != null) {
                            TransformComponent targetPos2 = transformMapper.get(target);
                            float ghostX = targetPos2.pos.x;
                            float ghostY = targetPos2.pos.y;
                            com.sam.TERMINAL.entities.EntityFactory.createEnemy(
                                (com.badlogic.ashley.core.PooledEngine) getEngine(),
                                ghostX, ghostY, game2.getEnemyAnimation(),
                                com.sam.TERMINAL.Main.ENEMY_DRAW_W, com.sam.TERMINAL.Main.ENEMY_DRAW_H);

                            if (game2.getMenuScreen() != null) {
                                game2.getMenuScreen().showConfrontation();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                } else {
                    try {
                        com.sam.TERMINAL.Main game3 = (com.sam.TERMINAL.Main) Gdx.app.getApplicationListener();
                        if (game3 != null && game3.getMenuScreen() != null) {
                            game3.getMenuScreen().showNarrativeDialog("You need something to offer...", 3f);
                        }
                    } catch (Exception ignored) {
                    }
                }
                break;

            default:
                System.out.println("Interacted with " + typeData.type);
        }
    }

    public void dispose() {
        if (promptTexture != null) {
            promptTexture.dispose();
            promptTexture = null;
        }
    }
}
