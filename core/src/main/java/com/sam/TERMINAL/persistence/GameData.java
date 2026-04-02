package com.sam.TERMINAL.persistence;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
    public String runId = "";
    public float playerX, playerY;
    public float playerBattery = 100f;
    public int totalBeepCardsSpawned = 0;

    // We'll use a simple list of Strings for item IDs (e.g., "key_card_blue")
    public List<String> inventoryItems;

    public Map<String, Boolean> interactableStates = new HashMap<>();

    public static class EnemySaveData {
        public float x;
        public float y;

        public EnemySaveData() {}
        
        public EnemySaveData(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class ItemSaveData {
        public float x;
        public float y;
        public String type;
        public String saveId;
        public boolean isActive;

        public ItemSaveData() {}
        
        public ItemSaveData(float x, float y, String type, String saveId, boolean isActive) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.saveId = saveId;
            this.isActive = isActive;
        }
    }

    public List<EnemySaveData> enemies;
    public List<ItemSaveData> items;

    // 2. Required for JSON serialization
    public GameData() {
        // Initialize lists here to avoid NullPointerExceptions later
        this.inventoryItems = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.items = new ArrayList<>();
    }
}
