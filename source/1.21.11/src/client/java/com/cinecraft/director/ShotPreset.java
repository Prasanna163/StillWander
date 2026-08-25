package com.cinecraft.director;

/** A reusable cinematic composition; placement and direction remain scene-relative. */
public record ShotPreset(
        String name,
        SubjectType subjectType,
        boolean wide,
        boolean aerial,
        MotionStyle motionStyle,
        double radius,
        double height,
        double sweepDegrees,
        double radialChange,
        long durationMillis,
        float startingFov,
        float endingFov,
        ShotEnvironment environment
) {
    public ShotPreset(
            String name,
            SubjectType subjectType,
            boolean wide,
            boolean aerial,
            MotionStyle motionStyle,
            double radius,
            double height,
            double sweepDegrees,
            double radialChange,
            long durationMillis,
            float startingFov,
            float endingFov
    ) {
        this(name, subjectType, wide, aerial, motionStyle, radius, height, sweepDegrees,
                radialChange, durationMillis, startingFov, endingFov, ShotEnvironment.ANY);
    }

    public ShotPreset(
            String name,
            SubjectType subjectType,
            boolean wide,
            boolean aerial,
            MotionStyle motionStyle,
            double radius,
            double height,
            double sweepDegrees,
            double radialChange,
            long durationMillis
    ) {
        this(
                name,
                subjectType,
                wide,
                aerial,
                motionStyle,
                radius,
                height,
                sweepDegrees,
                radialChange,
                durationMillis,
                defaultFov(subjectType, wide, aerial),
                defaultFov(subjectType, wide, aerial),
                ShotEnvironment.ANY
        );
    }

    public ShotPreset(
            String name,
            SubjectType subjectType,
            boolean wide,
            boolean aerial,
            MotionStyle motionStyle,
            double radius,
            double height,
            double sweepDegrees,
            double radialChange,
            long durationMillis,
            ShotEnvironment environment
    ) {
        this(name, subjectType, wide, aerial, motionStyle, radius, height, sweepDegrees,
                radialChange, durationMillis, defaultFov(subjectType, wide, aerial),
                defaultFov(subjectType, wide, aerial), environment);
    }

    private static float defaultFov(SubjectType type, boolean wide, boolean aerial) {
        if (wide) return aerial ? 66.0f : 74.0f;
        return switch (type) {
            case PLAYER -> 54.0f;
            case PLAYER_DETAIL -> 34.0f;
            case ENTITY -> 42.0f;
            case GROUP -> 52.0f;
            case FEATURE -> 46.0f;
            case LANDSCAPE -> 60.0f;
        };
    }
}
