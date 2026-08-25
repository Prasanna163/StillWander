/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 */
package com.cinecraft.director;

import com.cinecraft.director.BiomeMood;
import com.cinecraft.director.DimensionMood;
import com.cinecraft.director.SceneTime;
import com.cinecraft.director.SceneWeather;
import com.cinecraft.director.SpaceType;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public record EnvironmentProfile(SpaceType spaceType, boolean underground, double surfaceDepth, class_243 playerFocus, class_243 landscapeCenter, List<class_243> landscapeAnchors, double averageClearance, double skyVisibility, double terrainRelief, double waterCoverage, double sceneRadius, double maxCameraDistance, double maxVerticalRise, BiomeMood biomeMood, SceneWeather weather, SceneTime sceneTime, DimensionMood dimensionMood) {
    public EnvironmentProfile {
        landscapeAnchors = List.copyOf(landscapeAnchors);
    }

    public boolean supportsWideShots() {
        return !this.underground && this.maxCameraDistance >= 18.0;
    }
}

