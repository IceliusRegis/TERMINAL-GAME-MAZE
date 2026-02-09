package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * PersistenceComponent - Marks an entity to be saved/loaded.
 * * Any entity with this component will be processed by the SaveSystem.
 * The 'saveId' is used to match data in the save file to the correct entity.
 */

public class PersistenceComponent implements Component {
    public String saveId;
    public String type;

    // Default Constructor (Required by Ashley)
    public PersistenceComponent() {
        this.saveId = "unknown";
        this.type = "unknown";
    }

    // Dynamic Constructor (Fixes your Main.java error!)
    public PersistenceComponent(String type, String saveId) {
        this.type = type;
        this.saveId = saveId;
    }

}
