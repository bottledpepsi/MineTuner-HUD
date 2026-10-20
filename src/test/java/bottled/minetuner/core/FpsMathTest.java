package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FpsMathTest {

    @Test
    void oneHugeStallDominates() {
        float[] ft = new float[100];
        java.util.Arrays.fill(ft, 10f);
        ft[99] = 1000f;
        assertEquals(50.25f, FpsMath.averageFps(ft), 0.01f);
    }

    @Test
    void periodicStalls() {
        float[] ft = new float[600];
        java.util.Arrays.fill(ft, 16.7f);
        for (int i = 0; i < 10; i++) ft[i * 60] = 200f;
        assertEquals(50.62f, FpsMath.averageFps(ft), 0.01f);
    }

    @Test
    void emptyIsZero() {
        assertEquals(0f, FpsMath.averageFps(new float[0]), 0f);
        assertEquals(0f, FpsMath.averageFps(0L, 0.0), 0f);
    }

    @Test
    void constantSeries() {
        float[] ft = new float[50];
        java.util.Arrays.fill(ft, 10f);
        assertEquals(100f, FpsMath.averageFps(ft), 0.001f);
    }

    @Test
    void accumulatorFormMatchesArrayForm() {
        assertEquals(100f, FpsMath.averageFps(50L, 500.0), 0.001f);
    }
}
