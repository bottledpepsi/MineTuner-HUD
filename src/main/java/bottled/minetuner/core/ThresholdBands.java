package bottled.minetuner.core;

/**
 * Validity rules for a stat's two colour cut-offs.
 */
public final class ThresholdBands {

    private ThresholdBands() {
    }

    /**
     * Both cut-offs must be finite and &gt;= 0, and ordered so the yellow band is reachable:
     * higher-is-better stats need {@code warn <= good}; lower-is-better stats need
     * {@code warn >= good}. A violating {@code warn} is moved onto {@code good}; values that
     * are already valid are returned unchanged.
     */
    public static Cutoffs normalize(float good, float warn, boolean higherIsBetter) {
        float g = Float.isFinite(good) ? Math.max(0f, good) : 0f;
        float w = Float.isFinite(warn) ? Math.max(0f, warn) : 0f;
        if (higherIsBetter) {
            if (w > g) w = g;
        } else {
            if (w < g) w = g;
        }
        return new Cutoffs(g, w);
    }

    /**
     * The cut-offs after normalisation.
     */
    public record Cutoffs(float good, float warn) {
    }
}
