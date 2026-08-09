package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Horizontal (XZ-plane) distance in blocks from the world's shared spawn point. */
public final class DistanceFromSpawnStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.DISTANCE_FROM_SPAWN;
    }

    @Override
    public String token() {
        return "distance";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedDistanceFromSpawn(decimals);
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawDistanceFromSpawn(decimals);
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
