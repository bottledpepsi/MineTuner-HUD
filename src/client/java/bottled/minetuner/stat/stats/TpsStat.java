package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Server ticks per second. */
public final class TpsStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.TPS;
    }

    @Override
    public String token() {
        return "tps";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedTps(decimals);
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
        return MineTunerDataHolder.getTpsHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public float thresholdStep() {
        return 0.5f;
    }

    @Override
    public float defaultGoodMin() {
        return 18f;
    }

    @Override
    public float defaultWarnMin() {
        return 14f;
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
