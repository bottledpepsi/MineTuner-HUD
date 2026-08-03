package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * Horizontal movement speed in blocks/second. Its gray/yellow/white coloring
 * isn't a good/warn/bad scale, so unlike TPS/FPS/Ping/Memory/CPU it doesn't
 * support a custom threshold — {@link #color} ignores the argument.
 */
public final class SpeedStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.SPEED; }
    @Override public String token() { return "speed"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedSpeed(decimals); }
    @Override public int defaultDecimals() { return 2; }

    @Override public boolean supportsDecimals() { return true; }
    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getSpeedHistory(); }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getSpeedColor(); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.speedColorFor(value); }

    @Override public String formatAxisValue(float value) { return String.format("%.1f", value); }
}
