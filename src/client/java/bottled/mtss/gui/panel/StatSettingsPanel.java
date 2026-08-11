package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import bottled.mtss.stat.StatRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

import static bottled.mtss.gui.render.PanelChrome.*;


public final class StatSettingsPanel {

    private StatSettingsPanel() {
    }

    /** Stats whose formatted value supports a configurable decimal-places setting. */
    public static boolean supportsDecimals(MtssConfig.Stat stat) {
        return StatRegistry.get(stat).supportsDecimals();
    }

    /** Stats that can be rendered as a rolling graph instead of text. */
    public static boolean supportsGraph(MtssConfig.Stat stat) {
        return StatRegistry.get(stat).supportsGraph();
    }

    /** Stats that have a user-configurable good/warn color threshold (the row that,
     *  when present, opens {@link ThresholdPanel} via ClickResult.OPEN_THRESHOLDS). */
    public static boolean supportsThresholds(MtssConfig.Stat stat) {
        return StatRegistry.get(stat).supportsThreshold();
    }

    /** Row count for the stat settings panel. */
    public static int panelRows(MtssConfig.Stat stat) {
        int rows = 3; // header + prefix + back.
        if (supportsDecimals(stat)) rows++;
        if (supportsGraph(stat)) rows++;
        if (supportsThresholds(stat)) rows++;
        return rows;
    }

    public static int panelHeight(MtssConfig.Stat stat) {
        return PANEL_PAD * 2 + ROW_H * panelRows(stat);
    }

    /** Y-offsets (relative to the panel's top-left (px, py)) for every row in
     *  the panel that a click can actually land on — i.e. every row from
     *  {@link #panelRows} except the non-clickable header (row 0). */
    public static int[] rowOffsets(MtssConfig.Stat stat, int py) {
        List<Integer> offsets = new ArrayList<>();
        int row = 1; // row 0 is the header, which isn't clickable.
        offsets.add(py + PANEL_PAD + ROW_H * row++); // prefix.
        if (supportsDecimals(stat)) offsets.add(py + PANEL_PAD + ROW_H * row++);
        if (supportsGraph(stat)) offsets.add(py + PANEL_PAD + ROW_H * row++);
        if (supportsThresholds(stat)) offsets.add(py + PANEL_PAD + ROW_H * row++);
        offsets.add(py + PANEL_PAD + ROW_H * row); // back.
        return offsets.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc, MtssConfig.Stat statSettingsStat) {
        MtssConfig.StatSettings ss = lc.getStatSettings(statSettingsStat);
        String statLabel = I18n.get("stat.mtss." + statSettingsStat.name().toLowerCase());
        boolean decimalsRow = supportsDecimals(statSettingsStat);
        boolean graphRow = supportsGraph(statSettingsStat);
        boolean thresholdsRow = supportsThresholds(statSettingsStat);

        // 1 header row + 1 prefix row + (optional decimals row) + (optional graph row).
        int rows = panelRows(statSettingsStat);
        int panelH = PANEL_PAD * 2 + ROW_H * rows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        // Header.
        g.text(font, "§e" + I18n.get("gui.mtss.stat_settings.title", statLabel),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int[] rowY = rowOffsets(statSettingsStat, py);
        int idx = 0;

        // Show Prefix toggle.
        int ry1 = rowY[idx++];
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry1, PANEL_W, ROW_H);
        String prefixToggle = I18n.get("gui.mtss.stat_settings.show_prefix")
                + (ss.showPrefix ? " §a" + I18n.get("gui.mtss.menu.on")
                : " §c" + I18n.get("gui.mtss.menu.off"));
        g.text(font, "§f" + prefixToggle, px + PANEL_PAD, ry1 + 2, 0xFFFFFFFF, false);

        // Decimals stepper (only for numeric stats).
        if (decimalsRow) {
            int ryDec = rowY[idx++];
            boolean hoverDown = PanelChrome.isHoveringRow(mx, my, px, ryDec, PANEL_W / 2, ROW_H);
            boolean hoverUp = PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ryDec, PANEL_W / 2, ROW_H);
            if (hoverDown) g.fill(px + 1, ryDec, px + PANEL_W / 2, ryDec + ROW_H, 0x44FFFFFF);
            if (hoverUp) g.fill(px + PANEL_W / 2, ryDec, px + PANEL_W - 1, ryDec + ROW_H, 0x44FFFFFF);
            g.text(font, "§f- " + I18n.get("gui.mtss.stat_settings.decimals", ss.decimals),
                    px + PANEL_PAD, ryDec + 2, 0xFFFFFFFF, false);
            g.text(font, "§f+", px + PANEL_W - 14, ryDec + 2, 0xFFFFFFFF, false);
        }

