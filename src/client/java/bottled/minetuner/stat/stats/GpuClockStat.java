package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** GPU core clock speed in MHz, via LibreHardwareMonitor's Remote Web Server (opt-in;
 *  see {@link bottled.minetuner.sample.HardwareSensorPoller}). */
public final class GpuClockStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.GPU_CLOCK;
    }

    @Override
    public String token() {
        return "gpu_clock";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedGpuClock(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawGpuClock(decimals);
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
        return MineTunerDataHolder.getGpuClockHistory();
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "MHz";
    }
}
