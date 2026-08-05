package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

import static bottled.mtss.gui.render.PanelChrome.PANEL_PAD;
import static bottled.mtss.gui.render.PanelChrome.PANEL_W;
import static bottled.mtss.gui.render.PanelChrome.ROW_H;


public final class ReorderPanel {

    private ReorderPanel() {}

    public static List<MtssConfig.Stat> allStatsOrdered(MtssConfig.StatListConfig lc) {
        List<MtssConfig.Stat> result = new ArrayList<>();
        for (String name : lc.statOrder) {
            try { result.add(MtssConfig.Stat.valueOf(name)); }
            catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    /** Shared height formula for the reorder/toggle panel: header + one row per stat + close row. */
    public static int panelHeight(MtssConfig.StatListConfig lc) {
        return PANEL_PAD * 2 + ROW_H + ROW_H * allStatsOrdered(lc).size() + ROW_H;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc) {
        List<MtssConfig.Stat> all = allStatsOrdered(lc);
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);
        g.text(font, "§e" + I18n.get("gui.mtss.reorder.title"),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H;
        for (int i = 0; i < all.size(); i++) {
            MtssConfig.Stat stat = all.get(i);
            boolean enabled = lc.isEnabled(stat);
            int ry = rowTop + i * ROW_H;

            PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);

            // Use the lang key for each stat's display name
            String statName = I18n.get("stat.mtss." + stat.name().toLowerCase());
            String label = (enabled ? "§a✔ " : "§c✘ ") + statName;
            g.text(font, label, px + PANEL_PAD + 12, ry + 2, 0xFFFFFFFF, false);

            int orderIdx = lc.statOrder.indexOf(stat.name());
            // ⚙ cog button
            boolean cogHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 28, ry, 10, ROW_H);
            g.text(font, cogHovered ? "§e⚙" : "§7⚙", px + PANEL_W - 28, ry + 2, 0xFFFFFFFF, false);
            if (orderIdx > 0)
                g.text(font, "§7▲", px + PANEL_W - 18, ry + 2, 0xFFFFFFFF, false);
            if (orderIdx < lc.statOrder.size() - 1)
                g.text(font, "§7▼", px + PANEL_W - 10, ry + 2, 0xFFFFFFFF, false);
        }

        int closeY = rowTop + all.size() * ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, closeY, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.mtss.reorder.close"),
                px + PANEL_PAD, closeY + 2, 0xFFFFFFFF, false);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MtssConfig.StatListConfig lc) {
        int panelH = panelHeight(lc);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /**
     * Handles a click at (mx, my). Returns the stat whose ⚙ was clicked (caller opens
     * {@link StatSettingsPanel} for it), or null if the click affected this panel directly
     * (reorder/toggle/close) or missed entirely — callers distinguish "closed" via the
     * list's own reorderOpen flag, since a plain close click has no other return signal here.
     */
    public static MtssConfig.Stat handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                               MtssConfig.StatListConfig lc, Runnable onClose) {
        List<MtssConfig.Stat> all = allStatsOrdered(lc);
        int panelH = panelHeight(lc);
        int px     = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py     = PanelChrome.clampY(menuY, panelH, screenH);
        int rowTop = py + PANEL_PAD + ROW_H;

        int closeY = rowTop + all.size() * ROW_H;
        if (PanelChrome.isHoveringRow(mx, my, px, closeY, PANEL_W, ROW_H)) { onClose.run(); return null; }

        for (int i = 0; i < all.size(); i++) {
            MtssConfig.Stat stat = all.get(i);
            int ry = rowTop + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            int orderIdx = lc.statOrder.indexOf(stat.name());
            // ⚙ cog — open per-stat settings
            if (mx >= px + PANEL_W - 28 && mx < px + PANEL_W - 18) {
                return stat;
            }
            // ▲
            if (mx >= px + PANEL_W - 18 && mx < px + PANEL_W - 10 && orderIdx > 0) {
                lc.statOrder.add(orderIdx - 1, lc.statOrder.remove(orderIdx));
            // ▼
            } else if (mx >= px + PANEL_W - 10 && orderIdx < lc.statOrder.size() - 1) {
                lc.statOrder.add(orderIdx + 1, lc.statOrder.remove(orderIdx));
            // toggle enable
            } else {
                lc.setEnabled(stat, !lc.isEnabled(stat));
            }
            MtssConfig.getInstance().save();
            return null;
        }
        return null;
    }
}
