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

    /** Scratch set reused between ticks: {@code currentDemand} is consumed within a
     *  single SamplingDriver tick (its result is never retained), so one shared
     *  EnumSet removes the per-tick EnumSet + per-list ArrayList churn. EnumSet
     *  iterates in enum-declaration order, which is fine — demand is a membership
     *  test, not an ordering. Tick-thread only. */
    private static final EnumSet<MineTunerConfig.Stat> SCRATCH =
            EnumSet.noneOf(MineTunerConfig.Stat.class);

    private DemandTracker() {
    }

    /**
     * Live demand from the current config and benchmark state. The returned set is a
     * shared scratch view valid until the next call — read it, don't retain it (tests
     * and other long-lived consumers use the pure {@link #compute} instead).
     */
    public static Set<MineTunerConfig.Stat> currentDemand() {
        SCRATCH.clear();
        fill(SCRATCH, MineTunerConfig.getInstance(), BenchmarkSession.isRecording());
        return SCRATCH;
    }

    /**
     * Pure function of its inputs (fresh set, safe to retain), for tests.
     */
    public static Set<MineTunerConfig.Stat> compute(MineTunerConfig cfg, boolean benchmarkRecording) {
        EnumSet<MineTunerConfig.Stat> needed = EnumSet.noneOf(MineTunerConfig.Stat.class);
        fill(needed, cfg, benchmarkRecording);
        return needed;
    }

    private static void fill(EnumSet<MineTunerConfig.Stat> out, MineTunerConfig cfg, boolean benchmarkRecording) {
        // Lists count even while the overlay is hidden: graphs keep their history, and the
        // editor preview renders from the same data.
        for (MineTunerConfig.StatListConfig list : cfg.lists) addList(out, list);
        if (benchmarkRecording) {
            out.add(MineTunerConfig.Stat.CPU);
            out.add(MineTunerConfig.Stat.GPU_USAGE);
        }
    }

    private static void addList(EnumSet<MineTunerConfig.Stat> out, MineTunerConfig.StatListConfig list) {
        if (list.useTemplate) {
            for (var line : TemplateEngine.getParsedLines(list)) {
                for (TemplateEngine.Token token : line) {
                    if (token instanceof TemplateEngine.StatToken st) out.add(st.stat());
                }
            }
        } else {
            // Order-free, allocation-free collector — see StatListConfig's doc.
            list.collectVisibleStats(out);
        }
    }
}
