package com.cinecraft.camera;

public interface CinematicShot {
    CameraPose sample(float tickDelta);
    boolean finished();
}
