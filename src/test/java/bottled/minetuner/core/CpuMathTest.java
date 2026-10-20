package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpuMathTest {

    @Test
    void constantSeriesReturnsConstant() {
        float[] dt = {10f, 20f, 30f};
        float[] cpu = {42f, 42f, 42f};
        assertEquals(42.0, CpuMath.timeWeightedAverage(dt, cpu), 1e-9);
    }

    @Test
    void unavailableGapsAreIgnored() {
        float[] dt = {10f, 10f, 10f, 10f};
        float[] cpu = {20f, -1f, -1f, 40f};
        assertEquals(30.0, CpuMath.timeWeightedAverage(dt, cpu), 1e-9);
    }

    @Test
    void weightedByFrameDuration() {
        float[] dt = new float[11];
        float[] cpu = new float[11];
        dt[0] = 2000f;
        cpu[0] = 80f;
        for (int i = 1; i <= 10; i++) {
            dt[i] = 10f;
            cpu[i] = 10f;
        }
        assertEquals((2000.0 * 80 + 100.0 * 10) / 2100.0, CpuMath.timeWeightedAverage(dt, cpu), 1e-6);
    }

    @Test
    void allUnavailableOrEmptyIsMinusOne() {
        assertEquals(-1.0, CpuMath.timeWeightedAverage(new float[]{10f, 10f}, new float[]{-1f, -1f}), 0.0);
        assertEquals(-1.0, CpuMath.timeWeightedAverage(new float[0], new float[0]), 0.0);
    }
}
