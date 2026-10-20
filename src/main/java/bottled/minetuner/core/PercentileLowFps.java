package bottled.minetuner.core;

import java.util.Arrays;

public final class PercentileLowFps {

    private PercentileLowFps() {
    }

    public static final float UNAVAILABLE = Float.NaN;

    public static float computeLowFps(float[] frametimesMs, float fraction, int minSampleCount) {
        checkFraction(fraction);
        if (frametimesMs.length < minSampleCount) {
            return UNAVAILABLE;
        }
        return lowFromAscending(sortedAscending(frametimesMs), fraction);
    }

    /**
     * Both the 1% and the 0.1% low from a single sort. Results are identical to calling
     * {@link #computeLowFps} twice (same slice size, same summation order, same
     * per-fraction minimum-sample NaN rule).
     */
    public static Lows computeLows(float[] frametimesMs, int min1PctSamples, int min01PctSamples) {
        boolean want1 = frametimesMs.length >= min1PctSamples;
        boolean want01 = frametimesMs.length >= min01PctSamples;
        if (!want1 && !want01) return new Lows(UNAVAILABLE, UNAVAILABLE);
        float[] ascending = sortedAscending(frametimesMs);
        return new Lows(
                want1 ? lowFromAscending(ascending, 0.01f) : UNAVAILABLE,
                want01 ? lowFromAscending(ascending, 0.001f) : UNAVAILABLE);
    }

    private static void checkFraction(float fraction) {
        if (fraction <= 0f || fraction >= 1f) {
            throw new IllegalArgumentException("fraction must be in (0, 1), got " + fraction);
        }
    }

    private static float[] sortedAscending(float[] values) {
        float[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted;
    }

    /**
     * Mean frametime of the slowest {@code max(1, round(n * fraction))} frames, as FPS. The
     * slice is summed from the slowest frame downwards, exactly like the original
     * descending-sorted prefix, so the floating-point result is bit-identical.
     */
    private static float lowFromAscending(float[] ascending, float fraction) {
        int n = ascending.length;
        int sliceSize = Math.max(1, Math.round(n * fraction));
        double sum = 0.0;
        for (int i = 0; i < sliceSize; i++) {
            sum += ascending[n - 1 - i];
        }
        float avgFrametimeMs = (float) (sum / sliceSize);
        return 1000f / avgFrametimeMs;
    }

    /**
     * The 1% and 0.1% lows computed together. Either may be {@link #UNAVAILABLE}.
     */
    public record Lows(float low1Pct, float low01Pct) {
    }
}
