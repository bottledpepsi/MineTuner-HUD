package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Horizontal movement speed in blocks/second. */
public final class SpeedStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.SPEED;
    }

    @Override
    public String token() {
        return "speed";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedSpeed(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawSpeed(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 2;
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
        return MineTunerDataHolder.getSpeedHistory();
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getSpeedColor();
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.speedColorFor(value);
    }

    @Override
    public String formatAxisValue(float value) {
        return String.format("%.1f", value);
    }
}
