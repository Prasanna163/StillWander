package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CameraPathInvariantTest {
    @Test
    void railAndSplinePreserveTheirEndpoints() {
        Vec3d start = new Vec3d(-4.0, 70.0, 2.0);
        Vec3d end = new Vec3d(12.0, 73.0, -8.0);
        RailPath rail = new RailPath(start, end);
        ArcLengthSplinePath spline = new ArcLengthSplinePath(List.of(
                start,
                new Vec3d(2.0, 72.0, 4.0),
                end
        ));

        assertEquals(start, rail.sample(0.0));
        assertEquals(end, rail.sample(1.0));
        assertEquals(start, spline.sample(0.0));
        assertTrue(spline.sample(1.0).distanceTo(end) < 0.000001);
    }

    @Test
    void supportedPathsAndFovProduceFiniteBoundedSamples() {
        CameraPath pan = new PanPath(new Vec3d(0.0, 64.0, 0.0), 0.2, 1.1, 8.0, 12.0, 66.0, 69.0);
        FovPath fov = FovPath.linear(34.0f, 78.0f);

        for (int sample = 0; sample <= 100; sample++) {
            double progress = sample / 100.0;
            Vec3d point = pan.sample(progress);
            assertTrue(Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z));
            assertTrue(fov.sample(progress) >= 24.0f && fov.sample(progress) <= 90.0f);
        }
    }
}
