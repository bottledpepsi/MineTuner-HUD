package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Raw sky light level (0-15) at your current block, separate from the combined. */
public final class SkyLightStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.SKY_LIGHT;
    }

    @Override
    public String token() {
        return "skylight";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedSkyLight();
    }
}
