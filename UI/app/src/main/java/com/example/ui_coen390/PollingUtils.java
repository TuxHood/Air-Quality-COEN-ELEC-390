package com.example.ui_coen390;

public class PollingUtils {
    /**
     * Compute delay in milliseconds until the next interval boundary.
     * Returns a value in (0, intervalMs], i.e. if now is exactly on a boundary,
     * this returns intervalMs (so caller schedules the next tick intervalMs later).
     */
    public static long computeNextDelay(long intervalMs) {
        if (intervalMs <= 0) return 0L;
        long now = System.currentTimeMillis();
        long rem = now % intervalMs;
        return rem == 0L ? intervalMs : (intervalMs - rem);
    }
}
