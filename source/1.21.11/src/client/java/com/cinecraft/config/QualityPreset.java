package com.cinecraft.config;

/** Controls how many scene/path candidates are evaluated for each cut. */
public enum QualityPreset {
    PERFORMANCE(16),
    BALANCED(32),
    CINEMATIC(52);

    private final int candidateCount;

    QualityPreset(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public int candidateCount() {
        return candidateCount;
    }
}
