package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditBufferTest {

    private static final int EMOJI = 0x1F600; // surrogate pair in UTF-16

    @Test
    void backspaceRemovesWholeEmoji() {
        EditBuffer b = new EditBuffer(32);
        b.appendCodePoint('a');
        b.appendCodePoint(EMOJI);
        assertEquals(2, b.length());
        assertTrue(b.backspace());
        assertEquals("a", b.text());
        for (char c : b.text().toCharArray()) assertFalse(Character.isSurrogate(c));
    }

    @Test
    void capIsCountedInCodePoints() {
        EditBuffer b = new EditBuffer(32);
        for (int i = 0; i < 100; i++) b.appendCodePoint(EMOJI);
        assertEquals(32, b.length());
        assertEquals(64, b.text().length()); // 64 UTF-16 units, exactly 32 code points
        assertFalse(b.appendCodePoint('x'));
    }

    @Test
    void backspaceOnEmptyIsNoOp() {
        EditBuffer b = new EditBuffer(5);
        assertFalse(b.backspace());
        assertEquals("", b.text());
    }

    @Test
    void rejectsControlAndInvalidCodePoints() {
        EditBuffer b = new EditBuffer(5);
        assertFalse(b.appendCodePoint(10));
        assertFalse(b.appendCodePoint(0xD800));
        assertFalse(b.appendCodePoint(0x110000));
        assertTrue(b.isEmpty());
    }

    @Test
    void initialTextIsKeptEvenIfOverCap() {
        EditBuffer b = new EditBuffer(3, "abcdef");
        assertEquals("abcdef", b.text());
        assertFalse(b.appendCodePoint('x'));
        assertTrue(b.backspace());
        assertEquals("abcde", b.text());
    }

    // --- Caret & selection ---

    @Test
    void typingReplacesActiveSelection() {
        EditBuffer b = new EditBuffer(32, "hello");
        b.selectAll();
        assertTrue(b.appendCodePoint('x'));
        assertEquals("x", b.text());
        assertEquals(1, b.length());
        assertEquals(1, b.cursor());
    }

    @Test
    void rejectsFormattingCode() {
        EditBuffer b = new EditBuffer(32);
        assertFalse(b.appendCodePoint(167)); // "§" — Minecraft format code
        assertTrue(b.paste("a\u00A7bb"));
        assertEquals("abb", b.text());
    }

    @Test
    void cursorMovementIsClampedAndCodePointAware() {
        EditBuffer b = new EditBuffer(32, "ab" + new String(Character.toChars(EMOJI)));
        assertEquals(3, b.length());
        assertTrue(b.moveCursor(-1, false));
        assertEquals(2, b.cursor());
        assertTrue(b.moveCursor(-10, false)); // clamps at 0 and still moved (2 -> 0)
        assertEquals(0, b.cursor());
        b.end();
        assertEquals(3, b.cursor());
        // Arrow left over an emoji must land between code points, never mid-pair.
        b.moveCursor(-1, false);
        assertEquals(2, b.cursor());
        assertEquals("ab", b.textBeforeCursor());
        assertTrue(b.textAfterCursor().startsWith(String.valueOf(Character.toChars(EMOJI))));
    }

    @Test
    void shiftArrowExtendsSelection() {
        EditBuffer b = new EditBuffer(32, "abc");
        b.moveCursor(-1, true); // extend: anchor stays at 3
        assertTrue(b.hasSelection());
        assertEquals("c", b.selectedText());
        b.moveCursor(-1, true);
        assertEquals("bc", b.selectedText());
        b.setCursor(2); // collapse
        assertFalse(b.hasSelection());
    }

    @Test
    void backspaceDeletesSelectionBeforeTheCaret() {
        EditBuffer b = new EditBuffer(32, "abc");
        b.selectAll();
        assertTrue(b.backspace());
        assertEquals("", b.text());
        assertEquals(0, b.cursor());
    }

    @Test
    void forwardDeleteWorksAtAndBeforeEnd() {
        EditBuffer b = new EditBuffer(32, "abc");
        b.home();
        assertTrue(b.deleteForward());
        assertEquals("bc", b.text());
        b.end();
        assertFalse(b.deleteForward());
    }

    @Test
    void copyCutPasteSemantics() {
        EditBuffer b = new EditBuffer(32, "hello world");
        assertEquals("hello world", b.copy()); // no selection -> whole text
        b.home();
        b.moveCursor(5, true); // select "hello"
        assertEquals("hello", b.selectedText());
        assertEquals("hello", b.copy());
        assertEquals("hello", b.cut());
        assertEquals(" world", b.text());
        assertTrue(b.paste("goodbye"));
        assertEquals("goodbye world", b.text());
        assertEquals(7, b.cursor()); // caret after the pasted text (0 + 7 chars)
        assertFalse(b.paste(null)); // null clipboard is a harmless no-op
        assertFalse(b.paste(""));
    }

    @Test
    void pasteFiltersAndRespectsTheCap() {
        EditBuffer b = new EditBuffer(4, "ab");
        assertTrue(b.paste("c\u00A7def\u0000g"));
        // "c" fits (cap 4), "§" filtered, "def" stops at cap, control char filtered.
        assertEquals("abcd", b.text());
        assertEquals(4, b.cursor());
    }

    @Test
    void pasteIntoSelectionReplacesIt() {
        EditBuffer b = new EditBuffer(32, "hello world");
        b.home();
        b.moveCursor(5, true);
        assertTrue(b.paste("hi"));
        assertEquals("hi world", b.text());
    }

    @Test
    void clearResetsCaretAndSelection() {
        EditBuffer b = new EditBuffer(32, "abc");
        b.selectAll();
        b.clear();
        assertEquals(0, b.cursor());
        assertFalse(b.hasSelection());
        assertFalse(b.backspace());
    }
}
