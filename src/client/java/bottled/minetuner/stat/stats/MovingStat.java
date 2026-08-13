package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Whether the player currently has meaningful horizontal movement. */
public final class MovingStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.MOVING;
    }

    @Override
    public String token() {
        return "moving";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedMoving();
    }
}
