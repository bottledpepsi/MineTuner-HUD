package bottled.minetuner.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorMathTest {

    @Test
    void withAlphaReplacesAlphaKeepingRgb() {
        assertEquals(0xAB345678, ColorMath.withAlpha(0x12345678, 0xAB));
        assertEquals(0xFF345678, ColorMath.withAlpha(0x12345678, 0xFF));
        assertEquals(0x00345678, ColorMath.withAlpha(0x12345678, 0x00));
    }

    @Test
    void withAlphaKeepsOriginalWhenAlphaMatches() {
        assertEquals(0x12345678, ColorMath.withAlpha(0x12345678, 0x12));
    }

    @Test
    void brightenBlackGoesTo35PercentWhite() {
        // (255-0)*35/100 = 89 = 0x59.
        assertEquals(0xFF595959, ColorMath.brighten(0xFF000000));
    }

    @Test
    void brightenWhiteStaysWhite() {
        assertEquals(0xFFFFFFFF, ColorMath.brighten(0xFFFFFFFF));
    }

    @Test
    void brightenPreservesAlpha() {
        assertEquals(0x80FFFFFF, ColorMath.brighten(0x80FFFFFF));
    }

    @Test
    void brightenMovesEachChannelTowardWhite() {
        // r=0 -> 89; g=255 -> 255; b=100 -> 100 + 155*35/100 = 100 + 54 = 154.
        assertEquals(0xFF59FF9A, ColorMath.brighten(0xFF00FF64));
    }

    @Test
    void blendZeroAmountKeepsSource() {
        // blend retains the SOURCE color's alpha, so the 0x40 alpha survives.
        assertEquals(0x40001122, ColorMath.blend(0x40001122, 0x80FFEE00, 0f));
    }

    @Test
    void blendFullAmountKeepsTargetRgbButSourceAlpha() {
        assertEquals(0x40FFEE00, ColorMath.blend(0x40001122, 0x80FFEE00, 1f));
    }

    @Test
    void blendHalfWay() {
        // from 0x000000 to 0xFFFFFF at 0.5 -> 0x808080 for each channel (128 = round(127.5)).
        assertEquals(0xFF808080, ColorMath.blend(0xFF000000, 0xFFFFFFFF, 0.5f));
    }

    @Test
    void blendClampsOutOfRangeAmounts() {
        assertEquals(0x40001122, ColorMath.blend(0x40001122, 0x80FFEE00, -1f));
        assertEquals(0x40FFEE00, ColorMath.blend(0x40001122, 0x80FFEE00, 2f));
    }
}
