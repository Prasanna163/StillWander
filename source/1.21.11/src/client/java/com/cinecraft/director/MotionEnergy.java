package com.cinecraft.director;

/** Coarse editorial energy derived from what the subject is doing. */
public enum MotionEnergy {
    CALM,
    GENTLE,
    ACTIVE,
    INTENSE;

    public static MotionEnergy from(EntityAction action) {
        if (action == null) return CALM;
        return switch (action) {
            case SLEEPING, STILL -> CALM;
            case WALKING, USING_ITEM -> GENTLE;
            case SWIMMING, FLYING -> ACTIVE;
            case RUNNING, COMBAT -> INTENSE;
        };
    }

    public boolean preservesActionAxis() {
        return this == ACTIVE || this == INTENSE;
    }
}
