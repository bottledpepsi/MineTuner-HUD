package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

public final class FrametimeStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.FRAMETIME;
    }

    @Override
    public String token() {
        return "frametime";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedFrametime(decimals);
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MtssDataHolder.getFrametimeHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public float defaultGoodMin() {
        return 8.33f;
    }

    @Override
    public float defaultWarnMin() {
        return 16.67f;
    }

    @Override
    public int color(MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.getFrametimeColor(custom);
    }

    @Override
    public int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.frametimeColorFor(value, custom);
    }
}
