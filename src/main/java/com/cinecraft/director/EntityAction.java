/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 */
package com.cinecraft.director;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;

@Environment(value=EnvType.CLIENT)
public enum EntityAction {
    STILL,
    WALKING,
    RUNNING,
    SWIMMING,
    FLYING,
    SLEEPING,
    USING_ITEM,
    COMBAT;


    public static EntityAction detect(class_1297 entity) {
        if (entity instanceof class_1309) {
            class_1309 living = (class_1309)entity;
            if (living.method_6113()) {
                return SLEEPING;
            }
            if (living.field_6235 > 0 || living.method_6052() != null) {
                return COMBAT;
            }
            if (living.method_6115() || living.field_6252) {
                return USING_ITEM;
            }
        }
        if (entity.method_5681() || entity.method_5799()) {
            return SWIMMING;
        }
        double horizontalSpeedSquared = entity.method_18798().method_37268();
        if (!entity.method_24828() && (horizontalSpeedSquared > 0.006 || Math.abs(entity.method_18798().field_1351) > 0.045)) {
            return FLYING;
        }
        if (horizontalSpeedSquared > 0.1) {
            return RUNNING;
        }
        if (horizontalSpeedSquared > 0.0025) {
            return WALKING;
        }
        return STILL;
    }

    public double leadDistance() {
        return switch (this.ordinal()) {
            case 2, 4 -> 2.4;
            case 3 -> 1.7;
            case 1, 7 -> 1.1;
            case 6 -> 0.45;
            default -> 0.0;
        };
    }

    public boolean moving() {
        return this == WALKING || this == RUNNING || this == SWIMMING || this == FLYING || this == COMBAT;
    }
}

