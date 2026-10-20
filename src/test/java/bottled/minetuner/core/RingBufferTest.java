package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {

    @Test
    void emptyAndPartiallyFilled() {
        RingBuffer r = new RingBuffer(5);
        assertEquals(0, r.snapshot().length);
        r.push(1f);
        r.push(2f);
        assertArrayEquals(new float[]{1f, 2f}, r.snapshot());
        assertEquals(2, r.size());
    }

    @Test
    void exactlyFull() {
        RingBuffer r = new RingBuffer(3);
        r.push(1f);
        r.push(2f);
        r.push(3f);
        assertArrayEquals(new float[]{1f, 2f, 3f}, r.snapshot());
    }

    @Test
    void wrapAround() {
        RingBuffer r = new RingBuffer(3);
        for (int i = 1; i <= 8; i++) r.push(i);
        assertArrayEquals(new float[]{6f, 7f, 8f}, r.snapshot());
        r.push(9f);
        assertArrayEquals(new float[]{7f, 8f, 9f}, r.snapshot());
    }

    @Test
    void clearThenPush() {
        RingBuffer r = new RingBuffer(4);
        for (int i = 1; i <= 10; i++) r.push(i);
        r.clear();
        assertEquals(0, r.snapshot().length);
        r.push(42f);
        assertArrayEquals(new float[]{42f}, r.snapshot());
    }

    @Test
    void versionIncrementsOnPushAndClear() {
        RingBuffer r = new RingBuffer(2);
        long v0 = r.version();
        r.push(1f);
        long v1 = r.version();
        r.clear();
        long v2 = r.version();
        assertTrue(v1 > v0);
        assertTrue(v2 > v1);
        r.push(2f);
        assertTrue(r.version() > v2);
    }

    @Test
    void matchesModuloReferenceForManySizes() {
        java.util.Random rnd = new java.util.Random(7);
        for (int size = 1; size <= 12; size++) {
            RingBuffer r = new RingBuffer(size);
            float[] ref = new float[size];
            int head = 0, count = 0;
            for (int i = 0; i < 60; i++) {
                float v = rnd.nextFloat();
                r.push(v);
                ref[head] = v;
                head = (head + 1) % size;
                if (count < size) count++;
                float[] expected = new float[count];
                int start = (head - count + size) % size;
                for (int k = 0; k < count; k++) expected[k] = ref[(start + k) % size];
                assertArrayEquals(expected, r.snapshot());
            }
        }
    }
}
