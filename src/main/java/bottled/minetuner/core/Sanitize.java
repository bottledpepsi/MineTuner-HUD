package bottled.minetuner.core;

/**
 * Range clamps for values read from a hand-editable config file.
 */
public final class Sanitize {

    private Sanitize() {
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Clamps into [min, max]; NaN/Infinity become {@code fallback} (itself clamped).
     */
    public static float clamp(float v, float min, float max, float fallback) {
        float x = Float.isFinite(v) ? v : fallback;
        return Math.max(min, Math.min(max, x));
    }

    /**
     * Double variant of {@link #clamp(float, float, float, float)}.
     */
    public static double clamp(double v, double min, double max, double fallback) {
        double x = Double.isFinite(v) ? v : fallback;
        return Math.max(min, Math.min(max, x));
    }
}
