package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * PlayerComponent - Marker component to identify the player entity.
 *
 * This is a "tag" component with no data.
 * Systems use this to filter and find the player entity specifically.
 */
public class PlayerComponent implements Component {
    public float speed = 170f;     // The current speed (changes)
    public float baseSpeed = 170f;
}
