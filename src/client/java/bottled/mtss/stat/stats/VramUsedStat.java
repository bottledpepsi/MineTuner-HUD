package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** VRAM used, formatted as "used/maxMB" (same used/max shape { MemoryStat} uses. */
public final class VramUsedStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.VRAM_USED;
    }

    @Override
    public String token() {
        return "vram_used";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedVramUsed();
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawVramUsed();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MtssDataHolder.getVramUsedHistory();
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "MB";
    }
}
