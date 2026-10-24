package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

public final class FrametimeStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.FRAMETIME;
    }

    @Override
    public String token() {
        return "frametime";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedFrametime(decimals);
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getFrametimeHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    /** Frametime is lower-is-better, same direction as CPU/ping/GPU temp: fewer
     *  milliseconds per frame is better. This must match frametimeColorFor's own
     *  {@code <=} comparisons (see MineTunerDataHolder) — an inverted value here
     *  makes ThresholdBands.normalize collapse the warn band onto the good band on
     *  every config load, leaving custom thresholds with no reachable yellow band. */
    @Override
    public boolean higherIsBetter() {
        return false;
    }

    /** Frame-time equivalents of FpsStat's 60/30 defaults (1000/60 ≈ 16.7 ms,
     *  1000/30 ≈ 33.3 ms), matching the hardcoded bands in
     *  MineTunerDataHolder#frametimeColorFor so the Cloth "default value" display,
     *  freshly-seeded custom thresholds, and built-in coloring all describe the
     *  same real-world performance bands. */
    @Override
    public float defaultGoodMin() {
        return 16.7f;
    }

    @Override
    public float defaultWarnMin() {
        return 33.3f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getFrametimeColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.frametimeColorFor(value, custom);
    }
}
