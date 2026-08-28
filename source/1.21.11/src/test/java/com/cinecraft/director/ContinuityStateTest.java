package com.cinecraft.director;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContinuityStateTest {
    @Test
    void boundsHistoryAndKeepsNewestFirst() {
        ContinuityState state = new ContinuityState();
        for (int index = 0; index < 12; index++) state.remember(frame("subject-" + index, Framing.MEDIUM));

        assertEquals(ContinuityState.HISTORY_LIMIT, state.recent().size());
        assertEquals("subject-11", state.recent().getFirst().subjectKey());
        assertEquals("subject-4", state.recent().getLast().subjectKey());
    }

    @Test
    void resetRemovesSessionContinuity() {
        ContinuityState state = new ContinuityState();
        state.remember(frame("player", Framing.CLOSE));
        state.reset();
        assertTrue(state.last().isEmpty());
    }

    private static ContinuityFrame frame(String key, Framing framing) {
        return new ContinuityFrame(
                key,
                SubjectType.PLAYER,
                ShotType.PROCEDURAL_SUBJECT,
                framing,
                ScreenPlacement.LEFT_THIRD,
                CameraSide.UNKNOWN,
                ScreenDirection.STILL,
                0,
                MotionEnergy.CALM,
                50.0f,
                50.0f,
                30_000L
        );
    }
}
