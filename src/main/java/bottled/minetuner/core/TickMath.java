package bottled.minetuner.core;

/**
 * Pure tick-rate arithmetic.
 */
public final class TickMath {

    private TickMath() {
    }

    /**
     * TPS equivalent of a milliseconds-per-tick sample, capped at the tick rate.
     * A non-positive sample (no measurement) reads as the full tick rate.
     */
    public static float tpsFromMspt(float mspt, float tickRate) {
        if (mspt > 0f) return Math.min(tickRate, 1000f / mspt);
        return tickRate;
    }
}
