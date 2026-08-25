package com.cinecraft.director;

import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.FovPath;

public record ShotPlan(
        ShotType type,
        CameraPath path,
        CameraPath focusPath,
        CameraPath subjectPath,
        FovPath fovPath,
        long durationMillis,
        ShotComposition composition,
        String source
) { }
