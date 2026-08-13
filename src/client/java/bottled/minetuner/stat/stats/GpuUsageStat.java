package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** GPU utilization %, via LibreHardwareMonitor's Remote Web Server (opt-in;
 *  see {@link bottled.minetuner.sample.HardwareSensorPoller}). */
public final class GpuUsageStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.GPU_USAGE;
    }

    @Override
    public String token() {
        return "gpu_usage";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedGpuUsage(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawGpuUsage(decimals);
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
        return MineTunerDataHolder.getGpuUsageHistory();
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "%";
    }
}
