package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pins FrametimeStat's threshold direction and defaults.
 *
 * <p>Regression guard: higherIsBetter() used to return true (inverted — frametime is
 * lower-is-better), which made ThresholdBands.normalize pull the seeded warn cutoff
 * down onto good on every config load, leaving custom thresholds with no reachable
 * yellow band. The declared defaults also used to disagree (8.33/16.67) with the
 * actually-used built-in bands (16.7/33.3).
 */
class FrametimeStatTest {

    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int RED = 0xFFFF5555;

    @AfterEach
    void restoreSharedStatic() {
        MineTunerDataHolder.tickRate = 20.0f;
    }

    @Test
    void frametimeIsLowerIsBetter() {
        assertFalse(new FrametimeStat().higherIsBetter());
    }

    @Test
    void declaredDefaultsMatchTheBuiltInBands() {
        FrametimeStat stat = new FrametimeStat();
        assertEquals(16.7f, stat.defaultGoodMin());
        assertEquals(33.3f, stat.defaultWarnMin());
    }

    @Test
    void builtInBandsAreTheFps60And30Equivalents() {
        FrametimeStat stat = new FrametimeStat();
        // 1000/60 ≈ 16.7ms green, up to 1000/30 ≈ 33.3ms yellow, beyond that red.
        assertEquals(GREEN, stat.colorFor(10f, null));
        assertEquals(GREEN, stat.colorFor(16.7f, null));
        assertEquals(YELLOW, stat.colorFor(20f, null));
        assertEquals(YELLOW, stat.colorFor(33.3f, null));
        assertEquals(RED, stat.colorFor(40f, null));
    }

    @Test
    void customThresholdsAreHonoredLowerIsBetter() {
        FrametimeStat stat = new FrametimeStat();
        MineTunerConfig.ThresholdSettings custom =
                new MineTunerConfig.ThresholdSettings(true, 16.7f, 33.3f);
        assertEquals(GREEN, stat.colorFor(5f, custom));
        assertEquals(YELLOW, stat.colorFor(25f, custom));
        assertEquals(RED, stat.colorFor(50f, custom));
    }

    @Test
    void seededCustomThresholdsSurviveNormalizationWithoutCollapsing() {
        // StatListConfig.backFill() normalizes every threshold against the stat's
        // declared direction. With the old inverted higherIsBetter() this collapsed
        // warn onto good (16.7 -> 33.3 was "invalid" for higher-is-better).
        MineTunerConfig.StatListConfig cfg = new MineTunerConfig.StatListConfig(0);
        cfg.backFill();
        MineTunerConfig.ThresholdSettings seeded = cfg.statThresholds.get("FRAMETIME");
        assertEquals(16.7f, seeded.goodMin);
        assertEquals(33.3f, seeded.warnMin);
    }
}
