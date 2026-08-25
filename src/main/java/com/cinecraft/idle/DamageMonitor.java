/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1657
 */
package com.cinecraft.idle;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;

@Environment(value=EnvType.CLIENT)
public final class DamageMonitor {
    private static final float EPSILON = 0.001f;
    private UUID playerId;
    private float previousVitality = Float.NaN;

    public boolean update(class_1657 player) {
        UUID currentId = player.method_5667();
        float vitality = player.method_6032() + player.method_6067();
        if (!currentId.equals(this.playerId) || Float.isNaN(this.previousVitality)) {
            this.playerId = currentId;
            this.previousVitality = vitality;
            return false;
        }
        boolean damaged = vitality + 0.001f < this.previousVitality;
        this.previousVitality = vitality;
        return damaged;
    }

    public void reset() {
        this.playerId = null;
        this.previousVitality = Float.NaN;
    }
}

