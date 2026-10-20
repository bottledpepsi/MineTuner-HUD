package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PercentileLowFpsTest {

    @Test
    void belowMinimumSampleCountIsUnavailable() {
        assertTrue(Float.isNaN(PercentileLowFps.computeLowFps(new float[]{10f, 20f}, 0.5f, 3)));
    }

    @Test
    void constantFrametimeGivesConstantFps() {
        float[] ft = new float[200];
        java.util.Arrays.fill(ft, 10f);
        assertEquals(100f, PercentileLowFps.computeLowFps(ft, 0.01f, 1), 0.001f);
    }

    @Test
    void sliceIsAtLeastOneFrame() {
        // 10 samples * 0.01 rounds to 0, clamped to 1 => the single slowest frame.
        float[] ft = {10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 40f};
        assertEquals(25f, PercentileLowFps.computeLowFps(ft, 0.01f, 1), 0.001f);
    }

    @Test
    void slowestFramesAreAveraged() {
        // 100 samples, 1% => the single slowest; 5% => mean of the 5 slowest.
        float[] ft = new float[100];
        java.util.Arrays.fill(ft, 10f);
        ft[3] = 100f;
        ft[50] = 50f;
        assertEquals(10f, PercentileLowFps.computeLowFps(ft, 0.01f, 1), 0.001f);
        // 5 slowest: 100, 50, 10, 10, 10 => mean 36 => 27.78 fps.
        assertEquals(1000f / 36f, PercentileLowFps.computeLowFps(ft, 0.05f, 1), 0.001f);
    }

    @Test
    void invalidFractionThrows() {
        assertThrows(IllegalArgumentException.class, () -> PercentileLowFps.computeLowFps(new float[]{1f}, 0f, 1));
        assertThrows(IllegalArgumentException.class, () -> PercentileLowFps.computeLowFps(new float[]{1f}, 1f, 1));
    }

    // --- B5: computeLows must be bit-identical to the original implementation ---

    /**
     * The original implementation, retained verbatim as the reference.
     */
    private static float referenceLow(float[] frametimesMs, float fraction, int minSampleCount) {
        if (frametimesMs.length < minSampleCount) return Float.NaN;
        float[] sorted = frametimesMs.clone();
        java.util.Arrays.sort(sorted);
        for (int i = 0, j = sorted.length - 1; i < j; i++, j--) {
            float tmp = sorted[i];
            sorted[i] = sorted[j];
            sorted[j] = tmp;
        }
        int sliceSize = Math.max(1, Math.round(sorted.length * fraction));
        double sum = 0.0;
        for (int i = 0; i < sliceSize; i++) sum += sorted[i];
        float avg = (float) (sum / sliceSize);
        return 1000f / avg;
    }

    private static void assertSameBits(float expected, float actual, String msg) {
        assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(actual), msg);
    }

    @Test
    void computeLowsMatchesReferenceOnRandomArrays() {
        java.util.Random rnd = new java.util.Random(12345);
        int[] sizes = {1, 2, 5, 99, 100, 199, 200, 201, 999, 1000, 1999, 2000, 2001, 5000};
        for (int iter = 0; iter < 4000; iter++) {
            int n = sizes[rnd.nextInt(sizes.length)];
            float[] ft = new float[n];
            for (int i = 0; i < n; i++) {
                // Mix of ordinary frames, a few ties and the odd huge stall.
                int kind = rnd.nextInt(10);
                ft[i] = kind == 0 ? 10f : kind == 1 ? 1000f * rnd.nextFloat() + 1f : 3f + 30f * rnd.nextFloat();
            }
            PercentileLowFps.Lows lows = PercentileLowFps.computeLows(ft, 200, 2000);
            assertSameBits(referenceLow(ft, 0.01f, 200), lows.low1Pct(), "1% n=" + n);
            assertSameBits(referenceLow(ft, 0.001f, 2000), lows.low01Pct(), "0.1% n=" + n);
            assertSameBits(referenceLow(ft, 0.01f, 200), PercentileLowFps.computeLowFps(ft, 0.01f, 200), "single n=" + n);
        }
    }

    @Test
    void computeLowsEdgeCases() {
        PercentileLowFps.Lows none = PercentileLowFps.computeLows(new float[]{10f}, 200, 2000);
        assertTrue(Float.isNaN(none.low1Pct()));
        assertTrue(Float.isNaN(none.low01Pct()));

        PercentileLowFps.Lows one = PercentileLowFps.computeLows(new float[]{20f}, 1, 1);
        assertEquals(50f, one.low1Pct(), 0.0001f);
        assertEquals(50f, one.low01Pct(), 0.0001f);

        float[] ties = new float[300];
        java.util.Arrays.fill(ties, 8f);
        PercentileLowFps.Lows t = PercentileLowFps.computeLows(ties, 200, 2000);
        assertEquals(125f, t.low1Pct(), 0.0001f);
        assertTrue(Float.isNaN(t.low01Pct()));
    }
}
