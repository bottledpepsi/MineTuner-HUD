package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** World difficulty (peaceful/easy/normal/hard). */
public final class DifficultyStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.DIFFICULTY; }
    @Override public String token() { return "difficulty"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedDifficulty(); }
}
