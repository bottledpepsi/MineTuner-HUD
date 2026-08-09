package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** JVM heap usage (used / max MB), color-coded by fill percentage. */
public final class MemoryStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.MEMORY;
    }

    @Override
    public String token() {
        return "mem";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedMem();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MtssDataHolder.getMemHistory();
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
    public float defaultGoodMin() {
        return 60f;
    }

    @Override
    public float defaultWarnMin() {
        return 85f;
    }

    @Override
    public int color(MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.getMemColor(custom);
    }

    @Override
    public int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.memColorForPercent(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "%";
    }
}
