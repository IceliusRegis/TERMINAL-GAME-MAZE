package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.sam.TERMINAL.components.*;
import com.sam.TERMINAL.Main;

public class WinLossSystem extends EnemySystem {

    private Main mainGame;
    public boolean gameOver = false;
    public boolean win = false;

    public WinLossSystem(Main main) {this.mainGame = main;}

    @Override
    public void update(float deltaTime) {
        if (gameOver || win) return;

        //Lose Condition: If ghost catch you
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get());
        ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(Family.all(EnemyComponent.class).get());

        boolean hit = false;

        if (players.size() > 0 && enemies.size() > 0) {
            TransformComponent pT = players.first().getComponent(TransformComponent.class);
            for (Entity e : enemies) {
                TransformComponent eT = e.getComponent(TransformComponent.class);
                if (eT != null && eT.bounds.overlaps(pT.bounds)) {
                    hit = true;
                    break;
                }
            }
        }

        if (hit) {
            System.out.println("YOU DIED - GAME OVER");
            gameOver = true;
            //mainGame.resetGame();
        }

        //WIN CONDITION
        ImmutableArray<Entity> interactables = getEngine().getEntitiesFor(Family.all(InteractableComponent.class).get());
        for (Entity e : interactables) {
            InteractableComponent ic = e.getComponent(InteractableComponent.class);
            if (ic.type.equals("door") && !ic.isActive) {
                System.out.println("YOU WIN - ESCAPED!");
                win = true;
            }
        }
    }
}
