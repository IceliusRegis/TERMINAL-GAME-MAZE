package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IntervalSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.ashley.core.EntitySystem;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.Main;

public class WinLossSystem extends EntitySystem {

    private Main mainGame;
    public boolean gameOver = false;
    public boolean win = false;

    public WinLossSystem(Main main) { this.mainGame = main; }

    @Override
    public void update(float deltaTime) {
        if (gameOver || win) return;

        // Lose Condition: ghost catches player
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get());
        ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(Family.all(EnemyComponent.class).get());

        if (players.size() > 0 && enemies.size() > 0) {
            TransformComponent pT = players.first().getComponent(TransformComponent.class);
            for (Entity e : enemies) {
                TransformComponent eT = e.getComponent(TransformComponent.class);
                if (eT != null && eT.bounds.overlaps(pT.bounds)) {
                    gameOver = true;
                    System.out.println("YOU DIED - GAME OVER");
                    return; // no need to check win if already dead
                }
            }
        }

        // Win Condition: door is used
        ImmutableArray<Entity> interactables = getEngine().getEntitiesFor(Family.all(InteractableComponent.class).get());
        for (Entity e : interactables) {
            InteractableComponent ic = e.getComponent(InteractableComponent.class);
            if (ic.type.equals("door") && !ic.isActive) {
                win = true;
                System.out.println("YOU WIN - ESCAPED!");
                return;
            }
        }
    }

    public void reset() {
        gameOver = false;
        win = false;
    }
}
