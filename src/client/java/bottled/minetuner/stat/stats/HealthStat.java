package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Current/max health. */
public final class HealthStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.HEALTH;
    }

    @Override
    public String token() {
        return "health";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedHealth();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawHealth();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getHealthHistory();
    }

    @Override
    public boolean supportsThreshold() {
        return true;
    }

    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public float defaultGoodMin() {
        return 75f;
    }

    @Override
    public float defaultWarnMin() {
        return 35f;
    }

    @Override
    public int color(MineTunerConfig.ThresholdSettings custom) {
        return MineTunerDataHolder.getHealthColor(custom);
    }

    /** History is stored as a percent of max health (see MineTunerDataHolder.updateFastMetrics's
     *  healthHistory.push call), so this compares percentValue directly against
     *  goodMin/warnMin on that same 0-100 scale — unlike MineTunerDataHolder.healthColorFor
     *  (used by color(custom) above), which takes a raw current-health value and
     *  converts it to a percent internally using today's maxHealth. The two can
     *  disagree slightly if maxHealth has changed since a given historical sample
     *  was recorded, which is expected: the graph reflects each sample's percent
     *  of max *at the time it was taken*, not renormalized against today's max. */
    @Override
    public int colorFor(float percentValue, MineTunerConfig.ThresholdSettings custom) {
        if (custom != null && custom.enabled) {
            if (percentValue >= custom.goodMin) return 0xFF55FF55;
            if (percentValue >= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (percentValue >= defaultGoodMin()) return 0xFF55FF55;
        if (percentValue >= defaultWarnMin()) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    /** Graph axis values are percentages here. */
    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "%";
    }
}
