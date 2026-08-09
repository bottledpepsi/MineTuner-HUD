package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Food saturation. */
public final class SaturationStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.SATURATION;
    }

    @Override
    public String token() {
        return "saturation";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedSaturation(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawSaturation(decimals);
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
