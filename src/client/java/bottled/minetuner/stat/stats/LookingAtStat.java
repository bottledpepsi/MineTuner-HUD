package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Name of the block or entity currently under the crosshair. */
public final class LookingAtStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.LOOKING_AT;
    }

    @Override
    public String token() {
        return "lookingat";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedLookingAt();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawLookingAt();
    }
}
