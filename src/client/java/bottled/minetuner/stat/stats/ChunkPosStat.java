package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** The chunk coordinates (block XZ &gt;&gt; 4) the player currently stands in —
 *  Minecraft's standard block-to-chunk conversion, since chunks are 16x16 blocks. */
public final class ChunkPosStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.CHUNK_POS;
    }

    @Override
    public String token() {
        return "chunkpos";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedChunkPos();
    }
}
