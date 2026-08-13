package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Signed vertical velocity in blocks/second. */
public final class VerticalSpeedStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.VERTICAL_SPEED;
    }

    @Override
    public String token() {
        return "vspeed";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedVerticalSpeed(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawVerticalSpeed(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 2;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
