package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A damped entity-relative rig that removes tick-step jitter from live tracking. */
public final class TrackedMovingShot implements CinematicShot {
    private static final double TRACKING_TIME_CONSTANT_SECONDS = 0.70;
    private static final long SUBJECT_LOSS_GRACE_NANOS = 1_200_000_000L;
    private static final long UNSAFE_VIEW_GRACE_NANOS = 1_500_000_000L;

    private final CameraPath basePath;
    private final CameraPath liveFocusPath;
    private final Supplier<Vec3d> trackingCenter;
    private final BooleanSupplier targetAvailable;
    private final Vec3d originalTarget;
    private final FovPath fovPath;
    private final BiFunction<Vec3d, Vec3d, Vec3d> cameraResolver;
    private final long startedNanos = System.nanoTime();
    private final long durationNanos;
    private long previousNanos = startedNanos;
    private Vec3d smoothedTarget;
    private Vec3d smoothedFocus;
    private Vec3d lastSafeCamera;
    private Vec3d lastSafeFocus;
    private long unavailableSince;
    private long unsafeSince;
    private boolean gracefulExit;

    public TrackedMovingShot(
            CameraPath basePath,
            CameraPath liveFocusPath,
            Supplier<Vec3d> trackingCenter,
            BooleanSupplier targetAvailable,
            Vec3d originalTarget,
            FovPath fovPath,
            long durationMillis,
            BiFunction<Vec3d, Vec3d, Vec3d> cameraResolver
    ) {
        this.basePath = basePath;
        this.liveFocusPath = liveFocusPath;
        this.trackingCenter = trackingCenter;
        this.targetAvailable = targetAvailable;
        this.originalTarget = originalTarget;
        this.fovPath = fovPath;
        this.smoothedTarget = originalTarget;
        this.smoothedFocus = liveFocusPath.sample(0.0);
        this.durationNanos = durationMillis * 1_000_000L;
        this.cameraResolver = cameraResolver;
        this.lastSafeCamera = basePath.sample(0.0);
        this.lastSafeFocus = originalTarget;
    }

    @Override
    public CameraPose sample(float tickDelta) {
        long now = System.nanoTime();
        double deltaSeconds = Math.min(0.10, Math.max(0.0, (now - previousNanos) / 1_000_000_000.0));
        previousNanos = now;
        double alpha = 1.0 - Math.exp(-deltaSeconds / TRACKING_TIME_CONSTANT_SECONDS);

        if (!targetAvailable.getAsBoolean()) {
            if (unavailableSince == 0L) unavailableSince = now;
            if (now - unavailableSince >= SUBJECT_LOSS_GRACE_NANOS) gracefulExit = true;
        } else {
            unavailableSince = 0L;
        }

        Vec3d rawTarget = targetAvailable.getAsBoolean() ? trackingCenter.get() : smoothedTarget;
        smoothedTarget = smoothedTarget.lerp(rawTarget, alpha);
        Vec3d trackingOffset = smoothedTarget.subtract(originalTarget);

        double progress = Math.min(1.0, (now - startedNanos) / (double) durationNanos);
        Vec3d desiredCamera = basePath.sample(progress).add(trackingOffset);
        smoothedFocus = smoothedFocus.lerp(liveFocusPath.sample(progress), alpha);
        Vec3d resolvedCamera = cameraResolver.apply(desiredCamera, smoothedFocus);
        if (resolvedCamera != null) {
            lastSafeCamera = resolvedCamera;
            lastSafeFocus = smoothedFocus;
            unsafeSince = 0L;
        } else {
            if (unsafeSince == 0L) unsafeSince = now;
            if (now - unsafeSince >= UNSAFE_VIEW_GRACE_NANOS) gracefulExit = true;
        }
        return LookAt.pose(lastSafeCamera, lastSafeFocus)
                .withFov(fovPath.sample(progress));
    }

    @Override
    public boolean finished() {
        return gracefulExit || System.nanoTime() - startedNanos >= durationNanos;
    }
}
