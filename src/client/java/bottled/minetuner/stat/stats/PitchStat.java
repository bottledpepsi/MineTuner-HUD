package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Raw player pitch in degrees. */
public final class PitchStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.PITCH;
    }

    @Override
    public String token() {
        return "pitch";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedPitch(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawPitch(decimals);
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
