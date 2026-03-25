package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.GridPoint2;
import java.util.LinkedList;
import java.util.Queue;

/**
 * EnemyComponent — Data for enemy AI entities.
 *
 * Stores BFS path cache and movement speed.
 * Safe to use with engine.createComponent() — no constructor arguments.
 */
public class EnemyComponent implements Component {

    /** Movement speed in pixels per second. */
    public float speed = 80f;

    /**
     * Cached BFS path as a queue of tile coordinates.
     * The enemy pops one tile at a time and moves toward it.
     * When the queue is empty, a new BFS is triggered.
     */
    public Queue<GridPoint2> path = new LinkedList<>();

    /**
     * How many seconds between full BFS recalculations.
     * Lower = more responsive, higher = better performance.
     */
    public float pathRecalcInterval = 0.4f;

    /** Accumulates delta time. When >= pathRecalcInterval, BFS is re-run. */
    public float pathTimer = 0f;

    /**
     * Called by Ashley when the component is returned to the pool.
     * Clears cached path data to prevent stale routes on reuse.
     */
    public void reset() {
        path.clear();
        pathTimer = 0f;
    }
}