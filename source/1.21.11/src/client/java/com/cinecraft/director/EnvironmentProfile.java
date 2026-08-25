package com.cinecraft.director;

import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Cached measurements of the space around the idle player. */
public record EnvironmentProfile(
        SpaceType spaceType,
        boolean underground,
        double surfaceDepth,
        Vec3d playerFocus,
        Vec3d landscapeCenter,
        List<Vec3d> landscapeAnchors,
        double averageClearance,
        double skyVisibility,
        double terrainRelief,
        double waterCoverage,
        double sceneRadius,
        double maxCameraDistance,
        double maxVerticalRise,
        BiomeMood biomeMood,
        SceneWeather weather,
        SceneTime sceneTime,
        DimensionMood dimensionMood
) {
    public EnvironmentProfile {
        landscapeAnchors = List.copyOf(landscapeAnchors);
    }

    public boolean supportsWideShots() {
        return !underground && maxCameraDistance >= 18.0;
    }
}
