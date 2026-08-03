package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * JVM process CPU load %, polled every 500ms. HotSpot/OpenJDK only — shows
 * "N/A" (via MtssDataHolder) on other JVM vendors. Lower is better.
 */
public final class CpuStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.CPU; }
    @Override public String token() { return "cpu"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedCpu(decimals); }

    @Override public boolean supportsDecimals() { return true; }
    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getCpuHistory(); }

    @Override public boolean supportsThreshold() { return true; }
    @Override public boolean higherIsBetter() { return false; }
    @Override public float defaultGoodMin() { return 50f; }
    @Override public float defaultWarnMin() { return 80f; }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getCpuColor(custom); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.cpuColorFor(value, custom); }

    @Override public String formatAxisValue(float value) { return String.format("%.1f", value); }
}
