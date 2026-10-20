package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizeTest {

    @Test
    void intClamp() {
        assertEquals(20, Sanitize.clamp(100000000 * 0 + 5, 20, 400));
        assertEquals(400, Sanitize.clamp(100000000, 20, 400));
        assertEquals(100, Sanitize.clamp(100, 20, 400));
    }

    @Test
    void nonFiniteUsesFallback() {
        assertEquals(1f, Sanitize.clamp(Float.NaN, 0.5f, 2f, 1f), 0f);
        assertEquals(1f, Sanitize.clamp(Float.POSITIVE_INFINITY, 0.5f, 2f, 1f), 0f);
        assertEquals(2f, Sanitize.clamp(9f, 0.5f, 2f, 1f), 0f);
        assertEquals(0.01, Sanitize.clamp(Double.NaN, 0.0, 1.0, 0.01), 0.0);
        assertEquals(1.0, Sanitize.clamp(5.0, 0.0, 1.0, 0.01), 0.0);
    }
}
