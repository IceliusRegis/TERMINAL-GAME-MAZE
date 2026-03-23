package com.sam.TERMINAL.components;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import com.badlogic.ashley.core.Component;

/**
 * LightComponent — Stores references to an entity's active Box2DLights.
 *
 * IMPORTANT: This component is NOT poolable. The lights are owned and
 * disposed by LightingSystem, not by Ashley's pool. Never call
 * engine.createComponent(LightComponent.class) for this class.
 */
public class LightComponent implements Component {

    public ConeLight cone;
    public PointLight pointLight;

    public LightComponent(ConeLight cone, PointLight pointLight) {
        this.cone = cone;
        this.pointLight = pointLight;
    }
}
