package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeriesPlotTest {

    /**
     * The original per-column algorithm, retained verbatim as the reference.
     */
    private static float[] oldColumns(float[] series, int plotW) {
        int n = series.length;
        float[] out = new float[plotW];
        for (int col = 0; col < plotW; col++) {
            float t = (n <= 1) ? 0f : (col / (float) Math.max(1, plotW - 1)) * (n - 1);
            int i0 = (int) Math.floor(t);
            int i1 = Math.min(n - 1, i0 + 1);
            float frac = t - i0;
            float rawV0 = series[i0], rawV1 = series[i1];
            float v0 = rawV0 >= 0f ? rawV0 : oldNearest(series, i0);
            float v1 = rawV1 >= 0f ? rawV1 : oldNearest(series, i1);
            out[col] = v0 + (v1 - v0) * frac;
        }
        return out;
    }

    private static float oldNearest(float[] series, int fromIdx) {
        int n = series.length;
        for (int d = 0; d < n; d++) {
            int lo = fromIdx - d, hi = fromIdx + d;
            if (lo >= 0 && series[lo] >= 0f) return series[lo];
            if (hi < n && series[hi] >= 0f) return series[hi];
        }
        return 0f;
    }

    @Test
    void allUnavailableLargeSeriesIsNoDataAndFast() {
        float[] s = new float[432_000];
        java.util.Arrays.fill(s, -1f);
        long t0 = System.nanoTime();
        SeriesPlot.Plot p = SeriesPlot.compute(s, 330);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertFalse(p.hasData());
        assertTrue(ms < 50, "took " + ms + " ms");
        assertFalse(SeriesPlot.hasAvailableData(s));
    }

    @Test
    void normalSeriesMatchesOldAlgorithm() {
        java.util.Random rnd = new java.util.Random(1);
        for (int n : new int[]{1, 2, 3, 10, 331, 5000, 36_000}) {
            for (int w : new int[]{1, 2, 50, 330}) {
                float[] s = new float[n];
                for (int i = 0; i < n; i++) s[i] = 5f + 40f * rnd.nextFloat();
                assertArrayEquals(oldColumns(s, w), SeriesPlot.columns(s, w));
            }
        }
    }

    @Test
    void seriesWithGapsMatchesOldAlgorithmAndNeverDipsToBaseline() {
        java.util.Random rnd = new java.util.Random(2);
        for (int iter = 0; iter < 300; iter++) {
            int n = 1 + rnd.nextInt(800);
            float[] s = new float[n];
            boolean anyAvail = false;
            for (int i = 0; i < n; i++) {
                boolean gap = rnd.nextInt(3) == 0;
                s[i] = gap ? -1f : 30f + 10f * rnd.nextFloat();
                anyAvail |= !gap;
            }
            if (!anyAvail) s[rnd.nextInt(n)] = 33f;
            int w = 1 + rnd.nextInt(400);
            float[] cols = SeriesPlot.columns(s, w);
            assertArrayEquals(oldColumns(s, w), cols);
            for (float v : cols) assertTrue(v >= 30f && v <= 40f, "dip: " + v);
        }
    }

    @Test
    void leadingAndTrailingGapsUseNearestAvailable() {
        float[] s = {-1f, -1f, 10f, 20f, -1f, -1f, -1f};
        assertArrayEquals(oldColumns(s, 13), SeriesPlot.columns(s, 13));
    }

    @Test
    void boundsIgnoreSentinelAndWidenFlatRange() {
        float[] b = SeriesPlot.bounds(new float[]{-1f, 5f, 9f, -1f});
        assertEquals(5f, b[0], 0f);
        assertEquals(9f, b[1], 0f);
        float[] flat = SeriesPlot.bounds(new float[]{7f, 7f});
        assertEquals(7f, flat[0], 0f);
        assertEquals(8f, flat[1], 0f);
        float[] none = SeriesPlot.bounds(new float[]{-1f});
        assertEquals(0f, none[0], 0f);
        assertEquals(1f, none[1], 0f);
    }

    @Test
    void computeDoesNotMutateSeries() {
        float[] s = {-1f, 4f, -1f, 8f};
        float[] copy = s.clone();
        SeriesPlot.compute(s, 20);
        assertArrayEquals(copy, s);
    }
}
