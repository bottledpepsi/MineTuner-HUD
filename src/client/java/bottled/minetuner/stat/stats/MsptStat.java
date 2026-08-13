package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Milliseconds per tick. */
public final class MsptStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.MSPT;
    }

    @Override
    public String token() {
        return "mspt";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedMspt(decimals);
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
        return MineTunerDataHolder.getMsptHistory();
    }

    // MSPT isn't in THRESHOLD_STATS (no entry of its own).
    @Override
    public boolean supportsThreshold() {
        return false;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getTpsColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.tpsColorFor(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return String.format("%.1f", value);
    }
}
