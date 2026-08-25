package com.cinecraft.idle;

/** Keeps input monitoring independent of Minecraft's evolving window backend. */
public final class IdleDetector {
    private long timeoutMillis;
    private long lastActivityMillis = System.currentTimeMillis();

    public IdleDetector(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void activity() {
        lastActivityMillis = System.currentTimeMillis();
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = Math.max(1_000L, timeoutMillis);
    }

    public boolean isIdle() {
        return System.currentTimeMillis() - lastActivityMillis >= timeoutMillis;
    }
}
