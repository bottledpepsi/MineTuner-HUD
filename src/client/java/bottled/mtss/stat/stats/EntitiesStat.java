package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Loaded entity count in your dimension. Plain text, no decimals/graph/threshold — the minimal shape for a stat. */
public final class EntitiesStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.ENTITIES; }
    @Override public String token() { return "entities"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedEntities(); }
}
