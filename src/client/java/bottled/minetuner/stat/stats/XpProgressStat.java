package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Progress toward the next XP level, shown as a percent (0-100). */
public final class XpProgressStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.XP_PROGRESS;
    }

    @Override
    public String token() {
        return "xpprogress";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedXpProgress(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawXpProgress(decimals);
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
