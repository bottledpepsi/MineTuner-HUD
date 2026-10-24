package bottled.minetuner.hud;

import bottled.minetuner.hud.TemplateEngine.ColoredRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny, allocation-free-after-warmup change feedback for text rows. Values do
 * not jump through an expensive interpolation; instead, a fresh value receives
 * a brief luminance pulse. (A companion one-pixel settle animation was removed
 * — the pulse is the whole effect now.)
 *
 * <p>Per-list row state is kept in {@code ROWS}, keyed by list id. Because list
 * ids come from a monotonically increasing counter and are never reused, a
 * deleted list's entry would otherwise leak forever — {@link #clear(int)} is
 * called from the delete path to evict it.
 */
public final class HudMotion {

    private static final long PULSE_NANOS = 180_000_000L;
    private static final Map<Integer, List<RowState>> ROWS = new HashMap<>();

    private HudMotion() {
    }

    /** Evicts the per-row pulse state for a deleted list, so create/delete cycles
     *  don't accumulate entries under ids that will never be reused. */
    public static void clear(int listId) {
        ROWS.remove(listId);
    }

    static float pulseFor(int listId, int rowIndex, List<ColoredRun> runs, long now) {
        List<RowState> states = ROWS.computeIfAbsent(listId, ignored -> new ArrayList<>());
        while (states.size() <= rowIndex) states.add(new RowState());

        RowState state = states.get(rowIndex);
        int signature = signatureOf(runs);
        if (!state.initialized) {
            state.initialized = true;
            state.signature = signature;
            return 0f;
        }
        if (state.signature != signature) {
            state.signature = signature;
            state.changedAt = now;
        }
        float age = Math.min(1f, (now - state.changedAt) / (float) PULSE_NANOS);
        float inverse = 1f - age;
        return inverse * inverse;
    }

    private static int signatureOf(List<ColoredRun> runs) {
        int signature = 1;
        for (ColoredRun run : runs) {
            signature = 31 * signature + run.text().hashCode();
            signature = 31 * signature + run.color();
        }
        return signature;
    }

    private static final class RowState {
        private boolean initialized;
        private int signature;
        private long changedAt;
    }
}
