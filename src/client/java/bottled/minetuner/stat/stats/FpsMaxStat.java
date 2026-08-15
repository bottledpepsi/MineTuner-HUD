package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Highest per-frame FPS reading observed during the current session (mod load,
 *  or last world join/disconnect — see MineTunerDataHolder — to now). */
public final class FpsMaxStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.FPS_MAX;
    }

    @Override
    public String token() {
        return "fps_max";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedFpsMax(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawFpsMax(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 0;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }

    // No graph — see FpsAvgStat/FpsMinStat's doc.
    @Override
    public boolean supportsGraph() {
        return false;
    }

    // No user-configurable threshold — see MineTunerDataHolder.getSessionFpsColor()'s doc.
    @Override
    public boolean supportsThreshold() {
        return false;
    }

    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getSessionFpsColor(MineTunerDataHolder.getSessionMaxFps());
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getSessionFpsColor(value);
    }
}
