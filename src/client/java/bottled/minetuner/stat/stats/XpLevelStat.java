package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Experience level. */
public final class XpLevelStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.XP_LEVEL;
    }

    @Override
    public String token() {
        return "xplevel";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedXpLevel();
    }
}
