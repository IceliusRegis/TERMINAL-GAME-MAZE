package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.sam.TERMINAL.components.*;

public class EnemySystem extends EntitySystem {
    private ImmutableArray<Entity> monsters;
    private Entity player;

    private ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private ComponentMapper<EnemyComponent> em = ComponentMapper.getFor(EnemyComponent.class);

    // Temporary vector to avoid creating new objects every frame (GC friendly)
    private Vector2 tempVec = new Vector2();

    public EnemySystem(Entity player) {
        this.player = player;
    }

    @Override
    public void addedToEngine(Engine engine) {
        monsters = engine.getEntitiesFor(Family.all(EnemyComponent.class, TransformComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (player == null) return;

        Vector2 playerPos = tm.get(player).pos;

        // Use standard for loops to avoid Nested Iterator Exception
        for (int i = 0; i < monsters.size(); i++) {
            Entity monster = monsters.get(i);
            TransformComponent mt = tm.get(monster);
            EnemyComponent ai = em.get(monster);

            float dist = mt.pos.dst(playerPos);

            // 1. CHASE LOGIC
            if (dist < ai.detectionRange && dist > 5f) {
                tempVec.set(playerPos).sub(mt.pos).nor();
                mt.pos.add(tempVec.x * ai.speed * deltaTime, tempVec.y * ai.speed * deltaTime);
            }

            // 2. SEPARATION LOGIC (Nested Loop)
            for (int j = 0; j < monsters.size(); j++) {
                if (i == j) continue; // Skip comparing the monster to itself

                Entity other = monsters.get(j);
                TransformComponent otherTransform = tm.get(other);
                float distBetweenMonsters = mt.pos.dst(otherTransform.pos);

                float separationRadius = 30f;
                if (distBetweenMonsters < separationRadius) {
                    tempVec.set(mt.pos).sub(otherTransform.pos).nor();

                    float pushStrength = 40f;
                    mt.pos.add(tempVec.x * pushStrength * deltaTime, tempVec.y * pushStrength * deltaTime);
                }
            }

            mt.updateBounds();
        }
    }
}
