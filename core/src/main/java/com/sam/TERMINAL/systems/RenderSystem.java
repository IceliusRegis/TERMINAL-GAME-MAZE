package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.RoofComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;
import com.sam.TERMINAL.components.WallComponent;

import java.util.Comparator;

/**
 * RenderSystem - Draws all entities with sprites to the screen.
 *
 * Processes entities that have Transform + Sprite components.
 * Y-sorts entities so that those with a higher Y coordinate are drawn first
 * (behind).
 * Also performs a second pass to render player silhouettes when occluded by
 * walls.
 */
public class RenderSystem extends SortedIteratingSystem {

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<PlayerComponent> playerMapper;

    public boolean silhouettesEnabled = true;

    // Arrays to store the anchorY of any wall covering specific body parts
    private final FloatArray feetAnchors = new FloatArray();
    private final FloatArray torsoAnchors = new FloatArray();
    private final FloatArray headAnchors = new FloatArray();

    private static class YComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity e1, Entity e2) {
            TransformComponent t1 = e1.getComponent(TransformComponent.class);
            TransformComponent t2 = e2.getComponent(TransformComponent.class);

            float y1 = t1.pos.y;
            float y2 = t2.pos.y;

            // Apply the new anchor shifts
            WallComponent w1 = e1.getComponent(WallComponent.class);
            if (w1 != null)
                y1 -= w1.sortYShift;
            RoofComponent r1 = e1.getComponent(RoofComponent.class);
            if (r1 != null)
                y1 -= r1.sortYShift;

            WallComponent w2 = e2.getComponent(WallComponent.class);
            if (w2 != null)
                y2 -= w2.sortYShift;
            RoofComponent r2 = e2.getComponent(RoofComponent.class);
            if (r2 != null)
                y2 -= r2.sortYShift;

            // TIE BREAKER: If Y is identical, force the static wall to draw AFTER the
            // player (occluding them)
            if (y1 == y2) {
                boolean e1IsStatic = (w1 != null || r1 != null);
                boolean e2IsStatic = (w2 != null || r2 != null);
                if (e1IsStatic && !e2IsStatic)
                    return 1;
                if (!e1IsStatic && e2IsStatic)
                    return -1;
            }

            return Float.compare(y2, y1);
        }
    }

    public RenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        super(Family.all(TransformComponent.class, SpriteComponent.class).get(), new YComparator());
        this.batch = batch;
        this.camera = camera;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
        this.playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    }

    @Override
    public void update(float deltaTime) {
        // Force sort the backing array every frame to adjust for moving entities
        forceSort();

        // Render all sorted entities normally
        super.update(deltaTime);

        // Secondary pass for silhouettes
        if (silhouettesEnabled) {
            renderSilhouettes();
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);

        if (!sprite.isStatic) {
            sprite.stateTime += deltaTime;
        }

        // Check if the entity is the player and if it is occluded by a wall.
        boolean isPlayer = playerMapper.has(entity);
        if (isPlayer && silhouettesEnabled) {
            if (getPlayerOcclusionState(entity) == 1) {
                // Skip the normal opaque draw ONLY if fully occluded (State 1)
                return;
            }
        }

        TextureRegion currentFrame = getFrame(sprite);

        // === 1. GET SIZES ===
        float width = (sprite.drawWidth > 0) ? sprite.drawWidth : transform.width;
        float height = (sprite.drawHeight > 0) ? sprite.drawHeight : transform.height;

        // === 2. CENTER THE IMAGE ===
        float drawX = transform.pos.x - (width - transform.width) / 2 + sprite.offsetX;
        float drawY = transform.pos.y - (height - transform.height) / 2 + sprite.offsetY;

        // Draw
        if (currentFrame != null) {
            float scaleX;
            float scaleY;

            if (sprite.isStatic) {
                scaleX = sprite.flipX ? -1f : 1f;
                scaleY = sprite.flipY ? -1f : 1f;
            } else {
                boolean flipX = !sprite.facingRight;
                if (currentFrame.isFlipX()) {
                    flipX = !flipX;
                }
                scaleX = flipX ? -1f : 1f;
                scaleY = 1f;
            }

            batch.draw(currentFrame,
                    drawX, drawY,
                    width / 2f, height / 2f,
                    width, height,
                    scaleX, scaleY,
                    0f);
        }
    }

    private TextureRegion getFrame(SpriteComponent sprite) {
        if (sprite.isStatic) {
            return sprite.staticSprite;
        } else if (sprite.currentAnimation != null) {
            return sprite.currentAnimation.getKeyFrame(sprite.stateTime, sprite.looping);
        } else {
            return sprite.walkAnimation.getKeyFrame(sprite.stateTime, sprite.looping);
        }
    }

    /**
     * Returns:
     * 0 = Visible OR Partially Covered (Normal Draw - Y-sorting clips the legs
     * naturally)
     * 1 = Fully Occluded (Silhouette Draw)
     */
    private int getPlayerOcclusionState(Entity player) {
        TransformComponent pTransform = transformMapper.get(player);
        Rectangle pBounds = pTransform.bounds;

        // --- THE FIX: Adjusted measurements to match visual character height ---
        // The visual character is roughly ~96 pixels tall.

        float feetX = pBounds.x + pBounds.width / 2f;
        float feetY = pBounds.y + 2f; // Base level

        float torsoX = feetX;
        float torsoY = pBounds.y + 45f; // Middle of character (~1.5 tiles up)

        float headX = feetX;
        float headY = pBounds.y + 90f; // Top of character's head (~3 tiles up)
        // --------------------------------------------------------------------

        float playerY = pTransform.pos.y;

        com.badlogic.ashley.utils.ImmutableArray<Entity> walls = getEngine().getEntitiesFor(
                Family.all(WallComponent.class, TransformComponent.class).get());
        com.badlogic.ashley.utils.ImmutableArray<Entity> roofs = getEngine().getEntitiesFor(
                Family.all(RoofComponent.class, TransformComponent.class).get());

        // 1. Gather anchor points for walls covering the feet
        feetAnchors.clear();
        getOccludingAnchors(feetX, feetY, playerY, walls, roofs, feetAnchors);
        if (feetAnchors.isEmpty())
            return 0; // Quick exit if feet aren't hidden

        // 2. Gather anchor points for walls covering the torso
        torsoAnchors.clear();
        getOccludingAnchors(torsoX, torsoY, playerY, walls, roofs, torsoAnchors);
        if (torsoAnchors.isEmpty())
            return 0; // Quick exit if torso isn't hidden

        // 3. Gather anchor points for walls covering the head
        headAnchors.clear();
        getOccludingAnchors(headX, headY, playerY, walls, roofs, headAnchors);
        if (headAnchors.isEmpty())
            return 0; // Quick exit if head isn't hidden

        // 4. CHECK CONTIGUOUS STRUCTURE:
        // If any anchorY from the feet matches an anchorY in the torso AND head,
        // they are blocked by the SAME contiguous wall structure.
        for (int i = 0; i < feetAnchors.size; i++) {
            float anchor = feetAnchors.items[i];
            if (torsoAnchors.contains(anchor) && headAnchors.contains(anchor)) {
                return 1; // Silhouette ON
            }
        }

        // Points are covered, but by completely different walls (Corridor condition)
        return 0; // Normal Draw
    }

    /**
     * Populates a FloatArray with the anchorY values of any walls that overlap a
     * specific point.
     */
    private void getOccludingAnchors(float px, float py, float playerY,
            com.badlogic.ashley.utils.ImmutableArray<Entity> walls,
            com.badlogic.ashley.utils.ImmutableArray<Entity> roofs,
            FloatArray outAnchors) {

        // Check Wall Faces
        for (int j = 0; j < walls.size(); ++j) {
            Entity wall = walls.get(j);
            TransformComponent wTrans = transformMapper.get(wall);
            float anchorY = wTrans.pos.y - wall.getComponent(WallComponent.class).sortYShift;
            if (playerY > anchorY && wTrans.bounds.contains(px, py)) {
                if (!outAnchors.contains(anchorY))
                    outAnchors.add(anchorY);
            }
        }

        // Check Roofs
        for (int j = 0; j < roofs.size(); ++j) {
            Entity roof = roofs.get(j);
            TransformComponent rTrans = transformMapper.get(roof);
            float anchorY = rTrans.pos.y - roof.getComponent(RoofComponent.class).sortYShift;
            if (playerY > anchorY && rTrans.bounds.contains(px, py)) {
                if (!outAnchors.contains(anchorY))
                    outAnchors.add(anchorY);
            }
        }
    }

    private void renderSilhouettes() {
        com.badlogic.ashley.utils.ImmutableArray<Entity> players = getEngine().getEntitiesFor(
                Family.all(PlayerComponent.class, TransformComponent.class, SpriteComponent.class).get());

        if (players.size() == 0)
            return;

        for (int i = 0; i < players.size(); ++i) {
            Entity player = players.get(i);

            // Render the silhouette ONLY if fully occluded (State 1)
            if (getPlayerOcclusionState(player) == 1) {
                TransformComponent pTransform = transformMapper.get(player);
                SpriteComponent pSprite = spriteMapper.get(player);
                TextureRegion frame = getFrame(pSprite);

                if (frame != null) {
                    float width = (pSprite.drawWidth > 0) ? pSprite.drawWidth : pTransform.width;
                    float height = (pSprite.drawHeight > 0) ? pSprite.drawHeight : pTransform.height;
                    float drawX = pTransform.pos.x - (width - pTransform.width) / 2 + pSprite.offsetX;
                    float drawY = pTransform.pos.y - (height - pTransform.height) / 2 + pSprite.offsetY;

                    boolean flipX = !pSprite.facingRight;
                    if (frame.isFlipX()) {
                        flipX = !flipX;
                    }

                    // Dark tint at ~55% opacity
                    batch.setColor(0.1f, 0.1f, 0.15f, 0.55f);
                    batch.draw(frame, drawX, drawY, width / 2f, height / 2f, width, height, flipX ? -1f : 1f, 1f, 0f);
                    batch.setColor(Color.WHITE); // Reset immediately
                }
            }
        }
    }
}