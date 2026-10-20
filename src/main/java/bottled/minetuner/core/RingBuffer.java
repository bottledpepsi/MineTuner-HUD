package bottled.minetuner.core;

/**
 * Minimal fixed-size float ring buffer with copy-out reads. Not thread-safe: all access
 * is expected from one thread.
 */
public final class RingBuffer {

    private final float[] buf;
    private int count = 0; // number of valid samples, caps at buf.length.
    private int head = 0;  // index the NEXT sample will be written to.
    private long version = 0L;

    public RingBuffer(int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        this.buf = new float[size];
    }

    public void push(float value) {
        buf[head] = value;
        head++;
        if (head == buf.length) head = 0;
        if (count < buf.length) count++;
        version++;
    }

    /**
     * Returns a copy of the valid samples in oldest-to-newest order.
     */
    public float[] snapshot() {
        float[] out = new float[count];
        if (count == 0) return out;
        int start = head - count;
        if (start < 0) start += buf.length;
        int firstSegment = Math.min(count, buf.length - start);
        System.arraycopy(buf, start, out, 0, firstSegment);
        if (firstSegment < count) {
            System.arraycopy(buf, 0, out, firstSegment, count - firstSegment);
        }
        return out;
    }

    /**
     * Discards all samples without reallocating the backing array: resetting count/head is
     * enough because {@link #snapshot()} only reads the valid range.
     */
    public void clear() {
        count = 0;
        head = 0;
        version++;
    }

    /**
     * Number of valid samples.
     */
    public int size() {
        return count;
    }

    /**
     * Monotonically increasing; changes on every {@link #push} and {@link #clear}, so a
     * consumer can cache anything derived from the contents keyed on this value.
     */
    public long version() {
        return version;
    }
}
