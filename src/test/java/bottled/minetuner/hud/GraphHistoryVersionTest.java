package bottled.minetuner.hud;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;
import bottled.minetuner.stat.StatRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The version contract behind the HUD's graph-history cache: a graphable stat's
 * {@code historyVersion} must change whenever its buffer contents change and stay
 * stable otherwise, and must report -1 ("never cache") for stats without a
 * versioned buffer. A wrong value here would either break the cache's correctness
 * (stale graphs) or disable it entirely.
 */
class GraphHistoryVersionTest {

    @AfterEach
    void restoreSharedStatics() {
        MineTunerDataHolder.cpuPercent = -1.0;
        MineTunerDataHolder.mspt = -1f;
    }

    @Test
    void statsWithoutAVersionedBufferAreNeverCacheable() {
        assertEquals(-1L, MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.BIOME));
        assertEquals(-1L, MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.FPS_AVG));
        assertEquals(-1L, MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.GAME_MODE));
    }

    @Test
    void versionAdvancesWhenTheBufferIsPushed() {
        long before = MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.FPS);
        MineTunerDataHolder.updateFastMetrics(); // unconditionally pushes the fps history
        long after = MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.FPS);
        assertTrue(after > before, "an fps push must advance the version");
    }

    @Test
    void guardedBufferDoesNotAdvanceWhenTheStatIsUnavailable() {
        MineTunerDataHolder.cpuPercent = -1.0; // CPU unavailable: cpuHistory push is skipped
        long before = MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.CPU);
        MineTunerDataHolder.updateFastMetrics();
        long after = MineTunerDataHolder.historyVersion(MineTunerConfig.Stat.CPU);
        assertEquals(before, after, "a skipped push must not advance the version");
    }

    @Test
    void everyGraphableStatWithRealHistoryIsVersioned() {
        for (MineTunerConfig.Stat stat : MineTunerConfig.GRAPHABLE_STATS) {
            StatDefinition def = StatRegistry.get(stat);
            if (def.history().length == 0) continue; // nothing to cache (no history override)
            assertTrue(MineTunerDataHolder.historyVersion(stat) >= 0,
                    stat + " is graphable with a non-empty history() but reports -1;"
                            + " the graph cache would silently never cache for it");
        }
    }
}
