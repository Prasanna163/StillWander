package com.cinecraft.director;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContinuityRulesTest {
    private final ContinuityRules rules = new ContinuityRules();

    @Test
    void rewardsAnAdjacentScaleOverARepeatedScale() {
        ContinuityState state = stateWith(frame("player", Framing.MEDIUM, CameraSide.UNKNOWN,
                ScreenDirection.STILL, 1, MotionEnergy.CALM, 52.0f, 52.0f));

        double repeated = rules.assess(state, frame("feature", Framing.MEDIUM, CameraSide.UNKNOWN,
                ScreenDirection.STILL, -1, MotionEnergy.CALM, 52.0f, 52.0f)).adjustment();
        double adjacent = rules.assess(state, frame("feature", Framing.WIDE, CameraSide.UNKNOWN,
                ScreenDirection.STILL, -1, MotionEnergy.CALM, 52.0f, 52.0f)).adjustment();

        assertTrue(adjacent > repeated);
    }

    @Test
    void stronglyPenalizesCrossingTheAxisOfAContinuingMovingSubject() {
        ContinuityState state = stateWith(frame("horse", Framing.MEDIUM, CameraSide.LEFT,
                ScreenDirection.RIGHT, 1, MotionEnergy.ACTIVE, 48.0f, 48.0f));
        ContinuityFrame stable = frame("horse", Framing.WIDE, CameraSide.LEFT,
                ScreenDirection.RIGHT, -1, MotionEnergy.ACTIVE, 50.0f, 50.0f);
        ContinuityFrame crossed = frame("horse", Framing.WIDE, CameraSide.RIGHT,
                ScreenDirection.LEFT, -1, MotionEnergy.ACTIVE, 50.0f, 50.0f);

        ContinuityAssessment stableAssessment = rules.assess(state, stable);
        ContinuityAssessment crossedAssessment = rules.assess(state, crossed);

        assertTrue(crossedAssessment.adjustment() <= stableAssessment.adjustment() - 25.0);
        assertTrue(crossedAssessment.reasons().contains("axis-cross"));
        assertTrue(crossedAssessment.reasons().contains("screen-reversal"));
    }

    @Test
    void allowsOneContinuationButPreventsSubjectMonopoly() {
        ContinuityState once = stateWith(frame("player", Framing.MEDIUM, CameraSide.UNKNOWN,
                ScreenDirection.STILL, 0, MotionEnergy.CALM, 52.0f, 52.0f));
        assertEquals(4.0, rules.subjectAdjustment(once, "player", SubjectType.PLAYER));

        once.remember(frame("player", Framing.CLOSE, CameraSide.UNKNOWN,
                ScreenDirection.STILL, 0, MotionEnergy.CALM, 48.0f, 48.0f));
        assertEquals(-14.0, rules.subjectAdjustment(once, "player", SubjectType.PLAYER));
        assertTrue(rules.subjectAdjustment(once, "landscape", SubjectType.LANDSCAPE) > 0.0);
    }

    @Test
    void deterministicInputProducesTheSameAssessment() {
        ContinuityState state = stateWith(frame("player", Framing.CLOSE, CameraSide.UNKNOWN,
                ScreenDirection.STILL, 0, MotionEnergy.CALM, 36.0f, 36.0f));
        ContinuityFrame candidate = frame("landscape", Framing.EXTREME_WIDE, CameraSide.UNKNOWN,
                ScreenDirection.STILL, 1, MotionEnergy.CALM, 78.0f, 78.0f);

        assertEquals(rules.assess(state, candidate), rules.assess(state, candidate));
    }

    private static ContinuityState stateWith(ContinuityFrame frame) {
        ContinuityState state = new ContinuityState();
        state.remember(frame);
        return state;
    }

    private static ContinuityFrame frame(
            String key,
            Framing framing,
            CameraSide cameraSide,
            ScreenDirection screenDirection,
            int cameraDirection,
            MotionEnergy energy,
            float openingFov,
            float endingFov
    ) {
        return new ContinuityFrame(
                key,
                key.equals("landscape") ? SubjectType.LANDSCAPE : SubjectType.PLAYER,
                ShotType.PROCEDURAL_SUBJECT,
                framing,
                ScreenPlacement.LEFT_THIRD,
                cameraSide,
                screenDirection,
                cameraDirection,
                energy,
                openingFov,
                endingFov,
                30_000L
        );
    }
}
