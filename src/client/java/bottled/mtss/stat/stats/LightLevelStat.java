package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Local light level at your block position. */
public final class LightLevelStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.LIGHT_LEVEL;
    }

    @Override
    public String token() {
        return "light";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedLight();
    }
}
