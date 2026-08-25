package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

public final class LookAt {
    private LookAt() { }

    public static CameraPose pose(Vec3d camera, Vec3d target) {
        Vec3d delta = target.subtract(camera);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        return new CameraPose(camera, yaw, pitch).withFocusDistance((float) delta.length());
    }
}
