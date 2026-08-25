package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

/** Samples at constant temporal speed so a pan never visibly starts or stops inside a shot. */
public final class MovingShot implements CinematicShot {
    private final CameraPath path;
    private final CameraPath focusPath;
    private final FovPath fovPath;
    private final long startedNanos = System.nanoTime();
    private final long durationNanos;

    public MovingShot(CameraPath path, Vec3d target, long durationMillis) {
        this(path, progress -> target, FovPath.none(), durationMillis);
    }

    public MovingShot(CameraPath path, CameraPath focusPath, long durationMillis) {
        this(path, focusPath, FovPath.none(), durationMillis);
    }

    public MovingShot(CameraPath path, CameraPath focusPath, FovPath fovPath, long durationMillis) {
        this.path = path;
        this.focusPath = focusPath;
        this.fovPath = fovPath;
        this.durationNanos = durationMillis * 1_000_000L;
    }

    @Override
    public CameraPose sample(float tickDelta) {
        double progress = Math.min(1.0, (System.nanoTime() - startedNanos) / (double) durationNanos);
        return LookAt.pose(path.sample(progress), focusPath.sample(progress))
                .withFov(fovPath.sample(progress));
    }

    @Override
    public boolean finished() {
        return System.nanoTime() - startedNanos >= durationNanos;
    }
}
