package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** JVM heap usage (used / max MB), color-coded by fill percentage. */
public final class MemoryStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.MEMORY;
    }

    @Override
    public String token() {
        return "mem";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedMem();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getMemHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    @Override
    public boolean higherIsBetter() {
        return false;
    }

    @Override
    public float defaultGoodMin() {
        return 60f;
    }

    @Override
    public float defaultWarnMin() {
        return 85f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getMemColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.memColorForPercent(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "%";
    }
}
