package com.cinecraft.director;

import java.util.ArrayList;
import java.util.List;

/** Soft rules that improve cuts without bypassing collision or framing safety. */
public final class ContinuityRules {
    public ContinuityAssessment assess(ContinuityState state, ContinuityFrame candidate) {
        ContinuityFrame previous = state.last().orElse(null);
        if (previous == null) return ContinuityAssessment.opening();

        List<String> reasons = new ArrayList<>();
        double adjustment = 0.0;

        int scaleStep = Math.abs(candidate.scale().ordinal() - previous.scale().ordinal());
        if (scaleStep == 0) adjustment += add(reasons, "repeat-scale", -10.0);
        else if (scaleStep == 1) adjustment += add(reasons, "scale-step", 4.0);
        else if (scaleStep >= 3) adjustment += add(reasons, "scale-jump", -9.0);

        long matchingRecentScales = state.recent().stream()
                .limit(3)
                .filter(frame -> frame.scale() == candidate.scale())
                .count();
        if (matchingRecentScales >= 2) adjustment += add(reasons, "scale-run", -8.0);

        boolean sameSubject = previous.subjectKey().equals(candidate.subjectKey());
        if (sameSubject) {
            adjustment += add(reasons, "subject-continuity", 3.0);
            if (state.consecutiveSubject(candidate.subjectKey()) >= 2) {
                adjustment += add(reasons, "subject-overuse", -12.0);
            }
        } else if (previous.subjectType() != candidate.subjectType()) {
            adjustment += add(reasons, "subject-variety", 2.0);
        }

        if (sameSubject
                && (previous.motionEnergy().preservesActionAxis() || candidate.motionEnergy().preservesActionAxis())
                && previous.cameraSide().isLateral()
                && candidate.cameraSide().isOpposite(previous.cameraSide())) {
            adjustment += add(reasons, "axis-cross", -18.0);
        }
        if (sameSubject
                && previous.screenDirection().isMoving()
                && candidate.screenDirection().isOpposite(previous.screenDirection())) {
            adjustment += add(reasons, "screen-reversal", -12.0);
        }

        if (candidate.cameraMotionDirection() != 0
                && candidate.cameraMotionDirection() == previous.cameraMotionDirection()) {
            adjustment += add(reasons, "repeat-orbit", -5.0);
        }
        if (candidate.placement() == previous.placement()) {
            adjustment += add(reasons, "repeat-placement", -2.5);
        }
        if (candidate.shotType() == previous.shotType()) {
            adjustment += add(reasons, "repeat-intent", -4.0);
        }

        if (Float.isFinite(previous.endingFov()) && Float.isFinite(candidate.openingFov())) {
            double lensJump = Math.abs(candidate.openingFov() - previous.endingFov());
            if (lensJump > 24.0) {
                adjustment += add(reasons, "lens-jump", -12.0 - Math.min(12.0, (lensJump - 24.0) * 0.5));
            } else if (lensJump > 14.0) {
                adjustment += add(reasons, "lens-step", -4.0);
            }
        }

        double durationRatio = ratio(candidate.durationMillis(), previous.durationMillis());
        if (durationRatio > 2.25) adjustment += add(reasons, "duration-jump", -3.0);
        if (reasons.isEmpty()) reasons.add("neutral");
        return new ContinuityAssessment(adjustment, reasons);
    }

    public double subjectAdjustment(ContinuityState state, String subjectKey, SubjectType subjectType) {
        ContinuityFrame previous = state.last().orElse(null);
        if (previous == null) return 0.0;
        if (previous.subjectKey().equals(subjectKey)) {
            return state.consecutiveSubject(subjectKey) >= 2 ? -14.0 : 4.0;
        }
        if (previous.subjectType() == subjectType && state.consecutiveSubjectType(subjectType) >= 2) return -6.0;
        return previous.subjectType() == subjectType ? 0.0 : 2.0;
    }

    private static double add(List<String> reasons, String reason, double amount) {
        reasons.add(reason);
        return amount;
    }

    private static double ratio(long first, long second) {
        long smaller = Math.min(first, second);
        long larger = Math.max(first, second);
        return smaller <= 0L ? 1.0 : larger / (double) smaller;
    }
}
