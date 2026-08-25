/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 */
package com.cinecraft.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public record CameraPose(class_243 position, float yaw, float pitch, float fov, float focusDistance) {
    public CameraPose(class_243 position, float yaw, float pitch) {
        this(position, yaw, pitch, Float.NaN, Float.NaN);
    }

    public CameraPose withFov(float value) {
        return new CameraPose(this.position, this.yaw, this.pitch, value, this.focusDistance);
    }

    public CameraPose withFocusDistance(float value) {
        return new CameraPose(this.position, this.yaw, this.pitch, this.fov, value);
    }
}

