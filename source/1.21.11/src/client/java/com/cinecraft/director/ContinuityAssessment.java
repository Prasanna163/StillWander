package com.cinecraft.director;

import java.util.List;
import java.util.Locale;

/** Soft editorial adjustment and the reasons behind it. */
public record ContinuityAssessment(double adjustment, List<String> reasons) {
    public ContinuityAssessment {
        reasons = List.copyOf(reasons);
    }

    public static ContinuityAssessment opening() {
        return new ContinuityAssessment(0.0, List.of("opening"));
    }

    public String summary() {
        String labels = reasons.stream().limit(3).reduce((left, right) -> left + "," + right).orElse("neutral");
        return String.format(Locale.ROOT, "%+.1f %s", adjustment, labels);
    }
}
