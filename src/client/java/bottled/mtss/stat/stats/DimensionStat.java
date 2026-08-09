package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Current dimension id, e.g. */
public final class DimensionStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.DIMENSION;
    }

    @Override
    public String token() {
        return "dimension";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedDimension();
    }
}
