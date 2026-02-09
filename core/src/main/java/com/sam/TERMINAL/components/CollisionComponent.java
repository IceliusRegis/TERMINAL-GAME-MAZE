package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * CollisionComponent - Marker component for solid/collidable entities.
 *
 * Entities with this component block movement (walls, obstacles).
 * MovementSystem checks for this to prevent player from passing through.
 */
public class CollisionComponent implements Component {
    // Empty marker component - identifies solid objects
}
