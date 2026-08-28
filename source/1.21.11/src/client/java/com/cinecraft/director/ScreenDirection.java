package com.cinecraft.director;

/** Apparent horizontal movement of a subject inside the frame. */
public enum ScreenDirection {
    UNKNOWN,
    STILL,
    LEFT,
    RIGHT;

    public boolean isMoving() {
        return this == LEFT || this == RIGHT;
    }

    public boolean isOpposite(ScreenDirection other) {
        return (this == LEFT && other == RIGHT) || (this == RIGHT && other == LEFT);
    }
}
