package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import bottled.mtss.hud.TemplateEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.HashMap;
import java.util.Map;

import static bottled.mtss.gui.render.PanelChrome.*;

public final class TemplateListPanel {

    private static final long ADD_ROLLOUT_NANOS = 210_000_000L;
    private static final Map<Integer, AddedLine> ADDED_LINES = new HashMap<>();

    private TemplateListPanel() {
    }

    /** Row count for the template line list panel. */
    public static int rowCount(MtssConfig.StatListConfig lc) {
        return 1 + lc.templateLines.size() + 2; // header + lines + add + back.
    }

    public static int panelHeight(MtssConfig.StatListConfig lc) {
        return PANEL_PAD * 2 + ROW_H * rowCount(lc);
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc) {
        int fullPanelH = panelHeight(lc);
        AddedLine added = ADDED_LINES.get(lc.id);
        float reveal = added == null ? 1f : rolloutProgress(added);
        int panelH = fullPanelH - Math.round(ROW_H * (1f - reveal));
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, fullPanelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);
        g.text(font, "§e" + I18n.get("gui.mtss.template.title"),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H;
        for (int i = 0; i < lc.templateLines.size(); i++) {
            int ry = rowTop + i * ROW_H;
            boolean isNewLine = added != null && added.index() == i;
            if (isNewLine && reveal <= 0.01f) continue;
            if (isNewLine) {
                // Scale the entire row from its top edge; this has the same
                // continuous rollout as an expanded stat category.
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(0, ry);
                pose.scale(1f, reveal);
                pose.translate(0, -ry);
                renderLineRow(g, font, mx, my, px, ry, lc, i, reveal >= 0.98f);
                pose.popMatrix();
            } else {
                renderLineRow(g, font, mx, my, px, ry, lc, i, true);
            }
        }

        // Add and Back are downstream of the new line, so they travel with it.
        int insertionShift = added == null ? 0 : Math.round(ROW_H * (1f - reveal));
        int addY = rowTop + lc.templateLines.size() * ROW_H - insertionShift;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, addY, PANEL_W, ROW_H);
        g.text(font, "§a" + I18n.get("gui.mtss.template.add_line"),
                px + PANEL_PAD, addY + 2, 0xFFFFFFFF, false);

        int backY = addY + ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, backY, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.mtss.reorder.close"),
                px + PANEL_PAD, backY + 2, 0xFFFFFFFF, false);

        if (added != null && reveal >= 1f) ADDED_LINES.remove(lc.id);
    }

    private static void renderLineRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                      int mx, int my, int px, int ry, MtssConfig.StatListConfig lc,
                                      int index, boolean allowHover) {
        if (allowHover) PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
        String raw = lc.templateLines.get(index);
        String preview = raw.isEmpty() ? "§7" + I18n.get("gui.mtss.template.empty_line")
                : "§f" + truncateForRow(font, raw, PANEL_W - PANEL_PAD - 24);
        g.text(font, I18n.get("gui.mtss.template.line_number", index + 1) + " " + preview,
                px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);

        boolean removeHovered = allowHover && PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 12, ry, 10, ROW_H);
        g.text(font, removeHovered ? "§c✕" : "§7✕", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
    }

    /** Truncates a template line preview to fit the panel row, appending an ellipsis. */
    private static String truncateForRow(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ellipsis = "...";
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(s.substring(0, mid) + ellipsis) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MtssConfig.StatListConfig lc) {
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Handles a click at (mx, my). */
    public static ClickResult handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                          MtssConfig.StatListConfig lc) {
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        int rowTop = py + PANEL_PAD + ROW_H;
        int addY = rowTop + lc.templateLines.size() * ROW_H;
        int backY = addY + ROW_H;

        if (PanelChrome.isHoveringRow(mx, my, px, backY, PANEL_W, ROW_H)) return ClickResult.CLOSED_RESULT;
        if (PanelChrome.isHoveringRow(mx, my, px, addY, PANEL_W, ROW_H)) {
            lc.templateLines.add("");
            ADDED_LINES.put(lc.id, new AddedLine(lc.templateLines.size() - 1, System.nanoTime()));
            MtssConfig.getInstance().save();
            TemplateEngine.invalidate(lc.id);
            return ClickResult.NONE_RESULT;
        }

        for (int i = 0; i < lc.templateLines.size(); i++) {
            int ry = rowTop + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            if (mx >= px + PANEL_W - 12) {
                lc.templateLines.remove(i);
                ADDED_LINES.remove(lc.id);
                MtssConfig.getInstance().save();
                TemplateEngine.invalidate(lc.id);
            } else {
                return new ClickResult(Kind.EDIT_LINE, i);
            }
            return ClickResult.NONE_RESULT;
        }
        return ClickResult.NONE_RESULT;
    }

    /** Text-entry box for one templateLines entry. */
    public static void renderEditBox(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                     int menuX, int menuY, int screenW, int screenH,
                                     int templateEditIndex, String templateEditBuffer) {
        String prompt = "§e" + I18n.get("gui.mtss.template.edit_prompt", templateEditIndex + 1);
        String display = templateEditBuffer + "§7|";
        int panelW = Math.max(PANEL_W + 40, Math.min(300, font.width(display) + PANEL_PAD * 2));
        int panelH = PANEL_PAD * 2 + ROW_H * 2 + 2;
        int px = PanelChrome.clampX(menuX, panelW, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        PanelChrome.drawBackground(g, px, py, panelW, panelH);
        g.text(font, prompt, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);
        g.text(font, display, px + PANEL_PAD, py + PANEL_PAD + ROW_H + 2, 0xFFFFFFFF, false);
    }

    public enum Kind {NONE, CLOSED, EDIT_LINE}

    public record ClickResult(Kind kind, int editIndex) {
        private static final ClickResult NONE_RESULT = new ClickResult(Kind.NONE, -1);
        private static final ClickResult CLOSED_RESULT = new ClickResult(Kind.CLOSED, -1);
    }

    private static float rolloutProgress(AddedLine added) {
        float linear = Math.max(0f, Math.min(1f,
                (System.nanoTime() - added.startedAt()) / (float) ADD_ROLLOUT_NANOS));
        float inverse = 1f - linear;
        return 1f - inverse * inverse * inverse;
    }

    private record AddedLine(int index, long startedAt) {
    }
}
