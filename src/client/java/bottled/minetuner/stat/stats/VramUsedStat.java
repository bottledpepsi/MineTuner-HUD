package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** VRAM used, formatted as "used/maxMB" (same used/max shape {@link MemoryStat} uses
 *  for JVM heap, just against the GPU's reported total VRAM instead). */
public final class VramUsedStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.VRAM_USED;
    }

    @Override
    public String token() {
        return "vram_used";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedVramUsed();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawVramUsed();
    }

    @Override
    public boolean supportsGraph() {
        return true;
    }

    @Override
    public float[] history() {
        return MineTunerDataHolder.getVramUsedHistory();
    }

    @Override
    public String formatAxisValue(float value) {
        return Math.round(value) + "MB";
    }
}
