package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Remaining breath/air supply. */
public final class AirStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.AIR;
    }

    @Override
    public String token() {
        return "air";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedAir();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawAir();
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getAirColor();
    }

    @Override
    public int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.airColorFor(value, MineTunerDataHolder.maxAir);
    }
}
