package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PercentileLowFpsTest {

    @Test
    void belowMinimumSampleCountIsUnavailable() {
        assertTrue(Float.isNaN(PercentileLowFps.computeLowFps(new float[]{10f, 20f}, 0.5f, 3)));
    }

    @Test
    void constantFrametimeGivesConstantFps() {
        float[] ft = new float[200];
        java.util.Arrays.fill(ft, 10f);
        assertEquals(100f, PercentileLowFps.computeLowFps(ft, 0.01f, 1), 0.001f);
    }

    @Test
    void sliceIsAtLeastOneFrame() {
        // 10 samples * 0.01 rounds to 0, clamped to 1 => the single slowest frame.
        float[] ft = {10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 40f};
        assertEquals(25f, PercentileLowFps.computeLowFps(ft, 0.01f, 1), 0.001f);
    }

    @Test
    void slowestFramesAreAveraged() {
        // 100 samples, 1% => the single slowest; 5% => mean of the 5 slowest.
        float[] ft = new float[100];
        java.util.Arrays.fill(ft, 10f);
        ft[3] = 100f;
        ft[50] = 50f;
        assertEquals(10f, PercentileLowFps.computeLowFps(ft, 0.01f, 1), 0.001f);
        // 5 slowest: 100, 50, 10, 10, 10 => mean 36 => 27.78 fps.
        assertEquals(1000f / 36f, PercentileLowFps.computeLowFps(ft, 0.05f, 1), 0.001f);
    }

    @Test
    void invalidFractionThrows() {
        assertThrows(IllegalArgumentException.class, () -> PercentileLowFps.computeLowFps(new float[]{1f}, 0f, 1));
        assertThrows(IllegalArgumentException.class, () -> PercentileLowFps.computeLowFps(new float[]{1f}, 1f, 1));
    }
}
