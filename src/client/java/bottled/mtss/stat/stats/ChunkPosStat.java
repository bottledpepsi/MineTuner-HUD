package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** The chunk coordinates (block XZ &gt;&gt; 4) containing your current position. */
public final class ChunkPosStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.CHUNK_POS; }
    @Override public String token() { return "chunkpos"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedChunkPos(); }
}
