package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Raw block (torch-source) light level (0-15) at your current block, separate from the combined max-of-both {@link LightLevelStat}. */
public final class BlockLightStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.BLOCK_LIGHT; }
    @Override public String token() { return "blocklight"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedBlockLight(); }
}
