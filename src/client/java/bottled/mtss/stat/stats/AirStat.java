package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Remaining breath/air supply. */
public final class AirStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.AIR;
    }

    @Override
    public String token() {
        return "air";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedAir();
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawAir();
    }

    @Override
    public int color(MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.getAirColor();
    }

    @Override
    public int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.airColorFor(value, MtssDataHolder.maxAir);
    }
}
