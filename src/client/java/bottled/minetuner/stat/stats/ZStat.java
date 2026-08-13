package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Your block-rounded Z coordinate on its own, primarily for Template Mode's
 *  {@code {z}} token (as opposed to {@link CoordsStat}'s combined "x, y, z" line,
 *  which can't be split apart inside a template). */
public final class ZStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.Z;
    }

    @Override
    public String token() {
        return "z";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedZ();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawZ();
    }
}
