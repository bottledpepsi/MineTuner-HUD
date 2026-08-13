package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Selected hotbar slot, shown 1-indexed (matching what's printed on the hotbar
 *  itself and the 1-9 number keys used to select it, rather than the internal
 *  0-8 slot index). */
public final class SelectedSlotStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.SELECTED_SLOT;
    }

    @Override
    public String token() {
        return "slot";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedSelectedSlot();
    }
}
