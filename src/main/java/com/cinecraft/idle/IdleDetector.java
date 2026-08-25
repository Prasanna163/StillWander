/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.idle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class IdleDetector {
    private long timeoutMillis;
    private long lastActivityMillis = System.currentTimeMillis();

    public IdleDetector(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void activity() {
        this.lastActivityMillis = System.currentTimeMillis();
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = Math.max(1000L, timeoutMillis);
    }

    public boolean isIdle() {
        return System.currentTimeMillis() - this.lastActivityMillis >= this.timeoutMillis;
    }
}

