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

    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public float defaultGoodMin() {
        return 8.33f;
    }

    @Override
    public float defaultWarnMin() {
        return 16.67f;
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
