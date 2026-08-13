package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Biome at your current position. */
public final class BiomeStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.BIOME;
    }

    @Override
    public String token() {
        return "biome";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedBiome();
    }
}
