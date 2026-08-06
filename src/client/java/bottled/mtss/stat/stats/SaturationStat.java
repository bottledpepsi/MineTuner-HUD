package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * Food saturation — the hidden buffer that's consumed before the visible
 * hunger bar drops. No graph/threshold: it's a supporting value for Hunger
 * rather than a headline performance/vitals metric on its own.
 */
public final class SaturationStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.SATURATION; }
    @Override public String token() { return "saturation"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedSaturation(decimals); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawSaturation(decimals); }

    @Override public boolean supportsDecimals() { return true; }
}
