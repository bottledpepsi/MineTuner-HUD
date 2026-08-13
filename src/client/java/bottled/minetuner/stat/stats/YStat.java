package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Your block-rounded Y coordinate on its own, primarily for Template Mode's
 *  {@code {y}} token (as opposed to {@link CoordsStat}'s combined "x, y, z" line,
 *  which can't be split apart inside a template). */
public final class YStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.Y;
    }

    @Override
    public String token() {
        return "y";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedY();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawY();
    }
}
