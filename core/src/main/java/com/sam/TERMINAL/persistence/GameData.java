package com.sam.TERMINAL.persistence;

import java.util.List;
import java.util.ArrayList;

/**
 * GameData - A raw data container for saving and loading.
 *
 * Responsibilities:
 * - Acts as a "suitcase" to transport data between the game (ECS) and the disk (JSON).
 * - Only stores simple data (floats, strings, ints), NEVER heavy objects like Textures.
 * - Decouples the save file format from your runtime Components.
 * * Note: Must have a zero-argument constructor for LibGDX's Json serializer to work.
 */

public class GameData {
    // 1. Data we want to save
    public float playerX, playerY;

    // We'll use a simple list of Strings for item IDs (e.g., "key_card_blue")
    public List<String> inventoryItems;

    // 2. Required for JSON serialization
    public GameData() {
        // Initialize lists here to avoid NullPointerExceptions later
        this.inventoryItems = new ArrayList<>();
    }
}
