package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AveragesTest {

    @Test
    void emptySeriesIsUnavailable() {
        assertEquals(-1.0, Averages.averageAvailable(new float[0]));
        assertEquals(-1.0, Averages.averageAvailable(null));
    }

    @Test
    void allSentinelSeriesIsUnavailable() {
        assertEquals(-1.0, Averages.averageAvailable(new float[]{-1f, -1f, -1f}));
    }

    @Test
    void sentinelsAreSkippedNotAveragedAsZero() {
        // 20% real readings averaged with the -1 sentinels would read 5.6%;
        // skipping them must give the true 40%.
        assertEquals(40.0, Averages.averageAvailable(new float[]{-1f, 30f, -1f, 50f}), 1e-9);
    }

    @Test
    void allAvailableSeriesIsPlainMean() {
        assertEquals(25.0, Averages.averageAvailable(new float[]{10f, 20f, 30f, 40f}), 1e-9);
    }

    @Test
    void zeroIsAValidReadingNotASentinel() {
        assertEquals(0.0, Averages.averageAvailable(new float[]{0f}), 1e-9);
    }
}
