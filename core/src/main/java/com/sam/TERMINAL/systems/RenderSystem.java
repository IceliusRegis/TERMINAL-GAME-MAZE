package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.RoofComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;

/**
 * RenderSystem - Draws all entities with sprites to the screen.
 *
 * Processes entities that have Transform + Sprite components.
 * Advances animation frames and renders them at entity positions.
 */
public class RenderSystem extends IteratingSystem {

    private final SpriteBatch batch;
    private final OrthographicCamera camera; // Add this
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

    public RenderSystem(SpriteBatch batch, OrthographicCamera camera) { // Add camera here
        super(Family.all(TransformComponent.class, SpriteComponent.class).get());
        this.batch = batch;
        this.camera = camera; // Initialize it
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);

        if (!sprite.isStatic){
            sprite.stateTime += deltaTime;
        }

        // Check if the entity is the player and if it is occluded by a wall.
        boolean isPlayer = playerMapper.has(entity);
        if (isPlayer && silhouettesEnabled) {
            if (getPlayerOcclusionState(entity) == 1) {
                // Skip the normal opaque draw ONLY if fully occluded (State 1)
                return;
            }
        TextureRegion currentFrame = null;

        //Advance timer for animation to play
        if (sprite.isStatic) {
            currentFrame = sprite.staticSprite;
        } else if (sprite.currentAnimation != null) {   // If we have a specific animation set (Walk or Idle), use it.
            currentFrame = sprite.currentAnimation.getKeyFrame(sprite.stateTime, sprite.looping);
        }else { // If not (fallback), use walk.
            currentFrame = sprite.walkAnimation.getKeyFrame(sprite.stateTime, sprite.looping);
        }

        // === 1. GET SIZES ===
        // If drawWidth is 0 (forgot to set it), fallback to transform width
        float width = (sprite.drawWidth > 0) ? sprite.drawWidth : transform.width;
        float height = (sprite.drawHeight > 0) ? sprite.drawHeight : transform.height;

        // === 2. CENTER THE IMAGE ===
        // We calculate where to draw so the image is centered over the hitbox
        // Logic: (HitboxX) - (ExtraWidth / 2)
        float drawX = transform.pos.x - (width - transform.width) / 2;
        float drawY = transform.pos.y - (height - transform.height) / 2;

        // Draw
        if (currentFrame != null) {
            float scaleX;
            float scaleY;
            boolean flipX = !sprite.facingRight; // if sprite is facing left this decides it

            if (currentFrame.isFlipX()) {
                flipX = !flipX; //if player chaarcter is already facing left, it toggles it
            }

           batch.draw(currentFrame,
               drawX, drawY, //Position
               width / 2f, height /2f, //Center of Cam
               width, height, //How large to draw
               flipX ? -1f : 1f, 1f, //Draws the flipped version if facing left
               0f); //Rotate in place, or just dont rotate
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
