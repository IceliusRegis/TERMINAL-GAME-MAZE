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
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.SpriteComponent;
import com.sam.TERMINAL.components.TransformComponent;
import com.sam.TERMINAL.components.WallComponent;

import java.util.Comparator;

/**
 * RenderSystem - Draws all entities with sprites to the screen.
 *
 * Processes entities that have Transform + Sprite components.
 * Y-sorts entities so that those with a higher Y coordinate are drawn first (behind).
 * Also performs a second pass to render player silhouettes when occluded by walls.
 */
public class RenderSystem extends SortedIteratingSystem {

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<PlayerComponent> playerMapper;



    private static class YComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity e1, Entity e2) {
            TransformComponent t1 = e1.getComponent(TransformComponent.class);
            TransformComponent t2 = e2.getComponent(TransformComponent.class);
            // Higher Y drawn first = descending sort.
            return Float.compare(t2.pos.y, t1.pos.y);
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
        renderSilhouettes();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);

        if (!sprite.isStatic){
            sprite.stateTime += deltaTime;
        }

        // Check if the entity is the player and if it is occluded by a wall.
        // If occluded, we skip the normal opaque draw pass so the entire sprite
        // can be drawn exclusively as a translucent silhouette in the secondary pass.
        boolean isPlayer = playerMapper.has(entity);
        if (isPlayer && isPlayerOccluded(entity)) {
            return;
        }

        TextureRegion currentFrame = getFrame(sprite);

        // === 1. GET SIZES ===
        float width = (sprite.drawWidth > 0) ? sprite.drawWidth : transform.width;
        float height = (sprite.drawHeight > 0) ? sprite.drawHeight : transform.height;

        // === 2. CENTER THE IMAGE ===
        float drawX = transform.pos.x - (width - transform.width) / 2;
        float drawY = transform.pos.y - (height - transform.height) / 2;

        // Draw
        if (currentFrame != null) {
            // Determine flip axes:
            // Static tiles use Tiled editor metadata (sprite.flipX / flipY).
            // Animated sprites use the runtime facingRight direction.
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
     * Determines if the player is visually occluded by any wall tile.
     *
     * Uses a strict "Point vs. Box" containment check: calculates the center
     * point of the player's torso (horizontal and vertical midpoint of bounds)
     * and checks whether that single point falls inside any occluding wall's
     * bounding box.  This eliminates false positives from invisible texture
     * boundaries that a rectangle-overlap approach would catch.
     */
    private boolean isPlayerOccluded(Entity player) {
        TransformComponent pTransform = transformMapper.get(player);
        Rectangle pBounds = pTransform.bounds;

        // Single torso center-point derived from the player's collision bounds
        float torsoX = pBounds.x + pBounds.width / 2f;
        float torsoY = pBounds.y + pBounds.height / 2f;

        com.badlogic.ashley.utils.ImmutableArray<Entity> walls = getEngine().getEntitiesFor(
                Family.all(WallComponent.class, TransformComponent.class).get()
        );

        for (int j = 0; j < walls.size(); ++j) {
            Entity wall = walls.get(j);
            TransformComponent wTransform = transformMapper.get(wall);

            // Player must be behind the wall (higher base Y) AND the torso
            // center-point must be inside the wall's bounding box.
            if (pTransform.pos.y > wTransform.pos.y
                    && wTransform.bounds.contains(torsoX, torsoY)) {
                return true;
            }
        }

        return false;
    }

    private void renderSilhouettes() {
        com.badlogic.ashley.utils.ImmutableArray<Entity> players = getEngine().getEntitiesFor(
                Family.all(PlayerComponent.class, TransformComponent.class, SpriteComponent.class).get()
        );

        if (players.size() == 0) return;

        for (int i = 0; i < players.size(); ++i) {
            Entity player = players.get(i);
            
            // Render the silhouette only if the player is currently occluded
            if (isPlayerOccluded(player)) {
                TransformComponent pTransform = transformMapper.get(player);
                SpriteComponent pSprite = spriteMapper.get(player);
                TextureRegion frame = getFrame(pSprite);
                
                if (frame != null) {
                    float width = (pSprite.drawWidth > 0) ? pSprite.drawWidth : pTransform.width;
                    float height = (pSprite.drawHeight > 0) ? pSprite.drawHeight : pTransform.height;
                    float drawX = pTransform.pos.x - (width - pTransform.width) / 2;
                    float drawY = pTransform.pos.y - (height - pTransform.height) / 2;
                    
                    boolean flipX = !pSprite.facingRight;
                    if (frame.isFlipX()) {
                        flipX = !flipX;
                    }

                    // Render as a faint translucent silhouette (white with 40% opacity) over the wall
                    batch.setColor(1f, 1f, 1f, 0.4f);
                    batch.draw(frame, drawX, drawY, width / 2f, height / 2f, width, height, flipX ? -1f : 1f, 1f, 0f);
                    batch.setColor(Color.WHITE); // Reset immediately
                }
            }
        }
    }
}
