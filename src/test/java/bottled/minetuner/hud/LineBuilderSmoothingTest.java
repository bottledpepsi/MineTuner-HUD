package bottled.minetuner.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The moving-average ramp-up behavior of {@link LineBuilder#applySmoothing}: the
 * window grows from size 1 at the first sample rather than padding with zeros, so
 * early points aren't pulled artificially low.
 */
class LineBuilderSmoothingTest {

    @Test
    void windowOfOneReturnsSameArray() {
        float[] raw = {1, 2, 3};
        assertSame(raw, LineBuilder.applySmoothing(raw, 1));
        assertSame(raw, LineBuilder.applySmoothing(raw, 0));
        assertSame(raw, LineBuilder.applySmoothing(raw, -3));
    }

    @Test
    void shorterThanTwoSamplesReturnsSameArray() {
        float[] raw = {5};
        assertSame(raw, LineBuilder.applySmoothing(raw, 4));
    }

    @Test
    void rampUpWindowStartsAtSizeOne() {
        float[] out = LineBuilder.applySmoothing(new float[]{4, 8}, 2);
        assertArrayEquals(new float[]{4, 6}, out, 1e-6f);
    }

    @Test
    void fullWindowIsPlainMovingAverage() {
        float[] out = LineBuilder.applySmoothing(new float[]{1, 2, 3, 4}, 2);
        assertArrayEquals(new float[]{1, 1.5f, 2.5f, 3.5f}, out, 1e-6f);
    }

    @Test
    void windowLargerThanArrayNeverPullsEarlyPointsLow() {
        float[] out = LineBuilder.applySmoothing(new float[]{10, 20, 30}, 4);
        assertArrayEquals(new float[]{10, 15, 20}, out, 1e-6f);
    }

    @Test
    void inputIsNeverMutated() {
        float[] raw = {3, 6, 9, 12};
        float[] expected = raw.clone();
        LineBuilder.applySmoothing(raw, 3);
        assertArrayEquals(expected, raw, 1e-6f);
    }

    @Test
    void outputLengthMatchesInput() {
        float[] raw = new float[37];
        for (int i = 0; i < raw.length; i++) raw[i] = i;
        assertEquals(37, LineBuilder.applySmoothing(raw, 3).length);
    }
}
