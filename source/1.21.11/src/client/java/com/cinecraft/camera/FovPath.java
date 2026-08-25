package com.cinecraft.camera;

/** A shot-local field-of-view track in degrees; it never mutates the player's FOV option. */
@FunctionalInterface
public interface FovPath {
    float sample(double progress);

    static FovPath none() {
        return progress -> Float.NaN;
    }

    static FovPath fixed(float fov) {
        return progress -> fov;
    }

    static FovPath linear(float start, float end) {
        return progress -> {
            double clamped = Math.max(0.0, Math.min(1.0, progress));
            return (float) (start + (end - start) * clamped);
        };
    }
}
