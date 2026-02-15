package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * InteractableComponent - Marks an entity as something the player can use.
 *
 * Responsibilities:
 * - Stores data about the interaction (e.g., "This is a door", "This is a key").
 * - Defines the activation radius (how close the player needs to be).
 * - Tracks state (isActive) so you can't pick up the same key twice.
 *
 */

public class InteractableComponent implements Component {
    public float radius = 50f; // How close you need to be in pixels to the certain items
    public String type = "unknown";
    public boolean isActive = true; //This what determines if you can interact with it

    public InteractableComponent() {}

    public InteractableComponent(String type, float radius) {
        this.type = type;
        this.radius = radius;
    }
}
