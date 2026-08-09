package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Your block-rounded Y coordinate on its own, primarily for Template Mode's {. */
public final class YStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.Y;
    }

    @Override
    public String token() {
        return "y";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedY();
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawY();
    }
}
