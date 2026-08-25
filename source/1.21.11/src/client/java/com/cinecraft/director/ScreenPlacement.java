package com.cinecraft.director;

public enum ScreenPlacement {
    LEFT_THIRD(-1),
    CENTER(0),
    RIGHT_THIRD(1);

    private final int direction;

    ScreenPlacement(int direction) {
        this.direction = direction;
    }

    public int direction() {
        return direction;
    }
}
