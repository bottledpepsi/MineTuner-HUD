package bottled.minetuner.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Allocation-light fixed-point number formatting that matches
 * {@code String.format(Locale.ROOT, "%.<n>f", value)} exactly, including its
 * round-half-up behaviour on the value's shortest decimal representation.
 */
public final class FastFormat {

    private static final long[] POW10 = {1L, 10L, 100L, 1000L, 10_000L, 100_000L, 1_000_000L};

    private FastFormat() {
    }

    /**
     * Appends {@code value} with {@code decimals} (0..6) digits after the point. Inputs the
     * fast path cannot handle exactly (negative, NaN/Infinity, very large, or within a
     * hair of a rounding tie) fall back to the exact slow path.
     */
    public static void appendFixed(StringBuilder out, double value, int decimals) {
        if (decimals < 0 || decimals >= POW10.length) {
            throw new IllegalArgumentException("decimals must be in [0, 6]");
        }
        if (!(value >= 0.0) || value >= 1.0e9) {
            out.append(String.format(Locale.ROOT, "%." + decimals + "f", value));
            return;
        }
        long pow = POW10[decimals];
        double scaled = value * pow;
        double floor = Math.floor(scaled);
        double frac = scaled - floor;
        if (Math.abs(frac - 0.5) < 1.0e-6) {
            out.append(new BigDecimal(Double.toString(value)).setScale(decimals, RoundingMode.HALF_UP).toPlainString());
            return;
        }
        long units = (long) floor + (frac > 0.5 ? 1 : 0);
        out.append(units / pow);
        if (decimals > 0) {
            out.append('.');
            long fraction = units % pow;
            // Left-pad the fractional digits with zeros.
            for (long p = pow / 10; p > fraction && p > 1; p /= 10) out.append('0');
            out.append(fraction);
        }
    }
}
