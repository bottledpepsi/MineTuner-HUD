package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Your block XYZ position, floor-rounded to integers. */
public final class CoordsStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.COORDS;
    }

    @Override
    public String token() {
        return "coords";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedCoords();
    }
}
