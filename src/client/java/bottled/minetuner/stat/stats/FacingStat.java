package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Cardinal + intercardinal facing direction (full 8-way). */
public final class FacingStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.FACING;
    }

    @Override
    public String token() {
        return "facing";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedFacing();
    }
}
