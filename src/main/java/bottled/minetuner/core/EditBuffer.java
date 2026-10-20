package bottled.minetuner.core;

/**
 * A single-line text-entry buffer that counts and deletes whole code points, so emoji and
 * other supplementary characters (surrogate pairs in UTF-16) are never split. The cap is
 * counted in code points.
 */
public final class EditBuffer {

    private final StringBuilder text = new StringBuilder();
    private final int maxCodePoints;

    public EditBuffer(int maxCodePoints) {
        this(maxCodePoints, "");
    }

    /**
     * Starts with {@code initial}. The initial text is kept as is, even if it is already
     * longer than the cap; the cap only limits further typing.
     */
    public EditBuffer(int maxCodePoints, String initial) {
        if (maxCodePoints < 0) throw new IllegalArgumentException("maxCodePoints < 0");
        this.maxCodePoints = maxCodePoints;
        if (initial != null) text.append(initial);
    }

    /**
     * Appends a typed code point. Returns false (and changes nothing) for control
     * characters, invalid code points, lone surrogates, or when the cap is reached.
     */
    public boolean appendCodePoint(int codePoint) {
        if (codePoint < 32 || codePoint == 127) return false;
        if (!Character.isValidCodePoint(codePoint)) return false;
        if (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) return false;
        if (length() >= maxCodePoints) return false;
        text.appendCodePoint(codePoint);
        return true;
    }

    /**
     * Removes the last code point. Returns false if the buffer is empty.
     */
    public boolean backspace() {
        int chars = text.length();
        if (chars == 0) return false;
        text.setLength(text.offsetByCodePoints(chars, -1));
        return true;
    }

    /**
     * Length in code points.
     */
    public int length() {
        return text.codePointCount(0, text.length());
    }

    public boolean isEmpty() {
        return text.length() == 0;
    }

    public void clear() {
        text.setLength(0);
    }

    public String text() {
        return text.toString();
    }

    @Override
    public String toString() {
        return text();
    }
}
