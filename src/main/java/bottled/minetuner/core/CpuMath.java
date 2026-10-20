package bottled.minetuner.core;

/**
 * Pure benchmark CPU arithmetic.
 */
public final class CpuMath {

    private CpuMath() {
    }

    /**
     * Time-weighted average of a sample-and-hold CPU series: each frame's reading holds for
     * that frame's duration. Entries below 0 (the "unavailable" sentinel) and frames with a
     * non-positive duration are ignored. Returns -1 when no valid reading exists.
     */
    public static double timeWeightedAverage(float[] frametimesMs, float[] cpuPercent) {
        int n = Math.min(frametimesMs.length, cpuPercent.length);
        double weighted = 0.0;
        double totalMs = 0.0;
        for (int i = 0; i < n; i++) {
            float v = cpuPercent[i];
            float dt = frametimesMs[i];
            if (v < 0f || !(dt > 0f)) continue;
            weighted += (double) v * dt;
            totalMs += dt;
        }
        return totalMs > 0.0 ? weighted / totalMs : -1.0;
    }
}
