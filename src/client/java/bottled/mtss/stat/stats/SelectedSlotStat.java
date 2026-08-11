package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Selected hotbar slot, shown 1-indexed (matching what's printed on the hotbar
 *  itself and the 1-9 number keys used to select it, rather than the internal
 *  0-8 slot index). */
public final class SelectedSlotStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.SELECTED_SLOT;
    }

    @Override
    public String token() {
        return "slot";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedSelectedSlot();
    }
}
