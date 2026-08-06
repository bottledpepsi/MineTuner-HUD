package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Food/hunger level, 0-20. Higher is better; the scale is fixed (unlike Health) so history is stored and colored on raw values directly. */
public final class HungerStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.HUNGER; }
    @Override public String token() { return "hunger"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedHunger(); }

    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getHungerHistory(); }

    @Override public boolean supportsThreshold() { return true; }
    @Override public boolean higherIsBetter() { return true; }
    @Override public float defaultGoodMin() { return 15f; }
    @Override public float defaultWarnMin() { return 6f; }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getHungerColor(custom); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.hungerColorFor(value, custom); }

    @Override public String formatAxisValue(float value) { return Integer.toString(Math.round(value)); }
}
