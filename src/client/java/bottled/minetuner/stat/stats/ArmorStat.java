package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Armor points, 0-20. */
public final class ArmorStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.ARMOR;
    }

    @Override
    public String token() {
        return "armor";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedArmor();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getArmorHistory();
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
        return 5f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getArmorColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.armorColorFor(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return Integer.toString(Math.round(value));
    }
}
