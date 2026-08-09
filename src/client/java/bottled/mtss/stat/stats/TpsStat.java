package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Server ticks per second. */
public final class TpsStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.TPS;
    }

    @Override
    public String token() {
        return "tps";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedTps(decimals);
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
        return MtssDataHolder.getTpsHistory();
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
    public float thresholdStep() {
        return 0.5f;
    }

    @Override
    public float defaultGoodMin() {
        return 18f;
    }

    @Override
    public float defaultWarnMin() {
        return 14f;
    }

    @Override
    public int color(MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.getTpsColor(custom);
    }

    @Override
    public int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.tpsColorFor(value, custom);
    }

    @Override
    public String formatAxisValue(float value) {
        return String.format("%.1f", value);
    }
}
