package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.sam.TERMINAL.components.*;

public class EnemySystem extends IteratingSystem {

    private ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private Entity player;

    public EnemySystem() { super(Family.all(EnemyComponent.class).get());}

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (player == null) {
            try { player = getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get()).first(); }
            catch (Exception e) { return; }
        }

        TransformComponent enemyT = tm.get(entity);
        TransformComponent playerT = tm.get(player);

        //Simple Chase Chase hehehe
        float dx = playerT.pos.x - enemyT.pos.x;
        float dy = playerT.pos.y - enemyT.pos.y;
        Vector2 dir = new Vector2(dx, dy).nor();

        //Spped of gshost
        enemyT.pos.mulAdd(dir, 80 * deltaTime);
        enemyT.updateBounds();
    }
}
