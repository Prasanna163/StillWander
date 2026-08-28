package com.cinecraft.director;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShotTraceTest {
    @Test
    void ownsItsReasonListAndProducesACompactSummary() {
        List<String> reasons = new ArrayList<>(List.of("scale-step"));
        ShotTrace trace = new ShotTrace(
                7L,
                "player",
                SubjectType.PLAYER,
                ShotType.PROCEDURAL_SUBJECT,
                Framing.MEDIUM,
                CameraSide.UNKNOWN,
                ScreenDirection.STILL,
                new Vec3d(1.0, 65.0, 1.0),
                new Vec3d(2.0, 66.0, 2.0),
                52.0f,
                56.0f,
                30_000L,
                "procedural",
                74.0,
                4.0,
                reasons,
                2,
                850L
        );
        reasons.add("changed-after-construction");

        assertTrue(trace.summary().contains("#7 MEDIUM PLAYER"));
        assertTrue(trace.summary().contains("850 us"));
        assertThrows(UnsupportedOperationException.class, () -> trace.continuityReasons().add("mutable"));
    }
}
