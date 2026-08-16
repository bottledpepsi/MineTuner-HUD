package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

public final class Fps01LowStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.FPS_01PCT_LOW;
    }

    @Override
    public String token() {
        return "fps_01low";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedFps01Low(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawFps01Low(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 1;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getFps01LowGraphHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    @Override
    public boolean higherIsBetter() {
        // See Fps1LowStat#higherIsBetter()'s doc — same FPS-shaped reasoning applies here.
        return true;
    }

    @Override
    public float thresholdStep() {
        return 1.0f;
    }

    // Same FpsStat-default-reuse reasoning as Fps1LowStat — see that class's doc.
    @Override
    public float defaultGoodMin() {
        return 60f;
    }

    @Override
    public float defaultWarnMin() {
        return 30f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getPercentileLowFpsColor(currentValueOrZero(), custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getPercentileLowFpsColor(value, custom);
    }

    private static float currentValueOrZero() {
        float v = MineTunerDataHolder.getFps01LowRawValue();
        return Float.isNaN(v) ? 0f : v;
    }
}
