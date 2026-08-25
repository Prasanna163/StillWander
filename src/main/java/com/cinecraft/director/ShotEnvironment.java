/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.director;

import com.cinecraft.director.BiomeMood;
import com.cinecraft.director.DimensionMood;
import com.cinecraft.director.EnvironmentProfile;
import com.cinecraft.director.SceneTime;
import com.cinecraft.director.SceneWeather;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
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
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> true;
            case 1 -> {
                if (profile.biomeMood() == BiomeMood.CAVE) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                if (profile.biomeMood() == BiomeMood.FOREST) {
                    yield true;
                }
                yield false;
            }
            case 3 -> {
                if (profile.biomeMood() == BiomeMood.MOUNTAIN) {
                    yield true;
                }
                yield false;
            }
            case 4 -> {
                if (profile.biomeMood() == BiomeMood.OCEAN) {
                    yield true;
                }
                yield false;
            }
            case 5 -> {
                if (profile.biomeMood() == BiomeMood.DESERT) {
                    yield true;
                }
                yield false;
            }
            case 6 -> {
                if (profile.biomeMood() == BiomeMood.SNOW) {
                    yield true;
                }
                yield false;
            }
            case 7 -> {
                if (profile.biomeMood() == BiomeMood.SWAMP) {
                    yield true;
                }
                yield false;
            }
            case 8 -> {
                if (profile.biomeMood() == BiomeMood.PLAINS) {
                    yield true;
                }
                yield false;
            }
            case 9 -> {
                if (profile.dimensionMood() == DimensionMood.NETHER) {
                    yield true;
                }
                yield false;
            }
            case 10 -> {
                if (profile.dimensionMood() == DimensionMood.END) {
                    yield true;
                }
                yield false;
            }
            case 11 -> {
                if (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET) {
                    yield true;
                }
                yield false;
            }
            case 12 -> {
                if (profile.weather() == SceneWeather.RAIN || profile.weather() == SceneWeather.SNOW || profile.weather() == SceneWeather.THUNDER) {
                    yield true;
                }
                yield false;
            }
            case 13 -> profile.sceneTime() == SceneTime.NIGHT;
        };
    }
}

