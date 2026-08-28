package com.cinecraft.director;

import com.google.gson.Gson;
import net.minecraft.util.math.Vec3d;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

record SceneFixtureCatalog(List<SceneFixture> scenes) {
    static SceneFixtureCatalog load() {
        InputStream stream = SceneFixtureCatalog.class.getResourceAsStream("/scene-fixtures.json");
        if (stream == null) throw new IllegalStateException("Missing scene-fixtures.json");
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            SceneFixture[] fixtures = new Gson().fromJson(reader, SceneFixture[].class);
            return new SceneFixtureCatalog(List.copyOf(Arrays.asList(fixtures)));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to read scene fixtures", exception);
        }
    }

    record SceneFixture(
            String name,
            SpaceType spaceType,
            boolean underground,
            double surfaceDepth,
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
            DimensionMood dimensionMood,
            boolean expectedWide,
            List<List<Double>> anchors
    ) {
        EnvironmentProfile profile() {
            List<Vec3d> points = anchors.stream()
                    .map(point -> new Vec3d(point.get(0), point.get(1), point.get(2)))
                    .toList();
            return new EnvironmentProfile(
                    spaceType,
                    underground,
                    surfaceDepth,
                    new Vec3d(0.0, 64.0, 0.0),
                    new Vec3d(0.0, 64.0, 0.0),
                    points,
                    averageClearance,
                    skyVisibility,
                    terrainRelief,
                    waterCoverage,
                    sceneRadius,
                    maxCameraDistance,
                    maxVerticalRise,
                    biomeMood,
                    weather,
                    sceneTime,
                    dimensionMood
            );
        }
    }
}
