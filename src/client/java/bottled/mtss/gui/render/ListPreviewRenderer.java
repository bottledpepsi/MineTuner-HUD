package bottled.mtss.gui.render;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.hud.LineCache;
import bottled.mtss.hud.ListPositioner;
import bottled.mtss.hud.MtssRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;


public final class ListPreviewRenderer {

    private static final int SNAP_LINE_COL = 0xBBFFFFFF;
    private static final int SNAP_HIT_COL = 0xFFFFAA00;
    private static final int SNAP_TICK = 6;
    private ListPreviewRenderer() {
    }

    /** Draws one list's box. */
    public static void drawList(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                MtssConfig.StatListConfig lc, int mx, int my,
                                boolean isBeingDragged, int dragLiveX, int dragLiveY,
                                int screenW, int screenH) {
        LineCache cache = LineCache.getCachedLines(lc);
        boolean empty = cache.rowKinds().isEmpty();

        // For the empty placeholder, use the old single-line sizing (no.
        // LineCache row to measure).
        // entirely, so there's no scaled size to match here.
        // LineCache.boxW/boxH scaled by textScale.
        // MtssRenderer.render() uses, so the preview, hit-boxes, and.
        // drag/anchor math all agree with the live overlay.
        int lineH = font.lineHeight + 1;
        float scale = lc.textScale <= 0f ? 1f : lc.textScale;
        int boxW, boxH;
        if (empty) {
            String placeholder = I18n.get("gui.mtss.no_stats");
            boxW = font.width(placeholder) + 4;
            boxH = lineH + 3;
        } else {
            boxW = Math.round(cache.boxW(font) * scale);
            boxH = Math.round(cache.boxH(font) * scale);
        }

        int wx, wy;
        if (isBeingDragged) {
            wx = Math.max(0, Math.min(screenW - boxW, dragLiveX));
            wy = Math.max(0, Math.min(screenH - boxH, dragLiveY));
        } else {
            int[] pos = ListPositioner.getPosition(lc, screenW, screenH, boxW, boxH);
            wx = pos[0];
            wy = pos[1];
        }

        if (lc.showBackground || empty) {
            g.fill(wx - 1, wy - 1, wx + boxW + 1, wy + boxH + 1,
                    empty ? 0xAA222222 : 0xCC000000);
        }
        if (PanelChrome.isHoveringBox(mx, my, wx, wy, boxW, boxH) || isBeingDragged) {
            g.outline(wx - 1, wy - 1, boxW + 2, boxH + 2, 0xFFFFAA00);
        }

        boolean shadow = lc.textShadow;
        if (empty) {
            g.text(font, "§7" + I18n.get("gui.mtss.no_stats"), wx + 2, wy + 2, 0xFFAAAAAA, shadow);
        } else if (scale == 1f) {
            MtssRenderer.drawRows(g, font, cache, wx + 2, wy + 2, shadow);
        } else {
            // Translate to (wx, wy), scale, then draw at unscaled local offset (0, 0).
            // same pattern (and same lack of a +2 inset) as MtssRenderer.render()'s.
            // scaled branch, so the preview matches the live overlay exactly.
            var matrices = g.pose();
            matrices.pushMatrix();
            matrices.translate(wx, wy);
            matrices.scale(scale, scale);
            MtssRenderer.drawRows(g, font, cache, 0, 0, shadow);
            matrices.popMatrix();
        }
    }

    /** Draws the centre-axis snap guide lines + hit-tick while a list is being dragged. */
    public static void drawSnapLines(GuiGraphicsExtractor g, int screenW, int screenH,
                                     MtssConfig.SnapX dragSnapX, MtssConfig.SnapY dragSnapY,
                                     int dragLiveX, int dragLiveY, int dragBoxW, int dragBoxH) {
        int cx = screenW / 2;
        int cy = screenH / 2;
        if (dragSnapX != MtssConfig.SnapX.NONE) {
            g.fill(cx, 0, cx + 1, screenH, SNAP_LINE_COL);
            int hitY = dragLiveY + dragBoxH / 2;
            g.fill(cx - SNAP_TICK, hitY, cx + SNAP_TICK + 1, hitY + 1, SNAP_HIT_COL);
        }
        if (dragSnapY != MtssConfig.SnapY.NONE) {
            g.fill(0, cy, screenW, cy + 1, SNAP_LINE_COL);
            int hitX = dragLiveX + dragBoxW / 2;
            g.fill(hitX, cy - SNAP_TICK, hitX + 1, cy + SNAP_TICK + 1, SNAP_HIT_COL);
        }
    }
}
