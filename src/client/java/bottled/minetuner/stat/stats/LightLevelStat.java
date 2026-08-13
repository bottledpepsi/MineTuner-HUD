package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Local light level at your block position. */
public final class LightLevelStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.LIGHT_LEVEL;
    }

    @Override
    public String token() {
        return "light";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedLight();
    }
}
