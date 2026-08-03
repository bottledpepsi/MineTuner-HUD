package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Loaded chunk count. */
public final class ChunksStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.CHUNKS; }
    @Override public String token() { return "chunks"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedChunks(); }
}
