package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Horizontal (XZ-plane) distance in blocks from the world's shared spawn point. */
public final class DistanceFromSpawnStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.DISTANCE_FROM_SPAWN;
    }

    @Override
    public String token() {
        return "distance";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedDistanceFromSpawn(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawDistanceFromSpawn(decimals);
    }

    @Override
    public int defaultDecimals() {
        return 0;
    }

    @Override
    public boolean supportsDecimals() {
        return true;
    }
}
