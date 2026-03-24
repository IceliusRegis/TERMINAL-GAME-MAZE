package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * WallComponent - Marker component for static Tiled Map wall entities.
 * Used primarily for Y-sorting occlusion checks.
 */
public class WallComponent implements Component {
    public float sortYShift = 0f;
}
