package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** GPU core clock speed in MHz, by LibreHardwareMonitor's Remote Web Server. */
public final class GpuClockStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.GPU_CLOCK;
    }

    @Override
    public String token() {
        return "gpu_clock";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedGpuClock(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawGpuClock(decimals);
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MtssDataHolder.getGpuClockHistory();
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "MHz";
    }
}
