package persistence;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import persistence.GameData;

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

    public static void save(GameData data) {
        // 1. Convert the Java object to "pretty" JSON text (easier to read/debug)
        String text = json.prettyPrint(data);

        // 2. Write to file
        // Gdx.files.local creates the file in your desktop project's root directory
        FileHandle file = Gdx.files.local("saveFile.json");

        // false = overwrite the file (don't append to the end of the old save)
        file.writeString(text, false);
    }

    public static GameData load() {
        FileHandle file = Gdx.files.local("saveFile.json");

        if (!file.exists()) {
            return null;
        }

        return json.fromJson(GameData.class, file.readString());
    }
}
