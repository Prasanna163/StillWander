/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.director;

import com.cinecraft.director.MotionStyle;
import com.cinecraft.director.ShotEnvironment;
import com.cinecraft.director.SubjectType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record ShotPreset(String name, SubjectType subjectType, boolean wide, boolean aerial, MotionStyle motionStyle, double radius, double height, double sweepDegrees, double radialChange, long durationMillis, float startingFov, float endingFov, ShotEnvironment environment) {
    public ShotPreset(String name, SubjectType subjectType, boolean wide, boolean aerial, MotionStyle motionStyle, double radius, double height, double sweepDegrees, double radialChange, long durationMillis, float startingFov, float endingFov) {
        this(name, subjectType, wide, aerial, motionStyle, radius, height, sweepDegrees, radialChange, durationMillis, startingFov, endingFov, ShotEnvironment.ANY);
    }

    public ShotPreset(String name, SubjectType subjectType, boolean wide, boolean aerial, MotionStyle motionStyle, double radius, double height, double sweepDegrees, double radialChange, long durationMillis) {
        this(name, subjectType, wide, aerial, motionStyle, radius, height, sweepDegrees, radialChange, durationMillis, ShotPreset.defaultFov(subjectType, wide, aerial), ShotPreset.defaultFov(subjectType, wide, aerial), ShotEnvironment.ANY);
    }

    public ShotPreset(String name, SubjectType subjectType, boolean wide, boolean aerial, MotionStyle motionStyle, double radius, double height, double sweepDegrees, double radialChange, long durationMillis, ShotEnvironment environment) {
        this(name, subjectType, wide, aerial, motionStyle, radius, height, sweepDegrees, radialChange, durationMillis, ShotPreset.defaultFov(subjectType, wide, aerial), ShotPreset.defaultFov(subjectType, wide, aerial), environment);
    }

    private static float defaultFov(SubjectType type, boolean wide, boolean aerial) {
        if (wide) {
            return aerial ? 66.0f : 74.0f;
        }
        return switch (type) {
            default -> throw new MatchException(null, null);
            case SubjectType.PLAYER -> 54.0f;
            case SubjectType.PLAYER_DETAIL -> 34.0f;
            case SubjectType.ENTITY -> 42.0f;
            case SubjectType.GROUP -> 52.0f;
            case SubjectType.FEATURE -> 46.0f;
            case SubjectType.LANDSCAPE -> 60.0f;
        };
    }
}

