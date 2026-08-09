package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Raw player yaw in degrees, normalized to [0, 360). */
public final class YawStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.YAW;
    }

    @Override
    public String token() {
        return "yaw";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedYaw(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawYaw(decimals);
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
