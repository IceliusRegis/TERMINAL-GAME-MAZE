package com.sam.TERMINAL.persistence;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

import java.io.File;

/**
 * SaveManager - Handles the physical writing and reading of save files.
 *
 * Responsibilities:
 * - bridges the gap between Java objects (GameData) and the hard drive (save.json).
 * - Uses LibGDX's JSON serializer to create human-readable save files.
 * - Handles file existence checks to prevent crashes on "Load Game".
 *
 * Notes:
 * - Uses Gdx.files.local, so "save.json" will appear in your project's root folder.
 * - All methods are static for easy access from any System.
 */

public class SaveManager {

    private static final Json json = new Json();

    public static void save(GameData data, String fileName) {
        // 1. Ensure the directory exists
        FileHandle dir = Gdx.files.local("saves/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. Write to file
        // Gdx.files.local creates the file in your desktop project's root directory
        FileHandle file = Gdx.files.local("saves/" + fileName);

        // 3. Serialize data to pretty-printed JSON string
        // LibGDX's json.toJson() handles the conversion
        String text = json.prettyPrint(data);

        // 4. Write to disk (false = overwrite)
        file.writeString(text, false);
    }

    public static GameData load(String fileName) {
        // Look inside the saves folder
        FileHandle file = Gdx.files.local("saves/" + fileName);

        // LOGIC FIX: Check if file EXISTS, then read it.
        if (file.exists()) {
            try {
                return json.fromJson(GameData.class, file.readString());
            } catch (Exception e) {
                Gdx.app.error("SAVE_MANAGER", "Error parsing save file: " + fileName);
                return null;
            }
        }

        System.out.println("Save file not found: " + fileName);
        return null;
    }

    public static void delete(String fileName) {
        FileHandle file = Gdx.files.local("saves/" + fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}
