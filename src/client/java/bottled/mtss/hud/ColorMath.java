package bottled.mtss.hud;


final class ColorMath {

    private ColorMath() {
    }

    /** Replaces an ARGB color's alpha channel, keeping RGB intact. */
    static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    /** Lightens an RGB color's channels toward white by ~35%, used for the stroke. */
    static int brighten(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        r = Math.min(255, r + (255 - r) * 35 / 100);
        g = Math.min(255, g + (255 - g) * 35 / 100);
        b = Math.min(255, b + (255 - b) * 35 / 100);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Blends RGB while retaining the source color's alpha. */
    static int blend(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (from & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
