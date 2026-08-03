package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Round-trip latency in ms. Lower is better; color-coded green/yellow/red. */
public final class PingStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.PING; }
    @Override public String token() { return "ping"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedPing(); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawPing(); }

    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getPingHistory(); }

    @Override public boolean supportsThreshold() { return true; }
    @Override public boolean higherIsBetter() { return false; }
    @Override public float defaultGoodMin() { return 80f; }
    @Override public float defaultWarnMin() { return 150f; }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getPingColor(custom); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.pingColorFor(value, custom); }
}
