package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.SpriteComponent;

/**
 * AnimationSystem - Handles visual state changes based on input.
 *
 * Responsibilities:
 * - Switches between Walk and Idle animations.
 * - Flips the sprite (facingRight) based on A/D keys.
 * - Decouples visuals from physics (SRP).
 */

public class AnimationSystem extends IteratingSystem{

    private ComponentMapper<SpriteComponent> spriteMapper;

    public AnimationSystem(){
        super(Family.all(SpriteComponent.class, PlayerComponent.class).get());
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SpriteComponent sprite = spriteMapper.get(entity);

        //Check if player is moving
        boolean isMoving = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.S) ||
            Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D);

        if (isMoving) {
            sprite.currentAnimation = sprite.walkAnimation;

            //Flips
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                sprite.facingRight = false;
            } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                sprite.facingRight = true;
            }
        } else {
            sprite.currentAnimation = sprite.idleAnimation;
        }
    }
}
