package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Current weather state. */
public final class WeatherStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.WEATHER;
    }

    @Override
    public String token() {
        return "weather";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedWeather();
    }
}
