package bottled.minetuner.stat;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.config.MineTunerConfig.ThresholdSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registry-wide threshold invariants. This is the test that would have caught
 * FrametimeStat's inverted higherIsBetter(): for every threshold-capable stat,
 * the color ladder must actually respect the stat's declared direction, and the
 * seeded defaults must survive ThresholdBands normalization unchanged (a direction
 * mismatch collapses the warn band onto good).
 */
class StatThresholdConsistencyTest {

    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int RED = 0xFFFF5555;

    /** good=10/warn=20 keeps both bands reachable for lower-is-better stats. */
    private static ThresholdSettings customLower() {
        return new ThresholdSettings(true, 10f, 20f);
    }

    /** good=20/warn=10 (warn &le; good) keeps both bands reachable for
     *  higher-is-better stats. */
    private static ThresholdSettings customHigher() {
        return new ThresholdSettings(true, 20f, 10f);
    }

    @Test
    void everyThresholdStatHasARegistryDefinition() {
        assertFalse(MineTunerConfig.THRESHOLD_STATS.isEmpty());
        for (MineTunerConfig.Stat stat : MineTunerConfig.THRESHOLD_STATS) {
            StatDefinition def = StatRegistry.get(stat); // throws if unregistered
            assertTrue(def.supportsThreshold(), stat + " must support thresholds");
        }
    }

    @Test
    void colorLadderRespectsDeclaredDirection() {
        for (MineTunerConfig.Stat stat : MineTunerConfig.THRESHOLD_STATS) {
            StatDefinition def = StatRegistry.get(stat);
            if (def.higherIsBetter()) {
                ThresholdSettings custom = customHigher();
                assertEquals(GREEN, def.colorFor(25f, custom), stat + " above good must be green");
                assertEquals(YELLOW, def.colorFor(15f, custom), stat + " between bands must be yellow");
                assertEquals(RED, def.colorFor(5f, custom), stat + " below warn must be red");
            } else {
                ThresholdSettings custom = customLower();
                assertEquals(GREEN, def.colorFor(5f, custom), stat + " below good must be green");
                assertEquals(YELLOW, def.colorFor(15f, custom), stat + " between bands must be yellow");
                assertEquals(RED, def.colorFor(25f, custom), stat + " above warn must be red");
            }
        }
    }

    @Test
    void declaredDefaultsAreValidForTheirDirectionAndSurviveNormalization() {
        for (MineTunerConfig.Stat stat : MineTunerConfig.THRESHOLD_STATS) {
            StatDefinition def = StatRegistry.get(stat);
            ThresholdSettings seeded = new ThresholdSettings(false,
                    def.defaultGoodMin(), def.defaultWarnMin());
            seeded.normalize(def.higherIsBetter());
            assertEquals(def.defaultGoodMin(), seeded.goodMin,
                    stat + " good cutoff must not move during normalization");
            assertEquals(def.defaultWarnMin(), seeded.warnMin,
                    stat + " warn cutoff must not collapse onto good during normalization"
                            + " (check higherIsBetter() against the color ladder's direction)");
        }
    }

    @Test
    void tokensAreUniqueLowercaseAndNotBlank() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (StatDefinition def : StatRegistry.all()) {
            String token = def.token();
            assertFalse(token == null || token.isBlank(), def.key() + " has a blank token");
            assertEquals(token, token.toLowerCase(java.util.Locale.ROOT),
                    def.key() + "'s token must be lowercase");
            assertTrue(seen.add(token), "duplicate template token: " + token);
        }
    }

    @Test
    void everyStatEnumConstantHasADefinition() {
        for (MineTunerConfig.Stat stat : MineTunerConfig.Stat.values()) {
            StatDefinition def = StatRegistry.get(stat); // throws when missing
            assertEquals(stat, def.key(), "definition registered under the wrong key");
        }
    }

    // --- Registration guard ---

    /** A stub definition sharing TPS's key, to prove duplicate keys throw. */
    private static final class DuplicateKeyStub implements StatDefinition {
        @Override public MineTunerConfig.Stat key() { return MineTunerConfig.Stat.TPS; }
        @Override public String token() { return "tps_stub"; }
        @Override public String format(int decimals) { return "stub"; }
    }

    /** A stub definition sharing FPS's token, to prove duplicate tokens throw. */
    private static final class DuplicateTokenStub implements StatDefinition {
        @Override public MineTunerConfig.Stat key() { return MineTunerConfig.Stat.HEALTH; }
        @Override public String token() { return "fps"; }
        @Override public String format(int decimals) { return "stub"; }
    }

    @Test
    void duplicateRegistrationFailsLoudlyInsteadOfShadowing() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> StatRegistry.register(new DuplicateKeyStub()),
                "a second definition for the same key must throw, not silently replace");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> StatRegistry.register(new DuplicateTokenStub()),
                "a second definition for the same template token must throw");
        // The failed registrations must not have disturbed the real definitions.
        assertEquals(MineTunerConfig.Stat.TPS, StatRegistry.get(MineTunerConfig.Stat.TPS).key());
        assertEquals("fps", StatRegistry.get(MineTunerConfig.Stat.FPS).token());
    }
}
