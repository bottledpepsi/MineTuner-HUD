package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Current dimension id, e.g. "overworld", "the_nether", "the_end", or a modded
 *  dimension's path segment (the part after the namespace, e.g. "mymod:custom_dim"
 *  shows as "custom_dim"). */
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
