package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** GPU core temperature in °C, via LibreHardwareMonitor's Remote Web Server (opt-in;
 *  see {@link bottled.minetuner.sample.HardwareSensorPoller}). */
public final class GpuTempStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.GPU_TEMP;
    }

    @Override
    public String token() {
        return "gpu_temp";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedGpuTemp(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawGpuTemp(decimals);
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
        return MineTunerDataHolder.getGpuTempHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    @Override
    public boolean higherIsBetter() {
        return false;
    }

    @Override
    public float thresholdStep() {
        return 1.0f;
    }

    // Conservative headroom under typical thermal-throttle points for
    // consumer NVIDIA/AMD GPUs (commonly ~83-95°C depending on the card). This is
    // a general "getting warm" / "hot" signal, not a vendor-specific limit,
    // since LHM's tree doesn't expose the per-card throttle point itself.
    @Override
    public float defaultGoodMin() {
        return 70f;
    }

    @Override
    public float defaultWarnMin() {
        return 85f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getGpuTempColor(custom);
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.gpuTempColorFor(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return String.format("%.0f°C", value);
    }
}
