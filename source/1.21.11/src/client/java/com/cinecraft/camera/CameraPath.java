package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

/** A renderer-independent camera trajectory sampled from zero to one. */
@FunctionalInterface
public interface CameraPath {
    Vec3d sample(double progress);
}
