package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Cardinal + intercardinal facing direction (full 8-way). */
public final class FacingStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.FACING; }
    @Override public String token() { return "facing"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedFacing(); }
}
