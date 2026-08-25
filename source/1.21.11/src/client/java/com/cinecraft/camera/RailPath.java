package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

/** A straight, constant-speed camera rail used for corridors, paths, and gaps between scenery. */
public record RailPath(Vec3d start, Vec3d end) implements CameraPath {
    @Override
    public Vec3d sample(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return start.lerp(end, clamped);
    }
}
