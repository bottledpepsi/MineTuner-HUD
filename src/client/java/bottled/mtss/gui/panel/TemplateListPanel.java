package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import bottled.mtss.hud.TemplateEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.mtss.gui.render.PanelChrome.PANEL_PAD;
import static bottled.mtss.gui.render.PanelChrome.PANEL_W;
import static bottled.mtss.gui.render.PanelChrome.ROW_H;


public final class TemplateListPanel {

    private TemplateListPanel() {}

    /** Row count for the template line list panel: header + one row per line + "+ Add line" + "Back". */
    public static int rowCount(MtssConfig.StatListConfig lc) {
        return 1 + lc.templateLines.size() + 2; // header + lines + add + back
    }

    public static int panelHeight(MtssConfig.StatListConfig lc) {
        return PANEL_PAD * 2 + ROW_H * rowCount(lc);
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc) {
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);
        g.text(font, "§e" + I18n.get("gui.mtss.template.title"),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H;
        for (int i = 0; i < lc.templateLines.size(); i++) {
            int ry = rowTop + i * ROW_H;
            PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);

            String raw = lc.templateLines.get(i);
            String preview = raw.isEmpty() ? "§7" + I18n.get("gui.mtss.template.empty_line")
                                            : "§f" + truncateForRow(font, raw, PANEL_W - PANEL_PAD - 24);
            g.text(font, I18n.get("gui.mtss.template.line_number", i + 1) + " " + preview,
                    px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);

            // ✕ remove button, right-aligned
            boolean removeHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 12, ry, 10, ROW_H);
            g.text(font, removeHovered ? "§c✕" : "§7✕", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
        }

        int addY = rowTop + lc.templateLines.size() * ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, addY, PANEL_W, ROW_H);
        g.text(font, "§a" + I18n.get("gui.mtss.template.add_line"),
                px + PANEL_PAD, addY + 2, 0xFFFFFFFF, false);

        int backY = addY + ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, backY, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.mtss.reorder.close"),
                px + PANEL_PAD, backY + 2, 0xFFFFFFFF, false);
    }

    /** Truncates a template line preview to fit the panel row, appending an ellipsis marker when cut. */
    private static String truncateForRow(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ellipsis = "...";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(s.substring(0, mid) + ellipsis) <= maxWidth) lo = mid; else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MtssConfig.StatListConfig lc) {
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** What a click on the line-list panel did — communicated back to the coordinator. */
    public enum Kind { NONE, CLOSED, EDIT_LINE }

    /**
     * Outcome of a click on the line-list panel. {@code editIndex} is only
     * meaningful when {@code kind == EDIT_LINE}, in which case the caller
     * should open the edit box for {@code lc.templateLines.get(editIndex)}.
     */
    public record ClickResult(Kind kind, int editIndex) {
        private static final ClickResult NONE_RESULT = new ClickResult(Kind.NONE, -1);
        private static final ClickResult CLOSED_RESULT = new ClickResult(Kind.CLOSED, -1);
    }

    /**
     * Handles a click at (mx, my). Returns an {@code EDIT_LINE} result when
     * a line row was clicked so the caller can open the edit box for it,
     * {@code CLOSED} for the Back row, or {@code NONE} otherwise (add/remove
     * are applied directly here).
     */
    public static ClickResult handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                          MtssConfig.StatListConfig lc) {
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        int rowTop = py + PANEL_PAD + ROW_H;

        int addY  = rowTop + lc.templateLines.size() * ROW_H;
        int backY = addY + ROW_H;

        if (PanelChrome.isHoveringRow(mx, my, px, backY, PANEL_W, ROW_H)) { return ClickResult.CLOSED_RESULT; }

        if (PanelChrome.isHoveringRow(mx, my, px, addY, PANEL_W, ROW_H)) {
            lc.templateLines.add("");
            MtssConfig.getInstance().save();
            TemplateEngine.invalidate(lc.id);
            return ClickResult.NONE_RESULT;
        }

        for (int i = 0; i < lc.templateLines.size(); i++) {
            int ry = rowTop + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            if (mx >= px + PANEL_W - 12) {
                // ✕ remove this line
                lc.templateLines.remove(i);
                MtssConfig.getInstance().save();
                TemplateEngine.invalidate(lc.id);
            } else {
                // Open the text-entry box for this line
                return new ClickResult(Kind.EDIT_LINE, i);
            }
            return ClickResult.NONE_RESULT;
        }
        return ClickResult.NONE_RESULT;
    }

    /** Text-entry box for one templateLines entry — same pattern as RenameBoxPanel, just wider since template strings run longer. */
    public static void renderEditBox(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                     int menuX, int menuY, int screenW, int screenH,
                                     int templateEditIndex, String templateEditBuffer) {
        String prompt  = "§e" + I18n.get("gui.mtss.template.edit_prompt", templateEditIndex + 1);
        String display = templateEditBuffer + "§7|";
        int panelW = Math.max(PANEL_W + 40, Math.min(300, font.width(display) + PANEL_PAD * 2));
        int panelH = PANEL_PAD * 2 + ROW_H * 2 + 2;
        int px = PanelChrome.clampX(menuX, panelW, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, panelW, panelH);
        g.text(font, prompt,  px + PANEL_PAD, py + PANEL_PAD,             0xFFFFFFFF, false);
        g.text(font, display, px + PANEL_PAD, py + PANEL_PAD + ROW_H + 2, 0xFFFFFFFF, false);
    }
}
