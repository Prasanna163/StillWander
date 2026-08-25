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
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public final class ArcLengthSplinePath
implements CameraPath {
    private static final int SAMPLES_PER_SEGMENT = 18;
    private final List<class_243> samples;
    private final double[] cumulative;
    private final double totalLength;

    public ArcLengthSplinePath(List<class_243> controlPoints) {
        if (controlPoints.size() < 2) {
            throw new IllegalArgumentException("A path needs at least two points");
        }
        this.samples = ArcLengthSplinePath.buildSamples(List.copyOf(controlPoints));
        this.cumulative = new double[this.samples.size()];
        double length = 0.0;
        for (int index = 1; index < this.samples.size(); ++index) {
            this.cumulative[index] = length += this.samples.get(index - 1).method_1022(this.samples.get(index));
        }
        this.totalLength = Math.max(1.0E-4, length);
    }

    @Override
    public class_243 sample(double progress) {
        int lower;
        double target = ArcLengthSplinePath.clamp(progress) * this.totalLength;
        int low = 0;
        int high = this.cumulative.length - 1;
        while (low < high) {
            int middle = low + high >>> 1;
            if (this.cumulative[middle] < target) {
                low = middle + 1;
                continue;
            }
            high = middle;
        }
        int upper = Math.max(1, low);
        double section = this.cumulative[upper] - this.cumulative[lower = upper - 1];
        double local = section < 1.0E-6 ? 0.0 : (target - this.cumulative[lower]) / section;
        return this.samples.get(lower).method_35590(this.samples.get(upper), ArcLengthSplinePath.clamp(local));
    }

    private static List<class_243> buildSamples(List<class_243> points) {
        ArrayList<class_243> result = new ArrayList<class_243>();
        result.add(points.getFirst());
        for (int segment = 0; segment < points.size() - 1; ++segment) {
            class_243 p0 = points.get(Math.max(0, segment - 1));
            class_243 p1 = points.get(segment);
            class_243 p2 = points.get(segment + 1);
            class_243 p3 = points.get(Math.min(points.size() - 1, segment + 2));
            for (int sample = 1; sample <= 18; ++sample) {
                double t = (double)sample / 18.0;
                result.add(ArcLengthSplinePath.catmullRom(p0, p1, p2, p3, t));
            }
        }
        return result;
    }

    private static class_243 catmullRom(class_243 p0, class_243 p1, class_243 p2, class_243 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new class_243(ArcLengthSplinePath.coordinate(p0.field_1352, p1.field_1352, p2.field_1352, p3.field_1352, t, t2, t3), ArcLengthSplinePath.coordinate(p0.field_1351, p1.field_1351, p2.field_1351, p3.field_1351, t, t2, t3), ArcLengthSplinePath.coordinate(p0.field_1350, p1.field_1350, p2.field_1350, p3.field_1350, t, t2, t3));
    }

    private static double coordinate(double p0, double p1, double p2, double p3, double t, double t2, double t3) {
        return 0.5 * (2.0 * p1 + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

