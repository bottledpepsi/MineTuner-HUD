package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Raw sky light level (0-15) at your current block, separate from the combined max-of-both {@link LightLevelStat}. */
public final class SkyLightStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.SKY_LIGHT; }
    @Override public String token() { return "skylight"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedSkyLight(); }
}
