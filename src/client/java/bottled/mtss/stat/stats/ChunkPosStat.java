package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** The chunk coordinates (block XZ &gt;&gt; 4) the player currently stands in —
 *  Minecraft's standard block-to-chunk conversion, since chunks are 16x16 blocks. */
public final class ChunkPosStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.CHUNK_POS;
    }

    @Override
    public String token() {
        return "chunkpos";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedChunkPos();
    }
}
