package bottled.minetuner.stat.math;

import java.util.Arrays;

public final class PercentileLowFps {

    private PercentileLowFps() {
    }

    public static final float UNAVAILABLE = Float.NaN;

    public static float computeLowFps(float[] frametimesMs, float fraction, int minSampleCount) {
        if (fraction <= 0f || fraction >= 1f) {
            throw new IllegalArgumentException("fraction must be in (0, 1), got " + fraction);
        }
        if (frametimesMs.length < minSampleCount) {
            return UNAVAILABLE;
        }

        float[] sorted = frametimesMs.clone();
        // Descending (slowest/highest-frametime first), so the slice we want is a simple
        // prefix — no need to also track/derive an offset from the array's tail.
        sort(sorted);

        int sliceSize = Math.max(1, Math.round(sorted.length * fraction));

        double sum = 0.0;
        for (int i = 0; i < sliceSize; i++) {
            sum += sorted[i];
        }
        float avgFrametimeMs = (float) (sum / sliceSize);

        return 1000f / avgFrametimeMs;
    }

    private static void sort(float[] values) {
        Arrays.sort(values);
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            float tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }
}
