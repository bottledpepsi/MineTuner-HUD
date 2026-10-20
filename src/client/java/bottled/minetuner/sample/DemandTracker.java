package bottled.minetuner.sample;

import bottled.minetuner.benchmark.BenchmarkSession;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.hud.TemplateEngine;

import java.util.EnumSet;
import java.util.Set;

/**
 * Works out which stats are needed right now: every stat visible in a list (classic mode,
 * including graph mode), every stat token of a template-mode list, and whatever a running
 * benchmark records. Recomputed once per client tick, never per frame.
 */
public final class DemandTracker {

    private DemandTracker() {
    }

    /**
     * Live demand from the current config and benchmark state.
     */
    public static Set<MineTunerConfig.Stat> currentDemand() {
        return compute(MineTunerConfig.getInstance(), BenchmarkSession.isRecording());
    }

    /**
     * Pure function of its inputs, for tests.
     */
    public static Set<MineTunerConfig.Stat> compute(MineTunerConfig cfg, boolean benchmarkRecording) {
        Set<MineTunerConfig.Stat> needed = EnumSet.noneOf(MineTunerConfig.Stat.class);
        // Lists count even while the overlay is hidden: graphs keep their history, and the
        // editor preview renders from the same data.
        for (MineTunerConfig.StatListConfig list : cfg.lists) addList(needed, list);
        if (benchmarkRecording) {
            needed.add(MineTunerConfig.Stat.CPU);
            needed.add(MineTunerConfig.Stat.GPU_USAGE);
        }
        return needed;
    }

    private static void addList(Set<MineTunerConfig.Stat> needed, MineTunerConfig.StatListConfig list) {
        if (list.useTemplate) {
            for (var line : TemplateEngine.getParsedLines(list)) {
                for (TemplateEngine.Token token : line) {
                    if (token instanceof TemplateEngine.StatToken st) needed.add(st.stat());
                }
            }
        } else {
            needed.addAll(list.getVisibleStats());
        }
    }
}
