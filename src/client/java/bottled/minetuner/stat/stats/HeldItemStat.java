package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Display name of the item currently held in the main hand, or "-" when. */
public final class HeldItemStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.HELD_ITEM;
    }

    @Override
    public String token() {
        return "helditem";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedHeldItem();
    }
}
