package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * GPU utilization %, via LibreHardwareMonitor's Remote Web Server (opt-in —
 * see {@code HardwareSensorPoller}). High usage during gameplay is normal
 * and expected, not a warning sign the way high CPU or hot GPU temps are,
 * so this intentionally has no threshold coloring.
 */
public final class GpuUsageStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.GPU_USAGE; }
    @Override public String token() { return "gpu_usage"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedGpuUsage(decimals); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawGpuUsage(decimals); }

    @Override public boolean supportsDecimals() { return true; }
    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getGpuUsageHistory(); }

    @Override public String formatAxisValue(float value) { return Math.round(value) + "%"; }
}
