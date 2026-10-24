package bottled.minetuner.core;

/**
 * Mean over a sensor-style series that uses the codebase's shared "-1 = unavailable"
 * sentinel convention (CPU%, GPU%). Unavailable samples are skipped so they cannot
 * drag the average toward 0; returns -1 when no real reading exists.
 */
public final class Averages {

    private Averages() {
    }

    /**
     * Mean of every entry &gt;= 0, or -1.0 when the series is empty or entirely
     * sentinel-valued. Extracted from BenchmarkReport's inline Avg GPU loop so it is
     * independently testable and reusable (mirrors CpuMath's time-weighted variant,
     * which additionally weights each reading by its frame duration).
     */
    public static double averageAvailable(float[] series) {
        double sum = 0.0;
        int count = 0;
        if (series != null) {
            for (float v : series) {
                if (v >= 0f) {
                    sum += v;
                    count++;
                }
            }
        }
        return count > 0 ? sum / count : -1.0;
    }
}
