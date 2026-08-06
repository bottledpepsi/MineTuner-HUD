package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Whether your current block has a clear path to the sky (useful for spawn-proofing / mob-spawning checks). */
public final class CanSeeSkyStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.CAN_SEE_SKY; }
    @Override public String token() { return "canseesky"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedCanSeeSky(); }
}
