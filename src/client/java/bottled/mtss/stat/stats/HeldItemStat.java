package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Display name of the item currently held in the main hand, or "-" when. */
public final class HeldItemStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.HELD_ITEM;
    }

    @Override
    public String token() {
        return "helditem";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedHeldItem();
    }
}
