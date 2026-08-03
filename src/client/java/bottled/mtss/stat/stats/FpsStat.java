package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Client frames per second. Higher is better; color-coded green/yellow/red. */
public final class FpsStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.FPS; }
    @Override public String token() { return "fps"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedFps(); }

    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getFpsHistory(); }

    @Override public boolean supportsThreshold() { return true; }
    @Override public boolean higherIsBetter() { return true; }
    @Override public float defaultGoodMin() { return 60f; }
    @Override public float defaultWarnMin() { return 30f; }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getFpsColor(custom); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.fpsColorFor(value, custom); }
}
