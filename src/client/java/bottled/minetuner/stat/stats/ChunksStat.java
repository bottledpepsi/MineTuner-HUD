package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Loaded chunk count. */
public final class ChunksStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.CHUNKS;
    }

    @Override
    public String token() {
        return "chunks";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedChunks();
    }
}
