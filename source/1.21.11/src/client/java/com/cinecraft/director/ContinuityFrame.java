package com.cinecraft.director;

import net.minecraft.util.math.Vec3d;

/** Editorial facts retained after a shot without retaining live world objects. */
public record ContinuityFrame(
        String subjectKey,
        SubjectType subjectType,
        ShotType shotType,
        Framing scale,
        ScreenPlacement placement,
        CameraSide cameraSide,
        ScreenDirection screenDirection,
        int cameraMotionDirection,
        MotionEnergy motionEnergy,
        float openingFov,
        float endingFov,
        long durationMillis
) {
    private static final double MOVEMENT_EPSILON = 0.001;

    public ContinuityFrame {
        subjectKey = subjectKey == null || subjectKey.isBlank() ? "unknown" : subjectKey;
    }

    public static ContinuityFrame from(ShotPlan plan, SceneSubject subject) {
        Vec3d camera = plan.path().sample(0.0);
        Vec3d target = subject.target();
        Vec3d movement = subject.movementVector();
        return new ContinuityFrame(
                subject.key(),
                subject.type(),
                plan.type(),
                plan.composition().framing(),
                plan.composition().placement(),
                cameraSide(camera, target, movement),
                screenDirection(camera, target, movement),
                Integer.signum(plan.composition().movementDirection()),
                MotionEnergy.from(subject.action()),
                plan.fovPath().sample(0.0),
                plan.fovPath().sample(1.0),
                plan.durationMillis()
        );
    }

    static CameraSide cameraSide(Vec3d camera, Vec3d target, Vec3d movement) {
        double movementLength = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        Vec3d offset = camera.subtract(target);
        double offsetLength = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (movementLength < MOVEMENT_EPSILON || offsetLength < MOVEMENT_EPSILON) return CameraSide.UNKNOWN;
        double normalizedCross = (movement.x * offset.z - movement.z * offset.x)
                / (movementLength * offsetLength);
        if (Math.abs(normalizedCross) < 0.12) return CameraSide.ON_AXIS;
        return normalizedCross > 0.0 ? CameraSide.LEFT : CameraSide.RIGHT;
    }

    static ScreenDirection screenDirection(Vec3d camera, Vec3d target, Vec3d movement) {
        double movementLength = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (movementLength < MOVEMENT_EPSILON) return ScreenDirection.STILL;
        Vec3d view = target.subtract(camera);
        double viewLength = Math.sqrt(view.x * view.x + view.z * view.z);
        if (viewLength < MOVEMENT_EPSILON) return ScreenDirection.UNKNOWN;
        double rightX = -view.z / viewLength;
        double rightZ = view.x / viewLength;
        double normalizedDirection = (movement.x * rightX + movement.z * rightZ) / movementLength;
        if (Math.abs(normalizedDirection) < 0.08) return ScreenDirection.UNKNOWN;
        return normalizedDirection > 0.0 ? ScreenDirection.RIGHT : ScreenDirection.LEFT;
    }
}
