package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * Your block-rounded X coordinate on its own, primarily for Template Mode's
 * {@code {x}} token so a line can be built like {@code "{x} {y} {z}"} instead
 * of the fixed "XYZ: x / y / z" layout {@link CoordsStat} gives.
 */
public final class XStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.X; }
    @Override public String token() { return "x"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedX(); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawX(); }
}
