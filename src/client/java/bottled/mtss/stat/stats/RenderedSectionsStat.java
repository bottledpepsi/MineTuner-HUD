package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Number of chunk sections in the render pass, pulled from LevelRenderer via MtssRenderer. */
public final class RenderedSectionsStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.RENDERED_SECTIONS; }
    @Override public String token() { return "rendered"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedRendered(); }
}
