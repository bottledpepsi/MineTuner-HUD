package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Loaded entity count in your dimension. */
public final class EntitiesStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.ENTITIES;
    }

    @Override
    public String token() {
        return "entities";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedEntities();
    }
}
