package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * Marker component to identify roof tiles (the dark cap tiles that sit on top
 * of wall face tiles in the "Wall Roofs" Tiled layer).
 *
 * The RenderSystem's YComparator checks for this component and shifts a roof
 * entity's effective sort-Y downward by one tile height, so roof tiles always
 * group with the wall face directly beneath them in draw order. This prevents
 * the player sprite from being sandwiched between a face tile and its roof.
 *
 * No data fields are needed — the presence of this component is the tag.
 */
public class RoofComponent implements Component {
    // Intentionally empty — marker component only.
}