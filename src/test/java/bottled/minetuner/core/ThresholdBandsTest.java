package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThresholdBandsTest {

    private static void check(float good, float warn, boolean higher, float expGood, float expWarn) {
        ThresholdBands.Cutoffs c = ThresholdBands.normalize(good, warn, higher);
        assertEquals(expGood, c.good(), 0f, "good for " + good + "/" + warn + "/" + higher);
        assertEquals(expWarn, c.warn(), 0f, "warn for " + good + "/" + warn + "/" + higher);
    }

    @Test
    void validValuesAreUntouched() {
        check(18f, 14f, true, 18f, 14f);
        check(80f, 150f, false, 80f, 150f);
        check(10f, 10f, true, 10f, 10f);
        check(10f, 10f, false, 10f, 10f);
        check(0f, 0f, true, 0f, 0f);
    }

    @Test
    void higherIsBetterWarnAboveGoodIsPulledDown() {
        check(14f, 18f, true, 14f, 14f);
    }

    @Test
    void lowerIsBetterWarnBelowGoodIsPulledUp() {
        check(150f, 80f, false, 150f, 150f);
    }

    @Test
    void negativesAndNonFiniteBecomeZero() {
        check(-5f, -1f, true, 0f, 0f);
        check(-5f, 3f, false, 0f, 3f);
        check(Float.NaN, Float.POSITIVE_INFINITY, true, 0f, 0f);
    }
}
