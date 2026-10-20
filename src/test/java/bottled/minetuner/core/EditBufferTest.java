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
}
