package com.sam.TERMINAL.components;

import com.badlogic.ashley.core.Component;
import box2dLight.PointLight;

/**
 * Marks an entity as a static environmental light source.
 */
public class StaticLightComponent implements Component {
    public PointLight pointLight = null;
}
