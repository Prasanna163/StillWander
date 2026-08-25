package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

/** A generated, level cinematic pan with optional slow push-in or pull-back. */
public record PanPath(
        Vec3d center,
        double startingAngle,
        double sweepRadians,
        double startingRadius,
        double endingRadius,
        double startingY,
        double endingY
) implements CameraPath {
    @Override
    public Vec3d sample(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        double angle = startingAngle + sweepRadians * clamped;
        double radius = lerp(startingRadius, endingRadius, clamped);
        return new Vec3d(
                center.x + Math.cos(angle) * radius,
                lerp(startingY, endingY, clamped),
                center.z + Math.sin(angle) * radius
        );
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}
