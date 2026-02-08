package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * PersistenceComponent - Marks an entity to be saved/loaded.
 * * Any entity with this component will be processed by the SaveSystem.
 * The 'saveId' is used to match data in the save file to the correct entity.
 */

public class PersistenceComponent implements Component {
    public String saveId = "";
}
