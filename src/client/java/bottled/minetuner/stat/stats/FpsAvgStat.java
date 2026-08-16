package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Running average FPS across the current session (mod load, or last world
 *  join/disconnect — see MineTunerDataHolder's session-aggregate fields — to now). */
public final class FpsAvgStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.FPS_AVG;
    }

    @Override
    public String token() {
        return "fps_avg";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedFpsAvg(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawFpsAvg(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 0;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }

    // No graph: a session average changes slowly and smoothly by definition (it's a running
    // mean over potentially tens of thousands of samples), so a rolling history graph of it
    // would be a near-flat line carrying little information — unlike live FPS/Frametime,
    // whose whole value as a graph comes from frame-to-frame variance.
    @Override
    public boolean supportsGraph() {
        return false;
    }

    // No user-configurable threshold either — see MineTunerDataHolder's getSessionFpsColor()
    // doc for the reasoning. color()/colorFor() below still apply FPS's fixed default bands
    // as a non-editable visual cue.
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
        return MineTunerDataHolder.getSessionFpsColor(MineTunerDataHolder.getSessionAvgFps());
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getSessionFpsColor(value);
    }
}
