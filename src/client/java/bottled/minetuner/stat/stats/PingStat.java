package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Round-trip latency in ms. */
public final class PingStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.PING;
    }

    @Override
    public String token() {
        return "ping";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedPing();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawPing();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getPingHistory();
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
        return 80f;
    }

    @Override
    public float defaultWarnMin() {
        return 150f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getPingColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.pingColorFor(value, custom);
    }
}
