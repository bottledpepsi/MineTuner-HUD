package bottled.minetuner.core;

/**
 * A single-line text-entry buffer that counts and edits whole code points, so emoji and
 * other supplementary characters (surrogate pairs in UTF-16) are never split. The cap is
 * counted in code points.
 *
 * <p>Beyond plain append/backspace it models a caret and a selection (code point
 * indices), cursor movement (arrows/Home/End, with optional shift-extend), forward
 * delete, and clipboard-style bulk operations ({@link #selectAll}, {@link #copy},
 * {@link #cut}, {@link #paste(String)}) — the clipboard's contents are passed in by
 * the caller, keeping this class pure and unit-testable. Typing with a selection
 * active replaces it, matching every standard text field.
 *
 * <p>Minecraft's legacy "§" formatting code is rejected on input: it would otherwise
 * be rendered as a format prefix and corrupt the HUD/editor text around it.
 */
public final class EditBuffer {

    /** Minecraft's legacy formatting-code character — never allowed into user text. */
    private static final int SECTION_SIGN = 167;

    private final StringBuilder text = new StringBuilder();
    private final int maxCodePoints;
    /** Caret position in code points, always in [0, length]. Starts at the end. */
    private int cursor;
    /** Selection anchor in code points; equal to {@code cursor} when nothing is selected. */
    private int anchor;

    public EditBuffer(int maxCodePoints) {
        this(maxCodePoints, "");
    }

    /**
     * Starts with {@code initial}. The initial text is kept as is, even if it is already
     * longer than the cap; the cap only limits further typing. The caret starts at the
     * end, matching the pre-caret behavior of append/backspace.
     */
    public EditBuffer(int maxCodePoints, String initial) {
        if (maxCodePoints < 0) throw new IllegalArgumentException("maxCodePoints < 0");
        this.maxCodePoints = maxCodePoints;
        if (initial != null) text.append(initial);
        cursor = length();
        anchor = cursor;
    }

    private boolean isValidInputCodePoint(int codePoint) {
        if (codePoint < 32 || codePoint == 127) return false;
        if (codePoint == SECTION_SIGN) return false;
        if (!Character.isValidCodePoint(codePoint)) return false;
        return codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE;
    }

    /**
     * Types a code point at the caret (replacing any active selection). Returns false
     * (and changes nothing) for control characters, the "§" formatting code, invalid
     * code points, lone surrogates, or when the cap is reached.
     */
    public boolean appendCodePoint(int codePoint) {
        if (!isValidInputCodePoint(codePoint)) return false;
        if (hasSelection()) deleteSelection();
        if (length() >= maxCodePoints) return false;
        int charIdx = text.offsetByCodePoints(0, cursor);
        text.insert(charIdx, Character.toChars(codePoint));
        cursor++;
        anchor = cursor;
        return true;
    }

    /**
     * Removes the code point before the caret, or the whole selection if one is active.
     * Returns false when there is nothing to remove.
     */
    public boolean backspace() {
        if (hasSelection()) return deleteSelection();
        if (cursor == 0) return false;
        int charIdx = text.offsetByCodePoints(0, cursor - 1);
        int endChar = text.offsetByCodePoints(charIdx, 1);
        text.delete(charIdx, endChar);
        cursor--;
        anchor = cursor;
        return true;
    }

    /** Removes the code point after the caret, or the whole selection. False if at the end. */
    public boolean deleteForward() {
        if (hasSelection()) return deleteSelection();
        if (cursor >= length()) return false;
        int charIdx = text.offsetByCodePoints(0, cursor);
        int endChar = text.offsetByCodePoints(charIdx, 1);
        text.delete(charIdx, endChar);
        anchor = cursor;
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
        cursor = 0;
        anchor = 0;
    }

    public String text() {
        return text.toString();
    }

    @Override
    public String toString() {
        return text();
    }

    // --- Caret & selection ---

    /** Caret position in code points. */
    public int cursor() {
        return cursor;
    }

    /** True while a non-empty selection is active. */
    public boolean hasSelection() {
        return anchor != cursor;
    }

    /** First code point index of the selection (inclusive). */
    public int selectionStart() {
        return Math.min(anchor, cursor);
    }

    /** One past the last code point of the selection (exclusive). */
    public int selectionEnd() {
        return Math.max(anchor, cursor);
    }

    /** The selected text, or "" when nothing is selected. */
    public String selectedText() {
        if (!hasSelection()) return "";
        int startChar = text.offsetByCodePoints(0, selectionStart());
        int endChar = text.offsetByCodePoints(startChar, selectionEnd() - selectionStart());
        return text.substring(startChar, endChar);
    }

    /** Moves the caret by {@code delta} code points (clamped). When {@code extend} is
     *  false the selection collapses to the new position; when true the anchor stays,
     *  extending the selection. Returns false when the caret didn't move. */
    public boolean moveCursor(int delta, boolean extend) {
        int target = Math.max(0, Math.min(length(), cursor + delta));
        if (target == cursor) return false;
        cursor = target;
        if (!extend) anchor = cursor;
        return true;
    }

    /** Collapses the selection and puts the caret at {@code codePointIndex} (clamped). */
    public void setCursor(int codePointIndex) {
        cursor = Math.max(0, Math.min(length(), codePointIndex));
        anchor = cursor;
    }

    /** Caret to the start, selection cleared. */
    public void home() {
        cursor = 0;
        anchor = 0;
    }

    /** Caret to the end, selection cleared. */
    public void end() {
        cursor = length();
        anchor = cursor;
    }

    /** Selects everything. */
    public void selectAll() {
        anchor = 0;
        cursor = length();
    }

    /** Text before / after the caret, for cursor-aware rendering. */
    public String textBeforeCursor() {
        if (cursor == 0) return "";
        return text.substring(0, text.offsetByCodePoints(0, cursor));
    }

    /** Text after the caret (selection is still shown — it sits before the caret). */
    public String textAfterCursor() {
        if (cursor >= length()) return "";
        return text.substring(text.offsetByCodePoints(0, cursor));
    }

    // --- Clipboard-style operations (clipboard contents supplied by the caller) ---

    /** The text a Ctrl+C should copy: the selection, or the whole buffer when none. */
    public String copy() {
        return hasSelection() ? selectedText() : text();
    }

    /** Removes the selection and returns it; "" (and no change) when nothing is selected. */
    public String cut() {
        String selected = selectedText();
        if (hasSelection()) deleteSelection();
        return selected;
    }

    /** Inserts {@code clipboard} at the caret (replacing any selection), filtering
     *  each code point with the same rules as {@link #appendCodePoint} and stopping at
     *  the cap. Returns true when at least one code point landed. */
    public boolean paste(String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) return false;
        if (hasSelection()) deleteSelection();
        boolean inserted = false;
        for (int i = 0; i < clipboard.length(); ) {
            int cp = clipboard.codePointAt(i);
            i += Character.charCount(cp);
            if (!isValidInputCodePoint(cp)) continue;
            if (length() >= maxCodePoints) break;
            int charIdx = text.offsetByCodePoints(0, cursor);
            text.insert(charIdx, Character.toChars(cp));
            cursor++;
            anchor = cursor;
            inserted = true;
        }
        return inserted;
    }

    /** Removes the selection (if any). Returns false when there was nothing to remove. */
    public boolean deleteSelection() {
        if (!hasSelection()) return false;
        int start = selectionStart();
        int startChar = text.offsetByCodePoints(0, start);
        int endChar = text.offsetByCodePoints(startChar, selectionEnd() - start);
        text.delete(startChar, endChar);
        cursor = start;
        anchor = start;
        return true;
    }
}
