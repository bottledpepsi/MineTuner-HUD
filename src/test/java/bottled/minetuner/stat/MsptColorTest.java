package bottled.minetuner.stat;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.stat.stats.MsptStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Per-segment MSPT graph colouring must treat samples as milliseconds, not TPS.
 */
class MsptColorTest {

    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int RED = 0xFFFF5555;

    @Test
    void perSampleColourUsesTpsEquivalent() {
        MineTunerDataHolder.tickRate = 20f;
        MsptStat stat = new MsptStat();
        assertEquals(GREEN, stat.colorFor(5f, null));
        assertEquals(GREEN, stat.colorFor(20f, null));
        assertEquals(GREEN, stat.colorFor(50f, null));
        assertEquals(YELLOW, stat.colorFor(60f, null));
        assertEquals(RED, stat.colorFor(80f, null));
    }
}
