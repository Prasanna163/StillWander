/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.director;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public enum ScreenPlacement {
    LEFT_THIRD(-1),
    CENTER(0),
    RIGHT_THIRD(1);

    private final int direction;

    private ScreenPlacement(int direction) {
        this.direction = direction;
    }

    public int direction() {
        return this.direction;
    }
}

