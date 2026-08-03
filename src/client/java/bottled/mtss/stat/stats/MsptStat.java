package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * Milliseconds per tick. Singleplayer/LAN only — format() returns "" on
 * remote servers, same as the rest of the mod's "unavailable" convention.
 * <p>
 * MSPT shares TPS's color thresholds rather than having its own — they're
 * both readings of the same underlying tick-time signal, so a custom TPS
 * threshold on a list also recolors that list's MSPT rows/graph.
 */
public final class MsptStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.MSPT; }
    @Override public String token() { return "mspt"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedMspt(decimals); }

    @Override public boolean supportsDecimals() { return true; }
    @Override public boolean supportsGraph() { return true; }
    @Override public float[] history() { return MtssDataHolder.getMsptHistory(); }

    // MSPT isn't in THRESHOLD_STATS (no entry of its own) — it borrows TPS's.
    @Override public boolean supportsThreshold() { return false; }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getTpsColor(custom); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) { return MtssDataHolder.tpsColorFor(value, custom); }

    @Override public String formatAxisValue(float value) { return String.format("%.1f", value); }
}
