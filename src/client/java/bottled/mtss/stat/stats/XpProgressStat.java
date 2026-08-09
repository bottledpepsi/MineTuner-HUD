package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Progress toward the next XP level, shown as a percent (0-100). */
public final class XpProgressStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.XP_PROGRESS;
    }

    @Override
    public String token() {
        return "xpprogress";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedXpProgress(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawXpProgress(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 0;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
