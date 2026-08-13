package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Number of chunk sections in the render pass, pulled from LevelRenderer by. */
public final class RenderedSectionsStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.RENDERED_SECTIONS;
    }

    @Override
    public String token() {
        return "rendered";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedRendered();
    }
}
