package bottled.minetuner.core;

/**
 * Pure FPS arithmetic, free of Minecraft types.
 */
public final class FpsMath {

    private FpsMath() {
    }

    /**
     * True average FPS: frames divided by elapsed time, i.e. {@code n * 1000 / sum(ms)}.
     * (The mean of per-frame FPS overstates the average whenever frame times vary.)
     * Returns 0 for empty input or a non-positive total.
     */
    public static float averageFps(float[] frametimesMs) {
        double totalMs = 0.0;
        for (float ms : frametimesMs) totalMs += ms;
        return averageFps(frametimesMs.length, totalMs);
    }

    /**
     * Same definition from running accumulators.
     */
    public static float averageFps(long frames, double totalMs) {
        if (frames <= 0 || !(totalMs > 0.0)) return 0f;
        return (float) (frames * 1000.0 / totalMs);
    }
}
