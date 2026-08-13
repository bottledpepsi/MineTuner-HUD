package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Food saturation. */
public final class SaturationStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.SATURATION;
    }

    @Override
    public String token() {
        return "saturation";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedSaturation(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawSaturation(decimals);
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