        // Render-as-graph toggle (only for graphable stats.
        if (graphRow) {
            int ryGraph = rowY[idx++];
            PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ryGraph, PANEL_W, ROW_H);
            String graphToggle = I18n.get("gui.mtss.stat_settings.render_as_graph")
                    + (ss.renderAsGraph ? " §a" + I18n.get("gui.mtss.menu.on")
                    : " §c" + I18n.get("gui.mtss.menu.off"));
            g.text(font, "§f" + graphToggle, px + PANEL_PAD, ryGraph + 2, 0xFFFFFFFF, false);
        }

        // Custom Thresholds sub-panel opener (only for threshold-eligible stats).
        if (thresholdsRow) {
            int ryTh = rowY[idx++];
            PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ryTh, PANEL_W, ROW_H);
            g.text(font, "§f" + I18n.get("gui.mtss.stat_settings.custom_thresholds"),
                    px + PANEL_PAD, ryTh + 2, 0xFFFFFFFF, false);
        }

        // Back button.
        int ryBack = rowY[idx];
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ryBack, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.mtss.stat_settings.back"),
                px + PANEL_PAD, ryBack + 2, 0xFFFFFFFF, false);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MtssConfig.Stat statSettingsStat) {
        int rows = (statSettingsStat != null) ? panelRows(statSettingsStat) : 3;
        int panelH = PANEL_PAD * 2 + ROW_H * rows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    public static ClickResult handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                          MtssConfig.StatListConfig lc, MtssConfig.Stat statSettingsStat) {
        MtssConfig.StatSettings ss = lc.getStatSettings(statSettingsStat);
        boolean decimalsRow = supportsDecimals(statSettingsStat);
        boolean graphRow = supportsGraph(statSettingsStat);
        boolean thresholdsRow = supportsThresholds(statSettingsStat);

        int rows = panelRows(statSettingsStat);
        int panelH = PANEL_PAD * 2 + ROW_H * rows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        int[] rowY = rowOffsets(statSettingsStat, py);
        int idx = 0;

        int ry1 = rowY[idx++];
        if (PanelChrome.isHoveringRow(mx, my, px, ry1, PANEL_W, ROW_H)) {
            ss.showPrefix = !ss.showPrefix;
            MtssConfig.getInstance().save();
            return ClickResult.HANDLED;
        }

        if (decimalsRow) {
            int ryDec = rowY[idx++];
            if (PanelChrome.isHoveringRow(mx, my, px, ryDec, PANEL_W / 2, ROW_H)) {
                ss.decimals = Math.max(0, ss.decimals - 1);
                MtssConfig.getInstance().save();
                return ClickResult.HANDLED;
            } else if (PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ryDec, PANEL_W / 2, ROW_H)) {
                ss.decimals = Math.min(4, ss.decimals + 1);
                MtssConfig.getInstance().save();
                return ClickResult.HANDLED;
            }
        }

        if (graphRow) {
            int ryGraph = rowY[idx++];
            if (PanelChrome.isHoveringRow(mx, my, px, ryGraph, PANEL_W, ROW_H)) {
                ss.renderAsGraph = !ss.renderAsGraph;
                MtssConfig.getInstance().save();
                return ClickResult.HANDLED;
            }
        }

        if (thresholdsRow) {
            int ryTh = rowY[idx++];
            if (PanelChrome.isHoveringRow(mx, my, px, ryTh, PANEL_W, ROW_H)) {
                return ClickResult.OPEN_THRESHOLDS;
            }
        }

        int ryBack = rowY[idx];
        if (PanelChrome.isHoveringRow(mx, my, px, ryBack, PANEL_W, ROW_H)) {
            return ClickResult.BACK; // back to reorder panel.
        }
        return ClickResult.NONE;
    }

    /** What a click on this panel should do. */
    public enum ClickResult {NONE, HANDLED, OPEN_THRESHOLDS, BACK}
}
