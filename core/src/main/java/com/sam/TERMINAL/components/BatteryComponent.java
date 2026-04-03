package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;

/**
 * BatteryComponent - Stores the flashlight battery level for the player.
 */
public class BatteryComponent implements Component {

    public float battery = 100f;
    public float maxBattery = 100f;
    public boolean flashlightOn = false;

    public BatteryComponent(float maxBattery) {
        this.maxBattery = maxBattery;
        this.battery = maxBattery;
        this.flashlightOn = false;
    }

    public BatteryComponent() {
        // default constructor
    }
}
