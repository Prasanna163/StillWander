package com.cinecraft.director;

/** Side of a moving subject's action axis occupied by the camera. */
public enum CameraSide {
    UNKNOWN,
    ON_AXIS,
    LEFT,
    RIGHT;

    public boolean isLateral() {
        return this == LEFT || this == RIGHT;
    }

    public boolean isOpposite(CameraSide other) {
        return (this == LEFT && other == RIGHT) || (this == RIGHT && other == LEFT);
    }
}
