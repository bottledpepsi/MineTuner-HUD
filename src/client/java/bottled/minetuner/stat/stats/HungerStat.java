package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Food/hunger level, 0-20. */
public final class HungerStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.HUNGER;
    }

    @Override
    public String token() {
        return "hunger";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedHunger();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getHungerHistory();
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
        return 15f;
    }

    @Override
    public float defaultWarnMin() {
        return 6f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getHungerColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.hungerColorFor(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return Integer.toString(Math.round(value));
    }
}
