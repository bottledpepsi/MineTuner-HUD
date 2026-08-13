package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Current dimension id, e.g. "overworld", "the_nether", "the_end", or a modded
 *  dimension's path segment (the part after the namespace, e.g. "mymod:custom_dim"
 *  shows as "custom_dim"). */
public final class DimensionStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.DIMENSION;
    }

    @Override
    public String token() {
        return "dimension";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedDimension();
    }
}
