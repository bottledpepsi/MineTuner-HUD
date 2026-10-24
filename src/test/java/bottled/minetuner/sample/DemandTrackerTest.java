package bottled.minetuner.sample;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.hud.TemplateEngine;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The demand contract: which stats get sampled is exactly the union of every classic
 * list's visible stats, every template list's parsed tokens, and the benchmark's own
 * needs while recording — and nothing else. Also pins the allocation-free collector's
 * membership equality with the order-sensitive getVisibleStats().
 */
class DemandTrackerTest {

    @Test
    void classicListDemandIsItsVisibleStats() {
        MineTunerConfig.StatListConfig list = new MineTunerConfig.StatListConfig(0);
        list.setEnabled(MineTunerConfig.Stat.FPS, true);
        list.setEnabled(MineTunerConfig.Stat.TPS, true);

        var demand = DemandTracker.compute(configWith(list), false);
        assertTrue(demand.contains(MineTunerConfig.Stat.FPS));
        assertTrue(demand.contains(MineTunerConfig.Stat.TPS));
        assertFalse(demand.contains(MineTunerConfig.Stat.BIOME));
    }

    @Test
    void templateListDemandIsItsTokens() {
        MineTunerConfig.StatListConfig list = new MineTunerConfig.StatListConfig(0);
        list.useTemplate = true;
        list.templateLines.add("FPS {fps} vs {ping}");

        var demand = DemandTracker.compute(configWith(list), false);
        assertTrue(demand.contains(MineTunerConfig.Stat.FPS));
        assertTrue(demand.contains(MineTunerConfig.Stat.PING));
        assertFalse(demand.contains(MineTunerConfig.Stat.TPS));
    }

    @Test
    void recordingBenchmarkAddsItsOwnStats() {
        var demand = DemandTracker.compute(configWith(new MineTunerConfig.StatListConfig(0)), true);
        assertTrue(demand.contains(MineTunerConfig.Stat.CPU));
        assertTrue(demand.contains(MineTunerConfig.Stat.GPU_USAGE));
    }

    @Test
    void collectVisibleStatsMembershipMatchesGetVisibleStats() {
        MineTunerConfig.StatListConfig list = new MineTunerConfig.StatListConfig(0);
        // Enable an arbitrary, order-scattered selection.
        list.statOrder.remove(MineTunerConfig.Stat.HEALTH.name());
        list.statOrder.add(2, MineTunerConfig.Stat.HEALTH.name());
        for (MineTunerConfig.Stat s : List.of(MineTunerConfig.Stat.HEALTH, MineTunerConfig.Stat.FPS,
                MineTunerConfig.Stat.DIMENSION, MineTunerConfig.Stat.GPU_TEMP)) {
            list.setEnabled(s, true);
        }

        EnumSet<MineTunerConfig.Stat> collected = EnumSet.noneOf(MineTunerConfig.Stat.class);
        list.collectVisibleStats(collected);
        assertEquals(EnumSet.copyOf(list.getVisibleStats()), collected,
                "the allocation-free collector must be membership-identical to getVisibleStats");
    }

    private static MineTunerConfig configWith(MineTunerConfig.StatListConfig list) {
        MineTunerConfig cfg = new MineTunerConfig();
        cfg.lists.add(list);
        return cfg;
    }

    // TemplateEngine must not warn into a real log for the synthetic token above.
    static {
        TemplateEngine.invalidate(0);
    }
}
