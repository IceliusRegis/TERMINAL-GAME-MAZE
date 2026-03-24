package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.sam.TERMINAL.components.EnemyComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.TransformComponent;

public class EnemySystem extends IteratingSystem {
    private ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private Entity player;
    private Runnable onCatch; // ← callback
    private boolean triggered = false; // ← prevent firing every frame

    public EnemySystem(Runnable onCatch) {
        super(Family.all(EnemyComponent.class).get());
        this.onCatch = onCatch;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (player == null) {
            try { player = getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get()).first(); }
            catch (Exception e) { return; }
        }

        TransformComponent enemyT = tm.get(entity);
        TransformComponent playerT = tm.get(player);

        float dx = playerT.pos.x - enemyT.pos.x;
        float dy = playerT.pos.y - enemyT.pos.y;
        Vector2 dir = new Vector2(dx, dy).nor();

        enemyT.pos.mulAdd(dir, 80 * deltaTime);
        enemyT.updateBounds();

        // ← Check catch distance
        float dist = new Vector2(dx, dy).len();
        if (dist < 20f && !triggered) {
            triggered = true;
            onCatch.run();
        }
    }

    public void reset() { triggered = false; } // call this on game reset
}
