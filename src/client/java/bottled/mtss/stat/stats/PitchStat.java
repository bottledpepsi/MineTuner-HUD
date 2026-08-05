package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * Raw player pitch in degrees: -90 (straight up) to 90 (straight down),
 * unmodified from {@code Entity.getXRot()}. No graph/threshold — orientation
 * isn't a performance metric with a good/bad direction.
 */
public final class PitchStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.PITCH; }
    @Override public String token() { return "pitch"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedPitch(decimals); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawPitch(decimals); }

    @Override public boolean supportsDecimals() { return true; }
}
