package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** GPU core temperature in °C, by LibreHardwareMonitor's Remote Web Server (opt-in. */
public final class GpuTempStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.GPU_TEMP;
    }

    @Override
    public String token() {
        return "gpu_temp";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedGpuTemp(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawGpuTemp(decimals);
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
        return MtssDataHolder.getGpuTempHistory();
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

    // Conservative headroom under typical thermal-throttle points for.
    // consumer NVIDIA/AMD GPUs (commonly ~83-95°C depending on the card).
    // a general "getting warm" / "hot" signal, not a vendor-specific limit,.
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
    public int color(MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.getGpuTempColor(custom);
    }

    @Override
    public int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.gpuTempColorFor(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return String.format("%.0f°C", value);
    }
}
