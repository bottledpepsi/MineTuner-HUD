package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Signed vertical velocity in blocks/second. */
public final class VerticalSpeedStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.VERTICAL_SPEED;
    }

    @Override
    public String token() {
        return "vspeed";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedVerticalSpeed(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawVerticalSpeed(decimals);
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
