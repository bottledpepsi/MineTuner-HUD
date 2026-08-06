package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Selected hotbar slot, shown 1-indexed (matching what's printed on the hotbar itself, not the 0-indexed internal slot id). */
public final class SelectedSlotStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.SELECTED_SLOT; }
    @Override public String token() { return "slot"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedSelectedSlot(); }
}
