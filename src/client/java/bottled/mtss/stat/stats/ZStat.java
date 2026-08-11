package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Your block-rounded Z coordinate on its own, primarily for Template Mode's
 *  {@code {z}} token (as opposed to {@link CoordsStat}'s combined "x, y, z" line,
 *  which can't be split apart inside a template). */
public final class ZStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.Z;
    }

    @Override
    public String token() {
        return "z";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedZ();
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawZ();
    }
}
