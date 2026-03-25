package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IntervalSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.ashley.core.EntitySystem;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.Main;

/**
 * WinLossSystem — Checks win and lose conditions each frame.
 *
 * IMPORTANT: Although this previously extended EnemySystem, it now extends
 * EntitySystem directly. WinLossSystem does not process enemy entities —
 * it queries the engine directly for players and enemies.
 */
public class WinLossSystem extends EntitySystem {

    private Main mainGame;
    public boolean gameOver = false;
    public boolean win = false;

    public WinLossSystem(Main main) {
        // Priority 0 — runs in default order
        this.mainGame = main;
    }

    @Override
    public void update(float deltaTime) {
        if (gameOver || win)
            return;

        // --- LOSE: Enemy touches player ---
        ImmutableArray<Entity> players = getEngine()
                .getEntitiesFor(Family.all(PlayerComponent.class).get());
        ImmutableArray<Entity> enemies = getEngine()
                .getEntitiesFor(Family.all(EnemyComponent.class).get());

        if (players.size() > 0 && enemies.size() > 0) {
            TransformComponent pT = players.first().getComponent(TransformComponent.class);
            for (Entity e : enemies) {
                TransformComponent eT = e.getComponent(TransformComponent.class);
                if (eT != null && eT.bounds.overlaps(pT.bounds)) {
                    Gdx.app.log("TERMINAL", "YOU DIED - GAME OVER");
                    gameOver = true;
                    return;
                }
            }
        }

        // --- WIN: Door is inactive (opened) ---
        ImmutableArray<Entity> interactables = getEngine()
                .getEntitiesFor(Family.all(InteractableComponent.class).get());
        for (Entity e : interactables) {
            InteractableComponent ic = e.getComponent(InteractableComponent.class);
            if (ic.type.equals("door") && !ic.isActive) {
                Gdx.app.log("TERMINAL", "YOU WIN - ESCAPED!");
                win = true;
                return;
            }
        }
    }

    public void reset() {
        gameOver = false;
        win = false;
    }
}
