package bottled.minetuner.gui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A small, render-thread-only entrance transition for the editor's transient
 * panels. It deliberately has no ticking state: time is sampled at render time
 * so opening a menu never adds work to the client tick loop.
 */
public final class PanelTransition {

    /** The shared 210ms rollout/entrance window used by the editor's animated
     *  panels (previously duplicated as two private constants). */
    public static final long ROLLOUT_NANOS = 210_000_000L;
    private static final long DURATION_NANOS = ROLLOUT_NANOS;
    private int route = Integer.MIN_VALUE;
    private long startedAt;

    /** Starts a new entrance only when the visible panel route actually changes. */
    public void updateRoute(int newRoute) {
        if (route != newRoute) {
            route = newRoute;
            startedAt = System.nanoTime();
        }
    }

    /** Applies a short upward settle + scale reveal around the panel's anchor. */
    public void push(GuiGraphicsExtractor graphics, int anchorX, int anchorY) {
        float progress = progress();
        float scale = 0.955f + progress * 0.045f;
        float lift = (1f - progress) * 7f;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(anchorX, anchorY - lift);
        pose.scale(scale, scale);
        pose.translate(-anchorX, -anchorY);
    }

    public void pop(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    /** Current cubic ease-out progress, shared by panels with row rollouts. */
    public float progress() {
        if (startedAt == 0L) return 1f;
        float linear = Math.max(0f, Math.min(1f, (System.nanoTime() - startedAt) / (float) DURATION_NANOS));
        return easeOutCubic(linear);
    }

    /** Cubic ease-out: responsive at the beginning, then gently settles. The one
     *  shared copy (was duplicated in ReorderPanel and TemplateListPanel too). */
    public static float easeOutCubic(float linear) {
        float clamped = Math.max(0f, Math.min(1f, linear));
        float inverse = 1f - clamped;
        return 1f - inverse * inverse * inverse;
    }
}
