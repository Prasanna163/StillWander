package com.cinecraft.director;

/** Scene conditions used to route authored compositions into appropriate environments. */
public enum ShotEnvironment {
    ANY,
    CAVE,
    FOREST,
    MOUNTAIN,
    OCEAN,
    DESERT,
    SNOW,
    SWAMP,
    PLAINS,
    NETHER,
    END,
    GOLDEN_HOUR,
    STORM,
    NIGHT;

    public boolean matches(EnvironmentProfile profile) {
        return switch (this) {
            case ANY -> true;
            case CAVE -> profile.biomeMood() == BiomeMood.CAVE;
            case FOREST -> profile.biomeMood() == BiomeMood.FOREST;
            case MOUNTAIN -> profile.biomeMood() == BiomeMood.MOUNTAIN;
            case OCEAN -> profile.biomeMood() == BiomeMood.OCEAN;
            case DESERT -> profile.biomeMood() == BiomeMood.DESERT;
            case SNOW -> profile.biomeMood() == BiomeMood.SNOW;
            case SWAMP -> profile.biomeMood() == BiomeMood.SWAMP;
            case PLAINS -> profile.biomeMood() == BiomeMood.PLAINS;
            case NETHER -> profile.dimensionMood() == DimensionMood.NETHER;
            case END -> profile.dimensionMood() == DimensionMood.END;
            case GOLDEN_HOUR -> profile.sceneTime() == SceneTime.SUNRISE
                    || profile.sceneTime() == SceneTime.SUNSET;
            case STORM -> profile.weather() == SceneWeather.RAIN
                    || profile.weather() == SceneWeather.SNOW
                    || profile.weather() == SceneWeather.THUNDER;
            case NIGHT -> profile.sceneTime() == SceneTime.NIGHT;
        };
    }
}
