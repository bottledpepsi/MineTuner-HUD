package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Your block XYZ position, floor-rounded to integers. */
public final class CoordsStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.COORDS; }
    @Override public String token() { return "coords"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedCoords(); }
}
