package com.sam.TERMINAL.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * DebugManager - Handles developer shortcuts.
 * 
 * Separated from gameplay systems so debug polling never interferes
 * with or consumes regular gameplay input.
 */
public class DebugManager {
    
    private long lastLTapTime = 0;
    private static final long DOUBLE_TAP_MAX_DELAY = 400; // milliseconds

    /**
     * Polls debug inputs. Call this from Main.render() outside the ECS update loop.
     */
    public void update(LightingSystem lightingSystem) {
        if (lightingSystem == null) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            long currentTime = TimeUtils.millis();
            
            if (currentTime - lastLTapTime < DOUBLE_TAP_MAX_DELAY) {
                // Double tap detected! Toggle lighting.
                lightingSystem.lightingEnabled = !lightingSystem.lightingEnabled;
                Gdx.app.log("TERMINAL_DEBUG", "Lighting Enabled: " + lightingSystem.lightingEnabled);
                
                // Reset tap time so a third tap doesn't trigger it again instantly
                lastLTapTime = 0; 
            } else {
                lastLTapTime = currentTime;
            }
        }
    }
}
