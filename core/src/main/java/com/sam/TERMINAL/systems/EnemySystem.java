package com.sam.TERMINAL.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.sam.TERMINAL.components.EnemyComponent;
import com.sam.TERMINAL.components.PlayerComponent;
import com.sam.TERMINAL.components.TileWorldComponent;
import com.sam.TERMINAL.components.TransformComponent;

import java.util.*;

/**
 * EnemySystem — BFS-based pathfinding for enemy entities.
 *
 * Each frame:
 * 1. Accumulates delta time against pathRecalcInterval.
 * 2. When the interval fires (or path is empty), runs BFS to find
 * the shortest tile-path to the player.
 * 3. Moves the enemy pixel-by-pixel toward the next tile waypoint
 * in the cached path queue.
 *
 * Architecture Notes:
 * - Reads TileWorldComponent.isSolid() for wall data. No direct tile array
 * access.
 * - Stores path in EnemyComponent.path — component isolation is maintained.
 * - Does NOT extend WinLossSystem or any other system.
 */
public class EnemySystem extends IteratingSystem {

    /** Pixels — "close enough" to a waypoint to snap and advance. */
    private static final float ARRIVAL_THRESHOLD = 4f;

    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper
            .getFor(TransformComponent.class);
    private final ComponentMapper<EnemyComponent> enemyMapper = ComponentMapper.getFor(EnemyComponent.class);

    /** Cached player entity — looked up once and reused until engine reset. */
    private Entity cachedPlayer;

    /** ShapeRenderer for drawing debug BFS path lines. */
    private ShapeRenderer debugRenderer;

    public EnemySystem() {
        super(Family.all(EnemyComponent.class, TransformComponent.class).get());
    }

    // -----------------------------------------------------------------------
    // PER-ENTITY UPDATE
    // -----------------------------------------------------------------------

