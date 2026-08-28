package com.cinecraft.director;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ContinuityFrameTest {
    @Test
    void classifiesOppositeSidesOfAMovingSubjectsAxis() {
        Vec3d target = Vec3d.ZERO;
        Vec3d movement = new Vec3d(1.0, 0.0, 0.0);

        assertEquals(CameraSide.LEFT,
                ContinuityFrame.cameraSide(new Vec3d(0.0, 2.0, 8.0), target, movement));
        assertEquals(CameraSide.RIGHT,
                ContinuityFrame.cameraSide(new Vec3d(0.0, 2.0, -8.0), target, movement));
        assertEquals(ScreenDirection.RIGHT,
                ContinuityFrame.screenDirection(new Vec3d(0.0, 2.0, 8.0), target, movement));
        assertEquals(ScreenDirection.LEFT,
                ContinuityFrame.screenDirection(new Vec3d(0.0, 2.0, -8.0), target, movement));
    }

    @Test
    void stationarySubjectsDoNotInventAnActionAxis() {
        Vec3d camera = new Vec3d(4.0, 66.0, 4.0);
        Vec3d target = new Vec3d(0.0, 64.0, 0.0);

        assertEquals(CameraSide.UNKNOWN, ContinuityFrame.cameraSide(camera, target, Vec3d.ZERO));
        assertEquals(ScreenDirection.STILL, ContinuityFrame.screenDirection(camera, target, Vec3d.ZERO));
    }
}
