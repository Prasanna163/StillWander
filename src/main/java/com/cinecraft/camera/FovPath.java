/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface FovPath {
    public float sample(double var1);

    public static FovPath none() {
        return progress -> Float.NaN;
    }

    public static FovPath fixed(float fov) {
        return progress -> fov;
    }

    public static FovPath linear(float start, float end) {
        return progress -> {
            double clamped = Math.max(0.0, Math.min(1.0, progress));
            return (float)((double)start + (double)(end - start) * clamped);
        };
    }
}

