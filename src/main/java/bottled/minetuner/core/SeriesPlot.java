package bottled.minetuner.core;

/**
 * Display-ready data for plotting one frozen series across {@code plotW} pixel columns.
 * Unavailable samples use the {@code -1} sentinel; they are never modified here (the CSV
 * writer relies on the sentinel), they are only skipped when computing the plot.
 */
public final class SeriesPlot {

    private SeriesPlot() {
    }

    /**
     * True if at least one sample is available (not the -1 sentinel).
     */
    public static boolean hasAvailableData(float[] series) {
        for (float v : series) {
            if (v >= 0f) return true;
        }
        return false;
    }

    /**
     * [min, max] over the available samples, or [0, 1] if there are none; the range is
     * widened to at least 1 when flat. Same rules the results graphs always used.
     */
    public static float[] bounds(float[] series) {
        float minV = 0f, maxV = 0f;
        boolean seen = false;
        for (float v : series) {
            if (v < 0f) continue;
            if (!seen || v < minV) minV = v;
            if (v > maxV) maxV = v;
            seen = true;
        }
        if (!seen) return new float[]{0f, 1f};
        if (maxV <= minV) maxV = minV + 1f;
        return new float[]{minV, maxV};
    }

    /**
     * Computes everything a draw call needs for a series. Cost is O(N + plotW) and it is
     * meant to run once per (series, plotW), not per frame. An all-unavailable or empty
     * series (or a non-positive width) yields {@link Plot#EMPTY}.
     */
    public static Plot compute(float[] series, int plotW) {
        if (plotW <= 0 || series.length == 0 || !hasAvailableData(series)) return Plot.EMPTY;
        float[] b = bounds(series);
        return new Plot(true, columns(series, plotW), b[0], b[1]);
    }

    /**
     * One value per pixel column: linear interpolation between the two samples a column
     * maps to, where an unavailable sample is replaced by its nearest available neighbour
     * (ties prefer the earlier sample) so a sensor gap never draws as a dip to the bottom.
     * The series must contain at least one available sample.
     */
    public static float[] columns(float[] series, int plotW) {
        int n = series.length;
        float[] out = new float[plotW];
        int[] prev = null;
        int[] next = null;
        for (float v : series) {
            if (v < 0f) {
                prev = new int[n];
                next = new int[n];
                fillNeighbours(series, prev, next);
                break;
            }
        }
        for (int col = 0; col < plotW; col++) {
            float t = (n <= 1) ? 0f : (col / (float) Math.max(1, plotW - 1)) * (n - 1);
            int i0 = (int) Math.floor(t);
            int i1 = Math.min(n - 1, i0 + 1);
            float frac = t - i0;
            float v0 = series[i0] >= 0f ? series[i0] : nearest(series, prev, next, i0);
            float v1 = series[i1] >= 0f ? series[i1] : nearest(series, prev, next, i1);
            out[col] = v0 + (v1 - v0) * frac;
        }
        return out;
    }

    /**
     * prev[i] = last available index &lt;= i (or -1); next[i] = first available index &gt;= i (or -1).
     */
    private static void fillNeighbours(float[] s, int[] prev, int[] next) {
        int n = s.length;
        int last = -1;
        for (int i = 0; i < n; i++) {
            if (s[i] >= 0f) last = i;
            prev[i] = last;
        }
        int nxt = -1;
        for (int i = n - 1; i >= 0; i--) {
            if (s[i] >= 0f) nxt = i;
            next[i] = nxt;
        }
    }

    private static float nearest(float[] s, int[] prev, int[] next, int idx) {
        int lo = prev[idx];
        int hi = next[idx];
        if (lo < 0) return s[hi];
        if (hi < 0) return s[lo];
        return (idx - lo) <= (hi - idx) ? s[lo] : s[hi];
    }

    /**
     * Result of {@link #compute}: per-column values plus the vertical range.
     */
    public record Plot(boolean hasData, float[] columns, float min, float max) {
        public static final Plot EMPTY = new Plot(false, new float[0], 0f, 1f);
    }
}
