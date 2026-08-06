package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Armor points, 0-20. Higher is better. */
public final class ArmorStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.ARMOR; }
    @Override public String token() { return "armor"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedArmor(); }

    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getArmorHistory(); }

    @Override public boolean supportsThreshold() { return true; }
    @Override public boolean higherIsBetter() { return true; }
    @Override public float defaultGoodMin() { return 15f; }
    @Override public float defaultWarnMin() { return 5f; }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getArmorColor(custom); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.armorColorFor(value, custom); }

    @Override public String formatAxisValue(float value) { return Integer.toString(Math.round(value)); }
}
