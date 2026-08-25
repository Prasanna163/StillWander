package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

public record CameraPose(Vec3d position, float yaw, float pitch, float fov, float focusDistance) {
    public CameraPose(Vec3d position, float yaw, float pitch) {
        this(position, yaw, pitch, Float.NaN, Float.NaN);
    }

    public CameraPose withFov(float value) {
        return new CameraPose(position, yaw, pitch, value, focusDistance);
    }

    public CameraPose withFocusDistance(float value) {
        return new CameraPose(position, yaw, pitch, fov, value);
    }
}