    @Override
    protected void processEntity(Entity entity, float deltaTime) {

        // 1. Find player entity (cached after first frame)
        if (cachedPlayer == null) {
            ImmutableArray<Entity> players = getEngine()
                    .getEntitiesFor(Family.all(PlayerComponent.class, TransformComponent.class).get());
            if (players.size() == 0)
                return;
            cachedPlayer = players.first();
        }

        // 2. Get components
        EnemyComponent enemy = enemyMapper.get(entity);
        TransformComponent enemyT = transformMapper.get(entity);
        TransformComponent playerT = transformMapper.get(cachedPlayer);

        // 3. Get world component (needed for BFS)
        TileWorldComponent world = getWorldComponent();
        if (world == null)
            return; // Map not loaded yet — skip silently

        // 4. Accumulate timer; recalculate path when interval fires or path exhausted
        enemy.pathTimer += deltaTime;
        boolean shouldRepath = enemy.pathTimer >= enemy.pathRecalcInterval
                || enemy.path.isEmpty();

        if (shouldRepath) {
            enemy.pathTimer = 0f;

            // Convert pixel positions to tile coordinates
            int startTileX = (int) (enemyT.pos.x / world.tileWidth);
            int startTileY = (int) (enemyT.pos.y / world.tileHeight);
            int goalTileX = (int) (playerT.pos.x / world.tileWidth);
            int goalTileY = (int) (playerT.pos.y / world.tileHeight);

            // Run BFS and cache the result
            Queue<GridPoint2> newPath = findPath(
                    world, startTileX, startTileY, goalTileX, goalTileY);
            enemy.path.clear();
            if (newPath != null) {
                enemy.path.addAll(newPath);
            }
        }

        // 5. Move toward the next waypoint in the path
        if (!enemy.path.isEmpty()) {
            GridPoint2 nextTile = enemy.path.peek();

            // Target pixel: center of the next tile, offset by half entity size
            float targetX = nextTile.x * world.tileWidth
                    + world.tileWidth / 2f
                    - enemyT.width / 2f;
            float targetY = nextTile.y * world.tileHeight
                    + world.tileHeight / 2f
                    - enemyT.height / 2f;

            float dx = targetX - enemyT.pos.x;
            float dy = targetY - enemyT.pos.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= ARRIVAL_THRESHOLD) {
                // Snap to waypoint and pop it — move to the next one next frame
                enemyT.pos.set(targetX, targetY);
                enemy.path.poll();
            } else {
                // Move toward waypoint at configured speed
                float step = enemy.speed * deltaTime;
                enemyT.pos.x += (dx / dist) * step;
                enemyT.pos.y += (dy / dist) * step;
            }

            // CRITICAL: Always update bounds after moving
            enemyT.updateBounds();
        }
    }

    // -----------------------------------------------------------------------
    // BFS HELPER
    // -----------------------------------------------------------------------

    /**
     * Runs a Breadth-First Search on the tile collision grid to find the
     * shortest path from (startX, startY) to (goalX, goalY).
     *
     * @param world  The TileWorldComponent containing the collision layer.
     * @param startX Start tile X (column)
     * @param startY Start tile Y (row)
     * @param goalX  Goal tile X
     * @param goalY  Goal tile Y
     * @return A Queue of GridPoint2 tile coordinates from start (exclusive) to
     *         goal (inclusive), or null if no path exists.
     */
    private Queue<GridPoint2> findPath(TileWorldComponent world,
            int startX, int startY,
            int goalX, int goalY) {

        // Edge case: already at goal
        if (startX == goalX && startY == goalY) {
            return new LinkedList<>();
        }

        // Edge case: goal is a solid tile — guard against infinite BFS
        if (world.isSolid(goalX, goalY)) {
            return null;
        }

        // BFS data structures
        Queue<GridPoint2> frontier = new LinkedList<>();
        Map<GridPoint2, GridPoint2> cameFrom = new HashMap<>(); // child -> parent

        GridPoint2 start = new GridPoint2(startX, startY);
        GridPoint2 goal = new GridPoint2(goalX, goalY);

        frontier.add(start);
        cameFrom.put(start, null); // start has no parent

        // 4-directional BFS (no diagonal movement)
        int[] dX = { 0, 0, -1, 1 };
        int[] dY = { 1, -1, 0, 0 };

        boolean found = false;

        while (!frontier.isEmpty()) {
            GridPoint2 current = frontier.poll();

            if (current.equals(goal)) {
                found = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dX[i];
                int ny = current.y + dY[i];
                GridPoint2 neighbor = new GridPoint2(nx, ny);

                // Skip if solid or already visited
                if (world.isSolid(nx, ny))
                    continue;
                if (cameFrom.containsKey(neighbor))
                    continue;

                cameFrom.put(neighbor, current);
                frontier.add(neighbor);
            }
        }

        if (!found)
            return null;

        // Reconstruct path by walking from goal back to start via cameFrom
        LinkedList<GridPoint2> path = new LinkedList<>();
        GridPoint2 step = goal;
        while (step != null && !step.equals(start)) {
            path.addFirst(step); // prepend so path runs start -> goal
            step = cameFrom.get(step);
        }
        // 'start' is intentionally excluded — the enemy is already there

        return path;
    }

    // -----------------------------------------------------------------------
    // DEBUG RENDERER
    // -----------------------------------------------------------------------

    /**
     * Draws yellow lines connecting tile centers in each enemy's BFS path.
     * Call this from Main.render() AFTER batch.end() and BEFORE lighting.
     *
     * @param camera The game camera for projection matrix alignment.
     */
    public void renderDebug(Camera camera) {
        if (debugRenderer == null) {
            debugRenderer = new ShapeRenderer();
        }

        TileWorldComponent world = getWorldComponent();
        if (world == null)
            return;

        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.YELLOW);

        ImmutableArray<Entity> enemies = getEngine()
                .getEntitiesFor(Family.all(EnemyComponent.class, TransformComponent.class).get());

        for (Entity entity : enemies) {
            EnemyComponent enemy = enemyMapper.get(entity);
            if (enemy.path.isEmpty())
                continue;

            TransformComponent enemyT = transformMapper.get(entity);

            // Start line from the enemy's current pixel position (center)
            float prevX = enemyT.pos.x + enemyT.width / 2f;
            float prevY = enemyT.pos.y + enemyT.height / 2f;

            for (GridPoint2 tile : enemy.path) {
                float tilePixelX = tile.x * world.tileWidth + world.tileWidth / 2f;
                float tilePixelY = tile.y * world.tileHeight + world.tileHeight / 2f;

                debugRenderer.line(prevX, prevY, tilePixelX, tilePixelY);

                prevX = tilePixelX;
                prevY = tilePixelY;
            }
        }

        debugRenderer.end();
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    /**
     * Retrieves the TileWorldComponent from the engine.
     * Returns null if the map hasn't loaded yet.
     */
    private TileWorldComponent getWorldComponent() {
        ImmutableArray<Entity> worldEntities = getEngine()
                .getEntitiesFor(Family.all(TileWorldComponent.class).get());
        if (worldEntities.size() == 0)
            return null;
        return worldEntities.first().getComponent(TileWorldComponent.class);
    }

    /**
     * Clear cached player reference when the system is removed from the engine
     * (e.g., on game reset) to avoid stale entity references.
     */
    @Override
    public void removedFromEngine(Engine engine) {
        cachedPlayer = null;
        if (debugRenderer != null) {
            debugRenderer.dispose();
            debugRenderer = null;
        }
    }
}
