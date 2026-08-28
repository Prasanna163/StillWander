package com.cinecraft.director;

import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Locale;

/** One accepted planning decision retained in a bounded runtime diagnostic buffer. */
public record ShotTrace(
        long index,
        String subjectKey,
        SubjectType subjectType,
        ShotType shotType,
        Framing scale,
        CameraSide cameraSide,
        ScreenDirection screenDirection,
        Vec3d startCamera,
        Vec3d endCamera,
        float openingFov,
        float endingFov,
        long durationMillis,
        String source,
        double score,
        double continuityAdjustment,
        List<String> continuityReasons,
        int rejectedCandidates,
        long planningMicros
) {
    public ShotTrace {
        continuityReasons = List.copyOf(continuityReasons);
    }

    public String summary() {
        return String.format(
                Locale.ROOT,
                "#%d %s %s score %.1f continuity %+.1f in %d us",
                index,
                scale,
                subjectType,
                score,
                continuityAdjustment,
                planningMicros
        );
    }
}
