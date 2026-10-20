package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TickMathTest {

    @Test
    void conversionTable() {
        assertEquals(20f, TickMath.tpsFromMspt(5f, 20f), 0.001f);    // 200 TPS capped
        assertEquals(20f, TickMath.tpsFromMspt(20f, 20f), 0.001f);   // 50 TPS capped
        assertEquals(20f, TickMath.tpsFromMspt(50f, 20f), 0.001f);   // exactly 20
        assertEquals(16.667f, TickMath.tpsFromMspt(60f, 20f), 0.01f);
        assertEquals(12.5f, TickMath.tpsFromMspt(80f, 20f), 0.001f);
    }

    @Test
    void nonPositiveSampleReadsAsTickRate() {
        assertEquals(20f, TickMath.tpsFromMspt(0f, 20f), 0f);
        assertEquals(20f, TickMath.tpsFromMspt(-1f, 20f), 0f);
    }

    @Test
    void honoursConfiguredTickRate() {
        assertEquals(10f, TickMath.tpsFromMspt(10f, 10f), 0.001f);
    }
}
