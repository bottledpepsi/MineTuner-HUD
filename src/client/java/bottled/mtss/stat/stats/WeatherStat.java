package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Current weather state. */
public final class WeatherStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.WEATHER;
    }

    @Override
    public String token() {
        return "weather";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedWeather();
    }
}
