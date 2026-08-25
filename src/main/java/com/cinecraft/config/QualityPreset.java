/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public enum QualityPreset {
    PERFORMANCE(16),
    BALANCED(32),
    CINEMATIC(52);

    private final int candidateCount;

    private QualityPreset(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public int candidateCount() {
        return this.candidateCount;
    }
}

