package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Your block-rounded X coordinate on its own, primarily for Template Mode's
 *  {@code {x}} token (as opposed to {@link CoordsStat}'s combined "x, y, z" line,
 *  which can't be split apart inside a template). */
public final class XStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.X;
    }

    @Override
    public String token() {
        return "x";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedX();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawX();
    }
}
