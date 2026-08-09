package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Experience level. */
public final class XpLevelStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.XP_LEVEL;
    }

    @Override
    public String token() {
        return "xplevel";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedXpLevel();
    }
}
