package com.cinecraft.camera;

public final class StaticShot implements CinematicShot {
    private final CameraPose pose;
    private final long endMillis;

    public StaticShot(CameraPose pose, long durationMillis) {
        this.pose = pose;
        this.endMillis = System.currentTimeMillis() + durationMillis;
    }

    @Override public CameraPose sample(float tickDelta) { return pose; }
    @Override public boolean finished() { return System.currentTimeMillis() >= endMillis; }
}
