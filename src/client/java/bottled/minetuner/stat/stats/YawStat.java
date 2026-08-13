package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Raw player yaw in degrees, normalized to [0, 360). */
public final class YawStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.YAW;
    }

    @Override
    public String token() {
        return "yaw";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedYaw(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawYaw(decimals);
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
