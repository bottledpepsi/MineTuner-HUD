package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** World difficulty (peaceful/easy/normal/hard). */
public final class DifficultyStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.DIFFICULTY;
    }

    @Override
    public String token() {
        return "difficulty";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedDifficulty();
    }
}
