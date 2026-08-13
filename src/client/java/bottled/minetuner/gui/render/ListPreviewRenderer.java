package bottled.minetuner.gui.render;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.hud.HudPanelChrome;
import bottled.minetuner.hud.LineCache;
import bottled.minetuner.hud.ListPositioner;
import bottled.minetuner.hud.MineTunerRenderer;
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
                                MineTunerConfig.StatListConfig lc, int mx, int my,
                                boolean isBeingDragged, int dragLiveX, int dragLiveY,
                                int screenW, int screenH) {
        LineCache cache = LineCache.getCachedLines(lc);
        boolean empty = cache.rowKinds().isEmpty();

        // For the empty placeholder, use the old single-line sizing (no
        // LineCache row to measure) — an empty list has no rows to render at all,
        // so there's no scaled size to match here. For a non-empty list, use
        // LineCache.boxW/boxH scaled by textScale, the exact same box size
        // MineTunerRenderer.render() uses, so the preview, hit-boxes, and
        // drag/anchor math all agree with the live overlay.
        int lineH = font.lineHeight + 1;
        float scale = lc.textScale <= 0f ? 1f : lc.textScale;
        int boxW, boxH;
        if (empty) {
            String placeholder = I18n.get("gui.minetuner.no_stats");
            boxW = font.width(placeholder) + HudPanelChrome.PADDING_X * 2;
            boxH = lineH + HudPanelChrome.PADDING_Y * 2;
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
            if (empty) {
                g.fill(wx, wy, wx + boxW, wy + boxH, 0xB8141820);
                g.outline(wx, wy, boxW, boxH, 0x5E9BA9BE);
            } else {
                HudPanelChrome.drawPanel(g, wx, wy, boxW, boxH);
            }
        }
        if (PanelChrome.isHoveringBox(mx, my, wx, wy, boxW, boxH) || isBeingDragged) {
            g.outline(wx, wy, boxW, boxH, 0xFFFFAA00);
        }

        boolean shadow = lc.textShadow;
        if (empty) {
            g.text(font, "§7" + I18n.get("gui.minetuner.no_stats"), wx + HudPanelChrome.PADDING_X,
                    wy + HudPanelChrome.PADDING_Y, 0xFFAAAAAA, shadow);
        } else if (scale == 1f) {
            MineTunerRenderer.drawRows(g, font, cache, wx, wy, shadow);
        } else {
            // Same local panel coordinate system as the live overlay.
            var matrices = g.pose();
            matrices.pushMatrix();
            matrices.translate(wx, wy);
            matrices.scale(scale, scale);
            MineTunerRenderer.drawRows(g, font, cache, 0, 0, shadow);
            matrices.popMatrix();
        }
    }

    /** Draws the centre-axis snap guide lines + hit-tick while a list is being dragged. */
    public static void drawSnapLines(GuiGraphicsExtractor g, int screenW, int screenH,
                                     MineTunerConfig.SnapX dragSnapX, MineTunerConfig.SnapY dragSnapY,
                                     int dragLiveX, int dragLiveY, int dragBoxW, int dragBoxH) {
        int cx = screenW / 2;
        int cy = screenH / 2;
        if (dragSnapX != MineTunerConfig.SnapX.NONE) {
            g.fill(cx, 0, cx + 1, screenH, SNAP_LINE_COL);
            int hitY = dragLiveY + dragBoxH / 2;
            g.fill(cx - SNAP_TICK, hitY, cx + SNAP_TICK + 1, hitY + 1, SNAP_HIT_COL);
        }
        if (dragSnapY != MineTunerConfig.SnapY.NONE) {
            g.fill(0, cy, screenW, cy + 1, SNAP_LINE_COL);
            int hitX = dragLiveX + dragBoxW / 2;
            g.fill(hitX, cy - SNAP_TICK, hitX + 1, cy + SNAP_TICK + 1, SNAP_HIT_COL);
        }
    }
}
