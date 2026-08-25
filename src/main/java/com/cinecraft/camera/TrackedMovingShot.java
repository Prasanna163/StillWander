/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 */
package com.cinecraft.camera;

import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.CinematicShot;
import com.cinecraft.camera.FovPath;
import com.cinecraft.camera.LookAt;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public final class TrackedMovingShot
implements CinematicShot {
    private static final double TRACKING_TIME_CONSTANT_SECONDS = 0.7;
    private static final long SUBJECT_LOSS_GRACE_NANOS = 1200000000L;
    private static final long UNSAFE_VIEW_GRACE_NANOS = 1500000000L;
    private final CameraPath basePath;
    private final CameraPath liveFocusPath;
    private final Supplier<class_243> trackingCenter;
    private final BooleanSupplier targetAvailable;
    private final class_243 originalTarget;
    private final FovPath fovPath;
    private final BiFunction<class_243, class_243, class_243> cameraResolver;
    private final long startedNanos;
    private final long durationNanos;
    private long previousNanos;
    private class_243 smoothedTarget;
    private class_243 smoothedFocus;
    private class_243 lastSafeCamera;
    private class_243 lastSafeFocus;
    private long unavailableSince;
    private long unsafeSince;
    private boolean gracefulExit;

    public TrackedMovingShot(CameraPath basePath, CameraPath liveFocusPath, Supplier<class_243> trackingCenter, BooleanSupplier targetAvailable, class_243 originalTarget, FovPath fovPath, long durationMillis, BiFunction<class_243, class_243, class_243> cameraResolver) {
        this.previousNanos = this.startedNanos = System.nanoTime();
        this.basePath = basePath;
        this.liveFocusPath = liveFocusPath;
        this.trackingCenter = trackingCenter;
        this.targetAvailable = targetAvailable;
        this.originalTarget = originalTarget;
        this.fovPath = fovPath;
        this.smoothedTarget = originalTarget;
        this.smoothedFocus = liveFocusPath.sample(0.0);
        this.durationNanos = durationMillis * 1000000L;
        this.cameraResolver = cameraResolver;
        this.lastSafeCamera = basePath.sample(0.0);
        this.lastSafeFocus = originalTarget;
    }

    @Override
    public CameraPose sample(float tickDelta) {
        long now = System.nanoTime();
        double deltaSeconds = Math.min(0.1, Math.max(0.0, (double)(now - this.previousNanos) / 1.0E9));
        this.previousNanos = now;
        double alpha = 1.0 - Math.exp(-deltaSeconds / 0.7);
        if (!this.targetAvailable.getAsBoolean()) {
            if (this.unavailableSince == 0L) {
                this.unavailableSince = now;
            }
            if (now - this.unavailableSince >= 1200000000L) {
                this.gracefulExit = true;
            }
        } else {
            this.unavailableSince = 0L;
        }
        class_243 rawTarget = this.targetAvailable.getAsBoolean() ? this.trackingCenter.get() : this.smoothedTarget;
        this.smoothedTarget = this.smoothedTarget.method_35590(rawTarget, alpha);
        class_243 trackingOffset = this.smoothedTarget.method_1020(this.originalTarget);
        double progress = Math.min(1.0, (double)(now - this.startedNanos) / (double)this.durationNanos);
        class_243 desiredCamera = this.basePath.sample(progress).method_1019(trackingOffset);
        this.smoothedFocus = this.smoothedFocus.method_35590(this.liveFocusPath.sample(progress), alpha);
        class_243 resolvedCamera = this.cameraResolver.apply(desiredCamera, this.smoothedFocus);
        if (resolvedCamera != null) {
            this.lastSafeCamera = resolvedCamera;
            this.lastSafeFocus = this.smoothedFocus;
            this.unsafeSince = 0L;
        } else {
            if (this.unsafeSince == 0L) {
                this.unsafeSince = now;
            }
            if (now - this.unsafeSince >= 1500000000L) {
                this.gracefulExit = true;
            }
        }
        return LookAt.pose(this.lastSafeCamera, this.lastSafeFocus).withFov(this.fovPath.sample(progress));
    }

    @Override
    public boolean finished() {
        return this.gracefulExit || System.nanoTime() - this.startedNanos >= this.durationNanos;
    }
}

