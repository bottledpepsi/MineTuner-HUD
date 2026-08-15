package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Lowest per-frame FPS reading observed during the current session (mod load,
 *  or last world join/disconnect — see MineTunerDataHolder — to now). */
public final class FpsMinStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.FPS_MIN;
    }

    @Override
    public String token() {
        return "fps_min";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedFpsMin(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawFpsMin(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 0;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }

    // No graph: see FpsAvgStat's doc — a running extreme only ever moves in one direction
    // within a session (min only ever falls, max only ever rises), so like the average it
    // wouldn't show meaningful frame-to-frame variance as a rolling graph.
    @Override
    public boolean supportsGraph() {
        return false;
    }

    // No user-configurable threshold — see MineTunerDataHolder.getSessionFpsColor()'s doc.
    @Override
    public boolean supportsThreshold() {
        return false;
    }

    // Still FPS-shaped (higher is better) even though this is the session's worst reading,
    // not a live value — a low Min FPS is exactly as bad as a low live FPS would be.
    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getSessionFpsColor(MineTunerDataHolder.getSessionMinFps());
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getSessionFpsColor(value);
    }
}
