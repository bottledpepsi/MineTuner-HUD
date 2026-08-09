package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Biome at your current position. */
public final class BiomeStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.BIOME;
    }

    @Override
    public String token() {
        return "biome";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedBiome();
    }
}
