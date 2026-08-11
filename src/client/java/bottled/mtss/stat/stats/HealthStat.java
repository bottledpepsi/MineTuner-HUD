package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Current/max health. */
public final class HealthStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.HEALTH;
    }

    @Override
    public String token() {
        return "health";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedHealth();
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawHealth();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MtssDataHolder.getHealthHistory();
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
    public int color(MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.getHealthColor(custom);
    }

    /** History is stored as a percent of max health (see MtssDataHolder.updateFastMetrics's
     *  healthHistory.push call), so this compares percentValue directly against
     *  goodMin/warnMin on that same 0-100 scale — unlike MtssDataHolder.healthColorFor
     *  (used by color(custom) above), which takes a raw current-health value and
     *  converts it to a percent internally using today's maxHealth. The two can
     *  disagree slightly if maxHealth has changed since a given historical sample
     *  was recorded, which is expected: the graph reflects each sample's percent
     *  of max *at the time it was taken*, not renormalized against today's max. */
    @Override
    public int colorFor(float percentValue, MtssConfig.ThresholdSettings custom) {
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
