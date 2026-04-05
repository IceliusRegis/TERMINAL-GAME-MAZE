# TERMINAL

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

## 🔦 Overview
**TERMINAL** is a 2D top-down horror maze game built using LibGDX and the Ashley Entity Component System (ECS). The player must navigate a dark, tile-based labyrinth, manage limited resources like flashlight battery, avoid a pursuing entity, and collect key items to secure an escape. The game relies heavily on dynamic lighting (**Box2DLights**) and line-of-sight mechanics to build tension.

---

## 🎮 Controls
| Key | Action |
| :--- | :--- |
| **W, A, S, D** | Move the character (Up, Left, Down, Right) |
| **E** | Interact with nearby objects (pick up items, escape) |
| **F** | Toggle Flashlight on/off |
| **TAB** | Open/Close Inventory |
| **U** | Use a **Battery** from the inventory to recharge the flashlight |
| **P** | Use **Potion (Sting)** from the inventory for a speed boost |
| **F5** | **Quick Save** (stores current state, position, and inventory) |
| **F9** | **Quick Load** (restores the last save) |
| **ESC** | Open/Close in-game Settings |
| **X / SPACE** | Continue/Skip dialogues and UI screens |

---

## 🏆 Lose/Win Conditions
### **Win Condition**
* The player must explore the maze and collect all spawned **Beep Cards** (randomly 3 to 5 per level).
* The player must locate and stand adjacent to a designated **"Escape/Winning"** tile on the map.
* Pressing **E** with all Beep Cards in the inventory triggers the win sequence and loads the next level.

### **Lose Condition**
* The **Enemy's** collision bounding box overlaps the Player's bounding box.
* Results in an immediate jumpscare, audio sting, and a **"Game Over"** screen.

---

## 📦 Items
* **Beep Card**: The primary objective item. Collecting them progresses the win condition but simultaneously increases the Enemy's movement speed (**dynamic difficulty**).
* **Flashlight**: Enables the directional cone of light. Requires battery power to operate.
* **Battery**: Recharges the flashlight to 100%.
    * **Dynamic Spawning**: If the player has no battery and there are none left on the map, a new battery will automatically spawn after a **15-second delay** to prevent soft-locking.
* **Potion (Sting)**: A consumable item that increases the player's base speed by **+70** for exactly **10 seconds**.
* **Lily**: Starts the main loop of the game.

---

## 💀 Enemy
* **Spawn Delay**: The enemy does not spawn immediately. A **45-second timer** counts down at the start of the level, giving the player time to prepare.
* **Pathfinding**: The enemy tracks the player using a tile-based **BFS (Breadth-First Search)** pathfinding algorithm.
* **Dynamic Speed**: The enemy's base speed increases by **+10** for every Beep Card the player holds.
* **Proximity Audio**: Emits a heartbeat sound effect that scales in volume based on distance to the player.

---

## 🗺️ Map
* **Tile-Based Grid**: Built using Tiled (** .tmx **). Relies on specific layers: *Collision, Walls, Ground, Winning, No Spawn*.
* **Dynamic Rendering**: Entities and walls are **Y-sorted**. The player can walk "behind" walls; when occluded, the player is rendered as a dark silhouette.
* **Line of Sight**: Items cannot be interacted with through walls. The game checks if the visual line between the player and the item is obstructed.

---

## 💾 Persistence (Saving & Loading)
* **Save/Load Triggers**: Players can manually Quick Save (**F5**) or Quick Load (**F9**).
* **Data Serialization**: Game state is serialized into a JSON format (`saveFile.json`). This separates raw data (`GameData.java`) from active runtime components.
* **Tracked State**: Captures a snapshot of player coordinates, battery level, inventory, enemy position, and active interactables.
* **Run Validation**: Uses a unique `runId` to ensure the loaded snapshot matches the current session.
* **Safety Snapshot**: Upon starting a level, a hidden `temp_initial_state.json` is created. Choosing **"Reset"** restores this exact state instead of re-rolling RNG generation.

---

## 🛠️ Development (Gradle)
This project uses **Gradle** to manage dependencies.
* `lwjgl3:run`: Starts the application.
* `lwjgl3:jar`: Builds the runnable jar.
* `clean`: Removes build folders.
```bash
./gradlew lwjgl3:run
