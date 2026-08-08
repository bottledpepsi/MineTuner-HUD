package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Name of the block or entity currently under the crosshair. Empty (line skipped) when nothing is targeted. */
public final class LookingAtStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.LOOKING_AT; }
    @Override public String token() { return "lookingat"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedLookingAt(); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawLookingAt(); }
}
