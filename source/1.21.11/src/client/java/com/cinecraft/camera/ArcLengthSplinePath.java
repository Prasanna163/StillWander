package com.cinecraft.camera;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Catmull-Rom waypoint path resampled by arc length for constant visual speed. */
public final class ArcLengthSplinePath implements CameraPath {
    private static final int SAMPLES_PER_SEGMENT = 18;

    private final List<Vec3d> samples;
    private final double[] cumulative;
    private final double totalLength;

    public ArcLengthSplinePath(List<Vec3d> controlPoints) {
        if (controlPoints.size() < 2) throw new IllegalArgumentException("A path needs at least two points");
        this.samples = buildSamples(List.copyOf(controlPoints));
        this.cumulative = new double[samples.size()];
        double length = 0.0;
        for (int index = 1; index < samples.size(); index++) {
            length += samples.get(index - 1).distanceTo(samples.get(index));
            cumulative[index] = length;
        }
        this.totalLength = Math.max(0.0001, length);
    }

    @Override
    public Vec3d sample(double progress) {
        double target = clamp(progress) * totalLength;
        int low = 0;
        int high = cumulative.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (cumulative[middle] < target) low = middle + 1;
            else high = middle;
        }
        int upper = Math.max(1, low);
        int lower = upper - 1;
        double section = cumulative[upper] - cumulative[lower];
        double local = section < 0.000001 ? 0.0 : (target - cumulative[lower]) / section;
        return samples.get(lower).lerp(samples.get(upper), clamp(local));
    }

    private static List<Vec3d> buildSamples(List<Vec3d> points) {
        List<Vec3d> result = new ArrayList<>();
        result.add(points.getFirst());
        for (int segment = 0; segment < points.size() - 1; segment++) {
            Vec3d p0 = points.get(Math.max(0, segment - 1));
            Vec3d p1 = points.get(segment);
            Vec3d p2 = points.get(segment + 1);
            Vec3d p3 = points.get(Math.min(points.size() - 1, segment + 2));
            for (int sample = 1; sample <= SAMPLES_PER_SEGMENT; sample++) {
                double t = sample / (double) SAMPLES_PER_SEGMENT;
                result.add(catmullRom(p0, p1, p2, p3, t));
            }
        }
        return result;
    }

    private static Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3d(
                coordinate(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
                coordinate(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
                coordinate(p0.z, p1.z, p2.z, p3.z, t, t2, t3)
        );
    }

    private static double coordinate(double p0, double p1, double p2, double p3, double t, double t2, double t3) {
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
