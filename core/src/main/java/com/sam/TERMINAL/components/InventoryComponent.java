package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * InventoryComponent - acts as the "pockets" or backpack for an entity.
 *
 * Responsibilities:
 * - Stores a list of item IDs (Strings) that the entity is holding.
 * - Provides helper methods to add items or check if an item exists.
 * - This component is usually only added to the Player entity.
 */

public class InventoryComponent implements Component {

    public List<String> items = new ArrayList<>();

    public void addItem(String item) {
        items.add(item);
    }

    public boolean hasItem(String item){
        return items.contains(item);
    }



}
