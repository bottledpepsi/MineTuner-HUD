package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Whether the player currently has meaningful horizontal movement. Plain on/off text, no decimals/graph/threshold. */
public final class MovingStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.MOVING; }
    @Override public String token() { return "moving"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedMoving(); }
}
