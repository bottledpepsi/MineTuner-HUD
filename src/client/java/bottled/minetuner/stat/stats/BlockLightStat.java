package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Raw block (torch-source) light level (0-15) at your current block, separate. */
public final class BlockLightStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.BLOCK_LIGHT;
    }

    @Override
    public String token() {
        return "blocklight";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedBlockLight();
    }
}
