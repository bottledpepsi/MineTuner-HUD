package bottled.mtss.hud;

import bottled.mtss.hud.TemplateEngine.ColoredRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny, allocation-free-after-warmup change feedback for text rows. Values do
 * not jump through an expensive interpolation; instead, a fresh value receives
 * a brief luminance pulse and one-pixel settle animation.
 */
final class HudMotion {

    private static final long PULSE_NANOS = 180_000_000L;
    private static final Map<Integer, List<RowState>> ROWS = new HashMap<>();

    private HudMotion() {
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
